<template>
  <div class="calendar-page">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">培训日历</h2>
        <p class="gmp-page-subtitle">按日期查看培训任务截止与考试安排</p>
      </div>
      <el-button :icon="Refresh" @click="loadEvents">刷新</el-button>
    </div>

    <div class="calendar-layout">
      <!-- 左侧：月历 -->
      <div v-loading="loading" class="calendar-card gmp-card">
        <el-calendar v-model="currentDate">
          <template #date-cell="{ data }">
            <div class="day-cell" :class="{ 'day-cell--selected': isSelected(data) }">
              <span class="day-num">{{ data.day.split('-')[2] }}</span>
              <!-- 事件标记：最多展示 2 个圆点 + 超出数量 -->
              <div v-if="eventsOf(data.day).length" class="day-marks">
                <i
                  v-for="(evt, i) in eventsOf(data.day).slice(0, 2)"
                  :key="i"
                  class="mark"
                  :style="{ background: eventColor(evt) }"
                ></i>
                <span v-if="eventsOf(data.day).length > 2" class="mark-more">
                  +{{ eventsOf(data.day).length - 2 }}
                </span>
              </div>
            </div>
          </template>
        </el-calendar>
      </div>

      <!-- 右侧：当日事件清单 + 图例 -->
      <div class="side-panel">
        <!-- 当日事件 -->
        <div class="side-card gmp-card">
          <div class="card-head">
            <span class="card-title">{{ selectedDateText }} 安排</span>
            <span class="card-sub">{{ selectedEvents.length }} 项</span>
          </div>

          <el-empty
            v-if="selectedEvents.length === 0"
            description="当日暂无培训安排"
            :image-size="70"
          />
          <div v-else class="event-list">
            <div
              v-for="(evt, index) in selectedEvents"
              :key="index"
              class="event-item"
              @click="goEvent(evt)"
            >
              <span class="event-bar" :style="{ background: eventColor(evt) }"></span>
              <div class="event-main">
                <span class="event-title">{{ evt.title }}</span>
                <span class="event-time">
                  <el-icon><Clock /></el-icon>
                  {{ eventTimeText(evt) }}
                </span>
              </div>
              <el-tag :type="evt.type === 1 ? 'warning' : 'primary'" effect="light" size="small">
                {{ evt.type === 1 ? '截止' : '考试' }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 近期到期 -->
        <div class="side-card gmp-card">
          <div class="card-head">
            <span class="card-title">近期到期</span>
            <span class="card-sub">按日期升序</span>
          </div>

          <el-empty v-if="upcoming.length === 0" description="暂无临近截止任务" :image-size="70" />
          <div v-else class="upcoming-list">
            <div
              v-for="(evt, index) in upcoming"
              :key="index"
              class="upcoming-item"
              @click="goEvent(evt)"
            >
              <div class="upcoming-date">
                <span class="upcoming-day">{{ evt.date.slice(5) }}</span>
              </div>
              <span class="upcoming-title">{{ evt.title }}</span>
            </div>
          </div>
        </div>

        <!-- 图例 -->
        <div class="side-card gmp-card">
          <div class="card-head">
            <span class="card-title">图例</span>
          </div>
          <div class="legend">
            <span><i class="mark" style="background: #faad14"></i> 任务截止日</span>
            <span><i class="mark" style="background: #1677ff"></i> 考试安排日</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 培训日历页面
 * <p>
 * 数据来源：GET /study/calendar?startDate&endDate（仅返回有事件的日期，跨度上限 62 天）。
 * 本页按「当前显示月份」整月拉取，切换月份时自动重新加载。
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getCalendar } from '@/api/calendar'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

/** 当前选中日期（el-calendar 双向绑定） */
const currentDate = ref(new Date())
/** 加载状态 */
const loading = ref(false)
/** 日期 -> 事件列表 映射（键为 yyyy-MM-dd） */
const dateMap = ref({})

/** 选中日期文本（yyyy-MM-dd） */
const selectedDateText = computed(() => formatDay(currentDate.value))

/** 当前选中日期的事件 */
const selectedEvents = computed(() => dateMap.value[selectedDateText.value] || [])

/** 近期到期列表（任务截止事件，按日期升序，取前 6 条） */
const upcoming = computed(() => {
  const result = []
  Object.keys(dateMap.value).forEach((day) => {
    ;(dateMap.value[day] || []).forEach((evt) => {
      if (evt.type === 1) {
        result.push({ ...evt, date: day })
      }
    })
  })
  return result.sort((a, b) => a.date.localeCompare(b.date)).slice(0, 6)
})

/**
 * Date 对象格式化为 yyyy-MM-dd（本地时区，避免 toISOString 时区偏移）
 * @param {Date} date 日期对象
 * @returns {string} yyyy-MM-dd
 */
function formatDay(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/**
 * 取某日事件列表
 * @param {string} day yyyy-MM-dd
 */
function eventsOf(day) {
  return dateMap.value[day] || []
}

/**
 * 事件颜色：截止=警告色，考试=主色
 */
function eventColor(evt) {
  return evt.type === 1 ? '#faad14' : '#1677ff'
}

/**
 * 事件时间文案（仅取时分）
 */
function eventTimeText(evt) {
  const text = formatDateTime(evt.time)
  return text === '-' ? '全天' : text.slice(11)
}

/**
 * 判断日期单元格是否为当前选中日期
 */
function isSelected(data) {
  return data.day === selectedDateText.value
}

/**
 * 加载当前显示月份的日历事件
 */
async function loadEvents() {
  loading.value = true
  try {
    const year = currentDate.value.getFullYear()
    const month = currentDate.value.getMonth()
    // 计算当月首日与末日（本地时区）
    const start = new Date(year, month, 1)
    const end = new Date(year, month + 1, 0)
    const res = await getCalendar({
      startDate: formatDay(start),
      endDate: formatDay(end)
    })
    // 转换为 日期 -> 事件 映射，便于单元格快速检索
    const map = {}
    ;(res.data || []).forEach((item) => {
      map[item.date] = item.events || []
    })
    dateMap.value = map
  } finally {
    loading.value = false
  }
}

/**
 * 点击事件条目 → 跳转对应任务详情
 */
function goEvent(evt) {
  if (evt.taskId) {
    router.push(`/task/${evt.taskId}`)
  }
}

// 切换月份（currentDate 的 年/月 变化）时重新加载
watch(
  () => [currentDate.value.getFullYear(), currentDate.value.getMonth()].join('-'),
  () => loadEvents()
)

onMounted(loadEvents)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.calendar-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  align-items: start;
}

/* ---------- 月历 ---------- */
.calendar-card {
  padding: 10px 16px 16px;
}

/* 覆盖 el-calendar 内部样式，贴近品牌风格 */
.calendar-card :deep(.el-calendar__header) {
  padding: 10px 6px;
}

.calendar-card :deep(.el-calendar__title) {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.calendar-card :deep(.el-calendar-table td) {
  border-color: var(--border-color);
}

.calendar-card :deep(.el-calendar-table .el-calendar-day) {
  height: 76px;
  padding: 6px;
}

.day-cell {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-radius: var(--radius-sm);
  padding: 2px 4px;
  transition: background 0.2s;
}

.day-cell--selected {
  background: var(--brand-primary-light);
}

.day-num {
  font-size: 13px;
  color: var(--text-regular);
}

.day-marks {
  display: flex;
  align-items: center;
  gap: 4px;
}

.mark {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.mark-more {
  font-size: 11px;
  color: var(--text-secondary);
}

/* ---------- 右侧面板 ---------- */
.side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  padding: 16px 18px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-sub {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 事件列表 */
.event-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.event-item:hover {
  border-color: var(--brand-primary);
  background: #f7faff;
}

.event-bar {
  width: 4px;
  height: 30px;
  border-radius: 2px;
  flex-shrink: 0;
}

.event-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.event-title {
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 近期到期 */
.upcoming-list {
  display: flex;
  flex-direction: column;
}

.upcoming-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 4px;
  border-bottom: 1px dashed var(--border-color);
  cursor: pointer;
}

.upcoming-item:last-child {
  border-bottom: none;
}

.upcoming-item:hover .upcoming-title {
  color: var(--brand-primary);
}

.upcoming-date {
  flex-shrink: 0;
}

.upcoming-day {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 600;
}

.upcoming-title {
  flex: 1;
  font-size: 13px;
  color: var(--text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

/* 图例 */
.legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: var(--text-regular);
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1180px) {
  .calendar-layout {
    grid-template-columns: 1fr;
  }
}
</style>
