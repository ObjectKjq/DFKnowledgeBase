/** 管理页页签配置：`menuPath` 对应后端菜单树的路由地址，`permission` 是页签入口的权限串。 */
export interface TabConfig {
  key: string
  title: string
  description: string
  icon: string
  group: '知识空间' | '实例管理' | '个人'
  menuPath?: string
  permission?: string
}

export const TAB_CONFIGS: TabConfig[] = [
  {
    key: 'libraries',
    title: '知识库与成员',
    description: '整理知识库，与成员共享资料和协作权限。',
    icon: 'book',
    group: '知识空间',
  },
  {
    key: 'docs',
    title: '文档管理',
    description: '查看文档与入库进度，管理可用于检索的资料。',
    icon: 'file',
    group: '知识空间',
  },
  {
    key: 'mcp',
    title: 'MCP 接入',
    description: '将知识库连接到你的 AI 工具，管理凭证与调用记录。',
    icon: 'plug',
    group: '知识空间',
  },
  {
    key: 'models',
    title: '实例设置',
    description: '配置对话模型、接入能力与文档处理参数。',
    icon: 'settings',
    group: '实例管理',
  },
  {
    key: 'users',
    title: '用户管理',
    description: '维护实例用户，分配角色与管理账号状态。',
    icon: 'users',
    group: '实例管理',
    menuPath: '/system/user',
    permission: 'system:user:list',
  },
  {
    key: 'roles',
    title: '角色管理',
    description: '为不同的协作角色分配合适的功能权限。',
    icon: 'shield',
    group: '实例管理',
    menuPath: '/system/role',
    permission: 'system:role:list',
  },
  {
    key: 'menus',
    title: '菜单管理',
    description: '维护功能菜单与权限标识。',
    icon: 'layers',
    group: '实例管理',
    menuPath: '/system/menu',
    permission: 'system:menu:list',
  },
  {
    key: 'account',
    title: '账号',
    description: '管理个人资料、登录密码与当前会话。',
    icon: 'users',
    group: '个人',
  },
]

/**
 * 菜单路由地址 → 前端路由（半动态：路由组件静态注册，菜单树只驱动显隐与跳转）。
 * 未映射的地址回首页。
 */
export function menuRoute(path?: string): string {
  if (path === '/chat') return '/'
  const tab = TAB_CONFIGS.find((t) => t.menuPath === path)
  return tab ? `/settings/${tab.key}` : '/'
}
