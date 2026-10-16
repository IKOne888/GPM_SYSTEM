// pages/task/detail.js - 任务详情（课程信息 + 视频课件 + 课程资料 + 考试入口）
const {
  get,
  getTaskStatusText,
  getTaskStatusClass,
  formatTime,
  formatDuration,
  formatStudyDuration,
  formatFileSize
} = require('../../utils/request')

Page({
  data: {
    // 任务ID
    taskId: null,
    // 任务详情（后端 TaskDetailVO，为扁平结构）
    task: null,
    // 视频课件列表（含本人进度）
    videoList: [],
    // 课程资料列表
    fileList: [],
    // 考试信息（未配置试卷时为 null）
    exam: null
  },

  onLoad(options) {
    // 从路由参数取任务ID
    if (options.id) {
      this.setData({ taskId: options.id })
      this.loadDetail(options.id)
    }
  },

  onPullDownRefresh() {
    // 下拉刷新详情
    this.loadDetail(this.data.taskId)
    wx.stopPullDownRefresh()
  },

  onShow() {
    // 从学习/考试页返回时刷新进度与状态
    if (this.data.taskId && this.data.task) {
      this.loadDetail(this.data.taskId)
    }
  },

  /** 加载任务详情 */
  loadDetail(taskId) {
    get('/study/tasks/' + taskId, {}, { loading: false }).then(res => {
      const task = res.data
      if (!task) return

      // 补充展示字段
      task.statusText = getTaskStatusText(task.status)
      task.statusClass = getTaskStatusClass(task.status)
      task.deadlineText = formatTime(task.deadline)
      task.createTimeText = formatTime(task.createTime)
      task.studyDurationText = formatStudyDuration(task.studyDuration)
      task.progressPercent = Math.floor(Number(task.videoProgress) || 0)

      // 视频列表补充时长文本与进度百分比
      const videoList = (task.videos || []).map(v => {
        v.durationText = formatDuration(v.duration)
        v.progressPercent = Math.floor(Number(v.progress) || 0)
        return v
      })

      // 资料列表补充大小文本与上传时间文本
      const fileList = (task.files || []).map(f => {
        f.fileSizeText = formatFileSize(f.fileSize)
        f.uploadTimeText = formatTime(f.uploadTime)
        return f
      })

      this.setData({
        task: task,
        videoList: videoList,
        fileList: fileList,
        exam: task.exam || null
      })
    })
  },

  /** 开始/继续学习：进入首个视频播放 */
  onStartStudy() {
    const { taskId, videoList } = this.data
    if (!videoList || videoList.length === 0) {
      wx.showToast({ title: '该课程暂无视频课件', icon: 'none' })
      return
    }
    // 优先跳转到未完成的视频，否则回到第一个
    const target = videoList.find(v => v.isCompleted !== 1) || videoList[0]
    wx.navigateTo({
      url: '/pages/video/play?taskId=' + taskId + '&videoId=' + target.id
    })
  },

  /** 点击某个视频进入播放 */
  onVideoTap(e) {
    const videoId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/video/play?taskId=' + this.data.taskId + '&videoId=' + videoId
    })
  },

  /** 进入考试须知页 */
  onGoExam() {
    wx.navigateTo({
      url: '/pages/exam/enter?taskId=' + this.data.taskId
    })
  },

  /** 查看课程详情（由任务跳转课程中心详情） */
  onCourseTap() {
    const courseId = this.data.task && this.data.task.courseId
    if (!courseId) return
    wx.navigateTo({ url: '/pages/course/detail?id=' + courseId })
  },

  /** 下载课程资料（复制预签名地址） */
  onFileTap(e) {
    const fileId = e.currentTarget.dataset.id
    get('/study/courses/files/' + fileId + '/url', {}, { loading: true }).then(res => {
      const url = res.data
      if (!url) {
        wx.showToast({ title: '资料地址获取失败', icon: 'none' })
        return
      }
      // 复制下载地址到剪贴板（小程序不支持直接下载二进制到本地文件）
      wx.setClipboardData({
        data: url,
        success() {
          wx.showToast({ title: '下载链接已复制', icon: 'none' })
        }
      })
    }).catch(() => {})
  }
})