<template>
  <div class="material-page">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">资料中心</h2>
        <p class="gmp-page-subtitle">汇总您已报名课程的培训资料，支持预览与下载</p>
      </div>
      <el-button :icon="Refresh" @click="loadList">刷新</el-button>
    </div>

    <!-- 筛选栏：关键词 + 课程 + 文件类型 -->
    <div class="filter-bar gmp-card">
      <el-input
        v-model="query.keyword"
        placeholder="搜索资料名称"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <el-select
        v-model="query.courseId"
        placeholder="全部课程"
        clearable
        class="filter-select"
        @change="handleSearch"
      >
        <el-option
          v-for="item in courseOptions"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>

      <el-select
        v-model="query.fileType"
        placeholder="全部类型"
        clearable
        class="filter-select filter-select--sm"
        @change="handleSearch"
      >
        <el-option v-for="type in FILE_TYPES" :key="type" :label="type" :value="type" />
      </el-select>
    </div>

    <!-- 资料表格 -->
    <div v-loading="loading" class="table-card gmp-card">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="name" label="资料名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <el-icon class="file-icon" :style="{ color: typeColor(row.fileType) }">
                <Document />
              </el-icon>
              <span>{{ row.name || row.fileName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="courseName" label="所属课程" min-width="180" show-overflow-tooltip />

        <el-table-column prop="fileType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :color="typeColor(row.fileType)" effect="dark" size="small" style="border: none">
              {{ row.fileType || '-' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>

        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.uploadTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
            <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无符合条件的资料" :image-size="90" />
        </template>
      </el-table>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
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
 * 资料中心页面
 * <p>
 * 数据来源：GET /study/materials（仅返回本人已关联课程的启用资料）。
 * 预览/下载统一通过 GET /study/courses/files/{fileId}/url 获取 MinIO 预签名地址。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getMaterials, getMaterialCourseOptions } from '@/api/material'
import { getCourseFileUrl } from '@/api/study'
import { formatFileSize, formatDateTime } from '@/utils/format'

/** 允许的资料类型（与后端白名单一致） */
const FILE_TYPES = ['PDF', 'DOC', 'DOCX', 'XLS', 'XLSX']

/** 加载状态 */
const loading = ref(false)
/** 资料列表 */
const list = ref([])
/** 总条数 */
const total = ref(0)
/** 课程下拉选项 */
const courseOptions = ref([])

/** 查询条件 */
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  courseId: null,
  fileType: ''
})

/** 文件类型标识色 */
function typeColor(type) {
  const map = {
    PDF: '#FF4D4F',
    DOC: '#1677FF',
    DOCX: '#1677FF',
    XLS: '#52C41A',
    XLSX: '#52C41A'
  }
  return map[type] || '#8C9AAE'
}

/**
 * 加载资料列表
 */
async function loadList() {
  loading.value = true
  try {
    const res = await getMaterials({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      courseId: query.courseId || undefined,
      fileType: query.fileType || undefined
    })
    list.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

/**
 * 加载课程下拉选项（取用户可见课程，失败不阻断主流程）
 */
async function loadCourses() {
  try {
    const res = await getMaterialCourseOptions({ page: 1, size: 100 })
    courseOptions.value = res.data?.records || []
  } catch (e) {
    courseOptions.value = []
  }
}

/**
 * 条件变更后重新查询（回到第一页）
 */
function handleSearch() {
  query.page = 1
  loadList()
}

/**
 * 获取资料预签名地址
 * @param {object} row 资料对象
 * @returns {Promise<string|null>} 预签名地址
 */
async function resolveUrl(row) {
  try {
    const res = await getCourseFileUrl(row.id)
    return res.data?.url || res.data || null
  } catch (e) {
    return null
  }
}

/**
 * 预览资料（新窗口打开预签名地址）
 */
async function handlePreview(row) {
  const url = await resolveUrl(row)
  if (url) {
    window.open(url, '_blank')
  } else {
    ElMessage.warning('预览地址获取失败，请稍后重试')
  }
}

/**
 * 下载资料（通过临时 a 标签触发下载）
 */
async function handleDownload(row) {
  const url = await resolveUrl(row)
  if (!url) {
    ElMessage.warning('下载地址获取失败，请稍后重试')
    return
  }
  const link = document.createElement('a')
  link.href = url
  link.download = row.fileName || row.name || 'download'
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(() => {
  loadCourses()
  loadList()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.filter-select {
  width: 220px;
}

.filter-select--sm {
  width: 150px;
}

.table-card {
  padding: 8px 16px 16px;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 17px;
  flex-shrink: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
