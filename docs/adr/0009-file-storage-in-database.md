# 文件本体存 MySQL，上传与业务保存拆成两步

本地化部署不能假设对象存储或共享磁盘可用，业务侧又要求「先上传文件拿到链接、再保存业务数据」。采用「文件」双表存 MySQL（GLOSSARY：文件是文档、图片资源等知识对象的原始载体）：`system_file` 存元数据、`system_file_content` 存字节（LONGBLOB），提供通用上传/下载/删除接口（`/system/file`）；业务表只落 `file_id` + `file_url`（相对路径），文件本体不再散落磁盘。

**Considered Options**：

- 本地磁盘（原 `DocFileService` 方案）：文件与库分离、备份迁移易失配，且文件落盘与业务保存耦合在同一接口里；被否决。
- 对象存储（MinIO/OSS）：引入外部依赖，与单机本地化部署（ADR-0001/0003）不符；被否决。
- 可插拔存储器（参考 ruoyi-vue-pro 的 FileClient + 存储器配置表）：当前没有多存储器需求，配置表与工厂属过度设计；仅保留其元数据/内容分离的双表形态。

**Consequences**：单文件上限 50 MB，`content` 用 LONGBLOB，部署 MySQL 需 `max_allowed_packet` ≥ 64MB；文件删除为逻辑删除且不做引用/归属校验，标记后立即 404、不回收字节；文档创建拆为「先 `POST /system/file/upload` 拿 `{id, url}`，再 `POST /knowledge/doc/create` 保存」，旧 `POST /knowledge/doc/upload` 移除；存量磁盘文件（`knowledge.doc.storage-path`）由启动迁移器入库并改写引用，过渡期读文件双读（DB 优先、磁盘兜底），迁移完成后移除双读与 `knowledge_doc.file_path` 列。
