<template>
  <div class="user-list">
    <el-row :gutter="20">
      <el-col :span="24" v-for="user in list" :key="user.id">
        <div class="user-item">
          <img :src="getAvatarUrl(user.avatar)" class="avatar" />
          <span class="username">{{ user.username }}</span>
        </div>
      </el-col>
    </el-row>
    <div v-if="!finished" class="load-more">
      <el-button :loading="loading" @click="$emit('load-more')">加载更多</el-button>
    </div>
    <div v-else class="no-more">没有更多了</div>
  </div>
</template>

<script setup lang="ts">
import type { FollowUserInfo } from '../types/api'
import { defineProps } from 'vue'

const props = defineProps<{
  list: FollowUserInfo[]
  loading: boolean
  finished: boolean
}>()

const defaultAvatar = 'https://img1.imgtp.com/2023/07/21/2F1QKQbA.png'
function getAvatarUrl(avatar: string) {
  if (!avatar) return defaultAvatar
  const filename = avatar.replace(/\\/g, '/').split('/').pop()
  return filename ? `http://127.0.0.1:7071/avatarImg/${filename}` : defaultAvatar
}
</script>

<style scoped>
.user-list {
  width: 100%;
  padding: 0 32px;
}
.user-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 16px;
  object-fit: cover;
  background: #eee;
}
.username {
  font-size: 18px;
  color: #333;
}
.load-more {
  text-align: center;
  margin: 24px 0 8px 0;
}
.no-more {
  text-align: center;
  color: #aaa;
  margin: 24px 0 8px 0;
}
</style> 