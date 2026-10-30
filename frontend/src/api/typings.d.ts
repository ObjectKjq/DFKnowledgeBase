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

  type CommonResultDocDocumentPreviewRespVO = {
    code?: number
    msg?: string
    data?: DocDocumentPreviewRespVO
  }

  type CommonResultDocGuideRespVO = {
    code?: number
    msg?: string
    data?: DocGuideRespVO
  }

  type CommonResultDocRespVO = {
    code?: number
    msg?: string
    data?: DocRespVO
  }

  type CommonResultDocSearchPageRespVO = {
    code?: number
    msg?: string
    data?: DocSearchPageRespVO
  }

  type CommonResultFileRespVO = {
    code?: number
    msg?: string
    data?: FileRespVO
  }

  type CommonResultInstanceSettingsRespVO = {
    code?: number
    msg?: string
    data?: InstanceSettingsRespVO
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

  type CommonResultListMapStringObject = {
    code?: number
    msg?: string
    data?: Record<string, any>[]
  }

  type CommonResultListMcpCredentialRespVO = {
    code?: number
    msg?: string
    data?: McpCredentialRespVO[]
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

  type CommonResultListModelConfigurationRespVO = {
    code?: number
    msg?: string
    data?: ModelConfigurationRespVO[]
  }

  type CommonResultListModelConfigurationSimpleRespVO = {
    code?: number
    msg?: string
    data?: ModelConfigurationSimpleRespVO[]
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

  type CommonResultMcpCredentialCreatedRespVO = {
    code?: number
    msg?: string
    data?: McpCredentialCreatedRespVO
  }

  type CommonResultMenuRespVO = {
    code?: number
    msg?: string
    data?: MenuRespVO
  }

  type CommonResultModelConfigurationRespVO = {
    code?: number
    msg?: string
    data?: ModelConfigurationRespVO
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

  type CommonResultPageResultMcpCallLogRespVO = {
    code?: number
    msg?: string
    data?: PageResultMcpCallLogRespVO
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
    /** 会话当前范围、过滤、模式、模型、所选上下文与引导状态 */
    state?: RetrievalState
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
    /** 会话当前范围、过滤、模式、模型、所选上下文与引导状态 */
    state?: RetrievalState
    /** 发起会话时的知识库编号范围，空列表表示全部有权知识库 */
    knowledgeBaseIds?: number[]
    /** 创建时间 */
    createTime?: string
    /** 更新时间 */
    updateTime?: string
  }

  type ConversationStateReqVO = {
    /** 会话编号 */
    id: number
    /** 完整会话当前选择；新选择只影响后续消息 */
    state: RetrievalState
  }

  type deleteConversationParams = {
    /** 会话编号 */
    id: number
  }

  type deleteDocParams = {
    /** 文档编号 */
    id: number
  }

  type deleteFileParams = {
    /** 文件编号 */
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

  type deleteUsingDELETEParams = {
    /** 配置编号 */
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
    /** Excel 工作表名称 */
    sheetName?: string
    /** 分块是否来自扫描页 OCR */
    ocr?: boolean
    /** 实际命中类型：KEYWORD、SEMANTIC 或 HYBRID */
    matchType?: string
    /** 实际索引相关性，混合模式为 RRF 得分 */
    score?: number
    /** HTML 转义后以 em 标记关键词的原文 */
    highlight?: string
    /** 原始文件编号，下载时仍重新校验授权 */
    fileId?: number
    /** 当前授权文件下载地址 */
    downloadUrl?: string
    /** PDF 原始页面、TEXT 原文、OFFICE 解析内容 */
    previewType?: string
    /** 文档格式编码 */
    format?: number
    /** 文档上传时间 */
    uploadTime?: string
  }

  type DocCreateReqVO = {
    /** 知识库编号 */
    knowledgeBaseId: number
    /** 文件编号（上传接口返回） */
    fileId: number
    /** 文件下载链接（上传接口返回） */
    fileUrl: string
  }

  type DocDocumentPreviewRespVO = {
    /** 原命中分块与原始文件下载元数据 */
    source?: DocChunkRespVO
    /** 文档全部解析分块，按原文顺序并保留页码、工作表、偏移 */
    chunks?: DocChunkRespVO[]
  }

  type DocGuideReqVO = {
    /** 检索文本 */
    query: string
    /** 知识库编号范围，不传或为空表示全部有权知识库 */
    knowledgeBaseIds?: number[]
    /** 检索模式：HYBRID 混合、FUZZY 模糊 */
    mode?: string
    /** 会话编号 */
    conversationId: number
    /** 页码，从 1 开始 */
    pageNo?: number
    /** 每页数量，默认 20 */
    pageSize?: number
    /** 文档格式编码过滤，空列表表示不限 */
    formats?: number[]
    /** 上传时间起点，包含 */
    uploadedFrom?: string
    /** 上传时间终点，包含 */
    uploadedTo?: string
    /** 澄清回答，支持推荐选项或自由输入 */
    answer?: string
    /** 立即跳过后续澄清并执行混合检索 */
    skip?: boolean
    /** 命名对话模型；省略沿用会话选择 */
    modelId?: number
  }

  type DocGuideRespVO = {
    /** CLARIFY 澄清、RESULTS 检索完成、FALLBACK 模型故障后直接混合 */
    stage?: string
    /** 实际执行的混合查询，供翻页沿用 */
    query?: string
    /** 原始用户问题 */
    originalQuestion?: string
    /** 本轮完整选择快照 */
    state?: RetrievalState
    /** 已提出的澄清问题轮数，上限 2 */
    round?: number
    /** 澄清问题 */
    question?: string
    /** 推荐选项，仍可自由输入 */
    options?: string[]
    /** 模型未配置或失败后的中文原因 */
    reason?: string
    /** 真实混合检索分页结果 */
    search?: DocSearchPageRespVO
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
    /** 文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败） */
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
    /** 文件下载链接 */
    fileUrl?: string
    /** 文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败） */
    status?: number
    /** 失败原因（保留首次失败原因） */
    failReason?: string
    /** 上传时间 */
    createTime?: string
    /** 最近状态变更时间 */
    updateTime?: string
  }

  type DocSearchPageRespVO = {
    total?: number
    list?: DocChunkRespVO[]
    /** 当前有权过滤范围内入库中的文档数 */
    indexingCount?: number
    /** 候选达到一万条窗口，建议缩小过滤范围；total 为当前候选总量 */
    candidateLimitReached?: boolean
  }

  type DocSearchReqVO = {
    /** 检索文本 */
    query: string
    /** 知识库编号范围，不传或为空表示全部有权知识库 */
    knowledgeBaseIds?: number[]
    /** 检索模式：HYBRID 混合、FUZZY 模糊 */
    mode?: string
    /** 会话编号；提供时恢复本会话选择并持久化本轮问题和结果，不传为无会话检索 */
    conversationId?: number
    /** 页码，从 1 开始 */
    pageNo?: number
    /** 每页数量，默认 20 */
    pageSize?: number
    /** 文档格式编码过滤，空列表表示不限 */
    formats?: number[]
    /** 上传时间起点，包含 */
    uploadedFrom?: string
    /** 上传时间终点，包含 */
    uploadedTo?: string
  }

  type downloadFileParams = {
    /** 文件编号 */
    id: number
  }

  type FileRespVO = {
    /** 文件编号 */
    id?: number
    /** 文件下载链接（相对路径） */
    url?: string
    /** 上传时的原始文件名 */
    name?: string
    /** 文件大小（字节） */
    size?: number
    /** 文件 MIME 类型 */
    type?: string
  }

  type get1Params = {
    /** 配置编号 */
    id: number
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

  type getDocumentPreviewParams = {
    /** 原命中分块编号 */
    chunkId: number
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

  type InstanceSettingsRespVO = {
    /** MCP 总开关 */
    mcpEnabled?: boolean
    /** 每凭证每分钟工具调用上限 */
    mcpRateLimitPerMinute?: number
    /** 全实例入库并发 */
    ingestGlobalConcurrency?: number
    /** 单知识库入库并发 */
    ingestKnowledgeBaseConcurrency?: number
  }

  type InstanceSettingsSaveReqVO = {
    /** MCP 总开关 */
    mcpEnabled: boolean
    /** 每凭证每分钟工具调用上限，默认 60 */
    mcpRateLimitPerMinute: number
    /** 全实例入库并发，默认 2 */
    ingestGlobalConcurrency: number
    /** 单知识库入库并发，默认 1 */
    ingestKnowledgeBaseConcurrency: number
  }

  type JsonNode = Record<string, any>

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

  type McpCallLogPageReqVO = {
    /** 页码，从 1 开始 */
    pageNum?: number
    /** 每页条数 */
    pageSize?: number
  }

  type McpCallLogRespVO = {
    /** 记录编号 */
    id?: number
    /** 来源凭证编号 */
    credentialId?: number
    /** 调用时间 */
    callTime?: string
    /** 工具名称 */
    toolName?: string
    /** 已确认有权的知识库编号 JSON */
    knowledgeBaseIds?: string
    /** 结果条数 */
    resultCount?: number
    /** SUCCESS、FAILED 或 REJECTED */
    outcome?: string
    /** 固定原因码 */
    reasonCode?: string
  }

  type McpCredentialCreatedRespVO = {
    /** 凭证编号 */
    id?: number
    /** 名称 */
    name?: string
    /** 仅供辨认的前缀 */
    tokenPrefix?: string
    /** 允许工具 */
    tools?: string[]
    /** 过期时间 */
    expiresAt?: string
    /** 撤销时间 */
    revokedAt?: string
    /** 最后使用时间 */
    lastUsedAt?: string
    /** 创建时间 */
    createTime?: string
    /** 仅本次返回的完整令牌 */
    token?: string
    /** 接入指引 */
    instructions?: string
  }

  type McpCredentialCreateReqVO = {
    /** 凭证名称 */
    name: string
    /** 允许工具：search、ask、list_knowledge_bases，可为空 */
    tools: string[]
    /** 有效天数，默认 90 */
    validDays: number
  }

  type McpCredentialRespVO = {
    /** 凭证编号 */
    id?: number
    /** 名称 */
    name?: string
    /** 仅供辨认的前缀 */
    tokenPrefix?: string
    /** 允许工具 */
    tools?: string[]
    /** 过期时间 */
    expiresAt?: string
    /** 撤销时间 */
    revokedAt?: string
    /** 最后使用时间 */
    lastUsedAt?: string
    /** 创建时间 */
    createTime?: string
  }

  type McpCredentialToolsReqVO = {
    /** 本人凭证编号 */
    id: number
    /** 允许工具名称，可为空 */
    tools: string[]
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
    children?: MenuVO[]
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

  type ModelConfigurationRespVO = {
    /** 配置编号 */
    id?: number
    /** 配置名称 */
    name?: string
    /** 用途 */
    kind?: string
    /** 服务地址 */
    baseUrl?: string
    /** 提供方模型名 */
    model?: string
    /** 是否已配置密钥 */
    hasApiKey?: boolean
    /** 是否本地模拟，true 不代表真实模型连接 */
    simulated?: boolean
    /** 超时秒数 */
    timeoutSeconds?: number
    /** 最近测试状态 */
    testStatus?: string
    /** 最近测试时间 */
    lastTestTime?: string
    /** 安全错误原因 */
    testError?: string
  }

  type ModelConfigurationSaveReqVO = {
    /** 配置编号，创建时为空 */
    id?: number
    /** 唯一配置名称 */
    name: string
    /** 模型用途 CHAT 或 RERANK */
    kind: string
    /** OpenAI 兼容服务根地址，例如 http://localhost:1234/v1 */
    baseUrl: string
    /** 密钥，更新不传则保留，空字符串清除 */
    apiKey?: string
    /** 提供方模型名 */
    model: string
    /** 超时秒数，默认 60 */
    timeoutSeconds?: number
  }

  type ModelConfigurationSimpleRespVO = {
    /** 配置编号 */
    id?: number
    /** 配置名称 */
    name?: string
    /** 是否本地模拟，非真实生成 */
    simulated?: boolean
  }

  type pageParams = {
    request: McpCallLogPageReqVO
  }

  type PageResultDocRespVO = {
    total?: number
    list?: DocRespVO[]
  }

  type PageResultKnowledgeBaseRespVO = {
    total?: number
    list?: KnowledgeBaseRespVO[]
  }

  type PageResultMcpCallLogRespVO = {
    total?: number
    list?: McpCallLogRespVO[]
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
    /** 本次提问的知识库范围；不传沿用会话当前范围，空列表表示全部有权知识库 */
    knowledgeBaseIds?: number[]
    /** 所选分块：不传沿用会话，空数组清空；选择后严格只用这些分块 */
    selectedChunkIds?: number[]
    /** 所选命名模型；不传沿用会话模型 */
    modelId?: number
    /** 格式过滤，不传沿用会话，空列表清空 */
    formats?: number[]
    /** 上传起点，不传沿用会话 */
    uploadedFrom?: string
    /** 上传终点，不传沿用会话 */
    uploadedTo?: string
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

  type RetrievalState = {
    /** 当前模式，新会话默认混合 */
    mode: string
    /** 知识库范围，空数组表示全部有权内容 */
    knowledgeBaseIds?: number[]
    /** 格式编码过滤 */
    formats?: number[]
    /** 上传起点 */
    uploadedFrom?: string
    /** 上传终点 */
    uploadedTo?: string
    /** 所选分块，空数组自动检索 */
    selectedChunkIds?: number[]
    /** 所选命名模型编号 */
    modelId?: number
    /** 引导原始问题 */
    guideQuestion?: string
    /** 已回答的澄清轮数，最多两轮 */
    guideRound?: number
    /** 澄清回答，支持选项或自由输入 */
    guideAnswers?: string[]
  }

  type retryDocParams = {
    /** 文档编号 */
    id: number
  }

  type revokeParams = {
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

  type testParams = {
    /** 配置编号 */
    id: number
  }

  type uploadFileParams = {
    /** 上传文件 */
    file: any
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
