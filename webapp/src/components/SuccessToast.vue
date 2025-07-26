<template>
  <Transition name="toast">
    <div v-if="visible" class="success-toast">
      <div class="toast-content">
        <div class="toast-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
        <div class="toast-message">{{ message }}</div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Props {
  message: string
  duration?: number
}

const props = withDefaults(defineProps<Props>(), {
  duration: 3000
})

const visible = ref(false)

onMounted(() => {
  visible.value = true
  
  setTimeout(() => {
    visible.value = false
  }, props.duration)
})
</script>

<style scoped>
.success-toast {
  position: fixed;
  top: 20%;
  left: 50%;
  transform: translate(-50%, 0);
  z-index: 9999;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(16, 185, 129, 0.3);
  min-width: 300px;
  max-width: 400px;
  text-align: center;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toast-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
}

.toast-icon svg {
  width: 100%;
  height: 100%;
}

.toast-message {
  font-size: 16px;
  font-weight: 500;
  line-height: 1.4;
}

/* 动画效果 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.9);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.9);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .success-toast {
    top: 10px;
    right: 10px;
    left: 10px;
    min-width: auto;
    max-width: none;
  }
}
</style> 