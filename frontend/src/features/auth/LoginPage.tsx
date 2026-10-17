import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'

import { useAuthStore } from '@/stores/auth'

import { Icon } from '@/components/Icon'

/**
 * 登录页：本期无真实鉴权接口，任意非空用户名/密码即写入本地会话。
 * 视觉沿用原型「知序」的米白纸感与苔绿点缀，与主界面保持同一套设计语言。
 */
export function LoginPage() {
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名与密码')
      return
    }
    setSession('dev-token', { id: 1, username: username.trim() })
    navigate('/', { replace: true })
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
          <span>
            当前为交互演示版本，任意非空用户名与密码即可进入。数据仅保存在你的浏览器中，不会发送到服务器。
          </span>
        </div>
      </div>
    </div>
  )
}
