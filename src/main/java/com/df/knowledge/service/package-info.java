/**
 * 业务逻辑层：Service 接口与实现，承载业务规则与校验
 *
 * 1. 命名：XxxService（接口）+ XxxServiceImpl（实现），同包存放
 * 2. 注解组合：@Service + @Validated + @Slf4j
 * 3. 入参写操作收 ReqVO，出参 DO / PageResult&lt;DO&gt; / Long / void，转 RespVO 留给 Controller
 * 4. 业务错误统一抛 ServiceException(ErrorCodeConstants.X)
 */
package com.df.knowledge.service;
