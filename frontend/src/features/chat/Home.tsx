import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

import { getDocPage } from '@/api/docController'
import { Icon } from '@/components/Icon'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'

/** 首页以检索输入为中心，下方展示最近可用文档。 */
export function Home({ composer }: { composer?: ReactNode }) {
  const navigate = useNavigate()
  const token = useAuthStore((s) => s.token)
  const { libs } = useLibraries()
  const query = useQuery({
    queryKey: ['manage-documents', token, 'recent'],
    queryFn: async () =>
      (await getDocPage({ reqVO: { pageNum: 1, pageSize: 3, status: 4 } })).data?.list ?? [],
    enabled: !!token,
  })
  return (
    <div className="home">
      <div className="hero">
        <h1>今天想探索什么？</h1>
        <p className="hero-description">搜索资料、梳理思路，从你的知识库开始。</p>
      </div>
      {composer}
      <div className="recent-docs">
        <div className="micro-heading">
          <span>最近可用文档</span>
          <button className="text-btn" onClick={() => navigate('/settings/docs')}>
            查看全部
            <Icon name="arrow" />
          </button>
        </div>
        <div className="recent-doc-list">
          {query.data?.map((doc) => (
            <button className="recent-doc" key={doc.id} onClick={() => navigate('/settings/docs')}>
              <span className="file-icon">
                <Icon name="file" />
              </span>
              <div>
                <strong>{doc.title || doc.fileName}</strong>
                <small>
                  {libName(libs, String(doc.knowledgeBaseId))} · {doc.createTime?.slice(0, 10)}
                </small>
              </div>
            </button>
          ))}
          {query.isError && (
            <button className="text-btn" onClick={() => void query.refetch()}>
              最近文档加载失败，重试
            </button>
          )}
          {query.isSuccess && !query.data.length && (
            <p className="table-note">暂无可用文档，请先上传并等待索引完成。</p>
          )}
        </div>
      </div>
    </div>
  )
}
