// pages/task/list.js - 全部任务列表（搜索 + 状态筛选 + 分页）
const {
  get,
  getTaskStatusText,
  getTaskStatusClass,
  formatTime
} = require('../../utils/request')

Page({
  data: {
    // 任务列表
    taskList: [],
    // 当前状态筛选：'' 表示全部
    statusFilter: '',
    // 状态筛选项（字符串枚举）
    statusOptions: [
      { value: '', label: '全部' },
      { value: 'not_started', label: '待学习' },
      { value: 'studying', label: '学习中' },
      { value: 'waiting_exam', label: '待考试' },
      { value: 'pending_review', label: '审核中' },
      { value: 'rejected', label: '已退回' },
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
    // 搜索关键字
    keyword: ''
  },

  onLoad() {
    this.loadTasks()
  },

  onPullDownRefresh() {
    // 下拉刷新
    this.setData({ page: 1, taskList: [], hasMore: true })
    this.loadTasks()
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    // 触底加载更多
    if (this.data.hasMore && !this.data.loading) {
      this.loadTasks()
    }
  },

  /** 分页加载任务列表 */
  loadTasks() {
    if (this.data.loading) return
    this.setData({ loading: true })

    const params = { page: this.data.page, size: this.data.size }
    // 状态筛选
    if (this.data.statusFilter) {
      params.status = this.data.statusFilter
    }
    // 关键字搜索
    if (this.data.keyword) {
      params.keyword = this.data.keyword
    }

    get('/study/tasks', params, { loading: false }).then(res => {
      const result = res.data || {}
      const records = result.records || []

      const list = records.map(item => {
        item.statusText = getTaskStatusText(item.status)
        item.statusClass = getTaskStatusClass(item.status)
        item.deadlineText = formatTime(item.deadline)
        item.progressPercent = Math.floor(Number(item.videoProgress) || 0)
        return item
      })

      this.setData({
        taskList: this.data.taskList.concat(list),
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
    this.setData({ statusFilter: value, page: 1, taskList: [], hasMore: true })
    this.loadTasks()
  },

  /** 输入搜索关键字 */
  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  /** 触发搜索 */
  onSearch() {
    this.setData({ page: 1, taskList: [], hasMore: true })
    this.loadTasks()
  },

  /** 点击任务卡片进入详情 */
  onTaskTap(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/task/detail?id=' + id })
  }
})