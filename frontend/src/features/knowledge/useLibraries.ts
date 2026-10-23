import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthStore } from '@/stores/auth'
import { useChat } from '@/stores/chat'

import { fetchLibraries, librariesKey } from './libraries'

/** 侧栏、管理页和范围选择共用知识库查询，并清理失效范围。 */
export function useLibraries() {
  const userId = useAuthStore((s) => s.user?.id)
  const token = useAuthStore((s) => s.token)
  const scope = useChat((s) => s.scope)
  const restoring = useChat((s) => s.restoreScope)
  const query = useQuery({
    queryKey: librariesKey(userId, token),
    queryFn: () => fetchLibraries(userId!),
    staleTime: 5_000,
    refetchInterval: (query) => (query.state.status === 'error' ? false : 5_000),
    enabled: userId !== undefined && !!token,
  })
  useEffect(() => {
    if (!query.data || restoring) return
    const next = scope.filter((id) => query.data.some((library) => library.id === id))
    if (next.length !== scope.length) useChat.getState().setScope(next)
  }, [query.data, scope, restoring])
  return { ...query, libs: query.isError ? [] : (query.data ?? []) }
}
