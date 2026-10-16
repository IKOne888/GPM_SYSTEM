<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="需求标题">
          <el-input v-model="queryParams.keyword" placeholder="请输入需求标题" clearable />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="已采纳" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请部门">
          <el-select v-model="queryParams.departmentId" placeholder="请选择部门" clearable filterable>
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
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
          <el-icon><Plus /></el-icon> 登记培训需求
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="title" label="需求标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="trainingTopic" label="培训主题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="trainingTarget" label="培训对象" min-width="140" show-overflow-tooltip />
        <el-table-column prop="departmentName" label="申请部门" width="120" />
        <el-table-column prop="statusText" label="状态" width="90" align="center">
          <template #default="{ row }">
            <!-- 状态标签：待处理=警告色，已采纳=成功色，已拒绝=危险色 -->
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登记时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <!-- 仅待处理状态允许编辑/处理/删除，已处理需求保留台账 -->
            <template v-if="row.status === 0">
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="success" @click="handleOpenHandle(row, 1)">采纳</el-button>
              <el-button link type="warning" @click="handleOpenHandle(row, 2)">拒绝</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
            <el-button v-else link type="primary" @click="handleDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增 / 编辑 对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入需求标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="培训主题" prop="trainingTopic">
          <el-input v-model="formData.trainingTopic" placeholder="请输入培训主题" maxlength="200" />
        </el-form-item>
        <el-form-item label="申请部门" prop="departmentId">
          <el-select v-model="formData.departmentId" placeholder="请选择申请部门" clearable filterable style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="培训对象" prop="trainingTarget">
          <el-input v-model="formData.trainingTarget" placeholder="如：生产车间全体员工" maxlength="500" />
        </el-form-item>
        <el-form-item label="需求内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="3" placeholder="请描述培训需求的具体内容" />
        </el-form-item>
        <el-form-item label="需求原因" prop="reason">
          <el-input v-model="formData.reason" type="textarea" :rows="2" placeholder="请说明提出该需求的原因" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 处理（采纳 / 拒绝）对话框 -->
    <el-dialog v-model="handleDialogVisible" :title="handleTitle" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item :label="handleAction === 1 ? '采纳备注' : '拒绝原因'">
          <el-input
            v-model="handleOpinion"
            type="textarea"
            :rows="3"
            :placeholder="handleAction === 1 ? '可填写采纳后的安排说明（选填）' : '请填写拒绝原因（必填）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="confirmHandle">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="培训需求详情" width="560px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="需求标题">{{ detailRow.title }}</el-descriptions-item>
        <el-descriptions-item label="培训主题">{{ detailRow.trainingTopic || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请部门">{{ detailRow.departmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="培训对象">{{ detailRow.trainingTarget || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需求内容">{{ detailRow.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需求原因/处理意见">{{ detailRow.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ detailRow.statusText }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ formatDateTime(detailRow.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put, del } from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

/**
 * 状态标签配色映射
 * @param {number} s 状态值：0-待处理 1-已采纳 2-已拒绝
 */
function statusTagType(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info'
}

/** 查询条件（page/size 与后端分页参数命名保持一致） */
const queryParams = reactive({ keyword: '', status: '', departmentId: '', page: 1, size: 10 })
/** 表格加载态 */
const loading = ref(false)
/** 表格数据 */
const tableData = ref([])
/** 总条数 */
const total = ref(0)
/** 部门下拉数据（用于申请部门筛选与表单选择） */
const departments = ref([])

/** 新增/编辑对话框可见性 */
const dialogVisible = ref(false)
/** 对话框标题 */
const dialogTitle = ref('')
/** 提交按钮加载态 */
const submitLoading = ref(false)
/** 表单引用（用于触发校验） */
const formRef = ref(null)
/** 表单数据 */
const formData = reactive({
  id: null, title: '', trainingTopic: '', departmentId: null, trainingTarget: '', content: '', reason: ''
})
/** 表单校验规则 */
const formRules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }]
}

/** 处理对话框可见性 */
const handleDialogVisible = ref(false)
/** 处理对话框标题 */
const handleTitle = ref('')
/** 处理意见文本 */
const handleOpinion = ref('')
/** 处理按钮加载态 */
const handleLoading = ref(false)
/** 当前处理的行 */
const currentRow = ref(null)
/** 处理动作：1-采纳 2-拒绝 */
const handleAction = ref(1)

/** 详情对话框可见性 */
const detailVisible = ref(false)
/** 详情数据 */
const detailRow = ref({})

/**
 * 拉取需求分页数据
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await get('/training/demands', {
      page: queryParams.page,
      size: queryParams.size,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status === '' ? undefined : queryParams.status,
      departmentId: queryParams.departmentId || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

/**
 * 加载部门下拉数据（复用系统管理接口，所有角色均可读）
 */
async function loadDepartments() {
  try {
    const res = await get('/system/departments/list')
    departments.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

/** 搜索：重置到第一页后查询 */
function handleSearch() {
  queryParams.page = 1
  fetchData()
}

/** 重置查询条件 */
function handleReset() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.departmentId = ''
  queryParams.page = 1
  fetchData()
}

/** 新增：清空表单 */
function handleAdd() {
  dialogTitle.value = '登记培训需求'
  Object.assign(formData, {
    id: null, title: '', trainingTopic: '', departmentId: null, trainingTarget: '', content: '', reason: ''
  })
  dialogVisible.value = true
}

/** 编辑：回填表单 */
function handleEdit(row) {
  dialogTitle.value = '编辑培训需求'
  Object.assign(formData, {
    id: row.id,
    title: row.title,
    trainingTopic: row.trainingTopic,
    departmentId: row.departmentId,
    trainingTarget: row.trainingTarget,
    content: row.content,
    reason: row.reason
  })
  dialogVisible.value = true
}

/** 提交新增/编辑 */
async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitLoading.value = true
  try {
    if (formData.id) {
      await put(`/training/demands/${formData.id}`, formData)
      ElMessage.success('修改成功')
    } else {
      await post('/training/demands', formData)
      ElMessage.success('登记成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

/**
 * 打开处理对话框
 * @param {object} row 目标行
 * @param {number} action 1-采纳 2-拒绝
 */
function handleOpenHandle(row, action) {
  currentRow.value = row
  handleAction.value = action
  handleTitle.value = action === 1 ? '采纳培训需求' : '拒绝培训需求'
  handleOpinion.value = ''
  handleDialogVisible.value = true
}

/** 确认处理（采纳 / 拒绝） */
async function confirmHandle() {
  // 拒绝必须填写原因，与后端校验保持一致
  if (handleAction.value === 2 && !handleOpinion.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  handleLoading.value = true
  try {
    await post(`/training/demands/${currentRow.value.id}/handle`, {
      status: handleAction.value,
      opinion: handleOpinion.value
    })
    ElMessage.success(handleAction.value === 1 ? '已采纳' : '已拒绝')
    handleDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    handleLoading.value = false
  }
}

/** 删除需求（仅待处理状态可删） */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除需求「${row.title}」？`, '提示', { type: 'warning' })
    await del(`/training/demands/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

/** 查看详情 */
function handleDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
  loadDepartments()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
</style>