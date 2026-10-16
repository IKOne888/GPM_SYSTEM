<template>
  <div v-loading="loading" class="cert-page">
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">培训档案</h2>
        <p class="gmp-page-subtitle">
          按课程再培训周期自动计算证书有效期，临期（≤30 天）请及时参加复训
        </p>
      </div>
    </div>

    <!-- 汇总卡 -->
    <div class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card gmp-card">
        <div class="summary-icon" :style="{ background: item.bg, color: item.color }">
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <div class="summary-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="summary-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 证书卡片列表 -->
    <el-empty v-if="!loading && certificates.length === 0" description="暂无培训档案，完成课程学习并通过考试后将自动生成" />

    <div v-else class="cert-grid">
      <div
        v-for="cert in certificates"
        :key="cert.courseId"
        class="cert-card gmp-card"
        :class="`cert-card--${cert.status}`"
      >
        <!-- 证书头部 -->
        <div class="cert-head">
          <div class="cert-badge">
            <el-icon :size="22"><Medal /></el-icon>
          </div>
          <div class="cert-title-wrap">
            <h3 class="cert-title">{{ cert.courseName }}</h3>
            <p class="cert-code">{{ cert.courseCode || '未设置课程编码' }}</p>
          </div>
          <el-tag :type="getCertStatus(cert.status).type" effect="light" size="small">
            {{ getCertStatus(cert.status).label }}
          </el-tag>
        </div>

        <!-- 证书信息 -->
        <div class="cert-body">
          <div class="cert-row">
            <span class="cert-label">SOP 文件</span>
            <span class="cert-value">
              {{ cert.sopCode || '-' }} <template v-if="cert.sopVersion">/ {{ cert.sopVersion }}</template>
            </span>
          </div>
          <div class="cert-row">
            <span class="cert-label">培训学时</span>
            <span class="cert-value">{{ cert.trainingHours || '-' }} 学时</span>
          </div>
          <div class="cert-row">
            <span class="cert-label">再培训周期</span>
            <span class="cert-value">{{ cert.retrainPeriod ? cert.retrainPeriod + ' 天' : '长期有效' }}</span>
          </div>
          <div class="cert-row">
            <span class="cert-label">通过时间</span>
            <span class="cert-value">{{ formatDateTime(cert.lastPassTime) }}</span>
          </div>
          <div class="cert-row">
            <span class="cert-label">通过成绩</span>
            <span class="cert-value" style="color: #52C41A; font-weight: 600">
              {{ cert.examScore !== null && cert.examScore !== undefined ? cert.examScore + ' 分' : '-' }}
            </span>
          </div>
          <div class="cert-row">
            <span class="cert-label">有效期至</span>
            <span class="cert-value">{{ formatDateTime(cert.validUntil) }}</span>
          </div>
        </div>

        <!-- 有效期进度 -->
        <div class="cert-footer">
          <div class="validity-head">
            <span>证书有效期</span>
            <span :style="{ color: remainColor(cert) }">{{ remainText(cert) }}</span>
          </div>
          <el-progress
            :percentage="validityPercent(cert)"
            :stroke-width="8"
            :color="remainColor(cert)"
            :show-text="false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCertificates } from '@/api/profile'
import { getCertStatus } from '@/utils/status'
import { formatDateTime } from '@/utils/format'

/** 加载状态 */
const loading = ref(false)
/** 证书列表 */
const certificates = ref([])

/** 汇总统计卡 */
const summaryCards = computed(() => {
  const list = certificates.value
  return [
    {
      label: '有效证书',
      value: list.filter((c) => c.status === 'valid').length,
      color: '#52C41A',
      bg: '#F0FAE8',
      icon: 'CircleCheckFilled'
    },
    {
      label: '临期证书',
      value: list.filter((c) => c.status === 'expiring').length,
      color: '#FAAD14',
      bg: '#FFF7E6',
      icon: 'WarningFilled'
    },
    {
      label: '已过期',
      value: list.filter((c) => c.status === 'expired').length,
      color: '#FF4D4F',
      bg: '#FFF1F0',
      icon: 'CircleCloseFilled'
    },
    {
      label: '档案总数',
      value: list.length,
      color: '#1677FF',
      bg: '#E8F1FF',
      icon: 'Medal'
    }
  ]
})

/**
 * 剩余天数文案
 * @param {object} cert 证书对象
 * @returns {string} 文案
 */
function remainText(cert) {
  if (cert.status === 'not_pass') return '尚未通过考试'
  if (cert.remainDays === null || cert.remainDays === undefined) return '长期有效'
  if (cert.remainDays < 0) return `已过期 ${Math.abs(cert.remainDays)} 天`
  return `剩余 ${cert.remainDays} 天`
}

/**
 * 剩余天数配色
 * @param {object} cert 证书对象
 * @returns {string} 颜色值
 */
function remainColor(cert) {
  if (cert.status === 'expired') return '#FF4D4F'
  if (cert.status === 'expiring') return '#FAAD14'
  if (cert.status === 'not_pass') return '#8C9AAE'
  return '#52C41A'
}

/**
 * 有效期剩余百分比（剩余天数 / 再培训周期）
 * @param {object} cert 证书对象
 * @returns {number} 0~100 的百分比
 */
function validityPercent(cert) {
  if (!cert.retrainPeriod || cert.remainDays === null || cert.remainDays === undefined) {
    return 100
  }
  const used = cert.retrainPeriod - cert.remainDays
  const percent = (used / cert.retrainPeriod) * 100
  return Math.min(100, Math.max(0, Math.round(percent)))
}

/**
 * 加载培训档案
 */
async function loadCertificates() {
  loading.value = true
  try {
    const res = await getCertificates()
    certificates.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadCertificates)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}

/* ---------- 汇总 ---------- */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  flex-shrink: 0;
}

.summary-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
}

.summary-label {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---------- 证书卡片 ---------- */
.cert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.cert-card {
  padding: 20px;
  border-left: 4px solid var(--success);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.cert-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.cert-card--expiring {
  border-left-color: var(--warning);
}

.cert-card--expired {
  border-left-color: var(--danger);
}

.cert-card--not_pass {
  border-left-color: var(--info);
}

.cert-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cert-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  color: #fff;
  background: var(--brand-gradient);
  flex-shrink: 0;
}

.cert-title-wrap {
  flex: 1;
  min-width: 0;
}

.cert-title {
  margin: 0;
  font-size: 15px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cert-code {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.cert-body {
  margin: 16px 0 14px;
  padding: 14px 0;
  border-top: 1px dashed var(--border-color);
  border-bottom: 1px dashed var(--border-color);
}

.cert-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
}

.cert-label {
  color: var(--text-secondary);
}

.cert-value {
  color: var(--text-primary);
  font-weight: 500;
}

.cert-footer {
  margin-top: 4px;
}

.validity-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
