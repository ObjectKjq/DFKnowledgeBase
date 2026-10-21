import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { logout } from '@/api/authController'
import { getProfile, updateProfilePassword } from '@/api/userController'

import { AccountTab } from './AccountTab'

vi.mock('@/api/authController', () => ({
  logout: vi.fn(),
}))

vi.mock('@/api/userController', () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  updateProfilePassword: vi.fn(),
}))

function renderTab() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AccountTab />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AccountTab 修改密码', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(getProfile).mockResolvedValue({
      code: 0,
      msg: '',
      data: { id: 1, username: 'admin', nickname: '管理员' },
    })
    vi.mocked(logout).mockResolvedValue({ code: 0, msg: '', data: true })
  })

  it('未填旧密码时提示', async () => {
    renderTab()
    fireEvent.click(await screen.findByRole('button', { name: '修改密码' }))
    expect(screen.getByText('请输入旧密码')).toBeInTheDocument()
    expect(updateProfilePassword).not.toHaveBeenCalled()
  })

  it('两次新密码不一致时提示', async () => {
    renderTab()
    fireEvent.change(await screen.findByLabelText('旧密码'), {
      target: { value: 'old-pass' },
    })
    fireEvent.change(screen.getByLabelText('新密码'), {
      target: { value: 'new-pass' },
    })
    fireEvent.change(screen.getByLabelText('确认新密码'), {
      target: { value: 'other-pass' },
    })
    fireEvent.click(screen.getByRole('button', { name: '修改密码' }))
    expect(screen.getByText('两次输入的新密码不一致')).toBeInTheDocument()
    expect(updateProfilePassword).not.toHaveBeenCalled()
  })

  it('旧密码不正确时展示提示', async () => {
    vi.mocked(updateProfilePassword).mockRejectedValue(new Error('旧密码不正确'))
    renderTab()
    fireEvent.change(await screen.findByLabelText('旧密码'), {
      target: { value: 'bad-pass' },
    })
    fireEvent.change(screen.getByLabelText('新密码'), {
      target: { value: 'new-pass' },
    })
    fireEvent.change(screen.getByLabelText('确认新密码'), {
      target: { value: 'new-pass' },
    })
    fireEvent.click(screen.getByRole('button', { name: '修改密码' }))
    expect(await screen.findByText('旧密码不正确')).toBeInTheDocument()
    await waitFor(() => expect(updateProfilePassword).toHaveBeenCalled())
  })
})
