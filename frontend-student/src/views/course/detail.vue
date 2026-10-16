<template>
  <div v-loading="loading" class="course-detail">
    <!-- 页头：返回 + 标题 -->
    <div class="page-head">
      <el-button :icon="ArrowLeft" @click="goBack">返回课程中心</el-button>
    </div>

    <template v-if="course.id">
      <!-- 课程信息卡 -->
      <div class="info-card gmp-card">
        <div class="info-main">
          <div class="course-title-row">
            <h2 class="gmp-page-title">{{ course.name }}</h2>
            <el-tag :type="course.enrolled ? 'success' : 'info'" effect="light">
              {{ course.enrolled ? '已报名' : '未报名' }}
            </el-tag>
          </div>

          <div class="course-tags">
            <el-tag v-if="course.categoryName" type="primary" effect="plain" size="small">
              {{ course.categoryName }}
            </el-tag>
            <span v-if="course.code" class="code">课程编码：{{ course.code }}</span>
          </div>

          <div class="meta-grid">
            <div class="meta-item">
              <span class="meta-label">培训学时</span>
              <span class="meta-value">{{ course.trainingHours ?? '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">及格分数</span>
              <span class="meta-value">{{ course.passScore ?? '-' }} 分</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">复训周期</span>
              <span class="meta-value">{{ course.retrainPeriod ? course.retrainPeriod + ' 天' : '不限' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">SOP编号</span>
              <span class="meta-value">{{ course.sopCode || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">SOP版本</span>
              <span class="meta-value">{{ course.sopVersion || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">适用部门</span>
              <span class="meta-value">{{ course.departmentName || '全校通用' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">适用岗位</span>
              <span class="meta-value">{{ course.positionName || '不限' }}</span>
            </div>
          </div>
        </div>

        <!-- 右侧资源统计与主操作 -->
        <div class="info-side">
          <div class="count-row">
            <div class="count-item">
              <div class="count-value gmp-gradient-text">{{ course.videoCount ?? 0 }}</div>
              <div class="count-label">视频课件</div>
            </div>
            <div class="count-item">
              <div class="count-value gmp-gradient-text">{{ course.fileCount ?? 0 }}</div>
              <div class="count-label">培训资料</div>
            </div>
            <div class="count-item">
              <div class="count-value gmp-gradient-text">{{ course.questionCount ?? 0 }}</div>
              <div class="count-label">考试题目</div>
            </div>
          </div>

          <el-button
            v-if="course.enrolled"
            type="primary"
            size="large"
            class="main-btn"
            @click="goTask"
          >
            进入学习
          </el-button>
          <el-button
            v-else
            type="primary"
            size="large"
            class="main-btn"
            :loading="enrolling"
            @click="handleEnroll"
          >
            立即报名
          </el-button>
        </div>
      </div>

      <!-- 培训目标 -->
      <div class="content-card gmp-card">
        <div class="card-head">
          <el-icon color="#1677FF"><Aim /></el-icon>
          <span class="card-title">培训目标</span>
        </div>
        <p class="objective-text">{{ course.objective || '本课程暂未填写培训目标说明。' }}</p>
      </div>

      <!-- 课程资料清单（报名后可下载） -->
      <div class="content-card gmp-card">
        <div class="card-head">
          <el-icon color="#1677FF"><FolderOpened /></el-icon>
          <span class="card-title">课程资料</span>
          <span class="card-sub">共 {{ files.length }} 份</span>
        </div>

        <el-empty v-if="files.length === 0" description="该课程暂无资料" :image-size="80" />
        <div v-else class="file-list">
          <div v-for="file in files" :key="file.id" class="file-item">
            <el-icon class="file-icon"><Document /></el-icon>
            <div class="file-info">
              <span class="file-name">{{ file.name || file.fileName }}</span>
              <span class="file-meta">
                {{ file.fileType || '文件' }}
                <template v-if="file.fileSize"> · {{ formatFileSize(file.fileSize) }}</template>
              </span>
            </div>
            <el-button link type="primary" @click="downloadFile(file)">下载</el-button>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="课程不存在或已下架" />
  </div>
</template>

<script setup>
/**
 * 课程详情页面
 * <p>
 * 数据来源：GET /study/courses/{id}（含资料/视频/题目数量与部门岗位名称）。
 * 资料下载复用 GET /study/courses/files/{fileId}/url 获取 MinIO 预签名地址。
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getCourseDetail, enrollCourse } from '@/api/course'
import { getCourseFiles, getCourseFileUrl } from '@/api/study'
import { formatFileSize } from '@/utils/format'

const route = useRoute()
const router = useRouter()

/** 加载状态 */
const loading = ref(false)
/** 报名中状态 */
const enrolling = ref(false)
/** 课程详情 */
const course = ref({})
/** 课程资料列表 */
const files = ref([])

/**
 * 加载课程详情与资料清单
 */
async function loadDetail() {
  loading.value = true
  try {
    const res = await getCourseDetail(route.params.id)
    course.value = res.data || {}
    // 资料清单失败时不影响详情展示
    try {
      const fileRes = await getCourseFiles(route.params.id)
      files.value = fileRes.data || []
    } catch (e) {
      files.value = []
    }
  } finally {
    loading.value = false
  }
}

/**
 * 报名课程，成功后跳转任务详情开始学习
 */
async function handleEnroll() {
  enrolling.value = true
  try {
    const res = await enrollCourse(route.params.id)
    const taskId = res.data?.taskId
    ElMessage.success('报名成功，已加入「我的任务」')
    if (taskId) {
      router.push(`/task/${taskId}`)
    } else {
      loadDetail()
    }
  } finally {
    enrolling.value = false
  }
}

/**
 * 进入学习（跳转已报名任务详情）
 */
function goTask() {
  if (course.value.taskId) {
    router.push(`/task/${course.value.taskId}`)
  } else {
    ElMessage.info('暂未查询到关联任务，请前往「我的任务」查看')
    router.push('/dashboard')
  }
}

/**
 * 下载资料（获取预签名地址后新窗口打开）
 * @param {object} file 资料对象
 */
async function downloadFile(file) {
  try {
    const res = await getCourseFileUrl(file.id)
    const url = res.data?.url || res.data
    if (url) {
      window.open(url, '_blank')
    } else {
      ElMessage.warning('资料下载地址获取失败')
    }
  } catch (e) {
    ElMessage.error('资料下载失败，请稍后重试')
  }
}

/**
 * 返回课程中心
 * <p>
 * 优先回退一层历史记录：课程中心与课程详情互相跳转，
 * 若此处使用 push 会不断叠加历史，导致返回键可以无限点击、页面无限切换；
 * 仅在无历史可退（如直接刷新进入本页）时才用 replace 跳转。
 */
function goBack() {
  const prev = (window.history.state && window.history.state.back) || ''
  if (prev.startsWith('/courses')) {
    router.back()
  } else {
    router.replace('/courses')
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}

/* ---------- 课程信息卡 ---------- */
.info-card {
  display: flex;
  gap: 28px;
  padding: 22px 24px;
  margin-bottom: 16px;
}

.info-main {
  flex: 1;
  min-width: 0;
}

.course-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.course-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.code {
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px 20px;
  margin-top: 18px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

/* ---------- 右侧统计与操作 ---------- */
.info-side {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  padding-left: 24px;
  border-left: 1px dashed var(--border-color);
}

.count-row {
  display: flex;
  justify-content: space-between;
}

.count-item {
  text-align: center;
  flex: 1;
}

.count-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}

.count-label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

.main-btn {
  width: 100%;
}

/* ---------- 内容卡 ---------- */
.content-card {
  padding: 18px 20px;
  margin-bottom: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-sub {
  font-size: 12px;
  color: var(--text-secondary);
}

.objective-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-regular);
  white-space: pre-wrap;
}

/* ---------- 资料清单 ---------- */
.file-list {
  display: flex;
  flex-direction: column;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 4px;
  border-bottom: 1px dashed var(--border-color);
}

.file-item:last-child {
  border-bottom: none;
}

.file-icon {
  font-size: 20px;
  color: var(--brand-primary);
}

.file-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 1100px) {
  .info-card {
    flex-direction: column;
  }

  .info-side {
    width: 100%;
    padding-left: 0;
    padding-top: 16px;
    border-left: none;
    border-top: 1px dashed var(--border-color);
  }

  .meta-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
