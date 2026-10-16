// pages/course/list.js - 课程中心（tabBar 第 2 项，浏览课程并自主报名）
const { get } = require('../../utils/request')

Page({
  data: {
    // 课程列表
    courseList: [],
    // 分页
    page: 1,
    size: 10,
    hasMore: true,
    // 加载中
    loading: false,
    // 搜索关键字
    keyword: '',
    // 报名状态筛选：'' 全部 | false 未报名 | true 已报名
    enrolledFilter: ''
  },

  onLoad() {
    this.loadCourses()
  },

  onShow() {
    // 报名后返回需刷新
    if (this.data.courseList.length > 0) {
      this.setData({ page: 1, courseList: [], hasMore: true })
      this.loadCourses()
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 1, courseList: [], hasMore: true })
    this.loadCourses()
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadCourses()
    }
  },

  /** 加载课程列表 */
  loadCourses() {
    if (this.data.loading) return
    this.setData({ loading: true })

    const params = { page: this.data.page, size: this.data.size }
    // 关键字筛选
    if (this.data.keyword) {
      params.keyword = this.data.keyword
    }
    // 报名状态筛选
    if (this.data.enrolledFilter !== '') {
      params.enrolled = this.data.enrolledFilter === 'true'
    }

    get('/study/courses', params, { loading: false }).then(res => {
      const records = (res.data && res.data.records) || []
      this.setData({
        courseList: this.data.courseList.concat(records),
        hasMore: records.length >= this.data.size,
        page: this.data.page + 1,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 输入搜索关键字 */
  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  /** 触发搜索 */
  onSearch() {
    this.setData({ page: 1, courseList: [], hasMore: true })
    this.loadCourses()
  },

  /** 切换报名状态筛选 */
  onFilterTap(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ enrolledFilter: value, page: 1, courseList: [], hasMore: true })
    this.loadCourses()
  },

  /** 进入课程详情 */
  onCourseTap(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/course/detail?id=' + id })
  }
})