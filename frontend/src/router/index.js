import { createRouter, createWebHistory } from 'vue-router'
import Login from '../pages/Login.vue'
import Register from '../pages/Register.vue'
import Documents from '../pages/Documents.vue'
import Editor from '../pages/Editor.vue'
import { authState } from '../utils/storage'

// 路由配置
const routes = [
  { path: '/', redirect: '/documents' },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/documents', component: Documents },
  { path: '/documents/:id', component: Editor, props: true },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 简单路由守卫：未登录跳转登录页
router.beforeEach((to) => {
  const authed = Boolean(authState.value.token)
  const publicPages = ['/login', '/register']
  if (!authed && !publicPages.includes(to.path)) {
    return '/login'
  }
  if (authed && publicPages.includes(to.path)) {
    return '/documents'
  }
  return true
})

export default router
