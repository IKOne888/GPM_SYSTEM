<template>
  <!-- 应用根组件：仅承载路由出口，具体布局由 layout/index.vue 负责 -->
  <router-view />
</template>

<script setup>
// 根组件无业务逻辑，页面切换交由 Vue Router 处理
</script>

<style>
/* 根组件样式统一放在 styles/theme.css，此处保持空以方便全局重置 */
</style>
