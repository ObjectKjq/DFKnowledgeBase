import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface AuthUser {
  id: number
  username: string
  nickname?: string
}

interface AuthState {
  token: string | null
  user: AuthUser | null
  /** 登录成功后写入（本期为守卫桩，无真实登录接口）。 */
  setSession: (token: string, user: AuthUser | null) => void
  /** 退出登录：清空本地会话，路由守卫据此跳回登录页。 */
  clearSession: () => void
}

/** 登录态存 localStorage（persist），刷新后仍保持。 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setSession: (token, user) => set({ token, user }),
      clearSession: () => set({ token: null, user: null }),
    }),
    { name: 'df-kb-auth' },
  ),
)
