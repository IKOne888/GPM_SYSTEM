<template>
  <div v-loading="loading" class="exam-enter">
    <el-page-header class="page-header" @back="goBack">
      <template #content>
        <span class="header-title">考试须知</span>
      </template>
    </el-page-header>

    <template v-if="info">
      <!-- 试卷概览 -->
      <div class="paper-card gmp-card">
        <div class="paper-head">
          <div class="paper-icon">
            <el-icon :size="26"><EditPen /></el-icon>
          </div>
          <div class="paper-title-wrap">
            <h2>{{ info.paperName }}</h2>
            <p>请仔细阅读考试须知，确认后开始答题</p>
          </div>
          <el-tag
            v-if="info.lastPass !== null && info.lastPass !== undefined"
            :type="info.lastPass === 1 ? 'success' : 'danger'"
            effect="light"
            size="large"
          >
            上次{{ info.lastPass === 1 ? '已通过' : '未通过' }} · {{ info.lastScore }} 分
          </el-tag>
        </div>

        <div class="paper-metrics">
          <div class="metric">
            <div class="metric-value">{{ info.questionCount }}</div>
            <div class="metric-label">题目数量</div>
          </div>
          <div class="metric">
            <div class="metric-value">{{ info.duration }}</div>
            <div class="metric-label">考试时长（分钟）</div>
          </div>
          <div class="metric">
            <div class="metric-value">{{ info.totalScore }}</div>
            <div class="metric-label">试卷总分</div>
          </div>
          <div class="metric">
            <div class="metric-value metric-value--pass">{{ info.passScore }}</div>
            <div class="metric-label">及格分数</div>
          </div>
          <div class="metric">
            <div class="metric-value">{{ info.remainRetry }}</div>
            <div class="metric-label">剩余次数</div>
          </div>
        </div>
      </div>

      <!-- 考试须知条目 -->
      <div class="gmp-card notice-card">
        <div class="panel-head">
          <span class="panel-title">考试须知</span>
        </div>
        <ul class="notice-list">
          <li>
            <el-icon><Clock /></el-icon>
            本场考试限时 <b>{{ info.duration }} 分钟</b>，开始后倒计时立即生效，超时系统将自动交卷；
          </li>
          <li>
            <el-icon><DocumentChecked /></el-icon>
            试卷满分 <b>{{ info.totalScore }} 分</b>，达到 <b>{{ info.passScore }} 分</b> 及以上视为通过；
          </li>
          <li>
            <el-icon><RefreshRight /></el-icon>
            剩余考试次数 <b>{{ info.remainRetry }}</b> 次（共 {{ info.maxRetry }} 次，已用 {{ info.usedRetry }} 次），
            次数用尽后需联系培训管理员重置；
          </li>
          <li>
            <el-icon><CircleCheck /></el-icon>
            交卷后系统自动判分，可在「成绩与解析」中查看逐题对错与答案解析；
          </li>
          <li>
            <el-icon><WarningFilled /></el-icon>
            答题过程中请勿刷新或关闭页面，<b>切屏、断电等异常情况可能导致答案丢失</b>；
          </li>
          <li>
            <el-icon><Lock /></el-icon>
            考试期间请遵守企业培训纪律，独立完成作答，严禁代考、抄袭。
          </li>
        </ul>

        <el-alert
          v-if="!info.canExam"
          :title="info.reason || '当前不满足考试条件'"
          type="warning"
          :closable="false"
          show-icon
          class="reason-alert"
        />

        <div class="notice-footer">
          <el-checkbox v-model="agreed" :disabled="!info.canExam">
            我已阅读并同意上述考试须知，承诺独立完成考试
          </el-checkbox>
          <el-button
            type="primary"
            size="large"
            :loading="starting"
            :disabled="!agreed || !info.canExam"
            @click="handleStart"
          >
            开始考试
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExamInfo, startExam } from '@/api/exam'

const route = useRoute()
const router = useRouter()

/** 加载状态 */
const loading = ref(false)
/** 开始考试请求状态 */
const starting = ref(false)
/** 考试信息 */
const info = ref(null)
/** 是否勾选承诺 */
const agreed = ref(false)

/**
 * 加载考试信息
 */
async function loadInfo() {
  loading.value = true
  try {
    const res = await getExamInfo(route.params.taskId)
    info.value = res.data || null
  } finally {
    loading.value = false
  }
}

/**
 * 返回上一页（左上角返回箭头）
 * <p>
 * 优先回退一层历史记录，避免与任务详情页反复 push 造成历史堆积、返回键可无限点击；
 * 仅在无历史可退（如直接刷新进入本页）时才用 replace 兜底跳回任务详情。
 */
function goBack() {
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.replace(`/task/${route.params.taskId}`)
  }
}

/**
 * 开始考试：创建考试记录后跳转答题页
 */
async function handleStart() {
  starting.value = true
  try {
    const res = await startExam(route.params.taskId)
    const recordId = res.data?.recordId
    if (!recordId) {
      ElMessage.error('创建考试记录失败，请稍后重试')
      return
    }
    // 记录 ID 写入 sessionStorage，防止用户误刷新进入重复开考
    sessionStorage.setItem(`exam_record_${route.params.taskId}`, String(recordId))
    router.push(`/exam/test/${recordId}`)
  } catch (e) {
    // 若已有进行中的考试记录，后端会返回提示，此时刷新信息
    await loadInfo()
  } finally {
    starting.value = false
  }
}

onMounted(loadInfo)
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

/* ---------- 试卷概览 ---------- */
.paper-card {
  padding: 24px;
  margin-bottom: 16px;
  border-top: 3px solid transparent;
  border-image: var(--brand-gradient) 1;
}

.paper-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.paper-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: #fff;
  background: var(--brand-gradient);
  flex-shrink: 0;
}

.paper-title-wrap {
  flex: 1;
  min-width: 0;
}

.paper-title-wrap h2 {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
}

.paper-title-wrap p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.paper-metrics {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px dashed var(--border-color);
}

.metric {
  text-align: center;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--brand-primary);
  line-height: 1.2;
}

.metric-value--pass {
  color: var(--success);
}

.metric-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* ---------- 须知 ---------- */
.notice-card {
  padding: 20px 24px 24px;
}

.panel-head {
  margin-bottom: 14px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.notice-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-regular);
  border-bottom: 1px dashed var(--border-color);
}

.notice-list li:last-child {
  border-bottom: none;
}

.notice-list .el-icon {
  margin-top: 3px;
  color: var(--brand-primary);
  flex-shrink: 0;
}

.reason-alert {
  margin-top: 14px;
}

.notice-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.notice-footer .el-button {
  min-width: 160px;
}

@media (max-width: 1100px) {
  .paper-metrics {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
