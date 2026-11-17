import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layout/AdminLayout.vue'
import {ensureSession} from '../api/session'

const routes = [
  {path:'/forbidden',component:()=>import('../views/system/ResultView.vue'),meta:{public:true,forbidden:true}},
  {path:'/:pathMatch(.*)*',component:()=>import('../views/system/ResultView.vue'),meta:{public:true}},
  { path: '/login', name: 'login', component: () => import('../views/system/LoginView.vue') },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/platform/DashboardView.vue') },
      { path: 'platform/organizations', name: 'organizations', component: () => import('../views/platform/OrganizationView.vue') },
      { path: 'platform/reviews', name: 'reviews', component: () => import('../views/platform/ReviewCenter.vue') },
      { path: 'platform/audit', name: 'audit', component: () => import('../views/platform/AuditView.vue') },
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
router.beforeEach(async to=>{
 const protectedRoute=(!to.path.startsWith('/m/')||to.path.startsWith('/m/board/'))&&to.path!=='/login'&&!to.meta.public
 if(!protectedRoute)return
 const profile=await ensureSession()
 if(!profile)return{path:'/login',query:{redirect:to.fullPath}}
 if((to.path.startsWith('/platform/organizations')||to.path.startsWith('/platform/audit')||to.path.startsWith('/system/accounts'))&&profile.roleCode!=='SUPER')return'/forbidden'
})
window.addEventListener('session-expired',()=>{const current=router.currentRoute.value;if(current.path!=='/login'&&(!current.path.startsWith('/m/')||current.path.startsWith('/m/board/')))router.replace({path:'/login',query:{redirect:current.fullPath}})})
export default router
