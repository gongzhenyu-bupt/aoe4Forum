<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <button class="close-btn" @click="$emit('close')">×</button>
      
      <div class="auth-card">
        <!-- 标题 -->
        <div class="auth-header">
          <h2 class="auth-title">{{ isLogin ? '登录' : '注册' }}</h2>
        </div>

        <!-- 表单内容 -->
        <form @submit.prevent="handleSubmit" class="auth-form">
          <!-- 用户昵称输入（仅注册时显示） -->
          <div v-if="!isLogin" class="form-group">
            <label class="form-label">用户名字</label>
            <input
              v-model="formData.name"
              type="text"
              class="form-input"
              placeholder="请输入用户昵称"
              required
            />
          </div>

          <!-- 手机号输入 -->
          <div class="form-group">
            <label class="form-label">手机号</label>
            <input
              v-model="formData.phoneNo"
              type="tel"
              class="form-input"
              placeholder="请输入11位手机号"
              maxlength="11"
              pattern="[0-9]{11}"
              required
            />
            <div v-if="phoneError" class="error-message">{{ phoneError }}</div>
          </div>

          <!-- 密码输入 -->
          <div class="form-group">
            <div class="password-header">
              <label class="form-label">密码</label>
              <a
                v-if="isLogin"
                href="#"
                class="forgot-password"
                @click.prevent="handleForgotPassword"
              >
                忘记密码？
              </a>
            </div>
            <input
              v-model="formData.password"
              type="password"
              class="form-input"
              placeholder="请输入密码"
              required
            />
          </div>

          <!-- 确认密码输入（仅注册时显示） -->
          <div v-if="!isLogin" class="form-group">
            <label class="form-label">确认密码</label>
            <input
              v-model="formData.confirmPassword"
              type="password"
              class="form-input"
              placeholder="请再次输入密码"
              required
            />
            <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
          </div>

          <!-- 验证码输入（仅注册时显示） -->
          <div v-if="!isLogin" class="form-group">
            <label class="form-label">验证码</label>
            <div class="captcha-container">
              <input
                v-model="formData.checkCode"
                type="text"
                class="form-input captcha-input"
                placeholder="请输入验证码"
                required
              />
              <div class="captcha-image-container">
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="验证码"
                  class="captcha-image"
                  @click="refreshCaptcha"
                  title="点击刷新验证码"
                />
                <button
                  v-else
                  type="button"
                  class="refresh-captcha-btn"
                  @click="refreshCaptcha"
                  :disabled="loadingCaptcha"
                >
                  {{ loadingCaptcha ? '加载中...' : '获取验证码' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 登录时的记住我选项 -->
          <div v-if="isLogin" class="form-options">
            <label class="checkbox-label">
              <input
                v-model="formData.rememberMe"
                type="checkbox"
                class="checkbox-input"
              />
              <span class="checkbox-text">记住我</span>
            </label>
          </div>

          <!-- 注册时的协议同意选项 -->
          <div v-if="!isLogin" class="form-options">
            <label class="checkbox-label">
              <input
                v-model="formData.agreeTerms"
                type="checkbox"
                class="checkbox-input"
                required
              />
              <span class="checkbox-text">
                注册即代表同意
                <a href="#" class="terms-link" @click.prevent="showTerms">服务条款</a>
              </span>
            </label>
          </div>

          <!-- 错误信息显示 -->
          <div v-if="errorMessage" class="error-message global-error">
            {{ errorMessage }}
          </div>

          <!-- 成功信息显示 -->
          <div v-if="successMessage" class="success-message">
            {{ successMessage }}
          </div>

          <!-- 提交按钮 -->
          <button
            type="submit"
            class="submit-button"
            :disabled="loading || !isFormValid"
          >
            <span v-if="loading" class="loading-spinner"></span>
            {{ isLogin ? '登录' : '注册' }}
          </button>
        </form>

        <!-- 切换状态链接 -->
        <div class="auth-switch">
          <span class="switch-text">
            {{ isLogin ? '还没有账号？' : '已经有账号了？' }}
          </span>
          <a
            href="#"
            class="switch-link"
            @click.prevent="toggleAuthMode"
          >
            {{ isLogin ? '马上注册' : '马上登陆' }} 👍
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { loginApi, registerApi, getCheckCodeApi, getProfileApi } from '../utils/api'
import { showSuccess } from '../utils/toast'
import type { LoginRequest, RegisterRequest } from '../types/api'

// 定义事件
const emit = defineEmits<{
  close: []
  success: []
}>()

// 组件状态
const isLogin = ref(true)
const loading = ref(false)
const loadingCaptcha = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const captchaImage = ref('')
const checkCodeKey = ref('')

// 表单数据
const formData = reactive({
  name: '',
  phoneNo: '',
  password: '',
  confirmPassword: '',
  checkCode: '',
  rememberMe: false,
  agreeTerms: false
})

// 表单验证
const phoneError = computed(() => {
  if (formData.phoneNo && !/^1[3-9]\d{9}$/.test(formData.phoneNo)) {
    return '请输入正确的11位手机号'
  }
  return ''
})

const passwordError = computed(() => {
  if (!isLogin.value && formData.password && formData.confirmPassword) {
    if (formData.password !== formData.confirmPassword) {
      return '两次输入的密码不一致'
    }
  }
  return ''
})

const isFormValid = computed(() => {
  const baseValid = formData.phoneNo && formData.password && !phoneError.value
  
  if (isLogin.value) {
    return baseValid
  } else {
    return baseValid && 
           formData.name && 
           formData.confirmPassword && 
           formData.checkCode && 
           formData.agreeTerms && 
           !passwordError.value
  }
})

// 处理遮罩层点击
const handleOverlayClick = () => {
  emit('close')
}

// 获取验证码
const refreshCaptcha = async () => {
  if (loadingCaptcha.value) return
  
  loadingCaptcha.value = true
  errorMessage.value = ''
  
  try {
    const response = await getCheckCodeApi()
    checkCodeKey.value = response.data.checkCodeKey
    captchaImage.value = response.data.checkCodeBase64
  } catch (error) {
    console.error('获取验证码失败:', error)
    errorMessage.value = '获取验证码失败，请重试'
  } finally {
    loadingCaptcha.value = false
  }
}

// 切换登录/注册模式
const toggleAuthMode = () => {
  isLogin.value = !isLogin.value
  clearMessages()
  
  // 清空相关字段
  if (isLogin.value) {
    formData.name = ''
    formData.confirmPassword = ''
    formData.checkCode = ''
    formData.agreeTerms = false
    captchaImage.value = ''
    checkCodeKey.value = ''
  } else {
    formData.rememberMe = false
    // 切换到注册时自动获取验证码
    refreshCaptcha()
  }
}

// 清除消息
const clearMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

// 表单提交处理
const handleSubmit = async () => {
  if (!isFormValid.value) return
  
  loading.value = true
  clearMessages()
  
  try {
    if (isLogin.value) {
      await handleLogin()
    } else {
      await handleRegister()
    }
  } catch (error) {
    console.error('提交失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '操作失败，请重试'
  } finally {
    loading.value = false
  }
}

// 登录处理
const handleLogin = async () => {
  const loginData: LoginRequest = {
    phoneNo: formData.phoneNo,
    password: formData.password
  }
  
  try {
    const response = await loginApi(loginData)
    
    if (response.code === 200 || response.code === "0") {
      successMessage.value = '登录成功！'
      
      
      // 显示成功提示框
      showSuccess('登录成功！')
      
      // 保存token到localStorage
      if (response.data && response.data.token) {
        localStorage.setItem('token', response.data.token)
      }
      
      if (formData.rememberMe) {
        localStorage.setItem('phoneNo', formData.phoneNo)
      }
      
      // 新增：登录成功后获取用户信息并写入localStorage
      try {
        const profile = await getProfileApi()
        if ((profile.code === 0 || profile.code === '0') && profile.data) {
          if (profile.data.id) localStorage.setItem('userid', profile.data.id)
          if (profile.data.avatar) localStorage.setItem('avatar', profile.data.avatar)
          if (profile.data.username) localStorage.setItem('username', profile.data.username)
          if (profile.data.name) localStorage.setItem('username', profile.data.name)
        }
      } catch {}
      
      // 延迟关闭弹窗并触发成功事件
      setTimeout(() => {
        emit('success')
        window.location.reload()
      }, 1500)
    } else {
      errorMessage.value = response.message || '登录失败'
    }
  } catch (error) {
    throw new Error('登录请求失败，请检查网络连接')
  }
}

// 注册处理
const handleRegister = async () => {
  const registerData: RegisterRequest = {
    name: formData.name,
    password: formData.password,
    phoneNo: formData.phoneNo,
    checkCode: formData.checkCode,
    checkCodeKey: checkCodeKey.value
  }
  
  try {
    const response = await registerApi(registerData)
    
    if (response.code === "200" || response.code === "0") {
      console.log('注册返回', response)
      console.log('准备弹Toast')
      showSuccess('注册成功！欢迎加入我们！')
      setTimeout(() => {
        console.log('准备关闭弹窗')
        emit('success')
      }, 1000)
    } else {
      errorMessage.value = response.message || '注册失败'
      // 注册失败时刷新验证码
      refreshCaptcha()
    }
  } catch (error) {
    // 注册失败时刷新验证码
    refreshCaptcha()
    throw new Error('注册请求失败，请检查网络连接')
  }
}

// 忘记密码处理
const handleForgotPassword = () => {
  alert('忘记密码功能待实现')
}

// 显示服务条款
const showTerms = () => {
  alert('服务条款内容')
}

// 监听模式切换，重置表单
watch(isLogin, () => {
  // 保留手机号，清空其他字段
  const phoneNo = formData.phoneNo
  Object.assign(formData, {
    name: '',
    phoneNo,
    password: '',
    confirmPassword: '',
    checkCode: '',
    rememberMe: false,
    agreeTerms: false
  })
  clearMessages()
})

// 组件挂载时从localStorage恢复手机号
onMounted(() => {
  const savedPhoneNo = localStorage.getItem('phoneNo')
  if (savedPhoneNo) {
    formData.phoneNo = savedPhoneNo
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-container {
  position: relative;
  max-width: 400px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  font-size: 24px;
  color: #64748b;
  cursor: pointer;
  z-index: 10;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #374151;
}

.auth-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  padding: 40px;
  position: relative;
  overflow: hidden;
  animation: modalSlideUp 0.3s ease-out;
}

@keyframes modalSlideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.auth-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  letter-spacing: -0.5px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.password-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-password {
  font-size: 14px;
  color: #6366f1;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.forgot-password:hover {
  color: #4f46e5;
}

.form-input {
  padding: 14px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 16px;
  transition: all 0.2s ease;
  background: #f9fafb;
}

.form-input:focus {
  outline: none;
  border-color: #6366f1;
  background: white;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-input::placeholder {
  color: #9ca3af;
}

.captcha-container {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.captcha-input {
  flex: 1;
}

.captcha-image-container {
  flex-shrink: 0;
  width: 120px;
  height: 48px;
}

.captcha-image {
  width: 100%;
  height: 100%;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease;
  object-fit: cover;
}

.captcha-image:hover {
  border-color: #6366f1;
}

.refresh-captcha-btn {
  width: 100%;
  height: 100%;
  background: #f3f4f6;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-captcha-btn:hover:not(:disabled) {
  background: #e5e7eb;
  border-color: #6366f1;
  color: #6366f1;
}

.refresh-captcha-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.form-options {
  margin: 8px 0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #4b5563;
}

.checkbox-input {
  width: 18px;
  height: 18px;
  accent-color: #6366f1;
  cursor: pointer;
}

.terms-link {
  color: #6366f1;
  text-decoration: none;
  font-weight: 500;
}

.terms-link:hover {
  color: #4f46e5;
  text-decoration: underline;
}

.error-message {
  color: #ef4444;
  font-size: 14px;
  margin-top: 4px;
}

.global-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 12px;
  margin-top: 0;
}

.success-message {
  color: #10b981;
  font-size: 14px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.submit-button {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border: none;
  border-radius: 12px;
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
  margin-top: 8px;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(99, 102, 241, 0.3);
}

.submit-button:active {
  transform: translateY(0);
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.auth-switch {
  text-align: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.switch-text {
  color: #6b7280;
  font-size: 14px;
  margin-right: 4px;
}

.switch-link {
  color: #6366f1;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  transition: color 0.2s ease;
}

.switch-link:hover {
  color: #4f46e5;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .modal-overlay {
    padding: 16px;
  }
  
  .auth-card {
    padding: 24px;
  }
  
  .auth-title {
    font-size: 24px;
  }
  
  .form-input {
    padding: 12px 14px;
    font-size: 16px;
  }
  
  .submit-button {
    padding: 14px;
  }
  
  .captcha-container {
    flex-direction: column;
    gap: 8px;
  }
  
  .captcha-image-container {
    width: 100%;
    height: 60px;
  }
}
</style>