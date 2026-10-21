import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'

import { PreviewDrawer } from '@/features/chat/PreviewDrawer'
import { useChat } from '@/stores/chat'
import { useKb } from '@/stores/knowledge'

import { AppSidebar } from './AppSidebar'
import { AppTopbar } from './AppTopbar'
import { ModalHost } from './ModalHost'
import { ToastHost } from './ToastHost'

/** 三区布局壳（复刻原型 `.app`）：左侧栏 + 主区（顶栏 + 工作区）+ 原文预览抽屉。 */
export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const previewId = useChat((s) => s.previewChunkId)
  const closeSidebar = () => setSidebarOpen(false)

  // 原型全局快捷键：Ctrl/Cmd+K 新检索；Esc 逐层关闭弹层。
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const s = useKb.getState()
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        useChat.getState().newChat()
        document.getElementById('query')?.focus()
      }
      if (e.key === 'Escape') {
        if (s.modal) s.closeModal()
        else if (s.popover) {
          s.closePopover()
          document.getElementById('query')?.focus()
        } else if (useChat.getState().previewChunkId) useChat.getState().closePreview()
        else setSidebarOpen(false)
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [])

  return (
    <>
      <div className="app" id="app">
        <div
          className={`overlay-mobile ${sidebarOpen ? 'open' : ''}`}
          onClick={closeSidebar}
          aria-hidden="true"
        />
        <AppSidebar open={sidebarOpen} onClose={closeSidebar} />
        <main className="main">
          <AppTopbar onToggleSidebar={() => setSidebarOpen((v) => !v)} />
          <div className={`workspace ${previewId ? 'with-drawer' : ''}`} id="workspace">
            <Outlet />
            <PreviewDrawer />
          </div>
        </main>
      </div>
      <ModalHost />
      <ToastHost />
    </>
  )
}
