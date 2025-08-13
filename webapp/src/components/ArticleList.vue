<template>
  <div class="article-list-page">
    <!-- 顶部导航栏 -->
    <header class="header">
      <TopBar @login-click="showAuthModal = true" />
    </header>

    <!-- 认证弹窗 -->
    <AuthModal v-if="showAuthModal" @close="showAuthModal = false" @success="showAuthModal = false" />

    <!-- 主要内容区域 -->
    <main class="main">
      <div class="main-container">
        <!-- 左侧内容区 -->
        <div class="content-left">
          <!-- 分类标签 -->
          <section class="category-section">
            <div class="category-bar">
              <button
                v-for="cat in categories"
                :key="cat.forum"
                :class="['category-btn', { active: activeCategory === cat.forum }]"
                @click="switchCategory(cat.forum)"
              >
                {{ cat.label }}
              </button>
            </div>
          </section>

          <!-- 文章列表 -->
          <section class="articles-section">
            <div v-if="loading" class="loading-container">
              <div class="loading-spinner"></div>
              <p>加载中...</p>
            </div>
            
            <div v-else-if="articles.length === 0" class="empty-container">
              <p>暂无文章</p>
            </div>
            
            <div v-else class="article-list">
              <article 
                v-for="article in articles" 
                :key="article.id"
                class="article-item"
                @click="viewArticle(article)"
              >
                <div class="article-header">
                                     <div class="author-info">
                     <div class="author-avatar">
                       <img 
                         v-if="article.avatar" 
                         :src="getAvatarUrlSync(article.avatar)" 
                         :alt="article.userName"
                         class="avatar-img"
                       />
                       <span v-else>{{ article.userName.charAt(0) }}</span>
                     </div>
                    <div class="author-details">
                      <span class="author-name">{{ article.userName }}</span>
                      <span class="publish-time">{{ formatTime(article.createTime) }}</span>
                    </div>
                  </div>
                  <div class="article-tags">
                    <span v-if="article.forum" class="tag">{{ getForumLabel(article.forum) }}</span>
                    <span v-if="article.status === 1" class="tag featured">置顶</span>
                  </div>
                </div>

                <div class="article-content">
                  <h3 class="article-title">{{ article.title }}</h3>
                  <p class="article-excerpt">
                    {{ article.postAbstract ?? article.title }}
                  </p>
                </div>

                <div class="article-footer">
                  <div class="article-stats">
                    <span class="stat-item">
                      👁 {{ formatNumber(article.pageViewCount) }}
                    </span>
                    <span class="stat-item">
                      👍 {{ formatNumber(article.likeCount) }}
                    </span>
                    <span class="stat-item">
                      💬 {{ formatNumber(article.commentCount) }}
                    </span>
                  </div>
                </div>
              </article>
            </div>

            <!-- 加载更多 -->
            <div v-if="!loading" class="load-more">
              <button v-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</button>
              <p v-else class="no-more-text">没有更多了</p>
            </div>
          </section>
        </div>

        <!-- 右侧边栏 -->
        <aside class="sidebar">
          <div v-if="!isLogin" class="welcome-card">
            <h3>欢迎你好！</h3>
            <p>点亮人生的每一天</p>
            <button class="welcome-btn" @click="openAuthModal">去登录</button>
          </div>

          <!-- 统计信息 -->
          <div class="stats-card">
            <h4>统计</h4>
            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-label">文章</span>
                <span class="stat-value">{{ forumStatus?.postCount || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">评论</span>
                <span class="stat-value">{{ forumStatus?.commentCount || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">访客</span>
                <span class="stat-value">{{ formatNumber(forumStatus?.viewCount || 0) }}</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, inject, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from './TopBar.vue'
import AuthModal from './AuthModal.vue'
import { getArticlesApi, getProfileApi, getForumStatusApi } from '../utils/api'  // 导入新接口
import type { Article } from '../types/api'
import AvatarCache from '../utils/avatarCache'

// 注入全局方法
const openAuthModal = inject('openAuthModal') as () => void

// 组件状态
const loading = ref(false)
const articles = ref<Article[]>([])
const hasMore = ref(true)
const showAuthModal = ref(false)
const isLogin = ref(false)
const userInfo = ref<any>({})
const defaultAvatar = 'https://img1.imgtp.com/2023/07/21/2F1QKQbA.png' // 占位头像
const router = useRouter()
const forumStatus = ref<any>({})

// 获取论坛统计数据
const fetchForumStatus = async () => {
  try {
    const response = await getForumStatusApi()
    if (response.code === '0' || response.code === 0) {
      forumStatus.value = response.data || {}
    }
  } catch (error) {
    console.error('获取论坛统计数据失败:', error)
  }
}

// 浏览状态管理
const browseState = reactive({
  category: 'all',
  offset: 0,
  scrollPosition: 0
})

// 分页参数
const pagination = reactive({
  offset: browseState.offset,
  limit: 10
})

// 分类选项
const categories = [
  { label: '全部', forum: 'all' },
  { label: '单排攻略', forum: 'solo-guide' },
  { label: '组排攻略', forum: 'team-guide' },
  { label: '强度讨论', forum: 'meta-discuss' },
  { label: '闲聊', forum: 'chat' },
  { label: '封神榜', forum: 'hall-of-fame' }
]

const activeCategory = ref(browseState.category)

// 获取文章列表
const fetchArticles = async (reset = false) => {
  if (loading.value) return
  
  loading.value = true
  
  try {
    const params = {
      forum: activeCategory.value,
      offset: reset ? 0 : pagination.offset,
      limit: pagination.limit
    }
    
    const response = await getArticlesApi(params)
    
    if (response.code === '0' || response.code === 0) {
      const newArticles = response.data || []
      
      if (reset) {
        articles.value = newArticles
        pagination.offset = 0
      } else {
        articles.value.push(...newArticles)
      }
      
      pagination.offset += newArticles.length
      
      // 检查是否还有更多数据
      hasMore.value = newArticles.length === pagination.limit
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 保存浏览状态
const saveBrowseState = () => {
  browseState.category = activeCategory.value
  browseState.offset = pagination.offset
  browseState.scrollPosition = window.scrollY
  sessionStorage.setItem('articleListState', JSON.stringify(browseState))
}

// 恢复浏览状态
const restoreBrowseState = () => {
  const savedState = sessionStorage.getItem('articleListState')
  if (savedState) {
    const state = JSON.parse(savedState)
    browseState.category = state.category || 'all'
    browseState.offset = state.offset || 0
    browseState.scrollPosition = state.scrollPosition || 0
    
    activeCategory.value = browseState.category
    pagination.offset = browseState.offset
  }
}

// 切换分类
const switchCategory = (forum: string) => {
  activeCategory.value = forum
  saveBrowseState()
  fetchArticles(true)
}

// 加载更多
const loadMore = () => {
  fetchArticles(false)
  saveBrowseState()
}

// 查看文章详情
const viewArticle = (article: Article) => {
  saveBrowseState()
  router.push(`/post/${article.id}`)
}

// 返回首页
const goHome = () => {
  // router.push('/')
  console.log('返回首页')
}

// 处理创作中心
const handleCreateAction = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    openAuthModal()
  } else {
    console.log('进入创作中心')
  }
}

// 格式化时间
const formatTime = (timeStr: string) => {
  const time = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - time.getTime()
  
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor(diff / (1000 * 60))
  
  if (days > 0) {
    return `${days}天前`
  } else if (hours > 0) {
    return `${hours}小时前`
  } else if (minutes > 0) {
    return `${minutes}分钟前`
  } else {
    return '刚刚'
  }
}

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}w`
  } else if (num >= 1000) {
    return `${(num / 1000).toFixed(1)}k`
  }
  return num.toString()
}

// 获取头像URL（使用缓存）
const getAvatarUrl = async (avatar: string) => {
  if (!avatar) return defaultAvatar
  return await AvatarCache.getAvatarUrl(avatar)
}

// 同步获取头像URL（用于模板）
const getAvatarUrlSync = (avatar: string) => {
  if (!avatar) return defaultAvatar
  // 这里可以添加一个简单的缓存检查，但主要依赖AvatarCache的内部缓存
  return avatar.startsWith('/defaultImg/') 
    ? `http://101.126.22.249:7071${avatar}` 
    : `http://101.126.22.249:7071/avatarImg/${avatar.replace(/\\/g, '/').split('/').pop()}`
}

// 获取板块标签
const getForumLabel = (forum: string) => {
  const category = categories.find(c => c.forum === forum)
  return category ? category.label : forum
}

async function checkLogin() {
  try {
    const res = await getProfileApi()
    if (res.code === 0 || res.code === '0') {
      isLogin.value = true
      userInfo.value = res.data
      // 新增：同步userid
      if (res.data.id) localStorage.setItem('userid', res.data.id)
    } else {
      isLogin.value = false
      userInfo.value = {}
    }
  } catch {
    isLogin.value = false
    userInfo.value = {}
  }
}

// 滚动事件处理
const handleScroll = () => {
  browseState.scrollPosition = window.scrollY
  // 节流保存，避免频繁保存
  clearTimeout((window as any).scrollTimeout)
  ;(window as any).scrollTimeout = setTimeout(() => {
    saveBrowseState()
  }, 100)
}

// 组件挂载时获取数据
onMounted(() => {
  // 获取论坛统计数据
  fetchForumStatus()

  // 恢复浏览状态
  restoreBrowseState()
  
  // 获取文章列表
  fetchArticles(true)
  
  // 检查登录状态
  checkLogin()
  
  // 恢复滚动位置
  nextTick(() => {
    if (browseState.scrollPosition > 0) {
      window.scrollTo(0, browseState.scrollPosition)
    }
  })
  
  // 添加滚动事件监听
  window.addEventListener('scroll', handleScroll)
})

// 组件卸载时保存状态
onUnmounted(() => {
  saveBrowseState()
  // 移除滚动事件监听
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.article-list-page {
  min-height: 100vh;
  background-color: #f8fafc;
}

/* 头部样式 */
.header {
  background: white;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.logo {
  font-size: 24px;
  font-weight: bold;
}

.logo-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 8px 12px;
  min-width: 200px;
}

.search-input {
  border: none;
  background: none;
  outline: none;
  flex: 1;
  font-size: 14px;
}

.search-btn {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
}

.create-btn {
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease;
}

.create-btn:hover {
  background: #2563eb;
}

.login-btn {
  background: transparent;
  color: #3b82f6;
  border: 1px solid #3b82f6;
  border-radius: 6px;
  padding: 8px 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-btn:hover {
  background: #3b82f6;
  color: white;
}

/* 主要内容区域 */
.main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.main-container {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
}

/* 分类标签 */
.category-section {
  margin-bottom: 16px;
}

.category-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.category-btn {
  background: #f1f5f9;
  border: none;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-btn:hover,
.category-btn.active {
  background: #3b82f6;
  color: white;
}

/* 文章列表 */
.loading-container,
.empty-container {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f4f6;
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;
}

.article-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.publish-time {
  font-size: 12px;
  color: #64748b;
}

.article-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.tag.vue {
  background: #dcfce7;
  color: #166534;
}

.tag.spring {
  background: #dbeafe;
  color: #1e40af;
}

.tag.featured {
  background: #fef3c7;
  color: #d97706;
}

.article-content {
  margin-bottom: 16px;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  line-height: 1.4;
}

.article-excerpt {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}

.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.article-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.load-more {
  text-align: center;
  margin-top: 24px;
}

.load-more-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 24px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.load-more-btn:hover {
  background: #e2e8f0;
  border-color: #cbd5e1;
}

.no-more-text {
  text-align: center;
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

/* 侧边栏样式 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
}

.welcome-card h3 {
  margin-bottom: 8px;
  font-size: 18px;
}

.welcome-card p {
  margin-bottom: 16px;
  opacity: 0.9;
  font-size: 14px;
}

.welcome-btn {
  background: white;
  color: #667eea;
  border: none;
  border-radius: 6px;
  padding: 8px 20px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.welcome-btn:hover {
  transform: translateY(-1px);
}

.stats-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-top: 55px;
}

.stats-card h4 {
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  text-align: center;
}

.stats-grid .stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
  }
  
  .search-box {
    min-width: 150px;
  }
  
  .main {
    padding: 16px;
  }
  
  .main-container {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .category-bar {
    overflow-x: auto;
    padding-bottom: 8px;
  }
  
  .article-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>