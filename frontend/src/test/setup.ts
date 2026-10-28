// 测试环境装配：接入 jest-dom 断言扩展与浏览器 API 测试桩。
import '@testing-library/jest-dom/vitest'

// jsdom 不绘制滚动条伪元素，弹窗测量时复用普通元素的计算样式。
const getComputedStyle = window.getComputedStyle.bind(window)
window.getComputedStyle = (element) => getComputedStyle(element)

// jsdom 未实现 matchMedia，而 antd 的响应式栅格依赖它，需在此补桩。
if (typeof window.matchMedia !== 'function') {
  window.matchMedia = (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })
}

// jsdom 不计算布局，Ant Design 的下拉浮层仍需要此 API 完成挂载。
if (typeof globalThis.ResizeObserver !== 'function') {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}
