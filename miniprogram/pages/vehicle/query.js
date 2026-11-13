const app = getApp()
const { request } = require('../../utils/request')

const STATUS = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}
const AUTH = {
  NONE: '未下发',
  PENDING: '下发中',
  SUCCESS: '已下发',
  FAILED: '下发失败'
}

Page({
  data: {
    form: { id: '', phone: '' },
    loading: false,
    error: '',
    result: null,
    statusText: '',
    authText: ''
  },

  onLoad(options) {
    if (options.id) this.setData({ 'form.id': String(options.id) })
    if (options.phone) this.setData({ 'form.phone': String(options.phone) })
    if (options.id && options.phone) this.query()
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  async query() {
    const f = this.data.form
    if (!/^\d+$/.test(String(f.id || '').trim())) {
      return this.setData({ error: '请输入预约成功后的数字编号' })
    }
    if (!/^1[3-9]\d{9}$/.test(f.phone)) {
      return this.setData({ error: '请输入预约时填写的手机号' })
    }
    this.setData({ loading: true, error: '', result: null })
    try {
      const code = app.globalData.tenantCode || 'DEMO'
      const data = await request({
        url: `/public/tenant/${code}/vehicle-appointment/query`,
        auth: false,
        params: { id: String(f.id).trim(), phone: f.phone }
      })
      this.setData({
        result: data,
        statusText: STATUS[data.status] || data.status || '-',
        authText: AUTH[data.authStatus] || data.authStatus || '-'
      })
    } catch (e) {
      this.setData({ error: e.message || '查询失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
