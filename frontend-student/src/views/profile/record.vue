<template>
  <div class="record-page">
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">学习记录</h2>
        <p class="gmp-page-subtitle">查看视频学习明细与历次考试成绩单</p>
      </div>
    </div>

    <div class="gmp-card tab-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 学习记录 -->
        <el-tab-pane label="视频学习记录" name="study">
          <div class="tab-toolbar">
            <el-input
              v-model="studyQuery.keyword"
              placeholder="搜索视频或课程名称"
              clearable
              class="search-input"
              @keyup.enter="searchStudy"
              @clear="searchStudy"
            >
              <template #append>
                <el-button :icon="Search" @click="searchStudy" />
              </template>
            </el-input>
          </div>

          <el-table v-loading="studyLoading" :data="studyRecords" stripe>
            <el-table-column type="index" label="#" width="56" align="center" />
            <el-table-column prop="taskName" label="培训任务" min-width="160" show-overflow-tooltip />
            <el-table-column prop="courseName" label="所属课程" min-width="150" show-overflow-tooltip />
            <el-table-column prop="videoName" label="视频课件" min-width="170" show-overflow-tooltip />
            <el-table-column label="学习进度" width="170">
              <template #default="{ row }">
                <el-progress
                  :percentage="toPercent(row.videoProgress)"
                  :stroke-width="10"
                  :color="progressColor"
                />
              </template>
            </el-table-column>
            <el-table-column label="学习时长" width="110" align="center">
              <template #default="{ row }">{{ formatDuration(row.studyDuration) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="96" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isCompleted === 1 ? 'success' : 'info'" size="small" effect="light">
                  {{ row.isCompleted === 1 ? '已完成' : '学习中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最后学习时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.lastStudyTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="goLearn(row)">继续学习</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无学习记录" :image-size="80" />
            </template>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="studyQuery.page"
              v-model:page-size="studyQuery.size"
              :total="studyTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="loadStudyRecords"
              @size-change="searchStudy"
            />
          </div>
        </el-tab-pane>

        <!-- 成绩单 -->
        <el-tab-pane label="考试成绩单" name="exam">
          <div class="tab-toolbar">
            <el-input
              v-model="examQuery.keyword"
              placeholder="搜索试卷或任务名称"
              clearable
              class="search-input"
              @keyup.enter="searchExam"
              @clear="searchExam"
            >
              <template #append>
                <el-button :icon="Search" @click="searchExam" />
              </template>
            </el-input>
          </div>

          <el-table v-loading="examLoading" :data="examRecords" stripe>
            <el-table-column type="index" label="#" width="56" align="center" />
            <el-table-column prop="paperName" label="试卷名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="taskName" label="培训任务" min-width="160" show-overflow-tooltip />
            <el-table-column prop="courseName" label="所属课程" min-width="140" show-overflow-tooltip />
            <el-table-column label="得分" width="120" align="center">
              <template #default="{ row }">
                <span class="score-text" :style="{ color: row.pass === 1 ? '#52C41A' : '#FF4D4F' }">
                  {{ row.score }}
                </span>
                <span class="score-total">/ {{ row.passScore }} 及格</span>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="96" align="center">
              <template #default="{ row }">
                <el-tag :type="row.pass === 1 ? 'success' : 'danger'" size="small" effect="light">
                  {{ row.pass === 1 ? '通过' : '未通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="第几次" width="90" align="center">
              <template #default="{ row }">第 {{ row.retryCount }} 次</template>
            </el-table-column>
            <el-table-column label="答卷用时" width="110" align="center">
              <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
            </el-table-column>
            <el-table-column label="交卷时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="goResult(row)">查看解析</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无考试成绩记录" :image-size="80" />
            </template>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="examQuery.page"
              v-model:page-size="examQuery.size"
              :total="examTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="loadExamRecords"
              @size-change="searchExam"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getStudyRecords, getExamRecords } from '@/api/profile'
import { formatDateTime, formatDuration, toPercent } from '@/utils/format'

const router = useRouter()

/** 当前激活的标签页 */
const activeTab = ref('study')

// ---------- 学习记录 ----------
/** 学习记录加载状态 */
const studyLoading = ref(false)
/** 学习记录数据 */
const studyRecords = ref([])
/** 学习记录总数 */
const studyTotal = ref(0)
/** 学习记录查询条件 */
const studyQuery = reactive({ page: 1, size: 10, keyword: '' })

// ---------- 成绩单 ----------
/** 成绩单加载状态 */
const examLoading = ref(false)
/** 成绩单数据 */
const examRecords = ref([])
/** 成绩单总数 */
const examTotal = ref(0)
/** 成绩单查询条件 */
const examQuery = reactive({ page: 1, size: 10, keyword: '' })

/** 进度条配色 */
function progressColor(percentage) {
  if (percentage >= 100) return '#52C41A'
  if (percentage >= 60) return '#1677FF'
  return '#FAAD14'
}

/**
 * 加载学习记录
 */
async function loadStudyRecords() {
  studyLoading.value = true
  try {
    const res = await getStudyRecords({
      page: studyQuery.page,
      size: studyQuery.size,
      keyword: studyQuery.keyword || undefined
    })
    studyRecords.value = res.data?.records || []
    studyTotal.value = Number(res.data?.total) || 0
  } finally {
    studyLoading.value = false
  }
}

/**
 * 学习记录搜索（回到第一页）
 */
function searchStudy() {
  studyQuery.page = 1
  loadStudyRecords()
}

/**
 * 加载成绩单
 */
async function loadExamRecords() {
  examLoading.value = true
  try {
    const res = await getExamRecords({
      page: examQuery.page,
      size: examQuery.size,
      keyword: examQuery.keyword || undefined
    })
    examRecords.value = res.data?.records || []
    examTotal.value = Number(res.data?.total) || 0
  } finally {
    examLoading.value = false
  }
}

/**
 * 成绩单搜索（回到第一页）
 */
function searchExam() {
  examQuery.page = 1
  loadExamRecords()
}

/**
 * 标签页切换：首次进入某标签时懒加载数据
 * @param {string} name 标签名
 */
function handleTabChange(name) {
  if (name === 'exam' && examRecords.value.length === 0) {
    loadExamRecords()
  }
}

/**
 * 继续学习：跳转学习页（带任务与视频定位）
 * @param {object} row 学习记录行
 */
function goLearn(row) {
  router.push(`/learn/${row.taskId}/${row.videoId}`)
}

/**
 * 查看成绩解析
 * @param {object} row 成绩单行
 */
function goResult(row) {
  router.push(`/exam/result/${row.recordId}`)
}

onMounted(loadStudyRecords)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}

.tab-card {
  padding: 8px 20px 20px;
}

.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.search-input {
  width: 280px;
}

.score-text {
  font-size: 17px;
  font-weight: 700;
}

.score-total {
  margin-left: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
