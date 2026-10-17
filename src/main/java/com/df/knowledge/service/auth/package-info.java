/**
 * 认证业务：登录、登出与登录日志
 *
 * 1. 登录成功通过 sa-token（StpUtil）颁发 token
 * 2. 登录结果全量记录到 system_login_log（成功 0、密码错 10、停用 20）
 */
package com.df.knowledge.service.auth;
