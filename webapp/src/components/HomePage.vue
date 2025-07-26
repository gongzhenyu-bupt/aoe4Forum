<template>
  <div class="homepage">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-container">
        <div class="logo">
          <span class="logo-text">NS</span>
        </div>
        <TopBar @login-click="showAuthModal = true" />
      </div>
    </header>
    <AuthModal v-if="showAuthModal" @close="showAuthModal = false" @success="showAuthModal = false" />

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

          <!-- 精选问答 -->
          <section class="qa-section">
            <div class="section-header">
              <h2 class="section-title">精选问答</h2>
              <a href="#" class="more-link">查看更多 →</a>
            </div>
            
            <div class="qa-list">
              <div class="qa-item">
                <div class="qa-avatar">
                  <span class="avatar-text">南</span>
                </div>
                <div class="qa-content">
                  <div class="qa-meta">
                    <span class="qa-author">南生论坛社</span>
                    <span class="qa-tag">优秀</span>
                  </div>
                  <h4 class="qa-title">南生论坛文章审核功能是如何实现的呢？</h4>
                  <p class="qa-excerpt">问题描述：去除敏感词，还有能力分为对文件审核的问题？</p>
                </div>
              </div>

              <div class="qa-item">
                <div class="qa-avatar">
                  <span class="avatar-text">测</span>
                </div>
                <div class="qa-content">
                  <div class="qa-meta">
                    <span class="qa-author">测试管理员</span>
                    <span class="qa-tag">1个月</span>
                  </div>
                  <h4 class="qa-title">测试一下</h4>
                  <p class="qa-excerpt">问题描述：测试问题</p>
                </div>
              </div>

              <div class="qa-item">
                <div class="qa-avatar">
                  <span class="avatar-text">测</span>
                </div>
                <div class="qa-content">
                  <div class="qa-meta">
                    <span class="qa-author">测试管理员</span>
                    <span class="qa-tag">2个月</span>
                  </div>
                  <h4 class="qa-title">测试发布一个问题</h4>
                  <p class="qa-excerpt">问题描述：本问题仅仅是测试问题</p>
                </div>
              </div>

              <div class="qa-item">
                <div class="qa-avatar">
                  <span class="avatar-text">马</span>
                </div>
                <div class="qa-content">
                  <div class="qa-meta">
                    <span class="qa-author">马高兴</span>
                    <span class="qa-tag">1个月</span>
                  </div>
                  <h4 class="qa-title">H2O 生成</h4>
                  <p class="qa-excerpt">这在有两种科技，氢 oxygen 和氧 hydrogen，你的目标...</p>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- 右侧边栏 -->
        <aside class="sidebar">
          <!-- 欢迎卡片 -->
          <div class="welcome-card">
            <h3>欢迎你好！</h3>
            <p>4条人工智能一天</p>
            <button class="welcome-btn" @click="openAuthModal">去登录</button>
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
            <div class="article-item">
              <span class="article-dot">📝</span>
              <div>
                <p class="article-mini-title">现场一记起把档案填了下去</p>
                <div class="article-mini-meta">
                  <span class="mini-author">测试管理员</span>
                  <span class="mini-stats">💬 3 👍 0</span>
                </div>
              </div>
            </div>

            <div class="article-item">
              <span class="article-dot">📝</span>
              <div>
                <p class="article-mini-title">This is a TEST</p>
                <div class="article-mini-meta">
                  <span class="mini-author">AdverHere</span>
                  <span class="mini-stats">💬 68 👍 0</span>
                </div>
              </div>
            </div>

            <div class="article-item">
              <span class="article-dot">📝</span>
              <div>
                <p class="article-mini-title">如何进行南生论坛管理呢？</p>
                <div class="article-mini-meta">
                  <span class="mini-author">马高兴</span>
                  <span class="mini-stats">💬 105 👍 2</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 测试效果 -->
          <div class="test-section">
            <h4>测试效果</h4>
            <div class="test-item">
              <span class="test-avatar">G</span>
              <div>
                <p class="test-title">Gimma</p>
                <span class="test-stats">💬 26 👍 0</span>
              </div>
            </div>
          </div>

          <!-- 最新问答 -->
          <div class="latest-qa">
            <h4>最新问答</h4>
            <div class="qa-mini-item">
              <span class="qa-star">⭐</span>
              <div>
                <p class="qa-mini-title">南生论坛文章审核功能是如何实现的呢？</p>
                <div class="qa-mini-meta">
                  <span class="qa-mini-author">南生论坛社</span>
                  <span class="qa-mini-stats">💬 9</span>
                </div>
              </div>
            </div>

            <div class="qa-mini-item">
              <span class="qa-star">⭐</span>
              <div>
                <p class="qa-mini-title">测试一下</p>
                <div class="qa-mini-meta">
                  <span class="qa-mini-author">测试管理员</span>
                  <span class="qa-mini-stats">💬 40</span>
                </div>
              </div>
            </div>

            <div class="qa-mini-item">
              <span class="qa-star">⭐</span>
              <div>
                <p class="qa-mini-title">测试发布一个问题</p>
                <div class="qa-mini-meta">
                  <span class="qa-mini-author">测试管理员</span>
                  <span class="qa-mini-stats">💬 75</span>
                </div>
              </div>
            </div>

            <div class="qa-mini-item">
              <span class="qa-star">⭐</span>
              <div>
                <p class="qa-mini-title">各种之文</p>
                <div class="qa-mini-meta">
                  <span class="qa-mini-author">Joseoh</span>
                  <span class="qa-mini-stats">💬 90</span>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </main>

    <section class="api-test-area">
      <h2>接口测试区</h2>
      <p>这里用于测试已完成但暂未有正式位置的接口。</p>
      <AvatarUpload />
    </section>
  </div>
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import TopBar from './TopBar.vue'
import AuthModal from './AuthModal.vue'
import AvatarUpload from './AvatarUpload.vue'

const showAuthModal = ref(false)
const openAuthModal = inject('openAuthModal') as () => void
const navigateToArticles = inject('navigateToArticles') as () => void

const handleCreateAction = () => {
  // 检查是否需要登录权限
  const token = localStorage.getItem('token')
  if (!token) {
    openAuthModal()
  } else {
    // 执行创作中心逻辑
    console.log('进入创作中心')
  }
}
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

/* 问答区域 */
.qa-section {
  margin-bottom: 32px;
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qa-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 12px;
  transition: transform 0.2s ease;
}

.qa-item:hover {
  transform: translateY(-1px);
}

.qa-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  flex-shrink: 0;
}

.qa-content {
  flex: 1;
}

.qa-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
}

.qa-author {
  font-weight: 500;
  color: #374151;
}

.qa-tag {
  background: #fbbf24;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
}

.qa-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.qa-excerpt {
  font-size: 13px;
  color: #64748b;
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

.latest-articles, .test-section, .latest-qa {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.latest-articles h4, .test-section h4, .latest-qa h4 {
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 12px;
}

.article-item, .test-item, .qa-mini-item {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}

.article-item:last-child, .test-item:last-child, .qa-mini-item:last-child {
  border-bottom: none;
}

.article-dot, .test-avatar, .qa-star {
  flex-shrink: 0;
  font-size: 14px;
}

.test-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.article-mini-title, .test-title, .qa-mini-title {
  font-size: 13px;
  color: #374151;
  margin-bottom: 4px;
  line-height: 1.3;
}

.article-mini-meta, .test-stats, .qa-mini-meta {
  font-size: 11px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-author, .qa-mini-author {
  font-weight: 500;
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

.api-test-area {
  margin: 40px auto;
  max-width: 600px;
  padding: 24px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafbfc;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
.api-test-area h2 {
  margin-bottom: 12px;
}
</style>