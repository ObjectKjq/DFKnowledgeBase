import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName, writable } from '@/mock/logic'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

/** 文档管理 Tab（复刻原型 docsHTML）。 */
export function DocsTab() {
  const docs = useKb((s) => s.docs)
  const { libs } = useLibraries()
  const docFilter = useKb((s) => s.docFilter)
  const docSearch = useKb((s) => s.docSearch)
  const setDocFilter = useKb((s) => s.setDocFilter)
  const setDocSearch = useKb((s) => s.setDocSearch)
  const openUpload = useKb((s) => s.openUpload)
  const showPreview = useKb((s) => s.showPreview)
  const deleteDoc = useKb((s) => s.deleteDoc)
  const restoreDoc = useKb((s) => s.restoreDoc)
  const purgeDoc = useKb((s) => s.purgeDoc)
  const retryDoc = useKb((s) => s.retryDoc)

  const filtered = docs
    .filter((d) => libs.some((library) => library.id === d.lib))
    .filter((d) =>
      docFilter === 'deleted'
        ? d.status === '已删除'
        : d.status !== '已删除' && (docFilter === 'all' || d.status === docFilter),
    )
    .filter((d) => (d.title + libName(libs, d.lib)).toLowerCase().includes(docSearch.toLowerCase()))

  return (
    <>
      <div className="manage-toolbar">
        <div className="manage-toolbar-left">
          <label className="field-search">
            <Icon name="search" />
            <input
              id="doc-search"
              aria-label="搜索文档"
              placeholder="搜索文档或知识库"
              value={docSearch}
              onChange={(e) => setDocSearch(e.target.value)}
            />
          </label>
          <select
            className="select"
            id="doc-filter"
            aria-label="文档状态"
            value={docFilter}
            onChange={(e) => setDocFilter(e.target.value)}
          >
            <option value="all">全部文档</option>
            {['可用', '失败', '排队', '解析中', '索引中'].map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
            <option value="deleted">回收站</option>
          </select>
        </div>
        <button className="btn primary" onClick={openUpload}>
          <Icon name="upload" />
          上传资料
        </button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>文档名称</th>
              <th>所属知识库</th>
              <th>状态</th>
              <th>上传时间</th>
              <th>大小</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((d) => (
              <tr key={d.id}>
                <td>
                  <button className="table-title" onClick={() => showPreview(d.id)}>
                    <Icon name={d.ext === 'png' ? 'image' : 'file'} />
                    {d.title}.{d.ext}
                  </button>
                  {d.status === '失败' && (
                    <div
                      className="field-hint"
                      style={{ maxWidth: 280, whiteSpace: 'normal', color: '#a0806a' }}
                    >
                      {d.reason}
                    </div>
                  )}
                </td>
                <td>{libName(libs, d.lib)}</td>
                <td>
                  <span
                    className={`tag ${
                      d.status === '失败' ? 'red' : d.status === '已删除' ? 'oat' : ''
                    }`}
                  >
                    {d.status}
                  </span>
                </td>
                <td>{d.date}</td>
                <td>{d.size}</td>
                <td>
                  <span className="table-actions">
                    {writable(libs, d.lib) ? (
                      d.status === '已删除' ? (
                        <>
                          <button onClick={() => restoreDoc(d.id)}>恢复</button>
                          <button onClick={() => purgeDoc(d.id)}>彻底删除</button>
                        </>
                      ) : (
                        <>
                          {d.status === '失败' && (
                            <button onClick={() => void retryDoc(d.id)}>重试</button>
                          )}
                          <button onClick={() => deleteDoc(d.id)}>删除</button>
                        </>
                      )
                    ) : (
                      <span className="doc-status">仅查看</span>
                    )}
                  </span>
                </td>
              </tr>
            ))}
            {!filtered.length && (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: 45 }}>
                  这里还没有符合条件的文档
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p className="table-note">
        {filtered.length} 份文档 · 删除后在回收站保留 30
        天。查看者仅可检索和查看文档。上传与解析状态为交互演示。
      </p>
    </>
  )
}
