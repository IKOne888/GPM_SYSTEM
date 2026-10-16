import { get, put } from '@/utils/request'

/**
 * 学员端「个人中心与培训档案」接口封装
 * 对应后端 /study/** 契约（设计文档 7.4 节）
 */

// ==================== 个人资料 ====================

/**
 * 查询个人信息（脱敏）
 */
export function getProfile() {
  return get('/study/profile')
}

/**
 * 修改个人资料（姓名 / 手机 / 邮箱）
 * @param {object} data { realName, phone, email }
 */
export function updateProfile(data) {
  return put('/study/profile', data)
}

/**
 * 修改登录密码
 * @param {object} data { oldPassword, newPassword }
 */
export function updatePassword(data) {
  return put('/study/profile/password', data)
}

// ==================== 学习记录与成绩单 ====================

/**
 * 学习记录分页
 * @param {object} params { page, size, keyword }
 */
export function getStudyRecords(params) {
  return get('/study/records/study', params)
}

/**
 * 考试成绩单分页
 * @param {object} params { page, size, keyword }
 */
export function getExamRecords(params) {
  return get('/study/records/exam', params)
}

/**
 * 培训档案 / 证书列表（含有效期状态）
 */
export function getCertificates() {
  return get('/study/records/certificate')
}

// ==================== 消息通知 ====================

/**
 * 消息分页
 * @param {object} params { page, size, isRead }
 */
export function getMessages(params) {
  return get('/study/messages', params)
}

/**
 * 未读消息数量
 */
export function getUnreadCount() {
  return get('/study/messages/unread-count')
}

/**
 * 单条消息标记已读
 * @param {number} id 消息ID
 */
export function readMessage(id) {
  return put(`/study/messages/${id}/read`)
}

/**
 * 全部消息标记已读
 */
export function readAllMessages() {
  return put('/study/messages/read-all')
}
