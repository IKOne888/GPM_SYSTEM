<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="题目关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索题目内容" clearable />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="queryParams.type" placeholder="请选择题型" clearable>
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
            <el-option label="填空题" value="fill" />
            <el-option label="简答题" value="short_answer" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="queryParams.difficulty" placeholder="请选择难度" clearable>
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程分类">
          <el-select v-model="queryParams.category" placeholder="请选择分类" clearable>
            <el-option label="GMP基础" value="gmp_basic" />
            <el-option label="岗位技能" value="skill" />
            <el-option label="法规标准" value="regulation" />
            <el-option label="质量安全" value="quality" />
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
          <el-icon><Plus /></el-icon> 新增题目
        </el-button>
        <el-button type="success" @click="importVisible = true">
          <el-icon><Upload /></el-icon> 批量导入
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon> 导出题库
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ typeMap[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="difficultyTagType(row.difficulty)" size="small">{{ difficultyMap[row.difficulty] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="score" label="分值" width="70" align="center" />
        <el-table-column prop="answer" label="正确答案" width="150" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该题目？" @confirm="handleDelete(row)">
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

    <!-- 新增/编辑题目对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="题型" prop="type">
          <el-radio-group v-model="formData.type" @change="handleTypeChange">
            <el-radio value="single">单选题</el-radio>
            <el-radio value="multiple">多选题</el-radio>
            <el-radio value="judge">判断题</el-radio>
            <el-radio value="fill">填空题</el-radio>
            <el-radio value="short_answer">简答题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="formData.difficulty">
            <el-radio value="easy">简单</el-radio>
            <el-radio value="medium">中等</el-radio>
            <el-radio value="hard">困难</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="课程分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="GMP基础" value="gmp_basic" />
            <el-option label="岗位技能" value="skill" />
            <el-option label="法规标准" value="regulation" />
            <el-option label="质量安全" value="quality" />
          </el-select>
        </el-form-item>
        <el-form-item label="分值" prop="score">
          <el-input-number v-model="formData.score" :min="1" :max="100" />
        </el-form-item>

        <!-- 选项（单选/多选） -->
        <template v-if="formData.type === 'single' || formData.type === 'multiple'">
          <el-form-item v-for="(opt, idx) in formData.options" :key="idx" :label="'选项' + String.fromCharCode(65 + idx)">
            <div style="display: flex; gap: 8px; width: 100%">
              <el-input v-model="opt.text" placeholder="选项内容" style="flex: 1" />
              <el-checkbox v-model="opt.isAnswer">正确答案</el-checkbox>
              <el-button link type="danger" @click="formData.options.splice(idx, 1)" v-if="formData.options.length > 2">删除</el-button>
            </div>
          </el-form-item>
          <el-form-item v-if="formData.type === 'single' || formData.type === 'multiple'" label=" ">
            <el-button type="primary" link @click="addOption" :disabled="formData.options.length >= 8">
              <el-icon><Plus /></el-icon> 添加选项
            </el-button>
          </el-form-item>
        </template>

        <!-- 判断题答案 -->
        <el-form-item v-if="formData.type === 'judge'" label="正确答案">
          <el-radio-group v-model="formData.answer">
            <el-radio value="正确">正确</el-radio>
            <el-radio value="错误">错误</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 填空/简答答案 -->
        <el-form-item v-if="formData.type === 'fill' || formData.type === 'short_answer'" label="参考答案">
          <el-input v-model="formData.answer" type="textarea" :rows="2" placeholder="请输入参考答案" />
        </el-form-item>

        <el-form-item label="解析">
          <el-input v-model="formData.analysis" type="textarea" :rows="2" placeholder="请输入题目解析（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importVisible" title="批量导入题目" width="500px" destroy-on-close>
      <el-upload
        drag
        accept=".xlsx,.xls,.doc,.docx"
        :auto-upload="false"
        :on-change="handleImportFile"
        :limit="1"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div>将文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="upload-tip">支持 xlsx/xls 格式，请先下载模板填写后上传</div>
        </template>
      </el-upload>
      <div style="margin-top: 12px">
        <el-button type="primary" link @click="downloadTemplate">
          <el-icon><Download /></el-icon> 下载导入模板
        </el-button>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">开始导入</el-button>
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

const typeMap = { single: '单选题', multiple: '多选题', judge: '判断题', fill: '填空题', short_answer: '简答题' }
const difficultyMap = { easy: '简单', medium: '中等', hard: '困难' }
function difficultyTagType(d) { return { easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info' }

const queryParams = reactive({ keyword: '', type: '', difficulty: '', category: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null, type: 'single', content: '', difficulty: 'medium', category: '', score: 5,
  options: [
    { text: '', isAnswer: true },
    { text: '', isAnswer: false },
    { text: '', isAnswer: false },
    { text: '', isAnswer: false }
  ],
  answer: '', analysis: ''
})
const formRules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  score: [{ required: true, message: '请输入分值', trigger: 'blur' }]
}

// 导入
const importVisible = ref(false)
const importFile = ref(null)
const importLoading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await get('/exam/questions', queryParams)
    tableData.value = res.data?.records || res.records || []
    total.value = res.data?.total || res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() { Object.assign(queryParams, { keyword: '', type: '', difficulty: '', category: '', page: 1 }); fetchData() }

function handleTypeChange(type) {
  if (type === 'judge') {
    formData.options = []
    formData.answer = '正确'
  } else if (type === 'single' || type === 'multiple') {
    formData.options = [
      { text: '', isAnswer: type === 'single' ? true : false },
      { text: '', isAnswer: false },
      { text: '', isAnswer: false },
      { text: '', isAnswer: false }
    ]
    formData.answer = ''
  } else {
    formData.options = []
    formData.answer = ''
  }
}

function addOption() {
  formData.options.push({ text: '', isAnswer: false })
}

function handleAdd() {
  dialogTitle.value = '新增题目'
  Object.assign(formData, { id: null, type: 'single', content: '', difficulty: 'medium', category: '', score: 5, options: [{ text: '', isAnswer: true }, { text: '', isAnswer: false }, { text: '', isAnswer: false }, { text: '', isAnswer: false }], answer: '', analysis: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑题目'
  Object.assign(formData, { ...row, options: row.options || [] })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (formData.id) {
      await put(`/exam/questions/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await post('/exam/questions', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

async function handleDelete(row) {
  try {
    await del(`/exam/questions/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { console.error(e) }
}

function handleImportFile(file) {
  importFile.value = file.raw
}

async function handleImport() {
  if (!importFile.value) { ElMessage.warning('请选择文件'); return }
  importLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    await post('/exam/questions/import', fd)
    ElMessage.success('导入成功')
    importVisible.value = false
    fetchData()
  } catch (e) { console.error(e) } finally { importLoading.value = false }
}

function downloadTemplate() {
  const link = document.createElement('a')
  link.href = '/api/exam/questions/template'
  link.download = '题目导入模板.xlsx'
  link.click()
}

function handleExport() {
  const link = document.createElement('a')
  link.href = '/api/exam/questions/export'
  link.download = '题库导出.xlsx'
  link.click()
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
.upload-icon { font-size: 40px; color: #c0c4cc; }
.upload-tip { margin-top: 8px; font-size: 12px; color: #909399; }
</style>
