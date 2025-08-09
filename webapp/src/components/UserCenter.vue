<template>
  <div class="usercenter-page">
    <TopBar />
    <div class="usercenter-main">
      <UserInfoCard :userid="userid" />
      <div class="user-posts-area">
        <div class="posts-header">
          <h3>发布的帖子</h3>
          <span class="post-count">{{ totalCount }} 篇</span>
        </div>
        
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
        
        <div v-else-if="posts.length === 0" class="empty-container">
          <p>暂无发布的帖子</p>
        </div>
        
        <div v-else class="posts-list">
          <div 
            v-for="post in posts" 
            :key="post.id"
            class="post-item"
            @click="viewPost(post)"
          >
            <div class="post-header">
              <div class="post-meta">
                <span class="post-forum">{{ getForumLabel(post.forum) }}</span>
                <span class="post-time">{{ formatTime(post.createTime) }}</span>
              </div>
              <div class="post-status" v-if="post.status === 1">
                <span class="status-tag">置顶</span>
              </div>
            </div>
            
            <div class="post-content">
              <h4 class="post-title">{{ post.title }}</h4>
              <p class="post-excerpt">{{ post.postAbstract || post.title }}</p>
            </div>
            
            <div class="post-footer">
              <div class="post-stats">
                <span class="stat-item">
                  👁 {{ formatNumber(post.pageViewCount) }}
                </span>
                <span class="stat-item">
                  👍 {{ formatNumber(post.likeCount) }}
                </span>
                <span class="stat-item">
                  💬 {{ formatNumber(post.commentCount) }}
                </span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 加载更多 -->
        <div v-if="!loading && posts.length > 0" class="load-more">
          <button v-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</button>
          <p v-else class="no-more-text">没有更多了</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ref, watch, onMounted } from 'vue'
import TopBar from './TopBar.vue'
import UserInfoCard from './UserInfoCard.vue'
import { queryPostsByUserIdApi } from '../utils/api'
import type { Article } from '../types/api'

const route = useRoute()
const router = useRouter()
const userid = ref(route.params.userid as string)

// 帖子列表状态
const posts = ref<Article[]>([])
const loading = ref(false)
const totalCount = ref(0)
const hasMore = ref(true)
const offset = ref(0)
const limit = 10

// 获取用户帖子列表
const fetchUserPosts = async (reset = false) => {
  if (loading.value) return
  
  loading.value = true
  try {
    const currentOffset = reset ? 0 : offset.value
    const response = await queryPostsByUserIdApi({
      userId: Number(userid.value),
      offset: currentOffset,
      limit: limit
    })
    
    if (response.code === '0' || response.code === 0) {
      const newPosts = response.data.posts || []
      totalCount.value = response.data.totalCount || 0
      hasMore.value = response.data.hasMore || false
      
      if (reset) {
        posts.value = newPosts
        offset.value = 0
      } else {
        posts.value.push(...newPosts)
      }
      
      offset.value += newPosts.length
    }
  } catch (error) {
    console.error('获取用户帖子失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  fetchUserPosts(false)
}

// 查看帖子详情
const viewPost = (post: Article) => {
  router.push(`/post/${post.id}`)
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

// 获取论坛标签
const getForumLabel = (forum: string) => {
  const forumLabels: Record<string, string> = {
    'solo-guide': '单排攻略',
    'team-guide': '组排攻略',
    'meta-discuss': '强度讨论',
    'chat': '闲聊',
    'hall-of-fame': '封神榜'
  }
  return forumLabels[forum] || forum
}

// 监听路由变化
watch(
  () => route.params.userid,
  (newVal) => {
    userid.value = newVal as string
    fetchUserPosts(true)
  }
)

// 组件挂载时获取数据
onMounted(() => {
  fetchUserPosts(true)
})
</script>

<style scoped>
.usercenter-page {
  min-height: 100vh;
  background: #f8fafc;
}

.usercenter-main {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-posts-area {
  width: 100%;
  margin-top: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  padding: 24px;
}

.posts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.posts-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.post-count {
  font-size: 14px;
  color: #64748b;
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

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e2e8f0;
}

.post-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.post-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.post-forum {
  background: #e0e7ff;
  color: #3730a3;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.post-time {
  font-size: 12px;
  color: #64748b;
}

.post-status {
  display: flex;
  gap: 6px;
}

.status-tag {
  background: #fef3c7;
  color: #d97706;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.post-content {
  margin-bottom: 12px;
}

.post-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.post-excerpt {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  font-size: 12px;
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
</style>
