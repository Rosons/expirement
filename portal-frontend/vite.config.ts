import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 与 Spring 控制器前缀一致：/v1/ai/chat、/v2/ai/chat（原 /ai 无法匹配）
      '/v1/ai': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/v2/ai': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
