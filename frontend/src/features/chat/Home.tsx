import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'

import { getDocPage } from '@/api/docController'
import { Icon } from '@/components/Icon'
import { useLibraries } from '@/features/knowledge/useLibraries'
import { libName } from '@/mock/logic'
import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

import { sendQuestion } from './chatActions'
import { HERO_ART } from './homeArt'

/** 首页入口与最近可用文档，点击问题直接执行真实检索。 */
export function Home() {
  const navigate = useNavigate()
  const token = useAuthStore((s) => s.token)
  const { libs } = useLibraries()
  const openUpload = useKb((s) => s.openUpload)
  const setMode = useChat((s) => s.setMode)
  const query = useQuery({
    queryKey: ['manage-documents', token, 'recent'],
    queryFn: async () =>
      (await getDocPage({ reqVO: { pageNum: 1, pageSize: 3, status: 4 } })).data?.list ?? [],
    enabled: !!token,
  })
  return (
    <div className="home">
      <div className="hero">
        <div dangerouslySetInnerHTML={{ __html: HERO_ART }} />
        <p className="eyebrow">A QUIET PLACE FOR YOUR KNOWLEDGE</p>
        <h1>
          今天想找<span>什么？</span>
        </h1>
        <p className="hero-description">
          让散落的文档，有序安放。
          <br />
          从一次检索开始，与积累的知识重新相遇。
        </p>
      </div>
      <div className="quick-actions">
        <button className="quick-card" onClick={openUpload}>
          <span className="quick-icon">
            <Icon name="upload" />
          </span>
          <Icon name="arrow" className="arrow" />
          <h2>上传文档</h2>
          <p>
            上传并建立索引，
            <br />
            让知识随时可寻。
          </p>
        </button>
        <button
          className="quick-card"
          onClick={() => {
            setMode('RAG 检索')
            document.getElementById('query')?.focus()
          }}
        >
          <span className="quick-icon">
            <Icon name="spark" />
          </span>
          <Icon name="arrow" className="arrow" />
          <h2>向知识提问</h2>
          <p>
            基于有权访问的文档，
            <br />
            生成可核对引用的答案。
          </p>
        </button>
        <button className="quick-card" onClick={() => navigate('/settings/libraries')}>
          <span className="quick-icon">
            <Icon name="book" />
          </span>
          <Icon name="arrow" className="arrow" />
          <h2>管理知识库</h2>
          <p>
            整理文档和成员，
            <br />
            维护自己的知识积累。
          </p>
        </button>
      </div>
      <div className="suggestions">
        <div className="micro-heading">从一个问题开始</div>
        <div className="suggestion-list">
          {['如何让知识检索更可信？', '总结文档中的主要观点'].map((question) => (
            <button
              className="suggestion"
              key={question}
              onClick={() => {
                setMode('RAG 检索')
                void sendQuestion(question)
              }}
            >
              <Icon name="spark" />
              {question}
            </button>
          ))}
        </div>
      </div>
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
