import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getPermissionInfo, login } from '@/api/authController'
import { useAuthStore } from '@/stores/auth'

import { LoginPage } from './LoginPage'

vi.mock('@/api/authController', () => ({
  login: vi.fn(),
  getPermissionInfo: vi.fn(),
}))

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<h1>检索首页</h1>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ token: null, user: null, roles: [], permissions: [], menus: [] })
  })

  it('未输入用户名与密码时提示', () => {
    renderPage()
    fireEvent.click(screen.getByRole('button', { name: /进入知识空间/ }))
    expect(screen.getByText('请输入用户名与密码')).toBeInTheDocument()
  })

  it('登录失败时展示错误提示', async () => {
    vi.mocked(login).mockRejectedValue(new Error('账号或密码错误'))
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('输入你的用户名'), {
      target: { value: 'alice' },
    })
    fireEvent.change(screen.getByPlaceholderText('输入你的密码'), { target: { value: 'bad' } })
    fireEvent.click(screen.getByRole('button', { name: /进入知识空间/ }))
    expect(await screen.findByText('账号或密码错误')).toBeInTheDocument()
  })

  it('登录成功后写入会话并跳转', async () => {
    vi.mocked(login).mockResolvedValue({ code: 0, msg: '', data: { userId: 1, token: 'tk-1' } })
    vi.mocked(getPermissionInfo).mockResolvedValue({
      code: 0,
      msg: '',
      data: {
        user: { id: 1, username: 'admin' },
        roles: ['instance_admin'],
        permissions: ['system:user:list'],
        menus: [],
      },
    })
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('输入你的用户名'), {
      target: { value: 'admin' },
    })
    fireEvent.change(screen.getByPlaceholderText('输入你的密码'), {
      target: { value: 'admin123' },
    })
    fireEvent.click(screen.getByRole('button', { name: /进入知识空间/ }))
    expect(await screen.findByText('检索首页')).toBeInTheDocument()
    expect(useAuthStore.getState().token).toBe('tk-1')
  })
})
