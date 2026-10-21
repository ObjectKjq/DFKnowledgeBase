package com.df.knowledge.service.doc;

import com.df.knowledge.dal.dataobject.doc.DocChunkDO;

import java.util.List;

/**
 * 文档解析 Service 接口
 *
 * 入库管线的解析阶段：文本抽取、切分与分块落库（PRD FR-3），仅供内部入库与索引调用
 */
public interface DocParseService {

    /**
     * 解析文档并落库分块
     *
     * @param docId 文档编号
     */
    void parse(Long docId);

    /**
     * 按块序号查询文档的分块（内部调用，内容访问权限由对外服务校验）
     *
     * @param docId 文档编号
     * @return 分块列表
     */
    List<DocChunkDO> getDocChunks(Long docId);
}
