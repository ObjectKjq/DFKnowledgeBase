# 文档分块索引

部署前在 ES 9.x 安装与服务端版本匹配的 ik 分词器，然后由部署者在 IDE HTTP Client 执行
`create-doc-chunk-index.http`。修改顶部的 `esUrl` 与 `indexName` 即可指定目标。
该脚本只创建新索引，不删除或覆盖已有索引；也可将 `PUT` 请求复制到 Kibana Dev Tools 执行。

应用配置位于 `application.yaml`：`ES_URL` 默认 `http://localhost:9200`，
`ES_INDEX` 默认 `knowledge_doc_chunk`，须与建索引脚本保持一致。应用不自动建索引。
内置 ONNX bge-small-zh-v15 模型随 Maven 依赖打包，首次入库时加载，固定输出 512 维向量。

每次入库或重启恢复索引时，先移除该文档的旧索引，再按每批最多 32 个分块向量化与写入。
ES 分块编号使用 MySQL 分块主键，重复恢复不会产生重复索引；分块表仍是权威副本。
批量写入使用 `refresh=wait_for`，任意条目失败都按整篇文档失败处理并尝试清理部分索引。
文档全部写入成功后才进入「可用」，失败保留首次原因，可显式重试。

软删除及超期彻底删除同步清理 ES，并在清理未成功时保留数据库记录供重试。
原先可用的文档恢复时先回到「索引中」，重新写入已清理的向量后成为「可用」。
排队、解析中、索引中的文档在服务启动后自动重新入队。

`kb_id`、`doc_id`、`chunk_id` 均为字符串类型的 `keyword`；`text` 使用 `ik_max_word` 建索引、
`ik_smart` 检索；`vector` 使用 512 维 `dense_vector`、HNSW 与余弦相似度。
后续检索必须同时限制当前有权知识库与 MySQL 中状态为「可用」的文档；
索引可见性不能替代内容权限校验，失败清理遇到 ES 故障时尤其如此。
