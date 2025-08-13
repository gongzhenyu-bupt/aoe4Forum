<template>
  <div class="top-bar-wrapper">
    <div class="header-actions">
      <nav class="nav">
        <a :class="['nav-link', { active: currentRoute === '/' }]" @click="goHome">首页</a>
        <a :class="['nav-link', { active: currentRoute === '/articles' }]" @click="goArticles">文章</a>

        <a :class="['nav-link', { active: currentRoute === '/trends' }]" @click="goTrends">动态</a>
      </nav>
      <button v-if="!isLogin" class="login-btn" @click="emitLoginClick">登录</button>
      <div v-else class="user-actions">
        <!-- 搜索栏 -->
        <div class="search-container">
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="搜索帖子..." 
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch">
            <img src="../assets/search_token.png" alt="搜索" width="20" height="20" />
          </button>
        </div>
        
        <div class="user-avatar" @click="toggleDropdown" tabindex="0" @blur="closeDropdown">
          <img :src="avatarUrl" alt="用户头像" class="avatar-img" />
          <div class="user-dropdown" v-show="dropdownVisible">
            <button class="dropdown-btn" @mousedown.prevent="goUserCenter">用户中心</button>
            <button class="logout-btn" @mousedown.prevent="logout">退出登录</button>
          </div>
        </div>
        <button class="notice-btn" @click="goNotice" title="通知">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M2 6.5V17.5C2 18.3284 2.67157 19 3.5 19H20.5C21.3284 19 22 18.3284 22 17.5V6.5C22 5.67157 21.3284 5 20.5 5H3.5C2.67157 5 2 5.67157 2 6.5Z" stroke="#3b82f6" stroke-width="2"/>
            <path d="M22 6.5L12 13L2 6.5" stroke="#3b82f6" stroke-width="2"/>
          </svg>
        </button>
        <button class="create-post-btn" @click="goCreatePost">发帖</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '../store/user'
import { getProfileApi } from '../utils/api'
import { logoutApi } from '../utils/api'
import { searchPostsApi } from '../utils/api'
import { onMounted, defineEmits, computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getCookie, deleteCookie } from '../utils/cookie'

const user = useUserStore()
const defaultAvatar = 'https://tse3.mm.bing.net/th/id/OIP.g5M-iZUiocFCi9YAzojtRAAAAA?rs=1&pid=ImgDetMain&o=7&rm=3'
const emit = defineEmits(['login-click'])
const router = useRouter()
const route = useRoute()
const currentRoute = computed(() => route.path)
const avatarUrl = ref('')
const DEFAULT_AVATAR = '/default-avatar.png' // 你可以放一张默认头像在 public 目录
const dropdownVisible = ref(false)
const searchKeyword = ref('') // 搜索关键词

const token = getCookie('token')
const isLogin = computed(() => !!token)

// 搜索处理函数
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    return
  }
  
  try {
    // 直接跳转到搜索结果页面，让搜索结果页面处理搜索逻辑
    const keyword = searchKeyword.value.trim()
    router.push(`/search?keyword=${encodeURIComponent(keyword)}`)
    // 清空搜索框
    searchKeyword.value = ''
  } catch (error) {
    console.error('搜索出错:', error)
  }
}

function emitLoginClick() {
  emit('login-click')
}

function goHome() {
  if (route.path !== '/') router.push('/')
}
function goArticles() {
  if (route.path !== '/articles') router.push('/articles')
}

function goTrends() {
  if (route.path !== '/trends') router.push('/trends')
}
function goCreatePost() {
  router.push('/create-post')
}
function goNotice() {
  router.push('/notice')
}
async function goUserCenter() {
  dropdownVisible.value = false
  // 优先用localStorage中的userid
  const userid = localStorage.getItem('userid')
  if (userid) {
    router.push(`/${userid}`)
    return
  }
  // 没有缓存则查profile接口
  try {
    const res = await getProfileApi()
    if ((res.code === 0 || res.code === '0') && res.data && res.data.id) {
      localStorage.setItem('userid', res.data.id)
      router.push(`/${res.data.id}`)
    } else {
      router.push('/')
    }
  } catch {
    router.push('/')
  }
}

function getImageUrl(path: string): string {
  if (!path) return DEFAULT_AVATAR
  if (/^https?:\/\//.test(path)) return path
  // 判断是否是默认头像路径
  if (path.startsWith('/defaultImg/')) {
    return `http://101.126.22.249:7071${path}`
  }
  const filename = path.replace(/\\/g, '/').split('/').pop()
  return filename ? `http://101.126.22.249:7071/avatarImg/${filename}` : DEFAULT_AVATAR
}

function loadAvatar() {
  const cachedAvatar = localStorage.getItem('avatar')
  if (cachedAvatar) {
    avatarUrl.value = getImageUrl(cachedAvatar)
    return
  }
  if (!isLogin.value) {
    avatarUrl.value = DEFAULT_AVATAR
    return
  }
  getProfileApi().then(res => {
    if ((res.code === '0' || res.code === '200') && res.data && res.data.avatar) {
      const url = getImageUrl(res.data.avatar)
      avatarUrl.value = url
      localStorage.setItem('avatar', res.data.avatar)
    } else {
      avatarUrl.value = DEFAULT_AVATAR
    }
  }).catch(() => {
    avatarUrl.value = DEFAULT_AVATAR
  })
}

async function refreshUserInfo() {
  try {
    const res = await getProfileApi()
    if (res.code === 0 || res.code === '0') {
      user.setUser(res.data)
      // 新增：同步userid
      if (res.data.id) localStorage.setItem('userid', res.data.id)
    } else {
      user.logout()
    }
  } catch {
    user.logout()
  }
}

async function logout() {
  try {
    await logoutApi()
  } catch {}
  // 清除本地缓存
  localStorage.removeItem('userid')
  localStorage.removeItem('avatar')
  localStorage.removeItem('name')
  // 清除cookie中的token
  deleteCookie('token')
  // 刷新页面
  window.location.reload()
}

function toggleDropdown(e: Event) {
  dropdownVisible.value = !dropdownVisible.value
}
function closeDropdown() {
  setTimeout(() => {
    dropdownVisible.value = false
  }, 100)
}

onMounted(() => {
  loadAvatar()
})
</script>

<style scoped>
.top-bar-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #fff;
  z-index: 1000;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(0,0,0,0.02);
}
.header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  padding: 0 32px;
}
.nav {
  display: flex;
  align-items: center;
  gap: 32px;
}
.nav-link {
  color: #64748b;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
  cursor: pointer;
}
.nav-link:hover,
.nav-link.active {
  color: #3b82f6;
}
.login-btn {
  margin-left: 24px;
}
.user-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 搜索栏样式 */
.search-container {
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 4px;
  transition: all 0.2s ease;
}

.search-container:focus-within {
  background: #fff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-input {
  border: none;
  background: transparent;
  padding: 8px 12px;
  font-size: 14px;
  color: #374151;
  outline: none;
  min-width: 200px;
}

.search-input::placeholder {
  color: #9ca3af;
}

.search-btn {
  background: transparent;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease;
}

.search-btn:hover {
  background: #f1f5f9;
}

.create-post-btn {
  margin-left: 0;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 18px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}
.create-post-btn:hover {
  background: #2563eb;
}
.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
  position: relative;
  outline: none;
  margin-left: 0;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}
.user-dropdown {
  position: absolute;
  top: 44px;
  right: 0;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  min-width: 120px;
  z-index: 10;
  display: flex;
  flex-direction: column;
}
.dropdown-btn, .logout-btn {
  background: none;
  border: none;
  color: #333;
  padding: 12px 16px;
  cursor: pointer;
  text-align: left;
  font-size: 15px;
  transition: background 0.2s;
}
.dropdown-btn:hover, .logout-btn:hover {
  background: #f5f5f5;
}
.logout-btn {
  color: #f56c6c;
}
.notice-btn {
  background: none;
  border: none;
  margin-left: 10px;
  cursor: pointer;
  padding: 0 6px;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}
.notice-btn:hover {
  background: #f1f5f9;
  border-radius: 50%;
}
</style> 