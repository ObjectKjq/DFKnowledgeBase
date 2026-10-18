import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { TAB_CONFIGS } from '@/features/manage/tabs'
import { useAuthStore } from '@/stores/auth'

/** 路由守卫：无 token 跳登录页；管理页按权限串拦截，越权访问带回首页。 */
export function RequireAuth() {
  const token = useAuthStore((s) => s.token)
  const permissions = useAuthStore((s) => s.permissions)
  const location = useLocation()

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  const tab = location.pathname.match(/^\/settings\/([^/]+)/)?.[1]
  const required = TAB_CONFIGS.find((t) => t.key === tab)?.permission
  if (required && !permissions.includes(required)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
