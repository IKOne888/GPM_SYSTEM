import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

/**
 * 学员端（用户端）Vite 配置
 * - 开发端口 3002（管理端 3000、后端 8080，避免端口冲突）
 * - /api 反向代理到后端 http://localhost:8080，解决开发环境跨域
 */
export default defineConfig({
  plugins: [
    // Vue 单文件组件支持
    vue(),
    // 自动导入 vue / vue-router / pinia 的 API（ref、computed、useRouter 等）
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts'
    }),
    // 自动按需注册 Element Plus 组件
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      // @ 指向 src 目录，简化模块引用路径
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    // 学员端开发服务端口
    port: 3002,
    proxy: {
      // 所有 /api 请求转发到后端（后端 context-path 为 /api，故不做路径重写）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
