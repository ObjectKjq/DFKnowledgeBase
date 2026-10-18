import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'

import { getSimpleRoleList } from '@/api/roleController'
import {
  createUser,
  deleteUser,
  getUser,
  getUserPage,
  resetUserPassword,
  updateUser,
  updateUserStatus,
} from '@/api/userController'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'
import { ModalShell } from '@/components/ModalHost'

const PAGE_SIZE = 10

/** 用户管理 Tab：分页检索、创建 / 编辑（含角色分配）、停用启用、重置密码与删除。 */
export function UsersTab() {
  const permissions = useAuthStore((s) => s.permissions)
  const confirmThen = useKb((s) => s.confirmThen)
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()

  const [pageNum, setPageNum] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [filters, setFilters] = useState<{ username?: string; status?: number }>({})
  const [editing, setEditing] = useState<API.UserRespVO | null>(null)
  const [creating, setCreating] = useState(false)
  const [resetting, setResetting] = useState<API.UserRespVO | null>(null)

  const canList = permissions.includes('system:user:list')
  const canCreate = permissions.includes('system:user:create')
  const canUpdate = permissions.includes('system:user:update')
  const canDelete = permissions.includes('system:user:delete')
  const canReset = permissions.includes('system:user:reset-password')
  const canUpdateStatus = permissions.includes('system:user:update-status')

  const query = useQuery({
    queryKey: ['manage-users', pageNum, filters],
    queryFn: async () => {
      const { data } = await getUserPage({
        reqVO: { pageNum, pageSize: PAGE_SIZE, ...filters },
      })
      return data ?? { total: 0, list: [] }
    },
    enabled: canList,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['manage-users'] })

  const statusMutation = useMutation({
    mutationFn: (values: API.UserUpdateStatusReqVO) => updateUserStatus(values),
    onSuccess: () => {
      toastMsg('用户状态已更新')
      void invalidate()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteUser({ id }),
    onSuccess: () => {
      toastMsg('用户已删除')
      void invalidate()
    },
  })

  const rows = query.data?.list ?? []
  const total = query.data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const search = () => {
    setPageNum(1)
    setFilters({
      username: keyword.trim() || undefined,
      status: statusFilter === 'all' ? undefined : Number(statusFilter),
    })
  }

  const toggleStatus = (u: API.UserRespVO) => {
    const next = u.status === 0 ? 1 : 0
    confirmThen(
      next === 1 ? '停用用户' : '启用用户',
      next === 1
        ? `停用「${u.username}」后将强制其下线，无法继续使用平台。`
        : `启用「${u.username}」后其可重新登录使用平台。`,
      next === 1 ? '确认停用' : '确认启用',
      () => statusMutation.mutate({ id: u.id!, status: next }),
    )
  }

  const remove = (u: API.UserRespVO) => {
    confirmThen('删除用户', `「${u.username}」将被删除，此操作不可恢复。`, '确认删除', () =>
      deleteMutation.mutate({ id: u.id! }),
    )
  }

  return (
    <>
      <div className="manage-toolbar">
        <div className="manage-toolbar-left">
          <label className="field-search">
            <Icon name="search" />
            <input
              aria-label="搜索用户"
              placeholder="搜索用户名"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && search()}
            />
          </label>
          <select
            className="select"
            aria-label="用户状态"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="all">全部状态</option>
            <option value="0">开启</option>
            <option value="1">停用</option>
          </select>
          <button className="btn ghost" onClick={search}>
            查询
          </button>
        </div>
        {canCreate && (
          <button className="btn primary" onClick={() => setCreating(true)}>
            <Icon name="plus" />
            新增用户
          </button>
        )}
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户名</th>
              <th>用户昵称</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.nickname}</td>
                <td>
                  <span className={`tag ${u.status === 1 ? 'red' : ''}`}>
                    {u.status === 1 ? '停用' : '开启'}
                  </span>
                </td>
                <td>{u.createTime?.slice(0, 10)}</td>
                <td>
                  <span className="table-actions">
                    {canUpdate && <button onClick={() => setEditing(u)}>编辑</button>}
                    {canUpdateStatus && (
                      <button onClick={() => toggleStatus(u)}>
                        {u.status === 1 ? '启用' : '停用'}
                      </button>
                    )}
                    {canReset && <button onClick={() => setResetting(u)}>重置密码</button>}
                    {canDelete && <button onClick={() => remove(u)}>删除</button>}
                  </span>
                </td>
              </tr>
            ))}
            {!rows.length && (
              <tr>
                <td colSpan={5} className="empty-cell">
                  这里还没有符合条件的用户
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="pager">
        <span>共 {total} 条</span>
        <button disabled={pageNum <= 1} onClick={() => setPageNum((p) => p - 1)}>
          上一页
        </button>
        <span>
          {pageNum} / {pageCount}
        </span>
        <button disabled={pageNum >= pageCount} onClick={() => setPageNum((p) => p + 1)}>
          下一页
        </button>
      </div>

      {(creating || editing) && (
        <UserFormModal
          user={editing}
          onClose={() => {
            setCreating(false)
            setEditing(null)
          }}
        />
      )}
      {resetting && <ResetPasswordModal user={resetting} onClose={() => setResetting(null)} />}
    </>
  )
}

/** 新增 / 编辑用户弹窗：资料、状态与角色分配。 */
function UserFormModal({ user, onClose }: { user: API.UserRespVO | null; onClose: () => void }) {
  const isCreate = !user?.id
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()

  const detail = useQuery({
    queryKey: ['manage-user', user?.id],
    queryFn: async () => (await getUser({ id: user!.id! })).data,
    enabled: !isCreate,
  })
  const roles = useQuery({
    queryKey: ['manage-roles-simple'],
    queryFn: async () => (await getSimpleRoleList()).data ?? [],
  })

  const [form, setForm] = useState({
    username: '',
    password: '',
    nickname: '',
    avatar: '',
    email: '',
    mobile: '',
    status: 0,
    remark: '',
  })
  const [roleIds, setRoleIds] = useState<number[]>([])
  const [hint, setHint] = useState('')

  useEffect(() => {
    if (!detail.data) return
    setForm({
      username: detail.data.username ?? '',
      password: '',
      nickname: detail.data.nickname ?? '',
      avatar: detail.data.avatar ?? '',
      email: detail.data.email ?? '',
      mobile: detail.data.mobile ?? '',
      status: detail.data.status ?? 0,
      remark: detail.data.remark ?? '',
    })
    setRoleIds(detail.data.roleIds ?? [])
  }, [detail.data])

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        nickname: form.nickname.trim(),
        avatar: form.avatar.trim(),
        email: form.email.trim(),
        mobile: form.mobile.trim(),
        status: form.status,
        remark: form.remark.trim(),
        roleIds,
      }
      return isCreate
        ? createUser({
            username: form.username.trim(),
            password: form.password,
            ...payload,
          })
        : updateUser({ id: user!.id!, ...payload })
    },
    onSuccess: () => {
      toastMsg(isCreate ? '用户已创建' : '用户已保存')
      void queryClient.invalidateQueries({ queryKey: ['manage-users'] })
      onClose()
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.username.trim()) {
      setHint('请输入用户名')
      return
    }
    if (isCreate && !form.password) {
      setHint('请输入初始密码')
      return
    }
    if (!form.nickname.trim()) {
      setHint('请输入用户昵称')
      return
    }
    setHint('')
    saveMutation.mutate()
  }

  return (
    <ModalShell
      title={isCreate ? '新增用户' : '编辑用户'}
      description="用户名用于登录平台；角色决定其可用的功能范围。"
      onClose={onClose}
    >
      <form onSubmit={submit}>
        <label className="field">
          用户名
          <input
            value={form.username}
            disabled={!isCreate}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            placeholder="登录用的唯一用户名"
            autoFocus
          />
        </label>
        {isCreate && (
          <label className="field">
            初始密码
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="用户首次登录的密码"
            />
          </label>
        )}
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
        <label className="field">
          用户状态
          <select
            value={form.status}
            onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}
          >
            <option value={0}>开启</option>
            <option value={1}>停用</option>
          </select>
        </label>
        <div className="field">
          角色
          <div className="check-list">
            {(roles.data ?? []).map((r) => (
              <label key={r.id}>
                <input
                  type="checkbox"
                  checked={roleIds.includes(r.id!)}
                  onChange={(e) =>
                    setRoleIds(
                      e.target.checked ? [...roleIds, r.id!] : roleIds.filter((id) => id !== r.id),
                    )
                  }
                />
                {r.name}
              </label>
            ))}
            {!roles.data?.length && <span className="field-hint">暂无可分配的角色</span>}
          </div>
        </div>
        {hint && <p className="field-hint">{hint}</p>}
        <div className="form-actions">
          <button type="button" className="btn ghost" onClick={onClose}>
            取消
          </button>
          <button className="btn primary">{isCreate ? '创建用户' : '保存修改'}</button>
        </div>
      </form>
    </ModalShell>
  )
}

/** 重置密码弹窗：管理员直接设置新密码，重置后强制该用户下线。 */
function ResetPasswordModal({ user, onClose }: { user: API.UserRespVO; onClose: () => void }) {
  const toastMsg = useKb((s) => s.toastMsg)
  const [password, setPassword] = useState('')
  const [hint, setHint] = useState('')

  const resetMutation = useMutation({
    mutationFn: () => resetUserPassword({ id: user.id!, password }),
    onSuccess: () => {
      toastMsg('密码已重置，该用户需使用新密码重新登录')
      onClose()
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!password) {
      setHint('请输入新密码')
      return
    }
    setHint('')
    resetMutation.mutate()
  }

  return (
    <ModalShell
      title="重置密码"
      description={`为「${user.username}」设置新密码。重置后该用户将被强制下线，需使用新密码重新登录。`}
      onClose={onClose}
    >
      <form onSubmit={submit}>
        <label className="field">
          新密码
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="输入新密码"
            autoFocus
          />
        </label>
        {hint && <p className="field-hint">{hint}</p>}
        <div className="form-actions">
          <button type="button" className="btn ghost" onClick={onClose}>
            取消
          </button>
          <button className="btn danger">确认重置</button>
        </div>
      </form>
    </ModalShell>
  )
}
