const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    records: [],
    submitting: false,
    error: '',
    today: ''
  },

  onShow() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    const now = new Date()
    this.setData({
      today: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`,
      nowTime: `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    })
    this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async check(type) {
    this.setData({ submitting: true, error: '' })
    const user = app.globalData.user || {}
    try {
      await request({
        url: '/logistics/attendance/check',
        method: 'POST',
        data: {
          guardId: user.id,
          guardName: user.realName,
          type,
          method: 'GPS',
          status: 'NORMAL'
        }
      })
      wx.showToast({ title: type === 'ON' ? '上班打卡成功' : '下班打卡成功', icon: 'success' })
      this.load()
    } catch (e) {
      this.setData({ error: e.message || '打卡失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  onCheckIn() {
    this.check('ON')
  },

  onCheckOut() {
    this.check('OFF')
  },

  async load() {
    const user = app.globalData.user || {}
    try {
      const data = await request({
        url: '/logistics/attendance',
        params: { page: 1, size: 20, guardId: user.id },
        silent: true
      })
      this.setData({
        records: (data.records || []).map((item) => Object.assign({}, item, {
          checkTimeText: util.formatDate(item.checkTime),
          typeText: item.type === 'OFF' ? '下班' : '上班'
        }))
      })
    } catch (e) {
      this.setData({ records: [] })
    }
  }
})
