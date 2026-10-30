import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { expect, it, vi } from 'vitest'

import { Home } from './Home'

vi.mock('@/lib/http', () => ({
  default: vi.fn().mockResolvedValue({ code: 0, data: { list: [], total: 0 } }),
}))

it('首页最近可用文档支持查看全部', () => {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/settings/docs" element={<p>文档管理</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  fireEvent.click(screen.getByRole('button', { name: /查看全部/ }))
  expect(screen.getByText('文档管理')).toBeInTheDocument()
})
