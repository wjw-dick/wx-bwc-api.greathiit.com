const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'feedback',
    types: ['报修', '意见建议', '安全隐患', '其他'],
    form: { typeIndex: 0, title: '', content: '', contact: '' },
    monitorForm: { reason: '', area: '', timeRange: '' },
    feedbackList: [],
    monitorList: [],
    loading: false,
    submitting: false,
    error: '',
    loggedIn: false
  },

  onShow() {
    const loggedIn = app.isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) {
      this.loadFeedback()
      this.loadMonitor()
    }
  },

  onPullDownRefresh() {
    Promise.all([this.loadFeedback(), this.loadMonitor()]).finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab, error: '' })
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  onMonitorInput(e) {
    this.setData({ [`monitorForm.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  onType(e) {
    this.setData({ 'form.typeIndex': Number(e.detail.value) })
  },

  async loadFeedback() {
    if (!app.isLoggedIn()) return
    this.setData({ loading: true })
    try {
      const data = await request({ url: '/crud/feedback', params: { page: 1, size: 20 }, silent: true })
      this.setData({
        feedbackList: (data.records || []).map((item) => Object.assign({}, item, {
          statusText: util.statusLabel(item.status),
          statusClass: util.statusClass(item.status),
          submitTimeText: util.formatDate(item.submitTime)
        }))
      })
    } catch (e) {
      this.setData({ feedbackList: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadMonitor() {
    if (!app.isLoggedIn()) return
    try {
      const data = await request({ url: '/report/monitor-access', params: { page: 1, size: 20 }, silent: true })
      this.setData({
        monitorList: (data.records || []).map((item) => Object.assign({}, item, {
          statusText: util.statusLabel(item.status),
          statusClass: util.statusClass(item.status)
        }))
      })
    } catch (e) {
      this.setData({ monitorList: [] })
    }
  },

  async submitFeedback() {
    const f = this.data.form
    if (!f.title) return this.setData({ error: '请填写标题' })
    if (!f.content) return this.setData({ error: '请填写内容' })
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/crud/feedback',
        method: 'POST',
        data: {
          type: this.data.types[f.typeIndex],
          title: f.title,
          content: f.content,
          contact: f.contact || undefined,
          submitTime: util.nowString(),
          status: 'PENDING'
        }
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      this.setData({ form: { typeIndex: 0, title: '', content: '', contact: '' } })
      this.loadFeedback()
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async submitMonitor() {
    const f = this.data.monitorForm
    if (!f.reason) return this.setData({ error: '请填写调取事由' })
    if (!f.area) return this.setData({ error: '请填写调取区域' })
    if (!f.timeRange) return this.setData({ error: '请填写调取时段' })
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/report/monitor-access',
        method: 'POST',
        data: {
          applicant: (app.globalData.user && app.globalData.user.realName) || '',
          applicantId: app.globalData.user && app.globalData.user.id,
          reason: f.reason,
          area: f.area,
          timeRange: f.timeRange
        }
      })
      wx.showToast({ title: '申请已提交', icon: 'success' })
      this.setData({ monitorForm: { reason: '', area: '', timeRange: '' } })
      this.loadMonitor()
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  }
})
