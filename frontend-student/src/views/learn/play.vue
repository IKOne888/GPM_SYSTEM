<template>
  <div class="learn-page">
    <!-- 页头 -->
    <el-page-header class="page-header" @back="goBack">
      <template #content>
        <span class="header-title">{{ taskName || '在线学习' }}</span>
      </template>
      <template #extra>
        <div class="header-extra">
          <span class="save-tip" :class="{ 'save-tip--ok': saveState === 'saved' }">
            <el-icon v-if="saveState === 'saving'"><Loading /></el-icon>
            <el-icon v-else-if="saveState === 'saved'"><CircleCheck /></el-icon>
            <el-icon v-else><Clock /></el-icon>
            {{ saveStateText }}
          </span>
          <el-button type="primary" plain @click="goTaskDetail">返回任务</el-button>
        </div>
      </template>
    </el-page-header>

    <div v-loading="loading" class="learn-grid">
      <!-- 左栏：视频播放器 + 进度操作 -->
      <div class="player-col">
        <div class="gmp-card player-card">
          <!-- 视频播放器（原生 video，直接请求 /study/videos/{id}/stream） -->
          <div class="video-wrapper">
            <video
              v-if="currentVideo"
              ref="videoRef"
              class="video-el"
              :src="streamUrl"
              controls
              controlsList="nodownload"
              preload="metadata"
              @loadedmetadata="handleLoadedMetadata"
              @timeupdate="handleTimeUpdate"
              @play="handlePlay"
              @pause="handlePause"
              @ended="handleEnded"
              @error="handleVideoError"
            ></video>
            <el-empty v-else description="该任务暂无视频课件" />
          </div>

          <!-- 视频信息与进度条 -->
          <div v-if="currentVideo" class="player-info">
            <div class="player-title">
              <span>{{ currentVideo.name }}</span>
              <el-tag v-if="videoCompleted" type="success" size="small" effect="light">
                <el-icon><Select /></el-icon> 本节已完成
              </el-tag>
            </div>
            <div class="player-meta">
              <span>时长 {{ formatClock(currentVideo.duration) }}</span>
              <span>已学 {{ formatDuration(displayStudyDuration) }}</span>
              <span>观看进度 {{ currentPercent }}%</span>
            </div>
            <el-progress
              :percentage="currentPercent"
              :stroke-width="8"
              :color="progressColor"
              class="main-progress"
            />
          </div>
        </div>

        <!-- 学习要求提示 -->
        <div class="gmp-card tip-card">
          <el-icon :size="16" color="#1677FF"><InfoFilled /></el-icon>
          <div>
            <p class="tip-title">学习要求</p>
            <p class="tip-text">
              每节视频观看进度达到 95% 即视为完成，全部视频完成后任务进入「待考试」；
              系统每 15 秒自动保存一次学习进度，关闭页面后可从上次位置继续播放。
            </p>
          </div>
        </div>

        <!-- 课程资料 -->
        <div v-if="files.length > 0" class="gmp-card panel">
          <div class="panel-head">
            <span class="panel-title">课程资料</span>
            <span class="panel-sub">{{ files.length }} 个附件</span>
          </div>
          <div
            v-for="file in files"
            :key="file.id"
            class="file-item"
            @click="downloadFile(file)"
          >
            <el-icon class="file-icon"><Document /></el-icon>
            <div class="file-info">
              <div class="file-name">{{ file.name || file.fileName }}</div>
              <div class="file-meta">{{ file.fileType }} · {{ formatFileSize(file.fileSize) }}</div>
            </div>
            <el-icon class="file-download"><Download /></el-icon>
          </div>
        </div>
      </div>

      <!-- 右栏：课件目录 + 任务进度 -->
      <div class="side-col">
        <!-- 任务整体进度 -->
        <div class="gmp-card panel">
          <div class="panel-head">
            <span class="panel-title">学习进度</span>
          </div>
          <div class="task-progress-wrap">
            <el-progress
              type="dashboard"
              :percentage="taskPercent"
              :width="110"
              :stroke-width="9"
              :color="progressColor"
            >
              <template #default="{ percentage }">
                <span class="task-percent">{{ percentage }}%</span>
              </template>
            </el-progress>
            <div class="task-progress-meta">
              <p>已完成 {{ completedCount }} / {{ videos.length }} 节</p>
              <p>累计学时 {{ formatDuration(displayStudyDuration) }}</p>
              <p v-if="canExam" class="exam-ready">
                <el-icon><CircleCheckFilled /></el-icon> 学习已达标，可参加考试
              </p>
            </div>
          </div>
          <el-button
            type="success"
            class="exam-btn"
            :disabled="!canExam"
            @click="goExam"
          >
            进入在线考试
          </el-button>
        </div>

        <!-- 课件目录 -->
        <div class="gmp-card panel">
          <div class="panel-head">
            <span class="panel-title">课件目录</span>
            <span class="panel-sub">{{ videos.length }} 节</span>
          </div>
          <div
            v-for="(video, index) in videos"
            :key="video.id"
            class="catalog-item"
            :class="{ 'catalog-item--active': currentVideo && currentVideo.id === video.id }"
            @click="switchVideo(video)"
          >
            <el-icon class="catalog-icon">
              <VideoPlay v-if="currentVideo && currentVideo.id === video.id" />
              <CircleCheckFilled v-else-if="video.isCompleted === 1" />
              <VideoCamera v-else />
            </el-icon>
            <div class="catalog-info">
              <div class="catalog-name">{{ index + 1 }}. {{ video.name }}</div>
              <div class="catalog-meta">
                {{ formatClock(video.duration) }} · {{ toPercent(video.progress) }}%
              </div>
            </div>
            <el-tag v-if="video.isCompleted === 1" type="success" size="small" effect="plain">
              完成
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskDetail, getTaskVideos, getVideoRecord, reportProgress, getCourseFileUrl } from '@/api/study'
import { formatDuration, formatClock, formatFileSize, toPercent } from '@/utils/format'

const route = useRoute()
const router = useRouter()

/** 上报节流间隔（毫秒） */
const REPORT_INTERVAL = 15000
/** 单视频完成阈值（百分比） */
const COMPLETE_THRESHOLD = 95

/** 视频元素引用 */
const videoRef = ref(null)
/** 加载状态 */
const loading = ref(false)
/** 任务名称 */
const taskName = ref('')
/** 视频课件列表 */
const videos = ref([])
/** 课程资料列表 */
const files = ref([])
/** 当前播放的视频 */
const currentVideo = ref(null)
/** 任务整体进度（百分比） */
const taskPercent = ref(0)
/** 任务累计学时（秒，服务端返回） */
const taskStudyDuration = ref(0)
/** 是否可参加考试 */
const canExam = ref(false)

/** 服务器端已保存的单视频进度（用于断点续播与防回退） */
const savedProgress = ref(0)
/** 服务器端已保存的学习时长（秒） */
const savedDuration = ref(0)
/** 本次会话新增播放秒数（未上报部分） */
const sessionSeconds = ref(0)
/** 已达成的最大进度（避免上报回退） */
const maxProgress = ref(0)
/** 当前播放位置（秒） */
const currentTime = ref(0)
/** 自动保存状态：idle-未变更 saving-保存中 saved-已保存 */
const saveState = ref('idle')
/** 定时上报器 */
let timer = null
/** 最近一次上报后是否又有变更 */
let dirty = false

/** 视频流地址（无 token，由后端白名单放行） */
const streamUrl = computed(() => {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return currentVideo.value ? `${base}/study/videos/${currentVideo.value.id}/stream` : ''
})

/** 当前观看进度（百分比，取已达最大值） */
const currentPercent = computed(() => Math.max(maxProgress.value, toPercent(savedProgress.value)))

/** 当前视频是否已达标 */
const videoCompleted = computed(() => currentPercent.value >= COMPLETE_THRESHOLD)

/** 已完成视频数量 */
const completedCount = computed(() => videos.value.filter((v) => v.isCompleted === 1).length)

/** 展示用学习时长（服务端 + 本次未上报） */
const displayStudyDuration = computed(() => savedDuration.value + sessionSeconds.value)

/** 保存状态文案 */
const saveStateText = computed(() => {
  if (saveState.value === 'saving') return '正在保存进度...'
  if (saveState.value === 'saved') return '进度已保存'
  return '进度自动保存中'
})

/** 进度配色 */
function progressColor(percentage) {
  if (percentage >= 100) return '#52C41A'
  if (percentage >= 60) return '#1677FF'
  return '#FAAD14'
}

/**
 * 加载任务、视频列表与课程资料
 */
async function loadAll() {
  loading.value = true
  try {
    const taskId = route.params.taskId
    const [detailRes, videosRes] = await Promise.all([
      getTaskDetail(taskId),
      getTaskVideos(taskId)
    ])
    const detail = detailRes.data || {}
    taskName.value = detail.name || ''
    files.value = detail.files || []
    taskPercent.value = toPercent(detail.videoProgress)
    taskStudyDuration.value = detail.studyDuration || 0
    canExam.value = !!detail.canExam

    videos.value = videosRes.data || []

    // 定位目标视频：URL 指定 > 第一个未完成 > 第一个
    const targetId = route.params.videoId ? Number(route.params.videoId) : null
    const target =
      videos.value.find((v) => v.id === targetId) ||
      videos.value.find((v) => v.isCompleted !== 1) ||
      videos.value[0]
    if (target) {
      await selectVideo(target)
    }
  } finally {
    loading.value = false
  }
}

/**
 * 切换目标视频：加载该视频的学习记录并初始化本地状态
 * @param {object} video 视频对象
 */
async function selectVideo(video) {
  currentVideo.value = video
  savedProgress.value = Number(video.progress) || 0
  savedDuration.value = video.studyDuration || 0
  sessionSeconds.value = 0
  maxProgress.value = savedProgress.value
  currentTime.value = 0
  saveState.value = 'idle'
  dirty = false

  // 拉取断点续播位置（以记录接口为准，比列表数据更新）
  try {
    const res = await getVideoRecord(video.id, route.params.taskId)
    const record = res.data || {}
    savedProgress.value = Number(record.progress) || savedProgress.value
    savedDuration.value = record.studyDuration || savedDuration.value
    maxProgress.value = Math.max(maxProgress.value, savedProgress.value)
    record.lastPosition = record.lastPosition || 0
    // 记录断点位置，待元数据加载完成后跳转
    pendingSeek.value = record.lastPosition
  } catch (e) {
    pendingSeek.value = 0
  }

  await nextTick()
  applySeek()
}

/** 待跳转的播放位置（秒） */
const pendingSeek = ref(0)

/**
 * 元数据加载完成后执行断点续播
 */
function handleLoadedMetadata() {
  applySeek()
}

/**
 * 应用断点续播位置
 */
function applySeek() {
  const el = videoRef.value
  if (!el || !pendingSeek.value || !el.duration) return
  // 剩余不足 5 秒则从头播放，避免直接触发 ended
  if (pendingSeek.value < el.duration - 5) {
    el.currentTime = pendingSeek.value
  }
  pendingSeek.value = 0
}

/**
 * 播放进度变化：累计观看秒数并标记需要保存
 */
function handleTimeUpdate() {
  const el = videoRef.value
  if (!el || !el.duration) return
  currentTime.value = el.currentTime
  const percent = Math.min(100, (el.currentTime / el.duration) * 100)
  if (percent > maxProgress.value) {
    maxProgress.value = Math.round(percent * 100) / 100
  }
  if (el.currentTime > 0) {
    dirty = true
  }
}

/** 开始播放：启动定时上报 */
function handlePlay() {
  startTimer()
}

/** 暂停：立即上报一次 */
function handlePause() {
  stopTimer()
  saveProgress()
}

/** 播放结束：进度置为 100 并上报 */
function handleEnded() {
  stopTimer()
  maxProgress.value = 100
  saveProgress(true)
}

/** 视频加载失败提示 */
function handleVideoError() {
  ElMessage.error('视频加载失败，请稍后重试或联系管理员')
}

/**
 * 启动定时上报（每 15 秒）
 */
function startTimer() {
  if (timer) return
  timer = setInterval(() => {
    saveProgress()
  }, REPORT_INTERVAL)
}

/**
 * 停止定时上报
 */
function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

/**
 * 上报学习进度
 * @param {boolean} force 是否强制上报（忽略变更标记）
 */
async function saveProgress(force = false) {
  if (!currentVideo.value || !route.params.taskId) return
  if (!force && !dirty) return

  const el = videoRef.value
  const position = el && el.duration ? Math.floor(el.currentTime) : Math.floor(currentTime.value)
  // 累计播放时长（绝对值覆盖，避免重复累加）
  const duration = Math.round(savedDuration.value + sessionSeconds.value)
  const progress = force && maxProgress.value >= COMPLETE_THRESHOLD
    ? Math.max(maxProgress.value, 100)
    : maxProgress.value

  saveState.value = 'saving'
  try {
    await reportProgress({
      taskId: Number(route.params.taskId),
      videoId: currentVideo.value.id,
      progress: Math.round(progress * 100) / 100,
      lastPosition: position,
      studyDuration: duration
    })
    // 上报成功后同步本地基数，避免重复累计
    savedProgress.value = progress
    savedDuration.value = duration
    sessionSeconds.value = 0
    dirty = false
    saveState.value = 'saved'
    // 首次达标时刷新任务与视频列表，同步状态到「待考试」
    if (progress >= COMPLETE_THRESHOLD) {
      await refreshAfterComplete()
    }
  } catch (e) {
    saveState.value = 'idle'
    // 错误提示由响应拦截器统一处理
  }
}

/**
 * 视频达标后刷新任务进度与视频列表
 */
async function refreshAfterComplete() {
  const taskId = route.params.taskId
  try {
    const [detailRes, videosRes] = await Promise.all([
      getTaskDetail(taskId),
      getTaskVideos(taskId)
    ])
    const detail = detailRes.data || {}
    taskPercent.value = toPercent(detail.videoProgress)
    taskStudyDuration.value = detail.studyDuration || 0
    canExam.value = !!detail.canExam
    videos.value = videosRes.data || []
    // 同步当前视频状态（列表刷新后对象会被替换）
    const refreshed = videos.value.find((v) => v.id === currentVideo.value?.id)
    if (refreshed) {
      currentVideo.value = refreshed
      savedDuration.value = refreshed.studyDuration || savedDuration.value
    }
  } catch (e) {
    // 忽略刷新失败，不影响播放
  }
}

/**
 * 切换视频（切换前先保存当前进度）
 * @param {object} video 目标视频
 */
async function switchVideo(video) {
  if (currentVideo.value && currentVideo.value.id === video.id) return
  stopTimer()
  await saveProgress(true)
  pendingSeek.value = 0
  await selectVideo(video)
  // 同步地址栏，便于刷新后保持
  router.replace(`/learn/${route.params.taskId}/${video.id}`)
}

/**
 * 返回任务详情（左上角返回箭头）
 */
function goBack() {
  backToTaskDetail()
}

/**
 * 跳转任务详情（页头「返回任务」按钮）
 * <p>
 * 该按钮语义明确指向任务详情：来源本身就是任务详情时回退一层，
 * 否则（如从工作台直接进入学习页）用 replace 兜底跳转，两种情况都不会新增历史记录。
 */
function goTaskDetail() {
  const prev = (window.history.state && window.history.state.back) || ''
  if (prev.startsWith('/task/')) {
    router.back()
  } else {
    router.replace(`/task/${route.params.taskId}`)
  }
}

/**
 * 回退到任务详情
 * <p>
 * 优先回退一层历史记录：播放页与任务详情页之间存在互相跳转，
 * 若此处使用 push 会在历史栈中不断叠加记录，导致返回键可以无限点击、页面无限切换；
 * 仅在无历史可退（如直接刷新进入本页）时才用 replace 兜底，避免再新增历史。
 */
function backToTaskDetail() {
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.replace(`/task/${route.params.taskId}`)
  }
}

/**
 * 进入在线考试
 */
async function goExam() {
  stopTimer()
  await saveProgress(true)
  router.push(`/exam/enter/${route.params.taskId}`)
}

/**
 * 下载课程资料
 * @param {object} file 资料对象
 */
async function downloadFile(file) {
  try {
    const res = await getCourseFileUrl(file.id)
    if (res.data) {
      window.open(res.data, '_blank')
    }
  } catch (e) {
    // 错误提示已统一处理
  }
}

/**
 * 页面隐藏时保存（切换标签页）
 */
function handleVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    saveProgress(true)
  }
}

// 路由离开前保存进度
onBeforeRouteLeave(async () => {
  stopTimer()
  await saveProgress()
})

onMounted(() => {
  loadAll()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  stopTimer()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

/**
 * 关闭/刷新页面时尽力上报一次（同步请求，可能被浏览器中断）
 */
function handleBeforeUnload() {
  if (!dirty || !currentVideo.value) return
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem('student_token') || ''
  // 使用同步 XHR 保证请求有机会发出（sendBeacon 无法携带 Authorization 头）
  try {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${base}/study/videos/progress`, false)
    xhr.setRequestHeader('Content-Type', 'application/json')
    xhr.setRequestHeader('Authorization', token)
    xhr.send(JSON.stringify({
      taskId: Number(route.params.taskId),
      videoId: currentVideo.value.id,
      progress: maxProgress.value,
      lastPosition: Math.floor(currentTime.value),
      studyDuration: Math.round(savedDuration.value + sessionSeconds.value)
    }))
  } catch (e) {
    // 忽略关闭时的上报失败
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-extra {
  display: flex;
  align-items: center;
  gap: 14px;
}

.save-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.save-tip--ok {
  color: var(--success);
}

.learn-grid {
  display: grid;
  grid-template-columns: 1.7fr 1fr;
  gap: 16px;
}

.player-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

/* ---------- 播放器 ---------- */
.player-card {
  overflow: hidden;
}

.video-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-el {
  width: 100%;
  height: 100%;
  background: #000;
  outline: none;
}

.player-info {
  padding: 16px 18px 18px;
}

.player-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.player-meta {
  display: flex;
  gap: 18px;
  margin: 10px 0 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---------- 提示卡 ---------- */
.tip-card {
  display: flex;
  gap: 12px;
  padding: 14px 18px;
  background: var(--brand-primary-light);
  border-color: #cfe1ff;
}

.tip-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.tip-text {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-regular);
}

/* ---------- 面板 ---------- */
.panel {
  padding: 18px 20px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel-sub {
  font-size: 12px;
  color: var(--text-secondary);
}

/* ---------- 任务进度 ---------- */
.task-progress-wrap {
  display: flex;
  align-items: center;
  gap: 20px;
}

.task-percent {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.task-progress-meta p {
  margin: 0 0 6px;
  font-size: 13px;
  color: var(--text-regular);
}

.exam-ready {
  color: var(--success) !important;
  display: flex;
  align-items: center;
  gap: 4px;
}

.exam-btn {
  width: 100%;
  margin-top: 16px;
}

/* ---------- 课件目录 ---------- */
.catalog-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 6px;
}

.catalog-item:hover,
.catalog-item--active {
  background: var(--brand-primary-light);
}

.catalog-icon {
  font-size: 17px;
  color: var(--brand-primary);
  flex-shrink: 0;
}

.catalog-item:not(.catalog-item--active) .catalog-icon {
  color: var(--text-secondary);
}

.catalog-info {
  flex: 1;
  min-width: 0;
}

.catalog-name {
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.catalog-meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* ---------- 资料 ---------- */
.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
}

.file-item:hover {
  background: var(--brand-primary-light);
}

.file-icon {
  font-size: 20px;
  color: var(--brand-primary);
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.file-download {
  color: var(--text-secondary);
  flex-shrink: 0;
}

@media (max-width: 1180px) {
  .learn-grid {
    grid-template-columns: 1fr;
  }
}
</style>
