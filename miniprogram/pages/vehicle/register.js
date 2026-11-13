const app = getApp()
const { request } = require('../../utils/request')

const STATUS = {
  PENDING: '待审核',
  NORMAL: '已备案',
  APPROVED: '已备案',
  REJECTED: '已驳回'
}
const AUTH = {
  NONE: '未下发',
  PENDING: '下发中',
  SUCCESS: '已下发',
  FAILED: '下发失败'
}

Page({
  data: {
    vehicleTypes: ['小型车', '新能源车', '摩托车', '其他'],
    form: {
      plateNo: '', ownerName: '', phone: '', vehicleTypeIndex: 0,
      validStart: '', validEnd: '', remark: ''
    },
    list: [],
    error: '',
    submitting: false,
    loading: false
  },

  onLoad() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    const user = app.globalData.user || {}
    if (user.realName) this.setData({ 'form.ownerName': user.realName })
  },

  onShow() {
    if (app.isLoggedIn()) this.loadMy()
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    let value = e.detail.value
    if (field === 'plateNo') value = String(value).toUpperCase()
    this.setData({ [`form.${field}`]: value, error: '' })
  },

  onType(e) {
    this.setData({ 'form.vehicleTypeIndex': Number(e.detail.value) })
  },

  onDate(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value })
  },

  async submit() {
    const f = this.data.form
    const plate = String(f.plateNo || '').replace(/\s/g, '').toUpperCase()
    if (!/^[\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}$/.test(plate) && !/^[A-Z0-9]{6,8}$/.test(plate)) {
      return this.setData({ error: '请输入正确的车牌号，如 京A12345' })
    }
    if (!f.ownerName) return this.setData({ error: '请填写车主姓名' })
    if (!/^1[3-9]\d{9}$/.test(f.phone)) return this.setData({ error: '请输入正确的 11 位手机号' })

    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/vehicle/register',
        method: 'POST',
        data: {
          plateNo: plate,
          ownerName: f.ownerName,
          phone: f.phone,
          vehicleType: this.data.vehicleTypes[f.vehicleTypeIndex],
          validStart: f.validStart || undefined,
          validEnd: f.validEnd || undefined,
          remark: f.remark || undefined
        }
      })
      wx.showToast({ title: '备案已提交', icon: 'success' })
      this.setData({
        form: { plateNo: '', ownerName: f.ownerName, phone: '', vehicleTypeIndex: 0, validStart: '', validEnd: '', remark: '' }
      })
      this.loadMy()
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadMy() {
    this.setData({ loading: true })
    try {
      const list = await request({ url: '/vehicle/my', silent: true })
      this.setData({
        list: (list || []).map((item) => Object.assign({}, item, {
          statusText: STATUS[item.status] || item.status || '-',
          authText: AUTH[item.authStatus] || item.authStatus || '-'
        }))
      })
    } catch (e) {
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  }
})
