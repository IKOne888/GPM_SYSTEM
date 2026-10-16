import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTaskStats } from '@/api/study'

/**
 * 任务统计 Store
 * <p>
 * 缓存「我的任务」工作台顶部的状态统计，避免多页面重复请求；
 * 学习/考试后可通过 refreshStats() 强制刷新。
 */
export const useTaskStore = defineStore('studentTask', () => {
  /** 统计结果：total / notStarted / studying / waitingExam / completed / overdue / pendingReview / rejected */
  const stats = ref({
    total: 0,
    notStarted: 0,
    studying: 0,
    waitingExam: 0,
    completed: 0,
    overdue: 0,
    pendingReview: 0,
    rejected: 0
  })

  /** 最近一次拉取时间戳（用于简单缓存判断） */
  const lastFetchTime = ref(0)

  /**
   * 拉取任务统计
   * @param {boolean} force 是否强制刷新（忽略 5 秒内的缓存）
   */
  async function fetchStats(force = false) {
    const now = Date.now()
    if (!force && now - lastFetchTime.value < 5000) {
      return stats.value
    }
    const res = await getTaskStats()
    stats.value = {
      total: 0,
      notStarted: 0,
      studying: 0,
      waitingExam: 0,
      completed: 0,
      overdue: 0,
      pendingReview: 0,
      rejected: 0,
      ...(res.data || {})
    }
    lastFetchTime.value = now
    return stats.value
  }

  /**
   * 重置统计（退出登录时调用）
   */
  function reset() {
    stats.value = { total: 0, notStarted: 0, studying: 0, waitingExam: 0, completed: 0, overdue: 0, pendingReview: 0, rejected: 0 }
    lastFetchTime.value = 0
  }

  return { stats, fetchStats, reset }
})
