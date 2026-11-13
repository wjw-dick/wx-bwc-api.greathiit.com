const app = getApp()
const { request } = require('../../utils/request')

Page({
  data: {
    keyword: '',
    logs: [],
    inPark: [],
    loading: false,
    error: ''
  },

  onLoad() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    this.load()
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    this.load()
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [page, inPark] = await Promise.all([
        request({
          url: '/vehicle/logs',
          params: { page: 1, size: 30, plateNo: this.data.keyword || undefined },
          silent: true
        }),
        request({ url: '/vehicle/in-park', silent: true })
      ])
      this.setData({
        logs: (page && page.records) || [],
        inPark: inPark || []
      })
    } catch (e) {
      this.setData({ error: e.message || '加载失败', logs: [], inPark: [] })
    } finally {
      this.setData({ loading: false })
    }
  }
})
