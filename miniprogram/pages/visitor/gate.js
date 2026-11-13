const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'check',
    checkCode: '',
    checking: false,
    tempForm: { visitorName: '', phone: '', idCard: '', reason: '', visitUser: '' },
    tempList: [],
    lateForm: { studentName: '', dorm: '', typeIndex: 0, reason: '' },
    lateTypes: ['晚归', '不归'],
    lateList: [],
    loading: false,
    submitting: false,
    error: ''
  },

  onShow() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    this.loadTemp()
    this.loadLate()
  },

  onPullDownRefresh() {
    Promise.all([this.loadTemp(), this.loadLate()]).finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab, error: '' })
  },

  onCheckInput(e) {
    this.setData({ checkCode: e.detail.value, error: '' })
  },

  scanCheck() {
    wx.scanCode({
      success: (res) => {
        this.setData({ checkCode: res.result })
        this.checkPass(res.result)
      }
    })
  },

  submitCheck() {
    if (!this.data.checkCode) return this.setData({ error: '请输入或扫描通行码' })
    this.checkPass(this.data.checkCode)
  },

  async checkPass(passCode) {
    this.setData({ checking: true, error: '' })
    try {
      const result = await request({
        url: '/visitor/check',
        method: 'POST',
        data: { passCode, gate: '小程序门岗', method: 'QR' }
      })
      wx.showModal({
        title: result.direction === 'IN' ? '入校核验通过' : '离校核验通过',
        content: `${result.visitorName || ''} ${result.message || ''}`,
        showCancel: false
      })
      this.setData({ checkCode: '' })
    } catch (err) {
      this.setData({ error: err.message || '核验失败' })
    } finally {
      this.setData({ checking: false })
    }
  },

  onTempInput(e) {
    this.setData({ [`tempForm.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  async submitTemp() {
    const f = this.data.tempForm
    if (!f.visitorName) return this.setData({ error: '请填写访客姓名' })
    if (f.phone && !/^1[3-9]\d{9}$/.test(f.phone)) return this.setData({ error: '请输入正确的手机号' })
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/visitor/temp-entry',
        method: 'POST',
        data: {
          visitorName: f.visitorName,
          phone: f.phone || undefined,
          idCard: f.idCard || undefined,
          reason: f.reason || undefined,
          visitUser: f.visitUser || undefined
        }
      })
      wx.showToast({ title: '已登记入校', icon: 'success' })
      this.setData({ tempForm: { visitorName: '', phone: '', idCard: '', reason: '', visitUser: '' } })
      this.loadTemp()
    } catch (e) {
      this.setData({ error: e.message || '登记失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadTemp() {
    try {
      const data = await request({ url: '/visitor/temp-entries', params: { page: 1, size: 20, status: 'IN' }, silent: true })
      this.setData({
        tempList: (data.records || []).map((item) => Object.assign({}, item, {
          inTimeText: util.formatDate(item.inTime)
        }))
      })
    } catch (e) {
      this.setData({ tempList: [] })
    }
  },

  outTemp(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '离校登记',
      content: '确认该访客已离校？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request({ url: `/visitor/temp-entry/${id}/out`, method: 'POST' })
          wx.showToast({ title: '已登记离校', icon: 'success' })
          this.loadTemp()
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    })
  },

  onLateInput(e) {
    this.setData({ [`lateForm.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  onLateType(e) {
    this.setData({ 'lateForm.typeIndex': Number(e.detail.value) })
  },

  async submitLate() {
    const f = this.data.lateForm
    if (!f.studentName) return this.setData({ error: '请填写学生姓名' })
    if (!f.dorm) return this.setData({ error: '请填写宿舍/班级' })
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/visitor/late-return',
        method: 'POST',
        data: {
          studentName: f.studentName,
          dorm: f.dorm,
          type: this.data.lateTypes[f.typeIndex],
          reason: f.reason || undefined
        }
      })
      wx.showToast({ title: '已登记', icon: 'success' })
      this.setData({ lateForm: { studentName: '', dorm: '', typeIndex: 0, reason: '' } })
      this.loadLate()
    } catch (e) {
      this.setData({ error: e.message || '登记失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadLate() {
    try {
      const data = await request({ url: '/visitor/late-returns', params: { page: 1, size: 20, confirmStatus: 'PENDING' }, silent: true })
      this.setData({
        lateList: (data.records || []).map((item) => Object.assign({}, item, {
          recordTimeText: util.formatDate(item.recordTime)
        }))
      })
    } catch (e) {
      this.setData({ lateList: [] })
    }
  },

  confirmLate(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认晚归',
      content: '确认已核实该学生晚归情况？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request({ url: `/visitor/late-return/${id}/confirm`, method: 'POST' })
          wx.showToast({ title: '已确认', icon: 'success' })
          this.loadLate()
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    })
  }
})
