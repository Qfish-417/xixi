import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/editor',
    name: 'Editor',
    component: () => import('../views/Editor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/library',
    name: 'Library',
    component: () => import('../views/Library.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/groups',
    name: 'Groups',
    component: () => import('../views/Groups.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from) => {
  const isAuthenticated = localStorage.getItem('token') !== null
  
  if (to.meta.requiresAuth && !isAuthenticated) {
    return '/login'
  } else if ((to.path === '/login' || to.path === '/register') && isAuthenticated) {
    return '/'
  }
})

export default router
