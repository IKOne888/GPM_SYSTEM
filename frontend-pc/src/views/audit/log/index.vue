<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="操作模块">
          <el-select v-model="queryParams.module" placeholder="请选择模块" clearable>
            <el-option label="用户管理" value="user" />
            <el-option label="部门管理" value="department" />
            <el-option label="岗位管理" value="position" />
            <el-option label="课程管理" value="course" />
            <el-option label="视频课件" value="video" />
            <el-option label="培训计划" value="plan" />
            <el-option label="培训任务" value="task" />
            <el-option label="题库管理" value="question" />
            <el-option label="试卷管理" value="paper" />
            <el-option label="考试" value="exam" />
            <el-option label="系统" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="queryParams.action" placeholder="请选择类型" clearable>
            <el-option label="新增" value="create" />
            <el-option label="修改" value="update" />
            <el-option label="删除" value="delete" />
            <el-option label="查询" value="query" />
            <el-option label="导出" value="export" />
            <el-option label="导入" value="import" />
            <el-option label="审批" value="approve" />
            <el-option label="登录" value="login" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="queryParams.operator" placeholder="请输入操作人" clearable />
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
          <el-button @click="handleExport">
            <el-icon><Download /></el-icon> 导出日志
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <!-- 只读提示 -->
      <el-alert
        title="审计追踪日志为只读记录，不可修改或删除"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="日志ID" width="80" />
        <el-table-column prop="module" label="操作模块" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ moduleMap[row.module] || row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" size="small">{{ actionMap[row.action] || row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="operatorIp" label="操作IP" width="140" />
        <el-table-column prop="requestMethod" label="请求方式" width="90" align="center" />
        <el-table-column prop="requestUrl" label="请求URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时(ms)" width="90" align="center" />
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.operateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="650px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="日志ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ moduleMap[detailData.module] || detailData.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ actionMap[detailData.action] || detailData.action }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailData.operator }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ detailData.operatorIp }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detailData.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ detailData.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ detailData.description }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <div class="json-content">{{ formatJson(detailData.requestParams) }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <div class="json-content">{{ formatJson(detailData.responseResult) }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="detailData.status === 1 ? 'success' : 'danger'" size="small">
            {{ detailData.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailData.duration }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detailData.operateTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.errorMsg" label="错误信息" :span="2">
          <span style="color: #f56c6c">{{ detailData.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { get } from '@/utils/request'
import service from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

const moduleMap = {
  user: '用户管理', department: '部门管理', position: '岗位管理',
  course: '课程管理', video: '视频课件', plan: '培训计划',
  task: '培训任务', question: '题库管理', paper: '试卷管理',
  exam: '考试', system: '系统'
}
const actionMap = {
  create: '新增', update: '修改', delete: '删除', query: '查询',
  export: '导出', import: '导入', approve: '审批', login: '登录'
}
function actionTagType(a) {
  return { create: 'success', update: 'primary', delete: 'danger', approve: 'warning', login: 'info' }[a] || 'info'
}

const queryParams = reactive({
  module: '', action: '', operator: '', dateRange: [], page: 1, size: 20
})
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const detailVisible = ref(false)
const detailData = reactive({})

async function fetchData() {
  loading.value = true
  const params = { ...queryParams }
  if (queryParams.dateRange?.length === 2) {
    params.startDate = queryParams.dateRange[0]
    params.endDate = queryParams.dateRange[1]
  }
  delete params.dateRange
  try {
    const res = await get('/audit/logs', params)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() {
  Object.assign(queryParams, { module: '', action: '', operator: '', dateRange: [], page: 1 })
  fetchData()
}

function handleViewDetail(row) {
  Object.assign(detailData, { ...row })
  detailVisible.value = true
}

function formatJson(str) {
  if (!str) return '--'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

async function handleExport() {
  // 通过带 Token 的请求下载导出文件（浏览器直链无法携带 Authorization 头）
  const params = {}
  if (queryParams.module) params.module = queryParams.module
  if (queryParams.action) params.action = queryParams.action
  if (queryParams.operator) params.operator = queryParams.operator
  try {
    const blob = await service.get('/audit/logs/export', { params, responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = '审计日志.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) { console.error(e) }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }

.json-content {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  width: 100%;
}
</style>
