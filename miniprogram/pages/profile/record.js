// pages/profile/record.js - 学习记录 + 成绩单（Tab 切换）
const {
  get,
  formatTime,
  formatDuration,
  formatStudyDuration
} = require('../../utils/request')

Page({
  data: {
    // 当前 Tab：study 学习记录 | exam 成绩单
    activeTab: 'study',
    // 学习记录
    studyList: [],
    studyPage: 1,
    studyHasMore: true,
    // 成绩单
    examList: [],
    examPage: 1,
    examHasMore: true,
    // 加载中
    loading: false
  },

  onLoad() {
    this.loadStudyRecords()
  },

  onPullDownRefresh() {
    // 下拉刷新当前 Tab
    if (this.data.activeTab === 'study') {
      this.setData({ studyPage: 1, studyList: [], studyHasMore: true })
      this.loadStudyRecords()
    } else {
      this.setData({ examPage: 1, examList: [], examHasMore: true })
      this.loadExamRecords()
    }
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    // 触底加载更多
    if (this.data.loading) return
    if (this.data.activeTab === 'study' && this.data.studyHasMore) {
      this.loadStudyRecords()
    } else if (this.data.activeTab === 'exam' && this.data.examHasMore) {
      this.loadExamRecords()
    }
  },

  /** 切换 Tab */
  onTabTap(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    // 首次切换到成绩单时加载
    if (tab === 'exam' && this.data.examList.length === 0) {
      this.loadExamRecords()
    }
  },

  /** 加载学习记录 */
  loadStudyRecords() {
    if (this.data.loading) return
    this.setData({ loading: true })

    get('/study/records/study', {
      page: this.data.studyPage,
      size: 10
    }, { loading: false }).then(res => {
      const records = (res.data && res.data.records) || []
      const list = records.map(item => {
        item.videoProgressPercent = Math.floor(Number(item.videoProgress) || 0)
        item.studyDurationText = formatStudyDuration(item.studyDuration)
        item.lastStudyTimeText = formatTime(item.lastStudyTime)
        return item
      })

      this.setData({
        studyList: this.data.studyList.concat(list),
        studyHasMore: records.length >= 10,
        studyPage: this.data.studyPage + 1,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 加载成绩单 */
  loadExamRecords() {
    if (this.data.loading) return
    this.setData({ loading: true })

    get('/study/records/exam', {
      page: this.data.examPage,
      size: 10
    }, { loading: false }).then(res => {
      const records = (res.data && res.data.records) || []
      const list = records.map(item => {
        item.submitTimeText = formatTime(item.submitTime)
        item.durationText = formatDuration(item.duration)
        return item
      })

      this.setData({
        examList: this.data.examList.concat(list),
        examHasMore: records.length >= 10,
        examPage: this.data.examPage + 1,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 点击成绩单查看成绩详情 */
  onExamTap(e) {
    const recordId = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/exam/result?recordId=' + recordId })
  },

  /** 返回（优先回退历史） */
  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/profile/index' })
    }
  }
})