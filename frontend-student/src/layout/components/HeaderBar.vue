<template>
  <!-- 顶部品牌栏：左侧品牌标识，右侧消息铃铛与用户下拉 -->
  <header class="header-bar">
    <!-- 品牌区 -->
    <div class="brand">
      <div class="brand-logo">
        <!-- 盾牌图标：象征质量与合规 -->
        <el-icon :size="22"><Stamp /></el-icon>
      </div>
      <div class="brand-text">
        <span class="brand-title">GMP 培训学习中心</span>
        <span class="brand-sub">药品生产质量管理规范 · 员工培训平台</span>
      </div>
    </div>

    <!-- 右侧操作区 -->
    <div class="header-actions">
      <!-- 消息铃铛 -->
      <MessageBell />

      <!-- 用户下拉 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-trigger">
          <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
          <span class="user-name">{{ userStore.displayName }}</span>
          <el-icon class="arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon> 个人资料
            </el-dropdown-item>
            <el-dropdown-item command="record">
              <el-icon><Tickets /></el-icon> 学习记录
            </el-dropdown-item>
            <el-dropdown-item command="certificate">
              <el-icon><Medal /></el-icon> 培训档案
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MessageBell from './MessageBell.vue'
import { useUserStore } from '@/store/user'
import { useTaskStore } from '@/store/task'

const router = useRouter()
const userStore = useUserStore()
const taskStore = useTaskStore()

/** 头像文字：取姓名首字 */
const avatarText = computed(() => (userStore.displayName || '学').slice(0, 1))

/**
 * 处理用户下拉菜单指令
 * @param {string} command 指令标识
 */
async function handleCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    taskStore.reset()
    ElMessage.success('已安全退出')
    router.push('/login')
    return
  }
  const pathMap = {
    profile: '/profile',
    record: '/profile/record',
    certificate: '/profile/certificate'
  }
  router.push(pathMap[command])
}
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  /* 品牌渐变背景：与管理端保持视觉区分，突出学习中心 */
  background: var(--brand-gradient);
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 12px rgba(22, 119, 255, 0.25);
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.32);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.78);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: 20px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.14);
  transition: background 0.2s;
  outline: none;
}

.user-trigger:hover {
  background: rgba(255, 255, 255, 0.26);
}

.user-avatar {
  background: rgba(255, 255, 255, 0.9);
  color: var(--brand-primary);
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: #fff;
}

.arrow {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}
</style>
