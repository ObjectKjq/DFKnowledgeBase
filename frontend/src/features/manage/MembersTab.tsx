import { useEffect } from 'react'

import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

/** 共享成员 Tab（复刻原型 membersHTML）。 */
export function MembersTab() {
  const libs = useKb((s) => s.libs)
  const members = useKb((s) => s.members)
  const memberLib = useKb((s) => s.memberLib)
  const setMemberLib = useKb((s) => s.setMemberLib)
  const setMemberRole = useKb((s) => s.setMemberRole)
  const removeMember = useKb((s) => s.removeMember)
  const openModal = useKb((s) => s.openModal)
  const deleteLibrary = useKb((s) => s.deleteLibrary)

  const own = libs.filter((l) => l.type === 'shared' && l.role === '所有者')
  const currentLib = own.some((l) => l.id === memberLib) ? memberLib : own[0]?.id

  useEffect(() => {
    if (own.length && currentLib && currentLib !== memberLib) setMemberLib(currentLib)
  }, [own.length, currentLib, memberLib, setMemberLib])

  if (!own.length) {
    return (
      <div className="empty-result">
        <h3>还没有可管理的共享知识库</h3>
        <button className="btn" onClick={() => openModal({ kind: 'library' })}>
          新建共享知识库
        </button>
      </div>
    )
  }

  return (
    <>
      <div className="manage-toolbar">
        <div className="manage-toolbar-left">
          <select
            className="select"
            id="member-lib"
            aria-label="选择共享知识库"
            value={currentLib}
            onChange={(e) => setMemberLib(e.target.value)}
          >
            {own.map((l) => (
              <option key={l.id} value={l.id}>
                {l.name}
              </option>
            ))}
          </select>
          <span className="tag">所有者</span>
        </div>
        <button className="btn primary" onClick={() => openModal({ kind: 'invite' })}>
          <Icon name="users" />
          添加演示成员
        </button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>成员</th>
              <th>角色</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {members
              .filter((m) => m.lib === currentLib)
              .map((m) => (
                <tr key={m.name}>
                  <td>
                    {m.name}
                    {m.name === '林予' ? ' · 你' : ''}
                  </td>
                  <td>
                    {m.role === '所有者' ? (
                      '所有者'
                    ) : (
                      <select
                        className="select"
                        aria-label={`${m.name}的角色`}
                        value={m.role}
                        onChange={(e) => setMemberRole(m.name, e.target.value)}
                      >
                        <option>编辑者</option>
                        <option>查看者</option>
                      </select>
                    )}
                  </td>
                  <td>
                    {m.role !== '所有者' ? (
                      <button className="btn small ghost" onClick={() => removeMember(m.name)}>
                        移除
                      </button>
                    ) : (
                      '—'
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      <p className="table-note">
        查看者仅可检索与阅读；编辑者可上传、删除、重试文档。成员变更同步影响 MCP 可访问范围。
      </p>

      <div className="form-actions">
        <button className="btn ghost" onClick={() => openModal({ kind: 'library' })}>
          <Icon name="plus" />
          新建共享知识库
        </button>
        <button className="btn danger" onClick={() => deleteLibrary(currentLib!)}>
          删除当前共享库
        </button>
      </div>
    </>
  )
}
