import type { ThemeConfig } from 'antd'

/**
 * 「知序」色板 —— 全工程唯一色源的 TypeScript 侧。
 * 取值与 `theme/index.css` 的 CSS 变量成对维护（改一处必须同步另一处），
 * 采用中性工作区、柔和边界与少量绿色强调。
 */
export const palette = {
  bg: '#f7f8f6',
  paper: '#fff',
  side: '#f4f5f2',
  ink: '#272d29',
  text: '#4c554e',
  muted: '#767f77',
  line: '#e5e8e2',
  sage: '#55745c',
  sageDark: '#3e5945',
  sageTint: '#edf3ed',
  oat: '#f4efe4',
  blue: '#eaf0f2',
  danger: '#a04e43',
  controlBg: '#fff',
  controlBorder: '#dce1d9',
  controlFocus: '#55745c24',
  hover: '#eeefeb',
  selected: '#e5e9e2',
  action: '#303b33',
  actionHover: '#202b23',
  dangerTint: '#fbefed',
  backdrop: '#18211c5c',
  shadow: '0 2px 8px #202a2208',
  popupShadow: '0 10px 40px #202a2214',
  composerShadow: '0 4px 24px #202a220a',
} as const

/** 原型表单控件的基础尺寸，与 CSS 变量同步。 */
export const controlMetrics = { radius: 8, height: 36 } as const

/** 下拉框和浮层的统一尺寸，与 CSS 变量同步。 */
export const selectMetrics = { radius: 8, popupRadius: 10, smallHeight: 32 } as const

/** 工作区的布局、圆角与间距，与 CSS 变量同步。 */
export const layoutMetrics = {
  sidebarWidth: 248,
  topbarHeight: 56,
  contentWidth: 1120,
  chatWidth: 800,
  surfaceRadius: 12,
  composerRadius: 20,
  modalRadius: 16,
  modelMenuWidth: 280,
  modelTriggerWidth: 220,
  composerMenuWidth: 320,
} as const
export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  section: 32,
  page: 40,
  hero: 48,
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
    colorTextSecondary: palette.muted,
    colorTextPlaceholder: palette.muted,
    colorBgBase: palette.bg,
    colorBgContainer: palette.paper,
    colorBgLayout: palette.bg,
    colorBorder: palette.line,
    colorBorderSecondary: palette.line,
    colorLink: palette.sageDark,
    borderRadius: 8,
    fontFamily: fontSans,
    fontSize: 14,
    controlHeight: controlMetrics.height,
    colorFillSecondary: palette.hover,
    controlItemBgActive: palette.sageTint,
  },
  components: {
    Popover: {
      colorBgElevated: palette.paper,
      borderRadiusLG: layoutMetrics.surfaceRadius,
      boxShadowSecondary: palette.popupShadow,
    },
    Dropdown: {
      colorBgElevated: palette.paper,
      controlItemBgHover: palette.hover,
      controlItemBgActive: palette.sageTint,
      controlItemBgActiveHover: palette.sageTint,
      borderRadiusLG: selectMetrics.popupRadius,
    },
    Modal: {
      headerBg: palette.paper,
      contentBg: palette.paper,
      titleColor: palette.ink,
      titleFontSize: 20,
      borderRadiusLG: layoutMetrics.modalRadius,
      colorBgMask: palette.backdrop,
      boxShadow: palette.popupShadow,
    },
    Menu: {
      itemBg: palette.side,
      itemColor: palette.text,
      itemSelectedBg: palette.sageTint,
      itemSelectedColor: palette.sageDark,
    },
    Input: {
      colorBgContainer: palette.controlBg,
      colorBorder: palette.controlBorder,
      activeBorderColor: palette.sage,
      hoverBorderColor: palette.sage,
      activeShadow: `0 0 0 2px ${palette.controlFocus}`,
      borderRadius: controlMetrics.radius,
      controlHeight: controlMetrics.height,
    },
    Select: {
      colorBgContainer: palette.controlBg,
      colorBgElevated: palette.paper,
      colorBgContainerDisabled: palette.side,
      colorBorder: palette.controlBorder,
      colorTextPlaceholder: palette.muted,
      colorTextDisabled: palette.muted,
      activeBorderColor: palette.sage,
      hoverBorderColor: palette.sage,
      activeOutlineColor: palette.controlFocus,
      optionActiveBg: palette.side,
      optionSelectedBg: palette.sageTint,
      optionSelectedColor: palette.sageDark,
      optionSelectedFontWeight: 500,
      optionFontSize: 13,
      optionHeight: 36,
      optionPadding: '8px 12px',
      borderRadius: selectMetrics.radius,
      borderRadiusSM: selectMetrics.radius,
      borderRadiusLG: selectMetrics.popupRadius,
      controlHeight: controlMetrics.height,
      controlHeightSM: selectMetrics.smallHeight,
      fontSize: 13,
      fontSizeSM: 12,
      boxShadowSecondary: palette.popupShadow,
    },
  },
}
