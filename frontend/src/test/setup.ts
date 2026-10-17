// 测试环境装配：接入 jest-dom 断言扩展（本轮只装依赖与配置，不写业务测试）。
import '@testing-library/jest-dom/vitest'

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
