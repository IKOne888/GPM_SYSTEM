/**
 * 格式化工具集（管理端）
 * <p>
 * 统一处理后端 LocalDateTime 字符串的展示，避免各页面直接渲染 ISO 格式
 * （如 2026-10-12T15:59:49）导致界面不统一。
 */

/**
 * 后端时间字符串转「yyyy-MM-dd HH:mm:ss」
 * <p>
 * 后端 LocalDateTime 经 Jackson 序列化后形如 2026-10-12T15:59:49，
 * 此处将中间的 T 替换为空格并截取到秒，得到统一的展示格式。
 *
 * @param {string} value 时间字符串
 * @returns {string} 格式化结果，空值返回 '-'
 */
export function formatDateTime(value) {
  // 空值兜底，避免页面出现 null/undefined
  if (!value) {
    return '-'
  }
  // 统一替换 T 分隔符并截取到秒（前 19 位即 yyyy-MM-dd HH:mm:ss）
  return String(value).replace('T', ' ').slice(0, 19)
}