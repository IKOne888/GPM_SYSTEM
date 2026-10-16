<template>
  <div class="demand-page">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">需求申报</h2>
        <p class="gmp-page-subtitle">提交培训诉求，培训管理员处理后将通过站内信反馈处理结果</p>
      </div>
      <el-button type="primary" @click="openSubmit">
        <el-icon><Plus /></el-icon> 提交培训需求
      </el-button>
    </div>

    <!-- 统计条：待处理 / 已采纳 / 已拒绝 -->
    <div class="stat-row">
      <div class="stat-card gmp-card">
        <div class="stat-num stat-num--pending">{{ stats.pending }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card gmp-card">
        <div class="stat-num stat-num--accepted">{{ stats.accepted }}</div>
        <div class="stat-label">已采纳</div>
      </div>
      <div class="stat-card gmp-card">
        <div class="stat-num stat-num--rejected">{{ stats.rejected }}</div>
        <div class="stat-label">已拒绝</div>
      </div>
    </div>

    <!-- 我的申报列表 -->
    <div v-loading="loading" class="list-card gmp-card">
      <template v-if="list.length > 0">
        <div v-for="item in list" :key="item.id" class="demand-item" @click="openDetail(item)">
          <div class="item-main">
            <span class="item-title">{{ item.title }}</span>
            <!-- 状态标签：0-待处理 1-已采纳 2-已拒绝 -->
            <el-tag :type="statusTag(item.status)" size="small" effect="light">{{ item.statusText }}</el-tag>
          </div>
          <div class="item-sub">
            <span>培训主题：{{ item.trainingTopic || '未填写' }}</span>
            <span>{{ formatDateTime(item.createTime) }}</span>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无申报记录，点击右上角提交培训需求" />

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

    <!-- 提交需求弹窗 -->
    <el-dialog v-model="submitVisible" title="提交培训需求" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入需求标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="培训主题" prop="trainingTopic">
          <el-input v-model="form.trainingTopic" placeholder="如：无菌操作规范" maxlength="100" />
        </el-form-item>
        <el-form-item label="培训对象" prop="trainingTarget">
          <el-input v-model="form.trainingTarget" placeholder="如：生产部新入职员工" maxlength="200" />
        </el-form-item>
        <el-form-item label="需求内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请描述培训需求的具体内容与背景"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 需求详情弹窗 -->
    <el-dialog v-model="detailVisible" title="需求详情" width="560px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="需求标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="培训主题">{{ detail.trainingTopic || '-' }}</el-descriptions-item>
        <el-descriptions-item label="培训对象">{{ detail.trainingTarget || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需求内容">{{ detail.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请部门">{{ detail.departmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="statusTag(detail.status)" size="small">{{ detail.statusText }}</el-tag>
        </el-descriptions-item>
        <!-- 处理意见：采纳备注 / 拒绝原因 -->
        <el-descriptions-item label="处理意见">{{ detail.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 学员端「培训需求申报」页面
 * <p>
 * 调用 /study/demands 系列接口：列表（仅本人）、详情、提交；
 * 统计条通过三次「状态过滤 + size=1」请求读取 total 得到各状态数量。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyDemands, getDemandDetail, submitDemand } from '@/api/demand'
import { formatDateTime } from '@/utils/format'

/** 状态标签配色：0-待处理 1-已采纳 2-已拒绝 */
function statusTag(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

/** 加载态 */
const loading = ref(false)
/** 列表数据 */
const list = ref([])
/** 总条数 */
const total = ref(0)
/** 查询条件 */
const query = reactive({ page: 1, size: 10 })
/** 各状态数量统计 */
const stats = reactive({ pending: 0, accepted: 0, rejected: 0 })

/** 提交弹窗可见性 */
const submitVisible = ref(false)
/** 提交中状态 */
const submitting = ref(false)
/** 表单引用 */
const formRef = ref(null)
/** 表单数据 */
const form = reactive({ title: '', trainingTopic: '', trainingTarget: '', content: '' })
/** 表单校验规则 */
const rules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入需求内容', trigger: 'blur' }]
}

/** 详情弹窗可见性 */
const detailVisible = ref(false)
/** 详情数据 */
const detail = ref({})

/** 加载本人申报列表 */
async function loadList() {
  loading.value = true
  try {
    const res = await getMyDemands({ page: query.page, size: query.size })
    list.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

/** 加载各状态数量（分别请求 size=1 读取 total） */
async function loadStats() {
  try {
    const [pending, accepted, rejected] = await Promise.all([
      getMyDemands({ page: 1, size: 1, status: 0 }),
      getMyDemands({ page: 1, size: 1, status: 1 }),
      getMyDemands({ page: 1, size: 1, status: 2 })
    ])
    stats.pending = Number(pending.data?.total) || 0
    stats.accepted = Number(accepted.data?.total) || 0
    stats.rejected = Number(rejected.data?.total) || 0
  } catch (e) {
    console.error(e)
  }
}

/** 条件变更后重新查询（回到第一页） */
function handleSearch() {
  query.page = 1
  loadList()
}

/** 打开提交弹窗并清空表单 */
function openSubmit() {
  form.title = ''
  form.trainingTopic = ''
  form.trainingTarget = ''
  form.content = ''
  submitVisible.value = true
}

/** 提交培训需求 */
async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await submitDemand({ ...form })
    ElMessage.success('已提交，等待培训管理员处理')
    submitVisible.value = false
    // 刷新列表与统计
    handleSearch()
    loadStats()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

/**
 * 打开详情（调用详情接口，越权时后端返回 403）
 * @param {object} item 列表行
 */
async function openDetail(item) {
  try {
    const res = await getDemandDetail(item.id)
    detail.value = res.data || {}
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadList()
  loadStats()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

/* 统计条 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 18px 20px;
  text-align: center;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
}

.stat-num--pending { color: #e6a23c; }
.stat-num--accepted { color: var(--brand-primary); }
.stat-num--rejected { color: var(--danger); }

.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* 列表 */
.list-card {
  padding: 8px 16px 16px;
  min-height: 320px;
}

.demand-item {
  padding: 16px 6px;
  border-bottom: 1px dashed var(--border-color);
  cursor: pointer;
  transition: background 0.2s;
}

.demand-item:last-of-type {
  border-bottom: none;
}

.demand-item:hover {
  background: #f7faff;
}

.item-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-title {
  font-size: 15px;
  color: var(--text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-sub {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>