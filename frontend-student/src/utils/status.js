/**
 * 状态字典工具
 * <p>
 * 集中维护后端 tinyint 状态码与前端的「文案 / 标签色 / Element Plus 标签类型」映射，
 * 避免各页面重复硬编码。定义参见设计文档 6.1 / 6.3 节。
 */

/**
 * 培训任务状态字典
 * 0-待学习 1-学习中 2-待考试 3-已完成 4-已逾期
 * 5-待主管审核 6-待QA复核 7-已退回（新增：两级审核闭环的展示状态）
 */
export const TASK_STATUS = {
  0: { value: 'not_started', label: '待学习', type: 'info', color: '#8C9AAE' },
  1: { value: 'studying', label: '学习中', type: 'primary', color: '#1677FF' },
  2: { value: 'waiting_exam', label: '待考试', type: 'warning', color: '#FAAD14' },
  3: { value: 'completed', label: '已完成', type: 'success', color: '#52C41A' },
  4: { value: 'overdue', label: '已逾期', type: 'danger', color: '#FF4D4F' },
  5: { value: 'pending_leader_review', label: '待主管审核', type: 'warning', color: '#FAAD14' },
  6: { value: 'pending_qa_review', label: '待QA复核', type: 'primary', color: '#1677FF' },
  7: { value: 'rejected', label: '已退回', type: 'danger', color: '#FF4D4F' }
}

/**
 * 任务状态筛选项（用于列表页筛选下拉）
 * 值为后端接收的字符串状态编码
 */
export const TASK_STATUS_OPTIONS = [
  { value: 'not_started', label: '待学习' },
  { value: 'studying', label: '学习中' },
  { value: 'waiting_exam', label: '待考试' },
  { value: 'pending_review', label: '审核中' },
  { value: 'rejected', label: '已退回' },
  { value: 'completed', label: '已完成' },
  { value: 'overdue', label: '已逾期' }
]

/**
 * 考试记录状态字典：0-考试中 1-已提交 2-已批阅
 */
export const EXAM_STATUS = {
  0: { label: '考试中', type: 'primary' },
  1: { label: '已提交', type: 'warning' },
  2: { label: '已批阅', type: 'success' }
}

/**
 * 题型字典：1-单选 2-多选 3-判断 4-填空 5-简答
 */
export const QUESTION_TYPE = {
  1: { label: '单选题', tag: 'primary' },
  2: { label: '多选题', tag: 'success' },
  3: { label: '判断题', tag: 'warning' },
  4: { label: '填空题', tag: 'info' },
  5: { label: '简答题', tag: 'danger' }
}

/**
 * 证书状态字典：valid-有效 expiring-临期 expired-已过期 not_pass-未通过
 */
export const CERT_STATUS = {
  valid: { label: '有效', type: 'success' },
  expiring: { label: '临期', type: 'warning' },
  expired: { label: '已过期', type: 'danger' },
  not_pass: { label: '未通过', type: 'info' }
}

/**
 * 消息类型字典：1-任务通知 2-到期提醒 3-补考提醒 4-系统通知
 */
export const MESSAGE_TYPE = {
  1: { label: '任务通知', type: 'primary' },
  2: { label: '到期提醒', type: 'warning' },
  3: { label: '补考提醒', type: 'danger' },
  4: { label: '系统通知', type: 'info' }
}

/**
 * 按状态码获取任务状态信息
 * @param {number} code 状态码
 * @returns {{label:string,type:string,color:string}} 状态信息，未知返回中性值
 */
export function getTaskStatus(code) {
  return TASK_STATUS[code] || { label: '未知', type: 'info', color: '#8C9AAE', value: '' }
}

/**
 * 按状态码获取题型名称
 * @param {number} type 题型编码
 * @returns {string} 题型中文名
 */
export function getQuestionTypeName(type) {
  return QUESTION_TYPE[type]?.label || '未知题型'
}

/**
 * 按状态字符串获取证书状态信息
 * @param {string} status 证书状态
 * @returns {{label:string,type:string}} 证书状态信息
 */
export function getCertStatus(status) {
  return CERT_STATUS[status] || { label: '未知', type: 'info' }
}
