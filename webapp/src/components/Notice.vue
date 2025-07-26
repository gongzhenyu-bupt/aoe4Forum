<template>
  <div class="notice-page">
    <header class="header">
      <div class="header-container">
        <TopBar />
      </div>
    </header>
    <div class="notice-main">
      <aside class="notice-sidebar">
        <ul>
          <li :class="{active: activeTab === 'reply'}" @click="handleTab('reply')">回复我的</li>
          <li :class="{active: activeTab === 'like'}" @click="handleTab('like')">收到的赞</li>
          <li :class="{active: activeTab === 'fan'}" @click="handleTab('fan')">新增粉丝</li>
        </ul>
      </aside>
      <main class="notice-content">
        <div v-if="loading" class="empty-data">加载中...</div>
        <div v-else-if="noticeList.length === 0" class="empty-data">
          <img src="https://img.alicdn.com/imgextra/i2/O1CN01Qn1QwC1w6QwZpUKlA_!!6000000006312-2-tps-400-300.png" alt="no data" class="empty-img" />
          <div class="empty-text">然而并没有数据</div>
        </div>
        <ul v-else class="notice-list">
          <li v-for="notice in noticeList" :key="notice.id" class="notice-item"
              :class="{ clickable: activeTab === 'fan' }"
              @click="activeTab === 'fan' ? goToUser(notice) : null">
            <span v-if="notice.avatar" class="notice-avatar"><img :src="notice.avatar" alt="avatar" /></span>
            <span class="notice-nickname" v-if="notice.nickname">{{ notice.nickname }}</span>
            <span class="notice-content-text">{{ notice.content || notice.msg || '暂无内容' }}</span>
          </li>
        </ul>
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

function goToUser(notice: any) {
  if (notice && notice.businessId) {
    router.push(`/${notice.businessId}`)
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
  margin-top: 20px;
  /* 不要 min-height */
  min-height: 600px;
}

.notice-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #f0f0f0;
  padding-top: 8px;
  box-sizing: border-box;
  height: 100%;
  position: static;
}
.notice-sidebar ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.notice-sidebar li {
  padding: 16px 24px;
  cursor: pointer;
  color: #64748b;
  font-size: 16px;
  border-left: 4px solid transparent;
  transition: background 0.2s, border-color 0.2s;
}
.notice-sidebar li.active {
  background: #e6fcfa;
  color: #1abc9c;
  border-left: 4px solid #1abc9c;
}
.notice-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  min-height: 100%;
  padding: 32px 32px 0 32px;
  /* 不要 align-items/justify-content:center */
}
.empty-data {
  text-align: center;
  color: #b0b8c9;
}
.empty-img {
  width: 220px;
  margin-bottom: 12px;
  opacity: 0.7;
}
.empty-text {
  font-size: 15px;
  color: #b0b8c9;
}
.notice-list {
  width: 100%;
  padding: 0;
  margin: 0;
  list-style: none;
}
.notice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  color: #333;
}
.notice-avatar img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}
.notice-nickname {
  font-weight: 600;
  color: #1abc9c;
}
.notice-content-text {
  color: #64748b;
}
.notice-item.clickable {
  cursor: pointer;
  transition: background 0.2s;
}
.notice-item.clickable:hover {
  background: #f8fafc;
}
</style> 