import type { Library } from '@/features/knowledge/libraries'

/** 按文件大小显示便于阅读的单位。 */
export function formatSize(bytes: number) {
  return bytes > 1048576
    ? (bytes / 1048576).toFixed(1) + ' MB'
    : Math.max(1, Math.round(bytes / 1024)) + ' KB'
}

/** 授权列表中已不可见的知识库只显示未知名称。 */
export function libName(libraries: Library[], id: string) {
  return libraries.find((library) => library.id === id)?.name || '未知知识库'
}
