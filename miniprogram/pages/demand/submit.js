// pages/demand/submit.js - 提交培训需求
//
// 对应后端接口：POST /study/demands
// 请求体字段：title（必填≤100）、content（必填≤1000）、trainingTopic（≤100）、trainingTarget（≤200）
// 提交成功后返回上一页（列表页 onShow 会自动刷新）。
const app = getApp()
const { post } = require('../../utils/request')

Page({
  data: {
    // 表单数据
    form: {
      title: '',
      content: '',
      trainingTopic: '',
      trainingTarget: ''
    },
    // 是否提交中（防止重复点击）
    submitting: false
  },

  onLoad() {
    // 未登录重定向
    if (!app.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/index' })
    }
  },

  /** 输入需求标题 */
  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  /** 输入需求内容 */
  onContentInput(e) {
    this.setData({ 'form.content': e.detail.value })
  },

  /** 输入培训主题 */
  onTopicInput(e) {
    this.setData({ 'form.trainingTopic': e.detail.value })
  },

  /** 输入培训对象 */
  onTargetInput(e) {
    this.setData({ 'form.trainingTarget': e.detail.value })
  },

  /** 提交需求 */
  onSubmit() {
    // 取出表单并去除首尾空白
    const form = {
      title: (this.data.form.title || '').trim(),
      content: (this.data.form.content || '').trim(),
      trainingTopic: (this.data.form.trainingTopic || '').trim(),
      trainingTarget: (this.data.form.trainingTarget || '').trim()
    }

    // 前端基础校验：标题必填且不超过 100 字
    if (!form.title) {
      wx.showToast({ title: '请输入需求标题', icon: 'none' })
      return
    }
    if (form.title.length > 100) {
      wx.showToast({ title: '标题不能超过100字', icon: 'none' })
      return
    }
    // 前端基础校验：内容必填且不超过 1000 字
    if (!form.content) {
      wx.showToast({ title: '请输入需求内容', icon: 'none' })
      return
    }
    if (form.content.length > 1000) {
      wx.showToast({ title: '内容不能超过1000字', icon: 'none' })
      return
    }

    // 防止重复提交
    if (this.data.submitting) return
    this.setData({ submitting: true })

    post('/study/demands', form, { loading: true }).then(() => {
      wx.showToast({ title: '提交成功', icon: 'success' })
      // 提交成功后延迟返回列表页（列表页 onShow 自动刷新）
      setTimeout(() => {
        wx.navigateBack()
      }, 800)
    }).catch(() => {
      // 失败时恢复按钮可用
      this.setData({ submitting: false })
    })
  }
})