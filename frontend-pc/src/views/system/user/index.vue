<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="queryParams.departmentId" placeholder="请选择部门" clearable>
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
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

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增用户
        </el-button>
        <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon> 批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
        border
        stripe
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="departmentName" label="部门" width="120" />
        <el-table-column prop="positionName" label="岗位" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleResetPwd(row)">重置密码</el-button>
            <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row)">
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
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="!!formData.id" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item v-if="!formData.id" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="部门" prop="departmentId">
          <el-select v-model="formData.departmentId" placeholder="请选择部门" style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="positionId">
          <el-select v-model="formData.positionId" placeholder="请选择岗位" style="width: 100%">
            <el-option v-for="pos in positions" :key="pos.id" :label="pos.name" :value="pos.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post, put, del } from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

// 查询参数
const queryParams = reactive({
  username: '',
  departmentId: '',
  status: '',
  page: 1,
  size: 10
})

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedIds = ref([])
const departments = ref([])
const positions = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null,
  username: '',
  realName: '',
  password: '',
  departmentId: '',
  positionId: '',
  email: '',
  phone: '',
  status: 1
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  positionId: [{ required: true, message: '请选择岗位', trigger: 'change' }]
}

// 获取数据
async function fetchData() {
  loading.value = true
  try {
    const res = await get('/system/users', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 加载部门和岗位下拉
async function loadOptions() {
  try {
    const [deptRes, posRes] = await Promise.all([
      get('/system/departments/list'),
      get('/system/positions/list')
    ])
    departments.value = deptRes.data || deptRes || []
    positions.value = posRes.data || posRes || []
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  queryParams.page = 1
  fetchData()
}

function handleReset() {
  Object.assign(queryParams, { username: '', departmentId: '', status: '', page: 1 })
  fetchData()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

function handleAdd() {
  dialogTitle.value = '新增用户'
  Object.assign(formData, { id: null, username: '', realName: '', password: '', departmentId: '', positionId: '', email: '', phone: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑用户'
  Object.assign(formData, { ...row, password: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (formData.id) {
      await put(`/system/users/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await post('/system/users', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await del(`/system/users/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个用户？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await post('/system/users/batch-delete', { ids: selectedIds.value })
      ElMessage.success('批量删除成功')
      fetchData()
    } catch (e) {
      console.error(e)
    }
  }).catch(() => {})
}

async function handleStatusChange(row) {
  try {
    await put(`/system/users/${row.id}/status`, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch (e) {
    row.status = row.status === 1 ? 0 : 1
    console.error(e)
  }
}

function handleResetPwd(row) {
  ElMessageBox.confirm(`确定重置用户 "${row.username}" 的密码？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await put(`/system/users/${row.id}/reset-password`)
      ElMessage.success('密码已重置为默认密码')
    } catch (e) {
      console.error(e)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
  loadOptions()
})
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
</style>
