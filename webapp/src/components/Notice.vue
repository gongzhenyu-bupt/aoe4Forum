<template>
  <div class="notice-page">
    <header class="header">
      <div class="header-container">
        <TopBar />
      </div>
    </header>
    
    <div class="notice-main">
      <aside class="notice-sidebar">
        <div class="sidebar-header">
          <h3>消息中心</h3>
        </div>
        <ul class="sidebar-tabs">
          <li 
            :class="{active: activeTab === 'reply'}" 
            @click="handleTab('reply')"
            class="tab-item"
          >
            <div class="tab-icon">💬</div>
            <div class="tab-content">
              <span class="tab-title">回复我的</span>
              <span class="tab-desc">评论和回复通知</span>
            </div>
          </li>
          <li 
            :class="{active: activeTab === 'like'}" 
            @click="handleTab('like')"
            class="tab-item"
          >
            <div class="tab-icon">👍</div>
            <div class="tab-content">
              <span class="tab-title">收到的赞</span>
              <span class="tab-desc">点赞通知</span>
            </div>
          </li>
          <li 
            :class="{active: activeTab === 'fan'}" 
            @click="handleTab('fan')"
            class="tab-item"
          >
            <div class="tab-icon">👥</div>
            <div class="tab-content">
              <span class="tab-title">新增粉丝</span>
              <span class="tab-desc">关注通知</span>
            </div>
          </li>
        </ul>
      </aside>
      
      <main class="notice-content">
        <div class="content-header">
          <h2>{{ getTabTitle() }}</h2>
          <span class="notice-count">{{ noticeList.length }} 条消息</span>
        </div>
        
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
        
        <div v-else-if="noticeList.length === 0" class="empty-container">
          <div class="empty-icon">📭</div>
          <h3>暂无消息</h3>
          <p>当有人回复、点赞或关注你时，消息会显示在这里</p>
        </div>
        
        <div v-else class="notice-list">
          <div 
            v-for="notice in noticeList" 
            :key="notice.id" 
            class="notice-item"
            :class="{ clickable: activeTab === 'fan' || activeTab === 'reply' }"
            @click="handleNoticeClick(notice)"
          >
            <div class="notice-avatar">
              <img 
                v-if="notice.avatar" 
                :src="getAvatarUrl(notice.avatar)" 
                :alt="notice.nickname || '用户'"
              />
              <div v-else class="avatar-placeholder">
                {{ (notice.nickname || '用户').charAt(0) }}
              </div>
            </div>
            
            <div class="notice-content">
              <div class="notice-header">
                <span class="notice-nickname">{{ notice.nickname || '用户' }}</span>
                <span class="notice-time">{{ formatTime(notice.createTime) }}</span>
              </div>
              <div class="notice-message">
                <span class="message-text">{{ notice.content || notice.msg || '暂无内容' }}</span>
                <span v-if="activeTab === 'fan'" class="follow-action">关注了你</span>
              </div>
            </div>
            
            <div class="notice-action">
              <div class="action-icon">
                <span v-if="activeTab === 'reply'">💬</span>
                <span v-else-if="activeTab === 'like'">👍</span>
                <span v-else-if="activeTab === 'fan'">👥</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import TopBar from './TopBar.vue'
import { getLikeNoticesApi, getCommentNoticesApi, getFollowNoticesApi, getProfileApi } from '../utils/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const activeTab = ref('reply')
const loading = ref(false)
const noticeList = ref<any[]>([])
const userId = ref<number | null>(null)

async function fetchUserId() {
  const cached = localStorage.getItem('userid')
  if (cached) return Number(cached)
  const res = await getProfileApi()
  if ((res.code === 0 || res.code === '0') && res.data && res.data.id) {
    localStorage.setItem('userid', res.data.id)
    return res.data.id
  }
  return null
}

async function fetchNotices() {
  if (!userId.value) return
  loading.value = true
  let res: any
  if (activeTab.value === 'reply') {
    res = await getCommentNoticesApi({ userId: userId.value })
  } else if (activeTab.value === 'like') {
    res = await getLikeNoticesApi({ userId: userId.value })
  } else if (activeTab.value === 'fan') {
    res = await getFollowNoticesApi({ userId: userId.value })
  }
  if (res && (res.code === 0 || res.code === '0') && res.data && res.data.noticeList) {
    noticeList.value = res.data.noticeList
  } else {
    noticeList.value = []
  }
  loading.value = false
}

function handleTab(tab: string) {
  activeTab.value = tab
  fetchNotices()
}

function handleNoticeClick(notice: any) {
  if (notice && notice.businessId) {
    if (activeTab.value === 'reply') {
      // 评论通知：跳转到帖子页面，并传递评论ID作为锚点
      router.push(`/post/${notice.postId}?commentId=${notice.businessId}`)
    } else if (activeTab.value === 'fan') {
      router.push(`/usercenter/${notice.businessId}`)
    }
  }
}

function getTabTitle() {
  const titles = {
    reply: '回复我的',
    like: '收到的赞',
    fan: '新增粉丝'
  }
  return titles[activeTab.value as keyof typeof titles] || '消息'
}

function getAvatarUrl(avatar: string) {
  if (!avatar) return ''
  if (avatar.startsWith('/defaultImg/')) {
    return `http://101.126.22.249:7071${avatar}`
  }
  return `http://101.126.22.249:7071/avatarImg/${avatar.replace(/\\/g, '/').split('/').pop()}`
}

function formatTime(timeStr: string) {
  if (!timeStr) return ''
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

onMounted(async () => {
  userId.value = await fetchUserId()
  fetchNotices()
})
</script>

<style scoped>
.notice-page {
  background: #f8fafc;
  min-height: 100vh;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #fff;
  z-index: 1000;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(0,0,0,0.02);
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  padding: 0 32px;
  display: flex;
  align-items: center;
}

.notice-main {
  display: flex;
  margin-top: 80px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  min-height: calc(100vh - 100px);
}

.notice-sidebar {
  width: 280px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
}

.sidebar-header {
  padding: 24px 24px 16px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.sidebar-tabs {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.tab-item:hover {
  background: #f1f5f9;
}

.tab-item.active {
  background: #eff6ff;
  border-left-color: #3b82f6;
}

.tab-icon {
  font-size: 20px;
  width: 24px;
  text-align: center;
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tab-title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.tab-desc {
  font-size: 12px;
  color: #64748b;
}

.notice-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  border-bottom: 1px solid #e2e8f0;
}

.content-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.notice-count {
  font-size: 14px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 12px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 32px;
  color: #64748b;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f4f6;
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 32px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-container h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.empty-container p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.notice-list {
  padding: 0;
  margin: 0;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 32px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.2s ease;
}

.notice-item:hover {
  background: #f8fafc;
}

.notice-item.clickable {
  cursor: pointer;
}

.notice-avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.notice-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  color: white;
  font-weight: 600;
  font-size: 18px;
}

.notice-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-nickname {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.notice-time {
  font-size: 12px;
  color: #94a3b8;
}

.notice-message {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-text {
  color: #475569;
  font-size: 14px;
  line-height: 1.5;
}

.follow-action {
  background: #dbeafe;
  color: #1e40af;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.notice-action {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f1f5f9;
}

.action-icon {
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .notice-main {
    flex-direction: column;
    margin: 80px 16px 16px 16px;
  }
  
  .notice-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e2e8f0;
  }
  
  .sidebar-tabs {
    display: flex;
    overflow-x: auto;
  }
  
  .tab-item {
    flex-shrink: 0;
    min-width: 120px;
  }
  
  .content-header {
    padding: 16px 20px;
  }
  
  .notice-item {
    padding: 16px 20px;
  }
}
</style> 