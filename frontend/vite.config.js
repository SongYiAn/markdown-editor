import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：开发代理与 WebSocket
export default defineConfig({
  plugins: [vue()],
  define: {
    // 兼容部分依赖对 global 的访问
    global: 'globalThis',
  },
  server: {
    // 代理后端 API 与 WS，避免跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
    // 允许所有主机（含 ngrok 等内网穿透域名）
    allowedHosts: 'all',
  },
})
