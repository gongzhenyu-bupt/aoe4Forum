import type { LoginRequest, RegisterRequest, CheckCodeResponse, ApiResponse, ArticleListParams, ArticleListResponse } from '../types/api'
import type { FollowCursorPageRequest, FollowQueryResult } from '../types/api'

// API 基础配置
const API_BASE_URL = 'http://127.0.0.1:7071'

// 通用请求函数
async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    credentials:'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  return response.json()
}

// 登录API
export async function loginApi(data: LoginRequest): Promise<ApiResponse> {
  return request('/account/login', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// 获取验证码API
export async function getCheckCodeApi(): Promise<CheckCodeResponse> {
  return request('/account/checkCode', {
    method: 'GET',
  })
}

// 注册API
export async function registerApi(data: RegisterRequest): Promise<ApiResponse> {
  return request('/account/register', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// 获取文章列表API
export async function getArticlesApi(params: ArticleListParams): Promise<ArticleListResponse> {
  return request('/community/queryPostByForum', {
    method: 'POST',
    body: JSON.stringify(params),
  })
}

// 获取当前用户信息API
export async function getProfileApi(): Promise<ApiResponse> {
  return request('/account/profile', {
    method: 'GET',
  })
}

// 获取帖子详情API
export async function getPostDetailApi(id: number): Promise<any> {
  return request(`/community/queryPostContent?postId=${id}`, {
    method: 'GET'
  })
}

// 获取帖子基本信息API
export async function getPostBaseInfoApi(id: number): Promise<any> {
  return request(`/community/queryPostById?postId=${id}`, {
    method: 'GET'
  })
}

// 获取评论列表API
export async function getCommentsByPostIdApi(postId: number, offset = 0, limit = 10): Promise<any> {
  return request(`/community/getCommentsByPostId?postId=${postId}&offset=${offset}&limit=${limit}`, {
    method: 'GET',
  })
}

// 发表评论API
export async function createCommentApi(data: { postId: number; content: string; parentId?: number; repliedUserId?: number; repliedUsername?: string }): Promise<any> {
  return request('/community/createComment', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// 删除评论API
export async function deleteCommentApi(commentId: number): Promise<any> {
  return request(`/community/deleteComment?commentId=${commentId}`, {
    method: 'GET',
  })
}

// 获取子评论API
export async function getCommentsByParentIdApi(parentId: number, offset = 0, limit = 10): Promise<any> {
  return request(`/community/getCommentsByParentId?parentId=${parentId}&offset=${offset}&limit=${limit}`, {
    method: 'GET',
  })
}

// 批量获取多个评论的子评论（每个最多3条）
export async function getCommentsByParentIdsApi(parentIds: number[]): Promise<{ code: string | number, message: string, data: Record<number, any[]> }> {
  return request('/community/getCommentsByParentIds', {
    method: 'POST',
    body: JSON.stringify(parentIds),
  })
}

// 上传头像API
export async function uploadAvatarApi(file: File): Promise<any> {
  const formData = new FormData();
  formData.append('multipartFile', file);
  const response = await fetch(`${API_BASE_URL}/account/uploadAvatar`, {
    method: 'POST',
    body: formData,
    credentials: 'include',
  });
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
}

// 确认更换头像API
export async function confirmAvatarApi(path: string, id: number): Promise<any> {
  return request('/account/confirmAvatar', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({ path, id: id.toString() }).toString(),
  })
}

// 通过用户ID获取用户信息API
export async function getUserInfoApi(userid: string): Promise<any> {
  return request(`/account/userInfo?userId=${encodeURIComponent(userid)}`, {
    method: 'GET',
  })
}

// 登出API
export async function logoutApi(): Promise<any> {
  return request('/account/logout', {
    method: 'GET',
  })
}

// 查询某用户的关注列表API
export async function getFollowerListApi(params: {
  userId: number,
  lastCreateTime?: string,
  lastId?: number,
  needCursor?: boolean
}): Promise<ApiResponse<FollowQueryResult>> {
  const query = new URLSearchParams()
  query.append('userId', params.userId.toString())
  if (params.needCursor) {
    if (params.lastCreateTime) query.append('lastCreateTime', params.lastCreateTime)
    if (params.lastId !== undefined) query.append('lastId', params.lastId.toString())
  }
  query.append('needCursor', params.needCursor ? 'true' : 'false')
  return request(`/follow/followerList?${query.toString()}`, {
    method: 'GET',
  })
}

// 查询某用户的粉丝列表API
export async function getFolloweeListApi(params: {
  userId: number,
  lastCreateTime?: string,
  lastId?: number,
  needCursor?: boolean
}): Promise<ApiResponse<FollowQueryResult>> {
  const query = new URLSearchParams()
  query.append('userId', params.userId.toString())
  if (params.needCursor) {
    if (params.lastCreateTime) query.append('lastCreateTime', params.lastCreateTime)
    if (params.lastId !== undefined) query.append('lastId', params.lastId.toString())
  }
  query.append('needCursor', params.needCursor ? 'true' : 'false')
  return request(`/follow/followeeList?${query.toString()}`, {
    method: 'GET',
  })
}

// 用户关注另一个用户
export async function followUserApi(followerId: number, followeeId: number): Promise<any> {
  return request('/follow/follow', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({ followerId: followerId.toString(), followeeId: followeeId.toString() }).toString(),
  })
}

// 用户取关另一个用户
export async function unfollowUserApi(followerId: number, followeeId: number): Promise<any> {
  return request('/follow/unfollow', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({ followerId: followerId.toString(), followeeId: followeeId.toString() }).toString(),
  })
}

// 查询是否已关注
export async function isFollowApi(follower: number, followee: number): Promise<any> {
  return request(`/follow/isFollow?follower=${follower}&followee=${followee}`, {
    method: 'GET',
  })
}

// 发帖API
export async function createPostApi(data: { title: string; content: string; forum: string }): Promise<any> {
  return request('/community/createPost', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

/**
 * 拉取关注用户的帖子动态
 */
export async function pullFeedApi(params: { userId: number, lastId?: number | string, lastCreateTime?: string }) {
  const queryArr = [
    `userId=${params.userId}`,
    `lastId=${params.lastId ?? ''}`,
    `lastCreateTime=${encodeURIComponent(params.lastCreateTime ?? '')}`
  ]
  return request(`/feed/pullFeed?${queryArr.join('&')}`, {
    method: 'GET',
  })
}

/**
 * 获取点赞通知
 */
export async function getLikeNoticesApi(params: { userId: number, lastId?: number | string, lastCreateTime?: string }) {
  const queryArr = [
    `userId=${params.userId}`,
    `lastId=${params.lastId ?? ''}`,
    `lastCreateTime=${encodeURIComponent(params.lastCreateTime ?? '')}`
  ]
  return request(`/notice/likeNotices?${queryArr.join('&')}`, {
    method: 'GET',
  })
}

/**
 * 获取评论通知
 */
export async function getCommentNoticesApi(params: { userId: number, lastId?: number | string, lastCreateTime?: string }) {
  const queryArr = [
    `userId=${params.userId}`,
    `lastId=${params.lastId ?? ''}`,
    `lastCreateTime=${encodeURIComponent(params.lastCreateTime ?? '')}`
  ]
  return request(`/notice/commentNotices?${queryArr.join('&')}`, {
    method: 'GET',
  })
}

/**
 * 获取新增粉丝通知
 */
export async function getFollowNoticesApi(params: { userId: number, lastId?: number | string, lastCreateTime?: string }) {
  const queryArr = [
    `userId=${params.userId}`,
    `lastId=${params.lastId ?? ''}`,
    `lastCreateTime=${encodeURIComponent(params.lastCreateTime ?? '')}`
  ]
  return request(`/notice/followNotices?${queryArr.join('&')}`, {
    method: 'GET',
  })
}