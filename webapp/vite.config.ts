import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    allowedHosts: ['www.aoe4forum.cn', '101.126.22.249','aoe4forum.cn'],
    port: 5173
  },
  define: {
    global: 'globalThis',
  }
})