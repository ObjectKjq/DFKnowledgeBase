# ES 索引自管，混合检索用 ES 原生 RRF

检索要求「混合检索」（关键字 + 语义融合排序）与 kb_id/doc_id 级的内容权限过滤，且分块以 MySQL 为权威副本、ES 索引须可重建。我们不用 langchain4j 的 `ElasticsearchEmbeddingStore`（固定 vector/text/metadata 三字段、无自定义 mapping 钩子、RRF 混合配置带许可限制），改为自管 ES 索引：mapping 由本工程维护（`vector` 512 维 dense_vector（hnsw/cosine）、`text` ik 分词、`kb_id`/`doc_id`/`chunk_id` keyword），检索一次 ES 请求走原生 RRF retriever（knn + bm25 融合），langchain4j 只承担文档解析/切分/向量化/AiServices/引用编排，检索侧以自定义 `ContentRetriever` 桥接 ES。ES 侧建索引等变更以 DSL 脚本（`es/` 目录）交付，由部署者执行。

**Considered Options**：

- `ElasticsearchEmbeddingStore` + langchain4j `ReciprocalRankFuser` 双路融合：mapping 不可控、需两条查询自行融合、metadata 过滤黑盒；被否决。
- langchain4j `ElasticsearchConfigurationHybrid`（RRF）：依赖企业许可；被否决。
- 自管索引 + ES 原生 RRF retriever：融合在 ES 侧一次完成，mapping/分词/元数据字段完全可控；被采纳。

**Consequences**：ES 索引可从 MySQL 分块表全量重建（FR-11 重建索引、FR-10 换模型重建的前提）；检索 DSL 由本工程维护；ik 分词器成为 ES 侧前置依赖（ES 9.4.8，elasticsearch-java 9.x）；langchain4j 不再依赖 `langchain4j-elasticsearch`。
