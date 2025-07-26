<template>
  <div class="qa-list-page">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-container">
        <div class="logo">
          <span class="logo-text">NS</span>
        </div>
        <TopBar @login-click="showAuthModal = true" />
      </div>
    </header>
    <AuthModal v-if="showAuthModal" @close="showAuthModal = false" @success="showAuthModal = false" />
    <main class="main">
      <div class="main-container">
        <div class="qa-placeholder">
          <h2>问答模块</h2>
          <p>这里是问答区内容占位，后续可补充具体内容。</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TopBar from './TopBar.vue'
import AuthModal from './AuthModal.vue'
const showAuthModal = ref(false)
</script>

<style scoped>
.qa-list-page {
  min-height: 100vh;
  background: #f8fafc;
}
.header {
  background: white;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}
.logo {
  font-size: 24px;
  font-weight: bold;
}
.logo-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.nav {
  display: flex;
  align-items: center;
  gap: 32px;
}
.nav-link {
  color: #64748b;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
  cursor: pointer;
}
.nav-link:hover,
.nav-link.active {
  color: #3b82f6;
}
.nav-dropdown {
  cursor: pointer;
}
.main-container {
  max-width: 1200px;
  margin: 40px auto 0 auto;
  padding: 0 20px;
}
.qa-placeholder {
  min-height: 400px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #888;
  font-size: 20px;
  padding: 60px 0;
}
</style> 