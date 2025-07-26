<template>
  <div class="article-detail">
    <!-- 标题 -->
    <h1 class="article-title">{{ article.title }}</h1>
    <!-- 发帖人信息 -->
    <div class="author-info">
      <img :src="article.authorAvatar" class="author-avatar" />
      <div class="author-meta">
        <div class="author-name">{{ article.authorName }}</div>
        <div class="post-date">发表于：{{ article.date }}</div>
      </div>
    </div>
    <!-- 正文内容 -->
    <div class="article-content" v-html="article.content"></div>
    <div>评论组件已加载，postId: {{ postId }}</div>
    <!-- 评论区复用组件 -->
    <CommentSection :postId="postId" :myUserId="myUserId" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CommentSection from './CommentSection.vue'

// 文章数据（实际应通过props或API获取）
const article = ref({
  title: '专栏测试',
  authorName: '边九',
  authorAvatar: 'https://dummyimage.com/56x56',
  date: '2024年04月05日 04:21',
  content: '<p>test</p>'
})

const postId = 1 // 实际应通过路由参数等方式获取
const myUserId = 1 // 实际应通过登录信息获取
</script>

<style scoped>
.article-detail {
  max-width: 700px;
  margin: 32px auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  padding: 32px 32px 24px 32px;
}

.article-title {
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

.article-content {
  font-size: 16px;
  color: #444;
  line-height: 1.8;
  margin-bottom: 40px;
}
</style> 