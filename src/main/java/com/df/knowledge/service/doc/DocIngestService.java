package com.df.knowledge.service.doc;

/**
 * 文档入库 Service 接口
 *
 * 异步执行骨架承载入库任务（ADR-0007）：Spring 线程池执行 + 文档状态机（PRD §8.2）驱动
 */
public interface DocIngestService {

    /**
     * 执行单个文档的入库状态机：排队/解析中/索引中 → 解析中 → 索引中 → 可用，异常时迁移失败并记录原因
     *
     * @param docId 文档编号
     */
    void processDoc(Long docId);

    /**
     * 扫描排队、解析中和索引中的文档，按全局与知识库并发限制执行入库任务
     */
    void dispatchPendingDocs();
}
