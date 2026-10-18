# AGENTS.md

## Agent skills

### Issue tracker

Issue 与 spec 在本仓库的 GitHub Issues 中跟踪（使用 `gh` CLI）。详见 `docs/agents/issue-tracker.md`。

### Triage labels

使用五个默认 triage 标签（`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`）。详见 `docs/agents/triage-labels.md`。

### Domain docs

单上下文布局：`GLOSSARY.md` 与 `docs/adr/` 位于仓库根。详见 `docs/agents/domain.md`。

### Code style

写代码前先读对应规范：后端 Java（`src/`）见 `docs/agents/backend-code-style.md`，前端 TS/React（`frontend/`）见 `docs/agents/frontend-code-style.md`。前后端契约唯一出处：后端 `com.df.knowledge.common`，前端 `frontend/src/api`。
