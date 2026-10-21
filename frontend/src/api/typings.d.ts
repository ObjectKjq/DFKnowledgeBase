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

  type CommonResultConversationDetailRespVO = {
    code?: number
    msg?: string
    data?: ConversationDetailRespVO
  }

  type CommonResultDocChunkRespVO = {
    code?: number
    msg?: string
    data?: DocChunkRespVO
  }

  type CommonResultDocRespVO = {
    code?: number
    msg?: string
    data?: DocRespVO
  }

  type CommonResultKnowledgeBaseRespVO = {
    code?: number
    msg?: string
    data?: KnowledgeBaseRespVO
  }

  type CommonResultListConversationRespVO = {
    code?: number
    msg?: string
    data?: ConversationRespVO[]
  }

  type CommonResultListDocChunkRespVO = {
    code?: number
    msg?: string
    data?: DocChunkRespVO[]
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

  type CommonResultListMessageRespVO = {
    code?: number
    msg?: string
    data?: MessageRespVO[]
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

  type CommonResultPageResultDocRespVO = {
    code?: number
    msg?: string
    data?: PageResultDocRespVO
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

  type CommonResultRagAnswerRespVO = {
    code?: number
    msg?: string
    data?: RagAnswerRespVO
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

  type ConversationCreateReqVO = {
    /** 会话标题 */
    title: string
    /** 发起会话时的知识库编号范围，不传或为空表示全部有权知识库 */
    knowledgeBaseIds?: number[]
  }

  type ConversationDetailRespVO = {
    /** 会话编号 */
    id?: number
    /** 会话标题 */
    title?: string
    /** 发起会话时的知识库编号范围，空列表表示全部有权知识库 */
    knowledgeBaseIds?: number[]
    /** 创建时间 */
    createTime?: string
    /** 更新时间 */
    updateTime?: string
    /** 按时间正序排列的消息流 */
    messages?: MessageRespVO[]
  }

  type ConversationRespVO = {
    /** 会话编号 */
    id?: number
    /** 会话标题 */
    title?: string
    /** 发起会话时的知识库编号范围，空列表表示全部有权知识库 */
    knowledgeBaseIds?: number[]
    /** 创建时间 */
    createTime?: string
    /** 更新时间 */
    updateTime?: string
  }

  type deleteConversationParams = {
    /** 会话编号 */
    id: number
  }

  type deleteDocParams = {
    /** 文档编号 */
    id: number
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

  type DocChunkRespVO = {
    /** 知识库编号 */
    knowledgeBaseId?: number
    /** 知识库名称 */
    knowledgeBaseName?: string
    /** 文档编号 */
    docId?: number
    /** 文档标题 */
    docTitle?: string
    /** 分块编号，可用于原文预览 */
    chunkId?: number
    /** 文档内块序号，从 0 开始 */
    chunkIndex?: number
    /** 原文页码，从 1 开始；无分页结构时为空 */
    pageNumber?: number
    /** 原文字符起点，含 */
    startOffset?: number
    /** 原文字符终点，不含 */
    endOffset?: number
    /** 命中分块原文 */
    text?: string
  }

  type DocPageReqVO = {
    /** 页码，从 1 开始 */
    pageNum?: number
    /** 每页条数 */
    pageSize?: number
    /** 知识库编号（不传查全部有权知识库） */
    knowledgeBaseId?: number
    /** 文档标题（模糊匹配） */
    title?: string
    /** 文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败 6 已删除） */
    status?: number
  }

  type DocRespVO = {
    /** 文档编号 */
    id?: number
    /** 知识库编号 */
    knowledgeBaseId?: number
    /** 文档标题 */
    title?: string
    /** 上传时的原始文件名 */
    fileName?: string
    /** 文档格式（1 PDF 2 Word 3 PPT 4 Excel 5 TXT 6 Markdown） */
    format?: number
    /** 文件大小（字节） */
    fileSize?: number
    /** 文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败 6 已删除） */
    status?: number
    /** 失败原因（保留首次失败原因） */
    failReason?: string
    /** 删除时间（软删除 30 天保留期起算点） */
    deleteTime?: string
    /** 上传时间 */
    createTime?: string
    /** 最近状态变更时间 */
    updateTime?: string
  }

  type DocSearchReqVO = {
    /** 检索文本 */
    query: string
    /** 知识库编号范围，不传或为空表示全部有权知识库 */
    knowledgeBaseIds?: number[]
  }

  type getConversationParams = {
    /** 会话编号 */
    id: number
  }

  type getDocPageParams = {
    reqVO: DocPageReqVO
  }

  type getDocParams = {
    /** 文档编号 */
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

  type getMessageListParams = {
    /** 会话编号 */
    conversationId: number
  }

  type getPreviewParams = {
    /** 分块编号 */
    chunkId: number
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

  type MessageCreateReqVO = {
    /** 会话编号 */
    conversationId: number
    /** 消息类型 */
    type: 'user' | 'results' | 'answer' | 'guide' | 'empty' | 'note'
    /** 结构化消息内容，可保存文本、结果卡片与引用 */
    content: JsonNode
  }

  type MessageRespVO = {
    /** 消息编号 */
    id?: number
    /** 会话编号 */
    conversationId?: number
    /** 消息类型 */
    type?: string
    /** 结构化消息内容 */
    content?: JsonNode
    /** 创建时间 */
    createTime?: string
  }

  type PageResultDocRespVO = {
    total?: number
    list?: DocRespVO[]
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

  type RagAnswerRespVO = {
    /** 是否已基于充分依据作答 */
    answered?: boolean
    /** 答案或拒答说明 */
    answer?: string
    /** 拒答后的下一步建议 */
    suggestions?: string[]
    /** 分块级引用 */
    citations?: RagCitationRespVO[]
  }

  type RagAskReqVO = {
    /** 会话编号 */
    conversationId: number
    /** 问题 */
    question: string
    /** 本次提问的知识库范围；不传沿用会话初始范围，空列表表示全部有权知识库 */
    knowledgeBaseIds?: number[]
  }

  type RagCitationRespVO = {
    /** 答案中的引用编号 */
    number?: number
    /** 知识库编号 */
    knowledgeBaseId?: number
    /** 知识库名称 */
    knowledgeBaseName?: string
    /** 文档编号 */
    docId?: number
    /** 文档标题 */
    docTitle?: string
    /** 分块编号 */
    chunkId?: number
    /** 分块序号 */
    chunkIndex?: number
    /** 页码；非 PDF/PPT 文档为空 */
    pageNumber?: number
    /** 原文起始字符偏移 */
    startOffset?: number
    /** 原文结束字符偏移 */
    endOffset?: number
  }

  type removeMemberParams = {
    /** 知识库编号 */
    knowledgeBaseId: number
    /** 成员编号 */
    id: number
  }

  type restoreDocParams = {
    /** 文档编号 */
    id: number
  }

  type retryDocParams = {
    /** 文档编号 */
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

  type SseEmitter = {
    timeout?: number
  }

  type uploadDocParams = {
    /** 知识库编号 */
    knowledgeBaseId: number
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
