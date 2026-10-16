/**
 * 格式化工具集
 * <p>
 * 统一处理时长、日期时间、截止倒计时、文件大小与百分比展示，
 * 保证各页面展示口径一致。
 */

/**
 * 秒数转「x小时y分」/「y分z秒」可读文案
 * @param {number} seconds 秒数
 * @returns {string} 例如 3720 -> "1小时2分"，90 -> "1分30秒"
 */
export function formatDuration(seconds) {
  const total = Number(seconds) || 0
  if (total <= 0) {
    return '0分钟'
  }
  const hour = Math.floor(total / 3600)
  const minute = Math.floor((total % 3600) / 60)
  const second = total % 60
  if (hour > 0) {
    return `${hour}小时${minute}分`
  }
  if (minute > 0) {
    return `${minute}分${second}秒`
  }
  return `${second}秒`
}

/**
 * 秒数转 HH:mm:ss（用于视频播放时间轴）
 * @param {number} seconds 秒数
 * @returns {string} 例如 125 -> "02:05"
 */
export function formatClock(seconds) {
  const total = Math.max(0, Math.floor(Number(seconds) || 0))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n) => String(n).padStart(2, '0')
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`
}

/**
 * 后端 LocalDateTime 字符串（yyyy-MM-ddTHH:mm:ss）转「yyyy-MM-dd HH:mm」
 * @param {string} value 时间字符串
 * @returns {string} 格式化结果，空值返回 '-'
 */
export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

/**
 * 时间字符串转「yyyy-MM-dd」
 * @param {string} value 时间字符串
 * @returns {string} 日期部分，空值返回 '-'
 */
export function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

/**
 * 计算距截止时间的天数（用于倒计时提示）
 * @param {string} deadline 截止时间字符串
 * @returns {{days:number,text:string,urgent:boolean}} 剩余天数与文案；无截止时间返回 0/''/false
 */
export function countdown(deadline) {
  if (!deadline) {
    return { days: 0, text: '无截止时间', urgent: false }
  }
  const target = new Date(String(deadline).replace('T', ' ').replace(/-/g, '/')).getTime()
  const diff = target - Date.now()
  if (Number.isNaN(target)) {
    return { days: 0, text: '无截止时间', urgent: false }
  }
  if (diff <= 0) {
    return { days: 0, text: '已逾期', urgent: true }
  }
  const days = Math.ceil(diff / (24 * 3600 * 1000))
  if (days <= 1) {
    return { days, text: '今日截止', urgent: true }
  }
  return { days, text: `剩余 ${days} 天`, urgent: days <= 3 }
}

/**
 * 字节数转可读文件大小
 * @param {number} bytes 字节数
 * @returns {string} 例如 1048576 -> "1.00 MB"
 */
export function formatFileSize(bytes) {
  const size = Number(bytes) || 0
  if (size <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  const value = size / Math.pow(1024, index)
  return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

/**
 * 百分比数值归一化（保留两位小数，限制 0~100）
 * @param {number|string} value 百分比数值
 * @returns {number} 归一化后的百分比
 */
export function toPercent(value) {
  const num = Number(value)
  if (Number.isNaN(num)) return 0
  return Math.min(100, Math.max(0, Math.round(num * 100) / 100))
}
