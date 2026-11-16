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
  },
  build: {
    // 主包曾达 1.12MB 触发告警，按体积最大的三个库拆包：
    // 首屏只下载业务代码，第三方库可被浏览器长期缓存
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('element-plus')) return 'element'
          if (id.includes('vue-router') || id.includes('pinia') || id.includes('@vue') || /node_modules[\\/]vue[\\/]/.test(id)) return 'vue'
        }
      }
    },
    chunkSizeWarningLimit: 1100
  }
})
