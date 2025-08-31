import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import CacheManager from './utils/cacheManager'

// 启动缓存管理系统
CacheManager.startPeriodicCleanup()

// 注册Service Worker用于头像缓存
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then((registration) => {
        console.log('SW registered: ', registration)
      })
      .catch((registrationError) => {
        console.log('SW registration failed: ', registrationError)
      })
  })
}

// 路由配置
const routes = [
  { path: '/', component: () => import('./components/HomePage.vue') },
  { path: '/articles', component: () => import('./components/ArticleList.vue') },
  { path: '/post/:id', component: () => import('./components/PostDetail.vue') },
  { path: '/:userid', component: () => import('./components/UserCenter.vue') },
  { path: '/edit-avatar', component: () => import('./components/AvatarEdit.vue') },
  { path: '/follow-list/:userid', component: () => import('./components/FollowList.vue') },
  { path: '/create-post', component: () => import('./components/PostCreate.vue') },
  { path: '/trends', component: () => import('./components/Trends.vue') },
  { path: '/notice', component: () => import('./components/Notice.vue') },
  { path: '/search', component: () => import('./components/SearchResults.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
