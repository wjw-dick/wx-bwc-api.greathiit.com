const app = getApp()
const { request } = require('../../utils/request')
const subscribe = require('../../utils/subscribe')
const util = require('../../utils/util')

Page({
  data: {
    reasons: ['公务来访', '家长来访', '学术交流', '面试应聘', '物流配送', '其他'],
    form: {
      visitorName: '', phone: '', idCard: '', reasonIndex: 0, visitDate: '', visitClock: '',
      leaveDate: '', leaveClock: '', accompanyCount: 0, plateNo: '', visitUserName: '', remark: ''
    },
    idCardMask: '',
    error: '',
    submitting: false,
    success: false,
    result: null,
    today: ''
  },

  onLoad() {
    const d = new Date()
    const date = `${d.getFullYear()}-${util.pad(d.getMonth() + 1)}-${util.pad(d.getDate())}`
    this.setData({ today: date })
    this.setField('visitDate', date)
    this.setField('visitClock', `${util.pad(d.getHours() + 1)}:00`)
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    let value = e.detail.value
    if (field === 'idCard') {
      value = String(value).toUpperCase()
      this.setData({ idCardMask: util.mask(value) })
    }
    this.setField(field, value)
  },

  setField(field, value) {
    this.setData({ [`form.${field}`]: value, error: '' })
  },

  onReason(e) {
    this.setData({ 'form.reasonIndex': Number(e.detail.value) })
  },

  onDate(e) {
    this.setField(e.currentTarget.dataset.field, e.detail.value)
  },

  async submit() {
    const f = this.data.form
    if (!f.visitorName) return this.setData({ error: '请填写访客姓名' })
    if (!/^1[3-9]\d{9}$/.test(f.phone)) return this.setData({ error: '请输入正确的 11 位手机号' })
    if (f.idCard && !/^\d{17}[\dX]$/.test(f.idCard)) return this.setData({ error: '请输入正确的 18 位身份证号' })
    if (!f.visitDate || !f.visitClock) return this.setData({ error: '请选择来访时间' })

    const visitTime = `${f.visitDate} ${f.visitClock}:00`
    const leaveTime = f.leaveDate && f.leaveClock ? `${f.leaveDate} ${f.leaveClock}:00` : undefined
    this.setData({ submitting: true, error: '' })
    try {
      const data = await request({
        url: '/visitor/appointments',
        method: 'POST',
        data: {
          visitorName: f.visitorName,
          phone: f.phone,
          idCard: f.idCard || undefined,
          reason: this.data.reasons[f.reasonIndex],
          visitTime,
          leaveTime,
          accompanyCount: Number(f.accompanyCount || 0),
          plateNo: f.plateNo || undefined,
          visitUserId: (app.globalData.user && app.globalData.user.id) || undefined,
          visitUserName: f.visitUserName || (app.globalData.user && app.globalData.user.realName) || undefined,
          remark: f.remark || undefined,
          source: 'MINI'
        }
      })
      this.setData({
        success: true,
        result: Object.assign({}, data, {
          phoneMask: data.phoneMask || util.mask(f.phone),
          idCardMask: data.idCardMask || util.mask(f.idCard)
        })
      })
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  again() {
    wx.navigateBack()
  },

  goList() {
    wx.switchTab({ url: '/pages/visitor/list' })
  },

  async subscribeNotice() {
    const result = await subscribe.requestSubscribe(['VISITOR_APPROVE', 'TODO'])
    if (result.ok) {
      wx.showToast({ title: '已开启审批通知', icon: 'success' })
    } else {
      wx.showToast({ title: result.reason || '未开启通知', icon: 'none' })
    }
  }
})
