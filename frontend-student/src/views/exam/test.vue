<template>
  <div v-loading="loading" class="exam-test">
    <!-- 顶部固定栏：试卷信息 + 倒计时 + 交卷 -->
    <div class="exam-topbar gmp-card">
      <div class="topbar-left">
        <el-icon :size="18" color="#1677FF"><EditPen /></el-icon>
        <span class="paper-name">{{ paperName }}</span>
        <el-tag type="info" effect="plain" size="small">
          第 {{ currentIndex + 1 }} / {{ questions.length }} 题
        </el-tag>
      </div>

      <div class="topbar-right">
        <div class="countdown" :class="{ 'countdown--danger': remainSeconds <= 60 }">
          <el-icon><Timer /></el-icon>
          <span class="countdown-text">{{ countdownText }}</span>
        </div>
        <span class="answered-tip">已答 {{ answeredCount }} / {{ questions.length }}</span>
        <el-button type="primary" :loading="submitting" @click="handleSubmit(false)">
          交 卷
        </el-button>
      </div>
    </div>

    <div class="exam-body">
      <!-- 答题卡导航 -->
      <div class="gmp-card card-panel">
        <div class="panel-head">
          <span class="panel-title">答题卡</span>
        </div>
        <div class="card-grid">
          <div
            v-for="(item, index) in questions"
            :key="item.id"
            class="card-cell"
            :class="{
              'card-cell--active': index === currentIndex,
              'card-cell--answered': isAnswered(item)
            }"
            @click="currentIndex = index"
          >
            {{ index + 1 }}
          </div>
        </div>
        <div class="card-legend">
          <span><i class="dot dot--answered"></i>已作答</span>
          <span><i class="dot dot--empty"></i>未作答</span>
        </div>

        <!-- 试卷信息 -->
        <div class="paper-info">
          <div class="info-row">
            <span>总分</span><b>{{ totalScore }} 分</b>
          </div>
          <div class="info-row">
            <span>及格分</span><b>{{ passScore }} 分</b>
          </div>
          <div class="info-row">
            <span>考试时长</span><b>{{ duration }} 分钟</b>
          </div>
        </div>
      </div>

      <!-- 题目作答区 -->
      <div class="gmp-card question-panel">
        <template v-if="currentQuestion">
          <div class="question-head">
            <el-tag :type="questionTypeTag(currentQuestion.questionType)" effect="light" size="small">
              {{ currentQuestion.typeName }}
            </el-tag>
            <span class="question-score">{{ currentQuestion.score }} 分</span>
          </div>

          <div class="question-content">
            <span class="question-index">{{ currentIndex + 1 }}.</span>
            {{ currentQuestion.content }}
          </div>

          <!-- 单选 / 判断（judge 使用固定两项） -->
          <div v-if="currentQuestion.questionType === 1 || currentQuestion.questionType === 3" class="option-list">
            <div
              v-for="(opt, oi) in displayOptions(currentQuestion)"
              :key="oi"
              class="option-item"
              :class="{ 'option-item--selected': answers[currentQuestion.id] === opt.value }"
              @click="selectSingle(currentQuestion.id, opt.value)"
            >
              <span class="option-label">{{ opt.label }}</span>
              <span class="option-text">{{ opt.text }}</span>
            </div>
          </div>

          <!-- 多选 -->
          <div v-else-if="currentQuestion.questionType === 2" class="option-list">
            <div
              v-for="(opt, oi) in displayOptions(currentQuestion)"
              :key="oi"
              class="option-item"
              :class="{ 'option-item--selected': isMultiSelected(currentQuestion.id, opt.value) }"
              @click="toggleMulti(currentQuestion.id, opt.value)"
            >
              <span class="option-label">{{ opt.label }}</span>
              <span class="option-text">{{ opt.text }}</span>
            </div>
            <p class="multi-tip">本题为多选题，请选择所有正确选项</p>
          </div>

          <!-- 填空 -->
          <div v-else-if="currentQuestion.questionType === 4" class="text-answer">
            <el-input
              v-model="answers[currentQuestion.id]"
              placeholder="请输入答案"
              size="large"
              clearable
            />
          </div>

          <!-- 简答 -->
          <div v-else class="text-answer">
            <el-input
              v-model="answers[currentQuestion.id]"
              type="textarea"
              :rows="6"
              placeholder="请输入你的作答内容"
              maxlength="1000"
              show-word-limit
            />
          </div>

          <!-- 底部操作 -->
          <div class="question-footer">
            <el-button :disabled="currentIndex === 0" @click="prevQuestion">
              <el-icon><ArrowLeft /></el-icon> 上一题
            </el-button>

            <div class="footer-right">
              <el-button v-if="currentIndex < questions.length - 1" type="primary" @click="nextQuestion">
                下一题 <el-icon><ArrowRight /></el-icon>
              </el-button>
              <el-button v-else type="success" :loading="submitting" @click="handleSubmit(false)">
                交卷并判分
              </el-button>
            </div>
          </div>
        </template>

        <el-empty v-else description="该试卷暂无题目" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExamRecord, submitExam } from '@/api/exam'

const route = useRoute()
const router = useRouter()

/** 选项字母表 */
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'

/** 加载状态 */
const loading = ref(false)
/** 交卷中状态 */
const submitting = ref(false)
/** 试卷名称 */
const paperName = ref('')
/** 试卷总分 / 及格分 / 时长 */
const totalScore = ref(0)
const passScore = ref(0)
const duration = ref(0)
/** 题目列表 */
const questions = ref([])
/** 当前题目下标 */
const currentIndex = ref(0)
/** 剩余答题秒数 */
const remainSeconds = ref(0)
/** 倒计时定时器 */
let timer = null
/** 是否已交卷（用于阻止离开确认） */
let submitted = false

/**
 * 作答内容：{ [questionId]: string | string[] }
 * 单选/判断/填空/简答为字符串，多选为字母数组
 */
const answers = reactive({})

/** 当前题目 */
const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

/** 已作答题目数 */
const answeredCount = computed(() => questions.value.filter((q) => isAnswered(q)).length)

/** 倒计时文案（HH:mm:ss / mm:ss） */
const countdownText = computed(() => {
  const total = Math.max(0, remainSeconds.value)
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n) => String(n).padStart(2, '0')
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`
})

/** 题型标签色 */
function questionTypeTag(type) {
  const map = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
  return map[type] || 'info'
}

/**
 * 判断某题是否已作答
 * @param {object} question 题目对象
 * @returns {boolean} 是否已作答
 */
function isAnswered(question) {
  const value = answers[question.id]
  if (Array.isArray(value)) return value.length > 0
  return value !== undefined && value !== null && String(value).trim() !== ''
}

/**
 * 构造题目的可选项
 * - 单选/多选：使用后端下发的选项，字母按顺序 A/B/C...
 * - 判断：固定「正确 / 错误」两项
 * @param {object} question 题目对象
 * @returns {Array<{label:string,value:string,text:string}>} 选项列表
 */
function displayOptions(question) {
  if (question.questionType === 3) {
    return [
      { label: '√', value: '正确', text: '正确' },
      { label: '×', value: '错误', text: '错误' }
    ]
  }
  return (question.options || []).map((opt, index) => ({
    label: LETTERS[index],
    value: LETTERS[index],
    text: opt.text
  }))
}

/**
 * 单选题选择
 */
function selectSingle(questionId, value) {
  answers[questionId] = value
}

/**
 * 多选题勾选/取消
 */
function toggleMulti(questionId, value) {
  const list = Array.isArray(answers[questionId]) ? [...answers[questionId]] : []
  const index = list.indexOf(value)
  if (index >= 0) {
    list.splice(index, 1)
  } else {
    list.push(value)
  }
  answers[questionId] = list
}

/**
 * 多选题某选项是否已选
 */
function isMultiSelected(questionId, value) {
  return Array.isArray(answers[questionId]) && answers[questionId].includes(value)
}

/** 上一题 */
function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
  }
}

/** 下一题 */
function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value += 1
  }
}

/**
 * 组装提交数据
 * @returns {Array<{questionId:number,answer:string}>} 作答列表
 */
function buildAnswers() {
  return questions.value.map((q) => {
    const value = answers[q.id]
    let answer = ''
    if (Array.isArray(value)) {
      // 多选：字母升序拼接，与后端判分口径一致
      answer = [...value].sort().join('')
    } else if (value !== undefined && value !== null) {
      answer = String(value)
    }
    return { questionId: q.id, answer }
  })
}

/**
 * 交卷
 * @param {boolean} auto 是否为超时自动交卷
 */
async function handleSubmit(auto) {
  if (submitting.value) return

  if (!auto) {
    const unanswered = questions.value.length - answeredCount.value
    const tip = unanswered > 0
      ? `还有 ${unanswered} 题未作答，确认交卷吗？`
      : '确认提交试卷吗？提交后不可修改。'
    try {
      await ElMessageBox.confirm(tip, '交卷确认', {
        confirmButtonText: '确认交卷',
        cancelButtonText: '继续答题',
        type: 'warning'
      })
    } catch (e) {
      return
    }
  }

  submitting.value = true
  stopTimer()
  try {
    const res = await submitExam(route.params.recordId, { answers: buildAnswers() })
    submitted = true
    ElMessage.success(auto ? '考试时间已到，系统已自动交卷' : '交卷成功，正在生成成绩单')
    router.replace(`/exam/result/${route.params.recordId}`)
    return res
  } catch (e) {
    // 交卷失败时恢复倒计时，允许重新提交
    if (!auto) {
      startTimer()
    }
  } finally {
    submitting.value = false
  }
}

/**
 * 加载考试数据（进入答题 / 刷新恢复）
 */
async function loadExam() {
  loading.value = true
  try {
    const res = await getExamRecord(route.params.recordId)
    const data = res.data || {}
    paperName.value = data.paperName || '在线考试'
    totalScore.value = data.totalScore || 0
    passScore.value = data.passScore || 0
    duration.value = data.duration || 0
    questions.value = data.questions || []

    // 恢复已作答内容（刷新页面场景）
    ;(data.savedAnswers || []).forEach((item) => {
      const question = questions.value.find((q) => q.id === item.questionId)
      if (!question) return
      if (question.questionType === 2) {
        answers[item.questionId] = String(item.answer || '').split('').filter((c) => LETTERS.includes(c))
      } else {
        answers[item.questionId] = item.answer
      }
    })

    // 倒计时以服务端剩余秒数为准
    remainSeconds.value = Number(data.remainSeconds) || (data.duration || 0) * 60
    startTimer()
  } finally {
    loading.value = false
  }
}

/** 启动倒计时 */
function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    remainSeconds.value -= 1
    if (remainSeconds.value <= 0) {
      remainSeconds.value = 0
      stopTimer()
      // 时间到自动交卷
      handleSubmit(true)
    }
  }, 1000)
}

/** 停止倒计时 */
function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// 离开答题页前的二次确认（已交卷或跳转成绩页则放行）
onBeforeRouteLeave(async (to) => {
  if (submitted || to.path.startsWith('/exam/result')) return true
  try {
    await ElMessageBox.confirm(
      '离开后本次答题内容将保留在考试记录中，可再次进入继续作答，确认离开吗？',
      '离开确认',
      { confirmButtonText: '确认离开', cancelButtonText: '继续答题', type: 'warning' }
    )
    stopTimer()
    return true
  } catch (e) {
    return false
  }
})

onMounted(loadExam)
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.exam-test {
  padding-bottom: 16px;
}

/* ---------- 顶部栏 ---------- */
.exam-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  margin-bottom: 16px;
  position: sticky;
  top: 0;
  z-index: 5;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.paper-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-shrink: 0;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  font-weight: 700;
}

.countdown-text {
  font-size: 17px;
  letter-spacing: 1px;
  font-variant-numeric: tabular-nums;
}

/* 剩余 1 分钟内标红提醒 */
.countdown--danger {
  background: #fff1f0;
  color: var(--danger);
}

.answered-tip {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---------- 主体布局 ---------- */
.exam-body {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: start;
}

.card-panel {
  padding: 18px;
  position: sticky;
  top: 84px;
}

.panel-head {
  margin-bottom: 14px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.card-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-regular);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.card-cell:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
}

.card-cell--answered {
  background: var(--brand-primary-light);
  border-color: #b9d4ff;
  color: var(--brand-primary);
}

.card-cell--active {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
  color: #fff;
  font-weight: 600;
}

.card-legend {
  display: flex;
  gap: 16px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--text-secondary);
}

.card-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  display: inline-block;
}

.dot--answered {
  background: var(--brand-primary);
}

.dot--empty {
  background: #fff;
  border: 1px solid var(--border-color);
}

.paper-info {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--border-color);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-row b {
  color: var(--text-primary);
}

/* ---------- 题目区 ---------- */
.question-panel {
  padding: 22px 24px 18px;
  min-height: 420px;
  display: flex;
  flex-direction: column;
}

.question-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-score {
  font-size: 13px;
  color: var(--warning);
  font-weight: 600;
}

.question-content {
  margin: 16px 0 20px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
}

.question-index {
  font-weight: 700;
  margin-right: 4px;
}

/* ---------- 选项 ---------- */
.option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.option-item:hover {
  border-color: var(--brand-primary);
  background: #f7faff;
}

.option-item--selected {
  border-color: var(--brand-primary);
  background: var(--brand-primary-light);
}

.option-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #eef3fa;
  color: var(--text-regular);
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.option-item--selected .option-label {
  background: var(--brand-primary);
  color: #fff;
}

.option-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
}

.multi-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.text-answer {
  margin-top: 4px;
}

/* ---------- 底部操作 ---------- */
.question-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 22px;
}

.footer-right {
  display: flex;
  gap: 10px;
}

@media (max-width: 1100px) {
  .exam-body {
    grid-template-columns: 1fr;
  }

  .card-panel {
    position: static;
  }

  .card-grid {
    grid-template-columns: repeat(10, 1fr);
  }
}
</style>
