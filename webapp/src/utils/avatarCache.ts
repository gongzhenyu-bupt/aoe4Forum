// 头像缓存工具类
class AvatarCache {
  private static readonly CACHE_PREFIX = 'avatar_cache_'
  private static readonly CACHE_EXPIRY = 7 * 24 * 60 * 60 * 1000 // 7天过期

  // 获取缓存的头像URL
  static async getAvatarUrl(avatar: string): Promise<string> {
    if (!avatar) return 'https://dummyimage.com/36x36'
    
    const cacheKey = this.CACHE_PREFIX + this.hashString(avatar)
    const cached = localStorage.getItem(cacheKey)
    
    if (cached) {
      try {
        const cacheData = JSON.parse(cached)
        // 检查是否过期
        if (Date.now() - cacheData.timestamp < this.CACHE_EXPIRY) {
          return cacheData.dataUrl
        } else {
          // 过期了，删除缓存
          localStorage.removeItem(cacheKey)
        }
      } catch (e) {
        localStorage.removeItem(cacheKey)
      }
    }
    
    // 缓存中没有或已过期，从网络获取
    return this.fetchAndCacheAvatar(avatar, cacheKey)
  }

  // 从网络获取头像并缓存
  private static async fetchAndCacheAvatar(avatar: string, cacheKey: string): Promise<string> {
    try {
      let imageUrl: string
      
      // 判断是否是默认头像路径
      if (avatar.startsWith('/defaultImg/')) {
        imageUrl = `http://www.aoe4forum.cn:7071${avatar}`
      } else {
        // 只取文件名，拼接为Spring Boot静态资源URL
        const filename = avatar.replace(/\\/g, '/').split('/').pop()
        imageUrl = filename ? `http://www.aoe4forum.cn:7071/avatarImg/${filename}` : 'https://dummyimage.com/36x36'
      }
      
      // 获取图片并转换为base64
      const response = await fetch(imageUrl)
      if (!response.ok) throw new Error('Failed to fetch avatar')
      
      const blob = await response.blob()
      const dataUrl = await this.blobToDataUrl(blob)
      
      // 缓存到localStorage
      const cacheData = {
        dataUrl,
        timestamp: Date.now()
      }
      localStorage.setItem(cacheKey, JSON.stringify(cacheData))
      
      return dataUrl
    } catch (error) {
      console.error('Failed to cache avatar:', error)
      return 'https://dummyimage.com/36x36'
    }
  }

  // 将blob转换为data URL
  private static blobToDataUrl(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as string)
      reader.onerror = reject
      reader.readAsDataURL(blob)
    })
  }

  // 简单的字符串哈希函数
  private static hashString(str: string): string {
    let hash = 0
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i)
      hash = ((hash << 5) - hash) + char
      hash = hash & hash // 转换为32位整数
    }
    return hash.toString()
  }

  // 清理过期的缓存
  static cleanExpiredCache(): void {
    const keys = Object.keys(localStorage)
    const now = Date.now()
    
    keys.forEach(key => {
      if (key.startsWith(this.CACHE_PREFIX)) {
        try {
          const cached = localStorage.getItem(key)
          if (cached) {
            const cacheData = JSON.parse(cached)
            if (now - cacheData.timestamp > this.CACHE_EXPIRY) {
              localStorage.removeItem(key)
            }
          }
        } catch (e) {
          localStorage.removeItem(key)
        }
      }
    })
  }

  // 清除所有头像缓存
  static clearAllCache(): void {
    const keys = Object.keys(localStorage)
    keys.forEach(key => {
      if (key.startsWith(this.CACHE_PREFIX)) {
        localStorage.removeItem(key)
      }
    })
  }
}

export default AvatarCache 