<template>
  <div class="user-info-card">
    <div class="card-main">
      <div class="avatar-col">
        <img :src="getAvatarUrl(user.avatar)" class="avatar" :class="{ clickable: isSelf }" @click="handleAvatarClick" />
      </div>
      <div class="info-col">
        <div class="nickname">{{ user.username || '未登录用户' }}</div>
        <div class="stats-row">
          <div class="stat-block">
            <div class="stat-num clickable" @click="goToFollowList('follower')">{{ user.followerNums ?? 0 }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-block">
            <div class="stat-num clickable" @click="goToFollowList('followee')">{{ user.followeeNums ?? 0 }}</div>
            <div class="stat-label">关注</div>
          </div>
        </div>
        <button v-if="!isSelf" class="follow-btn" :class="{ followed: isFollowed }" @click="handleFollowClick">
          {{ isFollowed ? '已关注' : '关注' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, defineProps, watch } from 'vue'
import { getUserInfoApi, isFollowApi, followUserApi, unfollowUserApi } from '../utils/api'
import { useRouter } from 'vue-router'
  
const props = defineProps<{ userid: string }>()
const user = ref({
  username: '',
  avatar: '',
  followeeNums: 0,
  followerNums: 0
})
const defaultAvatar = 'https://img1.imgtp.com/2023/07/21/2F1QKQbA.png'

const getAvatarUrl = (avatar: string) => {
  if (!avatar) return defaultAvatar
  // 只取文件名，拼接为Spring Boot静态资源URL
  const filename = avatar.replace(/\\/g, '/').split('/').pop()
  return filename ? `http://127.0.0.1:7071/avatarImg/${filename}` : defaultAvatar
}

const router = useRouter()
const isSelf = localStorage.getItem('userid') === props.userid
const myUserId = localStorage.getItem('userid')
const isFollowed = ref(false)
const followLoading = ref(false)

// 检查关注状态
async function checkFollowStatus() {
  if (!myUserId || !props.userid || myUserId === props.userid) {
    isFollowed.value = false
    return
  }
  try {
    const res = await isFollowApi(Number(myUserId), Number(props.userid))
    if (res.code === 0 || res.code === '0') {
      isFollowed.value = true
    } else {
      isFollowed.value = false
    }
  } catch {
    isFollowed.value = false
  }
}

async function handleFollowClick() {
  if (followLoading.value) return
  followLoading.value = true
  try {
    if (isFollowed.value) {
      await unfollowUserApi(Number(myUserId), Number(props.userid))
      isFollowed.value = false
    } else {
      await followUserApi(Number(myUserId), Number(props.userid))
      isFollowed.value = true
    }
  } catch (e) {
    // 可选：弹窗提示失败
  } finally {
    followLoading.value = false
  }
}

const handleAvatarClick = () => {
  if (isSelf) {
    router.push('/edit-avatar')
  }
}

const goToFollowList = (tab: 'follower' | 'followee') => {
  router.push({
    path: `/follow-list/${props.userid}`,
    query: { tab }
  })
}

// 封装加载用户信息和关注状态的函数
async function loadUserInfoAndFollowStatus() {
  if (!props.userid) {
    console.error('UserInfoCard: userid 为空，未发起请求')
    return
  }
  try {
    const res = await getUserInfoApi(props.userid)
    if (res.code === 0 || res.code === '0') {
      user.value.username = res.data.username || ''
      user.value.avatar = res.data.avatar || ''
      user.value.followeeNums = res.data.followeeNums ?? 0
      user.value.followerNums = res.data.followerNums ?? 0
    }
  } catch (e) {
    user.value = { username: '', avatar: '', followeeNums: 0, followerNums: 0 }
    console.error('加载用户信息失败', e)
  }
  checkFollowStatus()
}

onMounted(() => {
  loadUserInfoAndFollowStatus()
})

// 监听props.userid变化，切换主页时刷新内容
watch(
  () => props.userid,
  (newVal, oldVal) => {
    if (newVal !== oldVal) {
      loadUserInfoAndFollowStatus()
    }
  }
)
</script>

<style scoped>
.user-info-card {
  position: relative;
  width: 100%;
  max-width: 900px;
  min-height: 140px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin: 0 auto 12px auto;
  overflow: hidden;
}
.card-main {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 24px 40px 24px 40px;
  z-index: 2;
}
.avatar-col {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-right: 32px;
}
.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #fff;
  background: #f5f7fa;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin: 0;
}
.info-col {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  flex: 1;
}
.nickname {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  margin-top: 8px;
  letter-spacing: 1px;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
}
.meta-online {
  color: #6ee755;
  font-size: 15px;
  font-weight: 500;
}
.meta-join {
  color: #888;
  font-size: 15px;
}
.stats-row {
  display: flex;
  gap: 48px;
}
.stat-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.stat-num {
  font-size: 28px;
  font-weight: 600;
  color: #7c6fa7;
  margin-bottom: 2px;
}
.stat-label {
  font-size: 15px;
  color: #888;
}
.avatar.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.avatar.clickable:hover {
  box-shadow: 0 0 0 4px #a78bfa;
}
.stat-num.clickable {
  cursor: pointer;
  color: #7c6fa7;
  transition: color 0.2s;
}
.stat-num.clickable:hover {
  color: #a78bfa;
}
.follow-btn {
  margin-top: 18px;
  padding: 8px 32px;
  font-size: 16px;
  border-radius: 20px;
  border: none;
  background: #7c6fa7;
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
}
.follow-btn.followed {
  background: #aaa;
  color: #fff;
}
.follow-btn:active {
  opacity: 0.8;
}
</style> 