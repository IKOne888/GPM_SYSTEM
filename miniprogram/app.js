// app.js - 小程序全局逻辑（学员端）
const { getToken, getUserInfo, setAuth, clearAuth } = require('./utils/request')

App({
  // 全局数据：登录态与后端地址
  globalData: {
    // 脱敏用户信息（id/username/realName/phone/email/departmentId/positionId/roles 等）
    userInfo: null,
    // 访问令牌（Sa-Token）
    token: '',
    // 后端基础地址（含 context-path /api）
    baseUrl: 'http://localhost:8080/api',
    // 未读消息数（顶部铃铛角标）
    unreadCount: 0
  },

  onLaunch() {
    // 启动时恢复本地登录态
    const token = getToken()
    const userInfo = getUserInfo()
    if (token && userInfo && userInfo.id) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
    }
  },

  /**
   * 判断是否已登录
   * @returns {boolean} 是否已登录
   */
  isLoggedIn() {
    return !!this.globalData.token
  },

  /**
   * 保存登录信息（token + 用户信息）
   * @param {string} token 访问令牌
   * @param {object} user 脱敏用户信息
   */
  setLoginInfo(token, user) {
    this.globalData.token = token
    this.globalData.userInfo = user
    setAuth(token, user)
  },

  /**
   * 清除登录信息（登出或 token 失效时调用）
   */
  clearLoginInfo() {
    this.globalData.token = ''
    this.globalData.userInfo = null
    this.globalData.unreadCount = 0
    clearAuth()
  }
})
