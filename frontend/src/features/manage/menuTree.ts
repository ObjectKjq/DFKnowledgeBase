/** 菜单类型（对齐后端 MenuTypeEnum）。 */
export const MENU_TYPE_DIR = 1
export const MENU_TYPE_BUTTON = 3

/** 菜单平铺项：`depth` 是树中层级（根为 0），用于按层级缩进展示。 */
export interface FlatMenu {
  menu: API.MenuRespVO
  depth: number
}

/** 把平铺的菜单列表（按 parentId）组装为树序平铺，供菜单管理与分配菜单勾选展示。 */
export function flattenMenuTree(list: API.MenuRespVO[]): FlatMenu[] {
  const byParent = new Map<number, API.MenuRespVO[]>()
  for (const menu of list) {
    const arr = byParent.get(menu.parentId ?? 0) ?? []
    arr.push(menu)
    byParent.set(menu.parentId ?? 0, arr)
  }
  const out: FlatMenu[] = []
  const walk = (parentId: number, depth: number) => {
    for (const menu of byParent.get(parentId) ?? []) {
      out.push({ menu, depth })
      walk(menu.id!, depth + 1)
    }
  }
  walk(0, 0)
  return out
}

/** 菜单类型（1 目录 2 菜单 3 按钮）→ 展示文案。 */
export function menuTypeLabel(type?: number): string {
  return type === MENU_TYPE_DIR ? '目录' : type === MENU_TYPE_BUTTON ? '按钮' : '菜单'
}
