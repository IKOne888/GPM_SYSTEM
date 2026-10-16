import { get } from '@/utils/request'

/**
 * 学员端「学习看板」接口封装
 * 对应后端 /study/analysis/** 契约（设计文档 4.1 节）
 */

/**
 * 学习看板聚合概览
 * <p>
 * 一次性返回看板全部数据：4 个指标卡 + 3 个图表（月度趋势 / 任务状态分布 / 成绩分布）。
 * 数据隔离由后端以登录用户为边界强制保证。
 * @returns {Promise<Result>} data 结构见 AnalysisOverviewVO：
 *   { totalStudyDuration, totalTask, completedTask, studyingTask, overdueTask,
 *     certificateCount, avgScore, passRate, monthly[], statusDist[], scoreDist[] }
 */
export function getAnalysisOverview() {
  return get('/study/analysis/overview')
}
