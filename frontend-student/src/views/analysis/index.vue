<template>
  <div class="analysis-page">
    <!-- 页头：标题 + 刷新 -->
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">学习看板</h2>
        <p class="gmp-page-subtitle">汇总个人学习时长、任务完成情况与考试成绩趋势</p>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <div v-loading="loading">
      <!-- ---------- 指标卡：学习时长 / 任务完成 / 证书 / 平均成绩 ---------- -->
      <div class="stat-grid">
        <div
          v-for="card in statCards"
          :key="card.label"
          class="stat-card gmp-card gmp-card--hoverable"
        >
          <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" :style="{ color: card.color }">
              {{ card.value }}<small v-if="card.unit">{{ card.unit }}</small>
            </div>
            <div class="stat-label">{{ card.label }}</div>
            <div v-if="card.extra" class="stat-extra">{{ card.extra }}</div>
          </div>
        </div>
      </div>

      <!-- ---------- 图表区：月度趋势 / 任务状态分布 / 成绩分布 ---------- -->
      <div class="chart-grid">
        <!-- 月度趋势折线图 -->
        <div class="chart-card gmp-card">
          <div class="chart-head">
            <span class="chart-title">近 6 个月学习趋势</span>
            <span class="chart-sub">单位：小时 / 次</span>
          </div>
          <div v-if="hasMonthly" ref="monthlyRef" class="chart-body"></div>
          <el-empty v-else description="暂无学习趋势数据" :image-size="90" />
        </div>

        <!-- 任务状态环形图 -->
        <div class="chart-card gmp-card">
          <div class="chart-head">
            <span class="chart-title">任务状态分布</span>
            <span class="chart-sub">共 {{ overview.totalTask }} 项任务</span>
          </div>
          <div v-if="hasStatus" ref="statusRef" class="chart-body"></div>
          <el-empty v-else description="暂无任务数据" :image-size="90" />
        </div>

        <!-- 成绩分布柱状图 -->
        <div class="chart-card gmp-card chart-card--wide">
          <div class="chart-head">
            <span class="chart-title">考试成绩分布</span>
            <span class="chart-sub">按分数区间统计考试次数</span>
          </div>
          <div v-if="hasScore" ref="scoreRef" class="chart-body"></div>
          <el-empty v-else description="暂无考试成绩数据" :image-size="90" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 学习看板页面
 * <p>
 * 数据来源：GET /study/analysis/overview（一次性返回指标卡 + 3 张图表数据）。
 * 图表使用 ECharts 按需引入（echarts/core + 折线/饼图/柱状 + Canvas 渲染器），避免整包体积膨胀。
 */
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
// ECharts 核心与按需组件
import * as echarts from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { Refresh } from '@element-plus/icons-vue'
import { getAnalysisOverview } from '@/api/analysis'
import { getTaskStatus } from '@/utils/status'
import { formatDuration } from '@/utils/format'

// 按需注册图表与组件（只打包用到的能力）
echarts.use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

/** 加载状态 */
const loading = ref(false)

/** 看板聚合数据（默认空结构，避免模板取值报错） */
const overview = ref({
  totalStudyDuration: 0,
  totalTask: 0,
  completedTask: 0,
  studyingTask: 0,
  overdueTask: 0,
  certificateCount: 0,
  avgScore: 0,
  passRate: 0,
  monthly: [],
  statusDist: [],
  scoreDist: []
})

/** 三个图表容器的 DOM 引用 */
const monthlyRef = ref(null)
const statusRef = ref(null)
const scoreRef = ref(null)

/** 三个 ECharts 实例（用于 resize 与 dispose） */
let monthlyChart = null
let statusChart = null
let scoreChart = null

/** 容器尺寸监听器（布局变化/窗口缩放时同步图表尺寸） */
let resizeObserver = null

/** 是否存在月度趋势数据（决定渲染图表还是空占位） */
const hasMonthly = computed(() => (overview.value.monthly || []).length > 0)
/** 是否存在任务状态数据 */
const hasStatus = computed(() => (overview.value.statusDist || []).some((item) => Number(item.count) > 0))
/** 是否存在成绩分布数据 */
const hasScore = computed(() => (overview.value.scoreDist || []).some((item) => Number(item.count) > 0))

/** 4 个指标卡配置 */
const statCards = computed(() => {
  const data = overview.value
  return [
    {
      label: '累计学习时长',
      value: formatDuration(data.totalStudyDuration),
      extra: `已完成 ${data.completedTask} / ${data.totalTask} 项任务`,
      color: '#1677FF',
      bg: '#E8F1FF',
      icon: 'Timer'
    },
    {
      label: '任务完成情况',
      value: data.completedTask,
      unit: ` / ${data.totalTask}`,
      extra: data.overdueTask > 0 ? `逾期 ${data.overdueTask} 项` : '暂无逾期任务',
      color: '#13C2C2',
      bg: '#E6FFFB',
      icon: 'CircleCheck'
    },
    {
      label: '已获证书',
      value: data.certificateCount,
      unit: ' 本',
      extra: '培训档案有效证书',
      color: '#52C41A',
      bg: '#F0FAE8',
      icon: 'Medal'
    },
    {
      label: '考试平均成绩',
      value: data.avgScore,
      unit: ' 分',
      extra: `通过率 ${data.passRate}%`,
      color: '#FAAD14',
      bg: '#FFF7E6',
      icon: 'Trophy'
    }
  ]
})

/**
 * 渲染月度趋势折线图
 * 学习时长由秒换算为小时（保留 1 位小数）
 */
function renderMonthly() {
  if (!monthlyRef.value) return
  if (!monthlyChart) {
    monthlyChart = echarts.init(monthlyRef.value)
  }
  const monthly = overview.value.monthly || []
  monthlyChart.setOption({
    grid: { left: 44, right: 24, top: 36, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['学习时长(小时)', '考试次数', '通过次数'], top: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 8 },
    xAxis: {
      type: 'category',
      data: monthly.map((item) => item.month),
      axisLine: { lineStyle: { color: '#E4EAF2' } },
      axisLabel: { color: '#8A94A6' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#8A94A6' },
      splitLine: { lineStyle: { color: '#F0F3F8' } }
    },
    series: [
      {
        name: '学习时长(小时)',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: monthly.map((item) => Number(((item.studyDuration || 0) / 3600).toFixed(1))),
        itemStyle: { color: '#1677FF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(22,119,255,0.28)' },
            { offset: 1, color: 'rgba(22,119,255,0.02)' }
          ])
        }
      },
      {
        name: '考试次数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: monthly.map((item) => Number(item.examCount) || 0),
        itemStyle: { color: '#FAAD14' }
      },
      {
        name: '通过次数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: monthly.map((item) => Number(item.passCount) || 0),
        itemStyle: { color: '#52C41A' }
      }
    ]
  })
}

/**
 * 渲染任务状态环形图
 * 状态文案与颜色复用全局状态字典，保证与「我的任务」一致
 */
function renderStatus() {
  if (!statusRef.value) return
  if (!statusChart) {
    statusChart = echarts.init(statusRef.value)
  }
  const dist = (overview.value.statusDist || []).filter((item) => Number(item.count) > 0)
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 项（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 10, itemHeight: 10 },
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{c}', color: '#5A6B7C', fontSize: 12 },
        data: dist.map((item) => {
          const status = getTaskStatus(item.status)
          return { name: status.label, value: Number(item.count) || 0, itemStyle: { color: status.color } }
        })
      }
    ]
  })
}

/**
 * 渲染成绩分布柱状图
 */
function renderScore() {
  if (!scoreRef.value) return
  if (!scoreChart) {
    scoreChart = echarts.init(scoreRef.value)
  }
  const dist = overview.value.scoreDist || []
  scoreChart.setOption({
    grid: { left: 44, right: 24, top: 24, bottom: 30 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: dist.map((item) => item.range),
      axisLine: { lineStyle: { color: '#E4EAF2' } },
      axisLabel: { color: '#8A94A6' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#8A94A6' },
      splitLine: { lineStyle: { color: '#F0F3F8' } }
    },
    series: [
      {
        type: 'bar',
        barWidth: 34,
        data: dist.map((item) => Number(item.count) || 0),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#1677FF' },
            { offset: 1, color: '#13C2C2' }
          ])
        },
        label: { show: true, position: 'top', color: '#5A6B7C' }
      }
    ]
  })
}

/**
 * 窗口尺寸变化时重绘图表（节流由浏览器 resize 事件天然合并）
 */
function handleResize() {
  monthlyChart?.resize()
  statusChart?.resize()
  scoreChart?.resize()
}

/**
 * 初始化图表并监听容器尺寸变化
 * <p>
 * 页面刚渲染时栅格列宽可能尚未稳定（尤其配合路由过渡动画），
 * 若此时初始化 ECharts 会按错误宽度绘制，故统一用 ResizeObserver 兜底校正尺寸。
 * @param {HTMLElement} el 图表容器
 * @param {object} chart 已创建的 ECharts 实例
 * @param {Function} render 重绘函数
 */
function bindChart(el, chart, render) {
  if (!el || !chart || !resizeObserver) return
  resizeObserver.observe(el)
  // 立即校正一次尺寸，避免首帧按未稳定的宽度绘制
  chart.resize()
  render()
}

/**
 * 加载看板数据并渲染图表
 */
async function loadData() {
  loading.value = true
  try {
    const res = await getAnalysisOverview()
    overview.value = { ...overview.value, ...(res.data || {}) }
    // 等待 DOM 更新（v-if 切换）后再初始化图表
    await nextTick()
    if (hasMonthly.value) {
      if (!monthlyChart) monthlyChart = echarts.init(monthlyRef.value)
      bindChart(monthlyRef.value, monthlyChart, renderMonthly)
    }
    if (hasStatus.value) {
      if (!statusChart) statusChart = echarts.init(statusRef.value)
      bindChart(statusRef.value, statusChart, renderStatus)
    }
    if (hasScore.value) {
      if (!scoreChart) scoreChart = echarts.init(scoreRef.value)
      bindChart(scoreRef.value, scoreChart, renderScore)
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 监听各图表容器尺寸变化，自动重绘（布局变化 + 窗口缩放均可覆盖）
  resizeObserver = new ResizeObserver(() => handleResize())
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  resizeObserver?.disconnect()
  resizeObserver = null
  // 释放图表实例，避免内存泄漏
  monthlyChart?.dispose()
  statusChart?.dispose()
  scoreChart?.dispose()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

/* ---------- 指标卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.15;
}

.stat-value small {
  font-size: 14px;
  font-weight: 600;
  margin-left: 2px;
}

.stat-label {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-regular);
}

.stat-extra {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* ---------- 图表区 ---------- */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.chart-card {
  padding: 16px 18px 8px;
}

.chart-card--wide {
  grid-column: 1 / -1;
}

.chart-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 6px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-sub {
  font-size: 12px;
  color: var(--text-secondary);
}

.chart-body {
  width: 100%;
  height: 260px;
}

@media (max-width: 1180px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
