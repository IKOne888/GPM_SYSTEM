<template>
  <div v-loading="loading" class="task-detail">
    <!-- 返回 -->
    <el-page-header class="page-header" @back="goBack">
      <template #content>
        <span class="header-title">任务详情</span>
      </template>
    </el-page-header>

    <template v-if="detail">
      <!-- 顶部概览卡：任务名 + 状态 + 关键指标 -->
      <div class="overview gmp-card">
        <div class="overview-left">
          <div class="overview-head">
            <h2>{{ detail.name }}</h2>
            <el-tag :type="getTaskStatus(detail.statusCode).type" effect="light">
              {{ getTaskStatus(detail.statusCode).label }}
            </el-tag>
          </div>
          <p class="overview-desc">{{ detail.description || '暂无任务说明' }}</p>
          <!-- 审核进度提示：进入两级审核链后展示当前所处阶段 -->
          <el-alert
            v-if="detail.reviewStatusText"
            :title="`审核进度：${detail.reviewStatusText}`"
            :type="detail.reviewStatus === 3 ? 'error' : (detail.reviewStatus === 2 ? 'success' : 'warning')"
            :closable="false"
            show-icon
            style="margin-bottom: 10px"
          />
          <!-- 被退回时高亮展示退回意见，便于学员了解原因 -->
          <el-alert
            v-if="detail.rejectRemark"
            :title="`退回意见：${detail.rejectRemark}`"
            type="error"
            :closable="false"
            show-icon
            style="margin-bottom: 10px"
          />
          <div class="overview-meta">
            <span><el-icon><Notebook /></el-icon>{{ detail.courseName || '-' }}</span>
            <span v-if="detail.sopCode"><el-icon><Document /></el-icon>SOP：{{ detail.sopCode }} {{ detail.sopVersion }}</span>
            <span><el-icon><Calendar /></el-icon>截止：{{ formatDateTime(detail.deadline) }}</span>
            <span :class="{ 'meta-urgent': countdown(detail.deadline).urgent }">
              <el-icon><AlarmClock /></el-icon>{{ countdown(detail.deadline).text }}
            </span>
            <span v-if="detail.planName"><el-icon><Files /></el-icon>{{ detail.planName }}</span>
          </div>
        </div>

        <!-- 指标区 -->
        <div class="overview-metrics">
          <div class="metric">
            <el-progress
              type="dashboard"
              :percentage="toPercent(detail.videoProgress)"
              :width="88"
              :stroke-width="7"
              :color="progressColor"
            >
              <template #default="{ percentage }">
                <span class="metric-percent">{{ percentage }}%</span>
              </template>
            </el-progress>
            <span class="metric-label">学习进度</span>
          </div>
          <div class="metric">
            <div class="metric-value">{{ formatDuration(detail.studyDuration) }}</div>
            <span class="metric-label">累计学时</span>
          </div>
          <div class="metric">
            <div class="metric-value" :style="{ color: detail.examPass === 1 ? '#52C41A' : '#1F2D3D' }">
              {{ detail.examScore !== null && detail.examScore !== undefined ? detail.examScore + ' 分' : '--' }}
            </div>
            <span class="metric-label">考试成绩</span>
          </div>
          <div class="metric">
            <div class="metric-value">{{ detail.trainingHours || detail.retrainPeriod ? (detail.trainingHours || '-') : '-' }}</div>
            <span class="metric-label">培训学时要求</span>
          </div>
        </div>
      </div>

      <!-- 操作条 -->
      <div class="action-bar gmp-card">
        <div class="action-left">
          <el-icon :size="16" color="#1677FF"><InfoFilled /></el-icon>
          <span>{{ actionTip }}</span>
        </div>
        <div class="action-right">
          <el-button type="primary" :disabled="videos.length === 0" @click="goLearn()">
            {{ Number(detail.videoProgress) > 0 ? '继续学习' : '开始学习' }}
          </el-button>
          <el-tooltip
            :disabled="detail.canExam"
            :content="detail.exam?.reason || '需先完成全部视频学习'"
            placement="top"
          >
            <span class="btn-wrap">
              <el-button type="success" :disabled="!detail.canExam" @click="goExam">
                在线考试
              </el-button>
            </span>
          </el-tooltip>
        </div>
      </div>

      <div class="content-grid">
        <!-- 左栏：视频课件 -->
        <div class="gmp-card panel">
          <div class="panel-head">
            <span class="panel-title">视频课件</span>
            <span class="panel-sub">共 {{ videos.length }} 节，单节观看进度达 95% 视为完成</span>
          </div>

          <el-empty v-if="videos.length === 0" description="该课程暂无视频课件" :image-size="80" />

          <div
            v-for="(video, index) in videos"
            :key="video.id"
            class="video-item"
            :class="{ 'video-item--active': currentVideoId === video.id }"
            @click="goLearn(video.id)"
          >
            <div class="video-index">{{ index + 1 }}</div>
            <div class="video-info">
              <div class="video-name">{{ video.name }}</div>
              <div class="video-meta">
                <span>时长 {{ formatClock(video.duration) }}</span>
                <span>已学 {{ formatDuration(video.studyDuration) }}</span>
              </div>
              <el-progress
                :percentage="toPercent(video.progress)"
                :stroke-width="5"
                :color="progressColor"
                :show-text="false"
                class="video-progress"
              />
            </div>
            <div class="video-status">
              <el-tag v-if="video.isCompleted === 1" type="success" size="small" effect="light">
                <el-icon><Select /></el-icon> 已完成
              </el-tag>
              <el-tag v-else-if="Number(video.progress) > 0" type="primary" size="small" effect="light">
                观看中
              </el-tag>
              <el-tag v-else type="info" size="small" effect="plain">未开始</el-tag>
            </div>
          </div>
        </div>

        <!-- 右栏：课程资料 + 考试信息 -->
        <div class="side-col">
          <!-- 考试信息 -->
          <div class="gmp-card panel">
            <div class="panel-head">
              <span class="panel-title">考试信息</span>
            </div>
            <template v-if="detail.exam">
              <div class="exam-row">
                <span class="exam-label">试卷名称</span>
                <span class="exam-value">{{ detail.exam.paperName }}</span>
              </div>
              <div class="exam-row">
                <span class="exam-label">考试时长</span>
                <span class="exam-value">{{ detail.exam.duration }} 分钟</span>
              </div>
              <div class="exam-row">
                <span class="exam-label">及格分 / 总分</span>
                <span class="exam-value">{{ detail.exam.passScore }} / {{ detail.exam.totalScore }}</span>
              </div>
              <div class="exam-row">
                <span class="exam-label">题目数量</span>
                <span class="exam-value">{{ detail.exam.questionCount }} 题</span>
              </div>
              <div class="exam-row">
                <span class="exam-label">剩余次数</span>
                <span class="exam-value">
                  {{ detail.exam.remainRetry }} / {{ detail.exam.maxRetry }} 次
                </span>
              </div>
              <div v-if="detail.exam.lastScore !== null && detail.exam.lastScore !== undefined" class="exam-row">
                <span class="exam-label">上次成绩</span>
                <span class="exam-value" :style="{ color: detail.exam.lastPass === 1 ? '#52C41A' : '#FF4D4F' }">
                  {{ detail.exam.lastScore }} 分（{{ detail.exam.lastPass === 1 ? '通过' : '未通过' }}）
                </span>
              </div>
            </template>
            <el-empty v-else description="该课程未配置试卷" :image-size="70" />
          </div>

          <!-- 课程资料 -->
          <div class="gmp-card panel">
            <div class="panel-head">
              <span class="panel-title">课程资料</span>
              <span class="panel-sub">{{ files.length }} 个附件</span>
            </div>
            <el-empty v-if="files.length === 0" description="暂无课程资料" :image-size="70" />
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
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskDetail, getCourseFileUrl } from '@/api/study'
import { findLatestRecordId } from '@/api/exam'
import { getTaskStatus } from '@/utils/status'
import {
  formatDateTime,
  formatDuration,
  formatClock,
  formatFileSize,
  countdown,
  toPercent
} from '@/utils/format'

const route = useRoute()
const router = useRouter()

/** 加载状态 */
const loading = ref(false)
/** 任务详情 */
const detail = ref(null)

/** 视频课件列表（来自详情返回） */
const videos = computed(() => detail.value?.videos || [])
/** 课程资料列表（来自详情返回） */
const files = computed(() => detail.value?.files || [])
/** 当前高亮视频：取第一个未完成的视频 */
const currentVideoId = computed(() => {
  const list = videos.value
  const unfinished = list.find((item) => item.isCompleted !== 1)
  return unfinished ? unfinished.id : list[0]?.id
})

/** 操作提示文案 */
const actionTip = computed(() => {
  if (!detail.value) return ''
  // 审核相关状态优先提示：退回需重考，审核中等待结果
  if (detail.value.reviewStatus === 3) return '培训记录已被退回，请重新学习并参加考试'
  if (detail.value.reviewStatus === 0) return '培训记录已提交主管审核，请耐心等待审核结果'
  if (detail.value.reviewStatus === 1) return '主管审核已通过，正在等待 QA 复核'
  if (detail.value.reviewStatus === 2) return '培训记录已通过 QA 复核并归档，可查看培训证书'
  if (detail.value.statusCode === 3) return '任务已完成，可在培训档案中查看证书有效期'
  if (detail.value.canExam) return '视频学习已达标，可以进入在线考试'
  return '请先完成全部视频学习，达标后方可参加考试'
})

/** 进度环配色 */
function progressColor(percentage) {
  if (percentage >= 100) return '#52C41A'
  if (percentage >= 60) return '#1677FF'
  return '#FAAD14'
}

/**
 * 加载任务详情
 */
async function loadDetail() {
  loading.value = true
  try {
    const res = await getTaskDetail(route.params.id)
    detail.value = res.data || null
  } finally {
    loading.value = false
  }
}

/**
 * 跳转在线学习页
 * @param {number} [videoId] 指定视频ID，不传则由学习页自动定位到第一个未完成视频
 */
function goLearn(videoId) {
  const target = videoId || currentVideoId.value
  if (!target) {
    ElMessage.warning('该任务暂无视频课件')
    return
  }
  router.push(`/learn/${detail.value.id}/${target}`)
}

/**
 * 返回上一页（左上角返回箭头）
 * <p>
 * 优先回退一层历史记录，避免与学习页/考试页反复 push 造成历史堆积、返回键可无限点击；
 * 仅在无历史可退（如直接刷新进入本页）时才用 replace 兜底回到工作台。
 */
function goBack() {
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.replace('/dashboard')
  }
}

/**
 * 跳转考试须知页
 */
async function goExam() {
  // 已完成的任务直接查看成绩
  if (detail.value.statusCode === 3) {
    const recordId = await findLatestRecordId(detail.value.id)
    if (recordId) {
      router.push(`/exam/result/${recordId}`)
      return
    }
  }
  router.push(`/exam/enter/${detail.value.id}`)
}

/**
 * 下载课程资料（获取预签名地址后新窗口打开）
 * @param {object} file 资料对象
 */
async function downloadFile(file) {
  try {
    const res = await getCourseFileUrl(file.id)
    if (res.data) {
      window.open(res.data, '_blank')
    }
  } catch (e) {
    // 错误提示已由响应拦截器统一处理
  }
}

onMounted(loadDetail)
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

/* ---------- 概览卡 ---------- */
.overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 24px;
  margin-bottom: 16px;
  /* 顶部渐变细条，突出主任务 */
  border-top: 3px solid transparent;
  border-image: var(--brand-gradient) 1;
}

.overview-left {
  flex: 1;
  min-width: 0;
}

.overview-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overview-head h2 {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
}

.overview-desc {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.6;
}

.overview-meta {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-wrap: wrap;
}

.overview-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-urgent {
  color: var(--danger) !important;
  font-weight: 600;
}

.overview-metrics {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-shrink: 0;
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 88px;
}

.metric-percent {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
}

/* ---------- 操作条 ---------- */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  margin-bottom: 16px;
}

.action-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-regular);
}

.btn-wrap {
  display: inline-block;
}

/* ---------- 内容栅格 ---------- */
.content-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
}

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

/* ---------- 视频列表 ---------- */
.video-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 6px;
}

.video-item:hover,
.video-item--active {
  background: var(--brand-primary-light);
}

.video-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #eef3fa;
  color: var(--text-regular);
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.video-item--active .video-index {
  background: var(--brand-primary);
  color: #fff;
}

.video-info {
  flex: 1;
  min-width: 0;
}

.video-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.video-meta {
  display: flex;
  gap: 14px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.video-progress {
  margin-top: 6px;
}

.video-status {
  width: 74px;
  text-align: right;
  flex-shrink: 0;
}

/* ---------- 右栏 ---------- */
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border-color);
  font-size: 13px;
}

.exam-row:last-child {
  border-bottom: none;
}

.exam-label {
  color: var(--text-secondary);
}

.exam-value {
  color: var(--text-primary);
  font-weight: 500;
}

/* ---------- 资料列表 ---------- */
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

/* 窄屏：单列排布 */
@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .overview {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
