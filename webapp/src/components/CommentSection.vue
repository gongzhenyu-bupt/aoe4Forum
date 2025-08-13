<template>
  <div class="comments-section comment-section">
    <div class="comments-title">评论</div>
    <div class="comment-input-area">
      <img class="input-avatar" :src="getCurrentUserAvatarSync()" />
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
      <div 
        v-for="comment in comments" 
        :key="comment.commentId" 
        class="comment-item"
        :data-comment-id="comment.commentId"
      >
        <img :src="getAvatarUrlSync(comment.avatar)" class="comment-avatar" @click="goToUserCenter(comment.userId)" style="cursor:pointer;" />
        <div class="comment-main">
          <div class="comment-header">
            <span class="comment-user">{{ comment.username }}</span>
            <span v-if="comment.userTag" class="user-tag">{{ comment.userTag }}</span>
          </div>
          <div class="comment-text">{{ comment.content }}</div>
          <div class="comment-footer">
            <span class="comment-date">{{ comment.createTime }}</span>
            <el-button size="small" text icon="el-icon-thumb" class="comment-action" @click="likeComment(comment)">👍 {{ comment.likeCount || 0 }}</el-button>
            <el-button size="small" text class="comment-action" @click="showReplyInput(comment)">回复</el-button>
            <el-button size="small" text type="danger" v-if="isMyComment(comment)" @click="deleteComment(comment.commentId)">删除</el-button>
          </div>
          <!-- 动态回复输入框 -->
          <div v-if="activeReplyId === comment.commentId" class="reply-input-area">
            <img class="reply-avatar" :src="getCurrentUserAvatarSync()" />
            <el-input
              v-model="replyContent"
              type="textarea"
              placeholder="回复评论..."
              rows="2"
              class="reply-input-box"
            />
            <div class="reply-actions">
              <el-button size="small" @click="submitReply(comment)">回复</el-button>
              <el-button size="small" @click="cancelReply">取消</el-button>
            </div>
          </div>
          <!-- 子评论展示（可选） -->
          <div class="child-comments" v-if="comment.childComments && comment.childComments.length">
            <div 
              v-for="child in comment.childComments" 
              :key="child.commentId" 
              class="child-comment"
              :data-comment-id="child.commentId"
            >
              <img :src="getAvatarUrlSync(child.avatar)" class="child-avatar" @click="goToUserCenter(child.userId)" style="cursor:pointer;" />
              <div class="child-main">
                <div class="child-header">
                  <span class="child-user">{{ child.username }}</span>
                  <span v-if="child.userTag" class="user-tag">{{ child.userTag }}</span>
                </div>
                <div class="child-text">{{ child.content }}</div>
                <div class="child-footer">
                  <span class="child-date">{{ child.createTime }}</span>
                  <el-button size="small" text icon="el-icon-thumb" class="comment-action" @click="likeComment(child)">👍 {{ child.likeCount || 0 }}</el-button>
                  <el-button size="small" text class="comment-action" @click="showReplyInput(child)">回复</el-button>
                  <el-button size="small" text type="danger" v-if="isMyComment(child)" @click="deleteComment(child.commentId)">删除</el-button>
                </div>
                <!-- 子评论的动态回复输入框 -->
                <div v-if="activeReplyId === child.commentId" class="reply-input-area">
                  <img class="reply-avatar" :src="getCurrentUserAvatarSync()" />
                  <el-input
                    v-model="replyContent"
                    type="textarea"
                    placeholder="回复评论..."
                    rows="2"
                    class="reply-input-box"
                  />
                  <div class="reply-actions">
                    <el-button size="small" @click="submitReply(child)">回复</el-button>
                    <el-button size="small" @click="cancelReply">取消</el-button>
                  </div>
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
import { getCommentsByPostIdApi, createCommentApi, deleteCommentApi, getCommentsByParentIdApi, getCommentsByParentIdsApi, likeCommentApi, getCommentByIdApi } from '../utils/api'
import { getCookie } from '../utils/cookie'
import AvatarCache from '../utils/avatarCache'

// 点赞（最小改动实现）
const likeComment = async (target: any) => {
  const token = getCookie('token')
  if (!token) {
    ElMessage.warning('请先登录后再点赞')
    return
  }
  if (!target || !target.commentId) return

  const prevCount = target.likeCount || 0
  if (target._liked === true) {
    target.likeCount = Math.max(0, prevCount - 1)
  } else {
    target.likeCount = prevCount + 1
  }

  try {
    const res = await likeCommentApi(target.commentId)
    if (!(res && (res.code === '0' || res.code === 0))) {
      target.likeCount = prevCount
      ElMessage.error(res?.msg || res?.message || '操作失败')
    } else {
      const fresh = await getCommentByIdApi(target.commentId)
      if (fresh && (fresh.code === '0' || fresh.code === 0) && fresh.data) {
        const freshData = fresh.data
        target.likeCount = freshData.likeCount ?? target.likeCount
      }
      target._liked = !target._liked
    }
  } catch (e) {
    target.likeCount = prevCount
    ElMessage.error('操作失败')
  }
}

const props = defineProps<{ postId: number, myUserId?: number, commentCount: number }>()

const comments = ref<any[]>([])
const totalComments = ref(0)
const pageSize = 10
const currentPage = ref(1)
const newComment = ref('')
const commentLoading = ref(false)

// 当前回复目标（null为对帖子评论，否则为对评论回复）
const replyTarget = ref<{commentId: number, userId: number, username: string} | null>(null)

// 动态回复输入框相关
const activeReplyId = ref<number | null>(null)
const replyContent = ref('')

// 头像缓存相关
const avatarCache = ref<Map<string, string>>(new Map())

// 获取头像URL（使用缓存）
const getAvatarUrl = async (avatar: string) => {
  if (!avatar) return 'https://dummyimage.com/36x36'
  
  // 检查缓存
  if (avatarCache.value.has(avatar)) {
    return avatarCache.value.get(avatar)!
  }
  
  // 从缓存系统获取
  const cachedUrl = await AvatarCache.getAvatarUrl(avatar)
  avatarCache.value.set(avatar, cachedUrl)
  return cachedUrl
}

// 获取当前用户头像（使用缓存）
const getCurrentUserAvatar = async () => {
  const cachedAvatar = localStorage.getItem('avatar')
  if (cachedAvatar) {
    return await getAvatarUrl(cachedAvatar)
  }
  return 'https://dummyimage.com/36x36'
}

// 同步获取头像URL（用于模板）
const getAvatarUrlSync = (avatar: string) => {
  if (!avatar) return 'https://dummyimage.com/36x36'
  return avatarCache.value.get(avatar) || 'https://dummyimage.com/36x36'
}

// 同步获取当前用户头像（用于模板）
const getCurrentUserAvatarSync = () => {
  const cachedAvatar = localStorage.getItem('avatar')
  if (cachedAvatar) {
    return getAvatarUrlSync(cachedAvatar)
  }
  return 'https://dummyimage.com/36x36'
}

// 获取评论列表
const fetchComments = async () => {
  const offset = (currentPage.value - 1) * pageSize
  try {
    const res = await getCommentsByPostIdApi(props.postId, offset, pageSize)
    if (res.code === '0' || res.code === 0) {
      comments.value = res.data
      totalComments.value = res.data.length // 实际应由后端返回总数
      
      // 预加载所有头像到缓存
      const allAvatars = new Set<string>()
      comments.value.forEach((comment: any) => {
        if (comment.avatar) allAvatars.add(comment.avatar)
      })
      
      // 批量预加载头像
      await Promise.all(Array.from(allAvatars).map(avatar => getAvatarUrl(avatar)))
      
      // 批量获取每条评论的子评论
      const parentIds = comments.value.map((c: any) => c.commentId)
      if (parentIds.length > 0) {
        const subRes = await getCommentsByParentIdsApi(parentIds)
        // subRes.data 结构为 { [parentId]: [subComment, ...] }
        for (const comment of comments.value) {
          comment.childComments = (subRes.data && subRes.data[comment.commentId]) ? subRes.data[comment.commentId] : []
          
          // 预加载子评论的头像
          if (comment.childComments && comment.childComments.length > 0) {
            const childAvatars = comment.childComments.map((child: any) => child.avatar).filter((avatar: string) => Boolean(avatar))
            await Promise.all(childAvatars.map((avatar: string) => getAvatarUrl(avatar)))
          }
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
    
    // 获取token
    const token = getCookie('token') || undefined
    
    const res = await createCommentApi({
      postId: props.postId,
      content: newComment.value,
      parentId,
      repliedUserId,
      repliedUsername,
      token
    })
    if (res.code === '0' || res.code === 0) {
      ElMessage.success('评论成功')
      
      // 创建新的评论对象
      const newCommentObj = {
        commentId: res.data?.commentId || Date.now(), // 使用返回的ID或时间戳
        postId: props.postId,
        parentId: parentId,
        content: newComment.value,
        commentTime: res.data?.commentTime || new Date().toISOString(),
        likeCount: res.data?.likeCount || 0,
        status: res.data?.status || 0,
        username: res.data?.username || localStorage.getItem('name') || '用户',
        userId: res.data?.userId || Number(localStorage.getItem('userid')) || 0,
        avatar: res.data?.avatar || localStorage.getItem('avatar') || '/defaultImg/ottomans.png',
        repliedUsername: repliedUsername,
        repliedUserId: repliedUserId,
        childCount: res.data?.childCount || 0,
        childComments: []
      }
      
      // 根据parentId添加到对应位置
      if (parentId === -1) {
        // 对帖子的评论，添加到主评论列表末尾
        comments.value.push(newCommentObj)
      } else {
        // 对评论的回复，找到对应的父评论并添加到其子评论列表
        const parentComment = comments.value.find(c => c.commentId === parentId)
        if (parentComment) {
          if (!parentComment.childComments) {
            parentComment.childComments = []
          }
          parentComment.childComments.push(newCommentObj)
          parentComment.childCount = (parentComment.childCount || 0) + 1
        }
      }
      
      newComment.value = ''
      replyTarget.value = null
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
      
      // 从本地数组中移除评论
      const removeCommentFromArray = (commentsArray: any[], targetId: number) => {
        for (let i = 0; i < commentsArray.length; i++) {
          if (commentsArray[i].commentId === targetId) {
            // 找到要删除的评论
            const deletedComment = commentsArray[i]
            
            // 如果是主评论，需要同时删除其所有子评论
            if (deletedComment.parentId === -1) {
              commentsArray.splice(i, 1)
              return true
            } else {
              // 如果是子评论，只删除这一个
              commentsArray.splice(i, 1)
              return true
            }
          }
          
          // 递归检查子评论
          if (commentsArray[i].childComments && commentsArray[i].childComments.length > 0) {
            if (removeCommentFromArray(commentsArray[i].childComments, targetId)) {
              // 如果删除了子评论，更新父评论的childCount
              commentsArray[i].childCount = Math.max(0, (commentsArray[i].childCount || 0) - 1)
              return true
            }
          }
        }
        return false
      }
      
      // 执行删除操作
      removeCommentFromArray(comments.value, commentId)
      
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

// 回复评论（旧版本，保留兼容性）
const replyTo = (comment: any) => {
  replyTarget.value = {
    commentId: comment.commentId,
    userId: comment.userId,
    username: comment.username
  }
  newComment.value = `@${comment.username} `
}

// 显示动态回复输入框
const showReplyInput = (comment: any) => {
  activeReplyId.value = comment.commentId
  replyContent.value = `@${comment.username} `}

// 取消动态回复
const cancelReply = () => {
  activeReplyId.value = null
  replyContent.value = ''
}

// 提交动态回复
const submitReply = async (comment: any) => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  
  // 找到最外层的主评论ID
  let parentId: number
  if (comment.parentId === -1) {
    // 如果是对主评论的回复，使用主评论ID
    parentId = comment.commentId
  } else {
    // 如果是对子评论的回复，使用主评论ID（即comment.parentId）
    parentId = comment.parentId
  }
  
  const repliedUserId = comment.userId
  const repliedUsername = comment.username

  const token = getCookie('token') || undefined

  try {
    const res = await createCommentApi({
      postId: props.postId,
      content: replyContent.value,
      parentId,
      repliedUserId,
      repliedUsername,
      token
    })
    if (res.code === '0' || res.code === 0) {
      ElMessage.success('回复成功')
      
      // 创建新的回复对象
      const newReplyObj = {
        commentId: res.data?.commentId || Date.now(),
        postId: props.postId,
        parentId: parentId,
        content: replyContent.value,
        commentTime: res.data?.commentTime || new Date().toISOString(),
        likeCount: res.data?.likeCount || 0,
        status: res.data?.status || 0,
        username: res.data?.username || localStorage.getItem('name') || '用户',
        userId: res.data?.userId || Number(localStorage.getItem('userid')) || 0,
        avatar: res.data?.avatar || localStorage.getItem('avatar') || '/defaultImg/ottomans.png',
        repliedUsername: repliedUsername,
        repliedUserId: repliedUserId,
        childCount: res.data?.childCount || 0,
        childComments: []
      }
      
      // 找到对应的父评论并添加到其子评论列表
      const parentComment = comments.value.find(c => c.commentId === parentId)
      if (parentComment) {
        if (!parentComment.childComments) {
          parentComment.childComments = []
        }
        parentComment.childComments.push(newReplyObj)
        parentComment.childCount = (parentComment.childCount || 0) + 1
      }
      
      activeReplyId.value = null
      replyContent.value = ''
    } else {
      ElMessage.error(res.msg || res.message || '回复失败')
    }
  } catch (e) {
    ElMessage.error('回复失败')
  }
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
  color: #999;
  margin-top: 20px;
}
  
  /* 动态回复输入框样式 */
  .reply-input-area {
    display: flex;
    align-items: flex-start;
    margin-top: 12px;
    gap: 12px;
    padding: 12px;
    background-color: #f8f9fa;
    border-radius: 8px;
    border: 1px solid #e9ecef;
  }
  
  .reply-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    margin-top: 2px;
  }
  
  .reply-input-box {
    flex: 1;
  }
  
  .reply-actions {
    display: flex;
    gap: 8px;
    margin-left: 12px;
    align-items: center;
  }
  
  /* 高亮评论样式 */
  .comment-item.highlight-comment,
  .child-comment.highlight-comment {
    background: #fef3c7 !important;
    border: 2px solid #f59e0b !important;
    border-radius: 8px;
    animation: highlight-pulse 2s ease-in-out;
  }
  
  @keyframes highlight-pulse {
    0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7); }
    70% { box-shadow: 0 0 0 10px rgba(245, 158, 11, 0); }
    100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
  }
</style> 
