<template>
  <div class="page-container">
    <!-- 页签：任务列表 / 待我审核（部门负责人与管理员可见审核页签） -->
    <el-card shadow="never" class="tab-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane name="list">
          <template #label>
            <span><el-icon><List /></el-icon> 任务列表</span>
          </template>
        </el-tab-pane>
        <el-tab-pane v-if="canReview" name="review">
          <template #label>
            <span>
              <el-icon><Stamp /></el-icon> 待我审核
              <el-badge v-if="pendingTotal > 0" :value="pendingTotal" class="tab-badge" />
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ==================== 页签一：任务列表 ==================== -->
    <template v-if="activeTab === 'list'">
      <!-- 搜索栏 -->
      <el-card shadow="never" class="search-card">
        <el-form :model="queryParams" inline>
          <el-form-item label="任务名称">
            <el-input v-model="queryParams.name" placeholder="请输入任务名称" clearable />
          </el-form-item>
          <el-form-item label="培训状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="未开始" value="not_started" />
              <el-option label="进行中" value="in_progress" />
              <el-option label="已完成" value="completed" />
              <el-option label="已逾期" value="overdue" />
            </el-select>
          </el-form-item>
          <el-form-item label="审核状态">
            <el-select v-model="queryParams.reviewStatus" placeholder="请选择审核状态" clearable>
              <el-option label="待主管审核" :value="0" />
              <el-option label="待QA复核" :value="1" />
              <el-option label="已复核通过" :value="2" />
              <el-option label="已退回" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行人">
            <el-input v-model="queryParams.assigneeName" placeholder="请输入执行人" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 表格 -->
      <el-card shadow="never">
        <div v-if="isAdmin" class="toolbar">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 分配培训任务
          </el-button>
          <el-button type="success" @click="handleBatchAssign">
            <el-icon><Promotion /></el-icon> 批量分配
          </el-button>
        </div>

        <el-table v-loading="loading" :data="tableData" border stripe>
          <el-table-column prop="name" label="任务名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="planName" label="所属计划" width="160" show-overflow-tooltip />
          <el-table-column prop="courseName" label="培训课程" width="160" show-overflow-tooltip />
          <el-table-column prop="assigneeName" label="执行人" width="100" />
          <el-table-column prop="departmentName" label="部门" width="100" />
          <el-table-column prop="deadline" label="截止日期" width="120" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="taskTagType(row.status)" size="small">{{ taskStatusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <!-- 审核状态列：未进入审核链时展示 -- -->
          <el-table-column label="审核状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.reviewStatusText" :type="reviewTagType(row.reviewStatus)" size="small">
                {{ row.reviewStatusText }}
              </el-tag>
              <span v-else>--</span>
            </template>
          </el-table-column>
          <el-table-column prop="progress" label="进度" width="100" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.progress || 0" :stroke-width="6" style="width: 60px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
              <!-- 免考确认：仅管理员可对未达标任务直接置完成，必须填写理由 -->
              <el-button
                v-if="isAdmin && (row.status === 'not_started' || row.status === 'in_progress')"
                link type="success" @click="handleComplete(row)"
              >免考确认</el-button>
              <!-- 待我审核阶段的任务可直接从列表进入审核 -->
              <el-button
                v-if="canReviewRow(row)"
                link type="danger" @click="handleReview(row)"
              >审核</el-button>
              <el-button link type="warning" @click="handleNotify(row)">催办通知</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          style="margin-top: 16px; justify-content: flex-end"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </el-card>
    </template>

    <!-- ==================== 页签二：待我审核 ==================== -->
    <template v-else>
      <el-card shadow="never">
        <el-alert
          :title="reviewStageTip"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />
        <el-table v-loading="reviewLoading" :data="reviewData" border stripe>
          <el-table-column prop="name" label="任务名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="assigneeName" label="学员" width="100" />
          <el-table-column prop="departmentName" label="部门" width="110" />
          <el-table-column prop="courseName" label="培训课程" width="160" show-overflow-tooltip />
          <el-table-column prop="deadline" label="截止日期" width="120" />
          <el-table-column label="审核阶段" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="reviewTagType(row.reviewStatus)" size="small">{{ row.reviewStatusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="100" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.progress || 0" :stroke-width="6" style="width: 60px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
              <el-button link type="danger" @click="handleReview(row)">审核</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="reviewParams.page"
          v-model:page-size="reviewParams.size"
          :page-sizes="[10, 20, 50]"
          :total="reviewTotal"
          layout="total, sizes, prev, pager, next, jumper"
          style="margin-top: 16px; justify-content: flex-end"
          @size-change="fetchReviewData"
          @current-change="fetchReviewData"
        />
      </el-card>
    </template>

    <!-- 分配任务对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="培训计划">
          <el-select v-model="formData.planId" placeholder="关联培训计划（可选）" clearable style="width: 100%">
            <el-option v-for="p in plans" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="培训课程" prop="courseId">
          <el-select v-model="formData.courseId" placeholder="请选择课程" filterable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行人" prop="assigneeIds">
          <el-select v-model="formData.assigneeIds" multiple filterable placeholder="选择执行人" style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker v-model="formData.deadline" type="date" placeholder="选择截止日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="任务说明">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入任务说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="620px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务名称" :span="2">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="培训课程">{{ detailData.courseName }}</el-descriptions-item>
        <el-descriptions-item label="所属计划">{{ detailData.planName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="执行人">{{ detailData.assigneeName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detailData.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="截止日期">{{ detailData.deadline }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="taskTagType(detailData.status)" size="small">{{ taskStatusMap[detailData.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态" :span="2">
          <el-tag v-if="detailData.reviewStatusText" :type="reviewTagType(detailData.reviewStatus)" size="small">
            {{ detailData.reviewStatusText }}
          </el-tag>
          <span v-else>未进入审核流程</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.leaderReviewRemark" label="主管审核意见" :span="2">
          {{ detailData.leaderReviewRemark }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.qaReviewRemark" label="QA复核意见" :span="2">
          {{ detailData.qaReviewRemark }}
        </el-descriptions-item>
        <el-descriptions-item label="进度" :span="2">
          <el-progress :percentage="detailData.progress || 0" />
        </el-descriptions-item>
        <el-descriptions-item label="任务说明" :span="2">{{ detailData.description || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审核对话框：通过 / 退回 -->
    <el-dialog v-model="reviewVisible" :title="reviewDialogTitle" width="520px" destroy-on-close>
      <el-alert :title="reviewHint" type="warning" :closable="false" show-icon style="margin-bottom: 16px" />
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="90px">
        <el-form-item label="审核结论" prop="pass">
          <el-radio-group v-model="reviewForm.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">退回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="remark">
          <el-input
            v-model="reviewForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="reviewForm.pass ? '可填写审核意见（选填）' : '请填写退回原因（必填，将通知学员）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put } from '@/utils/request'

// ==================== 状态字典 ====================
/** 任务执行状态字典（与后端 statusToString 保持一致） */
const taskStatusMap = { not_started: '未开始', in_progress: '进行中', completed: '已完成', overdue: '已逾期' }
/** 执行状态对应的标签色 */
function taskTagType(s) { return { not_started: 'info', in_progress: 'primary', completed: 'success', overdue: 'danger' }[s] || 'info' }
/** 审核状态对应的标签色：待主管/待QA 为警示色，通过为成功色，退回为危险色 */
function reviewTagType(s) { return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger' }[s] || 'info' }

// ==================== 当前用户与权限 ====================
/** 读取当前登录用户角色列表 */
const userRoles = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return Array.isArray(info.roles) ? info.roles : []
  } catch (e) {
    return []
  }
})
/** 管理员角色（可分配任务、免考确认、催办） */
const isAdmin = computed(() => userRoles.value.some((r) => ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN'].includes(r)))
/** 是否为纯部门负责人（不含任何管理员角色） */
const isPureLeader = computed(() => userRoles.value.includes('DEPT_LEADER') && !isAdmin.value)
/** 是否可见「待我审核」页签 */
const canReview = computed(() => userRoles.value.some((r) => ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN', 'DEPT_LEADER'].includes(r)))

// ==================== 页签控制 ====================
/** 当前页签：纯部门负责人默认进入「待我审核」，其余默认「任务列表」 */
const activeTab = ref(isPureLeader.value ? 'review' : 'list')
/** 待办总数（页签角标） */
const pendingTotal = ref(0)

// ==================== 任务列表 ====================
const queryParams = reactive({ name: '', status: '', reviewStatus: '', assigneeName: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const plans = ref([])
const courses = ref([])
const users = ref([])

// ==================== 待我审核 ====================
const reviewParams = reactive({ page: 1, size: 10 })
const reviewLoading = ref(false)
const reviewData = ref([])
const reviewTotal = ref(0)
/** 审核阶段提示语：纯部门负责人为主管审核，QA负责人为复核 */
const reviewStageTip = computed(() => {
  if (isPureLeader.value) {
    return '以下是本部门学员已完成学习与考试、待您审核的培训记录。审核通过后将进入 QA 复核；退回则学员需重新学习并考试。'
  }
  if (userRoles.value.includes('QA_MANAGER') && !userRoles.value.includes('SUPER_ADMIN')) {
    return '以下是已通过主管审核、待您 QA 复核的培训记录。复核通过后任务归档；退回则学员需重新学习并考试。'
  }
  return '以下是根据您的角色需要您处理的培训审核待办。'
})

// ==================== 分配任务对话框 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({ name: '', planId: '', courseId: '', assigneeIds: [], deadline: '', description: '' })
const formRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  assigneeIds: [{ required: true, message: '请选择执行人', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }]
}

// ==================== 详情对话框 ====================
const detailVisible = ref(false)
const detailData = reactive({})

// ==================== 审核对话框 ====================
const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref(null)
/** 当前正在审核的任务（用于决定调用主管审核还是 QA 复核接口） */
const reviewTarget = ref(null)
const reviewForm = reactive({ pass: true, remark: '' })
/** 审核意见规则：退回时必填 */
const reviewRules = {
  remark: [{
    validator: (rule, value, callback) => {
      if (reviewForm.pass === false && (!value || !value.trim())) {
        callback(new Error('退回时必须填写退回意见'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }]
}
/** 审核弹窗标题：按任务所处阶段动态展示 */
const reviewDialogTitle = computed(() => {
  const rs = reviewTarget.value?.reviewStatus
  if (rs === 0) return '主管审核'
  if (rs === 1) return 'QA复核'
  return '培训审核'
})
/** 审核弹窗顶部提示 */
const reviewHint = computed(() => {
  const rs = reviewTarget.value?.reviewStatus
  if (rs === 0) return '您正在执行一级审核（主管审核）。通过后流转至 QA 复核。'
  if (rs === 1) return '您正在执行二级复核（QA复核）。通过后培训记录归档，流程结束。'
  return '请确认审核结论。'
})

// ==================== 数据加载 ====================
/** 拉取任务列表 */
async function fetchData() {
  loading.value = true
  try {
    const res = await get('/training/tasks', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

/** 拉取下拉选项（计划 / 课程 / 用户） */
async function loadOptions() {
  try {
    const [planRes, courseRes, userRes] = await Promise.all([
      get('/training/plans/list'),
      get('/training/courses/list'),
      get('/system/users/list')
    ])
    plans.value = planRes.data || planRes || []
    courses.value = courseRes.data || courseRes || []
    users.value = userRes.data || userRes || []
  } catch (e) { console.error(e) }
}

/** 拉取「待我审核」列表 */
async function fetchReviewData() {
  if (!canReview.value) return
  reviewLoading.value = true
  try {
    const res = await get('/training/tasks/review/pending', reviewParams)
    reviewData.value = res.data?.records || res.records || []
    reviewTotal.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { reviewLoading.value = false }
}

/** 拉取待办数量（页签角标）：主管阶段 + QA 阶段求和 */
async function fetchReviewStats() {
  if (!canReview.value) return
  try {
    const res = await get('/training/tasks/review/stats')
    const data = res.data || res || {}
    pendingTotal.value = Number(data.leader || 0) + Number(data.qa || 0)
  } catch (e) { console.error(e) }
}

/** 页签切换：切到审核页签时刷新待办数据 */
function handleTabChange(name) {
  if (name === 'review') {
    reviewParams.page = 1
    fetchReviewData()
  } else {
    fetchData()
  }
}

// ==================== 列表操作 ====================
function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() {
  Object.assign(queryParams, { name: '', status: '', reviewStatus: '', assigneeName: '', page: 1 })
  fetchData()
}

function handleAdd() {
  dialogTitle.value = '分配培训任务'
  Object.assign(formData, { name: '', planId: '', courseId: '', assigneeIds: [], deadline: '', description: '' })
  dialogVisible.value = true
}

function handleBatchAssign() {
  dialogTitle.value = '批量分配培训任务'
  Object.assign(formData, { name: '', planId: '', courseId: '', assigneeIds: [], deadline: '', description: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await post('/training/tasks/assign', formData)
    ElMessage.success('任务分配成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

function handleDetail(row) {
  Object.assign(detailData, { ...row })
  detailVisible.value = true
}

/** 免考确认：管理员对未达标任务直接置完成，必须填写确认理由（后端强制校验） */
async function handleComplete(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `免考确认将把《${row.name}》直接置为「已完成」并进入审核流程，请填写确认理由`,
      '免考确认',
      { inputPlaceholder: '请输入免考确认理由（必填）', inputValidator: (v) => (v && v.trim() ? true : '确认理由不能为空') }
    )
    // remark 以查询参数传递给后端（后端签名为 @RequestParam），故拼接到 URL 上
    await put(`/training/tasks/${row.id}/complete?remark=${encodeURIComponent(value)}`)
    ElMessage.success('已确认完成')
    fetchData()
    fetchReviewStats()
  } catch (e) { if (e !== 'cancel') console.error(e) }
}

async function handleNotify(row) {
  try {
    await post(`/training/tasks/${row.id}/notify`)
    ElMessage.success('催办通知已发送')
  } catch (e) { console.error(e) }
}

/** 判断当前用户是否可审核该行任务：主管阶段需 DEPT_LEADER 或管理员，QA 阶段需 QA_MANAGER 或超管 */
function canReviewRow(row) {
  if (row.reviewStatus === 0) {
    return userRoles.value.some((r) => ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN', 'DEPT_LEADER'].includes(r))
  }
  if (row.reviewStatus === 1) {
    return userRoles.value.some((r) => ['QA_MANAGER', 'SUPER_ADMIN'].includes(r))
  }
  return false
}

/** 打开审核弹窗 */
function handleReview(row) {
  reviewTarget.value = row
  reviewForm.pass = true
  reviewForm.remark = ''
  reviewVisible.value = true
}

/** 提交审核结论 */
async function submitReview() {
  const valid = await reviewFormRef.value.validate().catch(() => false)
  if (!valid) return
  const row = reviewTarget.value
  // 依据任务所处审核阶段选择对应接口：0-主管审核，1-QA复核
  const url = row.reviewStatus === 0
    ? `/training/tasks/${row.id}/leader-review`
    : `/training/tasks/${row.id}/qa-review`
  reviewSubmitting.value = true
  try {
    await post(url, { pass: reviewForm.pass, remark: reviewForm.remark })
    ElMessage.success(reviewForm.pass ? '审核已通过' : '已退回学员')
    reviewVisible.value = false
    fetchReviewData()
    fetchReviewStats()
    if (activeTab.value === 'list') fetchData()
  } catch (e) { console.error(e) } finally { reviewSubmitting.value = false }
}

onMounted(() => {
  fetchData()
  loadOptions()
  fetchReviewStats()
  if (activeTab.value === 'review') fetchReviewData()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
.tab-card :deep(.el-card__body) { padding-top: 4px; padding-bottom: 0; }
.tab-badge { margin-left: 6px; }
</style>
