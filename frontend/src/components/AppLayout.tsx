import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'

import { PreviewDrawer } from '@/features/chat/PreviewDrawer'
import { useKb } from '@/stores/knowledge'

import { AppSidebar } from './AppSidebar'
import { AppTopbar } from './AppTopbar'
import { ModalHost } from './ModalHost'
import { ToastHost } from './ToastHost'

/** 三区布局壳（复刻原型 `.app`）：左侧栏 + 主区（顶栏 + 工作区）+ 原文预览抽屉。 */
export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const previewId = useKb((s) => s.previewId)
  const closeSidebar = () => setSidebarOpen(false)

  // 原型全局快捷键：Ctrl/Cmd+K 新检索；Esc 逐层关闭弹层。
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const s = useKb.getState()
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        s.newChat()
        document.getElementById('query')?.focus()
      }
      if (e.key === 'Escape') {
        if (s.modal) s.closeModal()
        else if (s.popover) {
          s.closePopover()
          document.getElementById('query')?.focus()
        } else if (s.previewId) s.closePreview()
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
      <input
        type="file"
        className="hidden"
        id="image-input"
        accept="image/png,image/jpeg,image/webp,image/bmp"
        onChange={(e) => {
          const f = e.target.files?.[0]
          if (f) useKb.getState().setImage(f)
          e.target.value = ''
        }}
      />
    </>
  )
}
