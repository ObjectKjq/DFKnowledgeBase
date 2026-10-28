import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntdApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { useEffect, type ReactNode } from 'react'

import { queryClient } from '@/lib/queryClient'
import { setHttpErrorNotifier } from '@/lib/http'
import { antdTheme } from '@/theme/tokens'

/**
 * 把 antd 的 message 实例注入到 axios 拦截器，使接口错误提示吃到知序主题。
 * 暗色切换点预留在此处（`theme.darkAlgorithm`），本期只做亮色。
 */
function HttpErrorBridge() {
  const { message } = AntdApp.useApp()

  useEffect(() => {
    setHttpErrorNotifier((msg) => {
      void message.error(msg)
    })
  }, [message])

  return null
}

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider
        locale={zhCN}
        theme={antdTheme}
        select={{
          className: 'app-select',
          classNames: { popup: { root: 'app-select-popup' } },
        }}
      >
        <AntdApp>
          <HttpErrorBridge />
          {children}
        </AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  )
}
