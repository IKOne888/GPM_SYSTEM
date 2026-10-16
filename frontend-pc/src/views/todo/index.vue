<template>
  <div class="page-container">
    <!-- 总览条 -->
    <el-card shadow="never" class="summary-card">
      <div class="summary-left">
        <el-icon size="28" color="#409EFF"><Bell /></el-icon>
        <div class="summary-text">
          <div class="summary-title">我的待办</div>
          <div class="summary-desc">待审核任务、待处理需求、待审批计划统一聚合，按角色自动过滤</div>
        </div>
      </div>
      <div class="summary-total">
        <span class="total-num">{{ data.total || 0 }}</span>
        <span class="total-label">项待办</span>
      </div>
    </el-card>

    <!-- 三个待办分区 -->
    <el-row :gutter="16">
      <el-col v-for="section in sections" :key="section.key" :span="8">
        <el-card shadow="never" class="todo-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :color="section.color"><component :is="section.icon" /></el-icon>
                <span class="card-title">{{ section.title }}</span>
                <el-tag :type="section.tagType" size="small" round>{{ groupOf(section.key).count || 0 }}</el-tag>
              </div>
              <!-- 查看全部：跳转到对应菜单并带筛选参数 -->
              <el-button link type="primary" @click="goAll(section)">查看全部</el-button>
            </div>
          </template>

          <!-- 明细列表 -->
          <div v-if="groupOf(section.key).items && groupOf(section.key).items.length" class="todo-list">
            <div
              v-for="item in groupOf(section.key).items"
              :key="item.id"
              class="todo-item"
              @click="goDetail(section, item)"
            >
              <div class="item-main">
                <span class="item-title">{{ item.title }}</span>
                <!-- 待审核任务展示当前审核阶段 -->
                <el-tag v-if="item.stage" size="small" effect="plain" :type="item.stage === 'leader' ? 'warning' : 'success'">
                  {{ item.stage === 'leader' ? '待主管审核' : '待QA复核' }}
                </el-tag>
              </div>
              <div class="item-sub">
                <span>{{ item.extra }}</span>
                <span>{{ formatDateTime(item.time) }}</span>
              </div>
            </div>
          </div>
          <!-- 空态 -->
          <el-empty v-else description="暂无待办" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/utils/request'
// 时间格式化工具：统一渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

const router = useRouter()

/** 待办聚合数据 */
const data = ref({ reviewPending: {}, demandPending: {}, planPending: {}, total: 0 })

/** 三个待办分区的静态配置（key 对应后端返回字段） */
const sections = reactive([
  {
    key: 'reviewPending',
    title: '待审核任务',
    icon: 'List',
    color: '#E6A23C',
    tagType: 'warning',
    // 点击「查看全部」跳转到培训任务页的待我审核页签
    allPath: '/training/task?tab=review'
  },
  {
    key: 'demandPending',
    title: '待处理需求',
    icon: 'Notification',
    color: '#409EFF',
    tagType: 'primary',
    allPath: '/training/demand?status=0'
  },
  {
    key: 'planPending',
    title: '待审批计划',
    icon: 'Calendar',
    color: '#67C23A',
    tagType: 'success',
    allPath: '/training/plan?approvalStatus=1'
  }
])

/**
 * 按 key 取待办分组（避免模板中多次取值报错）
 * @param {string} key reviewPending/demandPending/planPending
 */
function groupOf(key) {
  return data.value[key] || { count: 0, items: [] }
}

/** 拉取待办聚合数据 */
async function fetchTodo() {
  try {
    const res = await get('/dashboard/todo')
    data.value = res.data || {}
  } catch (e) {
    console.error(e)
  }
}

/**
 * 查看全部：跳转到对应菜单并带筛选参数
 * @param {object} section 分区配置
 */
function goAll(section) {
  router.push(section.allPath)
}

/**
 * 点击明细：需求/计划跳详情列表；任务跳审核页签
 * @param {object} section 分区配置
 * @param {object} item 明细条目
 */
function goDetail(section, item) {
  router.push(section.allPath)
}

onMounted(fetchTodo)
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.summary-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.summary-left { display: flex; align-items: center; gap: 14px; }
.summary-title { font-size: 18px; font-weight: 600; color: #303133; }
.summary-desc { font-size: 13px; color: #909399; margin-top: 4px; }
.summary-total { display: flex; align-items: baseline; gap: 6px; }
.total-num { font-size: 30px; font-weight: 700; color: #409EFF; }
.total-label { font-size: 13px; color: #909399; }

.card-header { display: flex; align-items: center; justify-content: space-between; }
.header-left { display: flex; align-items: center; gap: 8px; }
.card-title { font-weight: 600; }

.todo-list { display: flex; flex-direction: column; }
.todo-item {
  padding: 10px 4px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}
.todo-item:last-child { border-bottom: none; }
.todo-item:hover { background-color: #f5f7fa; }
.item-main { display: flex; align-items: center; gap: 8px; }
.item-title { font-size: 14px; color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-sub { display: flex; justify-content: space-between; font-size: 12px; color: #909399; margin-top: 4px; }
</style>