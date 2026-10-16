// pages/exam/result.js - 考试成绩与逐题解析
const { get, formatTime, formatDuration } = require('../../utils/request')

Page({
  data: {
    // 考试记录ID
    recordId: null,
    // 成绩单数据（ExamResultVO）
    result: null,
    // 逐题解析列表
    items: [],
    // 是否展开解析（默认展开）
    showAnalysis: true
  },

  onLoad(options) {
    if (options.recordId) {
      this.setData({ recordId: options.recordId })
      this.loadResult(options.recordId)
    }
  },

  onPullDownRefresh() {
    // 下拉刷新成绩
    this.loadResult(this.data.recordId)
    wx.stopPullDownRefresh()
  },

  /** 加载成绩单 */
  loadResult(recordId) {
    get('/study/exams/records/' + recordId + '/result', {}, { loading: false }).then(res => {
      const result = res.data || null
      if (!result) return

      result.submitTimeText = formatTime(result.submitTime)
      result.durationText = formatDuration(result.duration)

      // 逐题解析：补充选项字母标记与答案文本
      const items = (result.items || []).map(item => {
        // 判断题不展示选项列表
        const options = (item.options || []).map((o, i) => ({
          label: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'[i] || '',
          text: o.text,
          // 是否为正确答案
          isAnswer: o.isAnswer === true
        }))
        // 标记考生所选项
        options.forEach(o => {
          o.userSelected = String(item.userAnswer || '').indexOf(o.label) >= 0
        })
        item.options = options
        return item
      })

      this.setData({ result: result, items: items })
    }).catch(() => {})
  },

  /** 切换解析展开/收起 */
  toggleAnalysis() {
    this.setData({ showAnalysis: !this.data.showAnalysis })
  },

  /** 返回任务详情（优先回退历史） */
  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/index/index' })
    }
  }
})