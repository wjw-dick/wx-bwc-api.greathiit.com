const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    loggedIn: false,
    tab: '',
    tabs: [
      { value: '', label: '全部' },
      { value: 0, label: '未读' },
      { value: 1, label: '已读' }
    ],
    list: [],
    total: 0,
    unread: 0,
    loading: false,
    detail: null,
    detailVisible: false
  },

  onShow() {
    const loggedIn = app.isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.value })
    this.load()
  },

  async load() {
    if (!app.isLoggedIn()) return
    this.setData({ loading: true })
    try {
      const params = { page: 1, size: 50 }
      if (this.data.tab !== '') params.readFlag = this.data.tab
      const [data, unreadPage] = await Promise.all([
        request({ url: '/system/messages', params, silent: true }),
        request({ url: '/system/messages', params: { page: 1, size: 1, readFlag: 0 }, silent: true })
      ])
      const rows = (data.records || []).map((item) => Object.assign({}, item, {
        timeText: util.formatDate(item.createTime),
        readText: item.readFlag === 1 ? '已读' : '未读',
        readClass: item.readFlag === 1 ? 'muted' : 'warn'
      }))
      this.setData({ list: rows, total: data.total || rows.length, unread: unreadPage.total || 0 })
    } catch (e) {
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  open(e) {
    const id = e.currentTarget.dataset.id
    const message = this.data.list.find((item) => item.id === id)
    if (!message) return
    this.setData({ detail: message, detailVisible: true })
    if (message.readFlag !== 1) this.markRead(id)
  },

  closeDetail() {
    this.setData({ detailVisible: false })
  },

  noop() {},

  async markRead(id) {
    try {
      await request({ url: `/system/messages/${id}/read`, method: 'POST', silent: true })
      const list = this.data.list.map((item) => item.id === id
        ? Object.assign({}, item, { readFlag: 1, readText: '已读', readClass: 'muted' })
        : item)
      this.setData({ list })
      this.load()
    } catch (e) {
      // 无权限或已读
    }
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  }
})
