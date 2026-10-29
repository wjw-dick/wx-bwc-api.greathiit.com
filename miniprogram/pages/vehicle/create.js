const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    reasons: ['公务车辆', '家长接送', '物流配送', '施工维修', '访客车辆', '其他'],
    form: {
      plateNo: '', visitorName: '', phone: '', reasonIndex: 0,
      visitDate: '', visitClock: '', leaveDate: '', leaveClock: '', remark: ''
    },
    error: '',
    submitting: false,
    success: false,
    result: null
  },

  onLoad() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    const d = new Date()
    const date = `${d.getFullYear()}-${util.pad(d.getMonth() + 1)}-${util.pad(d.getDate())}`
    this.setField('visitDate', date)
    this.setField('visitClock', `${util.pad(Math.min(d.getHours() + 1, 23))}:00`)
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    let value = e.detail.value
    if (field === 'plateNo') value = String(value).toUpperCase()
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
    const plate = String(f.plateNo || '').replace(/\s/g, '').toUpperCase()
    if (!/^[\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}$/.test(plate) && !/^[A-Z0-9]{6,8}$/.test(plate)) {
      return this.setData({ error: '请输入正确的车牌号，如 京A12345' })
    }
    if (!f.visitorName) return this.setData({ error: '请填写驾驶人姓名' })
    if (!/^1[3-9]\d{9}$/.test(f.phone)) return this.setData({ error: '请输入正确的 11 位手机号' })
    if (!f.visitDate || !f.visitClock) return this.setData({ error: '请选择入校时间' })

    const visitTime = `${f.visitDate} ${f.visitClock}:00`
    const leaveTime = f.leaveDate && f.leaveClock ? `${f.leaveDate} ${f.leaveClock}:00` : undefined
    this.setData({ submitting: true, error: '' })
    try {
      const data = await request({
        url: '/vehicle/appointments',
        method: 'POST',
        data: {
          plateNo: plate,
          visitorName: f.visitorName,
          phoneMask: util.mask(f.phone),
          reason: this.data.reasons[f.reasonIndex],
          visitTime,
          leaveTime,
          remark: f.remark || undefined
        }
      })
      this.setData({
        success: true,
        result: Object.assign({}, data, {
          plateNo: data.plateNo || plate,
          phoneMask: data.phoneMask || util.mask(f.phone),
          visitTime: data.visitTime || visitTime
        })
      })
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/index/index' }) })
  }
})
