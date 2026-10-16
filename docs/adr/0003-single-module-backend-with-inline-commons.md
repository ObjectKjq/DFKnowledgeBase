# 后端为单 Maven 模块骨架，公共代码内置

参考模板 ruoyi-vue-pro（yudao，MIT）是多模块 reactor，但其 yudao starter 未发布到任何 Maven 仓库（无 `distributionManagement`），独立工程无法解析，只能同 reactor 构建或先 `mvn install`。我们决定后端工程采用单个 Maven 模块（`pom.xml` 与 `src/` 直接位于仓库根），不沿用模板的多模块骨架，也不依赖其 starter；通用返回值、错误码、全局异常处理、分页结构等公共代码参照模板设计在 `com.df.knowledge.common` 下重写，汇总进本模块。原因：本轮只做初始化且明确「后端只要一个模块、不要分模块」，单模块最简；公共代码是全工程的稳定约定，值得先行固化。代价是框架能力（安全、API 日志、幂等等）暂缺，留待技术方案阶段决定自研或引入。

**Considered Options**：

- 保留 yudao 多模块 reactor（framework/15 个 starter + module-\*）：与「后端只要一个模块」冲突，被否决。
- 先 `mvn install` 模板再以依赖引用 yudao starter：增加构建前置与 SNAPSHOT 脆弱性，与「单机 Docker 一条命令跑起来」的产品目标冲突，被否决。
- 逐字拷贝 yudao 源码：需随代码保留 MIT 版权声明且包名混杂，被否决。

**Consequences**：依赖基线为 JDK 21 / Spring Boot 3.4.5 / MyBatis-Plus 3.5.10.1（对齐模板版本，正式选型阶段可整体覆盖）；`com.df.knowledge.common` 是全工程公共返回值/异常约定的唯一出处，业务代码不得另建平行体系。
