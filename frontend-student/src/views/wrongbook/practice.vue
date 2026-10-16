<template>
  <div v-loading="loading" class="practice-page">
    <!-- 页头 -->
    <div class="page-head">
      <el-button :icon="ArrowLeft" @click="goBack">返回错题本</el-button>
      <span v-if="queue.length" class="progress-tip">
        第 {{ currentIndex + 1 }} / {{ queue.length }} 题
      </span>
    </div>

    <template v-if="currentQuestion">
      <!-- 答题卡 -->
      <div class="practice-card gmp-card">
        <div class="question-head">
          <el-tag :type="typeTag(currentQuestion.questionType)" effect="light" size="small">
            {{ currentQuestion.typeName || getQuestionTypeName(currentQuestion.questionType) }}
          </el-tag>
          <el-tag v-if="currentQuestion.courseName" type="info" effect="plain" size="small">
            {{ currentQuestion.courseName }}
          </el-tag>
          <el-tag type="danger" effect="plain" size="small">
            答错 {{ currentQuestion.wrongCount }} 次
          </el-tag>
        </div>

        <div class="question-content">{{ currentQuestion.content }}</div>

        <!-- 单选 / 判断 -->
        <div
          v-if="currentQuestion.questionType === 1 || currentQuestion.questionType === 3"
          class="option-list"
        >
          <div
            v-for="(opt, index) in displayOptions(currentQuestion)"
            :key="index"
            class="option-item"
            :class="optionClass(index, opt.value)"
            @click="selectSingle(opt.value)"
          >
            <span class="option-label">{{ opt.label }}</span>
            <span class="option-text">{{ opt.text }}</span>
          </div>
        </div>

        <!-- 多选 -->
        <div v-else-if="currentQuestion.questionType === 2" class="option-list">
          <div
            v-for="(opt, index) in displayOptions(currentQuestion)"
            :key="index"
            class="option-item"
            :class="optionClass(index, opt.value)"
            @click="toggleMulti(opt.value)"
          >
            <span class="option-label">{{ opt.label }}</span>
            <span class="option-text">{{ opt.text }}</span>
          </div>
          <p class="multi-tip">本题为多选题，请选择所有正确选项</p>
        </div>

        <!-- 填空 / 简答 -->
        <div v-else class="text-answer">
          <el-input
            v-model="answerText"
            :type="currentQuestion.questionType === 5 ? 'textarea' : 'text'"
            :rows="5"
            :disabled="!!result"
            placeholder="请输入你的作答内容"
            maxlength="1000"
            show-word-limit
          />
        </div>

        <!-- 判断题提示 -->
        <p v-if="currentQuestion.questionType === 3 && !result" class="judge-tip">
          请选择「正确」或「错误」
        </p>
      </div>

      <!-- 判分结果 -->
      <div v-if="result" class="result-card gmp-card" :class="result.correct ? 'result-card--right' : 'result-card--wrong'">
        <div class="result-head">
          <el-icon :size="20">
            <CircleCheckFilled v-if="result.correct" />
            <CircleCloseFilled v-else />
          </el-icon>
          <span class="result-title">
            {{ result.correct ? '回答正确' : '回答错误' }}
          </span>
          <el-tag v-if="result.mastered" type="success" effect="light" size="small">
            已掌握，自动移出错题本
          </el-tag>
        </div>

        <div class="result-body">
          <p class="result-row">
            <span class="row-label">正确答案：</span>
            <b class="text-success">{{ result.correctAnswer || '-' }}</b>
          </p>
          <p class="result-row">
            <span class="row-label">答案解析：</span>
            <span class="text-analysis">{{ result.analysis || '暂无解析' }}</span>
          </p>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="footer-bar">
        <el-button :disabled="currentIndex === 0" @click="prevQuestion">上一题</el-button>

        <div class="footer-right">
          <template v-if="!result">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交答案</el-button>
          </template>
          <template v-else>
            <!-- 答错后可原地重做当前题，无需退出练习页 -->
            <el-button v-if="!result.correct" @click="resetAnswer">再试一次</el-button>
            <el-button v-if="currentIndex < queue.length - 1" type="primary" @click="nextQuestion">
              下一题
            </el-button>
            <el-button v-else type="success" @click="goBack">完成练习</el-button>
          </template>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="暂无未掌握错题，继续保持！" />
  </div>
</template>

<script setup>
/**
 * 错题练习页面
 * <p>
 * 单题答题卡模式：提交后通过 POST /study/wrong-questions/practice 即时判分，
 * 答对则该题自动标记为已掌握并移出未掌握列表。
 * 支持 `?questionId=` 指定从某题开始练习；不传则按错题本顺序依次练习。
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getWrongQuestions, practiceWrongQuestion } from '@/api/wrongbook'
import { getQuestionTypeName } from '@/utils/status'

const route = useRoute()
const router = useRouter()

/** 选项字母表 */
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'

/** 加载状态 */
const loading = ref(false)
/** 提交中状态 */
const submitting = ref(false)
/** 练习队列（未掌握错题） */
const queue = ref([])
/** 当前题目下标 */
const currentIndex = ref(0)
/** 单选/答案文本 */
const answerText = ref('')
/** 多选已选字母 */
const multiSelected = ref([])
/** 判分结果（null 表示未提交） */
const result = ref(null)

/** 当前题目 */
const currentQuestion = computed(() => queue.value[currentIndex.value] || null)

/**
 * 加载练习队列
 * <p>
 * 默认只取未掌握错题（最多 100 条）。
 * 若通过「重做本题」指定了 questionId，而该题已被标记为已掌握（不在未掌握队列中），
 * 则补查一次已掌握错题并把该题插入队首，保证确实能重做点击的那一题。
 */
async function loadQueue() {
  loading.value = true
  try {
    const res = await getWrongQuestions({ page: 1, size: 100, mastered: false })
    queue.value = res.data?.records || []

    const targetId = Number(route.query.questionId)
    if (targetId) {
      let index = queue.value.findIndex((item) => item.questionId === targetId)
      if (index < 0) {
        // 已掌握的错题不会出现在未掌握队列里，需单独查询后补充到队首
        const masteredRes = await getWrongQuestions({ page: 1, size: 100, mastered: true })
        const target = (masteredRes.data?.records || [])
          .find((item) => item.questionId === targetId)
        if (target) {
          queue.value = [target, ...queue.value]
          index = 0
        }
      }
      if (index >= 0) {
        currentIndex.value = index
      }
    }
    resetAnswer()
  } finally {
    loading.value = false
  }
}

/** 重置作答状态 */
function resetAnswer() {
  answerText.value = ''
  multiSelected.value = []
  result.value = null
}

/** 题型标签色 */
function typeTag(type) {
  const map = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
  return map[type] || 'info'
}

/**
 * 构造可选项（单选/多选用后端选项；判断固定正确/错误）
 * @param {object} question 题目对象
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
 * 选项样式：提交后正确项标绿、本人错误选择标红
 * @param {number} index 选项下标
 * @param {string} value 选项值
 */
function optionClass(index, value) {
  if (!result.value) {
    // 未提交：显示选中态
    if (currentQuestion.value.questionType === 2) {
      return { 'option-item--selected': multiSelected.value.includes(value) }
    }
    return { 'option-item--selected': answerText.value === value }
  }
  // 已提交：正确项标绿，本人选错项标红
  const correct = currentQuestion.value.correctAnswer || ''
  const correctLetters = correct.split('').filter((c) => LETTERS.includes(c))
  const isCorrect = currentQuestion.value.questionType === 2
    ? correctLetters.includes(value)
    : correct.toUpperCase() === value.toUpperCase()
  const myLetters = myAnswerLetters()
  const isMyChoice = currentQuestion.value.questionType === 2
    ? myLetters.includes(value)
    : answerText.value === value
  return {
    'option-item--right': isCorrect,
    'option-item--wrong': !isCorrect && isMyChoice
  }
}

/** 本人多选答案字母数组 */
function myAnswerLetters() {
  return multiSelected.value
}

/** 单选题选择 */
function selectSingle(value) {
  if (result.value) return
  answerText.value = value
}

/** 多选题勾选 */
function toggleMulti(value) {
  if (result.value) return
  const list = [...multiSelected.value]
  const index = list.indexOf(value)
  if (index >= 0) {
    list.splice(index, 1)
  } else {
    list.push(value)
  }
  multiSelected.value = list
}

/** 上一题 */
function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
    resetAnswer()
  }
}

/** 下一题 */
function nextQuestion() {
  if (currentIndex.value < queue.value.length - 1) {
    currentIndex.value += 1
    resetAnswer()
  }
}

/**
 * 组装当前题作答内容（与后端判分口径一致）
 * @returns {string} 答案字符串
 */
function buildAnswer() {
  const question = currentQuestion.value
  if (question.questionType === 2) {
    return [...multiSelected.value].sort().join('')
  }
  return answerText.value || ''
}

/**
 * 提交答案并即时判分
 */
async function handleSubmit() {
  const answer = buildAnswer()
  if (!answer) {
    ElMessage.warning('请先作答再提交')
    return
  }
  submitting.value = true
  try {
    const res = await practiceWrongQuestion({
      questionId: currentQuestion.value.questionId,
      answer
    })
    result.value = res.data || {}
    if (result.value.correct) {
      ElMessage.success('回答正确！')
    } else {
      ElMessage.warning('回答错误，请查看解析')
    }
  } finally {
    submitting.value = false
  }
}

/**
 * 返回错题本
 * <p>
 * 优先回退历史记录，避免反复 push 同一路由造成浏览器历史堆积（返回键需点很多次）。
 * history.state.back 由 Vue Router 维护，存在时说明有可回退的上一页。
 */
function goBack() {
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.replace('/wrong-book')
  }
}

onMounted(loadQueue)
</script>

<style scoped>
.practice-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.progress-tip {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---------- 答题卡 ---------- */
.practice-card {
  padding: 24px 26px;
}

.question-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.question-content {
  margin: 16px 0 20px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
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
  padding: 13px 16px;
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

/* 判分后：正确项绿色、错误项红色 */
.option-item--right {
  border-color: #b7eb8f;
  background: #f6ffed;
  cursor: default;
}

.option-item--wrong {
  border-color: #ffccc7;
  background: #fff1f0;
  cursor: default;
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

.option-item--right .option-label {
  background: var(--success);
  color: #fff;
}

.option-item--wrong .option-label {
  background: var(--danger);
  color: #fff;
}

.option-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
}

.multi-tip,
.judge-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

/* ---------- 判分结果 ---------- */
.result-card {
  margin-top: 16px;
  padding: 18px 22px;
  border-left: 4px solid var(--info);
}

.result-card--right {
  border-left-color: var(--success);
}

.result-card--wrong {
  border-left-color: var(--danger);
}

.result-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.result-card--right .result-head {
  color: var(--success);
}

.result-card--wrong .result-head {
  color: var(--danger);
}

.result-title {
  font-size: 16px;
  font-weight: 600;
}

.result-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-row {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-regular);
}

.row-label {
  color: var(--text-secondary);
}

.text-success {
  color: var(--success);
}

.text-analysis {
  white-space: pre-wrap;
}

/* ---------- 底部操作 ---------- */
.footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.footer-right {
  display: flex;
  gap: 10px;
}
</style>
