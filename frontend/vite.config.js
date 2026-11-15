import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端以根路径部署，代理去掉 /api/v1 前缀，使拦截器的路径判断与实际 URI 一致
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/v1/, '')
      }
    }
  }
})
