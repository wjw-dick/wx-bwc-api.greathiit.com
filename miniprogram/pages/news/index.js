const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    notices: [],
    warnings: [],
    loading: false,
    category: ''
  },

  onShow() {
    this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    const code = app.globalData.tenantCode || 'DEMO'
    this.setData({ loading: true })
    try {
      const [notices, warnings] = await Promise.all([
        request({ url: `/public/tenant/${code}/notices`, params: { limit: 50 }, auth: false, silent: true }),
        request({ url: `/public/tenant/${code}/warnings`, auth: false, silent: true })
      ])
      this.setData({
        notices: (notices || []).map((item) => Object.assign({}, item, {
          publishTimeText: util.formatDate(item.publishTime)
        })),
        warnings: (warnings || []).map((item) => Object.assign({}, item, {
          publishTimeText: util.formatDate(item.publishTime)
        }))
      })
    } catch (e) {
      // 保留空态
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(e) {
    wx.navigateTo({ url: `/pages/news/detail?id=${e.currentTarget.dataset.id}` })
  }
})
