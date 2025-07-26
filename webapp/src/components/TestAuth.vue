<template>
  <div class="test-auth">
    <h2>认证功能测试</h2>
    
    <div class="test-buttons">
      <button @click="testRegister" class="test-btn register-btn">
        测试注册
      </button>
      <button @click="testLogin" class="test-btn login-btn">
        测试登录
      </button>
      <button @click="testToast" class="test-btn toast-btn">
        测试Toast
      </button>
      <button @click="checkCookies" class="test-btn cookie-btn">
        检查Cookies
      </button>
    </div>
    
    <div class="test-results">
      <h3>测试结果：</h3>
      <pre>{{ testResults }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showSuccess } from '../utils/toast'
import { getCookie } from '../utils/cookie'

const testResults = ref('')

const testRegister = () => {
  testResults.value = '测试注册功能...\n'
  showSuccess('注册成功！欢迎加入我们！')
  testResults.value += '✅ 注册成功提示已显示\n'
}

const testLogin = () => {
  testResults.value = '测试登录功能...\n'
  showSuccess('登录成功！')
  testResults.value += '✅ 登录成功提示已显示\n'
}

const testToast = () => {
  testResults.value = '测试Toast组件...\n'
  showSuccess('这是一个测试提示消息！')
  testResults.value += '✅ Toast提示已显示\n'
}

const checkCookies = () => {
  testResults.value = '检查Cookies...\n'
  const token = getCookie('token')
  const userId = getCookie('userId')
  const userName = getCookie('userName')
  
  testResults.value += `Token: ${token || '未设置'}\n`
  testResults.value += `UserId: ${userId || '未设置'}\n`
  testResults.value += `UserName: ${userName || '未设置'}\n`
}
</script>

<style scoped>
.test-auth {
  padding: 40px;
  max-width: 600px;
  margin: 0 auto;
}

.test-auth h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #1f2937;
}

.test-buttons {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 30px;
}

.test-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.register-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.login-btn {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
}

.toast-btn {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.cookie-btn {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
  color: white;
}

.test-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.test-results {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
}

.test-results h3 {
  margin-bottom: 16px;
  color: #374151;
}

.test-results pre {
  background: #1f2937;
  color: #f9fafb;
  padding: 16px;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style> 