/**
 * 本地认证信息读写工具
 * <p>
 * 学员端与管理端为两个独立工程，为避免同域部署时 token 相互覆盖，
 * 学员端统一使用 `student_token` / `student_userInfo` 作为存储键名。
 */

/** token 存储键名 */
const TOKEN_KEY = 'student_token'
/** 用户信息存储键名 */
const USER_KEY = 'student_userInfo'

/**
 * 读取本地 token
 * @returns {string} token 字符串，未登录时返回空串
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 写入 token 与用户信息
 * @param {string} token 访问令牌
 * @param {object} user 脱敏用户信息
 */
export function setAuth(token, user) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  }
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }
}

/**
 * 读取本地缓存的用户信息
 * @returns {object} 用户信息对象，不存在时返回空对象
 */
export function getUserInfo() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch (e) {
    return {}
  }
}

/**
 * 清除本地认证信息（登出或 token 失效时调用）
 */
export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
