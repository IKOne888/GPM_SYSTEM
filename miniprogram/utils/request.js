// utils/request.js - 学员端网络请求封装（对接后端 /study/** 接口）
//
// 重要说明（务必遵守）：
// 1. 后端 Sa-Token 的 token-name 配置为 Authorization，且【未配置 token-prefix】，
//    因此请求头必须直接传原始 token，不能拼接 "Bearer " 前缀，
//    否则后端无法解析，会提示「未登录或登录已过期」。
// 2. 本地缓存键名使用 student_token / student_userInfo，与其它端隔离。
// 3. 后端统一返回 Result{code,message,data}，code=200 表示成功（业务异常也为 HTTP 200）。

/** token 本地缓存键名 */
const TOKEN_KEY = 'student_token'
/** 用户信息本地缓存键名 */
const USER_KEY = 'student_userInfo'

/**
 * 读取本地 token
 * @returns {string} token 字符串，未登录时返回空串
 */
function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

/**
 * 读取本地用户信息
 * @returns {object} 用户信息对象，不存在时返回空对象
 */
function getUserInfo() {
  return wx.getStorageSync(USER_KEY) || {}
}

/**
 * 写入 token 与用户信息
 * @param {string} token 访问令牌
 * @param {object} user 脱敏用户信息
 */
function setAuth(token, user) {
  if (token) {
    wx.setStorageSync(TOKEN_KEY, token)
  }
  if (user) {
    wx.setStorageSync(USER_KEY, user)
  }
}

/**
 * 清除本地认证信息（登出或 token 失效时调用）
 */
function clearAuth() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
}

/**
 * 获取后端基础地址（含 context-path /api）
 * @returns {string} 形如 http://localhost:8080/api
 */
function getBaseUrl() {
  const app = getApp()
  return (app && app.globalData && app.globalData.baseUrl) || 'http://localhost:8080/api'
}

/**
 * 构造视频流播放地址（该接口在白名单，无需携带 token，可由 <video> 直接访问）
 * @param {number} videoId 视频课件ID
 * @returns {string} 视频流完整地址
 */
function buildVideoUrl(videoId) {
  return getBaseUrl() + '/study/videos/' + videoId + '/stream'
}

/**
 * 统一请求方法
 * @param {string} url 请求路径（不含 baseUrl，如 /study/tasks）
 * @param {string} method 请求方法 GET/POST/PUT/DELETE
 * @param {object} data 请求数据（GET 走 query，其余走 body）
 * @param {object} options 额外选项 { loading, showError, header }
 * @returns {Promise} 成功时 resolve 完整 Result，失败时 reject
 */
function request(url, method = 'GET', data = {}, options = {}) {
  // 解构额外选项：是否显示 loading、是否自动提示错误、附加请求头
  const { loading = true, showError = true, header = {} } = options

  // 需要时展示全局 loading
  if (loading) {
    wx.showLoading({ title: '加载中...', mask: true })
  }

  return new Promise((resolve, reject) => {
    wx.request({
      // 拼接完整请求地址
      url: getBaseUrl() + url,
      method: method,
      data: data,
      header: {
        // 统一 JSON 请求体
        'Content-Type': 'application/json',
        // 直接传原始 token（不加 Bearer 前缀），未登录时传空串
        'Authorization': getToken(),
        ...header
      },
      success(res) {
        // 关闭 loading
        if (loading) wx.hideLoading()

        // HTTP 层成功
        if (res.statusCode === 200) {
          const result = res.data || {}
          // 业务码 200 表示成功
          if (result.code === 200) {
            resolve(result)
            return
          }

          // 业务码 401：token 失效或未登录
          if (result.code === 401) {
            clearAuth()
            wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
            setTimeout(() => {
              wx.reLaunch({ url: '/pages/login/index' })
            }, 1500)
            reject(result)
            return
          }

          // 其它业务异常（含 403 权限不足），按需提示
          if (showError) {
            wx.showToast({ title: result.message || '请求失败', icon: 'none' })
          }
          reject(result)
        } else {
          // HTTP 层异常
          if (showError) {
            wx.showToast({ title: '网络错误(' + res.statusCode + ')', icon: 'none' })
          }
          reject({ code: res.statusCode, message: '网络错误' })
        }
      },
      fail(err) {
        // 网络连接失败
        if (loading) wx.hideLoading()
        if (showError) {
          wx.showToast({ title: '网络连接失败', icon: 'none' })
        }
        reject({ code: -1, message: '网络连接失败', error: err })
      }
    })
  })
}

/** GET 快捷方法 */
function get(url, data, options) {
  return request(url, 'GET', data, options)
}

/** POST 快捷方法 */
function post(url, data, options) {
  return request(url, 'POST', data, options)
}

/** PUT 快捷方法 */
function put(url, data, options) {
  return request(url, 'PUT', data, options)
}

/** DELETE 快捷方法 */
function del(url, data, options) {
  return request(url, 'DELETE', data, options)
}

/**
 * 格式化时间为 yyyy-MM-dd HH:mm:ss（统一展示口径）
 * @param {string} dateStr 后端返回的时间字符串
 * @returns {string} 格式化后的时间，无效值返回空串
 */
function formatTime(dateStr) {
  if (!dateStr) return ''
  // 兼容 iOS 对 "yyyy-MM-dd HH:mm:ss" 的解析差异
  const date = new Date(String(dateStr).replace(/-/g, '/').replace('T', ' '))
  if (isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return (
    date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) +
    ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
  )
}

/**
 * 格式化秒数为 mm:ss（超过 1 小时为 HH:mm:ss）
 * @param {number} seconds 秒数
 * @returns {string} 时长文本
 */
function formatDuration(seconds) {
  const total = Math.floor(Number(seconds) || 0)
  if (total <= 0) return '00:00'
  const pad = (n) => String(n).padStart(2, '0')
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return h > 0 ? pad(h) + ':' + pad(m) + ':' + pad(s) : pad(m) + ':' + pad(s)
}

/**
 * 格式化学习时长为人性化文本（x小时y分钟 / y分钟 / z秒）
 * @param {number} seconds 秒数
 * @returns {string} 文本
 */
function formatStudyDuration(seconds) {
  const total = Math.floor(Number(seconds) || 0)
  if (total <= 0) return '0秒'
  if (total >= 3600) {
    return (total / 3600).toFixed(1) + '小时'
  }
  if (total >= 60) {
    return Math.floor(total / 60) + '分钟'
  }
  return total + '秒'
}

/**
 * 任务状态字符串枚举 → 中文文案
 * @param {string} status not_started/studying/waiting_exam/completed/overdue
 *                        /pending_leader_review/pending_qa_review/rejected
 * @returns {string} 中文状态
 */
function getTaskStatusText(status) {
  const map = {
    not_started: '待学习',
    studying: '学习中',
    waiting_exam: '待考试',
    completed: '已完成',
    overdue: '已逾期',
    pending_leader_review: '待主管审核',
    pending_qa_review: '待QA复核',
    rejected: '已退回'
  }
  return map[status] || '未知'
}

/**
 * 任务状态字符串枚举 → 样式类名
 * @param {string} status 状态枚举
 * @returns {string} 样式类名
 */
function getTaskStatusClass(status) {
  const map = {
    not_started: 'tag-pending',
    studying: 'tag-studying',
    waiting_exam: 'tag-exam',
    completed: 'tag-done',
    overdue: 'tag-overdue',
    pending_leader_review: 'tag-leader-review',
    pending_qa_review: 'tag-qa-review',
    rejected: 'tag-rejected'
  }
  return map[status] || ''
}

/**
 * 消息类型 → 中文文案（1-任务通知 2-到期提醒 3-补考提醒 4-系统通知）
 * @param {number} type 消息类型
 * @returns {string} 中文类型
 */
function getMessageTypeText(type) {
  const map = { 1: '任务通知', 2: '到期提醒', 3: '补考提醒', 4: '系统通知' }
  return map[type] || '通知'
}

/**
 * 证书状态 → 中文文案
 * @param {string} status valid/expiring/expired/not_pass
 * @returns {string} 中文状态
 */
function getCertStatusText(status) {
  const map = { valid: '有效', expiring: '临期', expired: '已过期', not_pass: '未通过' }
  return map[status] || '未知'
}

/**
 * 文件大小（字节）→ 可读文本
 * @param {number} size 字节数
 * @returns {string} 文本
 */
function formatFileSize(size) {
  const bytes = Number(size) || 0
  if (bytes <= 0) return '0B'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

module.exports = {
  // 网络请求
  request,
  get,
  post,
  put,
  del,
  // 认证信息
  getToken,
  getUserInfo,
  setAuth,
  clearAuth,
  getBaseUrl,
  buildVideoUrl,
  // 格式化与字典
  formatTime,
  formatDuration,
  formatStudyDuration,
  formatFileSize,
  getTaskStatusText,
  getTaskStatusClass,
  getMessageTypeText,
  getCertStatusText
}
