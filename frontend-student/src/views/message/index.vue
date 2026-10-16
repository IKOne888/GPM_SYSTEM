<template>
  <div class="message-page">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">消息中心</h2>
        <p class="gmp-page-subtitle">
          共 {{ unreadCount }} 条未读消息，点击消息可直接进入对应任务
        </p>
      </div>
      <el-button
        type="primary"
        plain
        :disabled="unreadCount === 0"
        @click="handleReadAll"
      >
        全部标记已读
      </el-button>
    </div>

    <div class="message-layout">
      <!-- 左侧：类型筛选 -->
      <div class="filter-card gmp-card">
        <div class="filter-group-title">阅读状态</div>
        <div
          v-for="item in readFilters"
          :key="String(item.value)"
          class="filter-item"
          :class="{ 'filter-item--active': query.isRead === item.value }"
          @click="selectRead(item.value)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>

        <div class="filter-group-title filter-group-title--gap">消息类型</div>
        <div
          v-for="(item, code) in MESSAGE_TYPE"
          :key="code"
          class="filter-item"
          :class="{ 'filter-item--active': query.messageType === Number(code) }"
          @click="selectType(Number(code))"
        >
          <el-icon><component :is="typeIcon(Number(code))" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>

      <!-- 右侧：消息列表 -->
      <div v-loading="loading" class="list-card gmp-card">
        <template v-if="list.length > 0">
          <div
            v-for="item in list"
            :key="item.id"
            class="msg-item"
            :class="{ 'msg-item--unread': item.isRead === 0 }"
            @click="handleClick(item)"
          >
            <!-- 未读圆点 -->
            <span class="unread-dot" :class="{ 'unread-dot--hidden': item.isRead === 1 }"></span>

            <div class="msg-body">
              <div class="msg-top">
                <el-tag :type="typeTag(item.messageType)" effect="light" size="small">
                  {{ typeLabel(item.messageType) }}
                </el-tag>
                <span class="msg-title">{{ item.title }}</span>
                <span class="msg-time">{{ formatDateTime(item.createTime) }}</span>
              </div>
              <div class="msg-content">{{ item.content }}</div>
              <div v-if="canJump(item)" class="msg-jump">
                <el-icon><Position /></el-icon>
                点击查看对应任务
              </div>
            </div>

            <el-button
              v-if="item.isRead === 0"
              link
              type="primary"
              class="read-btn"
              @click.stop="handleRead(item)"
            >
              标记已读
            </el-button>
          </div>
        </template>

        <el-empty v-else description="暂无符合条件的消息" />

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadList"
            @size-change="handleSearch"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 消息中心页面
 * <p>
 * 复用基础版消息接口：GET /study/messages（新增 messageType / isRead 筛选参数）、
 * GET /study/messages/unread-count、POST /study/messages/{id}/read、POST /study/messages/read-all。
 * 点击消息若关联任务（relatedId 非空且类型为 1/2/3）则跳转对应任务详情。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMessages, getUnreadCount, readMessage, readAllMessages } from '@/api/profile'
import { MESSAGE_TYPE } from '@/utils/status'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

/** 阅读状态筛选项（true-已读 false-未读 '' -全部） */
const readFilters = [
  { label: '全部消息', value: '', icon: 'Message' },
  { label: '未读消息', value: false, icon: 'Bell' },
  { label: '已读消息', value: true, icon: 'CircleCheck' }
]

/** 加载状态 */
const loading = ref(false)
/** 消息列表 */
const list = ref([])
/** 总数 */
const total = ref(0)
/** 未读数 */
const unreadCount = ref(0)

/** 查询条件（默认只看未读，聚焦待处理消息） */
const query = reactive({
  page: 1,
  size: 10,
  isRead: '',
  messageType: null
})

/** 消息类型文案 */
function typeLabel(type) {
  return MESSAGE_TYPE[type]?.label || '通知'
}

/** 消息类型标签色 */
function typeTag(type) {
  return MESSAGE_TYPE[type]?.type || 'info'
}

/** 消息类型图标 */
function typeIcon(type) {
  const map = { 1: 'Tickets', 2: 'AlarmClock', 3: 'RefreshRight', 4: 'InfoFilled' }
  return map[type] || 'Bell'
}

/**
 * 是否可跳转任务详情（任务通知/到期提醒/补考提醒 且关联ID非空）
 */
function canJump(item) {
  return item.relatedId && [1, 2, 3].includes(item.messageType)
}

/**
 * 加载消息列表
 */
async function loadList() {
  loading.value = true
  try {
    const res = await getMessages({
      page: query.page,
      size: query.size,
      isRead: query.isRead === '' ? undefined : query.isRead,
      messageType: query.messageType || undefined
    })
    list.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

/**
 * 加载未读数
 */
async function loadUnread() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = Number(res.data) || 0
  } catch (e) {
    // 静默失败
  }
}

/** 条件变更后重新查询（回到第一页） */
function handleSearch() {
  query.page = 1
  loadList()
}

/**
 * 选择阅读状态筛选（再次点击取消，回到全部）
 * @param {string|boolean} value 筛选项
 */
function selectRead(value) {
  query.isRead = query.isRead === value ? '' : value
  handleSearch()
}

/**
 * 选择消息类型筛选（再次点击取消）
 * @param {number} code 类型编码
 */
function selectType(code) {
  query.messageType = query.messageType === code ? null : code
  handleSearch()
}

/**
 * 单条标记已读
 * @param {object} item 消息对象
 */
async function handleRead(item) {
  if (item.isRead === 1) return
  await readMessage(item.id)
  item.isRead = 1
  unreadCount.value = Math.max(0, unreadCount.value - 1)
}

/**
 * 点击消息：标记已读并按需跳转任务详情
 * @param {object} item 消息对象
 */
async function handleClick(item) {
  await handleRead(item)
  if (canJump(item)) {
    router.push(`/task/${item.relatedId}`)
  }
}

/**
 * 全部标记已读
 */
async function handleReadAll() {
  await readAllMessages()
  ElMessage.success('已全部标记为已读')
  await Promise.all([loadUnread(), loadList()])
}

onMounted(() => {
  loadUnread()
  loadList()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.message-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  align-items: start;
}

/* ---------- 左侧筛选 ---------- */
.filter-card {
  padding: 14px 12px;
}

.filter-group-title {
  padding: 6px 10px;
  font-size: 12px;
  color: var(--text-secondary);
}

.filter-group-title--gap {
  margin-top: 10px;
  border-top: 1px dashed var(--border-color);
  padding-top: 14px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--text-regular);
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 2px;
}

.filter-item:hover {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
}

.filter-item--active {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  font-weight: 600;
}

/* ---------- 右侧列表 ---------- */
.list-card {
  padding: 8px 16px 16px;
  min-height: 320px;
}

.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 16px 6px;
  border-bottom: 1px dashed var(--border-color);
  cursor: pointer;
  transition: background 0.2s;
}

.msg-item:last-of-type {
  border-bottom: none;
}

.msg-item:hover {
  background: #f7faff;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--danger);
  margin-top: 7px;
  flex-shrink: 0;
}

.unread-dot--hidden {
  background: transparent;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.msg-title {
  font-size: 15px;
  color: var(--text-primary);
}

/* 未读消息标题加粗 */
.msg-item--unread .msg-title {
  font-weight: 600;
}

.msg-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-secondary);
}

.msg-content {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.msg-jump {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--brand-primary);
}

.read-btn {
  flex-shrink: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 1000px) {
  .message-layout {
    grid-template-columns: 1fr;
  }
}
</style>
