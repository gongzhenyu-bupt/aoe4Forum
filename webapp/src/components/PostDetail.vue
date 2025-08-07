<template>
  <div class="post-detail-page">
    <!-- 顶部导航栏 -->
    <header class="header">
        <TopBar />
    </header>
    <div class="post-detail-container">
      <!-- 标题 -->
      <h2 v-if="post" class="post-title">{{ post.title }}</h2>
      <div v-if="post">
        <!-- 用户信息和发帖信息 -->
        <div class="author-info">
          <img :src="post.userAvatar" class="author-avatar" @click="goToUserCenter(post.userId)" style="cursor:pointer;" />
          <div class="author-meta">
            <div class="author-name">{{ post.userName }}</div>
            <div class="post-date">{{ post.createTime }}</div>
          </div>
        </div>
        <div class="post-content" v-html="postContent"></div>
        <!-- 点赞/点踩比例条 -->
        <LikeDislikeBar :like="post.likeCount || 0" :dislike="post.dislikeCount || 0" :postId="post.id" @refresh="refreshPostInfo" />
      </div>
      <div v-else>加载中...</div>
      <!-- 评论区复用组件 -->
      <CommentSection :postId="postId" :myUserId="myUserId" :commentCount="post?.commentCount || 0" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from './TopBar.vue'
import { getPostDetailApi, getPostBaseInfoApi } from '../utils/api'
import CommentSection from './CommentSection.vue'
// 修复 LikeDislikeBar 路径，确保文件名大小写与实际一致
import LikeDislikeBar from './LikeDislikeBar.vue'

const route = useRoute()
const post = ref<any>(null)
const postContent = ref('')
const postId = Number(route.params.id)
const myUserId = 1 // 实际应通过登录信息获取
const router = useRouter()

onMounted(async () => {
  // 获取基本信息
  const baseRes = await getPostBaseInfoApi(postId)
  if (baseRes.code === 0 || baseRes.code === '0') {
    post.value = baseRes.data
    // 缓存发帖人信息
    if (post.value && post.value.userId && post.value.userName) {
      localStorage.setItem('postAuthorUserId', String(post.value.userId))
      localStorage.setItem('postAuthorUserName', post.value.userName)
    }
  }
  // 获取内容
  const contentRes = await getPostDetailApi(postId)
  if (contentRes.code === 0 || contentRes.code === '0') {
    postContent.value = contentRes.data.content || ''
  }
})

function goToUserCenter(userid: number|string) {
  if (userid) {
    router.push(`/${userid}`)
  }
}

function refreshPostInfo() {
  // 重新获取帖子基本信息，刷新点赞/点踩数
  getPostBaseInfoApi(postId).then(baseRes => {
    if (baseRes.code === 0 || baseRes.code === '0') {
      post.value = baseRes.data
    }
  })
}
</script>

<style scoped>
.post-detail-page {
  min-height: 100vh;
  background: #f8fafc;
}
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
.post-detail-container {
  max-width: 800px;
  margin: 40px auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  padding: 32px;
}
.post-title {
  font-size: 26px;
  font-weight: bold;
  margin-bottom: 18px;
  color: #222;
  text-align: left;
}
.author-info {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}
.author-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  margin-right: 16px;
  border: 2px solid #eee;
}
.author-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.author-name {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}
.post-date {
  font-size: 14px;
  color: #888;
  margin-top: 4px;
}
.post-content {
  font-size: 18px;
  color: #222;
  line-height: 1.8;
  text-align: left;
  margin-bottom: 48px;
}
</style> 