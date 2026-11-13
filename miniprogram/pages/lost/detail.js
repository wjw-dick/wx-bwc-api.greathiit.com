const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

const CATEGORY_LABEL = {
  CARD: '证件', ELECTRONIC: '电子产品', CLOTHING: '衣物', BOOK: '书籍', OTHER: '其他'
}

Page({
  data: {
    id: null,
    item: null,
    loading: true,
    loggedIn: false,
    claimVisible: false,
    autoClaim: false,
    claimForm: { claimant: '', phone: '', idCard: '', remark: '' },
    idCardMask: '',
    submitting: false,
    error: ''
  },

  onLoad(options) {
    this.setData({
      id: options.id,
      loggedIn: app.isLoggedIn(),
      autoClaim: options.claim === '1'
    })
    const user = app.globalData.user
    if (user && user.realName) {
      this.setData({ 'claimForm.claimant': user.realName })
    }
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      let item = null
      if (app.isLoggedIn()) {
        item = await request({ url: `/lost/items/${this.data.id}`, silent: true })
      }
      if (!item) {
        const code = app.globalData.tenantCode || 'DEMO'
        const rows = await request({ url: `/public/tenant/${code}/lost`, auth: false, silent: true })
        item = (rows || []).find((row) => String(row.id) === String(this.data.id))
      }
      if (!item) throw new Error('物品不存在或已下架')
      this.setData({
        item: Object.assign({}, item, {
          categoryLabel: CATEGORY_LABEL[item.category] || item.category || '其他',
          foundTimeText: util.formatDate(item.foundTime),
          statusText: util.statusLabel(item.status),
          photos: item.photos ? String(item.photos).split(',') : []
        })
      })
      if (this.data.autoClaim) {
        this.openClaim()
      }
    } catch (e) {
      this.setData({ item: null })
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openClaim() {
    if (!app.isLoggedIn()) {
      wx.navigateTo({ url: '/pages/login/index' })
      return
    }
    this.setData({ claimVisible: true, error: '' })
  },

  closeClaim() {
    this.setData({ claimVisible: false })
  },

  noop() {},

  onInput(e) {
    const field = e.currentTarget.dataset.field
    let value = e.detail.value
    if (field === 'idCard') {
      value = String(value).toUpperCase()
      this.setData({ idCardMask: util.mask(value) })
    }
    this.setData({ [`claimForm.${field}`]: value, error: '' })
  },

  async submitClaim() {
    const f = this.data.claimForm
    if (!f.claimant) return this.setData({ error: '请填写认领人姓名' })
    if (!/^1[3-9]\d{9}$/.test(f.phone)) return this.setData({ error: '请输入正确的手机号' })
    if (f.idCard && !/^\d{17}[\dX]$/.test(f.idCard)) return this.setData({ error: '请输入正确的身份证号' })
    this.setData({ submitting: true, error: '' })
    try {
      const data = await request({
        url: '/lost/claims/self',
        method: 'POST',
        data: {
          itemId: Number(this.data.id),
          claimant: f.claimant,
          phone: f.phone,
          idCard: f.idCard || undefined,
          remark: f.remark || undefined
        }
      })
      this.setData({ claimVisible: false })
      wx.showModal({
        title: '认领申请已提交',
        content: `认领编号 #${data.id}，请携带有效证件到保卫处核验领取。`,
        showCancel: false,
        success: () => wx.switchTab({ url: '/pages/lost/index' })
      })
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  preview(e) {
    const current = e.currentTarget.dataset.src
    wx.previewImage({ current, urls: this.data.item.photos })
  }
})
