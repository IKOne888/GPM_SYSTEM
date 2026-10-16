// pages/message/list.js - 消息中心（列表 / 标记已读 / 全部已读）
const app = getApp()
const { get, put, formatTime, getMessageTypeText } = require('../../utils/request')

Page({
  data: {
    // 消息列表
    messageList: [],
    // 筛选：'' 全部 | 0 未读 | 1 已读
    readFilter: '',
    // 分页
    page: 1,
    size: 10,
    hasMore: true,
    // 加载中
    loading: false
  },

  onLoad() {
    this.loadMessages()
  },

  onShow() {
    // 回到本页刷新（阅读状态可能变化）
    this.setData({ page: 1, messageList: [], hasMore: true })
    this.loadMessages()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, messageList: [], hasMore: true })
    this.loadMessages()
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMessages()
    }
  },

  /** 加载消息列表 */
  loadMessages() {
    if (this.data.loading) return
    this.setData({ loading: true })

    const params = { page: this.data.page, size: this.data.size }
    // 已读状态筛选（0 未读 / 1 已读）
    if (this.data.readFilter !== '') {
      params.isRead = Number(this.data.readFilter)
    }

    get('/study/messages', params, { loading: false }).then(res => {
      const records = (res.data && res.data.records) || []
      const list = records.map(item => {
        item.typeText = getMessageTypeText(item.messageType)
        item.createTimeText = formatTime(item.createTime)
        return item
      })

      this.setData({
        messageList: this.data.messageList.concat(list),
        hasMore: records.length >= this.data.size,
        page: this.data.page + 1,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 切换筛选 */
  onFilterTap(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      readFilter: value,
      page: 1,
      messageList: [],
      hasMore: true
    })
    this.loadMessages()
  },

  /**
   * 点击消息：标记已读，并按类型跳转
   */
  onMessageTap(e) {
    const index = e.currentTarget.dataset.index
    const item = this.data.messageList[index]
    if (!item) return

    // 未读则标记已读
    if (item.isRead === 0) {
      put('/study/messages/' + item.id + '/read', {}, { loading: false, showError: false }).then(() => {
        // 局部更新状态并同步全局未读数
        this.setData({ ['messageList[' + index + '].isRead']: 1 })
        this.refreshGlobalUnread()
      }).catch(() => {})
    }

    // 任务/考试类消息：根据关联ID跳转任务详情
    if ((item.messageType === 1 || item.messageType === 2 || item.messageType === 3) && item.relatedId) {
      wx.navigateTo({ url: '/pages/task/detail?id=' + item.relatedId })
    }
  },

  /** 全部标记已读 */
  onReadAll() {
    wx.showModal({
      title: '提示',
      content: '确认将全部消息标记为已读吗？',
      success: (res) => {
        if (!res.confirm) return
        put('/study/messages/read-all', {}, { loading: true }).then(() => {
          wx.showToast({ title: '已全部标记为已读', icon: 'success' })
          // 重新加载列表
          this.setData({ page: 1, messageList: [], hasMore: true })
          this.loadMessages()
          app.globalData.unreadCount = 0
        }).catch(() => {})
      }
    })
  },

  /** 刷新全局未读数（供首页角标同步） */
  refreshGlobalUnread() {
    get('/study/messages/unread-count', {}, { loading: false, showError: false }).then(res => {
      app.globalData.unreadCount = Number(res.data) || 0
    }).catch(() => {})
  }
})