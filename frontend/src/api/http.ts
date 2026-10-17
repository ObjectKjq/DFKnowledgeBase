import axios, { type AxiosError, type AxiosRequestConfig, type AxiosInstance } from 'axios'

import { useAuthStore } from '@/stores/auth'

import { SUCCESS_CODE, type CommonResult } from './types'

/**
 * 错误提示的注入点：由 `app/providers` 把 antd 的 message 实例注册进来，
 * 这样拦截器里的提示能吃到 ConfigProvider 的知序主题（静态 message 方法读不到主题）。
 */
let errorNotifier: ((msg: string) => void) | null = null

export function setHttpErrorNotifier(notifier: (msg: string) => void) {
  errorNotifier = notifier
}

/** 统一 HTTP 出口：baseURL `/api`（开发期由 Vite dev server 代理到 8080）。 */
export const http: AxiosInstance = axios.create({
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

http.interceptors.response.use(
  (response) => {
    const body = response.data as CommonResult | undefined
    // 统一按后端 `{code,msg,data}` 契约判定成败：非成功码提示并 reject。
    if (body && typeof body.code === 'number' && body.code !== SUCCESS_CODE) {
      const msg = body.msg || `请求失败（code=${body.code}）`
      errorNotifier?.(msg)
      return Promise.reject(new Error(msg))
    }
    return response
  },
  (error: AxiosError<CommonResult>) => {
    const msg = error.response?.data?.msg || error.message || '网络异常，请稍后重试'
    errorNotifier?.(msg)
    return Promise.reject(error)
  },
)

/** 已确认成功（拦截器已挡掉失败），返回值即 `CommonResult.data`。 */
export async function apiRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<CommonResult<T>>(config)
  return response.data.data
}
