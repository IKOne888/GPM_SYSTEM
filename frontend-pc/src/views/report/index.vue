<template>
  <div class="page-container">
    <!-- 筛选条件 -->
    <el-card shadow="never">
      <el-form :model="filterForm" inline>
        <el-form-item label="统计年度">
          <el-date-picker v-model="filterForm.year" type="year" placeholder="选择年度" format="YYYY" value-format="YYYY" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="filterForm.departmentId" placeholder="全部部门" clearable>
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReportData">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleExport">
            <el-icon><Download /></el-icon> 导出报表
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计概览 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in overviewCards" :key="item.title">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-content">
            <div>
              <p class="overview-title">{{ item.title }}</p>
              <p class="overview-value">{{ item.value }}</p>
            </div>
            <el-icon :size="40" :style="{ color: item.color }">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <div class="overview-footer">
            <span :style="{ color: item.trend > 0 ? '#67C23A' : '#F56C6C' }">
              {{ item.trend > 0 ? '+' : '' }}{{ item.trend }}%
            </span>
            <span style="color: #999; margin-left: 8px">较上期</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>各部门培训完成率</span></template>
          <div ref="deptChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>月度培训学时统计</span></template>
          <div ref="monthChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>考试成绩分布</span></template>
          <div ref="scoreChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>培训课程分类占比</span></template>
          <div ref="categoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细数据表 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header><span>部门培训统计明细</span></template>
      <el-table :data="detailTableData" border stripe>
        <el-table-column prop="departmentName" label="部门" width="120" />
        <el-table-column prop="totalEmployees" label="总人数" width="90" align="center" />
        <el-table-column prop="trainedCount" label="参训人数" width="90" align="center" />
        <el-table-column prop="trainingRate" label="培训覆盖率" width="120" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.trainingRate" :stroke-width="8" style="width: 80px" />
          </template>
        </el-table-column>
        <el-table-column prop="totalHours" label="总学时" width="90" align="center" />
        <el-table-column prop="avgHours" label="人均学时" width="90" align="center" />
        <el-table-column prop="examPassRate" label="考试通过率" width="120" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.examPassRate" :stroke-width="8" style="width: 80px" />
          </template>
        </el-table-column>
        <el-table-column prop="planCompletionRate" label="计划完成率" width="120" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.planCompletionRate" :stroke-width="8" style="width: 80px" />
          </template>
        </el-table-column>
        <el-table-column prop="overdueTasks" label="逾期任务" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.overdueTasks > 0" type="danger" size="small">{{ row.overdueTasks }}</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { get } from '@/utils/request'

const deptChartRef = ref(null)
const monthChartRef = ref(null)
const scoreChartRef = ref(null)
const categoryChartRef = ref(null)
let charts = []

const departments = ref([])
const filterForm = reactive({ year: '2026', departmentId: '' })

const overviewCards = reactive([
  { title: '培训总人次', value: '1,286', icon: 'User', color: '#409EFF', trend: 12.5 },
  { title: '总培训学时', value: '3,840', icon: 'Clock', color: '#67C23A', trend: 8.3 },
  { title: '考试通过率', value: '94.5%', icon: 'CircleCheck', color: '#E6A23C', trend: 2.1 },
  { title: '计划完成率', value: '87.2%', icon: 'Finished', color: '#F56C6C', trend: -3.4 }
])

const detailTableData = ref([])

async function loadDepartments() {
  try {
    const res = await get('/system/departments/list')
    departments.value = res.data || res || []
  } catch (e) { console.error(e) }
}

async function loadReportData() {
  try {
    const params = { year: filterForm.year, departmentId: filterForm.departmentId }
    const res = await get('/report/summary', params)
    const data = res.data || res || {}

    // 更新概览卡片（用 ?? 防止 0 值被 || 回退覆盖）
    if (data.overview) {
      overviewCards[0].value = data.overview.totalTrainees ?? '1,286'
      overviewCards[1].value = data.overview.totalHours ?? '3,840'
      overviewCards[2].value = (data.overview.passRate ?? '94.5') + '%'
      overviewCards[3].value = (data.overview.completionRate ?? '87.2') + '%'
    }

    // 更新明细表（后端无数据时使用演示数据兜底）
    detailTableData.value = data.departmentStats && data.departmentStats.length ? data.departmentStats : [
      { departmentName: '质量部', totalEmployees: 45, trainedCount: 42, trainingRate: 93, totalHours: 680, avgHours: 15.1, examPassRate: 96, planCompletionRate: 92, overdueTasks: 1 },
      { departmentName: '生产部', totalEmployees: 120, trainedCount: 108, trainingRate: 90, totalHours: 1560, avgHours: 13.0, examPassRate: 92, planCompletionRate: 85, overdueTasks: 3 },
      { departmentName: '仓储部', totalEmployees: 35, trainedCount: 33, trainingRate: 94, totalHours: 420, avgHours: 12.0, examPassRate: 95, planCompletionRate: 90, overdueTasks: 0 },
      { departmentName: '设备部', totalEmployees: 28, trainedCount: 22, trainingRate: 78, totalHours: 340, avgHours: 12.1, examPassRate: 89, planCompletionRate: 78, overdueTasks: 4 },
      { departmentName: '行政部', totalEmployees: 20, trainedCount: 18, trainingRate: 90, totalHours: 240, avgHours: 12.0, examPassRate: 97, planCompletionRate: 88, overdueTasks: 0 }
    ]

    initCharts(data)
  } catch (e) {
    console.error(e)
    initCharts()
  }
}

function initCharts(data) {
  charts.forEach(c => c.dispose())
  charts = []

  // 从后端数据中提取图表所需数据（无数据时使用空数组兜底）
  const deptLabels = data?.deptCompletion?.labels || []
  const deptValues = data?.deptCompletion?.values || []
  const monthLabels = data?.monthHours?.labels || []
  const monthValues = data?.monthHours?.values || []
  const scoreData = data?.scoreDistribution || []
  const categoryData = data?.categoryDistribution || []

  // 部门培训完成率
  const deptChart = echarts.init(deptChartRef.value)
  charts.push(deptChart)
  deptChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: deptLabels },
    series: [{
      type: 'bar',
      data: deptValues,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#83bff6' },
          { offset: 1, color: '#188df0' }
        ]),
        borderRadius: [0, 4, 4, 0]
      },
      barWidth: 20,
      label: { show: true, position: 'right', formatter: '{c}%' }
    }]
  })

  // 月度培训学时
  const monthChart = echarts.init(monthChartRef.value)
  charts.push(monthChart)
  monthChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: monthLabels },
    yAxis: { type: 'value', name: '学时' },
    series: [{
      type: 'line',
      data: monthValues,
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      },
      itemStyle: { color: '#409EFF' }
    }]
  })

  // 考试成绩分布
  const scoreChart = echarts.init(scoreChartRef.value)
  charts.push(scoreChart)
  scoreChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      data: scoreData.length ? scoreData.map((item, index) => ({
        ...item,
        itemStyle: { color: ['#F56C6C', '#E6A23C', '#409EFF', '#67C23A'][index] }
      })) : [
        { value: 0, name: '60分以下', itemStyle: { color: '#F56C6C' } },
        { value: 0, name: '60-79分', itemStyle: { color: '#E6A23C' } },
        { value: 0, name: '80-89分', itemStyle: { color: '#409EFF' } },
        { value: 0, name: '90-100分', itemStyle: { color: '#67C23A' } }
      ]
    }]
  })

  // 课程分类占比
  const categoryChart = echarts.init(categoryChartRef.value)
  charts.push(categoryChart)
  categoryChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['50%', '45%'],
      roseType: 'area',
      itemStyle: { borderRadius: 6 },
      label: { show: true, formatter: '{b}\n{c}门' },
      data: categoryData
    }]
  })
}

function handleResize() {
  charts.forEach(c => c.resize())
}

function handleExport() {
  const link = document.createElement('a')
  const params = new URLSearchParams()
  if (filterForm.year) params.append('year', filterForm.year)
  if (filterForm.departmentId) params.append('departmentId', filterForm.departmentId)
  link.href = '/api/report/export' + (params.toString() ? '?' + params.toString() : '')
  link.download = '培训统计报表.xlsx'
  link.click()
}

onMounted(() => {
  loadDepartments()
  loadReportData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.overview-card:hover {
  transform: translateY(-4px);
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-title {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.overview-value {
  font-size: 26px;
  font-weight: 700;
  color: #333;
  margin: 8px 0;
}

.overview-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
}

.chart-container {
  height: 300px;
  width: 100%;
}
</style>
