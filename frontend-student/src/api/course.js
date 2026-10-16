import { get, post } from '@/utils/request'

/**
 * 学员端「课程中心（自主选学）」接口封装
 * 对应后端 /study/courses/** 契约（设计文档 4.2 节）
 */

/**
 * 课程列表（分页）
 * <p>
 * 推荐逻辑由后端实现：优先返回与当前用户部门 / 岗位匹配的启用课程。
 * @param {object} params { page, size, keyword, categoryId, enrolled }
 *   enrolled：true-只看已报名，false-只看未报名，不传-全部
 * @returns {Promise<Result>} data 为 Page<StudyCourseVO>
 */
export function getCourses(params) {
  return get('/study/courses', params)
}

/**
 * 课程详情
 * @param {number} id 课程ID
 * @returns {Promise<Result>} data 为 StudyCourseVO（含 fileCount/videoCount/questionCount/departmentName/positionName）
 */
export function getCourseDetail(id) {
  return get(`/study/courses/${id}`)
}

/**
 * 自主报名课程
 * <p>
 * 前端无需阻止重复点击，后端已做幂等与状态校验（重复报名返回中文提示）。
 * @param {number} id 课程ID
 * @returns {Promise<Result>} data 为 { taskId }
 */
export function enrollCourse(id) {
  return post(`/study/courses/${id}/enroll`)
}

/**
 * 课程分类下拉选项
 * <p>
 * 复用只读字典接口 /training/course-categories（登录即可访问，未做管理员角色限制），
 * 用于课程中心的「分类」筛选与资料中心的课程筛选。
 * @returns {Promise<Result>} data 为 [{ id, name, code, sort, status }]
 */
export function getCourseCategories() {
  return get('/training/course-categories')
}
