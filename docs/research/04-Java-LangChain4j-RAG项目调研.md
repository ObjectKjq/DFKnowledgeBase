# Java + LangChain4j 的 RAG 项目调研

> 调研目的：为「本地化知识库平台」（Java 后端 + RAG + 关键字/向量/混合检索）的选型与架构借鉴提供参考。
> 调研范围：GitHub 仓库检索（name / description / README 关键词）+ Gitee 热门项目，筛选口径为「Java 实现 + 基于 LangChain4j + 具备 RAG 能力」。
> 热度口径：star 数，GitHub REST API / Gitee API 于 2026-10-18 抓取；同一项目 GitHub 与 Gitee 分别标注。
> 信息源优先级：GitHub 仓库 README / 仓库元数据（GitHub API）> 官方文档 > 技术社区文章。调研时间：2026-10。

---

## 一、筛选口径说明

- **入选条件**：后端以 Java 实现，且**明确使用 LangChain4j**（README 或仓库 topics 中出现 `langchain4j`），并提供 RAG / 知识库检索能力。
- **热度排序**：按 star 数降序。跨平台时以 GitHub 数为准，Gitee 项目单独标注其 Gitee star 数（不直接与 GitHub 数横向比较）。
- **混合检索标注**：
  - `✅ 明确` —— README / 官方说明中明确写出「混合检索 / hybrid / BM25 / RRF / 多路召回」。
  - `⚠️ 未明确` —— 确认有 RAG 与向量检索，但未在文档中声明混合检索（需读源码确认）。
- **已排除**：热度很高但技术栈为 Spring AI 的项目（见[第四节](#四被排除的高热度项目非-langchain4j)），例如 `nageoffer/ragent`（4,287★）——其 README 中专门有一篇《为什么不用 Spring AI / LangChain4j》说明选型。

---

## 二、对比总表（按热度降序）

| # | 项目 | 热度 | 平台 | 定位 | 检索能力 | 许可证 | 最近更新 |
|---|---|---|---|---|---|---|---|
| 0 | [langchain4j/langchain4j](https://github.com/langchain4j/langchain4j) | **13,259★** | GitHub | 框架本体（非应用项目） | RAG 全链路 + RRF 重排；Elasticsearch / Azure AI Search 集成原生支持 BM25 + kNN 混合检索 | Apache-2.0 | 2026-10-16 |
| — | [langchain4j/langchain4j-examples](https://github.com/langchain4j/langchain4j-examples) | 1,904★ | GitHub | 官方示例集 | 含 RAG / 检索示例 | Apache-2.0 | 2026-10-13 |
| 1 | [ageerle/ruoyi-ai](https://github.com/ageerle/ruoyi-ai) | **5,756★**（Gitee 5,073★） | GitHub + Gitee | 企业级 AI 应用开发框架：多模型管理、知识库、可视化工作流、多智能体 | 本地 RAG + 向量库（Milvus / Weaviate / Qdrant）；⚠️ 未明确声明混合检索 | MIT | 2026-10-12 |
| 2 | [moyangzhan/langchain4j-aideepin](https://github.com/moyangzhan/langchain4j-aideepin) | 1,383★ | GitHub | AI 应用平台：聊天、绘画、知识库、工作流、MCP 市场 | 知识库 RAG + GraphRAG 主题标签；⚠️ 未明确声明混合检索 | MIT | 2026-10-09 |
| 3 | [LangChat/langchat](https://github.com/LangChat/langchat) | 1,313★ | GitHub + Gitee | Java & LangChain4j 生态的 AI Agent 应用平台：多模型、Agent、知识库 RAG、Skills、MCP | 知识库 RAG；⚠️ 未明确声明混合检索 | Apache-2.0 | 2026-09-27 |
| 4 | [liyupi/ai-code-helper](https://github.com/liyupi/ai-code-helper) | 761★ | GitHub | 教学型项目：AI 编程学习 / 求职辅导机器人 | RAG + 向量数据库；⚠️ 未明确声明混合检索 | 未标注 | 2025-07-10 |
| 5 | [lynx-ai](https://gitee.com/lynx-ai/lynx-ai) | 279★（Gitee） | Gitee | 企业级 AI 智能体管理平台低代码基座（Spring Boot 3 + LangChain4j + Vue3） | 企业级 RAG 知识库：**向量检索 + 混合检索 + 重排序**、精准原文定位 | 见仓库说明 | 活跃 |
| 6 | [ChinaYiqun/java-rag](https://github.com/ChinaYiqun/java-rag) | 161★ | GitHub | 纯 Java 实现的 RAG 项目，面向企业级环境、便于二次开发 | **✅ 明确**（README 提及 hybrid + BM25） | Apache-2.0 | 2026-07-29 |
| 7 | [taishan666/MaxKB4j](https://github.com/taishan666/MaxKB4j) | 135★ | GitHub + Gitee | 借鉴 MaxKB / Dify / FastGPT 的 Java 版 LLMOps + RAG 平台 | pgvector 向量 + MongoDB 全文检索；**✅ 明确**（hybrid） | GPL-3.0 | 2026-10-15 |
| 8 | [Azure-Samples/azure-search-openai-demo-java](https://github.com/Azure-Samples/azure-search-openai-demo-java) | 129★ | GitHub | 微软官方 Java 版「ChatGPT + 企业数据」示例 | LangChain4j + Azure AI Search；**✅ 明确**（Azure AI Search 默认混合检索） | MIT | 2026-10-14 |
| 9 | [888newstep/ai-agent-platform](https://github.com/888newstep/ai-agent-platform) | 112★ | GitHub | 企业级 AI Agent 平台（Spring Boot 3 + LangChain4j + Milvus） | ReAct + **多路召回 RAG + 混合检索 + BM25 + RRF** + 语义缓存；**✅ 明确** | 见仓库说明 | 2026-10-11 |
| 10 | [ZJunCher/xiaoyan-ai-dev-assistant](https://github.com/ZJunCher/xiaoyan-ai-dev-assistant) | 26★ | GitHub | RAG 混合检索 + 多轮记忆的 AI 研发助手（团队知识问答） | **混合检索 + BM25 + RRF**；**✅ 明确** | 未标注 | 2026-05-12 |
| 11 | [qq749812679/rag-agent](https://github.com/qq749812679/rag-agent) | 17★ | GitHub | 跨文档复杂查询的 RAG + 知识图谱 Agent | **✅ 明确**（跨文档混合检索 + 知识图谱 + MCP） | 未标注 | 2025-03-22 |
| 12 | [yeffky/hmdp-ai-agent](https://github.com/yeffky/hmdp-ai-agent) | 11★ | GitHub | O2O 生活服务 AI Agent（Spring Boot + LangGraph4j + LangChain4j + DeepSeek） | **RAG 混合检索** + Text2SQL + SSE；**✅ 明确** | 未标注 | 2026-08-22 |
| 13 | [ZhaoShiBao/ruoyi-rag-langchain4j](https://gitee.com/ZhaoShiBao/ruoyi-rag-langchain4j) | 4★（Gitee） | Gitee | 基于 LangChain4j 的 RAG 知识库服务端（RuoYi 体系） | 向量检索 + SearXNG 联网 + Neo4j 知识图谱；⚠️ 未明确声明混合检索 | 见仓库说明 | 活跃 |
| 14 | [aleiliuwei/langchain4j-spring-agent](https://gitee.com/aleiliuwei/langchain4j-spring-agent) | 0★（Gitee） | Gitee | 企业级 AI 应用脚手架：Spring AI / LangChain4j + Elasticsearch + MCP | ES 向量 & 图谱增强检索；README 标注 **规划中** Hybrid / Re-rank | 见仓库说明 | 早期 |

> 补充：`moyangzhan/langchain4j-aideepin` 为多仓库结构，另有 [服务端](https://github.com/moyangzhan/langchain4j-aideepin)、[Web 端](https://github.com/moyangzhan/langchain4j-aideepin-web)（145★）、[管理端](https://github.com/moyangzhan/langchain4j-aideepin-admin)（119★）。

---

## 三、混合检索的实现路径（可借鉴）

在 LangChain4j 生态内，混合检索目前有三条成熟路径：

1. **Elasticsearch 集成（官方最成熟）**：一次查询同时发起 BM25 关键词检索与 kNN 向量检索，再用 RRF（Reciprocal Rank Fusion）融合排序；LangChain4j 提供对应检索器并通过「查询转换 → 路由 → 检索 → 重排」的高级 RAG 流水线接入（[Elasticsearch Labs 官方示例](https://www.elastic.co/search-labs/blog/langchain4j-elasticsearch-hybrid-search)、[LangChain4j 中文文档 · 检索与重排](https://langchain4j.cn/intro/)）。
2. **Azure AI Search 集成**：Azure AI Search 的查询默认即为「关键字 + 向量」混合模式，接 LangChain4j 后无需自行融合（见 `azure-search-openai-demo-java`）。
3. **应用层自研两路召回 + RRF**：使用 pgvector / Milvus 时，常见做法是「稀疏（BM25/全文索引，如 MongoDB 全文检索）+ 稠密（向量）」分别召回，在应用层用 RRF 融合后再重排——`MaxKB4j`、`888newstep/ai-agent-platform`、`xiaoyan-ai-dev-assistant` 均属此类。

关键结论：**LangChain4j 本身只提供 RRF 算法与检索抽象，混合检索能力最终取决于所选的向量/检索后端**（Elasticsearch、Azure AI Search 开箱即用；pgvector、Milvus 需自行融合）。

---

## 四、被排除的高热度项目（非 LangChain4j）

以下项目在「Java + RAG + 混合检索」检索中有很高热度，但技术栈是 **Spring AI / Spring AI Alibaba**，不满足 LangChain4j 条件，仅作横向参考：

| 项目 | 热度 | 技术栈 | 说明 |
|---|---|---|---|
| [nageoffer/ragent](https://github.com/nageoffer/ragent) | 4,287★ | Spring AI 2.0 | 企业级 Agentic RAG：向量 / 关键词 / 知识图谱 / 联网 四路并行召回 + 去重 + RRF 融合 + Rerank；README 专文说明为何不用 Spring AI / LangChain4j |
| [java-up-up/nexus-agent](https://github.com/java-up-up/nexus-agent) | 666★ | Spring AI Alibaba | 企业级 Agent 平台，**双通道混合检索** + 组合式切块引擎 |
| [ryanmeowy/anchr-app](https://github.com/ryanmeowy/anchr-app) | 109★ | Spring AI + Elasticsearch | 自托管文档知识库：**hybrid search** + 引用式回答 |

另有 `jeecgboot/JeecgBoot`（48,245★）在 README 中提及 langchain4j，但其定位是低代码平台（含 AI 应用模块），不属于 RAG 专项项目，未计入排名。

---

## 五、结论与选型建议

- **要「开箱即用的混合检索」**：优先 `lynx-ai`（Gitee，RAG 知识库含混合检索 + 重排序 + 原文定位）、`MaxKB4j`、`888newstep/ai-agent-platform`、`xiaoyan-ai-dev-assistant`——这几个 README 中明确写出混合检索 / BM25 / RRF，源码可直接参考其两路召回与融合实现。
- **要高热度、社区活跃的完整平台**：`ruoyi-ai`（5,756★，MIT，模块最全：多模型 + 知识库 + 工作流 + 多智能体）、`langchain4j-aideepin`（1,383★，MIT）、`LangChat`（1,313★，Apache-2.0）。
- **要官方最稳的工程路径**：直接基于 `langchain4j` 的 **Elasticsearch 集成**（`HybridSearchContentRetriever`，BM25 + kNN + RRF）或 **Azure AI Search 集成**；若已定 pgvector / Milvus，需自行实现 RRF 融合与重排。
- **本项目（本地化部署、关键字 + 图片 + 向量 + RAG、Web + MCP）最值得借鉴的三点**：
  1. `ruoyi-ai` 的**多向量库可插拔**（Milvus / Weaviate / Qdrant）与工作流编排；
  2. `lynx-ai` 的**混合检索 + 重排序 + 原文精准定位**闭环；
  3. `MaxKB4j` 的**向量库（pgvector）+ 全文检索库（MongoDB）分离**的混合检索架构。

---

## 六、数据说明与局限

- 热度为 2026-10-18 抓取快照，star 数持续变动；GitHub 与 Gitee 的 star 不可直接比较。
- 「混合检索」判定主要依据 README / 仓库描述的关键词（混合检索 / hybrid / BM25 / RRF / 多路召回），标注 `⚠️ 未明确` 的项目**可能存在实现但未写进文档**，需读源码确认。
- Gitee 项目（`lynx-ai` 等）受 API 限制，仅取到 star 数与描述，未取得许可证与更新时间字段。
- 本调研基于仓库公开信息，未逐一编译验证各项目的检索效果（BM25 分词、重排模型选型等细节需实测）。
