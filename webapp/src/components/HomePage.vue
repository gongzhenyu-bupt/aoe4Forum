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

          <!-- 精选文章 -->
          <section class="featured-section">
            <div class="section-header">
              <h2 class="section-title">精选文章</h2>
              <a href="#" class="more-link" @click="navigateToArticles">查看更多 →</a>
            </div>
            
            <div class="article-list">
              <article class="article-card featured">
                <div class="article-image">
                  <img src="https://images.pexels.com/photos/1181263/pexels-photo-1181263.jpeg?auto=compress&cs=tinysrgb&w=400&h=200&fit=crop" alt="Article" />
                </div>
                <div class="article-content">
                  <div class="article-meta">
                    <span class="author">马高兴</span>
                    <span class="date">2天前</span>
                    <div class="tags">
                      <span class="tag">SpringBoot</span>
                      <span class="tag">Vue.js</span>
                      <span class="tag">置顶</span>
                    </div>
                  </div>
                  <h3 class="article-title">【后端】安装部署教程</h3>
                  <p class="article-excerpt">产品设计下载程序安装部署以及核心目录介绍教程，如果遇到无法连接数据库的问题请看 #QQ: 924 8184的解答视频...</p>
                </div>
              </article>

              <article class="article-card">
                <div class="article-image">
                  <img src="https://images.pexels.com/photos/1181298/pexels-photo-1181298.jpeg?auto=compress&cs=tinysrgb&w=400&h=200&fit=crop" alt="Article" />
                </div>
                <div class="article-content">
                  <div class="article-meta">
                    <span class="author">马高兴</span>
                    <span class="date">2天前</span>
                    <div class="tags">
                      <span class="tag">Vue.js</span>
                      <span class="tag">SpringBoot</span>
                      <span class="tag">置顶</span>
                    </div>
                  </div>
                  <h3 class="article-title">【前端】安装部署教程</h3>
                  <p class="article-excerpt">产品设计下载程序安装部署以及核心目录介绍教程，如果遇到无法连接数据库的问题请看 #QQ: 924 8184的解答视频...</p>
                </div>
              </article>
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

          <!-- 权限提示 -->
          <div class="permission-notice">
            <h4>获取南生论坛使用权限</h4>
            <p class="notice-text">系统公告 <span class="notice-icon">📢</span></p>
            <p class="notice-desc">普通用户可见系统公告</p>
            <a href="#" class="notice-link">查看更多 →</a>
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
import { getLatestArticlesApi, getProfileApi, getUserInfoApi } from '../utils/api'
import { getCookie } from '../utils/cookie'

const router = useRouter()
const showAuthModal = ref(false)
const openAuthModal = inject('openAuthModal') as () => void
const navigateToArticles = inject('navigateToArticles') as () => void

// 最新文章数据
const latestArticles = ref<any[]>([])
const loadingLatestArticles = ref(false)

// 用户信息
const userInfo = ref<any>({})
const isLogin = ref(false)

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

const handleCreateAction = () => {
  // 检查是否需要登录权限
  const token = getCookie('token')
  if (!token) {
    openAuthModal()
  } else {
    // 执行创作中心逻辑
    console.log('进入创作中心')
  }
}

// 组件挂载时获取最新文章
onMounted(() => {
  fetchLatestArticles()
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

/* 文章卡片 */
.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.article-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  gap: 16px;
  padding: 16px;
}

.article-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.article-card.featured {
  border-left: 4px solid #10b981;
}

.article-image {
  flex-shrink: 0;
  width: 160px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
}

.article-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-content {
  flex: 1;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #64748b;
}

.author {
  font-weight: 500;
}

.tags {
  display: flex;
  gap: 6px;
}

.tag {
  background: #e0f2fe;
  color: #0369a1;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.tag:last-child {
  background: #fef3c7;
  color: #d97706;
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  line-height: 1.4;
}

.article-excerpt {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
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
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
}

.permission-notice {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.permission-notice h4 {
  color: #f59e0b;
  font-size: 14px;
  margin-bottom: 12px;
}

.notice-text {
  font-size: 13px;
  color: #374151;
  margin-bottom: 4px;
}

.notice-icon {
  margin-left: 4px;
}

.notice-desc {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.notice-link {
  color: #3b82f6;
  text-decoration: none;
  font-size: 12px;
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