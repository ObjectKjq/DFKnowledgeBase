# P0 MCP 接入实施与验收（#24）

## 已交付行为

服务端路径为应用 context-path 下的 `/mcp`，默认 `/api/mcp`。支持 **MCP 2025-11-25 Streamable HTTP**，实现 `initialize`、`notifications/initialized`、`ping`、`tools/list` 和 `tools/call`。正常请求返回单个 JSON-RPC JSON 响应，通知返回 202 空体；不提供服务器主动 SSE 流与协议会话，GET/DELETE 返回 405。官方标准允许这类仅 JSON 响应的实现。[官方传输规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)、[初始化生命周期](https://modelcontextprotocol.io/specification/2025-11-25/basic/lifecycle)。

仅提供三个只读工具：

| 工具 | 参数与行为 |
|---|---|
| `search` | `query` 必填；`mode` 为 HYBRID/FUZZY；`knowledgeBaseIds`、`formats`、`uploadedFrom`、`uploadedTo`、`pageNo`、`pageSize` 复用网页检索。默认每页 20，最大 100，最多 500 页。 |
| `ask` | `question` 必填；可选 `modelId`、知识库/格式/时间过滤与 `selectedChunkIds`。复用 `RagService.askReadOnly`，只返回最终校验后的答案或拒答，不写网页会话；选择分块后不自动补搜。 |
| `list_knowledge_bases` | 按当前用户内容权限分页列出知识库，分页参数为 `pageNo`、`pageSize`。管理员也不会取得额外内容权限。 |

工具描述包含 JSON Schema 和 `readOnlyHint=true`；`ask` 如使用真实外部模型会调用已配置的模型，因此其 `openWorldHint=true`。工具名称、参数结构错误和执行错误按协议分别报告，工具业务失败使用 `isError=true`。[官方工具规范](https://modelcontextprotocol.io/specification/2025-11-25/server/tools)、[工具注解定义](https://modelcontextprotocol.io/specification/2025-11-25/schema)。

## 凭证与运行设置

- 用户在 `/mcp/credential/create` 创建个人凭证，选择独立工具开关；默认有效期 90 天，允许 1～365 天，无永久凭证。完整令牌仅创建响应展示一次，其后只能取得摘要前缀和时间状态。
- 令牌为 SecureRandom 生成的 256 位随机值，数据库仅存 SHA-256 摘要。使用 `Authorization: Bearer <MCP令牌>`，网页登录令牌不能替代 MCP 令牌。
- `/mcp/credential/list`、`update-tools`、`revoke` 均强制绑定当前网页登录用户。撤销、过期、停用用户、成员变化、工具开关和实例总开关对后续调用即时生效。
- 复用 `/system/settings` 的 `mcpEnabled` 与 `mcpRateLimitPerMinute`。默认每凭证每分钟 60 次工具调用；数据库原子固定分钟窗口计数，初始化与发现不消耗工具额度。429 提供 `Retry-After`，分钟窗口更新后可继续调用。
- `/mcp/call-log/page` 仅本人记录，管理员 `/mcp/call-log/statistics` 仅返回工具与结果聚合计数；不会返回其他人的内容或目标知识库明细。记录不包含问题、答案、正文或令牌明文，只保存来源凭证、时间、固定原因码、结果条数和已经授权的知识库编号。
- 升级已有部署须执行 `sql/upgrade/016-mcp.sql`；新部署 `sql/schema.sql` 已包含表结构。升级创建两张新表，不删除旧数据。

## 接入与安全配置

选择支持自定义 Bearer 请求头的 Streamable HTTP 客户端，端点填应用地址下 `/mcp`，令牌来自本人凭证创建响应。发送 `Accept: application/json, text/event-stream`；初始化后每次发送 `MCP-Protocol-Version: 2025-11-25`。同目录 `McpProtocolController.http` 有完整手工协议示例。

服务端校验存在的 Origin，拒绝不受信任来源；未携带 Origin 的原生客户端按 Bearer 凭证认证。本地默认允许实际监听端口的 `http://127.0.0.1:<port>` 与 `http://localhost:<port>`，需要浏览器跨源或反向代理访问时设置明确的允许来源，例如：

```yaml
knowledge:
  mcp:
    allowed-origins: https://knowledge.example.com,http://localhost:5173
```

允许来源须为准确的 scheme、host、port，不接受路径、查询、用户信息、null 或域名后缀匹配。部署在局域网/公网时应由 HTTPS 反向代理保护令牌，本地单机部署可绑定 loopback。[官方 Origin 与部署要求](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)。

本期凭证是预配置 Bearer/PAT，不实现 OAuth 自动发现和授权流程；仅支持 OAuth 自动接入而不支持自定义请求头的客户端不属于本期兼容范围。[官方授权规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization)。

官方最新稳定版已更新到 2026-07-28，移除了初始化握手并采用逐请求元数据。本期明确支持上面的 2025-11-25 版本，不能把初始化实现称为 2026 新版实现；标准客户端必须能够按 2025 版本协商。[官方新版兼容边界](https://modelcontextprotocol.io/specification/2026-07-28/basic/versioning)。

## 已执行证据

2026-10-24，独立工作树 `.worktrees/p0-mcp`：

1. 先红测后实施凭证认证与初始化、通知、唯一消息帧解析和三个工具发现。日志包括 `target/mcp-red1.log`、`mcp-red2.log`、`mcp-red3.log`、`mcp-red6.log`、`mcp-red7.log` 与对应绿测日志。通知及凭证 API 的中间装配错误已修正；不将中间编译失败冒充业务失败用例。
2. 根进程使用用户指定 JDK 21、Maven 和 G:/maven/repository，真实 MySQL HTTP 验证 `McpCredentialHttpTest` 两项通过：创建仅一次明文、本人列表、跨用户撤销拒绝、工具关闭的拒绝审计及记录隔离。日志 `target/mcp-credential-mysql2.log`。
3. 根进程执行 `McpCredentialHttpTest,McpToolJourneyTest`，显式开启 `p0.mysql.enabled=true` 和 `p0.mcp.client.enabled=true`，**8 项全部通过，0 失败、0 错误、0 跳过**，日志 `target/mcp-tools-mysql-client2.log`（01:07:17）。真实凭证、数据库、权限、检索与 RAG 业务均参与；只对外部索引、向量与生成端口使用可控替身。
4. 这 6 项工具旅程覆盖：三个工具的真实业务链路、越权分块与计数过滤、所选上下文不补搜、无网页会话写入、90 天默认与 0/365/366 边界、恰好到期、撤销、初始化后移除成员、8 个并发请求仅 2 个取得限额、下一分钟恢复、实例关闭、用户停用、工具开关与失败/拒绝/成功审计、管理员仅聚合统计。
5. 已真实安装并使用官方 `@modelcontextprotocol/sdk` **1.32.1**。JUnit XML 记录 `p0.mcp.client.enabled=true`，`testOfficialClient_initializationDiscoveryAndAllTools` 实际运行 1.275 秒，无跳过；该测试启动 Node 官方 SDK，必须取得 `MCP_OFFICIAL_CLIENT_OK` 且子进程退出码为 0 才通过。官方客户端完成初始化、通知、发现三个只读工具并分别调用 list/search/ask，随后断言网页会话未增加。不是只安装包或自写 JSON-RPC 请求冒充官方客户端验收。

可按以下步骤复跑（MySQL 凭据仅从 `P0_MYSQL_USER` / `P0_MYSQL_PASSWORD` 环境读取，测试创建并最终删除专用数据库）：

```powershell
npm install --prefix target/mcp-client --no-audit --no-fund @modelcontextprotocol/sdk@1.32.1
$env:MAVEN_OPTS='-Xmx384m'
mvn '-Dmaven.repo.local=G:/maven/repository' '-DargLine=-Xmx512m' '-Dp0.mysql.enabled=true' '-Dp0.mcp.client.enabled=true' '-Dtest=McpCredentialHttpTest,McpToolJourneyTest' test
```

`src/test/java/.../McpProtocolControllerTest` 为无 MySQL 的协议边界测试，数据库接口与时钟使用替身，不作为业务闭环证明。Windows 受限执行环境若 Mockito 动态附加失败，可在测试 JVM 启动时显式添加本地 byte-buddy-agent 的 `-javaagent`，无需修改全局 Mockito 配置。

## 集成后回归与最后修正

- 提交 `c058066` 完成三个工具及官方 SDK 验收脚本后，已在 `7b6e72e` 合入集成分支 `55fe103`，保留 SQL015 与 SQL016。
- 合入后执行 MCP 协议、检索服务与 RAG 服务针对性回归 **32 项通过，0 失败、0 错误、0 跳过**；日志 `target/mcp-post-integration.log`（01:11:27）。
- 最后自查修正不支持方法的来源验证：GET/DELETE 及其他不支持方法也先检查 Origin，非法来源返回 403，合法或未携带来源才返回 405。新增 HTTP 红测先观察到 405 与预期 403 不符，修复后 MCP 协议 **13 项全部通过**；日志 `target/mcp-origin-red.log`、`target/mcp-origin-green.log`（01:13:49）。没有把前述 8 项真实数据库结果冒称为覆盖此后的来源处理改动。
- 新增代码已补中文分层说明、Controller/VO OpenAPI 注解、HTTP 示例及 Javadoc 标签，并通过差异格式检查。

## 待最终完整旅程验收

本票上述业务测试中外部索引、向量模型与生成端口为替身；它们证明 MCP 复用真实业务及授权规则，不证明真实 ES 的检索质量、真实对话模型答案质量或真实 OCR 效果。用户目前授权的本地模拟模型也不能替代真实模型质量验收。#25 仍需在真实数据库、ES、向量、OCR 和命名模型环境完成从上传、所选上下文 RAG、预览、MCP 到撤权、删除恢复与彻底清理的完整旅程，并记录尚不能验证的部分。
