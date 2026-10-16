<template>
  <!-- 学员端主框架：顶部品牌栏 + 左侧菜单 + 内容区 -->
  <div class="app-layout">
    <!-- 顶部品牌栏（品牌渐变背景） -->
    <HeaderBar />

    <div class="app-body">
      <!-- 左侧菜单：答题页（fullscreen）隐藏，保证专注作答 -->
      <SideMenu v-if="!isFullscreen" />

      <main class="app-main" :class="{ 'app-main--full': isFullscreen }">
        <!-- 路由内容区：带淡入动画 -->
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>

        <!-- 页脚版权 -->
        <footer class="app-footer">
          GMP Training Learning Center &copy; 2026 &nbsp;|&nbsp; 药品生产质量管理规范培训平台
        </footer>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import HeaderBar from './components/HeaderBar.vue'
import SideMenu from './components/SideMenu.vue'

const route = useRoute()

/** 是否为专注模式（在线答题页隐藏侧边菜单） */
const isFullscreen = computed(() => !!route.meta?.fullscreen)
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  /* 内容区内边距，营造洁净留白感 */
  padding: 20px 24px 0;
  background: var(--bg-page);
}

.app-main--full {
  padding: 16px 20px 0;
}

.app-footer {
  margin-top: 16px;
  padding: 16px 0 20px;
  text-align: center;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 路由切换动画：轻微上移淡入 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
