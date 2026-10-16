// pages/index/index.js - 首页（我的任务，tabBar 第 1 项）
const app = getApp()
const {
  get,
  getTaskStatusText,
  getTaskStatusClass,
  formatTime
} = require('../../utils/request')

Page({
  data: {
    // 用户信息（顶部问候语）
    userInfo: {},
    // 未读消息数（顶部铃铛角标）
    unreadCount: 0,
    // 任务列表
    taskList: [],
    // 当前状态筛选：'' 表示全部
    statusFilter: '',
    // 状态筛选项（使用后端字符串枚举口径）
    statusOptions: [
      { value: '', label: '全部' },
      { value: 'not_started', label: '待学习' },
      { value: 'studying', label: '学习中' },
      { value: 'waiting_exam', label: '待考试' },
      { value: 'completed', label: '已完成' },
      { value: 'overdue', label: '已逾期' }
    ],
    // 分页参数
    page: 1,
    size: 10,
    // 是否还有下一页
    hasMore: true,
    // 加载中
    loading: false,
    // 任务统计：待学习/学习中/待考试/已完成
    stats: {
      notStarted: 0,
      studying: 0,
      waitingExam: 0,
      completed: 0,
      total: 0
    }
  },

  onLoad() {
    // 未登录时重定向到登录页
    if (!app.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    // 回填用户信息
    this.setData({ userInfo: app.globalData.userInfo || {} })
    this.refreshData()
  },

  onShow() {
    // 每次回到首页刷新数据（学习/考试后状态会变化）
    if (app.isLoggedIn()) {
      this.setData({ userInfo: app.globalData.userInfo || {} })
      this.refreshData()
    }
  },

  onPullDownRefresh() {
    // 下拉刷新
    this.refreshData()
  },

  onReachBottom() {
    // 触底加载更多
    if (this.data.hasMore && !this.data.loading) {
      this.loadTasks()
    }
  },

  /** 刷新：重置分页后重新加载列表/统计/未读数 */
  refreshData() {
    this.setData({ page: 1, taskList: [], hasMore: true })
    this.loadTasks()
    this.loadStats()
    this.loadUnreadCount()
    wx.stopPullDownRefresh()
  },

  /** 加载任务状态统计 */
  loadStats() {
    get('/study/tasks/stats', {}, { loading: false, showError: false }).then(res => {
      this.setData({ stats: res.data || this.data.stats })
    }).catch(() => {})
  },

  /** 加载未读消息数 */
  loadUnreadCount() {
    get('/study/messages/unread-count', {}, { loading: false, showError: false }).then(res => {
      const count = Number(res.data) || 0
      this.setData({ unreadCount: count })
      app.globalData.unreadCount = count
    }).catch(() => {})
  },

  /** 分页加载任务列表 */
  loadTasks() {
    if (this.data.loading) return
    this.setData({ loading: true })

    const params = {
      page: this.data.page,
      size: this.data.size
    }
    // 仅在非「全部」时传递状态筛选
    if (this.data.statusFilter) {
      params.status = this.data.statusFilter
    }

    get('/study/tasks', params, { loading: false }).then(res => {
      const result = res.data || {}
      const records = result.records || []

      // 逐条补充展示字段（状态文本、样式类、截止时间文本、进度百分比）
      const list = records.map(item => {
        item.statusText = getTaskStatusText(item.status)
        item.statusClass = getTaskStatusClass(item.status)
        item.deadlineText = formatTime(item.deadline)
        item.progressPercent = Math.floor(Number(item.videoProgress) || 0)
        return item
      })

      this.setData({
        taskList: this.data.taskList.concat(list),
        // 返回条数不足一页则没有更多
        hasMore: records.length >= this.data.size,
        page: this.data.page + 1,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 切换状态筛选 */
  onFilterTap(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      statusFilter: value,
      page: 1,
      taskList: [],
      hasMore: true
    })
    this.loadTasks()
  },

  /** 点击任务卡片进入详情 */
  onTaskTap(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/task/detail?id=' + id })
  },

  /** 跳转消息中心 */
  onMessageTap() {
    wx.navigateTo({ url: '/pages/message/list' })
  }
})