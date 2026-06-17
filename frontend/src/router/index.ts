import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useSetupStore } from '../stores/setup'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
    { path: '/setup', component: () => import('../views/SetupView.vue'), meta: { setup: true } },
    { path: '/totp-enroll', component: () => import('../views/TotpEnrollView.vue'), meta: { guest: true } },
    { path: '/', component: () => import('../views/DashboardView.vue'), meta: { auth: true } },
    { path: '/clients', component: () => import('../views/clients/ClientListView.vue'), meta: { auth: true, permission: 'client:read' } },
    { path: '/clients/new', component: () => import('../views/clients/ClientCreateView.vue'), meta: { auth: true, permission: 'client:create' } },
    { path: '/clients/:id', component: () => import('../views/clients/ClientDetailView.vue'), meta: { auth: true, permission: 'client:read' } },
    { path: '/users', component: () => import('../views/users/UserListView.vue'), meta: { auth: true, admin: true, permission: 'user:view' } },
    { path: '/users/new', component: () => import('../views/users/UserCreateView.vue'), meta: { auth: true, admin: true, permission: 'user:create' } },
    { path: '/users/:id', component: () => import('../views/users/UserEditView.vue'), meta: { auth: true, admin: true, permission: 'user:manage' } },

    { path: '/audit-logs', component: () => import('../views/audit/AuditLogView.vue'), meta: { auth: true, admin: true, permission: 'audit:view' } },
    { path: '/re-enroll', component: () => import('../views/ReEnrollView.vue'), meta: { auth: true } },
    { path: '/recovery-codes', component: () => import('../views/RecoveryCodesView.vue'), meta: { auth: true, admin: true } },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()
  const setup = useSetupStore()

  if (!auth.initialized) {
    await auth.fetchUser()
  }
  if (!setup.initialized) {
    await setup.checkStatus()
  }

  if (to.meta.setup) {
    next()
  } else if (setup.needsSetup && to.path !== '/setup') {
    next('/setup')
  } else if (to.meta.auth && !auth.user) {
    next('/login')
  } else if (to.meta.guest && auth.user) {
    next('/')
  } else if ((to.meta as any).admin && auth.user?.role !== 'admin') {
    next('/')
  } else {
    next()
  }
})

export default router
