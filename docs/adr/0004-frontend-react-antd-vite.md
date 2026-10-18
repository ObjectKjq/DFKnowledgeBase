# 前端为独立的 Vite SPA，不进 Maven reactor

ADR-0001 已定前端采用 TypeScript + React。本轮决定前端工程的具体形态：在仓库根新建独立工程 `frontend/`，用 Vite 构建的纯客户端 SPA，React 19 + Ant Design 6 + TypeScript 5，npm 管理依赖；后端 `pom.xml` 单模块不动，前端不进 Maven reactor，前后端只以 `/api` 前缀下的 `CommonResult{code,msg,data}` HTTP 契约耦合（开发期由 Vite dev server 代理 `/api` 到 `localhost:8080`）。原因：本产品是登录后的单机/私有部署工作台，无 SEO 与首屏服务端渲染需求，纯客户端渲染足以满足；前端产物是纯静态文件，与「单机 Docker 一条命令跑起来」的部署目标一致，可由任意静态服务托管；把 Node 工具链挡在 Java 构建之外，可避免 `mvn` 依赖 Node 环境、避免构建失败面与时长被放大。

**Considered Options**：

- Next.js（含 SSR 与文件路由）：本项目无 SSR/SEO 需求，却引入 Node 运行时作为部署依赖，与静态托管目标冲突，被否决。
- 前端进 Maven reactor（frontend-maven-plugin 在 Maven 生命周期里跑 npm）：构建机需具备 Node 与网络，Java 构建被前端失败牵连，被否决。
- 不引入构建工具（直接写 HTML + CDN 版 antd）：无法做类型检查、无法支撑 `features/` 目录规模增长，被否决。
- 状态管理用 Redux Toolkit：本工程的复杂度不需要其样板量，改用 `@tanstack/react-query`（服务端状态）+ `zustand`（少量客户端状态），被否决。

**Consequences**：

- 仓库根存在两个独立构建入口：后端 `mvn`、前端 `npm`；前端不参与 `mvn package`，需分别执行。
- 前后端契约的唯一出口是 `frontend/src/api`（axios 实例 + `CommonResult<T>` 类型 + 响应拦截器，成功判定对齐 `CommonResult.isSuccess()`，即 `code === 0`）。
- 依赖基线：React/ReactDOM 19.3、antd 6.6、`@ant-design/icons` 6、Vite 8、TypeScript 5.9、react-router 7（library 模式 `createBrowserRouter`）、vitest + Testing Library；包管理器为 npm，不引入 pnpm。
- 主题沿用原型「知序」色板，唯一色源在 `frontend/src/theme`（`tokens.ts` 常量与 `index.css` CSS 变量双写），经 `ConfigProvider` 注入，仅实现亮色，暗色只预留 `algorithm` 切换点不实现。
