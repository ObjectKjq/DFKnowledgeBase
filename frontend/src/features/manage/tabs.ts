/** 管理页页签配置：`menuPath` 对应后端菜单树的路由地址，`permission` 是页签入口的权限串。 */
export interface TabConfig {
  key: string
  title: string
  menuPath?: string
  permission?: string
}

export const TAB_CONFIGS: TabConfig[] = [
  { key: 'libraries', title: '知识库与成员' },
  { key: 'docs', title: '文档管理' },
  { key: 'mcp', title: 'MCP 接入' },
  { key: 'models', title: '实例设置' },
  { key: 'users', title: '用户管理', menuPath: '/system/user', permission: 'system:user:list' },
  { key: 'roles', title: '角色管理', menuPath: '/system/role', permission: 'system:role:list' },
  { key: 'menus', title: '菜单管理', menuPath: '/system/menu', permission: 'system:menu:list' },
  { key: 'account', title: '账号' },
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
