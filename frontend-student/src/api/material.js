import { get } from '@/utils/request'

/**
 * 学员端「资料中心」接口封装
 * 对应后端 /study/materials 契约（设计文档 4.5 节）
 */

/**
 * 资料汇总（分页）
 * <p>
 * 范围限定：仅返回当前用户存在培训任务（任意状态）所关联课程的启用资料，
 * 避免越权访问未授权课程资料（由后端 Service 层强制）。
 * @param {object} params { page, size, courseId, fileType, keyword }
 *   fileType：PDF/DOC/DOCX/XLS/XLSX
 * @returns {Promise<Result>} data 为 Page<MaterialVO>：
 *   { id, name, fileName, fileType, fileSize, courseId, courseName, uploadTime }
 */
export function getMaterials(params) {
  return get('/study/materials', params)
}

/**
 * 资料课程下拉选项（复用课程中心接口，取全量已授权课程）
 * @param {object} params { page, size }
 * @returns {Promise<Result>} data 为 Page<StudyCourseVO>
 */
export function getMaterialCourseOptions(params) {
  return get('/study/courses', params)
}
