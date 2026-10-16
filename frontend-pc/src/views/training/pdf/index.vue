<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="所属课程">
          <el-select v-model="queryParams.courseId" placeholder="请选择课程" clearable filterable style="width: 220px">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课件名称">
          <el-input v-model="queryParams.keyword" placeholder="请输入课件名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <el-button type="primary" @click="handleOpenUpload">
          <el-icon><Upload /></el-icon> 上传PDF课件
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="name" label="课件名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="courseName" label="所属课程" min-width="160" show-overflow-tooltip />
        <el-table-column label="文件大小" width="110" align="right">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploadUserName" label="上传人" width="100" />
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <!-- 开关直接调用启停接口，成功后刷新列表 -->
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传PDF课件" width="520px" destroy-on-close>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="uploadForm.courseId" placeholder="请选择所属课程" filterable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课件名称" prop="name">
          <el-input v-model="uploadForm.name" placeholder="不填则默认使用文件名" maxlength="200" />
        </el-form-item>
        <el-form-item label="PDF文件" prop="file">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".pdf"
            :on-change="handleFileChange"
            :on-remove="() => (uploadForm.file = null)"
          >
            <el-button>
              <el-icon><FolderOpened /></el-icon> 选择文件
            </el-button>
            <template #tip>
              <div class="upload-tip">仅支持 PDF 格式，单个文件不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, put, del, upload } from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

/** 查询条件（page/size 与后端分页参数命名保持一致） */
const queryParams = reactive({ courseId: '', keyword: '', status: '', page: 1, size: 10 })
/** 表格加载态 */
const loading = ref(false)
/** 表格数据 */
const tableData = ref([])
/** 总条数 */
const total = ref(0)
/** 课程下拉数据 */
const courses = ref([])

/** 上传对话框可见性 */
const uploadDialogVisible = ref(false)
/** 上传加载态 */
const uploading = ref(false)
/** 上传表单引用 */
const uploadFormRef = ref(null)
/** 上传表单数据 */
const uploadForm = reactive({ courseId: null, name: '', file: null })
/** 上传表单校验规则 */
const uploadRules = {
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  file: [{ required: true, message: '请选择PDF文件', trigger: 'change' }]
}

/**
 * 文件大小格式化（字节 → 可读文本）
 * @param {number} size 字节数
 */
function formatSize(size) {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

/**
 * 拉取课件分页数据
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await get('/training/course-pdfs', {
      page: queryParams.page,
      size: queryParams.size,
      courseId: queryParams.courseId || undefined,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status === '' ? undefined : queryParams.status
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
 * 加载课程下拉数据（复用课程列表接口）
 */
async function loadCourses() {
  try {
    const res = await get('/training/courses/list')
    courses.value = res.data || []
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
  queryParams.courseId = ''
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.page = 1
  fetchData()
}

/** 打开上传对话框并重置表单 */
function handleOpenUpload() {
  Object.assign(uploadForm, { courseId: null, name: '', file: null })
  uploadDialogVisible.value = true
}

/**
 * el-upload 选择文件回调（仅保留原始文件对象，实际提交在确定时进行）
 * @param {object} file 选中文件
 */
function handleFileChange(file) {
  uploadForm.file = file.raw
}

/** 提交上传 */
async function handleUpload() {
  try {
    await uploadFormRef.value.validate()
  } catch (e) {
    return
  }
  // 提交 multipart 表单：file + courseId + name
  const formData = new FormData()
  formData.append('file', uploadForm.file)
  formData.append('courseId', uploadForm.courseId)
  if (uploadForm.name) {
    formData.append('name', uploadForm.name)
  }
  uploading.value = true
  try {
    await upload('/training/course-pdfs/upload', formData)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    uploading.value = false
  }
}

/**
 * 预览课件：获取预签名地址后在新窗口打开
 * @param {object} row 目标行
 */
async function handlePreview(row) {
  try {
    const res = await get(`/training/course-pdfs/${row.id}/url`)
    window.open(res.data, '_blank')
  } catch (e) {
    console.error(e)
  }
}

/**
 * 启用 / 停用课件
 * @param {object} row 目标行
 * @param {number} val 目标状态：0-禁用 1-启用
 */
async function handleToggleStatus(row, val) {
  try {
    await put(`/training/course-pdfs/${row.id}/status?status=${val}`)
    ElMessage.success(val === 1 ? '已启用' : '已停用')
    fetchData()
  } catch (e) {
    console.error(e)
    // 失败时刷新列表，让开关回到服务端的真实状态
    fetchData()
  }
}

/** 删除课件（逻辑删除 + 联动删除 MinIO 对象） */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除课件「${row.name}」？删除后文件将不可恢复。`, '提示', { type: 'warning' })
    await del(`/training/course-pdfs/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(() => {
  fetchData()
  loadCourses()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
.upload-tip { color: #909399; font-size: 12px; margin-top: 4px; }
</style>