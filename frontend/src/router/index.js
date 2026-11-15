import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layout/AdminLayout.vue'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/system/LoginView.vue') },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/activity',
    children: [
      { path: 'activity', name: 'activityList', component: () => import('../views/activity/ActivityListView.vue') },
      { path: 'activity/create', name: 'activityCreate', component: () => import('../views/activity/ActivityEditView.vue') },
      { path: 'activity/:id/edit', name: 'activityEdit', component: () => import('../views/activity/ActivityEditView.vue') },
      { path: 'activity/:id', name: 'activityDetail', component: () => import('../views/activity/ActivityDetailView.vue') },
      { path: 'activity/:id/registrations', name: 'registrationList', component: () => import('../views/signup/RegistrationListView.vue') },
      { path: 'activity/:id/checkins', name: 'checkinList', component: () => import('../views/checkin/CheckinRecordView.vue') },
      { path: 'statistics', name: 'statistics', component: () => import('../views/statistics/StatisticsView.vue') },
      { path: 'system/accounts', name: 'accounts', component: () => import('../views/system/AdminAccountView.vue') }
    ]
  },
  // 移动端 H5（不需要登录）
  { path: '/m/promo/:guid', name: 'promo', component: () => import('../mobile/PromoView.vue') },
  { path: '/m/signup/:guid', name: 'mobileSignup', component: () => import('../mobile/SignupView.vue') },
  { path: '/m/checkin/:activityId', name: 'mobileCheckin', component: () => import('../mobile/CheckinView.vue') },
  { path: '/m/board/:activityId', name: 'checkinBoard', component: () => import('../mobile/CheckinBoardView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 后台路由守卫：未登录跳转登录页
router.beforeEach((to) => {
  const isAdminRoute = !to.path.startsWith('/m/') && to.path !== '/login'
  if (isAdminRoute && !localStorage.getItem('token')) {
    return { path: '/login' }
  }
})

export default router
