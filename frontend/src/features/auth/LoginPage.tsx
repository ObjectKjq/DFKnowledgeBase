import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { getPermissionInfo, login } from '@/api/authController'
import { useAuthStore } from '@/stores/auth'

import { Icon } from '@/components/Icon'

/**
 * 登录页：真实账号密码登录（sa-token 令牌），成功后拉取权限信息并进入平台。
 * 视觉沿用原型「知序」的米白纸感与苔绿点缀，与主界面保持同一套设计语言。
 */
export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setToken = useAuthStore((s) => s.setToken)
  const setPermissionInfo = useAuthStore((s) => s.setPermissionInfo)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const from = (location.state as { from?: string } | null)?.from ?? '/'

  // 登录失败的提示内联展示在表单里，因此接口层传 silent 关闭全局弹错。
  const loginMutation = useMutation({
    mutationFn: async (values: { username: string; password: string }) => {
      const { data } = await login(values, { silent: true })
      if (!data?.token) throw new Error('登录失败，请稍后重试')
      setToken(data.token)
      const { data: info } = await getPermissionInfo({ silent: true })
      if (!info) throw new Error('获取权限信息失败，请稍后重试')
      return info
    },
    onSuccess: (info) => {
      setPermissionInfo(info)
      navigate(from, { replace: true })
    },
    onError: (e) => setError(e instanceof Error ? e.message : '登录失败，请稍后重试'),
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名与密码')
      return
    }
    loginMutation.mutate({ username: username.trim(), password })
  }

  return (
    <div className="login-page">
      <div className="login-panel">
        <div className="login-brand">
          <svg className="brand-mark" viewBox="0 0 36 40" aria-hidden="true">
            <path
              d="M17 32V12C11 7 5 8 3 9v21c6-1 11 0 14 3 4-3 9-4 15-3V9c-3-1-9-1-15 3"
              strokeWidth="1.5"
            />
            <path
              d="M20 8c0-5 5-7 9-6-1 5-4 8-9 6ZM9 16l4 2m-4 3 4 2m9-7 5-1m-5 6 5-1"
              strokeWidth="1.2"
            />
          </svg>
          <div>
            <div className="brand-name">知序</div>
            <div className="brand-en">KNOWLEDGE, AT HOME.</div>
          </div>
        </div>

        <div className="login-hero">
          <p className="eyebrow">A QUIET PLACE FOR YOUR KNOWLEDGE</p>
          <h1>
            让知识，<span>重新相遇。</span>
          </h1>
          <p className="login-description">
            资料存于你的环境，灵感随时可寻。
            <br />
            登录后，继续与积累的知识对话。
          </p>
        </div>

        <form className="login-card" onSubmit={submit}>
          <label className="field">
            用户名
            <input
              value={username}
              onChange={(e) => {
                setUsername(e.target.value)
                setError('')
              }}
              placeholder="输入你的用户名"
              autoComplete="username"
              autoFocus
            />
          </label>
          <label className="field">
            密码
            <input
              type="password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                setError('')
              }}
              placeholder="输入你的密码"
              autoComplete="current-password"
            />
          </label>
          {error && <p className="login-error">{error}</p>}
          <button type="submit" className="btn primary login-submit">
            进入知识空间
            <Icon name="arrow" />
          </button>
        </form>

        <div className="login-note">
          <Icon name="shield" />
          <span>请使用实例管理员为你创建的账号登录。忘记密码可联系管理员重置。</span>
        </div>
      </div>
    </div>
  )
}
