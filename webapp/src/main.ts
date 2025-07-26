import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import HomePage from './components/HomePage.vue'
import ArticleList from './components/ArticleList.vue'
import QAList from './components/QAList.vue'
import PostDetail from './components/PostDetail.vue'
import UserCenter from './components/UserCenter.vue'
import AvatarEdit from './components/AvatarEdit.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import FollowList from './components/FollowList.vue'
import PostCreate from './components/PostCreate.vue'
import Trends from './components/Trends.vue'
import Notice from './components/Notice.vue'

const routes = [
  { path: '/', component: HomePage },
  { path: '/articles', component: ArticleList },
  { path: '/qa', component: QAList },
  { path: '/post/:id', component: PostDetail },
  { path: '/:userid', component: UserCenter },
  { path: '/edit-avatar', component: AvatarEdit },
  { path: '/follow-list/:userid', component: FollowList },
  { path: '/create-post', component: PostCreate },
  { path: '/trends', component: Trends },
  { path: '/notice', component: Notice },
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
