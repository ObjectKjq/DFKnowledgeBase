// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 移除成员 仅共享知识库所有者可操作 DELETE /knowledge/base/member/delete */
export async function removeMember(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.removeMemberParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/base/member/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 邀请成员 按用户账号邀请成员并分配成员角色，仅共享知识库所有者可操作 POST /knowledge/base/member/invite */
export async function inviteMember(
  body: API.KnowledgeBaseMemberInviteReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/base/member/invite', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取成员列表 返回知识库的成员及其用户账号与昵称 GET /knowledge/base/member/list */
export async function getMemberList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMemberListParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultListKnowledgeBaseMemberRespVO>('/knowledge/base/member/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 更改成员角色 仅共享知识库所有者可操作 PUT /knowledge/base/member/update-role */
export async function updateMemberRole(
  body: API.KnowledgeBaseMemberUpdateReqVO,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/base/member/update-role', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
