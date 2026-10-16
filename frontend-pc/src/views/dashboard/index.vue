<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="item in statCards" :key="item.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-title">{{ item.title }}</p>
              <p class="stat-value">{{ item.value }}</p>
            </div>
            <el-icon :size="48" :style="{ color: item.color }">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <div class="stat-footer">
            <span>{{ item.footer }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>月度培训学时趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>各部门培训覆盖率</span>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办事项 & 最近活动 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 待办事项：直接调统一待办中心接口 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>我的待办</span>
              <el-button link type="primary" @click="router.push('/todo')">查看全部 →</el-button>
            </div>
          </template>
          <!-- 加载中骨架 -->
          <el-table v-loading="loading" :data="todoTableData" style="width: 100%">
            <el-table-column prop="title" label="事项" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.tagType" size="small">{{ row.stageText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="time" label="时间" width="160">
              <template #default="{ row }">{{ row.time || '-' }}</template>
            </el-table-column>
          </el-table>
          <!-- 暂无待办 -->
          <el-empty v-if="!loading && todoTableData.length === 0" description="暂无待办事项，去看看其他模块吧" />
        </el-card>
      </el-col>

      <!-- 快速入口：按当前角色展示常用功能，点击直接跳转 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>快速入口</span>
          </template>
          <div class="quick-entry-grid">
            <div
              v-for="entry in quickEntries"
              :key="entry.path"
              class="entry-item"
              @click="router.push(entry.path)"
            >
              <el-icon :size="28" :style="{ color: entry.color }">
                <component :is="entry.icon" />
              </el-icon>
              <span class="entry-label">{{ entry.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 管理端首页（Dashboard）
 *
 * 数据来源：
 * - 统计卡片 / 部门覆盖率饼图 / 月度趋势折线图  →  GET /report/summary
 *   （后端 ReportController 汇总接口，一次返回 overview + deptCompletion + monthHours）
 * - 待办事项表格                                →  GET /dashboard/todo
 *   （统一待办中心接口，按当前登录人角色过滤）
 *
 * 后端接口均已实现（本次开发已通过接口测试），前端直接消费即可。
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { get } from '@/utils/request'

const router = useRouter()

const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null

// 加载状态（控制待办表格 v-loading）
const loading = ref(false)

/**
 * 当前登录角色列表（从 localStorage 读取，quickEntries 用于按权限过滤入口）
 * 登录接口已把 roles 脱敏写入 userInfo
 */
const userRoles = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return Array.isArray(info.roles) ? info.roles : []
  } catch (e) { return [] }
})

/**
 * 快速入口九宫格（按当前登录角色动态展示可用入口）
 * SUPER_ADMIN 能看到全部；其它角色只看自己权限内的
 */
const quickEntries = computed(() => {
  const roles = userRoles.value
  const isSuper = roles.includes('SUPER_ADMIN')
  // 所有候选入口
  const all = [
    { label: '用户管理', path: '/system/user', icon: 'User', color: '#409EFF', roles: ['SUPER_ADMIN'] },
    { label: '角色管理', path: '/system/role', icon: 'UserFilled', color: '#909399', roles: ['SUPER_ADMIN'] },
    { label: '部门岗位', path: '/system/department', icon: 'OfficeBuilding', color: '#67C23A', roles: ['SUPER_ADMIN'] },
    { label: '通知公告', path: '/system/notice', icon: 'ChatDotSquare', color: '#E6A23C', roles: ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN'] },
    { label: '培训计划', path: '/training/plan', icon: 'Calendar', color: '#F56C6C', roles: ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN'] },
    { label: '培训任务', path: '/training/task', icon: 'List', color: '#409EFF', roles: ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN', 'DEPT_LEADER'] },
    { label: '题库试卷', path: '/exam/paper', icon: 'Document', color: '#67C23A', roles: ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN'] },
    { label: '审计日志', path: '/audit/log', icon: 'Tickets', color: '#909399', roles: ['SUPER_ADMIN', 'QA_MANAGER'] },
    { label: '统计报表', path: '/report/index', icon: 'DataAnalysis', color: '#E6A23C', roles: ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN'] },
    { label: '待办中心', path: '/todo', icon: 'Bell', color: '#F56C6C', roles: ['SUPER_ADMIN', 'QA_MANAGER', 'TRAINING_ADMIN', 'DEPT_LEADER'] }
  ]
  if (isSuper) return all
  return all.filter((e) => e.roles.some((r) => roles.includes(r)))
})

// 统计卡片：先放占位数字，接口返回后回填
const statCards = reactive([
  { title: '培训总人次', value: '-', icon: 'User', color: '#409EFF', footer: '年度累计参训人次' },
  { title: '总培训学时', value: '-', icon: 'Clock', color: '#67C23A', footer: '年度累计学时' },
  { title: '考试通过率', value: '-', icon: 'CircleCheck', color: '#E6A23C', footer: '年度平均' },
  { title: '计划完成率', value: '-', icon: 'Finished', color: '#F56C6C', footer: '年度平均' }
])

// 待办表格展示数据（从 /dashboard/todo 拉取并扁平化）
const todoTableData = ref([])

/**
 * 加载统计汇总（一次返回 overview + 图表数据）
 */
async function loadReport() {
  try {
    const res = await get('/report/summary', { year: '2026' })
    const data = res.data || res || {}

    // 回填概览卡片
    if (data.overview) {
      statCards[0].value = Number(data.overview.totalTrainees ?? 0).toLocaleString()
      statCards[1].value = Number(data.overview.totalHours ?? 0).toLocaleString()
      statCards[2].value = (data.overview.passRate ?? 0).toFixed(1) + '%'
      statCards[3].value = (data.overview.completionRate ?? 0).toFixed(1) + '%'
    }

    // 初始化图表（用接口返回的数据）
    initCharts(data)
  } catch (e) {
    console.error('Dashboard 统计汇总加载失败', e)
    // 接口不可用时仍渲染图表（空数据兜底），避免页面白屏
    initCharts()
  }
}

/**
 * 加载待办事项（聚合 reviewPending / demandPending / planPending 三类）
 */
async function loadTodo() {
  loading.value = true
  try {
    const res = await get('/dashboard/todo')
    const data = res.data || res || {}
    // 把三类待办合并为一张表（按返回顺序：审核 → 需求 → 计划）
    const table = []

    // 待审核任务
    ;(data.reviewPending?.items || []).forEach((item) => {
      table.push({
        title: item.title,
        stageText: item.stage === 'leader' ? '一级审核' : '二级复核',
        tagType: item.stage === 'leader' ? 'warning' : 'primary',
        time: item.time
      })
    })
    // 待处理需求
    ;(data.demandPending?.items || []).forEach((item) => {
      table.push({
        title: item.title,
        stageText: '培训需求',
        tagType: 'info',
        time: item.time
      })
    })
    // 待审批计划
    ;(data.planPending?.items || []).forEach((item) => {
      table.push({
        title: item.title,
        stageText: '培训计划',
        tagType: 'success',
        time: item.time
      })
    })

    // 按时间倒序（新的在前）
    table.sort((a, b) => (b.time || '').localeCompare(a.time || ''))
    todoTableData.value = table.slice(0, 6)
  } catch (e) {
    console.error('Dashboard 待办加载失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 初始化两个图表（无数据时用空数组兜底，保证图表能正常渲染）
 */
function initCharts(data) {
  // 数据提取（兼容接口未返回的情况）
  const monthLabels = data?.monthHours?.labels || []
  const monthValues = data?.monthHours?.values || []
  const deptLabels = data?.deptCompletion?.labels || []
  const deptValues = data?.deptCompletion?.values || []

  // 月度培训学时趋势（折线图）
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['培训学时'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: monthLabels },
    yAxis: { type: 'value', name: '学时' },
    series: [{
      name: '培训学时',
      type: 'line',
      data: monthValues,
      smooth: true,
      areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
      itemStyle: { color: '#409EFF' }
    }]
  })

  // 各部门培训覆盖率（饼图，接口返回的是 deptCompletion 横向条形数据，转成饼图）
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: deptLabels.map((label, i) => ({ name: label, value: deptValues[i] || 0 }))
    }]
  })
}

/** 窗口大小变化时重绘图表 */
function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  // 并行拉取两类数据，互不阻塞
  loadReport()
  loadTodo()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

.stat-row {
  margin-bottom: 0;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-title {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 8px 0;
}

.stat-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
}

.chart-container {
  height: 320px;
  width: 100%;
}

/* 卡片标题右侧的链接按钮 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 快速入口九宫格 */
.quick-entry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 8px 4px;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 18px 8px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafafa;
}

.entry-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.entry-label {
  font-size: 13px;
  color: #606266;
}
</style>