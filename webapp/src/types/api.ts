// API 相关类型定义
export interface LoginRequest {
  phoneNo: string
  password: string
}

export interface RegisterRequest {
  name: string
  password: string
  phoneNo: string
  checkCode: string
  checkCodeKey: string
}

export interface CheckCodeResponse {
  data: {
    checkCodeKey: string
    checkCodeBase64: string
  }
  message?: string
  code?: number
}

export interface ApiResponse<T = any> {
  data: T
  message: string
  code: number | string
}

// 文章相关类型
export interface Article {
  id: number
  uuid: string
  userId: number
  userName: string
  forum: string
  title: string
  createTime: string
  updateTime: string
  lastCommentTime: string
  likeCount: number
  dislikeCount: number
  commentCount: number
  pageViewCount: number
  status: number
  postAbstract: string | null;
}

export interface ArticleListParams {
  forum?: string
  offset?: number
  limit?: number
  page?: number
}

export interface ArticleListResponse {
  code: string | number
  message: string
  data: Article[]
}

// 关注/粉丝相关类型
export interface FollowCursorPageRequest {
  userId: number
  lastCreateTime?: string // ISO字符串，分页用
  lastId?: number // 分页用
  pageSize?: number
}

export interface FollowUserInfo {
  id: number
  username: string
  avatar: string
}

export interface FollowQueryResult {
  followers: FollowUserInfo[]
  lastCreateTime: string
  lastId: number
}