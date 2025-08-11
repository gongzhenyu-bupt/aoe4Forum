<script setup lang="ts">
import { ref, onMounted } from 'vue'
import CacheManager from '../utils/cacheManager'

const show = ref(false)
const status = ref({
  totalSize: 0,
  avatarCacheCount: 0,
  avatarCacheSize: 0
})

const formatSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const refreshStatus = () => {
  status.value = CacheManager.getCacheStatus()
}

const clearCache = () => {
  CacheManager.clearAllAvatarCache()
  refreshStatus()
}

onMounted(() => {
  refreshStatus()
  // 开发环境下显示缓存状态
  if (import.meta.env.DEV) {
    show.value = true
  }
})
</script>

<style scoped>
.cache-status {
  position: fixed;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 10px;
  border-radius: 5px;
  font-size: 12px;
  z-index: 9999;
}

.status-item {
  display: flex;
  justify-content: space-between;
  margin: 5px 0;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 5px;
}
</style> 