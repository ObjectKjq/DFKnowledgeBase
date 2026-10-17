import { AppLayout } from '@/components/AppLayout'
import { ChatPage } from '@/features/chat/ChatPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { KnowledgeBasesPage } from '@/features/knowledge/KnowledgeBasesPage'
import { McpCredentialsPage } from '@/features/mcp/McpCredentialsPage'
import { AccountPage } from '@/features/settings/AccountPage'
import { DocumentsPage } from '@/features/settings/DocumentsPage'
import { InstanceSettingsPage } from '@/features/settings/InstanceSettingsPage'
import { SettingsLayout } from '@/features/settings/SettingsLayout'
import { createBrowserRouter, Navigate } from 'react-router-dom'

import { RequireAuth } from './RequireAuth'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/',
        element: <AppLayout />,
        children: [{ index: true, element: <ChatPage /> }],
      },
      {
        path: '/settings',
        element: <SettingsLayout />,
        children: [
          { index: true, element: <Navigate to="/settings/knowledge-bases" replace /> },
          { path: 'knowledge-bases', element: <KnowledgeBasesPage /> },
          { path: 'documents', element: <DocumentsPage /> },
          { path: 'mcp', element: <McpCredentialsPage /> },
          { path: 'instance', element: <InstanceSettingsPage /> },
          { path: 'account', element: <AccountPage /> },
        ],
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
])
