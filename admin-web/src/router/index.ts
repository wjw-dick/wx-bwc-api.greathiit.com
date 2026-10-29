import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('@/layout/AppLayout.vue'),
      children: [
        {
          path: ':pathMatch(.*)*',
          name: 'DynamicPage',
          component: () => import('@/views/DynamicPage.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('bwc_token')
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
