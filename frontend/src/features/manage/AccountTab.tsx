import { useKb } from '@/stores/knowledge'
import { useAuthStore } from '@/stores/auth'

/** 账号 Tab（复刻原型 accountHTML）。 */
export function AccountTab() {
  const user = useAuthStore((s) => s.user)
  const clearSession = useAuthStore((s) => s.clearSession)
  const resetDemo = useKb((s) => s.resetDemo)

  const name = user?.nickname || user?.username || '林予'

  return (
    <>
      <div className="settings-card">
        <h3>你的个人空间</h3>
        <div className="account-avatar">{name.slice(0, 1)}</div>
        <div className="settings-pair">
          <span>用户名</span>
          <span>
            {name}
            {user?.username ? ` / ${user.username}` : ' / linyu'}
          </span>
        </div>
        <div className="settings-pair">
          <span>
            当前角色
            <small>实例管理员不天然拥有他人资料的访问权。</small>
          </span>
          <span className="tag">实例管理员 · 演示</span>
        </div>
        <div className="settings-pair">
          <span>
            数据存放位置
            <small>此原型仅使用当前浏览器本地存储。</small>
          </span>
          <span className="tag oat">当前浏览器</span>
        </div>
      </div>

      <div className="settings-card">
        <h3>关于这个原型</h3>
        <p>
          依据 PRD v0.3 第 6 章「界面与交互」制作。检索、模型回答、解析与凭证均为演示。上传文件不发送到服务器；关闭页面后仅保留文件元信息。图片查询预览不写入本地存储。
        </p>
        <div className="settings-pair">
          <span>
            交互数据
            <small>重置后恢复初始示例资料与会话。</small>
          </span>
          <button className="btn danger small" onClick={resetDemo}>
            重置演示
          </button>
        </div>
        <div className="settings-pair">
          <span>
            登录会话
            <small>退出后返回登录页，需要重新进入演示。</small>
          </span>
          <button className="btn ghost small" onClick={clearSession}>
            退出登录
          </button>
        </div>
      </div>
    </>
  )
}
