import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { expect, it, vi } from 'vitest'

import { Home } from './Home'

vi.mock('@/lib/http', () => ({
  default: vi.fn().mockResolvedValue({ code: 0, data: { list: [], total: 0 } }),
}))

it('首页提供真实 MCP 接入入口', () => {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/settings/mcp" element={<p>MCP 接入管理</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  fireEvent.click(screen.getByRole('button', { name: /连接 AI 智能体/ }))
  expect(screen.getByText('MCP 接入管理')).toBeInTheDocument()
})
