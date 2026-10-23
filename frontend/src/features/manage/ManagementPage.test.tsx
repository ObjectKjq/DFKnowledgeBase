import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { expect, it, vi } from 'vitest'
import { useAuthStore } from '@/stores/auth'
import { ManagementPage } from './ManagementPage'
vi.mock('@/lib/http', () => ({
  default: vi.fn().mockResolvedValue({ code: 0, data: { list: [], total: 0 } }),
}))
it('普通用户管理页不展示实例配置入口', () => {
  useAuthStore.setState({ roles: [], menus: [], token: 'test', user: { id: 1 } })
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={['/settings/docs']}>
        <Routes>
          <Route path="/settings/:tab" element={<ManagementPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  expect(screen.queryByRole('button', { name: '实例设置' })).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'MCP 接入' })).toBeInTheDocument()
})
