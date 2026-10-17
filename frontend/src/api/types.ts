export interface CommonResult<T = unknown> {
  /** 错误码，0 表示成功（对齐后端 GlobalErrorCodeConstants.SUCCESS） */
  code: number
  /** 错误提示 */
  msg: string
  /** 业务数据 */
  data: T
}

/** 后端成功码，对齐 `CommonResult.isSuccess()`。 */
export const SUCCESS_CODE = 0
