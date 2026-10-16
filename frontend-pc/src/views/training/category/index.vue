<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="分类名称">
          <el-input v-model="queryParams.name" placeholder="请输入分类名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增分类</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="name" label="分类名称" width="200" />
        <el-table-column prop="code" label="分类编码" width="180" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              @change="(val) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该分类？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="480px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：GMP基础 / 操作规范" maxlength="50" />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input v-model="form.code" placeholder="如 gmp_basic" maxlength="30" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" inline-prompt
                     active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 课程分类管理页面
 * <p>
 * 对应后端：GET /training/course-categories/page（分页）、
 *          POST /training/course-categories（新增）、
 *          PUT /training/course-categories/{id}（修改）、
 *          PUT /training/course-categories/{id}/status（启停用）、
 *          DELETE /training/course-categories/{id}（删除）。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { get, post, put, del } from '@/utils/request'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({ page: 1, size: 10, name: '', status: null })

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ id: null, name: '', code: '', sort: 0, status: 1 })
const formRef = ref(null)
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

/** 列表查询 */
async function loadData() {
  loading.value = true
  try {
    const res = await get('/training/course-categories/page', queryParams)
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { queryParams.page = 1; loadData() }
function handleReset() { queryParams.name = ''; queryParams.status = null; queryParams.page = 1; loadData() }

/** 新增 */
function handleAdd() {
  isEdit.value = false
  Object.assign(form, { id: null, name: '', code: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, { id: row.id, name: row.name, code: row.code, sort: row.sort, status: row.status })
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await put('/training/course-categories/' + form.id, form)
      ElMessage.success('修改成功')
    } else {
      await post('/training/course-categories', form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    if (e?.message?.includes('分类名称已存在')) {
      ElMessage.error('分类名称已存在，请使用其他名称')
    }
  } finally {
    submitting.value = false
  }
}

/** 删除 */
async function handleDelete(row) {
  await del('/training/course-categories/' + row.id)
  ElMessage.success('删除成功')
  loadData()
}

/** 切换状态 */
async function handleToggleStatus(row, val) {
  try {
    await put('/training/course-categories/' + row.id + '/status', null, { params: { status: val } })
    ElMessage.success(val === 1 ? '已启用' : '已禁用')
  } catch (e) {
    row.status = val === 1 ? 0 : 1 // 回滚
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.search-card { margin-bottom: 12px; }
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
