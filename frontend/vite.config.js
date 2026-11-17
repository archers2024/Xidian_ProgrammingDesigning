import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端以根路径部署，代理去掉 /api/v1 前缀，使拦截器的路径判断与实际 URI 一致
export default defineConfig({
  plugins: [vue()],
  build: {rollupOptions:{output:{manualChunks(id){
    if(id.includes('/node_modules/zrender/'))return 'charts-renderer'
    if(id.includes('/node_modules/echarts/'))return 'charts-core'
    if(id.includes('/node_modules/@vue/')||id.includes('/node_modules/vue/'))return 'framework'
    if(id.includes('/node_modules/lodash'))return 'utilities'
    if(id.includes('/node_modules/@floating-ui/')||id.includes('/node_modules/@popperjs/'))return 'positioning'
    if(id.includes('/node_modules/axios/'))return 'http'
    if(id.includes('/node_modules/dayjs/'))return 'dates'
  }}}},
  preview: {port:4173},
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.CAMPUS_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/v1/, '')
      }
    }
  }
})
