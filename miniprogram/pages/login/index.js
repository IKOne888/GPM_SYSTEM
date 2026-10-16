// pages/login/index.js - 登录页（账号密码 + 图形验证码）
const app = getApp()
const { get, post } = require('../../utils/request')

Page({
  data: {
    // 用户名
    username: '',
    // 密码
    password: '',
    // 验证码输入内容
    captchaCode: '',
    // 验证码 key（随验证码一同下发，登录时回传）
    captchaKey: '',
    // 验证码图片（base64 data URL）
    captchaImage: '',
    // 登录中状态（禁用按钮防重复提交）
    loading: false,
    // 是否记住用户名
    rememberUsername: true
  },

  onLoad() {
    // 恢复上次记住的用户名
    const savedUsername = wx.getStorageSync('savedUsername')
    if (savedUsername) {
      this.setData({ username: savedUsername })
    }
    // 首次进入即拉取验证码
    this.loadCaptcha()
  },

  /** 输入用户名 */
  onUsernameInput(e) {
    this.setData({ username: e.detail.value })
  },

  /** 输入密码 */
  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  /** 输入验证码 */
  onCaptchaInput(e) {
    this.setData({ captchaCode: e.detail.value })
  },

  /** 切换「记住用户名」 */
  onRememberChange() {
    this.setData({ rememberUsername: !this.data.rememberUsername })
  },

  /**
   * 获取图形验证码
   * 说明：验证码为一次性使用，登录失败后需重新获取
   */
  loadCaptcha() {
    get('/auth/captcha', {}, { loading: false, showError: false }).then(res => {
      const data = res.data || {}
      this.setData({
        captchaKey: data.captchaKey || '',
        captchaImage: data.captchaImage || '',
        // 刷新验证码时清空已输入内容
        captchaCode: ''
      })
    }).catch(() => {
      wx.showToast({ title: '验证码加载失败，请点击刷新', icon: 'none' })
    })
  },

  /** 点击验证码图片刷新 */
  onCaptchaTap() {
    this.loadCaptcha()
  },

  /**
   * 登录
   */
  onLogin() {
    const { username, password, captchaCode, captchaKey } = this.data

    // 前置校验：用户名
    if (!username.trim()) {
      wx.showToast({ title: '请输入用户名', icon: 'none' })
      return
    }
    // 前置校验：密码
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    // 前置校验：验证码
    if (!captchaCode.trim()) {
      wx.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }
    // 防重复提交
    if (this.data.loading) return

    this.setData({ loading: true })

    post('/auth/login', {
      username: username.trim(),
      password: password,
      captchaKey: captchaKey,
      captchaCode: captchaCode.trim()
    }, { loading: true, showError: true }).then(res => {
      // 后端返回 Result<LoginVO>{ token, user }
      const data = res.data || {}
      if (!data.token) {
        wx.showToast({ title: '登录失败，请重试', icon: 'none' })
        this.setData({ loading: false })
        return
      }

      // 保存登录态
      app.setLoginInfo(data.token, data.user || {})

      // 记住用户名
      if (this.data.rememberUsername) {
        wx.setStorageSync('savedUsername', username.trim())
      } else {
        wx.removeStorageSync('savedUsername')
      }

      wx.showToast({ title: '登录成功', icon: 'success' })
      // 切换至首页（tabBar 页面必须用 switchTab）
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' })
      }, 800)
    }).catch(() => {
      // 登录失败：重置按钮状态并刷新验证码（验证码一次性）
      this.setData({ loading: false })
      this.loadCaptcha()
    })
  }
})
