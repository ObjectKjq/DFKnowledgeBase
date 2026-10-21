// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 创建会话 记录当前用户和发起时的知识库检索范围 POST /knowledge/conversation/create */
export async function createConversation(
  body: API.ConversationCreateReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultLong>('/knowledge/conversation/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除会话 删除当前用户的会话及其全部消息 DELETE /knowledge/conversation/delete */
export async function deleteConversation(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteConversationParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/conversation/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 恢复会话 返回会话及按时间正序排列的完整消息流 GET /knowledge/conversation/get */
export async function getConversation(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getConversationParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultConversationDetailRespVO>('/knowledge/conversation/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 查询会话列表 仅返回当前用户自己的会话 GET /knowledge/conversation/list */
export async function getConversationList(options?: { [key: string]: any }) {
  return request<API.CommonResultListConversationRespVO>('/knowledge/conversation/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 写入消息 向当前用户自己的会话写入结构化消息 POST /knowledge/conversation/message/create */
export async function createMessage(
  body: API.MessageCreateReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultLong>('/knowledge/conversation/message/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 读取消息 按创建时间和消息编号正序返回当前用户会话的消息流 GET /knowledge/conversation/message/list */
export async function getMessageList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMessageListParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultListMessageRespVO>('/knowledge/conversation/message/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
