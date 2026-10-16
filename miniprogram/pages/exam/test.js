// pages/exam/test.js - 在线答题（倒计时 / 答题卡 / 交卷判分）
const { get, post } = require('../../utils/request')

// 选项字母表：单选/多选的选项按顺序映射为 A/B/C...
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'

Page({
  data: {
    // 考试记录ID（答题页以记录ID为主键）
    recordId: null,
    // 任务ID
    taskId: null,
    // 加载中
    loading: false,
    // 交卷中
    submitting: false,
    // 试卷信息
    paperName: '',
    totalScore: 0,
    passScore: 0,
    duration: 0,
    // 题目列表（含选项与作答状态）
    questions: [],
    // 当前题目下标
    currentIndex: 0,
    // 当前题目对象
    currentQuestion: null,
    // 已作答题目数
    answeredCount: 0,
    // 剩余答题秒数与展示文本
    remainSeconds: 0,
    countdownText: '00:00'
  },

  // 倒计时定时器
  _timer: null,
  // 是否已交卷（阻止离开确认）
  _submitted: false,

  onLoad(options) {
    this.setData({
      recordId: options.recordId,
      taskId: options.taskId || null
    })
    this.loadExam()
  },

  onUnload() {
    // 离开页面清理倒计时
    this.stopTimer()
  },

  /** 加载考试数据（进入答题 / 刷新恢复） */
  loadExam() {
    this.setData({ loading: true })
    get('/study/exams/records/' + this.data.recordId, {}, { loading: false }).then(res => {
      const data = res.data || {}

      // 构造题目展示结构
      const questions = (data.questions || []).map(q => this.decorateQuestion(q))

      // 恢复已作答内容（用于刷新后继续作答）
      ;(data.savedAnswers || []).forEach(item => {
        const q = questions.find(x => x.id === item.questionId)
        if (!q) return
        if (q.questionType === 2) {
          // 多选：字符串拆分为字母数组
          const letters = String(item.answer || '').split('').filter(c => LETTERS.indexOf(c) >= 0)
          q.userAnswer = letters
          q.options.forEach(o => {
            o.selected = letters.indexOf(o.value) >= 0
          })
        } else {
          // 其它题型：直接回填字符串
          q.userAnswer = item.answer || ''
          q.options.forEach(o => {
            o.selected = o.value === q.userAnswer
          })
        }
      })

      // 倒计时以服务端剩余秒数为准
      const remain = Number(data.remainSeconds) || (Number(data.duration) || 0) * 60

      this.setData({
        paperName: data.paperName || '在线考试',
        totalScore: data.totalScore || 0,
        passScore: data.passScore || 0,
        duration: data.duration || 0,
        questions: questions,
        remainSeconds: remain,
        countdownText: this.formatCountdown(remain),
        loading: false
      })

      // 刷新题目状态并启动倒计时
      this.syncQuestions(questions, this.data.currentIndex)
      this.startTimer()
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /**
   * 构造单题的展示结构
   * - 判断题（type=3）固定「正确 / 错误」两项
   * - 单选/多选使用后端选项，字母按顺序生成
   */
  decorateQuestion(q) {
    const isJudge = q.questionType === 3
    const options = isJudge
      ? [
          { label: '√', value: '正确', text: '正确', selected: false },
          { label: '×', value: '错误', text: '错误', selected: false }
        ]
      : (q.options || []).map((o, i) => ({
          label: LETTERS[i] || '',
          value: LETTERS[i] || '',
          text: o.text,
          selected: false
        }))

    return {
      id: q.id,
      questionType: q.questionType,
      typeName: q.typeName,
      content: q.content,
      score: q.score,
      sort: q.sort,
      options: options,
      // 单选题/判断题/填空题/简答题为字符串；多选为字母数组
      userAnswer: q.questionType === 2 ? [] : ''
    }
  },

  /** 同步题目与当前题状态（更新已答数、当前题对象） */
  syncQuestions(questions, currentIndex) {
    // 统计已作答题目数
    const answeredCount = questions.filter(q => this.isAnswered(q)).length
    this.setData({
      questions: questions,
      currentIndex: currentIndex,
      currentQuestion: questions[currentIndex] || null,
      answeredCount: answeredCount
    })
  },

  /** 判断某题是否已作答 */
  isAnswered(q) {
    if (Array.isArray(q.userAnswer)) {
      return q.userAnswer.length > 0
    }
    return String(q.userAnswer === undefined || q.userAnswer === null ? '' : q.userAnswer).trim() !== ''
  },

  /** 单选/判断题：点击选项（互斥） */
  onOptionTap(e) {
    const qi = e.currentTarget.dataset.qi
    const oi = e.currentTarget.dataset.oi
    const questions = this.data.questions.slice()
    const q = Object.assign({}, questions[qi])

    // 重新构造选项，仅当前项选中
    const options = q.options.map((o, i) => Object.assign({}, o, { selected: i === oi }))
    q.options = options
    q.userAnswer = options[oi].value
    questions[qi] = q

    this.syncQuestions(questions, this.data.currentIndex)
  },

  /** 多选题：勾选/取消选项 */
  onMultiTap(e) {
    const qi = e.currentTarget.dataset.qi
    const oi = e.currentTarget.dataset.oi
    const questions = this.data.questions.slice()
    const q = Object.assign({}, questions[qi])

    // 切换该选项选中态
    const options = q.options.map((o, i) => (i === oi ? Object.assign({}, o, { selected: !o.selected }) : Object.assign({}, o)))
    q.options = options
    // 作答值 = 已选中选项字母数组
    q.userAnswer = options.filter(o => o.selected).map(o => o.value)
    questions[qi] = q

    this.syncQuestions(questions, this.data.currentIndex)
  },

  /** 填空/简答题：输入文本 */
  onTextInput(e) {
    const qi = e.currentTarget.dataset.qi
    const questions = this.data.questions.slice()
    const q = Object.assign({}, questions[qi])
    q.userAnswer = e.detail.value
    questions[qi] = q

    this.syncQuestions(questions, this.data.currentIndex)
  },

  /** 答题卡跳题 */
  onCardTap(e) {
    const index = e.currentTarget.dataset.index
    this.syncQuestions(this.data.questions, index)
  },

  /** 上一题 */
  onPrev() {
    if (this.data.currentIndex > 0) {
      this.syncQuestions(this.data.questions, this.data.currentIndex - 1)
    }
  },

  /** 下一题 */
  onNext() {
    if (this.data.currentIndex < this.data.questions.length - 1) {
      this.syncQuestions(this.data.questions, this.data.currentIndex + 1)
    }
  },

  /** 组装交卷数据（多选字母升序拼接，与后端判分口径一致） */
  buildAnswers() {
    return this.data.questions.map(q => {
      let answer = ''
      if (Array.isArray(q.userAnswer)) {
        answer = q.userAnswer.slice().sort().join('')
      } else if (q.userAnswer !== undefined && q.userAnswer !== null) {
        answer = String(q.userAnswer)
      }
      return { questionId: q.id, answer: answer }
    })
  },

  /** 交卷 */
  onSubmit() {
    this.doSubmit(false)
  },

  /**
   * 执行交卷
   * @param {boolean} auto 是否为超时自动交卷
   */
  doSubmit(auto) {
    if (this.data.submitting) return

    const unanswered = this.data.questions.length - this.data.answeredCount
    const tip = unanswered > 0
      ? '还有 ' + unanswered + ' 题未作答，确认交卷吗？'
      : '确认提交试卷吗？提交后不可修改。'

    // 手动交卷需二次确认
    if (!auto) {
      wx.showModal({
        title: '交卷确认',
        content: tip,
        confirmText: '确认交卷',
        cancelText: '继续答题',
        success: (res) => {
          if (res.confirm) {
            this.submitRequest(auto)
          }
        }
      })
      return
    }
    this.submitRequest(auto)
  },

  /** 发送交卷请求 */
  submitRequest(auto) {
    this.setData({ submitting: true })
    this.stopTimer()

    post('/study/exams/records/' + this.data.recordId + '/submit', {
      answers: this.buildAnswers()
    }, { loading: true, showError: true }).then(() => {
      this._submitted = true
      wx.showToast({ title: auto ? '时间到，已自动交卷' : '交卷成功', icon: 'success' })
      // 跳转成绩页（用 redirectTo 替换当前页，避免返回时回到答题页）
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/exam/result?recordId=' + this.data.recordId })
      }, 800)
    }).catch(() => {
      // 交卷失败恢复倒计时，允许重新提交
      this.setData({ submitting: false })
      if (!auto) {
        this.startTimer()
      }
    })
  },

  /** 启动倒计时 */
  startTimer() {
    this.stopTimer()
    this._timer = setInterval(() => {
      const remain = this.data.remainSeconds - 1
      if (remain <= 0) {
        this.stopTimer()
        this.setData({ remainSeconds: 0, countdownText: '00:00' })
        // 时间到自动交卷
        this.doSubmit(true)
        return
      }
      this.setData({
        remainSeconds: remain,
        countdownText: this.formatCountdown(remain)
      })
    }, 1000)
  },

  /** 停止倒计时 */
  stopTimer() {
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
  },

  /** 秒数 → 倒计时文本（HH:mm:ss / mm:ss） */
  formatCountdown(seconds) {
    const total = Math.max(0, Math.floor(Number(seconds) || 0))
    const pad = (n) => String(n).padStart(2, '0')
    const h = Math.floor(total / 3600)
    const m = Math.floor((total % 3600) / 60)
    const s = total % 60
    return h > 0 ? pad(h) + ':' + pad(m) + ':' + pad(s) : pad(m) + ':' + pad(s)
  }
})