<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="课件名称">
          <el-input v-model="queryParams.name" placeholder="请输入课件名称" clearable />
        </el-form-item>
        <el-form-item label="所属课程">
          <el-select v-model="queryParams.courseId" placeholder="请选择课程" clearable filterable>
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
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
        <el-button type="primary" @click="openUploadDialog">
          <el-icon><Upload /></el-icon> 上传视频课件
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="name" label="课件名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="courseName" label="所属课程" width="180" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="文件大小" width="110" align="center">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="100" align="center">
          <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
        </el-table-column>
        <el-table-column prop="format" label="格式" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ready' ? 'success' : row.status === 'processing' ? 'warning' : 'info'" size="small">
              {{ { ready: '就绪', processing: '转码中', failed: '失败' }[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
            <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
            <el-popconfirm title="确定删除该课件？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
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

    <!-- 视频上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传视频课件"
      width="650px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="uploadForm.courseId" placeholder="请选择课程" filterable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课件名称" prop="name">
          <el-input v-model="uploadForm.name" placeholder="请输入课件名称" />
        </el-form-item>
        <el-form-item label="课件描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="2" placeholder="请输入课件描述" />
        </el-form-item>
        <el-form-item label="选择视频" prop="file">
          <div class="upload-area">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              accept="video/*"
              drag
            >
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">将视频文件拖到此处，或<em>点击选择</em></div>
              <template #tip>
                <div class="upload-tip">
                  支持 MP4/AVI/MOV/WMV 格式，单个文件不超过 2GB
                </div>
              </template>
            </el-upload>
          </div>
        </el-form-item>

        <!-- 已选文件信息 -->
        <el-form-item v-if="selectedFile" label="已选文件">
          <div class="selected-file">
            <el-icon><VideoCamera /></el-icon>
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">{{ formatSize(selectedFile.size) }}</span>
          </div>
        </el-form-item>

        <!-- 上传进度 -->
        <el-form-item v-if="uploading" label="上传进度">
          <div class="progress-area">
            <el-progress
              :percentage="uploadProgress"
              :status="uploadProgress === 100 ? 'success' : ''"
              :stroke-width="20"
              striped
              striped-flow
            />
            <div class="progress-info">
              <span v-if="uploading">上传中... {{ formatSize(uploadedSize) }} / {{ formatSize(selectedFile?.size || 0) }}</span>
              <span v-else-if="uploadProgress === 100">上传完成</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false" :disabled="uploading">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">
          {{ uploading ? '上传中...' : '开始上传' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 视频预览对话框 -->
    <el-dialog v-model="previewVisible" title="视频预览" width="800px" destroy-on-close>
      <video
        v-if="previewUrl"
        :src="previewUrl"
        controls
        autoplay
        style="width: 100%; max-height: 500px; background: #000"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post, del, upload } from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

const queryParams = reactive({ name: '', courseId: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const courses = ref([])

// 上传相关
const uploadDialogVisible = ref(false)
const uploadFormRef = ref(null)
const uploadRef = ref(null)
const selectedFile = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadedSize = ref(0)
const previewVisible = ref(false)
const previewUrl = ref('')

const uploadForm = reactive({
  courseId: '',
  name: '',
  description: ''
})

const uploadRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  name: [{ required: true, message: '请输入课件名称', trigger: 'blur' }]
}

// 格式化文件大小
function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i > 0 ? 2 : 0) + ' ' + units[i]
}

// 格式化时长
function formatDuration(seconds) {
  if (!seconds) return '--'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

// 获取课件列表
async function fetchData() {
  loading.value = true
  try {
    const res = await get('/training/videos', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

// 加载课程列表
async function loadCourses() {
  try {
    const res = await get('/training/courses/list')
    courses.value = res.data || res || []
  } catch (e) { console.error(e) }
}

function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() { Object.assign(queryParams, { name: '', courseId: '', page: 1 }); fetchData() }

function openUploadDialog() {
  Object.assign(uploadForm, { courseId: '', name: '', description: '' })
  selectedFile.value = null
  uploadProgress.value = 0
  uploadedSize.value = 0
  uploadDialogVisible.value = true
}

// 文件选择
function handleFileChange(file) {
  // 校验文件类型
  const allowedTypes = ['video/mp4', 'video/avi', 'video/x-msvideo', 'video/quicktime', 'video/x-ms-wmv']
  if (!allowedTypes.includes(file.raw.type) && !file.raw.type.startsWith('video/')) {
    ElMessage.error('请选择视频文件')
    return
  }
  // 校验文件大小 (2GB)
  if (file.raw.size > 2 * 1024 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 2GB')
    return
  }
  selectedFile.value = file.raw
  if (!uploadForm.name) {
    uploadForm.name = file.name.replace(/\.[^.]+$/, '')
  }
}

function handleFileRemove() {
  selectedFile.value = null
}

// 大文件上传 - 分片上传
async function handleUpload() {
  const valid = await uploadFormRef.value.validate().catch(() => false)
  if (!valid || !selectedFile.value) return

  uploading.value = true
  uploadProgress.value = 0
  uploadedSize.value = 0

  const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB 分片
  const file = selectedFile.value
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

  try {
    // 如果文件较小（< 50MB），直接上传
    if (file.size < 50 * 1024 * 1024) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('courseId', uploadForm.courseId)
      formData.append('name', uploadForm.name)
      formData.append('description', uploadForm.description || '')

      await upload('/training/videos/upload', formData, (percent) => {
        uploadProgress.value = percent
        uploadedSize.value = Math.round(file.size * percent / 100)
      })
    } else {
      // 大文件分片上传
      // 1. 初始化分片上传
      const initRes = await post('/training/videos/upload/init', {
        fileName: file.name,
        fileSize: file.size,
        totalChunks,
        courseId: uploadForm.courseId,
        name: uploadForm.name,
        description: uploadForm.description || ''
      })

      const uploadId = initRes.data?.uploadId
      const uploadedChunks = initRes.data?.uploadedChunks || []

      // 2. 逐个上传分片
      for (let i = 0; i < totalChunks; i++) {
        // 跳过已上传的分片（断点续传）
        if (uploadedChunks.includes(i)) {
          continue
        }

        const start = i * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, file.size)
        const chunk = file.slice(start, end)

        const chunkForm = new FormData()
        chunkForm.append('file', chunk)
        chunkForm.append('uploadId', uploadId)
        chunkForm.append('chunkIndex', i)
        chunkForm.append('totalChunks', totalChunks)

        await upload('/training/videos/upload/chunk', chunkForm, (percent) => {
          const overallProgress = ((i + percent / 100) / totalChunks) * 100
          uploadProgress.value = Math.round(overallProgress)
          uploadedSize.value = Math.round(file.size * overallProgress / 100)
        })
      }

      // 3. 合并分片
      await post('/training/videos/upload/complete', {
        uploadId,
        fileName: file.name
      })
    }

    uploadProgress.value = 100
    ElMessage.success('视频上传成功')
    uploadDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('上传失败:', e)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

function handlePreview(row) {
  previewUrl.value = `/api/training/videos/${row.id}/stream`
  previewVisible.value = true
}

function handleDownload(row) {
  const link = document.createElement('a')
  link.href = `/api/training/videos/${row.id}/download`
  link.download = row.fileName
  link.click()
}

async function handleDelete(row) {
  try {
    await del(`/training/videos/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { console.error(e) }
}

onMounted(() => { fetchData(); loadCourses() })
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload) {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 40px 0;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
}

.upload-text {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.upload-text em {
  color: #409eff;
  font-style: normal;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  width: 100%;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.file-size {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.progress-area {
  width: 100%;
}

.progress-info {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
