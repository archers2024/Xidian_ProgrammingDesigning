import 'vue-router'

/** 布局种类：后台外壳、参与者页面、全屏页面（登录、签到大屏） */
export type LayoutKind = 'admin' | 'public' | 'blank'

export type TransitionKind = 'page-fade' | 'slide-forward' | 'slide-back' | 'none'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题，用于浏览器标题与后台面包屑 */
    title: string
    layout?: LayoutKind
    requiresAuth?: boolean
    /** 强制转场；未声明时后台用 page-fade，手机按 depth 判断方向 */
    transition?: TransitionKind
    /** 手机页面层级，用于判断前进/后退方向 */
    depth?: number
  }
}
