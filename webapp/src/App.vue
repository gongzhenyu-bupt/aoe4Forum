<template>
  <div id="app">
    <router-view />
    <CacheStatus />
    <AuthModal 
      v-if="showAuthModal" 
      @close="closeAuthModal"
      @success="handleAuthSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, provide } from 'vue'
import AuthModal from './components/AuthModal.vue'
import CacheStatus from './components/CacheStatus.vue'

const showAuthModal = ref(false)

const openAuthModal = () => {
  showAuthModal.value = true
}

const closeAuthModal = () => {
  showAuthModal.value = false
}

const handleAuthSuccess = () => {
  showAuthModal.value = false
}

// 提供全局的认证弹窗方法
provide('openAuthModal', openAuthModal)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f8fafc;
}

#app {
  width: 100%;
  min-height: 100vh;
}
</style>