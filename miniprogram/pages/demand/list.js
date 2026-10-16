// pages/demand/list.js - 培训需求申报（我的申报列表）
//
// 对应后端接口（学员端 /study/** 前缀）：
//   GET  /study/demands?page=&size=&status=  我的需求分页（仅本人提交，服务端强制过滤）
//   GET  /study/demands/{id}                 我的需求详情（非本人返回 403）
// 数据隔离由后端 Service 层通过登录态强制实现，前端无需传 userId。
const app = getApp()
const { get, formatTime } = require('../../utils/request')

/** 需求状态字典：0-待处理 1-已采纳 2-已拒绝 */
const STATUS_MAP = {
  0: { text: '待处理', cls: 'tag-pending' },
  1: { text: '已采纳', cls: 'tag-done' },
  2: { text: '已拒绝', cls: 'tag-overdue' }
}

Page({
  data: {
    // 我的申报列表
    demandList: [],
    // 状态筛选：'' 全部 | 0 待处理 | 1 已采纳 | 2 已拒绝
    statusFilter: '',
    // 各状态统计数量（用于顶部统计条）
    stat: { pending: 0, accepted: 0, rejected: 0 },
    // 分页
    page: 1,
    size: 10,
    hasMore: true,
    // 加载中
    loading: false,
    // 详情弹窗
    showDetailModal: false,
    detail: {}
  },

  onLoad() {
    // 未登录重定向
    if (!app.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.loadDemands()
    this.loadStat()
  },

  onShow() {
    // 从提交页返回时刷新列表与统计
    this.setData({ page: 1, demandList: [], hasMore: true })
    this.loadDemands()
    this.loadStat()
  },

  onPullDownRefresh() {
    // 下拉刷新：重置分页并重新加载
    this.setData({ page: 1, demandList: [], hasMore: true })
    this.loadDemands()
    this.loadStat()
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    // 触底加载下一页
    if (this.data.hasMore && !this.data.loading) {
      this.loadDemands()
    }
  },

  /** 加载我的申报列表 */
  loadDemands() {
    if (this.data.loading) return
    this.setData({ loading: true })

    // 组装查询参数：状态为空时不传 status，避免后端误解为筛选待处理
    const params = { page: this.data.page, size: this.data.size }
    if (this.data.statusFilter !== '') {
      params.status = Number(this.data.statusFilter)
    }

    get('/study/demands', params, { loading: false }).then(res => {
      // 后端返回 MyBatis-Plus 分页对象：{ records, total, ... }
      const records = (res.data && res.data.records) || []
      // 逐条补充状态文案与样式类、格式化时间
      const list = records.map(item => {
        const statusMeta = STATUS_MAP[item.status] || { text: '未知', cls: '' }
        item.statusText = statusMeta.text
        item.statusClass = statusMeta.cls
        item.createTimeText = formatTime(item.createTime)
        return item
      })

      this.setData({
        // 追加到已有列表（分页累加）
        demandList: this.data.demandList.concat(list),
        hasMore: records.length >= this.data.size,
        page: this.data.page + 1,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 加载各状态统计数量（分别以 size=1 请求只读 total，降低数据量） */
  loadStat() {
    const statuses = [
      { key: 'pending', status: 0 },
      { key: 'accepted', status: 1 },
      { key: 'rejected', status: 2 }
    ]
    statuses.forEach(item => {
      get('/study/demands', { page: 1, size: 1, status: item.status }, { loading: false, showError: false }).then(res => {
        const total = Number((res.data && res.data.total) || 0)
        // 动态设置对应统计键值
        this.setData({ ['stat.' + item.key]: total })
      }).catch(() => {})
    })
  },

  /** 切换状态筛选 */
  onFilterTap(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      statusFilter: value,
      page: 1,
      demandList: [],
      hasMore: true
    })
    this.loadDemands()
  },

  /** 跳转提交需求页 */
  onGoSubmit() {
    wx.navigateTo({ url: '/pages/demand/submit' })
  },

  /** 点击卡片查看详情 */
  onDemandTap(e) {
    const id = e.currentTarget.dataset.id
    // 直接使用列表数据展示详情，避免多余请求；如需最新处理意见可改为调用详情接口
    const target = this.data.demandList.find(item => item.id === id)
    if (!target) return
    this.setData({ detail: target, showDetailModal: true })
  },

  /** 关闭详情弹窗 */
  onCloseDetail() {
    this.setData({ showDetailModal: false })
  },

  /** 空方法：阻止弹窗内容区点击穿透 */
  noop() {}
})