import { get, post } from '@/utils/request'

/**
 * 学员端「培训需求申报」接口封装
 * 对应后端 /study/demands 契约（设计文档第 2.3 节）
 */

/**
 * 我的需求申报分页（仅本人提交）
 * @param {object} params { page, size, status }
 */
export function getMyDemands(params) {
  return get('/study/demands', params)
}

/**
 * 我的需求详情（非本人返回 403）
 * @param {number} id 需求ID
 */
export function getDemandDetail(id) {
  return get(`/study/demands/${id}`)
}

/**
 * 提交培训需求
 * @param {object} data { title, content, trainingTopic, trainingTarget }
 */
export function submitDemand(data) {
  return post('/study/demands', data)
}