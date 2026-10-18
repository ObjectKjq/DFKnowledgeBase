import { createBrowserRouter, Navigate } from 'react-router-dom'

import { AppLayout } from '@/components/AppLayout'
import { LoginPage } from '@/features/auth/LoginPage'
import { ChatPage } from '@/features/chat/ChatPage'
import { ManagementPage } from '@/features/manage/ManagementPage'

import { RequireAuth } from './RequireAuth'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/',
        element: <AppLayout />,
        children: [
          { index: true, element: <ChatPage /> },
          { path: 'settings', element: <Navigate to="/settings/docs" replace /> },
          { path: 'settings/:tab', element: <ManagementPage /> },
        ],
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
])
