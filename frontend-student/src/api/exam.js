import { get, post } from '@/utils/request'
import { getExamRecords } from './profile'

/**
 * 学员端「在线考试」接口封装
 * 对应后端 /study/** 契约（设计文档 7.3 节）
 */

/**
 * 查询指定任务最近一次考试记录ID
 * <p>
 * 任务列表/详情接口未返回 recordId，通过成绩单接口按 taskId 反查，
 * 用于「查看成绩」跳转成绩解析页。
 * @param {number} taskId 任务ID
 * @returns {Promise<number|null>} 最近一次考试记录ID，无记录返回 null
 */
export async function findLatestRecordId(taskId) {
  const res = await getExamRecords({ page: 1, size: 100 })
  const records = res.data?.records || []
  const matched = records
    .filter((item) => item.taskId === taskId)
    .sort((a, b) => (b.recordId || 0) - (a.recordId || 0))
  return matched.length > 0 ? matched[0].recordId : null
}

/**
 * 考试信息（须知页：试卷参数、剩余次数、上次成绩）
 * @param {number} taskId 任务ID
 */
export function getExamInfo(taskId) {
  return get(`/study/tasks/${taskId}/exam`)
}

/**
 * 开始考试（创建考试记录并固化题目快照）
 * @param {number} taskId 任务ID
 */
export function startExam(taskId) {
  return post(`/study/exams/${taskId}/start`)
}

/**
 * 取回考试进行中的数据（刷新页面后恢复答题）
 * @param {number} recordId 考试记录ID
 */
export function getExamRecord(recordId) {
  return get(`/study/exams/records/${recordId}`)
}

/**
 * 交卷（后端自动判分）
 * @param {number} recordId 考试记录ID
 * @param {object} data { answers: [{ questionId, answer }] }
 */
export function submitExam(recordId, data) {
  return post(`/study/exams/records/${recordId}/submit`, data)
}

/**
 * 成绩单与逐题解析
 * @param {number} recordId 考试记录ID
 */
export function getExamResult(recordId) {
  return get(`/study/exams/records/${recordId}/result`)
}
