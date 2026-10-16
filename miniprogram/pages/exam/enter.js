// pages/exam/enter.js - 考试须知（展示试卷参数与剩余次数，进入答题）
const { get, post } = require('../../utils/request')

Page({
  data: {
    // 任务ID
    taskId: null,
    // 考试信息（ExamInfoVO）
    examInfo: null,
    // 开始考试中（防重复点击）
    starting: false
  },

  onLoad(options) {
    if (options.taskId) {
      this.setData({ taskId: options.taskId })
      this.loadExamInfo(options.taskId)
    }
  },

  onPullDownRefresh() {
    // 下拉刷新考试信息
    this.loadExamInfo(this.data.taskId)
    wx.stopPullDownRefresh()
  },

  /** 加载考试信息 */
  loadExamInfo(taskId) {
    get('/study/tasks/' + taskId + '/exam', {}, { loading: false }).then(res => {
      this.setData({ examInfo: res.data || null })
    }).catch(() => {})
  },

  /** 开始考试 */
  onStartExam() {
    const info = this.data.examInfo
    // 未配置试卷
    if (!info) {
      wx.showToast({ title: '该任务未配置试卷', icon: 'none' })
      return
    }
    // 不可考试：提示原因
    if (info.canExam === false) {
      wx.showToast({ title: info.reason || '当前不可参加考试', icon: 'none' })
      return
    }
    // 防重复提交
    if (this.data.starting) return
    this.setData({ starting: true })

    // 调用开始考试接口获取考试记录ID
    post('/study/exams/' + this.data.taskId + '/start', {}, { loading: true, showError: true }).then(res => {
      const data = res.data || {}
      if (!data.recordId) {
        wx.showToast({ title: '开始考试失败', icon: 'none' })
        this.setData({ starting: false })
        return
      }
      // 进入答题页
      wx.redirectTo({
        url: '/pages/exam/test?recordId=' + data.recordId + '&taskId=' + this.data.taskId
      })
    }).catch(() => {
      this.setData({ starting: false })
    })
  },

  /** 返回上一页（优先回退历史，避免路由栈膨胀） */
  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/index/index' })
    }
  }
})