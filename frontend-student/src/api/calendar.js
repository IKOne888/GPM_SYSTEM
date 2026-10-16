import { get } from '@/utils/request'

/**
 * 学员端「培训日历」接口封装
 * 对应后端 /study/calendar 契约（设计文档 4.3 节）
 */

/**
 * 按日期区间查询培训事件
 * <p>
 * 事件类型：1-任务截止（取自任务 deadline）、2-考试安排（取自考试记录 start_time）。
 * 返回结果仅包含有事件的日期。
 * @param {object} params { startDate, endDate }
 *   startDate / endDate：格式 yyyy-MM-dd，区间跨度不得超过 62 天
 * @returns {Promise<Result>} data 为 CalendarDayVO[]：
 *   [{ date: 'yyyy-MM-dd', events: [{ type, title, taskId, courseId, recordId, time, status }] }]
 */
export function getCalendar(params) {
  return get('/study/calendar', params)
}
