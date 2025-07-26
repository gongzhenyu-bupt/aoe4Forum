<template>
  <div class="follow-list-page">
    <TopBar />
    <div class="follow-list-main">
      <el-tabs v-model="activeTab" @tab-click="onTabChange">
        <el-tab-pane label="关注" name="followee">
          <UserList
            :list="followeeList"
            :loading="loading"
            :finished="finishedFollowee"
            @load-more="loadMoreFollowee"
          />
        </el-tab-pane>
        <el-tab-pane label="粉丝" name="follower">
          <UserList
            :list="followerList"
            :loading="loading"
            :finished="finishedFollower"
            @load-more="loadMoreFollower"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TopBar from './TopBar.vue'
import { getFollowerListApi, getFolloweeListApi, getUserInfoApi } from '../utils/api'
import type { FollowUserInfo } from '../types/api'
import UserList from './UserList.vue'

const route = useRoute()
const userId = Number(route.params.userid)
const PAGE_SIZE = 10

const activeTab = ref<'followee' | 'follower'>('followee')
const followeeList = ref<FollowUserInfo[]>([])
const followerList = ref<FollowUserInfo[]>([])
const loading = ref(false)
const finishedFollowee = ref(false)
const finishedFollower = ref(false)

const totalFollower = ref(0)
const totalFollowee = ref(0)

// 记录分页参数
let lastFolloweeCreateTime: string | undefined = undefined
let lastFolloweeId: number | undefined = undefined
let lastFollowerCreateTime: string | undefined = undefined
let lastFollowerId: number | undefined = undefined

// 记录是否已开始分页
let followeePaging = false
let followerPaging = false

const loadUserInfo = async () => {
  const res = await getUserInfoApi(userId.toString())
  if (res.code === 0 || res.code === '0') {
    totalFollower.value = res.data.followerNums || 0
    totalFollowee.value = res.data.followeeNums || 0
  }
}

const loadFollowee = async (reset = false) => {
  if (loading.value || finishedFollowee.value) return
  loading.value = true
  const params: any = { userId, needCursor: followeePaging }
  if (followeePaging) {
    params.lastCreateTime = lastFolloweeCreateTime
    params.lastId = lastFolloweeId
  }
  const res = await getFolloweeListApi(params)
  if (reset) followeeList.value = []
  if (res.data && res.data.followers) {
    followeeList.value.push(...res.data.followers)
    lastFolloweeCreateTime = res.data.lastCreateTime
    lastFolloweeId = res.data.lastId
    if (followeeList.value.length < totalFollowee.value) {
      finishedFollowee.value = false
      followeePaging = true
    } else {
      finishedFollowee.value = true
    }
  } else {
    finishedFollowee.value = true
  }
  loading.value = false
}

const loadFollower = async (reset = false) => {
  if (loading.value || finishedFollower.value) return
  loading.value = true
  const params: any = { userId, needCursor: followerPaging }
  if (followerPaging) {
    params.lastCreateTime = lastFollowerCreateTime
    params.lastId = lastFollowerId
  }
  const res = await getFollowerListApi(params)
  if (reset) followerList.value = []
  if (res.data && res.data.followers) {
    followerList.value.push(...res.data.followers)
    lastFollowerCreateTime = res.data.lastCreateTime
    lastFollowerId = res.data.lastId
    if (followerList.value.length < totalFollower.value) {
      finishedFollower.value = false
      followerPaging = true
    } else {
      finishedFollower.value = true
    }
  } else {
    finishedFollower.value = true
  }
  loading.value = false
}

const onTabChange = (tab: any) => {
  if (tab.paneName === 'followee' && followeeList.value.length === 0) {
    followeePaging = false
    loadFollowee(true)
  } else if (tab.paneName === 'follower' && followerList.value.length === 0) {
    followerPaging = false
    loadFollower(true)
  }
}

const loadMoreFollowee = () => loadFollowee(false)
const loadMoreFollower = () => loadFollower(false)

onMounted(async () => {
  followeePaging = false
  followerPaging = false
  await loadUserInfo()
  loadFollowee(true)
})
</script>

<style scoped>
.follow-list-page {
  min-height: 100vh;
  background: #f8fafc;
}
.follow-list-main {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  padding: 32px 0;
}
</style> 