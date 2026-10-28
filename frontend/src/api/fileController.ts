// @ts-ignore
/* eslint-disable */
import request from '@/lib/http'

/** 删除文件 仅上传者可删除未关联文件；已关联文件须通过删除文档回收 DELETE /system/file/delete */
export async function deleteFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteFileParams,
  options?: { [key: string]: any },
) {
  return request<API.CommonResultBoolean>('/system/file/delete', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 下载文件 按业务内容权限返回文件字节流：图片与 PDF 内联预览，其余附件下载；无权与不存在统一返回文件不存在 GET /system/file/download */
export async function downloadFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.downloadFileParams,
  options?: { [key: string]: any },
) {
  return request<any>('/system/file/download', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 上传文件 格式白名单：PDF/Word/PPT/Excel/TXT/Markdown 与常见图片，单文件上限 50 MB；返回文件编号与下载链接，业务侧保存链接引用 POST /system/file/upload */
export async function uploadFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadFileParams,
  body: {},
  options?: { [key: string]: any },
) {
  return request<API.CommonResultFileRespVO>('/system/file/upload', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: {
      ...params,
    },
    data: body,
    ...(options || {}),
  })
}
