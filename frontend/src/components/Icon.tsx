/** 图标组件：引用 index.html 中的 SVG sprite（`#i-<name>`），与原型用法一致。 */
export function Icon({ name, className }: { name: string; className?: string }) {
  return (
    <svg className={className} aria-hidden="true">
      <use href={`#i-${name}`} />
    </svg>
  )
}
