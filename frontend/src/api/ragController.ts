// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** RAG 检索 在当前用户会话中同步执行权限过滤、混合检索和带引用答案生成 POST /knowledge/rag/ask */
export async function ask(body: API.RagAskReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultRagAnswerRespVO>('/knowledge/rag/ask', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 流式 RAG 检索 SSE 事件的 data 均为 CommonResult。delta 的 data 是暂定答案文本增量；citations 的 data 是当前引用集合（整组替换）；done 的 data 是 RagAnswerRespVO，客户端须以该结果替换暂定文本（可能拒答）。error 使用公共错误码，随后关闭流；不会再发送 done。可用原会话和问题重新提交。 POST /knowledge/rag/ask-stream */
export async function stream(body: API.RagAskReqVO, options?: { [key: string]: any }) {
  return request<API.SseEmitter>('/knowledge/rag/ask-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
