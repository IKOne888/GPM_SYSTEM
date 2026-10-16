<template>
  <div class="help-page">
    <!-- 页头 -->
    <div class="page-head">
      <h2 class="gmp-page-title">帮助中心</h2>
      <p class="gmp-page-subtitle">培训制度要点、学习流程指引、常见问题与技术支持</p>
    </div>

    <div class="help-layout">
      <!-- 左侧：锚点导航 -->
      <div class="anchor-card gmp-card">
        <div class="anchor-title">目录</div>
        <ul class="anchor-list">
          <li
            v-for="item in sections"
            :key="item.id"
            :class="{ 'anchor-item--active': activeSection === item.id }"
            @click="scrollTo(item.id)"
          >
            {{ item.title }}
          </li>
        </ul>
      </div>

      <!-- 右侧：内容区 -->
      <div class="content-area">
        <!-- 一、培训制度要点 -->
        <section id="system" class="content-card gmp-card">
          <div class="card-head">
            <el-icon color="#1677FF"><Document /></el-icon>
            <span class="card-title">一、培训制度要点</span>
          </div>
          <ul class="rule-list">
            <li v-for="(rule, index) in rules" :key="index">
              <span class="rule-index">{{ index + 1 }}</span>
              <span class="rule-text">{{ rule }}</span>
            </li>
          </ul>
        </section>

        <!-- 二、学习流程指引 -->
        <section id="flow" class="content-card gmp-card">
          <div class="card-head">
            <el-icon color="#1677FF"><Guide /></el-icon>
            <span class="card-title">二、学习流程指引</span>
          </div>
          <el-steps :active="5" align-center finish-status="success" class="flow-steps">
            <el-step v-for="step in steps" :key="step.title" :title="step.title" :description="step.desc" />
          </el-steps>
        </section>

        <!-- 三、常见问题 FAQ -->
        <section id="faq" class="content-card gmp-card">
          <div class="card-head">
            <el-icon color="#1677FF"><QuestionFilled /></el-icon>
            <span class="card-title">三、常见问题（FAQ）</span>
          </div>
          <el-collapse v-model="activeFaq">
            <el-collapse-item
              v-for="(faq, index) in faqs"
              :key="index"
              :title="faq.q"
              :name="index"
            >
              <p class="faq-answer">{{ faq.a }}</p>
            </el-collapse-item>
          </el-collapse>
        </section>

        <!-- 四、技术支持 -->
        <section id="contact" class="content-card gmp-card">
          <div class="card-head">
            <el-icon color="#1677FF"><Service /></el-icon>
            <span class="card-title">四、技术支持与联系方式</span>
          </div>
          <div class="contact-grid">
            <div v-for="contact in contacts" :key="contact.label" class="contact-item">
              <el-icon :size="20" color="#1677FF"><component :is="contact.icon" /></el-icon>
              <div class="contact-info">
                <span class="contact-label">{{ contact.label }}</span>
                <span class="contact-value">{{ contact.value }}</span>
              </div>
            </div>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="如遇系统异常，请先尝试刷新页面或重新登录；问题仍然存在时请联系质量管理部门。"
            class="contact-tip"
          />
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 帮助中心页面
 * <p>
 * 纯静态内容页，无需后端接口：包含培训制度要点、学习流程步骤、FAQ 与技术支持联系方式。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

/** 锚点导航配置 */
const sections = [
  { id: 'system', title: '培训制度要点' },
  { id: 'flow', title: '学习流程指引' },
  { id: 'faq', title: '常见问题（FAQ）' },
  { id: 'contact', title: '技术支持' }
]

/** 当前高亮的锚点 */
const activeSection = ref('system')

/** 制度要点（静态文案） */
const rules = [
  '所有员工须按岗位要求完成年度 GMP 培训，未按期完成将影响岗位资质认定。',
  '视频学习须达到 95% 以上进度方可参加该课程的在线考试。',
  '在线考试满分 100 分，达到课程设定的及格分数线方视为通过。',
  '考试未通过可在允许次数内补考；补考仍未通过的，需重新学习课程后再次申请。',
  '考试答错的题目自动进入「错题本」，重做答对后自动移出未掌握列表。',
  '培训合格证书按课程复训周期到期，到期前需完成复训，否则证书失效。'
]

/** 学习流程步骤 */
const steps = [
  { title: '领取任务', desc: '在「我的任务」查看分配或自主报名的课程' },
  { title: '视频学习', desc: '观看视频课件，系统自动记录进度' },
  { title: '在线考试', desc: '学习进度达标后进入考试作答' },
  { title: '查询成绩', desc: '查看成绩单与逐题解析' },
  { title: '获得证书', desc: '通过后生成培训档案与合格证书' }
]

/** 常见问题 */
const faqs = [
  {
    q: '为什么「我的任务」里没有我要学的课程？',
    a: '培训任务由管理员通过培训计划分配，或您在「课程中心」自主报名后生成。若确认应有任务却未显示，请联系培训管理员核实分配情况。'
  },
  {
    q: '视频看到一半中断了，进度会丢失吗？',
    a: '不会。系统每 15 秒、暂停时、离开页面时都会自动上报进度，再次进入可从上次位置继续播放。'
  },
  {
    q: '考试中途退出会怎样？',
    a: '答题内容会保留在考试记录中，倒计时仍按服务端时间继续计算，请在有效时间内返回继续作答或交卷。'
  },
  {
    q: '错题本里的题目会一直保留吗？',
    a: '考试答错的题目会自动收集为「未掌握」；重做答对或手动标记已掌握后，该题会移出未掌握列表，但仍可在「已掌握」中查看。'
  },
  {
    q: '证书的有效期是怎么计算的？',
    a: '证书有效期依据课程的复训周期自动计算。临近到期会显示「临期」状态并推送到期提醒消息，请及时完成复训。'
  }
]

/** 默认展开第一个 FAQ */
const activeFaq = ref([0])

/** 联系方式 */
const contacts = [
  { label: '质量管理部', value: '内线 8001 / qa@gmp-demo.com', icon: 'OfficeBuilding' },
  { label: '培训管理员', value: '内线 8012 / training@gmp-demo.com', icon: 'UserFilled' },
  { label: '系统技术支持', value: '内线 8099 / support@gmp-demo.com', icon: 'Service' }
]

/**
 * 滚动到指定锚点并高亮
 * @param {string} id 区块ID
 */
function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeSection.value = id
  }
}

/** 滚动内容区时同步高亮当前锚点 */
function handleScroll(event) {
  const target = event.target
  // 内容区已滚动到底部时，末尾区块永远无法越过「scrollTop + 阈值」，
  // 因此单独判定：直接高亮最后一个锚点，避免点击「技术支持」后高亮停留在上一项
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 2) {
    activeSection.value = sections[sections.length - 1].id
    return
  }
  const position = target.scrollTop + 120
  sections.forEach((item) => {
    const el = document.getElementById(item.id)
    if (el && el.offsetTop <= position) {
      activeSection.value = item.id
    }
  })
}

onMounted(() => {
  // 内容区滚动监听（内容区为 .app-main，向上查找滚动容器）
  const scroller = document.querySelector('.app-main')
  if (scroller) {
    scroller.addEventListener('scroll', handleScroll)
  }
})

onBeforeUnmount(() => {
  const scroller = document.querySelector('.app-main')
  if (scroller) {
    scroller.removeEventListener('scroll', handleScroll)
  }
})
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}

.help-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 16px;
  align-items: start;
}

/* ---------- 锚点导航 ---------- */
.anchor-card {
  padding: 16px 12px;
  position: sticky;
  top: 0;
}

.anchor-title {
  padding: 0 10px 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

.anchor-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
}

.anchor-list li {
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-regular);
  cursor: pointer;
  transition: all 0.2s;
}

.anchor-list li:hover {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
}

.anchor-item--active {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  font-weight: 600;
}

/* ---------- 内容区 ---------- */
.content-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-card {
  padding: 20px 24px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 制度要点 */
.rule-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rule-list li {
  display: flex;
  gap: 10px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-regular);
}

.rule-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  margin-top: 3px;
}

/* 流程步骤 */
.flow-steps {
  padding: 10px 0;
}

.flow-steps :deep(.el-step__title) {
  font-size: 13px;
}

.flow-steps :deep(.el-step__description) {
  font-size: 12px;
  line-height: 1.6;
}

/* FAQ */
.faq-answer {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-regular);
}

/* 联系方式 */
.contact-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: #f7faff;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.contact-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.contact-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-all;
}

.contact-tip {
  margin-top: 16px;
}

@media (max-width: 1000px) {
  .help-layout {
    grid-template-columns: 1fr;
  }

  .anchor-card {
    position: static;
  }

  .anchor-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .contact-grid {
    grid-template-columns: 1fr;
  }
}
</style>
