const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    categories: [
      { value: 'CARD', label: '证件' },
      { value: 'ELECTRONIC', label: '电子产品' },
      { value: 'CLOTHING', label: '衣物' },
      { value: 'BOOK', label: '书籍' },
      { value: 'OTHER', label: '其他' }
    ],
    form: {
      name: '', categoryIndex: 0, location: '', foundDate: '',
      description: '', finder: '', storageLocation: ''
    },
    submitting: false,
    success: false,
    result: null,
    error: ''
  },

  onLoad() {
    const d = new Date()
    this.setData({ 'form.foundDate': `${d.getFullYear()}-${util.pad(d.getMonth() + 1)}-${util.pad(d.getDate())}` })
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  onCategory(e) {
    this.setData({ 'form.categoryIndex': Number(e.detail.value) })
  },

  onDate(e) {
    this.setData({ 'form.foundDate': e.detail.value })
  },

  async submit() {
    const f = this.data.form
    if (!f.name) return this.setData({ error: '请填写物品名称' })
    if (!f.location) return this.setData({ error: '请填写拾获地点' })
    this.setData({ submitting: true, error: '' })
    try {
      const data = await request({
        url: `/public/tenant/${app.globalData.tenantCode || 'DEMO'}/lost-report`,
        method: 'POST',
        auth: false,
        data: {
          name: f.name,
          category: this.data.categories[f.categoryIndex].value,
          location: f.location,
          foundTime: f.foundDate ? `${f.foundDate} 00:00:00` : undefined,
          description: f.description || undefined,
          finder: f.finder || undefined,
          storageLocation: f.storageLocation || undefined
        }
      })
      this.setData({ success: true, result: data })
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  goWall() {
    wx.switchTab({ url: '/pages/lost/index' })
  },

  again() {
    this.setData({ success: false, result: null })
  }
})
