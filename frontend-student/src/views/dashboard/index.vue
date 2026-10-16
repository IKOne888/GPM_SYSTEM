<template>
  <div class="dashboard">
    <!-- 页头：标题 + 刷新 -->
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">我的任务</h2>
        <p class="gmp-page-subtitle">
          共 {{ stats.total }} 项培训任务，请按时完成视频学习与在线考试
        </p>
      </div>
      <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
    </div>

    <!-- 状态统计卡：点击可快速筛选 -->
    <div class="stat-grid">
      <div
        v-for="card in statCards"
        :key="card.label"
        class="stat-card gmp-card gmp-card--hoverable"
        :class="{ 'stat-card--active': query.status === card.status }"
        @click="filterByStatus(card.status)"
      >
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar gmp-card">
      <el-radio-group v-model="query.status" @change="handleSearch">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button v-for="item in TASK_STATUS_OPTIONS" :key="item.value" :value="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>

      <el-input
        v-model="query.keyword"
        placeholder="搜索任务名称"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </div>

    <!-- 任务卡片列表 -->
    <div v-loading="loading" class="task-list">
      <el-empty
        v-if="!loading && tasks.length === 0"
        description="暂无符合条件的培训任务"
      />

      <div
        v-for="task in tasks"
        :key="task.id"
        class="task-card gmp-card gmp-card--hoverable"
      >
        <!-- 任务基本信息 -->
        <div class="task-main">
          <div class="task-head">
            <span class="task-name" @click="goDetail(task)">{{ task.name }}</span>
            <el-tag :type="getTaskStatus(task.statusCode).type" effect="light" size="small">
              {{ getTaskStatus(task.statusCode).label }}
            </el-tag>
            <el-tag
              v-if="task.planName"
              type="info"
              effect="plain"
              size="small"
            >
              {{ task.planName }}
            </el-tag>
          </div>

          <div class="task-meta">
            <span>
              <el-icon><Notebook /></el-icon>
              {{ task.courseName || '未关联课程' }}
            </span>
            <span>
              <el-icon><Calendar /></el-icon>
              截止：{{ formatDateTime(task.deadline) }}
            </span>
            <span :class="{ 'meta-urgent': countdown(task.deadline).urgent }">
              <el-icon><AlarmClock /></el-icon>
              {{ countdown(task.deadline).text }}
            </span>
            <span>
              <el-icon><Timer /></el-icon>
              已学 {{ formatDuration(task.studyDuration) }}
            </span>
            <span v-if="task.examScore !== null && task.examScore !== undefined">
              <el-icon><Trophy /></el-icon>
              成绩：<b :style="{ color: task.examPass === 1 ? '#52C41A' : '#FF4D4F' }">
                {{ task.examScore }} 分
              </b>
            </span>
          </div>
        </div>

        <!-- 学习进度环 -->
        <div class="task-progress">
          <el-progress
            type="dashboard"
            :percentage="toPercent(task.videoProgress)"
            :width="82"
            :stroke-width="7"
            :color="progressColor"
          >
            <template #default="{ percentage }">
              <span class="progress-text">{{ percentage }}%</span>
            </template>
          </el-progress>
          <span class="progress-label">学习进度</span>
        </div>

        <!-- 操作区 -->
        <div class="task-action">
          <el-button type="primary" @click="handlePrimary(task)">
            {{ primaryActionText(task) }}
          </el-button>
          <el-button @click="goDetail(task)">任务详情</el-button>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadTasks"
          @size-change="handleSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getTasks } from '@/api/study'
import { findLatestRecordId } from '@/api/exam'
import { useTaskStore } from '@/store/task'
import { getTaskStatus, TASK_STATUS_OPTIONS } from '@/utils/status'
import { formatDateTime, formatDuration, countdown, toPercent } from '@/utils/format'

const router = useRouter()
const taskStore = useTaskStore()

/** 列表加载状态 */
const loading = ref(false)
/** 任务列表数据 */
const tasks = ref([])
/** 总条数 */
const total = ref(0)

/** 查询条件（status 为空串表示全部） */
const query = reactive({
  page: 1,
  size: 10,
  status: '',
  keyword: ''
})

/** 统计结果（来自 Store，多页面共享） */
const stats = computed(() => taskStore.stats)

/** 顶部统计卡配置 */
const statCards = computed(() => [
  { label: '待学习', value: stats.value.notStarted, status: 'not_started', color: '#8C9AAE', bg: '#F2F5F9', icon: 'Document' },
  { label: '学习中', value: stats.value.studying, status: 'studying', color: '#1677FF', bg: '#E8F1FF', icon: 'VideoPlay' },
  { label: '待考试', value: stats.value.waitingExam, status: 'waiting_exam', color: '#FAAD14', bg: '#FFF7E6', icon: 'EditPen' },
  { label: '审核中', value: stats.value.pendingReview, status: 'pending_review', color: '#722ED1', bg: '#F5F0FF', icon: 'Stamp' },
  { label: '已退回', value: stats.value.rejected, status: 'rejected', color: '#FF7875', bg: '#FFF1F0', icon: 'CircleClose' },
  { label: '已完成', value: stats.value.completed, status: 'completed', color: '#52C41A', bg: '#F0FAE8', icon: 'CircleCheck' },
  { label: '已逾期', value: stats.value.overdue, status: 'overdue', color: '#FF4D4F', bg: '#FFF1F0', icon: 'WarningFilled' }
])

/** 进度环配色：按完成度分档，增强直观性 */
function progressColor(percentage) {
  if (percentage >= 100) return '#52C41A'
  if (percentage >= 60) return '#1677FF'
  return '#FAAD14'
}

/**
 * 加载任务列表
 */
async function loadTasks() {
  loading.value = true
  try {
    const res = await getTasks({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      keyword: query.keyword || undefined
    })
    tasks.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

/**
 * 条件变更后重新查询（回到第一页）
 */
function handleSearch() {
  query.page = 1
  loadTasks()
}

/**
 * 点击统计卡按状态筛选（再次点击取消筛选）
 * @param {string} status 状态编码
 */
function filterByStatus(status) {
  query.status = query.status === status ? '' : status
  handleSearch()
}

/**
 * 刷新统计与列表
 */
async function reloadAll() {
  await Promise.all([taskStore.fetchStats(true), loadTasks()])
}

/**
 * 主操作按钮文案
 * @param {object} task 任务对象
 * @returns {string} 按钮文案
 */
function primaryActionText(task) {
  const code = task.statusCode
  if (code === 3) return '查看成绩'
  if (code === 2) return '去考试'
  if (code === 1) return '继续学习'
  if (code === 4) return task.videoProgress >= 95 ? '去考试' : '继续学习'
  return '开始学习'
}

/**
 * 主操作按钮行为分发
 * @param {object} task 任务对象
 */
async function handlePrimary(task) {
  const code = task.statusCode
  if (code === 3) {
    await goResult(task)
    return
  }
  const percent = Number(task.videoProgress) || 0
  if (code === 2 || (code === 4 && percent >= 95)) {
    router.push(`/exam/enter/${task.id}`)
    return
  }
  router.push(`/learn/${task.id}`)
}

/**
 * 跳转任务详情
 * @param {object} task 任务对象
 */
function goDetail(task) {
  router.push(`/task/${task.id}`)
}

/**
 * 跳转成绩解析页（通过成绩单反查最近一次考试记录）
 * @param {object} task 任务对象
 */
async function goResult(task) {
  try {
    const recordId = await findLatestRecordId(task.id)
    if (recordId) {
      router.push(`/exam/result/${recordId}`)
      return
    }
    ElMessage.info('暂未查询到该任务的考试记录')
    router.push('/profile/record')
  } catch (e) {
    router.push('/profile/record')
  }
}

onMounted(() => {
  reloadAll()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

/* ---------- 统计卡 ---------- */
.stat-grid {
  display: grid;
  /* 自适应列宽：卡片数量变化（现有 7 张）时自动换行，避免固定列数导致的挤压 */
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  cursor: pointer;
  border: 1px solid transparent;
}

.stat-card--active {
  border-color: var(--brand-primary);
  box-shadow: var(--shadow-hover);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}

.stat-label {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---------- 筛选栏 ---------- */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 260px;
}

/* ---------- 任务卡片 ---------- */
.task-list {
  min-height: 240px;
}

.task-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 18px 20px;
  margin-bottom: 14px;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.task-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  cursor: pointer;
  transition: color 0.2s;
}

.task-name:hover {
  color: var(--brand-primary);
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 10px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-wrap: wrap;
}

.task-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 临期/逾期提示使用警示色 */
.meta-urgent {
  color: var(--danger) !important;
  font-weight: 600;
}

.task-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 96px;
  flex-shrink: 0;
}

.progress-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.progress-label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

.task-action {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 112px;
  flex-shrink: 0;
}

.task-action .el-button {
  margin-left: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* 窄屏适配：统计卡两列、任务卡纵向排列 */
@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .task-card {
    flex-wrap: wrap;
  }
}
</style>
