import { get, post, del } from '@/utils/request'

/**
 * 学员端「错题本与题库练习」接口封装
 * 对应后端 /study/wrong-questions/** 契约（设计文档 4.4 节）
 */

/**
 * 错题列表（分页）
 * @param {object} params { page, size, courseId, questionType, mastered }
 *   mastered：默认 false（只看未掌握）；传 true 查看已掌握
 * @returns {Promise<Result>} data 为 Page<WrongQuestionVO>
 */
export function getWrongQuestions(params) {
  return get('/study/wrong-questions', params)
}

/**
 * 错题统计（顶部统计卡 + 按题型 / 按课程分布）
 * @returns {Promise<Result>} data 为 WrongQuestionStatsVO：
 *   { total, notMastered, mastered, byType: [{ questionType, typeName, count }], byCourse: [{ courseId, courseName, count }] }
 */
export function getWrongQuestionStats() {
  return get('/study/wrong-questions/stats')
}

/**
 * 错题重做提交（单题即时判分）
 * <p>
 * 判分规则复用基础版（多选忽略顺序、判断题归一化）；答对则该题自动置为已掌握并移出未掌握列表。
 * @param {object} data { questionId, answer }
 * @returns {Promise<Result>} data 为 WrongPracticeResultVO：{ correct, correctAnswer, analysis, mastered }
 */
export function practiceWrongQuestion(data) {
  return post('/study/wrong-questions/practice', data)
}

/**
 * 手动移除错题（标记为已掌握）
 * @param {number} id 错题记录ID
 * @returns {Promise<Result>}
 */
export function removeWrongQuestion(id) {
  return del(`/study/wrong-questions/${id}`)
}
