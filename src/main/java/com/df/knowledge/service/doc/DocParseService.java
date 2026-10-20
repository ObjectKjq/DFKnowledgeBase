package com.df.knowledge.service.doc;

/**
 * 文档解析 Service 接口
 *
 * 入库管线的解析阶段：文本抽取、切分与分块落库（PRD FR-3），实现由入库管线后续交付补充
 */
public interface DocParseService {

    /**
     * 解析文档并落库分块
     *
     * @param docId 文档编号
     */
    void parse(Long docId);
}
