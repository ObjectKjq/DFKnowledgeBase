package com.df.knowledge.service.doc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.doc.vo.DocCreateReqVO;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.dataobject.file.FileDO;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocConstants;
import com.df.knowledge.enums.doc.DocFormatEnum;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.file.FileService;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档 Service 实现类
 *
 * 状态迁移唯一依据 PRD §8.2 文档生命周期；内容权限由 {@link KnowledgeBasePermissionService} 判定（PRD §8.1）。
 * 创建、重试、恢复不加事务包裹：入库任务在写入提交后才异步执行，事务包裹会让任务读不到最新状态
 */
@Service
@Validated
@Slf4j
public class DocServiceImpl implements DocService {

    @Resource
    private DocMapper docMapper;

    @Resource
    private DocChunkMapper docChunkMapper;

    @Resource
    private DocChunkIndexService docChunkIndexService;

    @Resource
    private FileService fileService;

    @Resource
    private DocIngestService docIngestService;

    @Resource
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Override
    public Long createDoc(Long userId, DocCreateReqVO reqVO) {
        // 1. 权限判定：查看者与非成员不可上传（PRD §8.1 权限矩阵）
        knowledgeBasePermissionService.checkContentWritePermission(userId, reqVO.getKnowledgeBaseId());
        // 2. 文件必须已上传（两步式：文件引用来自文件上传接口，ADR-0009）
        FileDO file = fileService.getFile(reqVO.getFileId());
        if (file == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        // 3. 校验格式白名单：PDF/Word/PPT/Excel/TXT/Markdown（PRD FR-3），大小与文件类型在上传时已校验
        String extension = FileUtil.extName(file.getName()).toLowerCase();
        DocFormatEnum format = DocFormatEnum.resolveByExtension(extension);
        if (format == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_FORMAT_NOT_SUPPORTED);
        }
        // 4. 插入文档（初始状态：排队），保存文件引用
        DocDO doc = new DocDO();
        doc.setKnowledgeBaseId(reqVO.getKnowledgeBaseId());
        doc.setTitle(file.getName());
        doc.setFileName(file.getName());
        doc.setFormat(format.getCode());
        doc.setFileSize(file.getSize());
        doc.setFileId(file.getId());
        doc.setFileUrl(reqVO.getFileUrl());
        doc.setStatus(DocStatusEnum.QUEUED.getCode());
        docMapper.insert(doc);
        // 5. 提交入库任务（异步执行骨架，ADR-0007）
        docIngestService.submit(doc.getId());
        return doc.getId();
    }

    @Override
    public PageResult<DocDO> getDocPage(Long userId, DocPageReqVO reqVO) {
        // 1. 仅查询当前用户有权访问的知识库范围内的文档（自己拥有的 + 自己是成员的共享库）
        List<Long> knowledgeBaseIds = knowledgeBasePermissionService.getAccessibleKnowledgeBaseIds(userId);
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return PageResult.empty();
        }
        // 2. 分页查询（可按知识库、标题、状态收窄；已删除文档随状态查询可见，供恢复）
        Page<DocDO> page = docMapper.selectPage(reqVO, knowledgeBaseIds);
        return PageResult.build(page.getTotal(), page.getRecords());
    }

    @Override
    public DocDO getDoc(Long userId, Long id) {
        // 1. 文档必须存在
        DocDO doc = validateDocExists(id);
        // 2. 权限判定：无权访问按「知识库不存在」拒绝，不泄露知识库存在性
        knowledgeBasePermissionService.checkViewPermission(userId, doc.getKnowledgeBaseId());
        return doc;
    }

    @Override
    public void retryDoc(Long userId, Long id) {
        // 1. 文档存在 + 内容写入权限（PRD §8.1：重试解析需编辑者及以上）
        DocDO doc = validateDocExists(id);
        knowledgeBasePermissionService.checkContentWritePermission(userId, doc.getKnowledgeBaseId());
        // 2. 仅失败的文档支持重试（PRD §8.2）
        if (!DocStatusEnum.FAILED.getCode().equals(doc.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.DOC_RETRY_NOT_ALLOWED);
        }
        // 3. 回到排队重新执行完整入库流程；失败原因保留首次值（PRD FR-3「重试再次失败保留首次原因」）
        doc.setStatus(DocStatusEnum.QUEUED.getCode());
        docMapper.updateById(doc);
        // 4. 重新提交入库任务
        docIngestService.submit(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoc(Long userId, Long id) {
        // 1. 文档存在 + 内容写入权限（PRD §8.1：删除文档需编辑者及以上）
        DocDO doc = docMapper.selectByIdForUpdate(id);
        if (doc == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        knowledgeBasePermissionService.checkContentWritePermission(userId, doc.getKnowledgeBaseId());
        // 2. 仅未删除的文档可删除（PRD §8.2：已删除的允许操作是恢复/彻底删除）
        if (DocStatusEnum.DELETED.getCode().equals(doc.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.DOC_DELETE_NOT_ALLOWED);
        }
        // 3. 与索引共用行锁，先同步清理 ES；清理失败时保留文档原状态供重试
        docChunkIndexService.deleteByDocId(id);
        // 4. 软删除：记录删除前状态与删除时间，30 天内可恢复
        doc.setPrevStatus(doc.getStatus());
        doc.setStatus(DocStatusEnum.DELETED.getCode());
        doc.setDeleteTime(LocalDateTime.now());
        docMapper.updateById(doc);
    }

    @Override
    public void restoreDoc(Long userId, Long id) {
        // 1. 文档存在 + 内容写入权限（PRD §8.1：恢复文档需编辑者及以上）
        DocDO doc = validateDocExists(id);
        knowledgeBasePermissionService.checkContentWritePermission(userId, doc.getKnowledgeBaseId());
        // 2. 仅已删除的文档支持恢复（PRD §8.2）
        if (!DocStatusEnum.DELETED.getCode().equals(doc.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.DOC_RESTORE_NOT_ALLOWED);
        }
        // 3. 超过 30 天保留期不可恢复（PRD §8.2：超期自动彻底删除）
        if (doc.getDeleteTime() == null
                || doc.getDeleteTime().plusDays(DocConstants.SOFT_DELETE_RETENTION_DAYS).isBefore(LocalDateTime.now())) {
            throw new ServiceException(ErrorCodeConstants.DOC_RESTORE_EXPIRED);
        }
        // 4. 原先可用的文档向量已经清理，先重建索引，不能直接恢复为可用
        doc.setStatus(DocStatusEnum.AVAILABLE.getCode().equals(doc.getPrevStatus())
                ? DocStatusEnum.INDEXING.getCode() : doc.getPrevStatus());
        doc.setPrevStatus(null);
        doc.setDeleteTime(null);
        docMapper.updateById(doc);
        // 5. 删除前仍在入库流程中的文档，恢复后重新入队继续处理
        if (DocStatusEnum.isInFlight(doc.getStatus())) {
            docIngestService.submit(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredDeletedDocs() {
        // 1. 找出超过保留期（30 天）的软删除文档（PRD §8.2：超期自动彻底删除）
        LocalDateTime deadline = LocalDateTime.now().minusDays(DocConstants.SOFT_DELETE_RETENTION_DAYS);
        List<DocDO> docs = docMapper.selectListByStatusAndDeleteTimeBefore(DocStatusEnum.DELETED.getCode(), deadline);
        // 2. 逐文档加锁复核，避免扫描后已恢复的文档被误清理
        docs.forEach(doc -> {
            DocDO current = docMapper.selectByIdForUpdate(doc.getId());
            if (current == null || !DocStatusEnum.DELETED.getCode().equals(current.getStatus())
                    || current.getDeleteTime() == null || !current.getDeleteTime().isBefore(deadline)) {
                return;
            }
            // ES 删除失败则抛异常回滚，保留文档、分块和原文件以便再次清理
            docChunkIndexService.deleteByDocId(current.getId());
            docMapper.deleteById(current.getId());
            docChunkMapper.deleteByDocId(current.getId());
            // 删除文件：新文档删文件记录（逻辑删除，ADR-0009）；存量文档过渡期删磁盘文件
            if (current.getFileId() != null) {
                fileService.delete(current.getFileId());
            } else if (StrUtil.isNotBlank(current.getFilePath())) {
                FileUtil.del(current.getFilePath());
            }
        });
        if (!docs.isEmpty()) {
            log.info("[purgeExpiredDeletedDocs][彻底删除超期文档({}个)]", docs.size());
        }
    }

    /**
     * 校验文档存在
     *
     * @param id 文档编号
     * @return 文档
     */
    private DocDO validateDocExists(Long id) {
        DocDO doc = docMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        return doc;
    }
}
