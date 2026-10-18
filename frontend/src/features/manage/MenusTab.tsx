import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'

import { createMenu, deleteMenu, getMenuList, updateMenu } from '@/api/menuController'
import { useAuthStore } from '@/stores/auth'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'
import { ModalShell } from '@/components/ModalHost'

import { MENU_TYPE_BUTTON, flattenMenuTree, menuTypeLabel } from './menuTree'

/** 菜单管理 Tab（最小实现）：菜单树与按钮权限点的列表、新增 / 编辑 / 删除。 */
export function MenusTab() {
  const permissions = useAuthStore((s) => s.permissions)
  const confirmThen = useKb((s) => s.confirmThen)
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()

  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<API.MenuRespVO | null>(null)

  const canList = permissions.includes('system:menu:list')
  const canCreate = permissions.includes('system:menu:create')
  const canUpdate = permissions.includes('system:menu:update')
  const canDelete = permissions.includes('system:menu:delete')

  const query = useQuery({
    queryKey: ['manage-menu-list'],
    queryFn: async () => (await getMenuList()).data ?? [],
    enabled: canList,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteMenu({ id }),
    onSuccess: () => {
      toastMsg('菜单已删除')
      void queryClient.invalidateQueries({ queryKey: ['manage-menu-list'] })
    },
  })

  const rows = flattenMenuTree(query.data ?? [])

  const remove = (m: API.MenuRespVO) => {
    confirmThen(
      '删除菜单',
      `「${m.name}」将被删除。若其下仍有子菜单，删除会被拒绝。`,
      '确认删除',
      () => deleteMutation.mutate({ id: m.id! }),
    )
  }

  return (
    <>
      <div className="manage-toolbar">
        <div className="manage-toolbar-left">
          <span className="table-note">目录、菜单与按钮权限点构成一棵权限树。</span>
        </div>
        {canCreate && (
          <button className="btn primary" onClick={() => setCreating(true)}>
            <Icon name="plus" />
            新增菜单
          </button>
        )}
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>菜单名称</th>
              <th>类型</th>
              <th>权限标识</th>
              <th>路由</th>
              <th>排序</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(({ menu, depth }) => (
              <tr key={menu.id}>
                <td>
                  <span style={{ paddingLeft: depth * 18 }}>{menu.name}</span>
                </td>
                <td>
                  <span className={`tag ${menu.type === MENU_TYPE_BUTTON ? 'oat' : ''}`}>
                    {menuTypeLabel(menu.type)}
                  </span>
                </td>
                <td>{menu.permission || '—'}</td>
                <td>{menu.path || '—'}</td>
                <td>{menu.sort}</td>
                <td>
                  <span className="table-actions">
                    {canUpdate && <button onClick={() => setEditing(menu)}>编辑</button>}
                    {canDelete && <button onClick={() => remove(menu)}>删除</button>}
                  </span>
                </td>
              </tr>
            ))}
            {!rows.length && (
              <tr>
                <td colSpan={6} className="empty-cell">
                  这里还没有菜单
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p className="table-note">
        按钮权限点挂在所属菜单下，权限标识格式为 system:资源:操作，与后端鉴权注解一一对应。
      </p>

      {(creating || editing) && (
        <MenuFormModal
          menu={editing}
          all={query.data ?? []}
          onClose={() => {
            setCreating(false)
            setEditing(null)
          }}
        />
      )}
    </>
  )
}

/** 新增 / 编辑菜单弹窗。 */
function MenuFormModal({
  menu,
  all,
  onClose,
}: {
  menu: API.MenuRespVO | null
  all: API.MenuRespVO[]
  onClose: () => void
}) {
  const isCreate = !menu?.id
  const toastMsg = useKb((s) => s.toastMsg)
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    name: menu?.name ?? '',
    type: menu?.type ?? 2,
    parentId: menu?.parentId ?? 0,
    permission: menu?.permission ?? '',
    path: menu?.path ?? '',
    sort: menu?.sort ?? 1,
    status: menu?.status ?? 0,
  })
  const [hint, setHint] = useState('')

  const parents = all.filter((m) => m.type !== MENU_TYPE_BUTTON && m.id !== menu?.id)

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        name: form.name.trim(),
        type: form.type,
        parentId: form.parentId,
        permission: form.permission.trim(),
        path: form.path.trim(),
        sort: form.sort,
        status: form.status,
      }
      return isCreate ? createMenu(payload) : updateMenu({ id: menu!.id!, ...payload })
    },
    onSuccess: () => {
      toastMsg(isCreate ? '菜单已创建' : '菜单已保存')
      void queryClient.invalidateQueries({ queryKey: ['manage-menu-list'] })
      onClose()
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.name.trim()) {
      setHint('请输入菜单名称')
      return
    }
    if (form.type === MENU_TYPE_BUTTON && !form.permission.trim()) {
      setHint('按钮类型需填写权限标识')
      return
    }
    setHint('')
    saveMutation.mutate()
  }

  return (
    <ModalShell
      title={isCreate ? '新增菜单' : '编辑菜单'}
      description="目录与菜单进入侧边栏，按钮作为权限点参与鉴权。"
      onClose={onClose}
    >
      <form onSubmit={submit}>
        <label className="field">
          菜单名称
          <input
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="例如：用户管理"
            autoFocus
          />
        </label>
        <label className="field">
          菜单类型
          <select
            value={form.type}
            onChange={(e) => setForm({ ...form, type: Number(e.target.value) })}
          >
            <option value={1}>目录</option>
            <option value={2}>菜单</option>
            <option value={3}>按钮</option>
          </select>
        </label>
        <label className="field">
          上级菜单
          <select
            value={form.parentId}
            onChange={(e) => setForm({ ...form, parentId: Number(e.target.value) })}
          >
            <option value={0}>根目录</option>
            {parents.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          权限标识{form.type === MENU_TYPE_BUTTON ? '' : ' · 选填'}
          <input
            value={form.permission}
            onChange={(e) => setForm({ ...form, permission: e.target.value })}
            placeholder="如 system:user:list"
          />
        </label>
        <label className="field">
          路由地址 · 选填
          <input
            value={form.path}
            onChange={(e) => setForm({ ...form, path: e.target.value })}
            placeholder="如 /system/user"
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
          菜单状态
          <select
            value={form.status}
            onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}
          >
            <option value={0}>开启</option>
            <option value={1}>停用</option>
          </select>
        </label>
        {hint && <p className="field-hint">{hint}</p>}
        <div className="form-actions">
          <button type="button" className="btn ghost" onClick={onClose}>
            取消
          </button>
          <button className="btn primary">{isCreate ? '创建菜单' : '保存修改'}</button>
        </div>
      </form>
    </ModalShell>
  )
}
