import { QueryClient } from '@tanstack/react-query'

/** 应用共用的服务端缓存，供组件与会话动作读取。 */
export const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})
