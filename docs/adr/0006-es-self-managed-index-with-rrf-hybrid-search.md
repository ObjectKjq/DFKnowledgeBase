# ES 索引自管，混合检索用 Java 端 RRF

> 2026-10-28 更新：业务统一通过 `spring-boot-starter-data-elasticsearch` 自动配置的 `ElasticsearchOperations` 访问 ES，不再直接注入 `ElasticsearchClient`。批量写入使用 `IndexQuery` 和 `BulkOptions`，删除使用 `DeleteQuery`，BM25 与 kNN 使用 `NativeQuery`，Java 端 RRF 融合规则保持不变。索引与 mapping 仍由部署脚本管理；使用显式 JSON source 避免引入 `_class` 字段。检索请求禁用部分结果，保持超时和分片失败时明确报错。

检索要求「混合检索」（关键词 + 语义融合排序）与 kb_id/doc_id 级的内容权限过滤，且分块以 MySQL 为权威副本、ES 索引须可重建。继续自管 ES 索引：mapping 由本工程维护（`vector` 512 维 dense_vector（hnsw/cosine）、`text` ik 分词、`kb_id`/`doc_id`/`chunk_id` keyword），由 ES 分别执行 BM25 与 kNN，再在 Java 端按 RRF 公式融合；模糊检索仅使用 kNN。两路查询均须过滤有权且选中的知识库和当前可用文档，返回前再次检查当前权限和状态。

2026-10-23，实施 #18 时实际探测本机 ES Basic 许可，原生 RRF 返回 `403 current license is non-compliant for [Reciprocal Rank Fusion (RRF)]`。此前关于直接使用原生 RRF 可避免费用的判断不成立；用户据此明确确认改为 Java 端融合，不引入付费许可依赖。[Elastic 订阅说明](https://www.elastic.co/subscriptions)列出许可边界，[官方 RRF 文档](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/reciprocal-rank-fusion)给出融合公式。

langchain4j 继续承担解析、切分、向量化及生成编排；检索侧以自定义 `ContentRetriever` 桥接业务检索。ES 建索引等变更仍以 `es/` 中的 DSL 脚本交付，由部署者执行。

**考虑过的方案**：

- `ElasticsearchEmbeddingStore` + langchain4j `ReciprocalRankFuser`：mapping 不可控、metadata 过滤黑盒；不采用该存储封装。
- langchain4j `ElasticsearchConfigurationHybrid`（RRF）：依赖企业许可；被否决。
- 自管索引 + ES 原生 RRF retriever：原方案，融合在 ES 侧一次完成；本机 Basic 许可不能执行，已被替代。
- 自管索引 + Java 端 RRF：保留透明的权限过滤、mapping 与分词，不要求付费许可；用户确认采用。

**影响**：Java 端负责合并候选、计算融合得分和保留实际命中类型，分页与可选重排须基于融合后的候选；两路 ES 查询增加调用成本，任一路失败必须明确报错，不返回伪装完整的结果。ES 索引仍可从 MySQL 分块表重建，检索 DSL 由本工程维护；ik 分词器是 ES 侧前置依赖（ES 9.4.8，elasticsearch-java 9.x），不引入 `langchain4j-elasticsearch`。
