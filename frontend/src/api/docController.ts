// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 创建文档 两步式上传：先调文件上传接口拿文件编号与下载链接，再保存文档；格式白名单 PDF/Word/PPT/Excel/TXT/Markdown，创建成功进入「排队」 POST /knowledge/doc/create */
export async function createDoc(body: API.DocCreateReqVO, options?: { [key: string]: any }) {
  return request<API.CommonResultLong>('/knowledge/doc/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除文档 删除文档、分块和索引，无其他引用时回收文件内容，不可恢复 DELETE /knowledge/doc/delete */
export async function deleteDoc(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteDocParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/doc/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取文档详情 失败文档附失败原因；非成员的请求按知识库不存在拒绝 GET /knowledge/doc/get */
export async function getDoc(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDocParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultDocRespVO>('/knowledge/doc/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 分页查询文档 仅返回当前用户有权访问的知识库范围内的文档，可按知识库、标题、状态收窄 GET /knowledge/doc/page */
export async function getDocPage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDocPageParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultPageResultDocRespVO>('/knowledge/doc/page', {
    method: 'GET',
    params: {
      ...params,
      reqVO: undefined,
      ...params['reqVO'],
    },
    ...(options || {}),
  })
}

/** 重试入库 失败文档回到「排队」重新执行；重复失败保留首次原因 POST /knowledge/doc/retry */
export async function retryDoc(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.retryDocParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/knowledge/doc/retry', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
