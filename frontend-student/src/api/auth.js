import { get, post } from '@/utils/request'

/**
 * 认证相关接口
 * 说明：学员端与管理端共用同一套后端认证接口（/auth/**）
 */

/**
 * 获取图形验证码
 * @returns {Promise} data: { captchaKey, captchaImage }
 */
export function fetchCaptcha() {
  return get('/auth/captcha')
}

/**
 * 学员登录
 * @param {object} data { username, password, captchaKey, captchaCode }
 * @returns {Promise} data: { token, user }
 */
export function login(data) {
  return post('/auth/login', data)
}

/**
 * 退出登录
 * @returns {Promise}
 */
export function logout() {
  return post('/auth/logout')
}
