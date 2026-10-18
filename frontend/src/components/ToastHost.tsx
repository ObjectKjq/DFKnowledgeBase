import { useKb } from '@/stores/knowledge'

/** 全局提示（复刻原型 `#toast-root`）。 */
export function ToastHost() {
  const toast = useKb((s) => s.toast)
  return (
    <div id="toast-root" aria-live="polite" role="status">
      {toast && <div className={`toast ${toast.error ? 'error' : ''}`}>{toast.msg}</div>}
    </div>
  )
}
