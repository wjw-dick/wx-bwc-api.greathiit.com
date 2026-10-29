import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/DEMO' },
  { path: '/:code', name: 'Home', component: () => import('@/views/Home.vue') },
  { path: '/:code/visitor', name: 'Visitor', component: () => import('@/views/Visitor.vue') },
  { path: '/:code/lost', name: 'Lost', component: () => import('@/views/Lost.vue') },
  { path: '/:code/news', name: 'News', component: () => import('@/views/News.vue') },
  { path: '/:code/news/:id', name: 'NewsDetail', component: () => import('@/views/NewsDetail.vue') },
  { path: '/:code/school', name: 'School', component: () => import('@/views/School.vue') },
  { path: '/:code/share/:type/:id', name: 'Share', component: () => import('@/views/Share.vue') },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory('/h5/'),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  const code = String(to.params.code || '')
  if (code && code !== 'DEMO') {
    document.title = `${code} · 校园安全门户`
  }
})

export default router
