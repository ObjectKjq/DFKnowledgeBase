-- ----------------------------------------------------------------------------
-- 知识库平台 · 用户功能（RBAC）初始数据
-- 前置：先执行 schema.sql
-- 初始账号：admin / admin123（BCrypt 哈希，由 python bcrypt 生成）
-- ----------------------------------------------------------------------------

-- ----------------------------
-- 初始用户：admin / admin123
-- ----------------------------
INSERT INTO `system_user` (`id`, `username`, `password`, `nickname`, `avatar`, `email`, `mobile`, `status`, `login_ip`, `login_date`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'admin', '$2b$10$EEH72pwTx51f6OSexoaJy.wa0jD4WNrCbayHQloBuN0VCcbUp2B9a', '管理员', '', '', '', 0, '', NULL, '系统内置管理员', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------
-- 初始角色
-- ----------------------------
INSERT INTO `system_role` (`id`, `name`, `code`, `sort`, `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '实例管理员', 'instance_admin', 1, 0, 1, '系统内置管理员角色', '1', NOW(), '1', NOW(), b'0'),
(2, '普通用户', 'user', 2, 0, 1, '系统内置普通用户角色', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------
-- 初始菜单（目录/菜单用 100 系编号，按钮 1000 起，参考 ruoyi-vue-pro）
-- ----------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `status`, `visible`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1,    '聊天',     '', 2, 1,  0,    '/chat',         '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(100,  '系统管理', '', 1, 10, 0,    '/system',       '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(101,  '用户管理', '', 2, 1,  100,  '/system/user',  '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(102,  '角色管理', '', 2, 2,  100,  '/system/role',  '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(103,  '菜单管理', '', 2, 3,  100,  '/system/menu',  '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
-- 用户管理按钮
(1000, '用户查询', 'system:user:list',           3, 1, 101, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1001, '用户新增', 'system:user:create',         3, 2, 101, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1002, '用户修改', 'system:user:update',         3, 3, 101, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1003, '用户删除', 'system:user:delete',         3, 4, 101, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1004, '重置密码', 'system:user:reset-password', 3, 5, 101, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1005, '修改状态', 'system:user:update-status',  3, 6, 101, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
-- 角色管理按钮
(1006, '角色查询', 'system:role:list',         3, 1, 102, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1007, '角色新增', 'system:role:create',       3, 2, 102, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1008, '角色修改', 'system:role:update',       3, 3, 102, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1009, '角色删除', 'system:role:delete',       3, 4, 102, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1010, '分配菜单', 'system:role:assign-menu',  3, 5, 102, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
-- 菜单管理按钮
(1011, '菜单查询', 'system:menu:list',    3, 1, 103, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1012, '菜单新增', 'system:menu:create',  3, 2, 103, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1013, '菜单修改', 'system:menu:update',  3, 3, 103, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0'),
(1014, '菜单删除', 'system:menu:delete',  3, 4, 103, '', '', 0, b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------
-- 用户-角色关联：admin → 实例管理员
-- ----------------------------
INSERT INTO `system_user_role` (`id`, `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 1, 1, '1', NOW(), '1', NOW(), b'0');

-- ----------------------------
-- 角色-菜单关联：实例管理员拥有全部菜单；普通用户仅「聊天」
-- ----------------------------
INSERT INTO `system_role_menu` (`id`, `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
-- 实例管理员（role_id = 1）
(1,  1, 1, '1', NOW(), '1', NOW(), b'0'),
(2,  1, 100, '1', NOW(), '1', NOW(), b'0'),
(3,  1, 101, '1', NOW(), '1', NOW(), b'0'),
(4,  1, 102, '1', NOW(), '1', NOW(), b'0'),
(5,  1, 103, '1', NOW(), '1', NOW(), b'0'),
(6,  1, 1000, '1', NOW(), '1', NOW(), b'0'),
(7,  1, 1001, '1', NOW(), '1', NOW(), b'0'),
(8,  1, 1002, '1', NOW(), '1', NOW(), b'0'),
(9,  1, 1003, '1', NOW(), '1', NOW(), b'0'),
(10, 1, 1004, '1', NOW(), '1', NOW(), b'0'),
(11, 1, 1005, '1', NOW(), '1', NOW(), b'0'),
(12, 1, 1006, '1', NOW(), '1', NOW(), b'0'),
(13, 1, 1007, '1', NOW(), '1', NOW(), b'0'),
(14, 1, 1008, '1', NOW(), '1', NOW(), b'0'),
(15, 1, 1009, '1', NOW(), '1', NOW(), b'0'),
(16, 1, 1010, '1', NOW(), '1', NOW(), b'0'),
(17, 1, 1011, '1', NOW(), '1', NOW(), b'0'),
(18, 1, 1012, '1', NOW(), '1', NOW(), b'0'),
(19, 1, 1013, '1', NOW(), '1', NOW(), b'0'),
(20, 1, 1014, '1', NOW(), '1', NOW(), b'0'),
-- 普通用户（role_id = 2）
(21, 2, 1, '1', NOW(), '1', NOW(), b'0');
