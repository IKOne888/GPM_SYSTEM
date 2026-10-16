import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { getProfile } from '@/api/profile'
import { setAuth, clearAuth, getToken, getUserInfo } from '@/utils/auth'

/**
 * 用户信息 Store
 * <p>
 * 管理登录令牌与当前用户信息，登录成功后同步写入 localStorage，
 * 供请求拦截器（token）与顶部用户下拉（姓名/角色）使用。
 */
export const useUserStore = defineStore('studentUser', () => {
  /** 访问令牌 */
  const token = ref(getToken())
  /** 当前用户信息（脱敏） */
  const userInfo = ref(getUserInfo())

  /** 展示用姓名：优先真实姓名，其次用户名 */
  const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '学员')
  /** 是否已登录 */
  const isLogin = computed(() => !!token.value)

  /**
   * 登录：调用认证接口并持久化 token 与用户信息
   * @param {object} form { username, password, captchaKey, captchaCode }
   * @returns {Promise<object>} 登录返回的用户信息
   */
  async function login(form) {
    const res = await authApi.login(form)
    const data = res.data || {}
    token.value = data.token || ''
    userInfo.value = data.user || {}
    // 写入 localStorage，保证刷新后仍处于登录态
    setAuth(token.value, userInfo.value)
    return userInfo.value
  }

  /**
   * 拉取最新个人信息（用于个人中心与顶部信息刷新）
   * @returns {Promise<object>} 用户信息
   */
  async function fetchProfile() {
    const res = await getProfile()
    userInfo.value = res.data || {}
    setAuth(token.value, userInfo.value)
    return userInfo.value
  }

  /**
   * 退出登录：调用后端登出接口并清理本地状态
   */
  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // 登出接口失败（如 token 已失效）不阻断前端清理流程
      console.warn('登出接口调用失败，已清理本地登录态')
    }
    token.value = ''
    userInfo.value = {}
    clearAuth()
  }

  return { token, userInfo, displayName, isLogin, login, fetchProfile, logout }
})
