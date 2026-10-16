import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      // 后端 Sa-Token 的 token-name 配置为 Authorization，且未配置 token-prefix，
      // 需直接传原始 token；若加 "Bearer " 前缀会导致后端无法解析，提示“未登录或登录已过期”
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 如果响应中包含 code 字段且不为成功状态
    if (res.code && res.code !== 200 && res.code !== 0) {
      ElMessage.error(res.message || '请求失败')

      // 401: Token 过期或未登录
      if (res.code === 401) {
        ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
        })
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
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
          break
        case 403:
          // 优先展示后端返回的具体原因（如"没有角色权限"），便于定位权限问题
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
 */
export function get(url, params) {
  return service.get(url, { params })
}

/**
 * 通用 POST 请求
 */
export function post(url, data) {
  return service.post(url, data)
}

/**
 * 通用 PUT 请求
 */
export function put(url, data) {
  return service.put(url, data)
}

/**
 * 通用 DELETE 请求
 */
export function del(url, params) {
  return service.delete(url, { params })
}

/**
 * 文件上传
 * @param {string} url - 上传接口地址
 * @param {FormData} formData - 表单数据
 * @param {Function} onProgress - 上传进度回调
 */
export function upload(url, formData, onProgress) {
  return service.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

export default service
