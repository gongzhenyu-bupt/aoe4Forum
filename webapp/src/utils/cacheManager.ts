// 缓存管理工具
class CacheManager {
  // 清理过期的头像缓存
  static cleanExpiredAvatarCache(): void {
    const keys = Object.keys(localStorage)
    const now = Date.now()
    const cachePrefix = 'avatar_cache_'
    const expiryTime = 7 * 24 * 60 * 60 * 1000 // 7天
    
    let cleanedCount = 0
    keys.forEach(key => {
      if (key.startsWith(cachePrefix)) {
        try {
          const cached = localStorage.getItem(key)
          if (cached) {
            const cacheData = JSON.parse(cached)
            if (now - cacheData.timestamp > expiryTime) {
              localStorage.removeItem(key)
              cleanedCount++
            }
          }
        } catch (e) {
          localStorage.removeItem(key)
          cleanedCount++
        }
      }
    })
    
    console.log(`Cleaned ${cleanedCount} expired avatar cache entries`)
  }

  // 获取缓存状态信息
  static getCacheStatus(): { totalSize: number, avatarCacheCount: number, avatarCacheSize: number } {
    const keys = Object.keys(localStorage)
    const avatarKeys = keys.filter(key => key.startsWith('avatar_cache_'))
    
    let totalSize = 0
    let avatarCacheSize = 0
    
    keys.forEach(key => {
      const value = localStorage.getItem(key)
      if (value) {
        totalSize += value.length
        if (key.startsWith('avatar_cache_')) {
          avatarCacheSize += value.length
        }
      }
    })
    
    return {
      totalSize,
      avatarCacheCount: avatarKeys.length,
      avatarCacheSize
    }
  }

  // 清除所有头像缓存
  static clearAllAvatarCache(): void {
    const keys = Object.keys(localStorage)
    const avatarKeys = keys.filter(key => key.startsWith('avatar_cache_'))
    
    avatarKeys.forEach(key => {
      localStorage.removeItem(key)
    })
    
    console.log(`Cleared ${avatarKeys.length} avatar cache entries`)
  }

  // 定期清理缓存（在应用启动时调用）
  static startPeriodicCleanup(): void {
    // 每小时清理一次过期缓存
    setInterval(() => {
      this.cleanExpiredAvatarCache()
    }, 60 * 60 * 1000) // 1小时
    
    // 应用启动时立即清理一次
    this.cleanExpiredAvatarCache()
  }
}

export default CacheManager 