// pages/course/detail.js - 课程详情与自主报名
const { get, post } = require('../../utils/request')

Page({
  data: {
    // 课程ID
    courseId: null,
    // 课程详情（StudyCourseVO）
    course: null,
    // 报名中状态
    enrolling: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ courseId: options.id })
      this.loadDetail(options.id)
    }
  },

  onPullDownRefresh() {
    this.loadDetail(this.data.courseId)
    wx.stopPullDownRefresh()
  },

  /** 加载课程详情 */
  loadDetail(id) {
    get('/study/courses/' + id, {}, { loading: false }).then(res => {
      this.setData({ course: res.data || null })
    }).catch(() => {})
  },

  /** 报名课程 */
  onEnroll() {
    const course = this.data.course
    if (!course) return
    // 已报名：直接进入对应任务
    if (course.enrolled) {
      if (course.taskId) {
        wx.navigateTo({ url: '/pages/task/detail?id=' + course.taskId })
      } else {
        wx.showToast({ title: '已报名该课程', icon: 'none' })
      }
      return
    }
    // 防重复提交
    if (this.data.enrolling) return

    wx.showModal({
      title: '确认报名',
      content: '报名后将为您生成学习任务，确认报名《' + course.name + '》吗？',
      success: (res) => {
        if (!res.confirm) return
        this.setData({ enrolling: true })
        post('/study/courses/' + this.data.courseId + '/enroll', {}, { loading: true }).then(res => {
          const taskId = res.data
          wx.showToast({ title: '报名成功', icon: 'success' })
          this.setData({ enrolling: false })
          // 报名成功后进入任务详情
          setTimeout(() => {
            if (taskId) {
              wx.navigateTo({ url: '/pages/task/detail?id=' + taskId })
            } else {
              this.loadDetail(this.data.courseId)
            }
          }, 800)
        }).catch(() => {
          this.setData({ enrolling: false })
        })
      }
    })
  },

  /** 返回（优先回退历史） */
  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/course/list' })
    }
  }
})