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
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocFormatEnum;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.file.FileService;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档 Service 实现类
 *
 * 状态迁移唯一依据 PRD §8.2 文档生命周期；内容权限由 {@link KnowledgeBasePermissionService} 判定（PRD §8.1）。
 * 创建与重试只保存待处理状态，由定时任务扫描已提交的文档。
 */
@Service
@Validated
public class DocServiceImpl implements DocService {

    @Resource
    private DocMapper docMapper;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private DocChunkMapper docChunkMapper;

    @Resource
    private DocChunkIndexService docChunkIndexService;

    @Resource
    private FileService fileService;

    @Resource
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDoc(Long userId, DocCreateReqVO reqVO) {
        // 知识库生命周期锁防止创建与删除交错产生孤立文档。
        knowledgeBaseMapper.selectByIdForUpdate(reqVO.getKnowledgeBaseId());
        // 1. 权限判定：查看者与非成员不可上传（PRD §8.1 权限矩阵）
        knowledgeBasePermissionService.checkContentWritePermission(userId, reqVO.getKnowledgeBaseId());
        // 2. 文件必须已上传（两步式：文件引用来自文件上传接口，ADR-0009）
        FileDO file = fileService.getAttachableFile(userId, reqVO.getFileId());
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
        doc.setFileUrl(fileService.buildDownloadUrl(file.getId()));
        doc.setStatus(DocStatusEnum.QUEUED.getCode());
        doc.setIngestSubmitTime(LocalDateTime.now());
        docMapper.insert(doc);
        return doc.getId();
    }

    @Override
    public PageResult<DocDO> getDocPage(Long userId, DocPageReqVO reqVO) {
        // 1. 仅查询当前用户有权访问的知识库范围内的文档（自己拥有的 + 自己是成员的共享库）
        List<Long> knowledgeBaseIds = knowledgeBasePermissionService.getAccessibleKnowledgeBaseIds(userId);
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return PageResult.empty();
        }
        // 2. 分页查询（可按知识库、标题、状态收窄）
        Page<DocDO> page = docMapper.selectPage(reqVO, knowledgeBaseIds);
        return PageResult.build(page.getTotal(), page.getRecords());
    }

    @Override
    public DocDO getDoc(Long userId, Long id) {
        // 1. 文档必须存在
        DocDO doc = validateDocExists(id);
        // 2. 权限判定：无权访问按「知识库不存在」拒绝，不泄露知识库存在性
        checkDocPermission(userId, doc, false);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryDoc(Long userId, Long id) {
        // 重试与删除共用文档行锁，防止重试与删除交错。
        DocDO doc = docMapper.selectByIdForUpdate(id);
        if (doc == null) throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        checkDocPermission(userId, doc, true);
        // 2. 仅失败的文档支持重试（PRD §8.2）
        if (!DocStatusEnum.FAILED.getCode().equals(doc.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.DOC_RETRY_NOT_ALLOWED);
        }
        // 3. 回到排队重新执行完整入库流程；失败原因保留首次值（PRD FR-3「重试再次失败保留首次原因」）
        doc.setStatus(DocStatusEnum.QUEUED.getCode());
        doc.setIngestSubmitTime(LocalDateTime.now());
        docMapper.updateById(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoc(Long userId, Long id) {
        // 与知识库删除保持一致的加锁顺序，避免和解析、索引交错。
        knowledgeBaseMapper.selectByDocIdForShare(id);
        // 1. 文档存在 + 内容写入权限（PRD §8.1：删除文档需编辑者及以上）
        DocDO doc = docMapper.selectByIdForUpdate(id);
        if (doc == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        checkDocPermission(userId, doc, true);
        // 2. 先清理索引，成功后删除分块、文档和无其他引用的文件。
        purgeContent(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBaseDocs(Long knowledgeBaseId) {
        for (DocDO doc : docMapper.selectListByKnowledgeBaseId(knowledgeBaseId)) {
            DocDO locked = docMapper.selectByIdForUpdate(doc.getId());
            if (locked != null) purgeContent(locked);
        }
    }

    /** 物理删除文档并回收不再被引用的字节。 */
    private void purgeContent(DocDO doc) {
        docChunkIndexService.deleteByDocId(doc.getId());
        docChunkMapper.deleteByDocId(doc.getId());
        docMapper.purgeById(doc.getId());
        if (doc.getFileId() != null) {
            fileService.delete(doc.getFileId());
        } else if (StrUtil.isNotBlank(doc.getFilePath())) {
            // 历史磁盘引用也可能共享，必须在最后一个引用移除后回收。
            Long remaining = docMapper.countByFilePath(doc.getFilePath());
            if (remaining == 0) FileUtil.del(doc.getFilePath());
        }
    }

    /** 无权文档与不存在文档使用同一错误，避免通过编号探测。 */
    private void checkDocPermission(Long userId, DocDO doc, boolean write) {
        try {
            if (write) {
                knowledgeBasePermissionService.checkContentWritePermission(userId, doc.getKnowledgeBaseId());
            } else {
                knowledgeBasePermissionService.checkViewPermission(userId, doc.getKnowledgeBaseId());
            }
        } catch (ServiceException exception) {
            if (ErrorCodeConstants.KB_NOT_FOUND.getCode() == exception.getErrorCode().getCode()) {
                throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
            }
            throw exception;
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
