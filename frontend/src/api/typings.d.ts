declare namespace API {
  type AuthLoginReqVO = {
    /** 用户账号 */
    username: string
    /** 密码 */
    password: string
  }

  type AuthLoginRespVO = {
    /** 用户编号 */
    userId: number
    /** 访问令牌 */
    token: string
  }

  type AuthPermissionInfoRespVO = {
    /** 用户信息 */
    user?: UserVO
    /** 角色标识列表 */
    roles?: string[]
    /** 权限标识列表 */
    permissions?: string[]
    /** 菜单列表（树形，不含按钮） */
    menus?: MenuVO[]
  }

  type CommonResultAuthLoginRespVO = {
    code?: number
    msg?: string
    data?: AuthLoginRespVO
  }

  type CommonResultAuthPermissionInfoRespVO = {
    code?: number
    msg?: string
    data?: AuthPermissionInfoRespVO
  }

  type CommonResultBoolean = {
    code?: number
    msg?: string
    data?: boolean
  }

  type CommonResultKnowledgeBaseRespVO = {
    code?: number
    msg?: string
    data?: KnowledgeBaseRespVO
  }

  type CommonResultListKnowledgeBaseMemberRespVO = {
    code?: number
    msg?: string
    data?: KnowledgeBaseMemberRespVO[]
  }

  type CommonResultListLong = {
    code?: number
    msg?: string
    data?: number[]
  }

  type CommonResultListMenuRespVO = {
    code?: number
    msg?: string
    data?: MenuRespVO[]
  }

  type CommonResultListRoleSimpleRespVO = {
    code?: number
    msg?: string
    data?: RoleSimpleRespVO[]
  }

  type CommonResultLong = {
    code?: number
    msg?: string
    data?: number
  }

  type CommonResultMenuRespVO = {
    code?: number
    msg?: string
    data?: MenuRespVO
  }

  type CommonResultPageResultKnowledgeBaseRespVO = {
    code?: number
    msg?: string
    data?: PageResultKnowledgeBaseRespVO
  }

  type CommonResultPageResultRoleRespVO = {
    code?: number
    msg?: string
    data?: PageResultRoleRespVO
  }

  type CommonResultPageResultUserRespVO = {
    code?: number
    msg?: string
    data?: PageResultUserRespVO
  }

  type CommonResultRoleRespVO = {
    code?: number
    msg?: string
    data?: RoleRespVO
  }

  type CommonResultUserRespVO = {
    code?: number
    msg?: string
    data?: UserRespVO
  }

  type deleteKnowledgeBaseParams = {
    /** 知识库编号 */
    id: number
  }

  type deleteMenuParams = {
    /** 菜单编号 */
    id: number
  }

  type deleteRoleParams = {
    /** 角色编号 */
    id: number
  }

  type deleteUserParams = {
    /** 用户编号 */
    id: number
  }

  type getKnowledgeBasePageParams = {
    reqVO: KnowledgeBasePageReqVO
  }

  type getKnowledgeBaseParams = {
    /** 知识库编号 */
    id: number
  }

  type getMemberListParams = {
    /** 知识库编号 */
    knowledgeBaseId: number
  }

  type getMenuParams = {
    /** 菜单编号 */
    id: number
  }

  type getRoleMenuIdListParams = {
    /** 角色编号 */
    roleId: number
  }

  type getRolePageParams = {
    reqVO: RolePageReqVO
  }

  type getRoleParams = {
    /** 角色编号 */
    id: number
  }

  type getUserPageParams = {
    reqVO: UserPageReqVO
  }

  type getUserParams = {
    /** 用户编号 */
    id: number
  }

  type JsonNode = true

  type KnowledgeBaseMemberInviteReqVO = {
    /** 知识库编号 */
    knowledgeBaseId: number
    /** 被邀请用户账号 */
    username: string
    /** 成员角色（1 编辑者 2 查看者） */
    memberRole: number
  }

  type KnowledgeBaseMemberRespVO = {
    /** 成员编号 */
    id?: number
    /** 知识库编号 */
    knowledgeBaseId?: number
    /** 成员用户编号 */
    userId?: number
    /** 成员用户账号 */
    username?: string
    /** 成员用户昵称 */
    nickname?: string
    /** 成员角色（1 编辑者 2 查看者） */
    memberRole?: number
    /** 加入时间 */
    createTime?: string
  }

  type KnowledgeBaseMemberUpdateReqVO = {
    /** 知识库编号 */
    knowledgeBaseId: number
    /** 成员编号 */
    id: number
    /** 成员角色（1 编辑者 2 查看者） */
    memberRole: number
  }

  type KnowledgeBasePageReqVO = {
    /** 页码，从 1 开始 */
    pageNum?: number
    /** 每页条数 */
    pageSize?: number
    /** 知识库名称（模糊匹配） */
    name?: string
    /** 知识库类型（1 个人 2 共享） */
    type?: number
  }

  type KnowledgeBaseRespVO = {
    /** 知识库编号 */
    id?: number
    /** 知识库名称 */
    name?: string
    /** 知识库类型（1 个人 2 共享） */
    type?: number
    /** 所有者用户编号 */
    ownerId?: number
    /** 是否默认知识库（不可删除） */
    defaultFlag?: boolean
    /** 知识库说明 */
    description?: string
    /** 创建时间 */
    createTime?: string
  }

  type KnowledgeBaseSaveReqVO = {
    /** 知识库名称 */
    name: string
    /** 知识库类型（1 个人 2 共享） */
    type: number
    /** 知识库说明 */
    description?: string
  }

  type MenuRespVO = {
    /** 菜单编号 */
    id?: number
    /** 菜单名称 */
    name?: string
    /** 权限标识 */
    permission?: string
    /** 菜单类型（1 目录 2 菜单 3 按钮） */
    type?: number
    /** 显示顺序 */
    sort?: number
    /** 父菜单ID（根为 0） */
    parentId?: number
    /** 路由地址 */
    path?: string
    /** 菜单图标 */
    icon?: string
    /** 菜单状态（0 开启 1 停用） */
    status?: number
    /** 是否可见 */
    visible?: boolean
    /** 创建时间 */
    createTime?: string
  }

  type MenuSaveReqVO = {
    /** 菜单编号（修改时必填） */
    id?: number
    /** 菜单名称 */
    name: string
    /** 权限标识（按钮类型必填，如 system:user:list） */
    permission?: string
    /** 菜单类型（1 目录 2 菜单 3 按钮） */
    type: number
    /** 显示顺序 */
    sort?: number
    /** 父菜单编号（根为 0） */
    parentId: number
    /** 路由地址 */
    path?: string
    /** 菜单图标 */
    icon?: string
    /** 菜单状态（0 开启 1 停用） */
    status?: number
    /** 是否可见 */
    visible?: boolean
  }

  type MenuVO = {
    /** 菜单编号 */
    id?: number
    /** 菜单名称 */
    name?: string
    /** 权限标识 */
    permission?: string
    /** 菜单类型（1 目录 2 菜单） */
    type?: number
    /** 显示顺序 */
    sort?: number
    /** 父菜单ID（根为 0） */
    parentId?: number
    /** 路由地址 */
    path?: string
    /** 菜单图标 */
    icon?: string
    /** 是否可见 */
    visible?: boolean
    /** 子菜单 */
    children?: any
  }

  type PageResultKnowledgeBaseRespVO = {
    total?: number
    list?: KnowledgeBaseRespVO[]
  }

  type PageResultRoleRespVO = {
    total?: number
    list?: RoleRespVO[]
  }

  type PageResultUserRespVO = {
    total?: number
    list?: UserRespVO[]
  }

  type removeMemberParams = {
    /** 知识库编号 */
    knowledgeBaseId: number
    /** 成员编号 */
    id: number
  }

  type RoleAssignMenuReqVO = {
    /** 角色编号 */
    roleId: number
    /** 菜单编号列表（空列表表示清空该角色的菜单权限） */
    menuIds?: number[]
  }

  type RolePageReqVO = {
    /** 页码，从 1 开始 */
    pageNum?: number
    /** 每页条数 */
    pageSize?: number
    /** 角色名称（模糊匹配） */
    name?: string
    /** 角色标识（模糊匹配） */
    code?: string
    /** 角色状态（0 开启 1 停用） */
    status?: number
  }

  type RoleRespVO = {
    /** 角色编号 */
    id?: number
    /** 角色名称 */
    name?: string
    /** 角色标识 */
    code?: string
    /** 显示顺序 */
    sort?: number
    /** 角色状态（0 开启 1 停用） */
    status?: number
    /** 角色类型（1 内置 2 自定义） */
    type?: number
    /** 备注 */
    remark?: string
    /** 创建时间 */
    createTime?: string
  }

  type RoleSaveReqVO = {
    /** 角色编号（修改时必填） */
    id?: number
    /** 角色名称 */
    name: string
    /** 角色标识 */
    code: string
    /** 显示顺序 */
    sort: number
    /** 角色状态（0 开启 1 停用） */
    status: number
    /** 备注 */
    remark?: string
  }

  type RoleSimpleRespVO = {
    /** 角色编号 */
    id?: number
    /** 角色名称 */
    name?: string
  }

  type UserPageReqVO = {
    /** 页码，从 1 开始 */
    pageNum?: number
    /** 每页条数 */
    pageSize?: number
    /** 用户账号（模糊匹配） */
    username?: string
    /** 用户昵称（模糊匹配） */
    nickname?: string
    /** 账号状态（0 开启 1 停用） */
    status?: number
  }

  type UserPasswordUpdateReqVO = {
    /** 旧密码 */
    oldPassword: string
    /** 新密码 */
    newPassword: string
  }

  type UserProfileUpdateReqVO = {
    /** 用户昵称 */
    nickname: string
    /** 头像地址 */
    avatar?: string
    /** 用户邮箱 */
    email?: string
    /** 手机号码 */
    mobile?: string
  }

  type UserResetPasswordReqVO = {
    /** 用户编号 */
    id: number
    /** 新密码（明文，落库前 BCrypt 加密） */
    password: string
  }

  type UserRespVO = {
    /** 用户编号 */
    id?: number
    /** 用户账号 */
    username?: string
    /** 用户昵称 */
    nickname?: string
    /** 头像地址 */
    avatar?: string
    /** 用户邮箱 */
    email?: string
    /** 手机号码 */
    mobile?: string
    /** 账号状态（0 开启 1 停用） */
    status?: number
    /** 备注 */
    remark?: string
    /** 创建时间 */
    createTime?: string
    /** 角色编号列表（仅单条查询返回） */
    roleIds?: number[]
  }

  type UserSaveReqVO = {
    /** 用户账号 */
    username: string
    /** 密码（明文，落库前 BCrypt 加密） */
    password: string
    /** 用户昵称 */
    nickname: string
    /** 头像地址 */
    avatar?: string
    /** 用户邮箱 */
    email?: string
    /** 手机号码 */
    mobile?: string
    /** 账号状态（0 开启 1 停用），不传默认开启 */
    status?: number
    /** 备注 */
    remark?: string
    /** 角色编号列表 */
    roleIds?: number[]
  }

  type UserUpdateReqVO = {
    /** 用户编号 */
    id: number
    /** 用户昵称 */
    nickname: string
    /** 头像地址 */
    avatar?: string
    /** 用户邮箱 */
    email?: string
    /** 手机号码 */
    mobile?: string
    /** 账号状态（0 开启 1 停用） */
    status: number
    /** 备注 */
    remark?: string
    /** 角色编号列表 */
    roleIds?: number[]
  }

  type UserUpdateStatusReqVO = {
    /** 用户编号 */
    id: number
    /** 账号状态（0 开启 1 停用） */
    status: number
  }

  type UserVO = {
    /** 用户编号 */
    id?: number
    /** 用户账号 */
    username?: string
    /** 用户昵称 */
    nickname?: string
    /** 头像地址 */
    avatar?: string
  }
}
