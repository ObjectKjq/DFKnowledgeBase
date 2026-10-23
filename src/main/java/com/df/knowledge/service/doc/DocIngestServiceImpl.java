package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.model.InstanceSettingsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * 文档入库 Service 实现类
 *
 * 状态迁移唯一依据 PRD §8.2 文档生命周期；迁移用 CAS 条件更新，避免与删除/恢复并发时把已删除文档拉回流程
 */
@Service
@Validated
@Slf4j
public class DocIngestServiceImpl implements DocIngestService {

    @Resource
    private DocMapper docMapper;

    @Resource
    private DocParseService docParseService;

    @Resource
    private DocIndexService docIndexService;

    @Resource
    private ThreadPoolTaskExecutor docIngestExecutor;

    private final Map<Long, Long> runningDocs = new HashMap<>();

    @Resource
    private InstanceSettingsService instanceSettingsService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void submit(Long docId) {
        // 提交时间与状态已在文档事务中保存；无锁读取避免回调与解析线程争写同一行。
        DocDO doc = docMapper.selectById(docId);
        if (doc != null && DocStatusEnum.isInFlight(doc.getStatus()) && doc.getIngestSubmitTime() == null) {
            // 兼容尚未写排序时间的旧调用，正常创建、重试与恢复不进入此分支。
            docMapper.updateIngestSubmitTime(docId);
        }
    }

    /** 定时从 MySQL 的未完成状态领取任务；单实例内同一文档最多一个执行者。 */
    @Scheduled(fixedDelayString = "${knowledge.ingest.poll-delay-ms:500}")
    public synchronized void dispatchPendingDocs() {
        var settings = instanceSettingsService.get();
        int globalConcurrency = settings.getIngestGlobalConcurrency();
        int knowledgeBaseConcurrency = settings.getIngestKnowledgeBaseConcurrency();
        if (runningDocs.size() >= globalConcurrency) {
            return;
        }
        // 管理员调整限额后下次领取生效，已执行任务自然完成。
        if (docIngestExecutor.getMaxPoolSize() != globalConcurrency) {
            docIngestExecutor.setCorePoolSize(0);
            docIngestExecutor.setMaxPoolSize(globalConcurrency);
            docIngestExecutor.setCorePoolSize(globalConcurrency);
        }
        List<DocDO> pending = docMapper.selectListByStatuses(DocStatusEnum.IN_FLIGHT_STATUS_CODES);
        for (DocDO doc : pending) {
            if (runningDocs.size() >= globalConcurrency) {
                break;
            }
            long activeInBase = runningDocs.values().stream().filter(doc.getKnowledgeBaseId()::equals).count();
            if (runningDocs.containsKey(doc.getId()) || activeInBase >= knowledgeBaseConcurrency) {
                continue;
            }
            runningDocs.put(doc.getId(), doc.getKnowledgeBaseId());
            try {
                docIngestExecutor.execute(() -> {
                    try {
                        processDoc(doc.getId());
                    } finally {
                        synchronized (DocIngestServiceImpl.this) {
                            runningDocs.remove(doc.getId());
                        }
                    }
                });
            } catch (TaskRejectedException ex) {
                // 线程池正在关停或调整：任务仍在数据库中，下次轮询继续，不由调度线程代跑。
                runningDocs.remove(doc.getId());
                break;
            }
        }
    }

    @Override
    public void processDoc(Long docId) {
        // 1. 仅处理入库进行中的文档（已删除或已结束的任务直接退出）
        DocDO doc = docMapper.selectById(docId);
        if (doc == null || !DocStatusEnum.isInFlight(doc.getStatus())) {
            return;
        }
        Integer entryStatus = doc.getStatus();
        // 2. 解析阶段（解析、切分与分块落库）：排队迁移到解析中，重启恢复的解析中文档续跑，索引中文档解析已完成、跳过
        if (!DocStatusEnum.INDEXING.getCode().equals(entryStatus)) {
            if (!DocStatusEnum.PARSING.getCode().equals(entryStatus)
                    && docMapper.updateStatus(docId, entryStatus, DocStatusEnum.PARSING.getCode(), null) == 0) {
                log.warn("[processDoc][文档({})状态已变化，跳过入库]", docId);
                return;
            }
            try {
                docParseService.parse(docId);
            } catch (Exception ex) {
                markFailed(docId, doc, DocStatusEnum.PARSING.getCode(), ex);
                return;
            }
        }
        // 3. 索引阶段（向量化与写入分块索引）：解析中迁移到索引中，重启恢复的索引中文档续跑
        if (!DocStatusEnum.INDEXING.getCode().equals(entryStatus)
                && docMapper.updateStatus(docId, DocStatusEnum.PARSING.getCode(), DocStatusEnum.INDEXING.getCode(), null) == 0) {
            log.warn("[processDoc][文档({})状态已变化，跳过索引]", docId);
            return;
        }
        try {
            docIndexService.index(docId);
        } catch (Exception ex) {
            markFailed(docId, doc, DocStatusEnum.INDEXING.getCode(), ex);
            return;
        }
        // 4. 索引服务在同一文档行锁内完成「索引中 → 可用」，本层不重复发布状态
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedDocs() {
        // 数据库保留排队及中断状态；不重写提交时间，避免重启改变先后顺序。
        dispatchPendingDocs();
    }

    /**
     * 迁移到失败并记录失败原因
     *
     * 失败原因保留首次值：重试后再次失败不覆盖首次原因（PRD FR-3）
     *
     * @param docId 文档编号
     * @param doc 文档（用于读取已有失败原因）
     * @param fromStatus 迁移前状态，枚举 {@link DocStatusEnum}
     * @param ex 失败异常
     */
    private void markFailed(Long docId, DocDO doc, Integer fromStatus, Exception ex) {
        // 正常关停打断的任务保持未完成状态，启动时继续，而非要求用户手动重试。
        if (Thread.currentThread().isInterrupted()) {
            log.info("[markFailed][文档({})处理因关停中断，保留状态供重启恢复]", docId);
            return;
        }
        String failReason = StrUtil.isBlank(doc.getFailReason())
                ? StrUtil.subPre(StrUtil.blankToDefault(ex.getMessage(), "文档处理失败"), 500)
                : doc.getFailReason();
        log.warn("[markFailed][文档({})入库失败，原因({})]", docId, failReason, ex);
        docMapper.updateStatus(docId, fromStatus, DocStatusEnum.FAILED.getCode(), failReason);
    }
}
