<template>
  <!-- 消息铃铛：展示未读数与最近消息，支持单条/全部标记已读 -->
  <el-popover
    placement="bottom-end"
    :width="340"
    trigger="click"
    @show="loadMessages"
  >
    <template #reference>
      <div class="bell">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
      </div>
    </template>

    <!-- 消息面板 -->
    <div class="msg-panel">
      <div class="msg-header">
        <span class="msg-title">消息通知</span>
        <el-button
          v-if="messages.length > 0 && unreadCount > 0"
          link
          type="primary"
          size="small"
          @click="handleReadAll"
        >
          全部已读
        </el-button>
      </div>

      <div v-loading="loading" class="msg-list">
        <template v-if="messages.length > 0">
          <div
            v-for="item in messages"
            :key="item.id"
            class="msg-item"
            :class="{ 'msg-item--unread': item.isRead === 0 }"
            @click="handleRead(item)"
          >
            <div class="msg-item-top">
              <el-tag :type="messageTypeTag(item.messageType)" size="small" effect="light">
                {{ messageTypeLabel(item.messageType) }}
              </el-tag>
              <span class="msg-time">{{ formatDateTime(item.createTime) }}</span>
            </div>
            <div class="msg-item-title">{{ item.title }}</div>
            <div class="msg-item-content">{{ item.content }}</div>
          </div>
        </template>
        <el-empty v-else description="暂无消息通知" :image-size="70" />
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMessages, getUnreadCount, readMessage, readAllMessages } from '@/api/profile'
import { MESSAGE_TYPE } from '@/utils/status'
import { formatDateTime } from '@/utils/format'

/** 未读消息数量 */
const unreadCount = ref(0)
/** 最近消息列表（最多展示 10 条） */
const messages = ref([])
/** 加载状态 */
const loading = ref(false)

/** 消息类型文案 */
function messageTypeLabel(type) {
  return MESSAGE_TYPE[type]?.label || '通知'
}

/** 消息类型标签色 */
function messageTypeTag(type) {
  return MESSAGE_TYPE[type]?.type || 'info'
}

/**
 * 拉取未读数量（顶部红点）
 */
async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = Number(res.data) || 0
  } catch (e) {
    // 静默失败，不干扰主流程
  }
}

/**
 * 拉取最近消息列表
 */
async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessages({ page: 1, size: 10 })
    messages.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

/**
 * 单条消息标记已读
 * @param {object} item 消息对象
 */
async function handleRead(item) {
  if (item.isRead === 1) return
  await readMessage(item.id)
  item.isRead = 1
  unreadCount.value = Math.max(0, unreadCount.value - 1)
}

/**
 * 全部标记已读
 */
async function handleReadAll() {
  await readAllMessages()
  messages.value.forEach((item) => {
    item.isRead = 1
  })
  unreadCount.value = 0
  ElMessage.success('已全部标记为已读')
}

// 组件挂载后立即获取未读数，保证红点即时可见
onMounted(loadUnreadCount)

// 暴露刷新方法，便于其他页面（如考试完成后）主动刷新未读数
defineExpose({ refresh: loadUnreadCount })
</script>

<style scoped>
.bell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  transition: background 0.2s;
}

.bell:hover {
  background: rgba(255, 255, 255, 0.28);
}

/* 避免徽标颜色与品牌栏冲突 */
.bell :deep(.el-badge__content) {
  border: none;
}

.msg-panel {
  margin: -6px -6px;
}

.msg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.msg-list {
  max-height: 360px;
  overflow-y: auto;
  padding-top: 6px;
}

.msg-item {
  padding: 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s;
}

.msg-item:hover {
  background: #f5f8fd;
}

.msg-item--unread {
  background: var(--brand-primary-light);
}

.msg-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.msg-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.msg-item-title {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.msg-item-content {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-regular);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
