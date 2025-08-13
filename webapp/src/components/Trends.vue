<template>
  <div class="trends-wrapper">
    <header class="header">
      <div class="header-container">
        <TopBar />
      </div>
    </header>
    <h2>关注动态</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <div v-if="posts.length === 0" class="empty">暂无动态</div>
      <div v-else class="article-list">
        <article 
          v-for="post in posts" 
          :key="post.id"
          class="article-item"
          @click="viewPost(post)"
        >
          <div class="article-header">
            <div class="author-info">
              <div class="author-avatar">
                <img 
                  v-if="post.avatar" 
                  :src="getAvatarUrl(post.avatar)" 
                  :alt="post.userName"
                  class="avatar-img"
                />
                <span v-else>{{ post.userName?.charAt(0) || '?' }}</span>
              </div>
              <div class="author-details">
                <span class="author-name">{{ post.userName }}</span>
                <span class="publish-time">{{ formatTime(post.createTime) }}</span>
              </div>
            </div>
            <div class="article-tags">
              <span class="tag" v-if="post.forum">{{ getForumLabel(post.forum) }}</span>
              <span v-if="post.status === 1" class="tag featured">置顶</span>
            </div>
          </div>
          <div class="article-content">
            <h3 class="article-title">{{ post.title }}</h3>
            <p class="article-excerpt">
              {{ post.postAbstract ?? post.title }}
            </p>
          </div>
          <div class="article-footer">
            <div class="article-stats">
              <span class="stat-item">👁 {{ post.pageViewCount }}</span>
              <span class="stat-item">👍 {{ post.likeCount }}</span>
              <span class="stat-item">💬 {{ post.commentCount }}</span>
            </div>
          </div>
        </article>
      </div>
      <div v-if="hasMore && !loading" class="load-more">
        <button @click="loadMore">加载更多</button>
      </div>
      <div v-else class="no-more">没有更多了</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { pullFeedApi } from '../utils/api'
import { getProfileApi } from '../utils/api'
import { useRouter } from 'vue-router'
import TopBar from './TopBar.vue'

const posts = ref<any[]>([])
const loading = ref(true)
const hasMore = ref(true)
let lastId = ref<string>('')
let lastCreateTime = ref<string>('')
let userId = ref<number | null>(null)
const router = useRouter()

// 获取头像URL
const getAvatarUrl = (avatar: string) => {
  if (!avatar) return 'https://img1.imgtp.com/2023/07/21/2F1QKQbA.png'
  // 判断是否是默认头像路径
  if (avatar.startsWith('/defaultImg/')) {
    return `http://127.0.0.1:7071${avatar}`
  }
  // 只取文件名，拼接为Spring Boot静态资源URL
  const filename = avatar.replace(/\\/g, '/').split('/').pop()
  return filename ? `http://127.0.0.1:7071/avatarImg/${filename}` : 'https://img1.imgtp.com/2023/07/21/2F1QKQbA.png'
}

function formatTime(time: string) {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 19)
}

// 获取论坛标签文本
const getForumLabel = (forum: string) => {
  switch (forum) {
    case 'solo-guide':
      return '单排攻略'
    case 'team-guide':
      return '组排攻略'
    case 'meta-discuss':
      return '强度讨论'
    case 'chat':
      return '闲聊'
    case 'hall-of-fame':
      return '封神榜'
    default:
      return forum
  }
}

async function fetchUserId() {
  // 优先本地缓存
  const cached = localStorage.getItem('userid')
  if (cached) return Number(cached)
  // 拉取 profile
  const res = await getProfileApi()
  if ((res.code === 0 || res.code === '0') && res.data && res.data.id) {
    localStorage.setItem('userid', res.data.id)
    return res.data.id
  }
  return null
}

async function fetchFeed(isInit = false) {
  if (!userId.value) return
  loading.value = true
  const params: any = {
    userId: userId.value,
    lastId: lastId.value,
    lastCreateTime: lastCreateTime.value,
  }
  const res: any = await pullFeedApi(params)
  if (res.code === '0' && res.data && res.data.posts) {
    if (isInit) posts.value = []
    posts.value.push(...res.data.posts)
    lastId.value = res.data.lastId
    lastCreateTime.value = res.data.lastCreateTime
    hasMore.value = res.data.posts.length > 0
  } else {
    hasMore.value = false
  }
  loading.value = false
}

async function loadMore() {
  await fetchFeed(false)
}

function viewPost(post: any) {
  router.push(`/post/${post.id}`)
}

onMounted(async () => {
  userId.value = await fetchUserId()
  lastId.value = ''
  lastCreateTime.value = ''
  await fetchFeed(true)
})
</script>

<style scoped>
.trends-wrapper {
  max-width: 700px;
  margin: 32px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  padding: 32px 24px;
}
.post-item {
  border-bottom: 1px solid #eee;
  padding: 16px 0;
}
.post-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 8px;
}
.post-meta {
  color: #888;
  font-size: 14px;
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}
.loading, .empty, .no-more {
  text-align: center;
  color: #888;
  margin: 24px 0;
}
.load-more {
  text-align: center;
  margin: 24px 0;
}
.load-more button {
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 24px;
  font-size: 16px;
  cursor: pointer;
}
.load-more button:hover {
  background: #2563eb;
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
  background: #f1f5f9;
  color: #64748b;
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
</style> 