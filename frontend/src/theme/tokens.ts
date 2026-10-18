import type { ThemeConfig } from 'antd'

/**
 * 「知序」色板 —— 全工程唯一色源的 TypeScript 侧。
 * 取值与 `theme/index.css` 的 CSS 变量成对维护（改一处必须同步另一处），
 * 色值来自原型 `docs/prototype/本地化知识库原型_第一版.html` 的 `:root`。
 */
export const palette = {
  bg: '#fafaf8',
  paper: '#fff',
  side: '#f2f3ee',
  ink: '#29392f',
  text: '#475249',
  muted: '#65725e',
  line: '#e5e8df',
  sage: '#657d62',
  sageDark: '#435c42',
  sageTint: '#e7ecdf',
  oat: '#f4efe4',
  blue: '#eaf0f2',
  danger: '#a04e43',
  shadow: '0 6px 30px #29392f06',
} as const

export const fontSans = '"Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif'
export const fontSerif =
  'Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif'

/** 产品名（登录页与侧栏标题使用）。 */
export const PRODUCT_NAME = '知序'

/**
 * 喂给 `ConfigProvider` 的 antd 主题：把默认主色替换为知序绿。
 * 只实现亮色；暗色切换点预留在 `algorithm`，本期不实现。
 */
export const antdTheme: ThemeConfig = {
  token: {
    colorPrimary: palette.sage,
    colorInfo: palette.sage,
    colorSuccess: palette.sage,
    colorError: palette.danger,
    colorTextBase: palette.text,
    colorTextHeading: palette.ink,
    colorBgBase: palette.bg,
    colorBgContainer: palette.paper,
    colorBgLayout: palette.bg,
    colorBorder: palette.line,
    colorBorderSecondary: palette.line,
    colorLink: palette.sageDark,
    borderRadius: 8,
    fontFamily: fontSans,
    fontSize: 14,
  },
  components: {
    Menu: {
      itemBg: palette.side,
      itemColor: palette.text,
      itemSelectedBg: palette.sageTint,
      itemSelectedColor: palette.sageDark,
    },
  },
}
