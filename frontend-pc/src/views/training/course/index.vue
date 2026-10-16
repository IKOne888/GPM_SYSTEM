<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="课程名称">
          <el-input v-model="queryParams.name" placeholder="请输入课程名称" clearable />
        </el-form-item>
        <el-form-item label="课程分类">
          <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width: 180px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
          <el-icon><Plus /></el-icon> 新增课程
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="code" label="课程编码" width="130" />
        <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="课程分类" width="120" />
        <!-- 所属部门：由系统管理员在"部门管理"中维护，课程仅引用 -->
        <el-table-column prop="departmentName" label="所属部门" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.departmentName || '-' }}</template>
        </el-table-column>
        <!-- 适用岗位：由系统管理员在"岗位管理"中维护，课程仅引用 -->
        <el-table-column prop="positionName" label="适用岗位" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.positionName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sopCode" label="SOP编号" width="120" show-overflow-tooltip />
        <el-table-column prop="sopVersion" label="SOP版本" width="90" align="center" />
        <el-table-column prop="trainingHours" label="学时" width="80" align="center" />
        <el-table-column prop="passScore" label="及格分" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleFiles(row)">课程资料</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该课程？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入课程名称" :maxlength="200" />
        </el-form-item>
        <el-form-item label="课程编码" prop="code">
          <el-input v-model="formData.code" placeholder="如: GMP-001" :maxlength="50" />
        </el-form-item>
        <el-form-item label="课程分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <!-- 所属部门：数据来源于系统管理员维护的部门，可为空（表示不限部门） -->
        <el-form-item label="所属部门">
          <el-select v-model="formData.departmentId" placeholder="请选择所属部门（可不选）" clearable filterable style="width: 100%" @change="handleDepartmentChange">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <!-- 适用岗位：数据来源于系统管理员维护的岗位，可为空（表示全岗位通用） -->
        <el-form-item label="适用岗位">
          <el-select v-model="formData.positionId" placeholder="请选择适用岗位（可不选）" clearable filterable style="width: 100%">
            <el-option v-for="p in filteredPositions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="SOP编号">
          <el-input v-model="formData.sopCode" placeholder="如: SOP-QA-001" :maxlength="50" />
        </el-form-item>
        <el-form-item label="SOP版本">
          <el-input v-model="formData.sopVersion" placeholder="如: V1.0" :maxlength="20" />
        </el-form-item>
        <el-form-item label="培训学时" prop="trainingHours">
          <el-input-number v-model="formData.trainingHours" :min="0.1" :max="999.99" :step="0.5" :precision="2" />
        </el-form-item>
        <el-form-item label="及格分数" prop="passScore">
          <el-input-number v-model="formData.passScore" :min="0" :max="100" :step="1" />
        </el-form-item>
        <el-form-item label="复训周期(天)" prop="retrainPeriod">
          <el-input-number v-model="formData.retrainPeriod" :min="1" :max="3650" :step="30" />
        </el-form-item>
        <el-form-item label="培训目标">
          <el-input v-model="formData.trainingObjective" type="textarea" :rows="3" placeholder="请输入培训目标" :maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 课程资料管理对话框（上传/预览/删除，仅管理员可操作） -->
    <el-dialog v-model="fileDialogVisible" :title="fileDialogTitle" width="760px" destroy-on-close>
      <div class="toolbar">
        <el-upload
          :show-file-list="false"
          :http-request="handleUpload"
          accept=".pdf,.doc,.docx,.xls,.xlsx"
        >
          <el-button type="primary" :loading="uploading">
            <el-icon><Upload /></el-icon> 上传资料
          </el-button>
        </el-upload>
        <span class="upload-tip">支持 PDF、Word(doc/docx)、Excel(xls/xlsx)，单个文件不超过 50MB</span>
      </div>
      <el-table v-loading="fileLoading" :data="fileList" border stripe>
        <el-table-column prop="name" label="资料名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="90" align="center" />
        <el-table-column label="大小" width="110" align="center">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
            <el-popconfirm title="确定删除该资料？" @confirm="handleDeleteFile(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="fileDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post, put, del, upload } from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

// ===== 列表查询相关 =====
const queryParams = reactive({ name: '', categoryId: null, status: null, pageNum: 1, pageSize: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
// 下拉数据源：课程分类、部门、岗位
const categories = ref([])
const departments = ref([])
const positions = ref([])

// ===== 新增/编辑对话框相关 =====
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null, name: '', code: '', categoryId: null, departmentId: null, positionId: null,
  sopCode: '', sopVersion: '', trainingObjective: '', trainingHours: 1,
  passScore: 60, retrainPeriod: 365, status: 1
})
const formRules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入课程编码', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择课程分类', trigger: 'change' }],
  trainingHours: [{ required: true, message: '请输入培训学时', trigger: 'blur' }]
}

// 适用岗位：若已选部门，仅展示该部门下的岗位；否则展示全部岗位
const filteredPositions = computed(() => {
  if (!formData.departmentId) return positions.value
  return positions.value.filter(p => p.departmentId === formData.departmentId)
})

// ===== 课程资料对话框相关 =====
const fileDialogVisible = ref(false)
const fileDialogTitle = ref('课程资料')
const fileLoading = ref(false)
const uploading = ref(false)
const fileList = ref([])
// 当前操作的课程ID
const currentCourseId = ref(null)

// 加载课程分类（下拉数据源）
async function loadCategories() {
  try {
    const res = await get('/training/course-categories')
    categories.value = res.data || []
  } catch (e) {
    console.error('加载课程分类失败:', e)
  }
}

// 加载部门下拉（部门由系统管理员维护，此处只读引用）
async function loadDepartments() {
  try {
    const res = await get('/system/departments/list')
    departments.value = res.data || []
  } catch (e) {
    console.error('加载部门失败:', e)
  }
}

// 加载岗位下拉（岗位由系统管理员维护，此处只读引用）
async function loadPositions() {
  try {
    const res = await get('/system/positions/list')
    positions.value = res.data || []
  } catch (e) {
    console.error('加载岗位失败:', e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await get('/training/courses', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

function handleSearch() { queryParams.pageNum = 1; fetchData() }
function handleReset() { Object.assign(queryParams, { name: '', categoryId: null, status: null, pageNum: 1 }); fetchData() }

function handleAdd() {
  dialogTitle.value = '新增课程'
  Object.assign(formData, {
    id: null, name: '', code: '', categoryId: null, departmentId: null, positionId: null,
    sopCode: '', sopVersion: '', trainingObjective: '', trainingHours: 1,
    passScore: 60, retrainPeriod: 365, status: 1
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑课程'
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

// 切换部门时，若当前已选岗位不属于新部门，则清空岗位，避免数据不一致
function handleDepartmentChange() {
  if (!formData.positionId) return
  const stillValid = positions.value.some(p => p.id === formData.positionId && p.departmentId === formData.departmentId)
  if (!stillValid) formData.positionId = null
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (formData.id) {
      await put(`/training/courses/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await post('/training/courses', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

async function handleDelete(row) {
  try {
    await del(`/training/courses/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { console.error(e) }
}

// ===== 课程资料 =====
// 打开某课程的资料管理对话框并加载资料列表
async function handleFiles(row) {
  currentCourseId.value = row.id
  fileDialogTitle.value = `课程资料 - ${row.name}`
  fileList.value = []
  fileDialogVisible.value = true
  await loadFiles()
}

async function loadFiles() {
  fileLoading.value = true
  try {
    const res = await get(`/training/courses/${currentCourseId.value}/files`)
    fileList.value = res.data || []
  } catch (e) { console.error(e) } finally { fileLoading.value = false }
}

// 自定义上传：使用共享 axios 实例（自动携带 token），提交 multipart/form-data
async function handleUpload(option) {
  const rawFile = option.file
  const formData = new FormData()
  formData.append('file', rawFile)
  uploading.value = true
  try {
    await upload(`/training/courses/${currentCourseId.value}/files`, formData)
    ElMessage.success('上传成功')
    await loadFiles()
  } catch (e) {
    console.error('上传失败:', e)
  } finally {
    uploading.value = false
  }
}

// 获取预览地址并在新窗口打开
async function handlePreview(row) {
  try {
    const res = await get(`/training/courses/files/${row.id}/url`)
    if (res.data) {
      window.open(res.data, '_blank')
    } else {
      ElMessage.warning('未获取到预览地址')
    }
  } catch (e) { console.error(e) }
}

async function handleDeleteFile(row) {
  try {
    await del(`/training/courses/files/${row.id}`)
    ElMessage.success('删除成功')
    await loadFiles()
  } catch (e) { console.error(e) }
}

// 文件大小格式化：字节 -> B/KB/MB
function formatSize(size) {
  if (size == null) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

onMounted(() => { loadCategories(); loadDepartments(); loadPositions(); fetchData() })
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; align-items: center; }
.upload-tip { color: #909399; font-size: 12px; }
</style>
