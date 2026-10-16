// pages/video/play.js - 在线学习（视频播放：倍速 / 断点续播 / 进度上报 / 防挂机）
const {
  get,
  post,
  buildVideoUrl,
  formatDuration,
  formatStudyDuration
} = require('../../utils/request')

Page({
  data: {
    // 任务ID / 当前视频ID
    taskId: null,
    videoId: null,
    // 当前视频信息
    videoInfo: null,
    // 视频流播放地址（走白名单接口，无需 token）
    videoUrl: '',
    // 播放状态
    isPlaying: false,
    // 当前播放位置（秒）与总时长（秒）
    currentTime: 0,
    duration: 0,
    // 展示用时间文本
    currentTimeText: '00:00',
    durationText: '00:00',
    // 倍速
    playbackRate: 1.0,
    speedOptions: [0.5, 0.75, 1.0, 1.25, 1.5, 2.0],
    speedIndex: 2,
    showSpeedPicker: false,
    // 进度百分比（进度条用）
    progressPercent: 0,
    // 防挂机：最近活跃时间与弹窗开关
    lastActiveTime: 0,
    showAntiIdle: false,
    // 断点续播：上次位置、是否已确认续播
    lastPosition: 0,
    hasResume: false,
    // 本次学习时长（秒）
    studyDuration: 0,
    // 本次学习时长展示文本
    studyDurationText: '0秒',
    // 视频列表与当前索引
    videoList: [],
    currentVideoIndex: 0,
    showPlaylist: false
  },

  // 防挂机检测间隔（毫秒）：30 秒无操作即暂停
  IDLE_CHECK_INTERVAL: 30000,
  // 进度上报间隔（毫秒）：15 秒上报一次
  PROGRESS_REPORT_INTERVAL: 15000,
  // 内部定时器句柄
  _idleTimer: null,
  _progressTimer: null,
  _studyTimer: null,
  // 视频上下文
  _videoContext: null,

  onLoad(options) {
    // 解析参数
    this.setData({
      taskId: options.taskId,
      videoId: options.videoId ? parseInt(options.videoId) : null
    })
    // 创建视频上下文
    this._videoContext = wx.createVideoContext('myVideo', this)
    this.loadVideoList()
  },

  onUnload() {
    // 页面卸载：上报最终进度并清理定时器
    this.reportProgress()
    this.clearTimers()
  },

  onHide() {
    // 页面隐藏：暂停播放并上报进度
    this.reportProgress()
    if (this._videoContext) {
      this._videoContext.pause()
    }
    this.setData({ isPlaying: false })
    this.clearTimers()
  },

  onShow() {
    // 页面重新显示：若在播放则恢复计时器
    if (this.data.isPlaying) {
      this.startTimers()
    }
  },

  /** 加载任务下的视频列表 */
  loadVideoList() {
    get('/study/tasks/' + this.data.taskId + '/videos', {}, { loading: false }).then(res => {
      const list = res.data || []
      list.forEach(item => {
        item.durationText = formatDuration(item.duration)
      })
      this.setData({ videoList: list })

      // 定位到目标视频（默认第一个）
      let currentIndex = 0
      if (this.data.videoId) {
        const idx = list.findIndex(v => v.id === this.data.videoId)
        if (idx >= 0) currentIndex = idx
      }
      this.setData({ currentVideoIndex: currentIndex })
      this.loadVideo(list[currentIndex])
    })
  },

  /** 加载单个视频（含断点续播判断） */
  loadVideo(videoInfo) {
    if (!videoInfo) {
      wx.showToast({ title: '暂无视频课件', icon: 'none' })
      return
    }

    this.setData({
      videoInfo: videoInfo,
      // 视频流地址走白名单接口
      videoUrl: buildVideoUrl(videoInfo.id),
      videoId: videoInfo.id,
      currentTime: 0,
      progressPercent: 0,
      studyDuration: 0,
      studyDurationText: '0秒',
      hasResume: false,
      lastPosition: 0
    })

    // 查询本人在该视频上的学习记录，用于断点续播
    get('/study/videos/' + videoInfo.id + '/record', { taskId: this.data.taskId }, { loading: false, showError: false })
      .then(res => {
        const record = res.data
        if (record && record.lastPosition > 0) {
          const videoDuration = videoInfo.duration || 0
          // 已接近结尾（差 10 秒内）则不提示续播
          const nearEnd = videoDuration > 0 && record.lastPosition >= videoDuration - 10

          this.setData({
            lastPosition: record.lastPosition,
            studyDuration: record.studyDuration || 0,
            studyDurationText: formatStudyDuration(record.studyDuration || 0)
          })

          if (!nearEnd) {
            wx.showModal({
              title: '断点续播',
              content: '上次播放到 ' + formatDuration(record.lastPosition) + '，是否从上次位置继续？',
              confirmText: '继续播放',
              cancelText: '从头开始',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  // 标记需要续播，待元数据加载完成后 seek
                  this.setData({ hasResume: true })
                } else {
                  this.setData({ lastPosition: 0, hasResume: false })
                }
              }
            })
          }
        }
      })
      .catch(() => {})
  },

  /** 视频元数据加载完成 */
  onVideoLoaded(e) {
    const duration = e.detail.duration
    this.setData({
      duration: duration,
      durationText: formatDuration(duration)
    })
    // 断点续播：跳转到上次位置
    if (this.data.lastPosition > 0 && this.data.hasResume) {
      this._videoContext.seek(this.data.lastPosition)
      this.setData({ currentTime: this.data.lastPosition })
    }
  },

  /** 开始播放 */
  onVideoPlay() {
    this.setData({ isPlaying: true, lastActiveTime: Date.now(), showAntiIdle: false })
    this.startTimers()
  },

  /** 暂停播放 */
  onVideoPause() {
    this.setData({ isPlaying: false })
    this.clearTimers()
    this.reportProgress()
  },

  /** 播放结束 */
  onVideoEnded() {
    this.setData({ isPlaying: false })
    this.clearTimers()
    this.reportProgress()

    // 自动播放下一个
    const nextIndex = this.data.currentVideoIndex + 1
    if (nextIndex < this.data.videoList.length) {
      wx.showModal({
        title: '视频播放完毕',
        content: '是否播放下一个视频？',
        success: (res) => {
          if (res.confirm) {
            this.switchVideo(nextIndex)
          }
        }
      })
    } else {
      wx.showToast({ title: '全部视频已播放完毕', icon: 'success' })
    }
  },

  /** 播放进度变化 */
  onTimeUpdate(e) {
    const currentTime = Math.floor(e.detail.currentTime || 0)
    const duration = this.data.duration
    this.setData({
      currentTime: currentTime,
      currentTimeText: formatDuration(currentTime),
      progressPercent: duration > 0 ? Math.floor((currentTime / duration) * 100) : 0,
      // 播放中视为活跃
      lastActiveTime: Date.now()
    })
  },

  /** 进度条拖拽 */
  onSliderChange(e) {
    const percent = e.detail.value
    const position = Math.floor((percent / 100) * this.data.duration)
    this._videoContext.seek(position)
    this.setData({
      currentTime: position,
      currentTimeText: formatDuration(position),
      lastActiveTime: Date.now()
    })
  },

  /** 展开/收起倍速选择器 */
  onSpeedTap() {
    this.setData({ showSpeedPicker: !this.data.showSpeedPicker })
  },

  /** 选择倍速 */
  onSpeedSelect(e) {
    const index = e.currentTarget.dataset.index
    const rate = this.data.speedOptions[index]
    this.setData({
      playbackRate: rate,
      speedIndex: index,
      showSpeedPicker: false,
      lastActiveTime: Date.now()
    })
    this._videoContext.playbackRate(rate)
    wx.showToast({ title: rate + 'x 倍速', icon: 'none', duration: 1000 })
  },

  /** 切换视频 */
  switchVideo(index) {
    // 先上报当前视频进度
    this.reportProgress()
    this.clearTimers()

    this.setData({
      currentVideoIndex: index,
      isPlaying: false,
      currentTime: 0,
      progressPercent: 0,
      studyDuration: 0,
      studyDurationText: '0秒',
      lastPosition: 0,
      hasResume: false,
      showPlaylist: false
    })
    this.loadVideo(this.data.videoList[index])
  },

  /** 点击播放列表项 */
  onVideoItemTap(e) {
    const index = e.currentTarget.dataset.index
    if (index !== this.data.currentVideoIndex) {
      this.switchVideo(index)
    }
  },

  /** 切换播放列表面板显隐 */
  togglePlaylist() {
    this.setData({ showPlaylist: !this.data.showPlaylist })
  },

  /** 启动定时器（防挂机 + 进度上报 + 学习时长） */
  startTimers() {
    this.clearTimers()

    // 防挂机：每 5 秒检查一次是否长时间无操作
    this._idleTimer = setInterval(() => {
      const elapsed = Date.now() - this.data.lastActiveTime
      if (elapsed > this.IDLE_CHECK_INTERVAL && this.data.isPlaying) {
        this._videoContext.pause()
        this.setData({ isPlaying: false, showAntiIdle: true })
        this.clearTimers()
      }
    }, 5000)

    // 进度定时上报
    this._progressTimer = setInterval(() => {
      this.reportProgress()
    }, this.PROGRESS_REPORT_INTERVAL)

    // 本地学习时长自增（每秒 +1）
    this._studyTimer = setInterval(() => {
      const duration = this.data.studyDuration + 1
      this.setData({
        studyDuration: duration,
        studyDurationText: formatStudyDuration(duration)
      })
    }, 1000)
  },

  /** 清理全部定时器 */
  clearTimers() {
    if (this._idleTimer) {
      clearInterval(this._idleTimer)
      this._idleTimer = null
    }
    if (this._progressTimer) {
      clearInterval(this._progressTimer)
      this._progressTimer = null
    }
    if (this._studyTimer) {
      clearInterval(this._studyTimer)
      this._studyTimer = null
    }
  },

  /** 防挂机弹窗：确认仍在观看 */
  onAntiIdleConfirm() {
    this.setData({ showAntiIdle: false, lastActiveTime: Date.now() })
    this._videoContext.play()
    this.setData({ isPlaying: true })
    this.startTimers()
  },

  /** 触摸屏幕：重置活跃时间 */
  onVideoTouch() {
    this.setData({ lastActiveTime: Date.now() })
  },

  /** 上报学习进度（字段名严格对齐后端 ProgressReportDTO） */
  reportProgress() {
    const { taskId, videoId, currentTime, duration, studyDuration } = this.data
    if (!taskId || !videoId) return

    // 计算播放进度百分比
    const progress = duration > 0 ? Math.min(100, Math.floor((currentTime / duration) * 100)) : 0

    post('/study/videos/progress', {
      taskId: Number(taskId),
      videoId: Number(videoId),
      progress: progress,
      lastPosition: currentTime,
      studyDuration: studyDuration
    }, { loading: false, showError: false }).catch(() => {})
  }
})