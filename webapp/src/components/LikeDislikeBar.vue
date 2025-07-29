<template>
  <div class="like-dislike-btns">
    <button class="like-btn" @click="handleLike">👍 {{ like }}</button>
    <button class="dislike-btn" @click="handleDislike">👎 {{ dislike }}</button>
  </div>
</template>

<script setup lang="ts">
import { likePostApi } from '../utils/api'
const props = defineProps<{ like: number, dislike: number, postId: number }>()
const emit = defineEmits(['refresh'])

async function handleLike() {
  await likePostApi({ postId: props.postId, type: 1 })
  emit('refresh')
}
async function handleDislike() {
  await likePostApi({ postId: props.postId, type: 0 })
  emit('refresh')
}
</script>

<style scoped>
.like-dislike-btns {
  margin: 16px 0 24px 0;
  display: flex;
  gap: 16px;
}
.like-btn, .dislike-btn {
  min-width: 64px;
  padding: 6px 18px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  background: #f0f0f0;
  color: #333;
  transition: background 0.2s, color 0.2s;
}
.like-btn:hover {
  background: #1abc9c;
  color: #fff;
}
.dislike-btn:hover {
  background: #e57373;
  color: #fff;
}
</style> 