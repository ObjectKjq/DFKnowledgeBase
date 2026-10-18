import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  /** 登录用户信息（get-permission-info 返回） */
  user: API.UserVO | null
  /** 角色标识列表 */
  roles: string[]
  /** 权限标识列表（按钮权限点，如 system:user:create） */
  permissions: string[]
  /** 菜单树（目录/菜单，不含按钮），驱动侧边栏与页签显隐 */
  menus: API.MenuVO[]
  /** 登录成功后写入访问令牌，后续请求由拦截器自动携带。 */
  setToken: (token: string) => void
  /** 写入 get-permission-info 返回的用户信息、角色、权限与菜单树。 */
  setPermissionInfo: (info: API.AuthPermissionInfoRespVO) => void
  /** 退出登录 / 令牌失效：清空本地会话，路由守卫据此跳回登录页。 */
  clearSession: () => void
}

/** 登录态存 localStorage（persist），刷新后仍保持。 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      roles: [],
      permissions: [],
      menus: [],
      setToken: (token) => set({ token }),
      setPermissionInfo: (info) =>
        set({
          user: info.user ?? null,
          roles: info.roles ?? [],
          permissions: info.permissions ?? [],
          menus: info.menus ?? [],
        }),
      clearSession: () => set({ token: null, user: null, roles: [], permissions: [], menus: [] }),
    }),
    { name: 'df-kb-auth' },
  ),
)

/** 菜单树中是否包含指定路由地址的菜单（用于页签 / 侧边栏入口显隐）。 */
export function hasMenuPath(menus: API.MenuVO[], path?: string): boolean {
  if (!path) return true
  return menus.some((m) => m.path === path || hasMenuPath((m.children ?? []) as API.MenuVO[], path))
}
