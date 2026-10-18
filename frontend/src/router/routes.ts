import type { RouteRecordRaw } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'
import PublicLayout from '@/layouts/PublicLayout.vue'

/**
 * 所有页面都在这里登记：路径、标题、布局、鉴权和转场。
 * 页面组件按路由拆包，宣传与签到页面不加载后台表格依赖。
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/LoginView.vue'),
    meta: { title: '管理员登录', layout: 'blank', transition: 'page-fade', depth: 0 },
  },
  {
    path: '/admin/activities/:id/screen',
    name: 'admin-checkin-screen',
    component: () => import('@/views/admin/CheckinScreenView.vue'),
    meta: {
      title: '签到大屏',
      layout: 'blank',
      requiresAuth: true,
      transition: 'none',
      depth: 3,
    },
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { title: '管理后台', layout: 'admin', requiresAuth: true },
    children: [
      { path: '', redirect: { name: 'admin-dashboard' } },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '工作台', transition: 'page-fade', depth: 1 },
      },
      {
        path: 'activities',
        name: 'admin-activity-list',
        component: () => import('@/views/admin/ActivityListView.vue'),
        meta: { title: '活动管理', transition: 'page-fade', depth: 1 },
      },
      {
        path: 'activities/new',
        name: 'admin-activity-create',
        component: () => import('@/views/admin/ActivityEditorView.vue'),
        meta: { title: '创建活动', transition: 'page-fade', depth: 2 },
      },
      {
        path: 'activities/:id',
        name: 'admin-activity-detail',
        component: () => import('@/views/admin/ActivityDetailView.vue'),
        meta: { title: '活动详情', transition: 'page-fade', depth: 2 },
      },
      {
        path: 'activities/:id/edit',
        name: 'admin-activity-edit',
        component: () => import('@/views/admin/ActivityEditorView.vue'),
        meta: { title: '编辑活动', transition: 'page-fade', depth: 3 },
      },
    ],
  },
  {
    path: '/',
    component: PublicLayout,
    meta: { title: '校园活动', layout: 'public' },
    children: [
      { path: '', name: 'home', redirect: { name: 'admin-dashboard' } },
      {
        path: 'events/:id',
        name: 'public-activity-promo',
        component: () => import('@/views/public/ActivityPromoView.vue'),
        meta: { title: '活动详情', transition: 'page-fade', depth: 1 },
      },
      {
        path: 'events/:id/register',
        name: 'public-registration',
        component: () => import('@/views/public/RegistrationView.vue'),
        meta: { title: '活动报名', depth: 2 },
      },
      {
        path: 'checkin/:sessionId',
        name: 'public-checkin',
        component: () => import('@/views/public/CheckinView.vue'),
        meta: { title: '活动签到', depth: 1 },
      },
      {
        path: ':pathMatch(.*)*',
        name: 'not-found',
        component: () => import('@/views/public/NotFoundView.vue'),
        meta: { title: '页面不存在', depth: 0 },
      },
    ],
  },
]
