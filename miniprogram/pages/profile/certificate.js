// pages/profile/certificate.js - 培训档案 / 证书
const { get, formatTime, getCertStatusText } = require('../../utils/request')

Page({
  data: {
    // 证书列表（CertificateVO）
    certList: [],
    // 加载中
    loading: true
  },

  onLoad() {
    this.loadCertificates()
  },

  onPullDownRefresh() {
    // 下拉刷新
    this.loadCertificates()
    wx.stopPullDownRefresh()
  },

  /** 加载培训档案 / 证书 */
  loadCertificates() {
    this.setData({ loading: true })
    get('/study/records/certificate', {}, { loading: false }).then(res => {
      const list = (res.data || []).map(item => {
        // 补充展示字段：状态文本、状态样式类、时间文本
        item.statusText = getCertStatusText(item.status)
        item.statusClass = this.certStatusClass(item.status)
        item.lastPassTimeText = formatTime(item.lastPassTime)
        item.validUntilText = formatTime(item.validUntil)
        return item
      })
      this.setData({ certList: list, loading: false })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 证书状态 → 样式类 */
  certStatusClass(status) {
    const map = {
      valid: 'cert-valid',
      expiring: 'cert-expiring',
      expired: 'cert-expired',
      not_pass: 'cert-notpass'
    }
    return map[status] || 'cert-valid'
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