import { createApp, h } from 'vue'
import SuccessToast from '../components/SuccessToast.vue'

interface ToastOptions {
  message: string
  duration?: number
}

/**
 * 显示成功提示
 * @param options 提示选项
 */
export function showSuccessToast(options: ToastOptions): void {
  // 创建容器
  const container = document.createElement('div')
  document.body.appendChild(container)
  
  // 创建Vue应用
  const app = createApp({
    render() {
      return h(SuccessToast, {
        message: options.message,
        duration: options.duration,
        onDestroy: () => {
          // 组件销毁后清理
          app.unmount()
          document.body.removeChild(container)
        }
      })
    }
  })
  
  // 挂载应用
  app.mount(container)
  
  // 自动清理（备用方案）
  setTimeout(() => {
    if (document.body.contains(container)) {
      app.unmount()
      document.body.removeChild(container)
    }
  }, (options.duration || 3000) + 1000)
}

/**
 * 显示成功提示（简化版）
 * @param message 提示消息
 */
export function showSuccess(message: string): void {
  showSuccessToast({ message })
} 