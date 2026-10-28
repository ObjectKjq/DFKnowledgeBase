import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getUserPage, resetUserPassword, updateUserStatus } from '@/api/userController'
import { ModalHost } from '@/components/ModalHost'
import { useAuthStore } from '@/stores/auth'

import { UsersTab } from './UsersTab'

vi.mock('@/api/userController', () => ({
  createUser: vi.fn(),
  deleteUser: vi.fn(),
  getUser: vi.fn(),
  getUserPage: vi.fn(),
  resetUserPassword: vi.fn(),
  updateUser: vi.fn(),
  updateUserStatus: vi.fn(),
}))

vi.mock('@/api/roleController', () => ({
  getSimpleRoleList: vi.fn(),
}))

const PAGE = {
  code: 0,
  msg: '',
  data: {
    total: 1,
    list: [
      {
        id: 2,
        username: 'alice',
        nickname: '爱丽丝',
        status: 0,
        createTime: '2026-10-18T10:00:00',
      },
    ],
  },
}

function renderTab() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <UsersTab />
        <ModalHost />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('UsersTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({
      permissions: [
        'system:user:list',
        'system:user:create',
        'system:user:update',
        'system:user:delete',
        'system:user:reset-password',
        'system:user:update-status',
      ],
    })
  })

  it('渲染用户列表', async () => {
    vi.mocked(getUserPage).mockResolvedValue(PAGE)
    renderTab()
    expect(await screen.findByText('alice')).toBeInTheDocument()
    expect(screen.getByText('爱丽丝')).toBeInTheDocument()
  })

  it('下拉筛选保留开启状态的数值 0，选择全部后清除条件', async () => {
    vi.mocked(getUserPage).mockResolvedValue(PAGE)
    renderTab()
    await screen.findByText('alice')
    const status = screen.getByRole('combobox', { name: '用户状态' })
    fireEvent.mouseDown(status)
    await screen.findByRole('listbox')
    fireEvent.keyDown(status, { key: 'ArrowDown', keyCode: 40 })
    fireEvent.keyDown(status, { key: 'Enter', keyCode: 13 })
    fireEvent.click(screen.getByRole('button', { name: '查询' }))
    await waitFor(() =>
      expect(getUserPage).toHaveBeenLastCalledWith({
        reqVO: { pageNum: 1, pageSize: 10, status: 0 },
      }),
    )
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '用户状态' }))
    fireEvent.click(await screen.findByText('全部状态'))
    fireEvent.click(screen.getByRole('button', { name: '查询' }))
    await waitFor(() =>
      expect(getUserPage).toHaveBeenLastCalledWith({
        reqVO: { pageNum: 1, pageSize: 10, status: undefined },
      }),
    )
  })

  it('停用用户前弹出确认', async () => {
    vi.mocked(getUserPage).mockResolvedValue(PAGE)
    vi.mocked(updateUserStatus).mockResolvedValue({ code: 0, msg: '', data: true })
    renderTab()
    fireEvent.click(await screen.findByRole('button', { name: '停用' }))
    expect(await screen.findByText('确认停用')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认停用' }))
    await waitFor(() => expect(updateUserStatus).toHaveBeenCalledWith({ id: 2, status: 1 }))
  })

  it('重置密码需填写新密码后确认', async () => {
    vi.mocked(getUserPage).mockResolvedValue(PAGE)
    vi.mocked(resetUserPassword).mockResolvedValue({ code: 0, msg: '', data: true })
    renderTab()
    fireEvent.click(await screen.findByRole('button', { name: '重置密码' }))
    fireEvent.change(await screen.findByLabelText('新密码'), {
      target: { value: 'new-pass' },
    })
    fireEvent.click(screen.getByRole('button', { name: '确认重置' }))
    await waitFor(() =>
      expect(resetUserPassword).toHaveBeenCalledWith({ id: 2, password: 'new-pass' }),
    )
  })
})
