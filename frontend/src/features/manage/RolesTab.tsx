import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState, type FormEvent } from 'react'
import { Select } from 'antd'

import { getMenuList } from '@/api/menuController'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getRoleMenuIdList,
  getRolePage,
  updateRole,
} from '@/api/roleController'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'
import { ModalShell } from '@/components/ModalHost'

import { flattenMenuTree } from './menuTree'

const PAGE_SIZE = 10

/** 角色管理 Tab：分页检索、维护角色、勾选分配菜单权限。 */
export function RolesTab() {
  const permissions = useAuthStore((s) => s.permissions)
  const confirmThen = useKb((s) => s.confirmThen)
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()

  const [pageNum, setPageNum] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [name, setName] = useState('')
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<API.RoleRespVO | null>(null)
  const [assigning, setAssigning] = useState<API.RoleRespVO | null>(null)

  const canList = permissions.includes('system:role:list')
  const canCreate = permissions.includes('system:role:create')
  const canUpdate = permissions.includes('system:role:update')
  const canDelete = permissions.includes('system:role:delete')
  const canAssign = permissions.includes('system:role:assign-menu')

  const query = useQuery({
    queryKey: ['manage-roles', pageNum, name],
    queryFn: async () => {
      const { data } = await getRolePage({
        reqVO: { pageNum, pageSize: PAGE_SIZE, name: name || undefined },
      })
      return data ?? { total: 0, list: [] }
    },
    enabled: canList,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteRole({ id }),
    onSuccess: () => {
      toastMsg('角色已删除')
      void queryClient.invalidateQueries({ queryKey: ['manage-roles'] })
    },
  })

  const rows = query.data?.list ?? []
  const total = query.data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const remove = (r: API.RoleRespVO) => {
    confirmThen(
      '删除角色',
      `「${r.name}」将被删除，此操作不可恢复。若仍有用户使用该角色，删除会被拒绝。`,
      '确认删除',
      () => deleteMutation.mutate(r.id!),
    )
  }

  return (
    <>
      <div className="manage-toolbar">
        <div className="manage-toolbar-left">
          <label className="field-search">
            <Icon name="search" />
            <input
              aria-label="搜索角色"
              placeholder="搜索角色名称"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setPageNum(1)
                  setName(keyword.trim())
                }
              }}
            />
          </label>
          <button
            className="btn ghost"
            onClick={() => {
              setPageNum(1)
              setName(keyword.trim())
            }}
          >
            查询
          </button>
        </div>
        {canCreate && (
          <button className="btn primary" onClick={() => setCreating(true)}>
            <Icon name="plus" />
            新增角色
          </button>
        )}
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>角色名称</th>
              <th>角色标识</th>
              <th>状态</th>
              <th>显示顺序</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>{r.name}</td>
                <td>{r.code}</td>
                <td>
                  <span className={`tag ${r.status === 1 ? 'red' : ''}`}>
                    {r.status === 1 ? '停用' : '开启'}
                  </span>
                </td>
                <td>{r.sort}</td>
                <td>{r.remark || '—'}</td>
                <td>
                  <span className="table-actions">
                    {canUpdate && <button onClick={() => setEditing(r)}>编辑</button>}
                    {canAssign && <button onClick={() => setAssigning(r)}>分配菜单</button>}
                    {canDelete && r.type !== 1 && <button onClick={() => remove(r)}>删除</button>}
                  </span>
                </td>
              </tr>
            ))}
            {!rows.length && (
              <tr>
                <td colSpan={6} className="empty-cell">
                  这里还没有符合条件的角色
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
        <RoleFormModal
          role={editing}
          onClose={() => {
            setCreating(false)
            setEditing(null)
          }}
        />
      )}
      {assigning && <AssignMenuModal role={assigning} onClose={() => setAssigning(null)} />}
    </>
  )
}

/** 新增 / 编辑角色弹窗。 */
function RoleFormModal({ role, onClose }: { role: API.RoleRespVO | null; onClose: () => void }) {
  const isCreate = !role?.id
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    name: role?.name ?? '',
    code: role?.code ?? '',
    sort: role?.sort ?? 1,
    status: role?.status ?? 0,
    remark: role?.remark ?? '',
  })
  const [hint, setHint] = useState('')

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = {
        name: form.name.trim(),
        code: form.code.trim(),
        sort: form.sort,
        status: form.status,
        remark: form.remark.trim(),
      }
      return isCreate ? createRole(payload) : updateRole({ id: role!.id!, ...payload })
    },
    onSuccess: () => {
      toastMsg(isCreate ? '角色已创建' : '角色已保存')
      void queryClient.invalidateQueries({ queryKey: ['manage-roles'] })
      onClose()
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.name.trim()) {
      setHint('请输入角色名称')
      return
    }
    if (!form.code.trim()) {
      setHint('请输入角色标识')
      return
    }
    setHint('')
    saveMutation.mutate()
  }

  return (
    <ModalShell
      title={isCreate ? '新增角色' : '编辑角色'}
      description="角色是功能权限的打包载体，创建后再为其分配菜单权限。"
      onClose={onClose}
    >
      <form onSubmit={submit}>
        <label className="field">
          角色名称
          <input
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="例如：内容编辑"
            autoFocus
          />
        </label>
        <label className="field">
          角色标识
          <input
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })}
            placeholder="英文标识，如 editor"
          />
        </label>
        <label className="field">
          显示顺序
          <input
            type="number"
            value={form.sort}
            onChange={(e) => setForm({ ...form, sort: Number(e.target.value) })}
          />
        </label>
        <label className="field">
          角色状态
          <Select
            aria-label="角色状态"
            value={form.status}
            onChange={(status) => setForm({ ...form, status })}
            options={[
              { value: 0, label: '开启' },
              { value: 1, label: '停用' },
            ]}
          />
        </label>
        <label className="field">
          备注 · 选填
          <input
            value={form.remark}
            onChange={(e) => setForm({ ...form, remark: e.target.value })}
            placeholder="这个角色负责什么"
          />
        </label>
        {hint && <p className="field-hint">{hint}</p>}
        <div className="form-actions">
          <button type="button" className="btn ghost" onClick={onClose}>
            取消
          </button>
          <button className="btn primary">{isCreate ? '创建角色' : '保存修改'}</button>
        </div>
      </form>
    </ModalShell>
  )
}

/** 分配菜单弹窗：勾选该角色可访问的菜单与按钮权限点，整体覆盖保存。 */
function AssignMenuModal({ role, onClose }: { role: API.RoleRespVO; onClose: () => void }) {
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()

  const menus = useQuery({
    queryKey: ['manage-menu-list'],
    queryFn: async () => (await getMenuList()).data ?? [],
  })
  const owned = useQuery({
    queryKey: ['manage-role-menus', role.id],
    queryFn: async () => (await getRoleMenuIdList({ roleId: role.id! })).data ?? [],
  })

  const [selection, setChecked] = useState<number[] | null>(null)
  const checked = selection ?? owned.data ?? []

  const rows = useMemo(() => flattenMenuTree(menus.data ?? []), [menus.data])

  const saveMutation = useMutation({
    mutationFn: () => assignRoleMenus({ roleId: role.id!, menuIds: checked }),
    onSuccess: () => {
      toastMsg('菜单权限已保存')
      void queryClient.invalidateQueries({ queryKey: ['manage-role-menus', role.id] })
      onClose()
    },
  })

  return (
    <ModalShell
      title="分配菜单"
      description={`为「${role.name}」勾选可访问的菜单与按钮权限点，保存后立即生效。`}
      onClose={onClose}
    >
      <div className="check-list">
        {rows.map(({ menu, depth }) => (
          <label key={menu.id} style={{ paddingLeft: depth * 18 }}>
            <input
              type="checkbox"
              checked={checked.includes(menu.id!)}
              onChange={(e) =>
                setChecked(
                  e.target.checked
                    ? [...checked, menu.id!]
                    : checked.filter((id) => id !== menu.id),
                )
              }
            />
            {menu.name}
            <span className="field-hint">{menu.permission || menu.path}</span>
          </label>
        ))}
        {!rows.length && <span className="field-hint">暂无菜单，请先在菜单管理中维护</span>}
      </div>
      <div className="form-actions">
        <button type="button" className="btn ghost" onClick={onClose}>
          取消
        </button>
        <button className="btn primary" onClick={() => saveMutation.mutate()}>
          保存权限
        </button>
      </div>
    </ModalShell>
  )
}
