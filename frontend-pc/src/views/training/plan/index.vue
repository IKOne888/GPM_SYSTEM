<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="计划名称">
          <el-input v-model="queryParams.name" placeholder="请输入计划名称" clearable />
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="queryParams.approvalStatus" placeholder="请选择状态" clearable>
            <el-option label="待提交" value="draft" />
            <el-option label="审批中" value="pending" />
            <el-option label="已发布" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-date-picker v-model="queryParams.year" type="year" placeholder="选择年度" format="YYYY" value-format="YYYY" clearable />
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
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增培训计划
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="code" label="计划编号" width="130" />
        <el-table-column prop="name" label="计划名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="year" label="年度" width="80" align="center" />
        <el-table-column prop="departmentName" label="制定部门" width="120" />
        <el-table-column prop="creatorName" label="制定人" width="100" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="approvalStatus" label="审批状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="approvalTagType(row.approvalStatus)" size="small">
              {{ approvalStatusMap[row.approvalStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.approvalStatus === 'draft' || row.approvalStatus === 'rejected'"
              link type="primary" @click="handleSubmitApproval(row)"
            >提交审批</el-button>
            <el-button
              v-if="row.approvalStatus === 'pending'"
              link type="success" @click="handleApprove(row)"
            >审批通过</el-button>
            <el-button
              v-if="row.approvalStatus === 'pending'"
              link type="warning" @click="handleReject(row)"
            >驳回</el-button>
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划编号" prop="code">
              <el-input v-model="formData.code" placeholder="如: PLAN-2026-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年度" prop="year">
              <el-date-picker v-model="formData.year" type="year" placeholder="选择年度" format="YYYY" value-format="YYYY" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入培训计划名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker v-model="formData.startDate" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="formData.endDate" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="培训对象">
          <el-select v-model="formData.targetDepartments" multiple placeholder="选择目标部门" style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入计划描述" />
        </el-form-item>

        <!-- 培训课程列表 -->
        <el-form-item label="培训课程">
          <div style="width: 100%">
            <el-button type="primary" link @click="addCourseRow">
              <el-icon><Plus /></el-icon> 添加课程
            </el-button>
            <el-table :data="formData.courses" border size="small" style="margin-top: 8px">
              <el-table-column label="课程" min-width="200">
                <template #default="{ row }">
                  <el-select v-model="row.courseId" placeholder="选择课程" filterable size="small" style="width: 100%">
                    <el-option v-for="c in allCourses" :key="c.id" :label="c.name" :value="c.id" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="讲师" width="120">
                <template #default="{ row }">
                  <el-input v-model="row.trainer" size="small" placeholder="讲师" />
                </template>
              </el-table-column>
              <el-table-column label="计划日期" width="160">
                <template #default="{ row }">
                  <el-date-picker v-model="row.plannedDate" type="date" size="small" value-format="YYYY-MM-DD" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="formData.courses.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审批意见对话框 -->
    <el-dialog v-model="approvalDialogVisible" :title="approvalTitle" width="440px" destroy-on-close>
      <!-- 通过时提示下发影响：审批通过后系统会按培训对象部门自动生成任务并通知学员 -->
      <el-alert
        v-if="approvalAction === 'approve'"
        type="warning"
        :closable="false"
        show-icon
        title="审批通过后计划将发布，系统会按「培训对象部门」下的在岗员工自动下发培训任务并发送站内信通知，请确认计划的课程与培训对象无误。"
        style="margin-bottom: 16px"
      />
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="approvalComment" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="approvalLoading" @click="confirmApproval">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put } from '@/utils/request'

const approvalStatusMap = { draft: '待提交', pending: '审批中', approved: '已发布', rejected: '已驳回' }
function approvalTagType(s) { return { draft: 'info', pending: 'warning', approved: 'success', rejected: 'danger' }[s] || 'info' }

const queryParams = reactive({ name: '', approvalStatus: '', year: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const departments = ref([])
const allCourses = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null, code: '', name: '', year: '', startDate: '', endDate: '',
  targetDepartments: [], description: '', courses: []
})
const formRules = {
  code: [{ required: true, message: '请输入计划编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  year: [{ required: true, message: '请选择年度', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

// 审批
const approvalDialogVisible = ref(false)
const approvalTitle = ref('')
const approvalComment = ref('')
const approvalLoading = ref(false)
const currentApprovalRow = ref(null)
const approvalAction = ref('') // approve / reject

async function fetchData() {
  loading.value = true
  try {
    const res = await get('/training/plans', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function loadOptions() {
  try {
    const [deptRes, courseRes] = await Promise.all([
      get('/system/departments/list'),
      get('/training/courses/list')
    ])
    departments.value = deptRes.data || deptRes || []
    allCourses.value = courseRes.data || courseRes || []
  } catch (e) { console.error(e) }
}

function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() { Object.assign(queryParams, { name: '', approvalStatus: '', year: '', page: 1 }); fetchData() }

function addCourseRow() {
  formData.courses.push({ courseId: '', trainer: '', plannedDate: '' })
}

function handleAdd() {
  dialogTitle.value = '新增培训计划'
  Object.assign(formData, { id: null, code: '', name: '', year: '', startDate: '', endDate: '', targetDepartments: [], description: '', courses: [] })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑培训计划'
  Object.assign(formData, { ...row, courses: row.courses || [], targetDepartments: row.targetDepartments || [] })
  dialogVisible.value = true
}

function handleDetail(row) {
  dialogTitle.value = '查看培训计划'
  Object.assign(formData, { ...row, courses: row.courses || [], targetDepartments: row.targetDepartments || [] })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (formData.id) {
      await put(`/training/plans/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await post('/training/plans', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

// 提交审批
async function handleSubmitApproval(row) {
  try {
    await ElMessageBox.confirm('确定提交审批？提交后将不可编辑', '提示', { type: 'info' })
    await put(`/training/plans/${row.id}/submit`)
    ElMessage.success('已提交审批')
    fetchData()
  } catch (e) { if (e !== 'cancel') console.error(e) }
}

// 审批通过
function handleApprove(row) {
  currentApprovalRow.value = row
  approvalAction.value = 'approve'
  approvalTitle.value = '审批通过'
  approvalComment.value = ''
  approvalDialogVisible.value = true
}

// 驳回
function handleReject(row) {
  currentApprovalRow.value = row
  approvalAction.value = 'reject'
  approvalTitle.value = '驳回申请'
  approvalComment.value = ''
  approvalDialogVisible.value = true
}

async function confirmApproval() {
  if (approvalAction.value === 'reject' && !approvalComment.value) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  approvalLoading.value = true
  try {
    const url = approvalAction.value === 'approve'
      ? `/training/plans/${currentApprovalRow.value.id}/approve`
      : `/training/plans/${currentApprovalRow.value.id}/reject`
    await post(url, { comment: approvalComment.value })
    ElMessage.success(approvalAction.value === 'approve' ? '审批通过' : '已驳回')
    approvalDialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { approvalLoading.value = false }
}

onMounted(() => { fetchData(); loadOptions() })
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
</style>
