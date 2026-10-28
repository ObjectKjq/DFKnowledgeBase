package com.df.knowledge.service.doc;

import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.doc.vo.DocCreateReqVO;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;

/**
 * 文档 Service 接口
 *
 * 文档生命周期操作（PRD §8.2）：创建、分页/详情、重试、删除
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
     * 删除文档并清理分块、索引和无其他引用的文件
     *
     * @param userId 操作者用户编号
     * @param id 文档编号
     */
    void deleteDoc(Long userId, Long id);

    /** 清理已授权删除的知识库内容，仅供知识库生命周期使用。
     * @param knowledgeBaseId 知识库编号
     */
    void deleteKnowledgeBaseDocs(Long knowledgeBaseId);
}
