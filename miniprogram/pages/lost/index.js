const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

const CATEGORIES = [
  { value: '', label: '全部' },
  { value: 'CARD', label: '证件' },
  { value: 'ELECTRONIC', label: '电子产品' },
  { value: 'CLOTHING', label: '衣物' },
  { value: 'BOOK', label: '书籍' },
  { value: 'OTHER', label: '其他' }
]

const CATEGORY_LABEL = {
  CARD: '证件', ELECTRONIC: '电子产品', CLOTHING: '衣物', BOOK: '书籍', OTHER: '其他'
}

Page({
  data: {
    loggedIn: false,
    tab: 'wall',
    categories: CATEGORIES,
    category: '',
    keyword: '',
    list: [],
    claims: [],
    loading: false
  },

  onShow() {
    this.setData({ loggedIn: app.isLoggedIn() })
    this.load()
    if (app.isLoggedIn()) this.loadClaims()
  },

  onPullDownRefresh() {
    Promise.all([this.load(), this.data.loggedIn ? this.loadClaims() : Promise.resolve()])
      .finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    if (e.currentTarget.dataset.tab === 'claims' && !app.isLoggedIn()) {
      wx.navigateTo({ url: '/pages/login/index' })
    }
  },

  onCategory(e) {
    this.setData({ category: e.currentTarget.dataset.value })
    this.load()
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },

  search() {
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      const code = app.globalData.tenantCode || 'DEMO'
      const params = {}
      if (this.data.category) params.category = this.data.category
      if (this.data.keyword) params.keyword = this.data.keyword
      const rows = await request({
        url: `/public/tenant/${code}/lost`,
        params,
        auth: false,
        silent: true
      })
      this.setData({
        list: (rows || []).map((item) => Object.assign({}, item, {
          categoryLabel: CATEGORY_LABEL[item.category] || item.category || '其他',
          foundTimeText: util.formatDate(item.foundTime),
          statusText: util.statusLabel(item.status),
          cover: item.photos ? String(item.photos).split(',')[0] : ''
        }))
      })
    } catch (e) {
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadClaims() {
    try {
      const rows = await request({ url: '/lost/claims/self', silent: true })
      this.setData({
        claims: (rows || []).map((item) => Object.assign({}, item, {
          statusText: util.statusLabel(item.status),
          statusClass: util.statusClass(item.status),
          claimTimeText: util.formatDate(item.claimTime)
        }))
      })
    } catch (e) {
      this.setData({ claims: [] })
    }
  },

  openDetail(e) {
    wx.navigateTo({ url: `/pages/lost/detail?id=${e.currentTarget.dataset.id}` })
  },

  goClaim(e) {
    if (!app.isLoggedIn()) {
      wx.showToast({ title: '请先登录后认领', icon: 'none' })
      setTimeout(() => wx.navigateTo({ url: '/pages/login/index' }), 500)
      return
    }
    wx.navigateTo({ url: `/pages/lost/detail?id=${e.currentTarget.dataset.id}&claim=1` })
  },

  goReport() {
    wx.navigateTo({ url: '/pages/lost/report' })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  }
})
