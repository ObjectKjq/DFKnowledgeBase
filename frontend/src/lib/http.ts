import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'

import { useAuthStore } from '@/stores/auth'

import { SUCCESS_CODE, UNAUTHORIZED_CODE, type CommonResult } from './types'

/** 请求配置：在 axios 配置之上允许 `silent`（表单内联提示场景跳过全局错误提示）。 */
export interface RequestConfig extends AxiosRequestConfig {
  silent?: boolean
}

/**
 * 错误提示的注入点：由 `app/providers` 把 antd 的 message 实例注册进来，
 * 这样拦截器里的提示能吃到 ConfigProvider 的知序主题（静态 message 方法读不到主题）。
 */
let errorNotifier: ((msg: string) => void) | null = null

export function setHttpErrorNotifier(notifier: (msg: string) => void) {
  errorNotifier = notifier
}

/** 统一 HTTP 出口：baseURL `/api`（后端 context-path，开发期由 Vite dev server 代理到 8080）。 */
const http = axios.create({
  baseURL: '/api',
  timeout: 30_000,
})

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 令牌失效（未登录 / 过期 / 被踢）：清会话并统一跳回登录页。 */
function redirectToLogin() {
  useAuthStore.getState().clearSession()
  window.location.assign('/login')
}

/** 普通响应与 SSE 事件共用错误码处理；内联错误可关闭全局提示。 */
export function assertResultSuccess(body: Partial<CommonResult>, silent = false): void {
  if (typeof body.code !== 'number' || body.code === SUCCESS_CODE) return
  if (body.code === UNAUTHORIZED_CODE) {
    redirectToLogin()
    throw new Error(body.msg || '登录已过期，请重新登录')
  }
  const msg = body.msg || `请求失败（code=${body.code}）`
  if (!silent) errorNotifier?.(msg)
  throw new Error(msg)
}

http.interceptors.response.use(
  async (response) => {
    if (
      response.config.responseType === 'stream' &&
      !String(response.headers['content-type']).includes('text/event-stream')
    ) {
      const body = (await new Response(response.data).json()) as Partial<CommonResult>
      assertResultSuccess(body, (response.config as RequestConfig).silent)
      throw new Error('服务器未返回问答流，请重试')
    }
    const body = response.data as Partial<CommonResult> | undefined
    // 统一按后端 `{code,msg,data}` 契约判定成败：非成功码提示并 reject。
    if (body) assertResultSuccess(body, (response.config as RequestConfig).silent)
    return response
  },
  (error: AxiosError<CommonResult>) => {
    if (error.response?.status === UNAUTHORIZED_CODE) {
      redirectToLogin()
      return Promise.reject(error)
    }
    const msg = error.response?.data?.msg || error.message || '网络异常，请稍后重试'
    if (!(error.config as RequestConfig | undefined)?.silent) {
      errorNotifier?.(msg)
    }
    return Promise.reject(new Error(msg))
  },
)

/**
 * 生成的 `api/*Controller.ts` 使用的请求函数（umi 风格入参）。
 * 契约判定已在拦截器完成，返回值即后端 `CommonResult` 全量，业务代码取 `.data`。
 */
export default function request<T>(url: string, options: RequestConfig = {}): Promise<T> {
  const { silent, ...config } = options
  // silent 需要透传进 axios config，拦截器据此判断是否弹全局错误提示。
  const requestConfig: RequestConfig = { url, ...config, silent }
  return http.request<T>(requestConfig).then((response) => response.data)
}
