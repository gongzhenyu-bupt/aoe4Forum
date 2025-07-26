<template>
  <div class="usercenter-page">
    <TopBar />
    <div class="usercenter-main">
      <UserInfoCard :userid="userid" />
      <div class="user-posts-area">
        <!-- 这里放用户的帖子列表或详情，后续可扩展为分页、筛选等 -->
        <div class="posts-placeholder">这里是发帖详情区（开发中...）</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ref, watch } from 'vue'
import TopBar from './TopBar.vue'
import UserInfoCard from './UserInfoCard.vue'
// 你可以后续引入 UserPostsList 组件

const route = useRoute()
const userid = ref(route.params.userid as string)

watch(
  () => route.params.userid,
  (newVal) => {
    userid.value = newVal as string
  }
)
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
  min-height: 300px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.posts-placeholder {
  color: #aaa;
  font-size: 20px;
  margin: 48px 0;
}
</style>
