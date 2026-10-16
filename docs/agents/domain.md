# 领域文档

工程技能探索代码库时，应如何消费本仓库的领域文档。

## 探索前先读

- 仓库根的 **`GLOSSARY.md`**，或
- 仓库根的 **`GLOSSARY-MAP.md`**（若存在）：它指向各上下文的 `GLOSSARY.md`，按主题读取相关的。
- **`docs/adr/`**：阅读与你即将改动的区域相关的 ADR。多上下文仓库中，还要检查 `src/<context>/docs/adr/` 下的上下文级决策。

这些文件不存在时**静默继续**。不要提示缺失，也不要建议预先创建。`/domain-modeling` 技能（经 `/grill-with-docs` 与 `/improve-codebase-architecture` 进入）会在术语或决策真正落定时惰性创建它们。

## 文件结构

单上下文仓库（大多数仓库）：

```
/
├── GLOSSARY.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

多上下文仓库（根目录存在 `GLOSSARY-MAP.md` 时）：

```
/
├── GLOSSARY-MAP.md
├── docs/adr/                          ← 系统级决策
└── src/
    ├── ordering/
    │   ├── GLOSSARY.md
    │   └── docs/adr/                  ← 上下文级决策
    └── billing/
        ├── GLOSSARY.md
        └── docs/adr/
```

## 使用词汇表的术语

输出中出现领域概念时（issue 标题、重构提案、假设、测试名），使用 `GLOSSARY.md` 中定义的术语，不要漂移到词汇表明确回避的同义词。

需要的概念不在词汇表中，这本身是信号：要么你在造项目不用的语言（重新考虑），要么确有缺口（记下来交给 `/domain-modeling`）。

## 标记 ADR 冲突

输出与现有 ADR 矛盾时，显式指出，不要默默覆盖：

> _与 ADR-0007（事件溯源订单）矛盾，但值得重新讨论，因为……_
