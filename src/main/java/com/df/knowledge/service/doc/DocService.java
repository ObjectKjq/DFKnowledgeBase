package com.df.knowledge.service.doc;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.doc.vo.DocCreateReqVO;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;

/**
 * 文档 Service 接口
 *
 * 文档生命周期操作（PRD §8.2）：创建、分页/详情、重试、软删除与恢复、超期彻底删除
 */
public interface DocService {

    /**
     * 创建文档（两步式上传：先传文件拿引用再保存，格式白名单校验后进入「排队」，ADR-0009）
     *
     * @param userId 上传者用户编号
     * @param reqVO 创建信息（知识库编号与文件引用）
     * @return 文档编号
     */
    Long createDoc(Long userId, DocCreateReqVO reqVO);

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
     * 彻底删除超过保留期的软删除文档（移除记录与文件引用，PRD §8.2）
     */
    void purgeExpiredDeletedDocs();
}
