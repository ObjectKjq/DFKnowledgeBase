import { getKnowledgeBasePage } from '@/api/knowledgeBaseController'
import { getMemberList } from '@/api/knowledgeBaseMemberController'
import { queryClient } from '@/lib/queryClient'
import { useAuthStore } from '@/stores/auth'

export interface Library {
  id: string
  name: string
  type: 'personal' | 'shared'
  role: '所有者' | '编辑者' | '查看者'
  defaultFlag: boolean
  description?: string
}

export const librariesKey = (userId?: number, token?: string | null) => [
  'knowledge-libraries',
  userId,
  token,
]

/** 分页读取全部有权知识库，成员权限由真实成员列表确定。 */
export async function fetchLibraries(userId: number): Promise<Library[]> {
  const rows: API.KnowledgeBaseRespVO[] = []
  let pageNum = 1
  while (true) {
    const { data } = await getKnowledgeBasePage({ reqVO: { pageNum, pageSize: 100 } })
    rows.push(...(data?.list ?? []))
    if (!data?.list?.length || rows.length >= (data.total ?? 0)) break
    pageNum++
  }
  return Promise.all(
    rows.map(async (row) => {
      let role: Library['role'] = row.ownerId === userId ? '所有者' : '查看者'
      if (row.type === 2 && row.ownerId !== userId) {
        const { data } = await getMemberList({ knowledgeBaseId: row.id! })
        if (data?.find((member) => member.userId === userId)?.memberRole === 1) role = '编辑者'
      }
      return {
        id: String(row.id),
        name: row.name ?? '',
        type: row.type === 1 ? 'personal' : 'shared',
        role,
        defaultFlag: row.defaultFlag === true,
        description: row.description,
      }
    }),
  )
}

/** 会话动作读取当前登录用户的缓存；不持久化服务端知识库。 */
export function getLibraries(): Library[] {
  const { user, token } = useAuthStore.getState()
  const key = librariesKey(user?.id, token)
  if (queryClient.getQueryState(key)?.status === 'error') return []
  return queryClient.getQueryData<Library[]>(key) ?? []
}
