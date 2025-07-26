<template>
  <div class="comments-section">
    <div class="comments-title">评论</div>
    <div class="comment-tabs">
      <el-tabs v-model="activeTab" @tab-click="fetchComments">
        <el-tab-pane label="最热" name="hot"></el-tab-pane>
        <el-tab-pane label="最新" name="new"></el-tab-pane>
      </el-tabs>
    </div>
    <div class="comment-input-area">
      <img class="input-avatar" :src="myAvatar" />
      <el-input
        v-model="newComment"
        type="textarea"
        placeholder="写下你的评论..."
        rows="2"
        class="input-box"
      />
      <el-button type="primary" @click="submitComment" :loading="commentLoading" class="input-btn">发表评论</el-button>
    </div>
    <div class="comment-list">
      <div v-for="comment in comments" :key="comment.commentId" class="comment-item">
        <img :src="comment.userAvatar" class="comment-avatar" @click="goToUserCenter(comment.userId)" style="cursor:pointer;" />
        <div class="comment-main">
          <div class="comment-header">
            <span class="comment-user">{{ comment.username }}</span>
            <span v-if="comment.userTag" class="user-tag">{{ comment.userTag }}</span>
          </div>
          <div class="comment-text">{{ comment.content }}</div>
          <div class="comment-footer">
            <span class="comment-date">{{ comment.createTime }}</span>
            <el-button size="small" text icon="el-icon-thumb" class="comment-action">👍 {{ comment.likes || 0 }}</el-button>
            <el-button size="small" text icon="el-icon-thumb" class="comment-action">👎</el-button>
            <el-button size="small" text class="comment-action" @click="replyTo(comment)">回复</el-button>
            <el-button size="small" text type="danger" v-if="isMyComment(comment)" @click="deleteComment(comment.commentId)">删除</el-button>
          </div>
          <!-- 子评论展示（可选） -->
          <div class="child-comments" v-if="comment.childComments && comment.childComments.length">
            <div v-for="child in comment.childComments" :key="child.commentId" class="child-comment">
              <img :src="child.userAvatar" class="child-avatar" @click="goToUserCenter(child.userId)" style="cursor:pointer;" />
              <div class="child-main">
                <div class="child-header">
                  <span class="child-user">{{ child.username }}</span>
                  <span v-if="child.userTag" class="user-tag">{{ child.userTag }}</span>
                </div>
                <div class="child-text">{{ child.content }}</div>
                <div class="child-footer">
                  <span class="child-date">{{ child.createTime }}</span>
                  <el-button size="small" text icon="el-icon-thumb" class="comment-action">👍 {{ child.likes || 0 }}</el-button>
                  <el-button size="small" text icon="el-icon-thumb" class="comment-action">👎</el-button>
                  <el-button size="small" text class="comment-action" @click="replyTo(child)">回复</el-button>
                  <el-button size="small" text type="danger" v-if="isMyComment(child)" @click="deleteComment(child.commentId)">删除</el-button>
                </div>
              </div>
            </div>
            <!-- 加载更多子评论按钮 -->
            <div v-if="comment.childCount > comment.childComments.length">
              <el-button size="small" @click="fetchMoreChildComments(comment)">加载更多回复</el-button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="comments.length === 0" class="no-comment">暂无评论</div>
    </div>
    <!-- 加载更多评论按钮 -->
    <el-button
      v-if="comments.length < props.commentCount"
      @click="fetchMoreComments"
      class="load-more-btn"
    >
      加载更多评论
    </el-button>
    <div v-else class="no-more-comments">没有更多评论了</div>
  </div>
</template>


<script setup lang="ts">
import { ref, onMounted, watch, defineProps } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCommentsByPostIdApi, createCommentApi, deleteCommentApi, getCommentsByParentIdApi, getCommentsByParentIdsApi } from '../utils/api'

const props = defineProps<{ postId: number, myUserId?: number, commentCount: number }>()

const comments = ref<any[]>([])
const totalComments = ref(0)
const pageSize = 10
const currentPage = ref(1)
const activeTab = ref('hot')
const newComment = ref('')
const commentLoading = ref(false)
const myAvatar = ref('https://dummyimage.com/36x36') // 可替换为实际登录用户头像

// 当前回复目标（null为对帖子评论，否则为对评论回复）
const replyTarget = ref<{commentId: number, userId: number, username: string} | null>(null)

// 获取评论列表
const fetchComments = async () => {
  const offset = (currentPage.value - 1) * pageSize
  try {
    const res = await getCommentsByPostIdApi(props.postId, offset, pageSize)
    if (res.code === '0' || res.code === 0) {
      comments.value = res.data
      totalComments.value = res.data.length // 实际应由后端返回总数
      // 批量获取每条评论的子评论
      const parentIds = comments.value.map((c: any) => c.commentId)
      if (parentIds.length > 0) {
        const subRes = await getCommentsByParentIdsApi(parentIds)
        // subRes.data 结构为 { [parentId]: [subComment, ...] }
        for (const comment of comments.value) {
          comment.childComments = (subRes.data && subRes.data[comment.commentId]) ? subRes.data[comment.commentId] : []
        }
      } else {
        for (const comment of comments.value) {
          comment.childComments = []
        }
      }
    }
  } catch (e) {
    ElMessage.error('获取评论失败')
  }
}

// 发表评论
const submitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  commentLoading.value = true
  try {
    let parentId: number, repliedUserId: number | undefined, repliedUsername: string | undefined
    if (replyTarget.value) {
      // 回复评论
      parentId = replyTarget.value.commentId
      repliedUserId = replyTarget.value.userId
      repliedUsername = replyTarget.value.username
    } else {
      // 对帖子本身评论
      parentId = -1
      const idStr = localStorage.getItem('postAuthorUserId')
      repliedUserId = idStr ? Number(idStr) : undefined
      const nameStr = localStorage.getItem('postAuthorUserName')
      repliedUsername = nameStr ? nameStr : undefined
    }
    const res = await createCommentApi({
      postId: props.postId,
      content: newComment.value,
      parentId,
      repliedUserId,
      repliedUsername
    })
    if (res.code === '0' || res.code === 0) {
      ElMessage.success('评论成功')
      newComment.value = ''
      replyTarget.value = null
      fetchComments()
    } else {
      ElMessage.error(res.msg || res.message || '评论失败')
    }
  } finally {
    commentLoading.value = false
  }
}

// 删除评论
const deleteComment = async (commentId: number) => {
  try {
    const res = await deleteCommentApi(commentId)
    if (res.code === '0' || res.code === 0) {
      ElMessage.success('删除成功')
      fetchComments()
    } else {
      ElMessage.error(res.msg || res.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

// 判断是否为自己的评论
const isMyComment = (comment: any) => {
  return props.myUserId && comment.userId === props.myUserId
}

// 回复评论
const replyTo = (comment: any) => {
  replyTarget.value = {
    commentId: comment.commentId,
    userId: comment.userId,
    username: comment.username
  }
  newComment.value = `@${comment.username} `
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchComments()
}

// 在每个父评论对象上维护子评论offset
const fetchMoreChildComments = async (parentComment: any) => {
  // 计算当前已加载数量
  const currentCount = parentComment.childComments ? parentComment.childComments.length : 0
  const res = await getCommentsByParentIdApi(parentComment.commentId, currentCount, 10)
  if (res.code === '0' || res.code === 0) {
    if (!parentComment.childComments) parentComment.childComments = []
    parentComment.childComments = parentComment.childComments.concat(res.data)
  }
}

// 加载更多评论
const fetchMoreComments = async () => {
  const offset = comments.value.length
  const res = await getCommentsByPostIdApi(props.postId, offset, pageSize)
  if (res.code === '0' || res.code === 0) {
    // 追加新评论
    comments.value = comments.value.concat(res.data)
    // 批量加载新评论的子评论
    const parentIds = res.data.map((c: any) => c.commentId)
    if (parentIds.length > 0) {
      const subRes = await getCommentsByParentIdsApi(parentIds)
      for (const comment of res.data) {
        comment.childComments = (subRes.data && subRes.data[comment.commentId]) ? subRes.data[comment.commentId] : []
      }
    }
  }
}

const router = useRouter()

function goToUserCenter(userid: number|string) {
  if (userid) {
    router.push(`/${userid}`)
  }
}

onMounted(() => {
  fetchComments()
})

watch(() => props.postId, () => {
  fetchComments()
})
</script>


<style scoped>
.comments-section {
  margin-top: 40px;
  background: #fff;
  padding: 24px 24px 8px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
.comments-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 18px;
}
.comment-input-area {
  display: flex;
  align-items: flex-start;
  margin-bottom: 18px;
  gap: 12px;
}
.input-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-top: 2px;
}
.input-box {
  flex: 1;
}
.input-btn {
  margin-left: 8px;
  height: 36px;
}
.comment-tabs {
  margin-bottom: 8px;
}
.comment-list {
  margin-bottom: 16px;
}
.comment-item {
  display: flex;
  padding: 18px 0 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  margin-right: 14px;
  margin-top: 2px;
}
.comment-main {
  flex: 1;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}
.comment-user {
  font-weight: 500;
  color: #222;
  font-size: 16px;
}
.user-tag {
  background: #f5f5f5;
  color: #ff9800;
  font-size: 12px;
  border-radius: 4px;
  padding: 2px 6px;
  margin-left: 4px;
}
.comment-text {
  font-size: 15px;
  color: #333;
  margin-bottom: 6px;
  word-break: break-all;
  text-align: left;
}
.comment-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}
.comment-date {
  margin-right: 8px;
  color: #bbb;
}
.comment-action {
  padding: 0 4px;
  font-size: 13px;
}
.child-comments {
  margin-left: 48px;
  margin-top: 8px;
  background: transparent;
  border-radius: 0;
  padding: 0;
}
.child-comment {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.child-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  margin-right: 10px;
  margin-top: 2px;
}
.child-main {
  flex: 1;
}
.child-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}
.child-user {
  font-weight: 500;
  color: #409eff;
  font-size: 15px;
}
.child-text {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  word-break: break-all;
  text-align: left;
}
.child-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}
.child-date {
  margin-right: 8px;
  color: #bbb;
}
.no-comment {
  text-align: center;
  color: #bbb;
  margin: 24px 0 12px 0;
}
.comment-pagination {
  margin-top: 12px;
  text-align: right;
}
.load-more-btn {
  margin-top: 12px;
  text-align: center;
}
.no-more-comments {
  text-align: center;
  color: #bbb;
  margin: 12px 0;
}
</style> 