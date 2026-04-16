import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      dts: 'src/auto-imports.d.ts',
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      dts: 'src/components.d.ts',
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),
  ],
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
      '/game/ai': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/knowledge/ai': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ai/files': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
