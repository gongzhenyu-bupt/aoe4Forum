<template>
  <div class="search-page">
    <!-- 添加顶部栏 -->
    <TopBar />
    
    <div class="search-results">
      <div class="search-header">
        <h2>搜索结果</h2>
        <p v-if="keyword">关键词: "{{ keyword }}"</p>
        <p v-if="results.length > 0">找到 {{ results.length }} 个结果</p>
        <p v-else-if="!loading">没有找到相关结果</p>
      </div>

      <div v-if="loading" class="loading">搜索中...</div>

      <div v-else-if="results.length > 0" class="results-list">
        <article 
          v-for="post in results" 
          :key="post.id" 
          class="article-item"
          @click="goToPost(post.id)"
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
              <span class="stat-item">👁 {{ post.pageViewCount || 0 }}</span>
              <span class="stat-item">👍 {{ post.likeCount || 0 }}</span>
              <span class="stat-item">💬 {{ post.commentCount || 0 }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="no-results">
        <p>没有找到包含 "{{ keyword }}" 的帖子</p>
        <p>请尝试其他关键词</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchPostsApi } from '../utils/api'
import TopBar from './TopBar.vue'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const results = ref<any[]>([])
const loading = ref(false)

onMounted(() => {
  const searchKeyword = route.query.keyword as string
  if (searchKeyword) {
    keyword.value = searchKeyword
    performSearch(searchKeyword)
  }
})

async function performSearch(searchKeyword: string) {
  loading.value = true
  try {
    const response = await searchPostsApi(searchKeyword)
    if (response.code === 0 || response.code === '0') {
      results.value = response.data || []
    } else {
      console.error('搜索失败:', response.message)
      results.value = []
    }
  } catch (error) {
    console.error('搜索出错:', error)
    results.value = []
  } finally {
    loading.value = false
  }
}

function goToPost(postId: number) {
  router.push(`/post/${postId}`)
}

// 获取头像URL
const getAvatarUrl = (avatar: string) => {
  if (!avatar) return 'https://img1.imgtp.com/2023/07/21/2F1QKQbA.png'
  // 判断是否是默认头像路径
  if (avatar.startsWith('/defaultImg/')) {
    return `http://www.aoe4forum.cn:7071${avatar}`
  }
  // 只取文件名，拼接为Spring Boot静态资源URL
  const filename = avatar.replace(/\\/g, '/').split('/').pop()
  return filename ? `http://www.aoe4forum.cn:7071/avatarImg/${filename}` : 'https://img1.imgtp.com/2023/07/21/2F1QKQbA.png'
}

function formatTime(timeString: string) {
  if (!timeString) return ''
  return timeString.replace('T', ' ').slice(0, 19)
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
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background-color: #f8fafc;
}

.search-results {
  max-width: 700px;
  margin: 32px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  padding: 32px 24px;
}

.search-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 8px;
}

.search-header h2 {
  margin: 0 0 10px 0;
  color: #1f2937;
}

.search-header p {
  margin: 5px 0;
  color: #6b7280;
}

.loading, .no-results {
  text-align: center;
  color: #888;
  margin: 24px 0;
}

.no-results p {
  margin: 10px 0;
}

.results-list {
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
