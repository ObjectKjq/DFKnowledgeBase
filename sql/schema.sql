-- ----------------------------------------------------------------------------
-- 知识库平台 · 用户功能（RBAC）建表脚本
-- 适用 MySQL 5.7+ / 8.0，InnoDB + utf8mb4
-- 逻辑删除约定：deleted = 0 未删除、1 已删除（登录日志除外，不删除）
-- ----------------------------------------------------------------------------

-- ----------------------------
-- 用户信息表
-- ----------------------------
DROP TABLE IF EXISTS `system_user`;
CREATE TABLE `system_user` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    varchar(30)  NOT NULL COMMENT '用户账号',
    `password`    varchar(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    varchar(30)  NOT NULL COMMENT '用户昵称',
    `avatar`      varchar(512) NULL DEFAULT '' COMMENT '头像地址',
    `email`       varchar(50)  NULL DEFAULT '' COMMENT '用户邮箱',
    `mobile`      varchar(30)  NULL DEFAULT '' COMMENT '手机号码',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '账号状态（0 开启 1 停用）',
    `login_ip`    varchar(50)  NULL DEFAULT '' COMMENT '最后登录 IP',
    `login_date`  datetime     NULL DEFAULT NULL COMMENT '最后登录时间',
    `remark`      varchar(500) NULL DEFAULT NULL COMMENT '备注',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表';

-- ----------------------------
-- 角色信息表
-- ----------------------------
DROP TABLE IF EXISTS `system_role`;
CREATE TABLE `system_role` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name`        varchar(30)  NOT NULL COMMENT '角色名称',
    `code`        varchar(100) NOT NULL COMMENT '角色权限标识',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '角色状态（0 开启 1 停用）',
    `type`        tinyint      NOT NULL COMMENT '角色类型（1 内置 2 自定义）',
    `remark`      varchar(500) NULL DEFAULT NULL COMMENT '备注',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_code` (`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色信息表';

-- ----------------------------
-- 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS `system_menu`;
CREATE TABLE `system_menu` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `name`        varchar(50)  NOT NULL COMMENT '菜单名称',
    `permission`  varchar(100) NULL DEFAULT '' COMMENT '权限标识（目录类型无权限串，留空）',
    `type`        tinyint      NOT NULL COMMENT '菜单类型（1 目录 2 菜单 3 按钮）',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父菜单ID（根为 0）',
    `path`        varchar(200) NULL DEFAULT '' COMMENT '路由地址',
    `icon`        varchar(100) NULL DEFAULT '' COMMENT '菜单图标',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '菜单状态（0 开启 1 停用）',
    `visible`     bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否可见',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '菜单权限表';

-- ----------------------------
-- 用户和角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `system_user_role`;
CREATE TABLE `system_user_role` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    `user_id`     bigint      NOT NULL COMMENT '用户ID（关联 system_user 表）',
    `role_id`     bigint      NOT NULL COMMENT '角色ID（关联 system_role 表）',
    `creator`     varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户和角色关联表';

-- ----------------------------
-- 角色和菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `system_role_menu`;
CREATE TABLE `system_role_menu` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    `role_id`     bigint      NOT NULL COMMENT '角色ID（关联 system_role 表）',
    `menu_id`     bigint      NOT NULL COMMENT '菜单ID（关联 system_menu 表）',
    `creator`     varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_role_id` (`role_id`) USING BTREE,
    KEY `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色和菜单关联表';

-- ----------------------------
-- 登录日志表（只增不改不删，故无 updater / update_time / deleted）
-- ----------------------------
DROP TABLE IF EXISTS `system_login_log`;
CREATE TABLE `system_login_log` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '日志编号',
    `user_id`     bigint       NULL DEFAULT NULL COMMENT '用户ID（关联 system_user 表；账号不存在时为空）',
    `username`    varchar(30)  NOT NULL COMMENT '用户账号（快照，不随用户删除）',
    `login_ip`    varchar(50)  NULL DEFAULT '' COMMENT '登录 IP',
    `user_agent`  varchar(512) NULL DEFAULT '' COMMENT '浏览器 UA',
    `result`      tinyint      NOT NULL COMMENT '登录结果（0 成功 10 账号或密码错 20 用户被禁用）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '登录日志表';
