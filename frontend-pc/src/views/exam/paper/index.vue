<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="试卷名称">
          <el-input v-model="queryParams.name" placeholder="请输入试卷名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已归档" value="archived" />
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
          <el-icon><Plus /></el-icon> 新增试卷
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="code" label="试卷编号" width="130" />
        <el-table-column prop="name" label="试卷名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="关联课程" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <!-- 学员端按「课程 + 已发布」匹配试卷，未绑定课程的试卷学员无法考试 -->
            <span v-if="row.courseId">{{ courseNameMap[row.courseId] || `课程#${row.courseId}` }}</span>
            <el-tag v-else type="danger" size="small">未绑定课程</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column prop="passScore" label="及格分" width="80" align="center" />
        <el-table-column prop="questionCount" label="题目数" width="80" align="center" />
        <el-table-column prop="duration" label="考试时长(分钟)" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="{ draft: 'info', published: 'success', archived: 'warning' }[row.status]" size="small">
              {{ { draft: '草稿', published: '已发布', archived: '已归档' }[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
            <el-popconfirm title="确定删除该试卷？" @confirm="handleDelete(row)">
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

    <!-- 新增/编辑试卷对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="试卷编号" prop="code">
              <el-input v-model="formData.code" placeholder="如: PAPER-2026-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入试卷名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="绑定课程" prop="courseId">
              <!-- 绑定课程后，学员端才能按课程匹配到本试卷并进入考试 -->
              <el-select v-model="formData.courseId" placeholder="请选择培训课程" clearable style="width: 100%">
                <el-option v-for="item in courseOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-alert type="info" :closable="false" show-icon
                      title="试卷保存为「发布」时会自动通知该课程学员并为其生成培训任务" />
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="总分" prop="totalScore">
              <el-input-number v-model="formData.totalScore" :min="1" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分" prop="passScore">
              <el-input-number v-model="formData.passScore" :min="1" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试时长">
              <el-input-number v-model="formData.duration" :min="10" :max="300" :step="5" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 组卷策略 -->
        <el-divider content-position="left">组卷策略</el-divider>
        <el-form-item label="组卷方式">
          <el-radio-group v-model="formData.compositionType">
            <el-radio value="manual">手动选题</el-radio>
            <el-radio value="random">随机组卷</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 随机组卷规则 -->
        <template v-if="formData.compositionType === 'random'">
          <el-form-item label="组卷规则">
            <div style="width: 100%">
              <el-button type="primary" link @click="addRule">
                <el-icon><Plus /></el-icon> 添加规则
              </el-button>
              <el-table :data="formData.rules" border size="small" style="margin-top: 8px">
                <el-table-column label="题型" width="120">
                  <template #default="{ row }">
                    <el-select v-model="row.type" size="small" style="width: 100%">
                      <el-option label="单选题" value="single" />
                      <el-option label="多选题" value="multiple" />
                      <el-option label="判断题" value="judge" />
                      <el-option label="填空题" value="fill" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="难度" width="120">
                  <template #default="{ row }">
                    <el-select v-model="row.difficulty" size="small" style="width: 100%" clearable>
                      <el-option label="简单" value="easy" />
                      <el-option label="中等" value="medium" />
                      <el-option label="困难" value="hard" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="数量" width="100">
                  <template #default="{ row }">
                    <el-input-number v-model="row.count" :min="1" :max="100" size="small" style="width: 100%" />
                  </template>
                </el-table-column>
                <el-table-column label="每题分值" width="110">
                  <template #default="{ row }">
                    <el-input-number v-model="row.score" :min="1" :max="50" size="small" style="width: 100%" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="60" align="center">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="formData.rules.splice($index, 1)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-form-item>
        </template>

        <el-form-item label="状态">
          <!-- 切换状态时立即校验绑定课程，发布状态未绑定课程将即时提示 -->
          <el-radio-group v-model="formData.status" @change="formRef?.validateField('courseId')">
            <el-radio value="draft">草稿</el-radio>
            <el-radio value="published">发布</el-radio>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post, put, del } from '@/utils/request'
// 时间格式化工具：统一将后端 ISO 时间渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

const queryParams = reactive({ name: '', status: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 课程下拉选项与课程ID→名称映射（列表展示关联课程用）
const courseOptions = ref([])
const courseNameMap = computed(() =>
  courseOptions.value.reduce((map, item) => { map[item.id] = item.name; return map }, {})
)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null, code: '', name: '', courseId: null, totalScore: 100, passScore: 60, duration: 60,
  compositionType: 'random', rules: [], status: 'draft'
})

// 绑定课程校验：发布状态必须绑定课程，否则学员端无法匹配到该试卷
function validateCourseId(rule, value, callback) {
  if (formData.status === 'published' && !value) {
    callback(new Error('试卷发布前必须绑定培训课程'))
    return
  }
  callback()
}

const formRules = {
  code: [{ required: true, message: '请输入试卷编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  courseId: [{ validator: validateCourseId, trigger: 'change' }],
  totalScore: [{ required: true, message: '请输入总分', trigger: 'blur' }],
  passScore: [{ required: true, message: '请输入及格分', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await get('/exam/papers', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() { Object.assign(queryParams, { name: '', status: '', page: 1 }); fetchData() }

function addRule() {
  formData.rules.push({ type: 'single', difficulty: '', count: 10, score: 2 })
}

function handleAdd() {
  dialogTitle.value = '新增试卷'
  Object.assign(formData, { id: null, code: '', name: '', courseId: null, totalScore: 100, passScore: 60, duration: 60, compositionType: 'random', rules: [{ type: 'single', difficulty: 'medium', count: 20, score: 2 }, { type: 'multiple', difficulty: 'medium', count: 10, score: 4 }, { type: 'judge', difficulty: 'easy', count: 10, score: 1 }], status: 'draft' })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑试卷'
  Object.assign(formData, { ...row, rules: row.rules || [] })
  dialogVisible.value = true
}

function handlePreview(row) {
  handleEdit(row)
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (formData.id) {
      await put(`/exam/papers/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await post('/exam/papers', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

async function handleDelete(row) {
  try {
    await del(`/exam/papers/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { console.error(e) }
}

// 加载课程下拉数据（试卷需绑定课程后才能被学员端匹配）
async function loadCourses() {
  try {
    const res = await get('/training/courses/list')
    courseOptions.value = res.data || res || []
  } catch (e) { console.error(e) }
}

onMounted(() => { fetchData(); loadCourses() })
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
</style>
