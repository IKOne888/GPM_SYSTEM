import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken, clearAuth } from './auth'

/**
 * Axios 实例（学员端）
 * - baseURL 取自环境变量，开发环境为 /api（由 Vite 代理到后端 8080）
 * - 超时 30 秒；视频流等长请求由浏览器原生 <video> 处理，不走此实例
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：注入 Sa-Token
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      // 后端 Sa-Token 的 token-name 配置为 Authorization，且未配置 token-prefix，
      // 必须直接传原始 token；若加 "Bearer " 前缀会导致后端无法解析，提示“未登录或登录已过期”
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/** 统一跳转登录页（避免多次弹窗） */
function redirectToLogin() {
  clearAuth()
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

// 响应拦截器：统一处理 Result 结构与异常
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 后端统一返回 Result{code,message,data}；code=200 表示成功
    // 注意：业务异常由全局异常处理器返回 HTTP 200 + body.code，故必须校验 code
    if (res && typeof res.code !== 'undefined' && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      // 401：token 失效或未登录，提示后跳转登录页
      if (res.code === 401) {
        ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => redirectToLogin())
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  (error) => {
    console.error('响应错误:', error)
    let message = '网络错误'
    if (error.response) {
      switch (error.response.status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          redirectToLogin()
          break
        case 403:
          message = error.response.data?.message || '拒绝访问：当前账号无该操作的权限'
          break
        case 404:
          message = '请求资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = error.response.data?.message || `请求失败(${error.response.status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/**
 * 通用 GET 请求
 * @param {string} url 接口地址
 * @param {object} params 查询参数
 */
export function get(url, params) {
  return service.get(url, { params })
}

/**
 * 通用 POST 请求
 * @param {string} url 接口地址
 * @param {object} data 请求体
 */
export function post(url, data) {
  return service.post(url, data)
}

/**
 * 通用 PUT 请求
 * @param {string} url 接口地址
 * @param {object} data 请求体
 */
export function put(url, data) {
  return service.put(url, data)
}

/**
 * 通用 DELETE 请求
 * <p>
 * 用于错题本「手动移除错题」等删除语义接口。
 * @param {string} url 接口地址
 * @param {object} params 查询参数（可选）
 */
export function del(url, params) {
  return service.delete(url, { params })
}

export default service
