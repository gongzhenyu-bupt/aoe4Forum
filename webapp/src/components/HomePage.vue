<template>
  <div class="homepage">
    <!-- 顶部导航栏 -->
    <header class="header">
        <TopBar @login-click="showAuthModal = true" />
    </header>
         <AuthModal v-if="showAuthModal" @close="showAuthModal = false" @success="handleAuthSuccess" />

    <!-- 主要内容区域 -->
    <main class="main">
      <div class="main-container">
        <!-- 左侧内容区 -->
        <div class="content-left">
          <!-- 英雄区域 -->
          <section class="hero-section">
            <div class="hero-image">
              <img src="https://images.pexels.com/photos/1666021/pexels-photo-1666021.jpeg?auto=compress&cs=tinysrgb&w=1000&h=400&fit=crop" alt="Hero Image" />
            </div>
          </section>

          <!-- 热门文章 -->
          <section class="hot-articles-section">
            <div class="section-header">
              <h2 class="section-title">热门文章</h2>
            </div>
            
            <div v-if="loadingHotArticles" class="loading-container">
              <div class="loading-spinner"></div>
              <p>加载中...</p>
            </div>
            
            <div v-else-if="hotArticles.length === 0" class="empty-container">
              <p>暂无热门文章</p>
            </div>
            
            <div v-else class="hot-article-list">
              <article 
                v-for="article in hotArticles" 
                :key="article.id"
                class="hot-article-item"
                @click="goToArticle(article.id)"
              >
                <div class="article-header">
                  <div class="author-info">
                    <div class="author-avatar">
                      <img 
                        v-if="article.avatar" 
                        :src="article.avatar.startsWith('/defaultImg/') ? `http://127.0.0.1:7071${article.avatar}` : `http://127.0.0.1:7071/avatarImg/${article.avatar.replace(/\\/g, '/').split('/').pop()}`" 
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
            <div v-if="!loadingHotArticles" class="load-more">
              <button v-if="hasMoreHotArticles" class="load-more-btn" @click="loadMoreHotArticles">加载更多</button>
              <p v-else class="no-more-text">没有更多了</p>
            </div>
          </section>


        </div>

        <!-- 右侧边栏 -->
        <aside class="sidebar">
                     <!-- 欢迎卡片 -->
           <div class="welcome-card">
             <h3 v-if="isLogin">你好，{{ userInfo.username || '用户' }}！</h3>
             <h3 v-else>欢迎你好！</h3>
             
             <div v-if="isLogin" class="user-stats">
               <div class="stat-item">
                 <span class="stat-label">关注</span>
                 <span class="stat-value">{{ userInfo.followeeNums || 0 }}</span>
               </div>
               <div class="stat-item">
                 <span class="stat-label">粉丝</span>
                 <span class="stat-value">{{ userInfo.followerNums || 0 }}</span>
               </div>
             </div>
             <p v-else>4条人工智能一天</p>
             
             <button v-if="!isLogin" class="welcome-btn" @click="openAuthModal">去登录</button>
             <button v-else class="welcome-btn" @click="goToUserCenter">个人中心</button>
           </div>

          

                     <!-- 最新文章 -->
           <div class="latest-articles">
             <h4>最新文章</h4>
             <div v-if="loadingLatestArticles" class="loading-state">
               <p>加载中...</p>
             </div>
             <div v-else-if="latestArticles.length === 0" class="empty-state">
               <p>暂无最新文章</p>
             </div>
                           <div v-else>
                <div v-for="article in latestArticles" :key="article.id" class="article-item" @click="goToArticle(article.id)">
                  <span class="article-dot">📝</span>
                  <div>
                    <p class="article-mini-title">{{ truncateTitle(article.title) }}</p>
                    <div class="article-mini-meta">
                      <span class="mini-author">{{ article.username }}</span>
                      <span class="mini-stats">💬 {{ article.commentCount || 0 }} 👍 {{ article.likeCount || 0 }}</span>
                    </div>
                  </div>
                </div>
              </div>
           </div>

          


        </aside>
      </div>
         </main>
   </div>
</template>

<script setup lang="ts">
import { inject, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from './TopBar.vue'
import AuthModal from './AuthModal.vue'
import { getLatestArticlesApi, getProfileApi, getUserInfoApi, getHotArticlesApi } from '../utils/api'
import { getCookie } from '../utils/cookie'

const router = useRouter()
const showAuthModal = ref(false)
const openAuthModal = inject('openAuthModal') as () => void
const navigateToArticles = inject('navigateToArticles') as () => void

// 最新文章数据
const latestArticles = ref<any[]>([])
const loadingLatestArticles = ref(false)

// 热门文章数据
const hotArticles = ref<any[]>([])
const loadingHotArticles = ref(false)
const hasMoreHotArticles = ref(true)
const currentPage = ref(1)

// 用户信息
const userInfo = ref<any>({})
const isLogin = ref(false)

// 获取热门文章
const fetchHotArticles = async (reset = false) => {
  if (loadingHotArticles.value) return
  
  loadingHotArticles.value = true
  try {
    let page = 1
    if (!reset) {
      page = currentPage.value + 1
    }
    
    // 检查是否超过5页
    if (page > 5) {
      hasMoreHotArticles.value = false
      loadingHotArticles.value = false
      return
    }
    
    const response = await getHotArticlesApi(page)
    
    if (response.code === '0' || response.code === 0) {
      const newArticles = response.data || []
      
      if (reset) {
        hotArticles.value = newArticles
        currentPage.value = 1
      } else {
        hotArticles.value.push(...newArticles)
        currentPage.value = page
      }
      
      // 检查是否还有更多数据
      hasMoreHotArticles.value = newArticles.length === 10 && page < 5
    }
  } catch (error) {
    console.error('获取热门文章失败:', error)
  } finally {
    loadingHotArticles.value = false
  }
}

// 加载更多热门文章
const loadMoreHotArticles = () => {
  fetchHotArticles(false)
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

// 获取板块标签
const getForumLabel = (forum: string) => {
  const categories = [
    { label: '单排攻略', forum: 'solo-guide' },
    { label: '组排攻略', forum: 'team-guide' },
    { label: '强度讨论', forum: 'meta-discuss' },
    { label: '闲聊', forum: 'chat' },
    { label: '封神榜', forum: 'hall-of-fame' }
  ]
  const category = categories.find(c => c.forum === forum)
  return category ? category.label : forum
}

// 获取最新文章
const fetchLatestArticles = async () => {
  if (loadingLatestArticles.value) return
  
  loadingLatestArticles.value = true
  try {
    const response = await getLatestArticlesApi(10) // 后端默认返回10条，我们请求10条
    if (response.code === '0' || response.code === 0) {
      // 只取前5条显示
      latestArticles.value = (response.data || []).slice(0, 5)
    }
  } catch (error) {
    console.error('获取最新文章失败:', error)
  } finally {
    loadingLatestArticles.value = false
  }
}

// 截断标题文本
const truncateTitle = (title: string, maxLength: number = 20) => {
  if (title.length <= maxLength) return title
  return title.substring(0, maxLength) + '...'
}

// 跳转到文章详情
const goToArticle = (articleId: number) => {
  router.push(`/post/${articleId}`)
}

// 检查登录状态并获取用户信息
const checkLoginAndGetUserInfo = async () => {
  const token = getCookie('token')
  if (token) {
    isLogin.value = true
    try {
      // 先获取当前用户基本信息
      const profileResponse = await getProfileApi()
      if (profileResponse.code === '0' || profileResponse.code === 0) {
        const currentUser = profileResponse.data || {}
        
        // 通过用户ID获取详细信息
        if (currentUser.id) {
          const userInfoResponse = await getUserInfoApi(currentUser.id.toString())
          if (userInfoResponse.code === '0' || userInfoResponse.code === 0) {
            userInfo.value = userInfoResponse.data || {}
          } else {
            // 如果获取详细信息失败，使用基本信息
            userInfo.value = currentUser
          }
        } else {
          userInfo.value = currentUser
        }
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  } else {
    isLogin.value = false
    userInfo.value = {}
  }
}

// 跳转到个人中心
const goToUserCenter = () => {
  // 从localStorage中获取userid
  const userid = localStorage.getItem('userid')
  if (userid) {
    router.push(`/${userid}`)
  } else {
    // 如果没有userid，尝试从用户信息中获取
    if (userInfo.value && userInfo.value.id) {
      localStorage.setItem('userid', userInfo.value.id.toString())
      router.push(`/${userInfo.value.id}`)
    } else {
      // 如果都没有，可以显示错误提示或跳转到默认页面
      console.error('无法获取用户ID')
      router.push('/')
    }
  }
}

// 处理登录成功
const handleAuthSuccess = async () => {
  showAuthModal.value = false
  await checkLoginAndGetUserInfo()
}

 

// 组件挂载时获取最新文章
onMounted(() => {
  // 重置页码，确保每次刷新都从第1页开始
  currentPage.value = 1
  hasMoreHotArticles.value = true
  
  fetchLatestArticles()
  fetchHotArticles(true)
  checkLoginAndGetUserInfo()
})
</script>

<style scoped>
.homepage {
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

/* 英雄区域 */
.hero-section {
  margin-bottom: 32px;
}

.hero-image {
  border-radius: 12px;
  overflow: hidden;
  height: 240px;
}

.hero-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 章节样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.more-link {
  color: #3b82f6;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.more-link:hover {
  text-decoration: underline;
}

/* 热门文章样式 */
.hot-articles-section {
  margin-bottom: 32px;
}

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

.hot-article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hot-article-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;
}

.hot-article-item:hover {
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

/* 加载更多样式 */
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

.user-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
  padding: 12px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.8;
  color: #fbbf24;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #fbbf24;
}

 

.latest-articles {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.latest-articles h4 {
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 12px;
}

.article-item {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.article-item:hover {
  background-color: #f8fafc;
  border-radius: 6px;
  padding: 8px 6px;
  margin: 0 -6px;
}

.article-item:last-child {
  border-bottom: none;
}

.article-dot {
  flex-shrink: 0;
  font-size: 14px;
}

.article-mini-title {
  font-size: 13px;
  color: #374151;
  margin-bottom: 4px;
  line-height: 1.3;
  text-align: left;
  word-break: break-word;
}

.article-mini-meta {
  font-size: 11px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-author {
  font-weight: 500;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 20px 0;
  color: #64748b;
  font-size: 14px;
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
  
  .article-card {
    flex-direction: column;
  }
  
  .article-image {
    width: 100%;
    height: 160px;
  }
}


</style>