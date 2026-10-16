<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="岗位名称">
          <el-input v-model="queryParams.name" placeholder="请输入岗位名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="正常" :value="1" />
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
          <el-icon><Plus /></el-icon> 新增岗位
        </el-button>
        <el-button type="warning" @click="matrixDialogVisible = true">
          <el-icon><Grid /></el-icon> 培训矩阵配置
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="code" label="岗位编码" width="120" />
        <el-table-column prop="name" label="岗位名称" width="150" />
        <el-table-column prop="departmentName" label="所属部门" width="150" />
        <el-table-column prop="level" label="岗位级别" width="100" />
        <el-table-column prop="description" label="岗位描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleMatrix(row)">培训矩阵</el-button>
            <el-popconfirm title="确定删除该岗位？" @confirm="handleDelete(row)">
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="岗位编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="所属部门" prop="departmentId">
          <el-select v-model="formData.departmentId" placeholder="请选择部门" style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位级别">
          <el-select v-model="formData.level" placeholder="请选择级别" style="width: 100%">
            <el-option label="初级" value="初级" />
            <el-option label="中级" value="中级" />
            <el-option label="高级" value="高级" />
            <el-option label="专家" value="专家" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入岗位描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 岗位培训矩阵配置对话框 -->
    <el-dialog v-model="matrixDialogVisible" title="岗位培训矩阵配置" width="900px" destroy-on-close>
      <div class="matrix-header">
        <el-select v-model="matrixPositionId" placeholder="选择岗位" style="width: 250px" @change="loadMatrixData">
          <el-option v-for="pos in allPositions" :key="pos.id" :label="pos.name" :value="pos.id" />
        </el-select>
        <el-button type="primary" @click="saveMatrix" :loading="matrixSaving">保存矩阵</el-button>
      </div>
      <el-table :data="matrixData" border style="margin-top: 16px">
        <el-table-column prop="courseName" label="培训课程" min-width="200" />
        <el-table-column prop="category" label="课程分类" width="120" />
        <el-table-column label="必修" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.required" />
          </template>
        </el-table-column>
        <el-table-column label="选修" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.elective" />
          </template>
        </el-table-column>
        <el-table-column label="培训频次" width="150">
          <template #default="{ row }">
            <el-select v-model="row.frequency" size="small" placeholder="选择频次">
              <el-option label="入职时" value="onboard" />
              <el-option label="每年" value="yearly" />
              <el-option label="每半年" value="half_year" />
              <el-option label="每季度" value="quarterly" />
              <el-option label="一次性" value="once" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post, put, del } from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

const queryParams = reactive({ name: '', status: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const departments = ref([])
const allPositions = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null, code: '', name: '', departmentId: '', level: '', description: '', status: 1
})
const formRules = {
  code: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

// 培训矩阵
const matrixDialogVisible = ref(false)
const matrixPositionId = ref('')
const matrixData = ref([])
const matrixSaving = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await get('/system/positions', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function loadOptions() {
  try {
    const [deptRes, posRes] = await Promise.all([
      get('/system/departments/list'),
      get('/system/positions/list')
    ])
    departments.value = deptRes.data || deptRes || []
    allPositions.value = posRes.data || posRes || []
  } catch (e) { console.error(e) }
}

function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() { Object.assign(queryParams, { name: '', status: '', page: 1 }); fetchData() }

function handleAdd() {
  dialogTitle.value = '新增岗位'
  Object.assign(formData, { id: null, code: '', name: '', departmentId: '', level: '', description: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑岗位'
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (formData.id) {
      await put(`/system/positions/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await post('/system/positions', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

async function handleDelete(row) {
  try {
    await del(`/system/positions/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { console.error(e) }
}

// 培训矩阵操作
function handleMatrix(row) {
  matrixPositionId.value = row.id
  matrixDialogVisible.value = true
  loadMatrixData()
}

async function loadMatrixData() {
  if (!matrixPositionId.value) return
  try {
    const res = await get(`/system/positions/${matrixPositionId.value}/training-matrix`)
    matrixData.value = res.data || res || []
  } catch (e) { console.error(e) }
}

async function saveMatrix() {
  matrixSaving.value = true
  try {
    await post(`/system/positions/${matrixPositionId.value}/training-matrix`, matrixData.value)
    ElMessage.success('矩阵配置保存成功')
  } catch (e) { console.error(e) } finally { matrixSaving.value = false }
}

onMounted(() => { fetchData(); loadOptions() })
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 10px;
}

.matrix-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
