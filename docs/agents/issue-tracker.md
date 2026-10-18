# Issue 跟踪器：GitHub

本仓库的 issue 与 spec 以 GitHub issue 形式存放。所有操作使用 `gh` CLI。

## 约定

- **创建 issue**：`gh issue create --title "..." --body "..."`。多行正文用 heredoc。
- **读取 issue**：`gh issue view <number> --json number,title,body,labels,comments`。
- **列出 issue**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，配合 `--label` 与 `--state` 过滤。
- **设为父 issue 的子 issue**：`gh issue create --parent <parent> ...`，或事后 `gh issue edit <parent> --add-sub-issue <child>`（`gh` 2.94+）。旧版 `gh`：`gh api --method POST repos/<owner>/<repo>/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>`（数据库 id，见下文 **Blocking**）。不支持子 issue 时，在子 issue 正文顶部写 `Part of #<parent>`。
- **评论**：`gh issue comment <number> --body "..."`
- **添加 / 移除标签**：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **关闭**：`gh issue close <number> --comment "..."`

仓库信息从 `git remote -v` 推断；在 clone 内运行时 `gh` 会自动识别。

## Pull request 作为 triage 入口

**PR 作为需求入口：否。**（若本仓库把外部 PR 当作功能请求，改为 `yes`；`/triage` 会读取此开关。）

设为 `yes` 时，PR 与 issue 使用同一套标签和状态，用 `gh pr` 等价命令：

- **读取 PR**：`gh pr view <number> --comments`，diff 用 `gh pr diff <number>`。
- **列出待 triage 的外部 PR**：`gh api --paginate 'repos/{owner}/{repo}/pulls?state=open' --jq '.[] | select(.author_association | IN("OWNER","MEMBER","COLLABORATOR") | not) | {number, title, author: .user.login, author_association, labels: [.labels[].name]}'`。
- **评论 / 打标签 / 关闭**：`gh pr comment`、`gh pr edit --add-label` / `--remove-label`、`gh pr close`。

GitHub 的 issue 与 PR 共用同一编号空间，裸 `#42` 可能是任一者：先 `gh pr view 42`，失败则 `gh issue view 42`。

## 当技能说「发布到 issue 跟踪器」

创建一个 GitHub issue。

## 当技能说「获取相关 ticket」

按上文 **读取 issue** 的方式读取。

## Wayfinder 操作

供 `/wayfinder` 使用。**地图**是一个 issue，**子** issue 是 ticket。

- **地图**：单个带 `wayfinder:map` 标签的 issue，正文含 Notes / Decisions-so-far / Fog。`gh issue create --label wayfinder:map`。
- **子 ticket**：作为 GitHub 子 issue 关联到地图（见 **设为父 issue 的子 issue**）。不支持子 issue 时，在地图正文的任务列表中登记，并在子 issue 正文顶部写 `Part of #<map>`。标签：`wayfinder:<type>`（`research`/`prototype`/`grilling`/`task`）。认领后把 ticket 分配给执行的开发者。
- **Blocking**：GitHub **原生 issue 依赖**，规范且 UI 可见。添加依赖：`gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`，其中 `<blocker-db-id>` 是 blocker 的**数据库 id**（`gh api repos/<owner>/<repo>/issues/<n> --jq .id`，不是 `#编号` 也不是 `node_id`）。GitHub 通过 `issue_dependencies_summary.blocked_by` 汇报（仅未关闭的 blocker，即实时门禁）。不支持依赖时，退化为子 issue 正文顶部的 `Blocked by: #<n>, #<n>` 行。所有 blocker 关闭后 ticket 解除阻塞。
- **Frontier 查询**：列出地图的未关闭子 issue（`gh issue list --state open`，限定地图的子 issue / 任务列表），剔除有未关闭 blocker（`issue_dependencies_summary.blocked_by > 0`，或 `Blocked by` 行中有未关闭 issue）或已被分配的，按地图顺序取第一个。
- **认领**：`gh issue edit <n> --add-assignee @me`，会话中的第一次写操作。
- **解决**：`gh issue comment <n> --body "<答案>"`，然后 `gh issue close <n>`，最后向地图的 Decisions-so-far 追加上下文指针（要点 + 链接）。
