import { get, post } from '@/utils/request'

/**
 * 学员端「我的任务 / 在线学习」接口封装
 * 对应后端 /study/** 契约（设计文档 7.1、7.2 节）
 */

// ==================== 模块一：我的任务 ====================

/**
 * 分页查询我的任务
 * @param {object} params { page, size, status, keyword }
 */
export function getTasks(params) {
  return get('/study/tasks', params)
}

/**
 * 我的任务状态统计
 */
export function getTaskStats() {
  return get('/study/tasks/stats')
}

/**
 * 任务详情（含课程、视频、资料、考试信息）
 * @param {number} id 任务ID
 */
export function getTaskDetail(id) {
  return get(`/study/tasks/${id}`)
}

/**
 * 任务下的视频列表（含本人学习进度）
 * @param {number} id 任务ID
 */
export function getTaskVideos(id) {
  return get(`/study/tasks/${id}/videos`)
}

// ==================== 模块二：在线学习 ====================

/**
 * 拼接视频播放地址（浏览器原生 <video> 直接使用，无需携带 token）
 * @param {number} videoId 视频课件ID
 * @returns {string} 视频流地址
 */
export function getVideoStreamUrl(videoId) {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${base}/study/videos/${videoId}/stream`
}

/**
 * 查询单视频学习记录（断点续播）
 * @param {number} videoId 视频课件ID
 * @param {number} taskId 任务ID
 */
export function getVideoRecord(videoId, taskId) {
  return get(`/study/videos/${videoId}/record`, { taskId })
}

/**
 * 上报视频学习进度
 * @param {object} data { taskId, videoId, progress, lastPosition, studyDuration }
 */
export function reportProgress(data) {
  return post('/study/videos/progress', data)
}

/**
 * 课程资料列表
 * @param {number} courseId 课程ID
 */
export function getCourseFiles(courseId) {
  return get(`/study/courses/${courseId}/files`)
}

/**
 * 获取资料下载地址（预签名 URL）
 * @param {number} fileId 资料ID
 */
export function getCourseFileUrl(fileId) {
  return get(`/study/courses/files/${fileId}/url`)
}
