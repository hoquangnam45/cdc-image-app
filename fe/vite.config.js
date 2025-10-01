import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    https: false,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      },
      '/api/image': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
