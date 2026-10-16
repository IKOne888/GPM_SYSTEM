<template>
  <div v-loading="loading" class="exam-result">
    <el-page-header class="page-header" @back="goBack">
      <template #content>
        <span class="header-title">考试成绩</span>
      </template>
    </el-page-header>

    <template v-if="result">
      <!-- 成绩概览卡 -->
      <div class="score-card gmp-card" :class="result.pass === 1 ? 'score-card--pass' : 'score-card--fail'">
        <div class="score-left">
          <div class="score-ring">
            <el-progress
              type="dashboard"
              :percentage="scorePercent"
              :width="140"
              :stroke-width="10"
              :color="result.pass === 1 ? '#52C41A' : '#FF4D4F'"
            >
              <template #default>
                <div class="ring-inner">
                  <span class="ring-score">{{ result.score }}</span>
                  <span class="ring-total">/ {{ result.totalScore }} 分</span>
                </div>
              </template>
            </el-progress>
          </div>

          <div class="score-summary">
            <div class="pass-badge" :class="result.pass === 1 ? 'pass' : 'fail'">
              <el-icon :size="22">
                <CircleCheckFilled v-if="result.pass === 1" />
                <CircleCloseFilled v-else />
              </el-icon>
              <span>{{ result.pass === 1 ? '恭喜，考试通过' : '很遗憾，未通过考试' }}</span>
            </div>
            <h2 class="paper-title">{{ result.paperName }}</h2>
            <div class="summary-meta">
              <span><el-icon><Trophy /></el-icon>及格分：{{ result.passScore }} 分</span>
              <span><el-icon><RefreshRight /></el-icon>第 {{ result.retryCount }} 次考试</span>
              <span><el-icon><Clock /></el-icon>用时：{{ formatDuration(result.duration) }}</span>
              <span><el-icon><Calendar /></el-icon>交卷时间：{{ formatDateTime(result.submitTime) }}</span>
            </div>
          </div>
        </div>

        <div class="score-stats">
          <div class="stat">
            <div class="stat-value">{{ result.questionCount }}</div>
            <div class="stat-label">题目总数</div>
          </div>
          <div class="stat">
            <div class="stat-value stat-value--ok">{{ result.correctCount }}</div>
            <div class="stat-label">答对题数</div>
          </div>
          <div class="stat">
            <div class="stat-value stat-value--err">{{ result.questionCount - result.correctCount }}</div>
            <div class="stat-label">答错题数</div>
          </div>
          <div class="stat">
            <div class="stat-value">{{ correctRate }}%</div>
            <div class="stat-label">正确率</div>
          </div>
        </div>
      </div>

      <!-- 结果提示与操作 -->
      <div class="action-bar gmp-card">
        <div class="action-tip">
          <el-icon :size="16" :color="result.pass === 1 ? '#52C41A' : '#FAAD14'"><InfoFilled /></el-icon>
          <span>{{ resultTip }}</span>
        </div>
        <div class="action-buttons">
          <el-button @click="goRecord">查看成绩单</el-button>
          <el-button v-if="result.pass !== 1" type="primary" @click="goRetry">重新考试</el-button>
          <el-button type="primary" plain @click="goBack">返回任务详情</el-button>
        </div>
      </div>

      <!-- 逐题解析 -->
      <div class="gmp-card analysis-panel">
        <div class="panel-head">
          <span class="panel-title">答题解析</span>
          <el-radio-group v-model="filter" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="wrong">只看错题</el-radio-button>
          </el-radio-group>
        </div>

        <el-empty v-if="filteredItems.length === 0" description="太棒了，没有错题！" :image-size="80" />

        <div
          v-for="item in filteredItems"
          :key="item.id"
          class="analysis-item"
          :class="item.isCorrect === 1 ? 'analysis-item--ok' : 'analysis-item--err'"
        >
          <div class="analysis-head">
            <span class="analysis-index">第 {{ item.sort }} 题</span>
            <el-tag type="info" effect="plain" size="small">{{ item.typeName }}</el-tag>
            <el-tag :type="item.isCorrect === 1 ? 'success' : 'danger'" effect="light" size="small">
              {{ item.isCorrect === 1 ? '回答正确' : '回答错误' }}
            </el-tag>
            <span class="analysis-score">得分 {{ item.earnedScore }} / {{ item.score }}</span>
          </div>

          <div class="analysis-content">{{ item.content }}</div>

          <!-- 选项（单选/多选） -->
          <div v-if="item.options && item.options.length > 0" class="analysis-options">
            <div
              v-for="(opt, oi) in item.options"
              :key="oi"
              class="analysis-option"
              :class="optionClass(item, oi)"
            >
              <span class="analysis-option-label">{{ letterOf(oi) }}</span>
              <span class="analysis-option-text">{{ opt.text }}</span>
              <el-icon v-if="isCorrectOption(item, oi)" class="mark mark--ok"><CircleCheckFilled /></el-icon>
              <el-icon v-if="isUserOption(item, oi)" class="mark mark--user"><UserFilled /></el-icon>
            </div>
          </div>

          <!-- 作答对照 -->
          <div class="answer-compare">
            <div class="compare-row">
              <span class="compare-label">你的答案</span>
              <span class="compare-value" :class="item.isCorrect === 1 ? 'value-ok' : 'value-err'">
                {{ item.userAnswer || '未作答' }}
              </span>
            </div>
            <div class="compare-row">
              <span class="compare-label">正确答案</span>
              <span class="compare-value value-ok">{{ item.correctAnswer || '-' }}</span>
            </div>
          </div>

          <!-- 解析 -->
          <div v-if="item.analysis" class="analysis-detail">
            <span class="detail-label"><el-icon><Reading /></el-icon> 答案解析</span>
            <p>{{ item.analysis }}</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExamResult } from '@/api/exam'
import { formatDateTime, formatDuration } from '@/utils/format'

const route = useRoute()
const router = useRouter()

/** 选项字母表 */
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'

/** 加载状态 */
const loading = ref(false)
/** 成绩单数据 */
const result = ref(null)
/** 解析筛选：all-全部 wrong-只看错题 */
const filter = ref('all')

/** 得分率（用于环形进度） */
const scorePercent = computed(() => {
  if (!result.value || !result.value.totalScore) return 0
  return Math.round((result.value.score / result.value.totalScore) * 100)
})

/** 正确率（百分比） */
const correctRate = computed(() => {
  if (!result.value || !result.value.questionCount) return 0
  return Math.round((result.value.correctCount / result.value.questionCount) * 100)
})

/** 结果提示文案 */
const resultTip = computed(() => {
  if (!result.value) return ''
  if (result.value.pass === 1) {
    return '本次考试已通过，任务状态已更新为「已完成」，可在培训档案中查看证书有效期。'
  }
  return '本次考试未通过，请复习课程内容后重新参加考试；若考试次数已用尽，请联系培训管理员重置。'
})

/** 过滤后的解析列表 */
const filteredItems = computed(() => {
  const items = result.value?.items || []
  return filter.value === 'wrong' ? items.filter((item) => item.isCorrect !== 1) : items
})

/** 获取选项字母 */
function letterOf(index) {
  return LETTERS[index] || ''
}

/**
 * 选项样式：正确项标绿、用户选项标蓝/红
 */
function optionClass(item, index) {
  const letter = letterOf(index)
  const classes = []
  if (isCorrectOption(item, index)) classes.push('analysis-option--correct')
  if (isUserOption(item, index)) classes.push('analysis-option--user')
  return classes
}

/** 是否为正确选项（单选/多选按字母匹配） */
function isCorrectOption(item, index) {
  const letter = letterOf(index)
  return String(item.correctAnswer || '').toUpperCase().includes(letter)
}

/** 是否为用户所选选项 */
function isUserOption(item, index) {
  const letter = letterOf(index)
  return String(item.userAnswer || '').toUpperCase().includes(letter)
}

/**
 * 加载成绩单
 */
async function loadResult() {
  loading.value = true
  try {
    const res = await getExamResult(route.params.recordId)
    result.value = res.data || null
  } finally {
    loading.value = false
  }
}

/**
 * 返回任务详情
 * <p>
 * 成绩页与任务详情页之间存在互相跳转，若此处使用 push 会在历史栈中不断叠加记录，
 * 导致返回键可以无限点击、页面无限切换；因此统一采用"弹出历史"而非"压入历史"：
 * 来源是任务详情时回退一层，否则（如从工作台/学习记录直接进入）用 replace 跳转。
 */
function goBack() {
  const prev = (window.history.state && window.history.state.back) || ''
  if (prev.startsWith('/task/')) {
    router.back()
  } else {
    router.replace(result.value?.taskId ? `/task/${result.value.taskId}` : '/dashboard')
  }
}

/** 跳转成绩单列表 */
function goRecord() {
  router.push('/profile/record')
}

/** 重新考试：回到考试须知页 */
function goRetry() {
  router.push(`/exam/enter/${result.value.taskId}`)
}

onMounted(loadResult)
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

/* ---------- 成绩卡 ---------- */
.score-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  margin-bottom: 16px;
  border-top: 3px solid transparent;
}

.score-card--pass {
  border-image: linear-gradient(135deg, #52c41a 0%, #13c2c2 100%) 1;
}

.score-card--fail {
  border-image: linear-gradient(135deg, #ff4d4f 0%, #faad14 100%) 1;
}

.score-left {
  display: flex;
  align-items: center;
  gap: 28px;
  flex: 1;
  min-width: 0;
}

.ring-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ring-score {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.1;
}

.ring-total {
  font-size: 12px;
  color: var(--text-secondary);
}

.score-summary {
  flex: 1;
  min-width: 0;
}

.pass-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.pass-badge.pass {
  background: #f0fae8;
  color: var(--success);
}

.pass-badge.fail {
  background: #fff1f0;
  color: var(--danger);
}

.paper-title {
  margin: 12px 0 10px;
  font-size: 19px;
  color: var(--text-primary);
}

.summary-meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-secondary);
}

.summary-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.score-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  flex-shrink: 0;
  padding-left: 24px;
  border-left: 1px dashed var(--border-color);
}

.stat {
  text-align: center;
  min-width: 74px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-value--ok {
  color: var(--success);
}

.stat-value--err {
  color: var(--danger);
}

.stat-label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* ---------- 操作条 ---------- */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  margin-bottom: 16px;
}

.action-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-regular);
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* ---------- 解析 ---------- */
.analysis-panel {
  padding: 20px 24px 24px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.analysis-item {
  padding: 16px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  margin-bottom: 14px;
  border-left: 3px solid var(--success);
}

.analysis-item--err {
  border-left-color: var(--danger);
}

.analysis-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.analysis-index {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.analysis-score {
  margin-left: auto;
  font-size: 13px;
  color: var(--text-secondary);
}

.analysis-content {
  margin: 12px 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-primary);
}

.analysis-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.analysis-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  border-radius: var(--radius-sm);
  background: #f8fafd;
  font-size: 13px;
  color: var(--text-regular);
}

.analysis-option--correct {
  background: #f0fae8;
  color: #3d8a1a;
}

.analysis-option--user {
  outline: 1px dashed var(--brand-primary);
}

.analysis-option-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e9eef6;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.analysis-option--correct .analysis-option-label {
  background: var(--success);
  color: #fff;
}

.analysis-option-text {
  flex: 1;
}

.mark {
  flex-shrink: 0;
}

.mark--ok {
  color: var(--success);
}

.mark--user {
  color: var(--brand-primary);
}

.answer-compare {
  display: flex;
  gap: 32px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
  flex-wrap: wrap;
}

.compare-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.compare-label {
  color: var(--text-secondary);
}

.compare-value {
  font-weight: 600;
}

.value-ok {
  color: var(--success);
}

.value-err {
  color: var(--danger);
}

.analysis-detail {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: var(--brand-primary-light);
}

.detail-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-primary);
}

.analysis-detail p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-regular);
}

@media (max-width: 1180px) {
  .score-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .score-stats {
    padding-left: 0;
    border-left: none;
    padding-top: 16px;
    border-top: 1px dashed var(--border-color);
    width: 100%;
  }
}
</style>
