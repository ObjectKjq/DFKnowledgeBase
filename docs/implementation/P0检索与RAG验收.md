# P0 检索、引导、会话、RAG 与预览后端

对应 #22，规格见 [P0 功能闭环补齐规格](../specs/P0功能闭环补齐规格.md)。以下为实施契约和实际验证范围，不代表真实模型内容质量或最终完整旅程已经验收。

## 升级与接口

既有部署执行 `sql/upgrade/015-search-conversation-state.sql`，为会话新增 JSON 状态并按原检索范围初始化为 `HYBRID`。新部署使用 `sql/schema.sql`。升级保留既有会话、消息及文档，不重新解释旧答案。

`POST /knowledge/search/search` 接受 `query`、`mode`（`HYBRID` 混合、`FUZZY` 纯语义）、`knowledgeBaseIds`、`formats`、`uploadedFrom`、`uploadedTo`、`pageNo`（从 1 开始）、`pageSize`（默认 20）。返回公共分页 `list/total`，另有 `indexingCount` 与 `candidateLimitReached`。过滤在索引检索前以 MySQL 当前可用文档实施；索引命中、重排后和索引中提示返回前重新按当前权限约束。

ES Basic 只执行 BM25 与 kNN，Java 按 `1/(60+排名)` 融合，两路共同命中为 `HYBRID`，其他为 `KEYWORD` 或 `SEMANTIC`；模糊结果保留向量得分。任一路失败均报检索失败。候选窗口最多一万条，`total` 表示权限重检后的当前候选数量；达到窗口时 `candidateLimitReached=true`，界面须提示缩小范围，不能显示为全部文档的精确命中总数。可选重排在过滤后、分页前调用，已配置模型故障显式报错。

提供 `conversationId` 时检索恢复该会话省略的模式、范围与过滤，并保存本轮 `user` 和 `results` 消息及独立状态快照。前端不应重复创建这两条消息。不提供时为无会话只读检索。`PUT /knowledge/conversation/state/update` 接受 `id/state` 保存当前完整选择，历史消息不随之后修改而改变；新会话默认混合。状态保留已失权的选择编号，后续读取按实时授权取交集，避免将失权后的空交集误解释成全部知识库。

`POST /knowledge/search/guide` 接受检索参数及 `conversationId`、`answer`、`skip`、`modelId`。每次 `query` 传原问题，`answer` 可为推荐选项或自由输入；返回 `stage`（`CLARIFY/RESULTS/FALLBACK`）、`round`、`question/options`、`reason` 和 `search`。同一问题最多提出两个澄清问题，随后混合检索；随时跳过直接混合。模型未配置、不可用或响应无效时返回明确中文原因与真实混合结果。同会话通过数据库行锁串行更新澄清轮数。

`POST /knowledge/rag/ask` 与 `ask-stream` 增加 `selectedChunkIds`（不传沿用、空数组清空）、`modelId` 及过滤；选择后仅使用这些分块，不补充自动搜索且不混入其他证据的历史答案。每次发送和生成完成后检查当前权限及文档状态；选中内容不可用时给出调整/清空提示。指定命名模型传递到生成端口。MCP 使用 `RagService.askReadOnly` 复用同一核心逻辑，忽略会话编号且不创建会话、消息或状态。

流式增量和中间引用均为暂定内容；最终 `done` 为完整答案或拒答，客户端必须整组替换临时文本与引用。无足够依据、非法引用或生成期间撤权时拒答并清空引用；只持久化最终结果。引用编号合法仅为程序校验，不证明每项陈述已被原文支持。

## 验证记录

- HTTP 检索首个红绿切片通过：默认 20 条、第二页、格式过滤、安全高亮及实际相关性得分。
- 真实 MySQL HTTP 会话与所选上下文旅程：选择与追问不调用索引，清空恢复混合，模型编号沿用；生成过程中撤权导致最终拒答且无引用。前两项通过，日志 `target/search-rag-mysql-green2.log`。
- 引导 HTTP 404 业务红测后，真实 MySQL 三项旅程全部通过：包含上述两项及两轮上限、选项/自由输入、跳过、指定模型失败后直接混合；日志 `target/search-rag-guide-green.log`。
- 普通边界回归 39 项通过，覆盖检索、会话、RAG、SSE；日志 `target-rag-regression2.log`。后续切片尚须复跑，不将此阶段结果作为最终所有改动的证明。

## 仍需真实依赖验收

本轮真实 MySQL 测试只替换外部索引、向量和生成端口；引导使用用户授权的明确本地模拟配置。Java RRF 的真实 ES Basic 行为、内置向量相关性、真实模型引用证据质量、Office/PDF 页面实际打开及完整网页/MCP撤权旅程仍由 #25 实测。模拟响应与模型编号传递不能替代真实生成质量验收。

## 整份解析预览

`GET /knowledge/search/document-preview?chunkId=...` 返回 `source/chunks`。`source` 为原命中分块，保留 PDF 页码、字符偏移、工作表、OCR 标记、格式与原始文件兜底；`chunks` 为同份文档全部解析分块，按原文顺序并分别携带位置。Office 展示解析内容，不承诺原始排版还原。全文读取完成后再次检查原分块的当前权限与文档可用状态。

原始文件 `downloadUrl` 包含实际部署的 context-path，打开和下载时仍须当前用户授权。存量无原始文件编号的文档可能没有下载兜底，仍可读取其已保存解析内容；最终存量文件迁移验收需在 #25 实例检查。

引导最终响应及持久化结果包含 `query`（实际组合查询）、`originalQuestion` 和 `state`。普通检索的结果消息固定为 `query/state/search`，后续翻页应使用该条历史消息快照，不能读取当前输入框过滤条件后改写历史结果。

- 2026-10-24：新增普通检索会话恢复快照、授权 PDF 全页及 Office 工作表解析预览后，真实 MySQL HTTP 旅程 5 项全部通过，0 失败/错误/跳过，日志 `target/search-rag-preview-green.log`。仅外部索引/生成使用可控替身。

## 最终分支回归

2026-10-24，规范与全文预览收尾并同步集成环境修正 `c186b36` 后，使用指定 JDK 21、G 盘 Maven 与 `G:/maven/repository` 运行完整普通回归：共 207 项，其中 182 项通过、25 项按明确条件跳过，0 失败、0 错误。日志为 `target/search-final-after-integration.log`。跳过项为需显式启用本机 MySQL/OCR 的旅程，不将跳过计为通过。

五项真实 MySQL HTTP 旅程已在根进程安全注入数据库凭据后执行并全部通过；该结果对应格式收尾前的功能切片，日志 `target/search-rag-preview-green.log`。格式收尾后的数据库/OCR/真实 ES 与最终网页及 MCP 整链路由 #25 继续执行；当前没有用户真实对话模型，引用证据质量仍需真实模型人工核对。
