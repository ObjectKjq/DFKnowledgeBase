/** 文档格式与限制对齐后端 DocFormatEnum / DocConstants。 */
const DOCUMENT_EXTENSIONS = [
  '.pdf',
  '.doc',
  '.docx',
  '.ppt',
  '.pptx',
  '.xls',
  '.xlsx',
  '.txt',
  '.md',
  '.markdown',
]
export const DOCUMENT_ACCEPT = DOCUMENT_EXTENSIONS.join(',')
export const MAX_DOCUMENT_SIZE = 50 * 1024 * 1024

export const DOCUMENT_STATUSES: Record<number, string> = {
  1: '排队',
  2: '解析中',
  3: '索引中',
  4: '可用',
  5: '失败',
  6: '已删除',
}

/** 所有上传入口使用同一校验，返回可直接提示的拒绝原因。 */
export function documentUploadError(file: File): string | null {
  if (!DOCUMENT_EXTENSIONS.some((extension) => file.name.toLowerCase().endsWith(extension))) {
    return `${file.name}：不支持该格式，请选择 PDF、Word、PPT、Excel、TXT 或 Markdown`
  }
  if (file.size > MAX_DOCUMENT_SIZE) return `${file.name}：超过单文件 50 MB 上限`
  return null
}
