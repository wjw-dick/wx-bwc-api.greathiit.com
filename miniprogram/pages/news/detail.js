const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    id: null,
    notice: null,
    loading: true
  },

  onLoad(options) {
    this.setData({ id: options.id })
    this.load()
  },

  async load() {
    const code = app.globalData.tenantCode || 'DEMO'
    try {
      const notice = await request({
        url: `/public/tenant/${code}/notices/${this.data.id}`,
        auth: false
      })
      this.setData({
        notice: Object.assign({}, notice, {
          publishTimeText: util.formatDate(notice.publishTime)
        })
      })
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onShareAppMessage() {
    const notice = this.data.notice
    return {
      title: notice ? notice.title : '校园安全资讯',
      path: `/pages/news/detail?id=${this.data.id}`
    }
  }
})
