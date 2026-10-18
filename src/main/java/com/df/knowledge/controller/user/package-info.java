/**
 * 用户接口层：用户管理与个人资料
 *
 * 1. URL 前缀 /system/user，返回统一 CommonResult / PageResult
 * 2. 管理端接口以 @SaCheckPermission("system:user:*") 鉴权，个人资料接口仅要求登录
 */
package com.df.knowledge.controller.user;
