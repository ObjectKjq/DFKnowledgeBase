# 前端代码规范（React + TypeScript）

`frontend/` 下 TS/TSX 代码的编写约定。本工程前端是独立的 Vite SPA（ADR-0004，React + TS，不进 Maven reactor），前后端以 `/api` 下的 `CommonResult{code,msg,data}` HTTP 契约耦合。

## 技术基线

React 19 + TypeScript 5.9 + antd 6 + Vite 8 + react-router 7 + zustand + @tanstack/react-query + axios。依赖唯一出处是 `frontend/package.json`。

常用命令：`npm run build`（含 `tsc --noEmit` 类型检查）、`npm run lint`、`npm run format`、`npm run test`（vitest）。改完代码跑 lint 与 build 自检。

## 格式与 lint

Prettier 与 ESLint 是格式的唯一出处（`.prettierrc.json`、`eslint.config.js`）：无分号、单引号、行宽 100、尾逗号 all。格式问题交给 `npm run format`，不对齐空格/换行做手工调整。

## 目录职责

| 目录                      | 职责                                                                                     |
| ------------------------- | ---------------------------------------------------------------------------------------- |
| `src/api`                 | `openapi2ts` 生成目录（`*Controller.ts`、`typings.d.ts`），可整体重新生成                |
| `src/lib`                 | HTTP 契约公共层：axios 实例、拦截器、`CommonResult<T>` 类型（`openapi2ts` 清理范围之外） |
| `src/app`                 | 应用装配：路由、Provider、布局、登录守卫                                                 |
| `src/components`          | 跨 feature 的通用组件                                                                    |
| `src/features/{域}`       | 页面与业务组件，按产品域分（auth / chat / manage）                                       |
| `src/stores`              | zustand store                                                                            |
| `src/theme`               | 唯一色源：`tokens.ts` 常量 + `index.css` CSS 变量                                        |
| `src/mock`                | 原型逻辑的纯函数移植（临时数据层）                                                       |
| `src/styles` / `src/test` | 全局样式 / vitest 装配                                                                   |

路径别名 `@/` = `src/`（`tsconfig.json` 与 `vite.config.ts` 两处维护）。全局样式入口固定在 `main.tsx`（`theme/index.css` → `styles/prototype.css` → `app/app.css`），新样式追加进对应文件，不新增导入入口。

## 组件

- 文件名 = 主组件名，PascalCase，具名导出（`export function Icon(...)`）；一个文件一个主组件，辅助子组件放同文件内不导出（如 `features/chat/Composer.tsx` 里的 `Popover`）。
- Props 就近定义（解构处 inline 类型或紧邻 interface）；组件顶部 JSDoc 一行说明职责或来源（风格见 `components/Icon.tsx`）。
- 图标优先用 `components/Icon`（引用 `index.html` 的 SVG sprite `#i-<name>`），与原型用法一致。
- antd 组件从 `antd` 直接具名导入；只实现亮色主题，暗色仅保留 `ConfigProvider` 的 algorithm 切换点。

## 状态管理

- 服务端状态（列表、详情、分页）用 `@tanstack/react-query`（`QueryClientProvider` 已在 `app/providers.tsx` 装配）；客户端/会话状态用 zustand。
- zustand：`create<State>()(...)`，需持久化时用 `persist`（登录态见 `stores/auth.ts`）。组件内逐字段 selector（`useKb((s) => s.libs)`），不整取 store。

## API 契约

- 请求函数由 `npm run openapi2ts` 从后端 OpenAPI 文档生成到 `src/api/*Controller.ts`（类型在 `api/typings.d.ts`），生成文件不手改；唯一例外是 `import request` 一行，随 `openapi2ts.config.ts` 的 `requestLibPath` 保持一致。`openapi2ts` 会清理 `src/api`，手写的公共文件一律放 `src/lib`，不要放回 `src/api`。
- 所有请求经 `lib/http.ts` 的 `request<T>()` 发出（axios 实例 + 拦截器），返回值是后端 `CommonResult` 全量，业务代码取 `.data`；`CommonResult` 与 `SUCCESS_CODE`（`code === 0`，对齐后端 `CommonResult.isSuccess()`）定义在 `lib/types.ts`。
- 失败提示由拦截器统一弹出，业务代码不重复弹错、不吞掉 reject；表单需要内联提示时传 `options: { silent: true }` 关闭全局弹错。
- 登录令牌由请求拦截器从 `useAuthStore` 取，业务代码不手写 `Authorization` 头；`code === 401`（未登录 / 被踢）由拦截器清会话并统一跳回登录页。
- 错误提示走 `setHttpErrorNotifier` 注入的 message 实例（`app/providers.tsx` 注册）——antd 静态 message 读不到 `ConfigProvider` 主题，不要直接调用。

## 样式

- 颜色/间距只写 `src/theme`：`tokens.ts` 与 `index.css` 的 CSS 变量成对维护，改一处必须同步另一处（见 `tokens.ts` 顶部注释）。
- 组件样式用类名（`styles/prototype.css` 的知序组件类、`app/app.css` 补充），不内联色值、不写新色值。

## mock 层

`src/mock/` 是原型（`docs/prototype/本地化知识库原型_第一版.html`）检索与文案逻辑的纯函数移植，行为必须与原型一致（`mock/logic.ts` 顶部注释注明了与原型函数的对应关系）。接真实后端时替换 mock 调用点，组件里不留 mock/真实双分支；mock 代码随对接完成删除。

## 测试

- vitest + Testing Library + jest-dom；装配在 `vite.config.ts` 的 `test` 段与 `src/test/setup.ts`（jsdom、`matchMedia` 补桩）。
- 组件测试按角色/文本查询（`getByRole`/`getByText`），把 `@/lib/http` 与 store mock 掉；测试文件与被测文件同目录。
