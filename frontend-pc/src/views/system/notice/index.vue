<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 左：发布表单 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">发布通知公告</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="公告标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="公告正文" prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="6"
                placeholder="请输入公告正文"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="接收范围" prop="targetType">
              <!-- targetType：1-全员 2-按部门 3-按岗位（与后端契约一致） -->
              <el-radio-group v-model="form.targetType" @change="handleTargetTypeChange">
                <el-radio-button :value="1">全员</el-radio-button>
                <el-radio-button :value="2">按部门</el-radio-button>
                <el-radio-button :value="3">按岗位</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <!-- 按部门：展示部门多选 -->
            <el-form-item v-if="form.targetType === 2" label="选择部门" prop="targetIds">
              <el-select v-model="form.targetIds" multiple filterable placeholder="请选择接收部门" style="width: 100%">
                <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
            <!-- 按岗位：展示岗位多选 -->
            <el-form-item v-if="form.targetType === 3" label="选择岗位" prop="targetIds">
              <el-select v-model="form.targetIds" multiple filterable placeholder="请选择接收岗位" style="width: 100%">
                <el-option v-for="p in positions" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="publishing" @click="handlePublish">立即发布</el-button>
              <el-button @click="handleResetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右：发布历史 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">发布历史</span>
          </template>
          <el-table v-loading="loading" :data="historyList" border stripe>
            <el-table-column prop="title" label="公告标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="content" label="公告正文" min-width="220" show-overflow-tooltip />
            <el-table-column prop="receiveCount" label="触达人数" width="100" align="center" />
            <el-table-column label="发布时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="fetchHistory"
            @current-change="fetchHistory"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post } from '@/utils/request'
// 时间格式化工具：统一渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

/** 发布表单引用 */
const formRef = ref(null)
/** 发布表单数据 */
const form = reactive({ title: '', content: '', targetType: 1, targetIds: [] })
/** 发布中状态 */
const publishing = ref(false)
/** 发布表单校验规则 */
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告正文', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择接收范围', trigger: 'change' }]
}

/** 部门下拉数据 */
const departments = ref([])
/** 岗位下拉数据 */
const positions = ref([])

/** 历史列表加载态 */
const loading = ref(false)
/** 历史列表数据 */
const historyList = ref([])
/** 历史总条数 */
const total = ref(0)
/** 历史分页条件 */
const query = reactive({ page: 1, size: 10 })

/** 切换接收范围时清空已选对象，避免残留无效ID */
function handleTargetTypeChange() {
  form.targetIds = []
}

/** 加载接收对象下拉（部门/岗位） */
async function fetchTargets() {
  try {
    const res = await get('/system/notices/targets')
    departments.value = res.data?.departments || []
    positions.value = res.data?.positions || []
  } catch (e) {
    console.error(e)
  }
}

/** 拉取发布历史 */
async function fetchHistory() {
  loading.value = true
  try {
    const res = await get('/system/notices', { page: query.page, size: query.size })
    historyList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

/** 发布公告 */
async function handlePublish() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  // 非全员时必须选择接收对象，与后端校验保持一致
  if (form.targetType !== 1 && (!form.targetIds || form.targetIds.length === 0)) {
    ElMessage.warning('请选择接收对象')
    return
  }
  publishing.value = true
  try {
    const res = await post('/system/notices', {
      title: form.title,
      content: form.content,
      targetType: form.targetType,
      targetIds: form.targetType === 1 ? [] : form.targetIds
    })
    // 后端在 message 中回显实际触达人数
    ElMessage.success(res.message || '发布成功')
    handleResetForm()
    // 回到第一页展示最新公告
    query.page = 1
    fetchHistory()
  } catch (e) {
    console.error(e)
  } finally {
    publishing.value = false
  }
}

/** 重置表单 */
function handleResetForm() {
  form.title = ''
  form.content = ''
  form.targetType = 1
  form.targetIds = []
}

onMounted(() => {
  fetchTargets()
  fetchHistory()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.card-title { font-weight: 600; }
</style>