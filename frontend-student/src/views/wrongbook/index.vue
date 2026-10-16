<template>
  <div class="wrongbook-page">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">错题本</h2>
        <p class="gmp-page-subtitle">考试答错的题目会自动收集到这里，重做答对即自动移出</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
        <el-button type="primary" :icon="EditPen" @click="startPractice()">开始练习</el-button>
      </div>
    </div>

    <!-- 统计卡 -->
    <div class="stat-grid">
      <div class="stat-card gmp-card">
        <div class="stat-icon" style="background: #e8f1ff; color: #1677ff">
          <el-icon :size="22"><Collection /></el-icon>
        </div>
        <div>
          <div class="stat-value" style="color: #1677ff">{{ stats.total }}</div>
          <div class="stat-label">错题总数</div>
        </div>
      </div>
      <div class="stat-card gmp-card">
        <div class="stat-icon" style="background: #fff1f0; color: #ff4d4f">
          <el-icon :size="22"><WarningFilled /></el-icon>
        </div>
        <div>
          <div class="stat-value" style="color: #ff4d4f">{{ stats.notMastered }}</div>
          <div class="stat-label">未掌握</div>
        </div>
      </div>
      <div class="stat-card gmp-card">
        <div class="stat-icon" style="background: #f0fae8; color: #52c41a">
          <el-icon :size="22"><CircleCheck /></el-icon>
        </div>
        <div>
          <div class="stat-value" style="color: #52c41a">{{ stats.mastered }}</div>
          <div class="stat-label">已掌握</div>
        </div>
      </div>
      <div class="stat-card gmp-card">
        <div class="stat-icon" style="background: #fff7e6; color: #faad14">
          <el-icon :size="22"><PieChart /></el-icon>
        </div>
        <div>
          <div class="stat-value" style="color: #faad14">{{ topTypeText }}</div>
          <div class="stat-label">易错题型</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar gmp-card">
      <el-select
        v-model="query.courseId"
        placeholder="全部课程"
        clearable
        class="filter-select"
        @change="handleSearch"
      >
        <el-option
          v-for="item in courseOptions"
          :key="item.courseId"
          :label="`${item.courseName}（${item.count}）`"
          :value="item.courseId"
        />
      </el-select>

      <el-select
        v-model="query.questionType"
        placeholder="全部题型"
        clearable
        class="filter-select"
        @change="handleSearch"
      >
        <el-option
          v-for="(item, code) in QUESTION_TYPE"
          :key="code"
          :label="item.label"
          :value="Number(code)"
        />
      </el-select>

      <el-radio-group v-model="query.mastered" @change="handleSearch">
        <el-radio-button :value="false">未掌握</el-radio-button>
        <el-radio-button :value="true">已掌握</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 错题列表 -->
    <div v-loading="loading" class="wrong-list">
      <el-empty
        v-if="!loading && list.length === 0"
        :description="query.mastered ? '暂无已掌握的错题' : '暂无未掌握错题，继续保持！'"
      />

      <div
        v-for="item in list"
        :key="item.id"
        class="wrong-card gmp-card"
      >
        <!-- 题头 -->
        <div class="wrong-head">
          <el-tag :type="typeTag(item.questionType)" effect="light" size="small">
            {{ item.typeName || getQuestionTypeName(item.questionType) }}
          </el-tag>
          <el-tag v-if="item.courseName" type="info" effect="plain" size="small">
            {{ item.courseName }}
          </el-tag>
          <el-tag type="danger" effect="plain" size="small">答错 {{ item.wrongCount }} 次</el-tag>
          <el-tag v-if="item.mastered === 1" type="success" effect="light" size="small">已掌握</el-tag>
          <span class="wrong-time">最近答错：{{ formatDateTime(item.lastWrongTime) }}</span>
        </div>

        <!-- 题干 -->
        <div class="wrong-content">{{ item.content }}</div>

        <!-- 选项 -->
        <div v-if="item.options && item.options.length" class="option-list">
          <div
            v-for="(opt, index) in item.options"
            :key="index"
            class="option-item"
            :class="{ 'option-item--answer': opt.isAnswer }"
          >
            <span class="option-label">{{ LETTERS[index] }}</span>
            <span class="option-text">{{ opt.text }}</span>
          </div>
        </div>

        <!-- 我的答案 / 正确答案 -->
        <div class="answer-row">
          <span class="answer-item answer-item--wrong">
            我的答案：<b>{{ item.myAnswer || '未作答' }}</b>
          </span>
          <span class="answer-item answer-item--right">
            正确答案：<b>{{ item.correctAnswer || '-' }}</b>
          </span>
        </div>

        <!-- 展开解析 -->
        <el-collapse class="analysis-collapse">
          <el-collapse-item title="查看解析">
            <p class="analysis-text">{{ item.analysis || '暂无解析' }}</p>
          </el-collapse-item>
        </el-collapse>

        <!-- 操作 -->
        <div class="wrong-footer">
          <el-button link type="primary" @click="startPractice(item.questionId)">重做本题</el-button>
          <el-button
            v-if="item.mastered !== 1"
            link
            type="success"
            @click="handleRemove(item)"
          >
            标记已掌握
          </el-button>
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
          @current-change="loadList"
          @size-change="handleSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 错题本页面
 * <p>
 * 数据来源：GET /study/wrong-questions（分页）与 GET /study/wrong-questions/stats（统计）。
 * 支持按课程/题型/掌握状态筛选；「重做本题/开始练习」进入错题练习页。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, EditPen } from '@element-plus/icons-vue'
import { getWrongQuestions, getWrongQuestionStats, removeWrongQuestion } from '@/api/wrongbook'
import { QUESTION_TYPE, getQuestionTypeName } from '@/utils/status'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

/** 选项字母表 */
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'

/** 加载状态 */
const loading = ref(false)
/** 错题列表 */
const list = ref([])
/** 总条数 */
const total = ref(0)
/** 统计数据 */
const stats = ref({ total: 0, notMastered: 0, mastered: 0, byType: [], byCourse: [] })

/** 查询条件（mastered 默认 false：仅看未掌握） */
const query = reactive({
  page: 1,
  size: 5,
  courseId: null,
  questionType: null,
  mastered: false
})

/** 课程筛选下拉选项（取自统计的按课程分布） */
const courseOptions = computed(() => stats.value.byCourse || [])

/** 易错题型文案（取错题数量最多的题型） */
const topTypeText = computed(() => {
  const byType = stats.value.byType || []
  if (byType.length === 0) return '暂无'
  const top = [...byType].sort((a, b) => Number(b.count) - Number(a.count))[0]
  return `${top.typeName || getQuestionTypeName(top.questionType)} ${top.count} 题`
})

/** 题型标签色 */
function typeTag(type) {
  const map = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
  return map[type] || 'info'
}

/**
 * 加载错题列表
 */
async function loadList() {
  loading.value = true
  try {
    const res = await getWrongQuestions({
      page: query.page,
      size: query.size,
      courseId: query.courseId || undefined,
      questionType: query.questionType || undefined,
      mastered: query.mastered
    })
    list.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

/**
 * 加载统计数据
 */
async function loadStats() {
  const res = await getWrongQuestionStats()
  stats.value = res.data || {
    total: 0,
    notMastered: 0,
    mastered: 0,
    byType: [],
    byCourse: []
  }
}

/**
 * 条件变更后重新查询（回到第一页）
 */
function handleSearch() {
  query.page = 1
  loadList()
}

/** 刷新统计与列表 */
async function reloadAll() {
  await Promise.all([loadStats(), loadList()])
}

/**
 * 进入错题练习
 * @param {number} [questionId] 指定题目ID（不传则按当前筛选顺序练习全部）
 */
function startPractice(questionId) {
  const param = questionId ? `?questionId=${questionId}` : ''
  router.push(`/wrong-book/practice${param}`)
}

/**
 * 手动移除错题（标记已掌握）
 * @param {object} item 错题对象
 */
async function handleRemove(item) {
  try {
    await ElMessageBox.confirm('确认将该题标记为已掌握并移出未掌握列表？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await removeWrongQuestion(item.id)
  ElMessage.success('已移出错题本')
  await reloadAll()
}

onMounted(reloadAll)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.head-actions {
  display: flex;
  gap: 10px;
}

/* ---------- 统计卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
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
  font-size: 24px;
  font-weight: 700;
  line-height: 1.15;
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
  gap: 14px;
  padding: 14px 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-select {
  width: 220px;
}

/* ---------- 错题卡片 ---------- */
.wrong-list {
  min-height: 240px;
}

.wrong-card {
  padding: 18px 20px;
  margin-bottom: 14px;
}

.wrong-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.wrong-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: auto;
}

.wrong-content {
  margin: 14px 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-primary);
}

/* ---------- 选项 ---------- */
.option-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-regular);
}

/* 正确选项高亮 */
.option-item--answer {
  border-color: #b7eb8f;
  background: #f6ffed;
  color: #389e0d;
}

.option-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #eef3fa;
  color: var(--text-regular);
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.option-item--answer .option-label {
  background: #52c41a;
  color: #fff;
}

/* ---------- 答案对比 ---------- */
.answer-row {
  display: flex;
  gap: 28px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background: #f7faff;
  font-size: 13px;
  color: var(--text-regular);
  flex-wrap: wrap;
}

.answer-item--wrong b {
  color: var(--danger);
}

.answer-item--right b {
  color: var(--success);
}

/* ---------- 解析 ---------- */
.analysis-collapse {
  margin-top: 10px;
  border-top: none;
}

.analysis-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  color: var(--brand-primary);
  height: 40px;
}

.analysis-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.analysis-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.wrong-footer {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 6px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

@media (max-width: 1180px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
