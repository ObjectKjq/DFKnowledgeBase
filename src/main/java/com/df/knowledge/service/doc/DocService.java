package com.df.knowledge.service.doc;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档 Service 接口
 *
 * 文档生命周期操作（PRD §8.2）：上传、分页/详情、重试、软删除与恢复、超期彻底删除
 */
public interface DocService {

    /**
     * 上传文档（格式白名单与 50 MB 上限校验，落盘后进入「排队」）
     *
     * @param userId 上传者用户编号
     * @param knowledgeBaseId 知识库编号
     * @param file 上传文件
     * @return 文档编号
     */
    Long uploadDoc(Long userId, Long knowledgeBaseId, MultipartFile file);

    /**
     * 分页查询文档（仅返回用户有权访问的知识库范围内的文档）
     *
     * @param userId 操作者用户编号
     * @param reqVO 分页查询条件
     * @return 文档分页
     */
    PageResult<DocDO> getDocPage(Long userId, DocPageReqVO reqVO);

    /**
     * 获取文档详情
     *
     * @param userId 操作者用户编号
     * @param id 文档编号
     * @return 文档
     */
    DocDO getDoc(Long userId, Long id);

    /**
     * 重试入库（失败文档回到「排队」重新执行，重复失败保留首次原因）
     *
     * @param userId 操作者用户编号
     * @param id 文档编号
     */
    void retryDoc(Long userId, Long id);

    /**
     * 删除文档（软删除，30 天内可恢复到删除前状态）
     *
     * @param userId 操作者用户编号
     * @param id 文档编号
     */
    void deleteDoc(Long userId, Long id);

    /**
     * 恢复文档（原先可用的文档先重建索引；入库进行中的文档重新入队）
     *
     * @param userId 操作者用户编号
     * @param id 文档编号
     */
    void restoreDoc(Long userId, Long id);

    /**
     * 彻底删除超过保留期的软删除文档（移除记录与存储文件，PRD §8.2）
     */
    void purgeExpiredDeletedDocs();
}
