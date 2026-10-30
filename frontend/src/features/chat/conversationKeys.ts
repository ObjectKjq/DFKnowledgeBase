/** 会话列表按登录身份隔离，供查询、发送和删除共用。 */
export const conversationsKey = (token: string | null) => ['conversations', token] as const

/** 会话详情使用独立键，避免更新某个会话时覆盖列表或其他会话。 */
export const conversationKey = (token: string | null, id: number | null) =>
  ['conversation', token, id] as const
