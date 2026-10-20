/**
 * 知识库接口层：知识库与知识库成员管理
 *
 * 1. URL 前缀 /knowledge/base（成员管理 /knowledge/base/member），返回统一 CommonResult / PageResult
 * 2. 内容权限在服务层按知识库成员关系判定（PRD §8.1 权限矩阵），接口仅要求登录，不走 RBAC 权限串
 */
package com.df.knowledge.controller.knowledge;
