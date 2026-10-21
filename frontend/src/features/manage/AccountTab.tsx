import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'

import { logout } from '@/api/authController'
import { getProfile, updateProfile, updateProfilePassword } from '@/api/userController'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

/** 账号 Tab：个人资料、修改密码与退出登录。 */
export function AccountTab() {
  const navigate = useNavigate()
  const clearSession = useAuthStore((s) => s.clearSession)
  const resetDemo = useKb((s) => s.resetDemo)

  const profile = useQuery({
    queryKey: ['manage-profile'],
    queryFn: async () => (await getProfile()).data,
  })

  const doLogout = () => {
    // 服务端登出尽力而为，本地会话一定清理。
    void logout().catch(() => undefined)
    clearSession()
    navigate('/login')
  }

  return (
    <>
      <div className="settings-card">
        <h3>个人资料</h3>
        {profile.data && <ProfileForm data={profile.data} />}
      </div>

      <div className="settings-card">
        <h3>修改密码</h3>
        <PasswordForm />
      </div>

      <div className="settings-card">
        <h3>关于本应用</h3>
        <p>文档上传、检索、会话和 RAG 问答由服务器处理。MCP 接入与模型管理页仍保留演示配置。</p>
        <div className="settings-pair">
          <span>
            交互数据
            <small>仅重置 MCP 与模型管理的本地演示配置，不影响服务器文档和会话。</small>
          </span>
          <button className="btn danger small" onClick={resetDemo}>
            重置演示
          </button>
        </div>
        <div className="settings-pair">
          <span>
            登录会话
            <small>退出后返回登录页，需要重新登录。</small>
          </span>
          <button className="btn ghost small" onClick={doLogout}>
            退出登录
          </button>
        </div>
      </div>
    </>
  )
}

/** 个人资料表单：昵称、头像与联系方式。 */
function ProfileForm({ data }: { data: API.UserRespVO }) {
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    nickname: data.nickname ?? '',
    avatar: data.avatar ?? '',
    email: data.email ?? '',
    mobile: data.mobile ?? '',
  })
  const [hint, setHint] = useState('')

  const saveMutation = useMutation({
    mutationFn: () =>
      updateProfile({
        nickname: form.nickname.trim(),
        avatar: form.avatar.trim(),
        email: form.email.trim(),
        mobile: form.mobile.trim(),
      }),
    onSuccess: () => {
      toastMsg('个人资料已保存')
      void queryClient.invalidateQueries({ queryKey: ['manage-profile'] })
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.nickname.trim()) {
      setHint('请输入用户昵称')
      return
    }
    setHint('')
    saveMutation.mutate()
  }

  return (
    <form onSubmit={submit}>
      <div className="settings-pair">
        <span>用户名</span>
        <span>{data.username}</span>
      </div>
      <label className="field">
        用户昵称
        <input
          value={form.nickname}
          onChange={(e) => setForm({ ...form, nickname: e.target.value })}
          placeholder="展示用的名称"
        />
      </label>
      <label className="field">
        头像地址 · 选填
        <input
          value={form.avatar}
          onChange={(e) => setForm({ ...form, avatar: e.target.value })}
          placeholder="图片 URL"
        />
      </label>
      <label className="field">
        邮箱 · 选填
        <input
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          placeholder="name@example.com"
        />
      </label>
      <label className="field">
        手机 · 选填
        <input
          value={form.mobile}
          onChange={(e) => setForm({ ...form, mobile: e.target.value })}
          placeholder="手机号码"
        />
      </label>
      {hint && <p className="field-hint">{hint}</p>}
      <div className="form-actions">
        <button className="btn primary">保存资料</button>
      </div>
    </form>
  )
}

/** 修改密码表单：校验旧密码，成功后强制重新登录。 */
function PasswordForm() {
  const navigate = useNavigate()
  const clearSession = useAuthStore((s) => s.clearSession)
  const [form, setForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [hint, setHint] = useState('')

  // 校验失败的提示内联展示在表单里，因此接口层传 silent 关闭全局弹错。
  const saveMutation = useMutation({
    mutationFn: () =>
      updateProfilePassword(
        { oldPassword: form.oldPassword, newPassword: form.newPassword },
        { silent: true },
      ),
    onSuccess: () => {
      clearSession()
      navigate('/login')
    },
    onError: (e) => setHint(e instanceof Error ? e.message : '修改密码失败，请稍后重试'),
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.oldPassword) {
      setHint('请输入旧密码')
      return
    }
    if (!form.newPassword) {
      setHint('请输入新密码')
      return
    }
    if (form.newPassword !== form.confirmPassword) {
      setHint('两次输入的新密码不一致')
      return
    }
    setHint('')
    saveMutation.mutate()
  }

  return (
    <form onSubmit={submit}>
      <label className="field">
        旧密码
        <input
          type="password"
          value={form.oldPassword}
          onChange={(e) => setForm({ ...form, oldPassword: e.target.value })}
          placeholder="输入当前密码"
          autoComplete="current-password"
        />
      </label>
      <label className="field">
        新密码
        <input
          type="password"
          value={form.newPassword}
          onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
          placeholder="输入新密码"
          autoComplete="new-password"
        />
      </label>
      <label className="field">
        确认新密码
        <input
          type="password"
          value={form.confirmPassword}
          onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
          placeholder="再次输入新密码"
          autoComplete="new-password"
        />
      </label>
      {hint && <p className="field-hint">{hint}</p>}
      <div className="form-actions">
        <button className="btn primary">修改密码</button>
      </div>
      <p className="table-note">修改成功后将退出登录，需使用新密码重新登录。</p>
    </form>
  )
}
