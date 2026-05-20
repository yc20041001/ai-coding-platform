import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
              return 'vendor-vue'
            }
            if (id.includes('element-plus')) {
              return 'vendor-element'
            }
            if (id.includes('markdown-it') || id.includes('highlight.js')) {
              return 'vendor-markdown'
            }
            if (id.includes('@element-plus/icons-vue') || id.includes('lucide')) {
              return 'vendor-icons'
            }
            return 'vendor'
          }
        },
      },
    },
  },
})
