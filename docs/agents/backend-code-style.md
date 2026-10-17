# 后端代码规范（Java）

`src/` 下 Java 代码的编写约定。公共约定（返回值/错误码/异常/分页）的唯一出处是 `com.df.knowledge.common`（`CommonResult`、`ErrorCode`、`PageResult`、`ServiceException`、`GlobalExceptionHandler`），业务代码不得另建平行体系（ADR-0003）。

## 技术基线

JDK 21 / Spring Boot 3.4.5 / MyBatis-Plus 3.5.10.1 / Hutool 5.8 / Lombok / springdoc + knife4j。版本唯一出处是根 `pom.xml`。

Lombok 行为由根 `lombok.config` 控制：setter 链式返回 this（`chain=true`）、`@ToString/@EqualsAndHashCode` 默认 callSuper。链式构造 `new Xxx().setA(...).setB(...)` 是常态。

公共能力优先用 MyBatis-Plus 原生 API 与 Hutool（`BeanUtil`、`StrUtil`、`CollUtil`）；确需封装时在 `com.df.knowledge.common` 下补一个通用类，语义与命名保持与现有 common 代码一致。

## 分层与包结构

单 Maven 模块（ADR-0003），业务在 `com.df.knowledge` 下按层分包：

| 包                                          | 职责                                                  |
| ------------------------------------------- | ----------------------------------------------------- |
| `controller/{业务}/vo/{业务}`               | REST 接口层；VO 归 controller 层，不建顶层 vo 包      |
| `service/{业务}`                            | 业务逻辑；`XxxService` + `XxxServiceImpl` 同包        |
| `dal/dataobject/{业务}`、`dal/mysql/{业务}` | DO、MyBatis-Plus Mapper                               |
| `enums/{业务}`                              | 枚举 + 常量类（`ErrorCodeConstants`、`XxxConstants`） |
| `config` / `job` / `mq`                     | 配置类、定时任务、消息（message/producer/consumer）   |
| `common`                                    | 公共返回值/错误码/异常/分页，只放稳定公共约定         |

每个分层包放中文 `package-info.java`，说明该层职责与命名规则（含 URL 前缀、表名前缀的声明）。

## 命名约定

| 类别                                        | 命名                                                         | 示例                 |
| ------------------------------------------- | ------------------------------------------------------------ | -------------------- |
| Controller / Service / ServiceImpl / Mapper | `XxxController`、`XxxService`、`XxxServiceImpl`、`XxxMapper` | `DocController`      |
| DO                                          | `XxxDO`                                                      | `DocDO`              |
| 请求 VO                                     | `XxxPageReqVO` / `XxxListReqVO` / `XxxSaveReqVO`             | `DocSaveReqVO`       |
| 响应 VO                                     | `XxxRespVO`（下拉精简版 `XxxSimpleRespVO`）                  | `DocRespVO`          |
| 枚举 / 常量 / 配置类                        | `XxxEnum` / `XxxConstants`（interface）/ `XxxConfiguration`  | `DocTypeEnum`        |
| 定时任务 / MQ                               | `XxxJob` / `XxxMessage`、`XxxProducer`、`XxxConsumer`        | `DemoJob`            |
| 测试类                                      | `XxxServiceImplTest`                                         | `DocServiceImplTest` |

VO/DTO 归属：VO 归 controller 层（`controller/.../vo`），跨层传输对象归调用方，DO 归 dal 层——不要建大一统的 `vo`/`dto` 顶层包。

## Controller 写法

- 类注解组合：`@Tag(name = "管理后台 - 部门")`（中文名）+ `@RestController` + `@RequestMapping("/模块/业务")` + `@Validated`；方法 `@Operation(summary = "...")`，参数 `@Parameter`，VO 字段 `@Schema`——统一 springdoc（`io.swagger.v3.oas.annotations`），不引旧版 `io.swagger.annotations`。
- 返回统一 `CommonResult<T>`，静态导入 `CommonResult.success`；分页返回 `CommonResult<PageResult<XxxRespVO>>`。
- 路径用动词风格（非 RESTful 名词风格）：`POST /create`、`PUT /update`、`DELETE /delete`、`GET /get`、`GET /page`、`GET /list`。单条查询/删除用 `@RequestParam("id")`，不用路径变量。
- 参数校验：body 前 `@Valid`，字段用 JSR-303（`@NotNull`/`@NotBlank`/`@Size`）；校验失败由 `GlobalExceptionHandler` 统一转 400。
- Service 返回 DO 时，在 Controller 层转 VO（Hutool `BeanUtil.copyProperties(list, XxxRespVO.class)`）；Controller 不写业务逻辑。
- 权限体系见 ADR-0005（sa-token）：方法用 `@SaCheckPermission("system:{资源}:{操作}")` 声明所需权限；全局登录态由 `SaTokenConfiguration` 的 `SaInterceptor` 兜底（放行登录接口与接口文档）。未登录 → 401、无权限 → 403，均由 `GlobalExceptionHandler` 统一转换。
- 惯例：Controller 同目录放同名 `.http` 文件记录请求示例（IDE HTTP Client）。

## Service 写法

- 注解组合 `@Service` + `@Validated` + `@Slf4j`。
- 入参：写操作收 ReqVO，读操作收 ReqVO 或裸 ID；出参 DO / `PageResult<DO>` / `Long` / `void`，转 RespVO 留给 Controller（Service 不 import RespVO）。
- 事务：`@Transactional(rollbackFor = Exception.class)`。
- 校验用私有 `validateXxx` 方法（不引第三方校验库），业务错误抛 `ServiceException`：

```java
private void validateDocExists(Long id) {
    if (docMapper.selectById(id) == null) {
        throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
    }
}
```

- 错误提示带动态参数时，用 Hutool `StrUtil.format` 拼好后走 `ServiceException(ErrorCode, String message)` 覆盖默认提示。
- 简单对象转换用 Hutool `BeanUtil.copyProperties`；复杂映射抽私有 convert 方法并用单测覆盖，转换逻辑不散落在业务方法里。

## 错误码

- 系统级错误码固定在 `GlobalErrorCodeConstants`：0 成功、400/401/403/404/500 对齐 HTTP 语义、9999 兜底，数值不改。
- 业务错误码集中在 `enums/ErrorCodeConstants`（interface + `ErrorCode` 常量，实现用 `ErrorCodeImpl`），按模块分段注释（`// ========== 文档模块 ==========`）。
- 编码为四段十位制：`1-001-004-002`（类型-系统-模块-序号），Java 写 `1_001_004_002`。本工程单系统：第一段固定 `1`（业务级异常）、第二段固定 `001`，第三段按业务模块认领（认领后登记在 `ErrorCodeConstants` 分段注释里），第四段模块内自增。
- 常量命名 `{业务}_{问题描述}`（`DOC_NOT_FOUND`、`DOC_NAME_DUPLICATE`）；文案中文短句（"已经存在该名字的Xxx"、"Xxx不存在"、"存在子Xxx，无法删除"），动态参数用 `{}` 占位、`【】` 包裹回显。

## DO / Mapper

- DO：`@TableName("模块_业务")`（小写下划线，表名带模块前缀）+ `@Data`；每个字段一行中文 Javadoc，枚举字段写 `枚举 {@link XxxEnum}`、外键字段写 `关联 {@link XxxDO#getId()}`——用 `{@link}` 建立字段级关联。
- 公共字段（create_time/update_time/deleted 等）出现第二个 DO 时再抽公共基类，语义对齐：`@TableField(fill = FieldFill.INSERT)` 自动填充 + `@TableLogic` 逻辑删除。
- Mapper：`@Mapper` + `extends BaseMapper<XxxDO>`；查询条件写成 Mapper 的 `default` 方法（简单场景不写 XML），动态条件用 wrapper 的 `condition` 重载（`eq(condition, ...)`、`like(condition, ...)`）实现"空参跳过"。
- 分页统一 `.orderByDesc(XxxDO::getId)`（id 倒序）；方法命名 `selectList(ReqVO)` / `selectPage(ReqVO)` / `selectByXxxAndYyy`。

## 枚举与常量

- 枚举：`@Getter` + `@AllArgsConstructor` + code/name 双字段，值后跟 `// 中文注释`（`ENABLE(0, "开启")`）。
- 常量类一律 `public interface XxxConstants`，行内 `// 中文注释`，分段注释 `// ========== XXX ==========`。

## 注释与日志

- Javadoc 与行内注释全中文；类注释一行概括职责；接口方法必写 `@param`/`@return`；关联用 `{@link}`。
- 行内注释讲"为什么"和步骤编号（`// 1. 不能把自己设为父级`、`// allEntries 清空所有缓存，因为涉及多个缓存`）。
- 日志 `@Slf4j` + `{}` 占位，格式 `log.info("[方法名][描述({})]", 参数)`；业务异常 `log.warn`，兜底 `log.error`（对齐 `GlobalExceptionHandler` 的分级）。

## 测试

- `src/test/java` 同包镜像；类名 `XxxServiceImplTest`，方法名 `test{方法}_{场景}`（`testCreateDoc_docNotFound`）。
- JUnit 5 + Mockito；异常断言用 `assertThrows` 校验 `ServiceException` 携带的错误码。
- 测试方法内用中文分段注释组织：`// 准备参数`、`// 调用`、`// 断言`。

## API 文档

springdoc + knife4j 已在依赖中，接口与 VO 靠注解全量标注（`@Tag`/`@Operation`/`@Schema`），文档即注解，不另写接口文档。
