# 用户权限采用 RBAC 模型，认证授权用 sa-token

平台需要用户功能与访问控制。参考模板 ruoyi-vue-pro 的用户体系是 Spring Security + 自研 OAuth2 令牌（老版芋道才是 sa-token），我们只取其 RBAC 数据模型（用户→角色→菜单/权限串），认证授权层改用 sa-token（`sa-token-spring-boot3-starter`，`@SaCheckPermission` 注解鉴权，token 用默认内存 DAO，密码 BCrypt），因为其踢人/多端/活跃续签开箱即用，比 Spring Security 全家桶轻得多，适合本地部署单机体量。ADR-0003 留白的安全框架就此定案。

**Considered Options**：

- 照搬 ruoyi 的 Spring Security + OAuth2 令牌体系（含 client 表、access/refresh token 落库）：与单机本地部署的体量不符，被否决。
- 自研 JWT + 拦截器：续签、踢人、权限缓存都要自己造，重复造轮子，被否决。
- sa-token：注解鉴权与会话管理内建、Spring Boot 3 适配成熟，被采纳。

**Consequences**：平台功能权限走 RBAC（用户→角色→菜单，按钮级权限串 `system:{资源}:{操作}`），不做多租户/部门/数据范围；平台「角色」与共享知识库「成员角色」（内容权限）是两套独立体系，后者不进 RBAC。token 不落库意味着服务重启需重新登录（单机可接受，预留切 Redis 的余地）；权限注解统一用 sa-token 原生注解，不自造 `@PreAuthorize("@ss.hasPermission(...)")` 风格包装；`docs/agents/backend-code-style.md` 中「本工程暂无权限体系」一节随之更新。
