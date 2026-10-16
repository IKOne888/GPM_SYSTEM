<template>
  <div class="archive-page">
    <!-- 顶部员工选择 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <span class="filter-label">选择员工：</span>
        <el-select
          v-model="selectedUserId"
          placeholder="请选择要查看培训档案的员工"
          filterable
          style="width: 320px"
          @change="handleUserChange"
        >
          <el-option
            v-for="user in userList"
            :key="user.id"
            :label="user.realName + '（' + user.username + '）'"
            :value="user.id"
          />
        </el-select>
        <el-button type="primary" :loading="loading" @click="refreshAll" :disabled="!selectedUserId">
          刷新
        </el-button>
      </div>
    </el-card>

    <!-- 员工未选择时的引导 -->
    <el-empty v-if="!selectedUserId" description="请先选择一名员工以查看其培训档案" class="empty-hint" />

    <!-- 已选择：概览卡 + 三个 Tab -->
    <template v-else>
      <!-- 概览卡 -->
      <el-card shadow="never" class="overview-card" v-loading="loading">
        <div class="overview-header">
          <div class="avatar">{{ overview.realName?.charAt(0) || 'U' }}</div>
          <div class="user-basic">
            <div class="name">{{ overview.realName || '-' }}</div>
            <div class="meta">
              <span>账号：{{ overview.username }}</span>
              <el-divider direction="vertical" />
              <span>部门：{{ overview.departmentName || '-' }}</span>
              <el-divider direction="vertical" />
              <span>岗位：{{ overview.positionName || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ overview.totalStudyHours ?? 0 }}</div>
            <div class="stat-label">累计学习时长（小时）</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ overview.studyRecordCount ?? 0 }}</div>
            <div class="stat-label">学习记录条数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ overview.examCount ?? 0 }}</div>
            <div class="stat-label">参加考试次数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ passedRate }}%</div>
            <div class="stat-label">考试通过率</div>
          </div>
          <div class="stat-item stat-valid">
            <div class="stat-value">{{ overview.validCertCount ?? 0 }}</div>
            <div class="stat-label">有效证书</div>
          </div>
          <div class="stat-item stat-expiring">
            <div class="stat-value">{{ overview.expiringCertCount ?? 0 }}</div>
            <div class="stat-label">临期证书（≤30天）</div>
          </div>
          <div class="stat-item stat-expired">
            <div class="stat-value">{{ overview.expiredCertCount ?? 0 }}</div>
            <div class="stat-label">过期证书</div>
          </div>
        </div>
      </el-card>

      <!-- 三个 Tab -->
      <el-card shadow="never" class="tab-card">
        <el-tabs v-model="activeTab">
          <!-- Tab 1: 学习记录 -->
          <el-tab-pane label="学习记录" name="study">
            <div class="tab-filter">
              <el-input
                v-model="studyKeyword"
                placeholder="按视频名称搜索"
                clearable
                style="width: 240px"
                @keyup.enter="loadStudyRecords"
              >
                <template #append>
                  <el-button :icon="Search" @click="loadStudyRecords" />
                </template>
              </el-input>
            </div>
            <el-table :data="studyList" v-loading="studyLoading" border stripe size="default">
              <el-table-column prop="taskName" label="培训任务" min-width="180" show-overflow-tooltip />
              <el-table-column prop="courseName" label="课程" min-width="140" show-overflow-tooltip />
              <el-table-column prop="videoName" label="视频课件" min-width="160" show-overflow-tooltip />
              <el-table-column label="学习时长" width="120">
                <template #default="{ row }">{{ formatDuration(row.studyDuration) }}</template>
              </el-table-column>
              <el-table-column label="进度" width="140">
                <template #default="{ row }">
                  <el-progress
                    :percentage="Number(row.videoProgress || 0)"
                    :status="row.isCompleted === 1 ? 'success' : ''"
                  />
                </template>
              </el-table-column>
              <el-table-column label="完成状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.isCompleted === 1 ? 'success' : 'info'" size="small">
                    {{ row.isCompleted === 1 ? '已完成' : '未完成' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="最后学习时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.lastStudyTime) }}</template>
              </el-table-column>
            </el-table>
            <div v-if="studyTotal > 0" class="pagination-wrap">
              <el-pagination
                v-model:current-page="studyQuery.page"
                v-model:page-size="studyQuery.size"
                :total="studyTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                background
                @current-change="loadStudyRecords"
                @size-change="loadStudyRecords"
              />
            </div>
          </el-tab-pane>

          <!-- Tab 2: 考试记录 -->
          <el-tab-pane label="考试记录" name="exam">
            <div class="tab-filter">
              <el-input
                v-model="examKeyword"
                placeholder="按试卷/任务名称搜索"
                clearable
                style="width: 240px"
                @keyup.enter="loadExamRecords"
              >
                <template #append>
                  <el-button :icon="Search" @click="loadExamRecords" />
                </template>
              </el-input>
            </div>
            <el-table :data="examList" v-loading="examLoading" border stripe>
              <el-table-column prop="taskName" label="培训任务" min-width="180" show-overflow-tooltip />
              <el-table-column prop="courseName" label="课程" min-width="140" show-overflow-tooltip />
              <el-table-column prop="paperName" label="试卷" min-width="160" show-overflow-tooltip />
              <el-table-column label="得分" width="120">
                <template #default="{ row }">
                  {{ row.score }} / {{ row.passScore }}
                </template>
              </el-table-column>
              <el-table-column label="结果" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.pass === 1 ? 'success' : 'danger'" size="small">
                    {{ row.pass === 1 ? '通过' : '未通过' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="次数" width="70">
                <template #default="{ row }">第 {{ row.retryCount ?? 1 }} 次</template>
              </el-table-column>
              <el-table-column label="提交时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
              </el-table-column>
            </el-table>
            <div v-if="examTotal > 0" class="pagination-wrap">
              <el-pagination
                v-model:current-page="examQuery.page"
                v-model:page-size="examQuery.size"
                :total="examTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                background
                @current-change="loadExamRecords"
                @size-change="loadExamRecords"
              />
            </div>
          </el-tab-pane>

          <!-- Tab 3: 培训证书 -->
          <el-tab-pane label="培训档案 / 证书" name="cert">
            <el-table :data="certList" v-loading="certLoading" border stripe>
              <el-table-column prop="courseName" label="课程" min-width="180" show-overflow-tooltip />
              <el-table-column label="SOP" width="180">
                <template #default="{ row }">
                  <span v-if="row.sopCode">{{ row.sopCode }}</span>
                  <span v-if="row.sopVersion" class="sop-version"> / v{{ row.sopVersion }}</span>
                </template>
              </el-table-column>
              <el-table-column label="学时" width="90">
                <template #default="{ row }">{{ row.trainingHours ? row.trainingHours + ' h' : '-' }}</template>
              </el-table-column>
              <el-table-column label="最近通过" width="170">
                <template #default="{ row }">{{ formatDateTime(row.lastPassTime) }}</template>
              </el-table-column>
              <el-table-column label="通过成绩" width="100">
                <template #default="{ row }">{{ row.examScore ?? '-' }} 分</template>
              </el-table-column>
              <el-table-column label="再培训周期" width="110">
                <template #default="{ row }">每 {{ row.retrainPeriod ?? 365 }} 天</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="certStatusType(row.status)" size="small">
                    {{ certStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="certList.length === 0 && !certLoading" description="暂无培训证书" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </div>
</template>

<script setup>
/**
 * 员工培训档案（管理员视角）
 * <p>
 * 调后端 /system/archive/{userId}/** 四个接口：
 * - overview         概览统计卡
 * - study-records    学习记录分页（复用 StudyService.pageStudyRecords(userId, ...)）
 * - exam-records     考试记录分页
 * - certificates     培训证书列表
 * 员工下拉走现成的 /system/user/list 接口。
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { get } from '@/utils/request'
import { formatDateTime } from '@/utils/format'

/**
 * 格式化学习时长（秒 → 小时/分钟/秒 组合文本）
 */
function formatDuration(seconds) {
  const s = Number(seconds) || 0
  if (s < 60) return s + ' 秒'
  if (s < 3600) return Math.floor(s / 60) + ' 分 ' + (s % 60) + ' 秒'
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  return h + ' 小时 ' + m + ' 分'
}

// ==================== 响应式数据 ====================

/** 员工下拉列表 */
const userList = ref([])
/** 当前选中的员工 ID */
const selectedUserId = ref(null)
/** 概览数据 */
const overview = ref({})
/** 全局加载状态（概览） */
const loading = ref(false)

/** 当前激活的 Tab：study / exam / cert */
const activeTab = ref('study')

/** 学习记录 */
const studyList = ref([])
const studyTotal = ref(0)
const studyLoading = ref(false)
const studyQuery = reactive({ page: 1, size: 10 })
const studyKeyword = ref('')

/** 考试记录 */
const examList = ref([])
const examTotal = ref(0)
const examLoading = ref(false)
const examQuery = reactive({ page: 1, size: 10 })
const examKeyword = ref('')

/** 证书列表 */
const certList = ref([])
const certLoading = ref(false)

// ==================== 计算属性 ====================

/** 考试通过率（百分比，保留一位小数） */
const passedRate = computed(() => {
  const total = overview.value.examCount || 0
  const passed = overview.value.passedExamCount || 0
  if (total === 0) return 0
  return ((passed / total) * 100).toFixed(1)
})

// ==================== 证书状态辅助 ====================

const CERT_STATUS_TYPE = {
  valid: 'success',
  expiring: 'warning',
  expired: 'danger',
  not_pass: 'info'
}
const CERT_STATUS_TEXT = {
  valid: '有效',
  expiring: '临期',
  expired: '已过期',
  not_pass: '未通过'
}
function certStatusType(s) { return CERT_STATUS_TYPE[s] || 'info' }
function certStatusText(s) { return CERT_STATUS_TEXT[s] || s }

// ==================== 数据加载 ====================

/** 页面挂载：拉员工列表 */
onMounted(loadUserList)

/** Tab 切换时按需加载对应数据 */
watch(activeTab, (tab) => {
  if (!selectedUserId.value) return
  if (tab === 'study' && studyList.value.length === 0) loadStudyRecords()
  if (tab === 'exam' && examList.value.length === 0) loadExamRecords()
  if (tab === 'cert' && certList.value.length === 0) loadCertificates()
})

/** 员工下拉列表 */
async function loadUserList() {
  try {
    const res = await get('/system/user/list')
    userList.value = res.data || []
  } catch (e) { /* 静默 */ }
}

/** 切换员工时重载全部 */
function handleUserChange() { refreshAll() }

/** 刷新全部数据 */
function refreshAll() {
  if (!selectedUserId.value) return
  loading.value = true
  // 概览
  get(`/system/archive/${selectedUserId.value}/overview`)
    .then(res => { overview.value = res.data || {} })
    .catch(() => {})
    .finally(() => { loading.value = false })
  // 重置三个 Tab 分页并首次加载
  studyQuery.page = 1; studyKeyword.value = ''; loadStudyRecords()
  examQuery.page = 1; examKeyword.value = ''; loadExamRecords()
  loadCertificates()
}

/** 学习记录分页 */
async function loadStudyRecords() {
  if (!selectedUserId.value) return
  studyLoading.value = true
  try {
    const res = await get(`/system/archive/${selectedUserId.value}/study-records`, {
      page: studyQuery.page, size: studyQuery.size, keyword: studyKeyword.value || undefined
    })
    studyList.value = res.data?.records || []
    studyTotal.value = Number(res.data?.total) || 0
  } finally {
    studyLoading.value = false
  }
}

/** 考试记录分页 */
async function loadExamRecords() {
  if (!selectedUserId.value) return
  examLoading.value = true
  try {
    const res = await get(`/system/archive/${selectedUserId.value}/exam-records`, {
      page: examQuery.page, size: examQuery.size, keyword: examKeyword.value || undefined
    })
    examList.value = res.data?.records || []
    examTotal.value = Number(res.data?.total) || 0
  } finally {
    examLoading.value = false
  }
}

/** 证书列表 */
async function loadCertificates() {
  if (!selectedUserId.value) return
  certLoading.value = true
  try {
    const res = await get(`/system/archive/${selectedUserId.value}/certificates`)
    certList.value = res.data || []
  } finally {
    certLoading.value = false
  }
}
</script>

<style scoped>
/* 顶部筛选卡 */
.filter-card { margin-bottom: 16px; }
.filter-row { display: flex; align-items: center; gap: 12px; }
.filter-label { font-size: 14px; color: #606266; }
.empty-hint { margin: 60px 0; }

/* 概览卡 */
.overview-card { margin-bottom: 16px; }
.overview-header { display: flex; align-items: center; gap: 16px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; margin-bottom: 16px; }
.avatar {
  width: 56px; height: 56px; border-radius: 50%;
  background: #409eff; color: #fff; font-size: 24px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}
.user-basic .name { font-size: 18px; font-weight: 600; color: #303133; }
.user-basic .meta { margin-top: 4px; font-size: 13px; color: #909399; }

/* 统计网格 */
.stats-grid {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 12px;
}
.stat-item {
  text-align: center; padding: 12px 8px; border-radius: 8px;
  background: #f5f7fa;
}
.stat-value { font-size: 22px; font-weight: 600; color: #303133; line-height: 1.2; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }
.stat-valid { background: #f0f9eb; }
.stat-valid .stat-value { color: #67c23a; }
.stat-expiring { background: #fdf6ec; }
.stat-expiring .stat-value { color: #e6a23c; }
.stat-expired { background: #fef0f0; }
.stat-expired .stat-value { color: #f56c6c; }

/* Tab 卡 */
.tab-card .tab-filter { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
.sop-version { color: #909399; }
</style>
