const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    routes: [],
    routeIndex: 0,
    routeName: '',
    points: [],
    records: [],
    selectedPointId: 0,
    remark: '',
    loading: false,
    submitting: false,
    error: ''
  },

  onShow() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    this.loadRoutes()
    this.loadRecords()
  },

  onPullDownRefresh() {
    Promise.all([this.loadRoutes(), this.loadRecords()]).finally(() => wx.stopPullDownRefresh())
  },

  async loadRoutes() {
    try {
      const routes = await request({ url: '/patrol/routes', silent: true })
      this.setData({ routes: routes || [], routeName: (routes && routes[0] && routes[0].name) || '' })
      if ((routes || []).length) {
        this.loadPoints(routes[0].id)
      }
    } catch (e) {
      this.setData({ routes: [], points: [] })
    }
  },

  async loadPoints(routeId) {
    try {
      const points = await request({ url: '/patrol/points', params: { routeId }, silent: true })
      this.setData({ points: points || [], selectedPointId: 0 })
    } catch (e) {
      this.setData({ points: [] })
    }
  },

  onRoute(e) {
    const index = Number(e.detail.value)
    const route = this.data.routes[index]
    this.setData({ routeIndex: index, routeName: route ? route.name : '' })
    if (route) this.loadPoints(route.id)
  },

  selectPoint(e) {
    this.setData({ selectedPointId: Number(e.currentTarget.dataset.id), error: '' })
  },

  onRemark(e) {
    this.setData({ remark: e.detail.value, error: '' })
  },

  scanPoint() {
    wx.scanCode({
      success: (res) => {
        const point = this.data.points.find((item) => item.qrCode === res.result)
        if (!point) {
          return wx.showToast({ title: '未匹配到本路线点位', icon: 'none' })
        }
        this.checkin(point)
      }
    })
  },

  checkinSelected() {
    const point = this.data.points.find((item) => item.id === this.data.selectedPointId)
    if (!point) return this.setData({ error: '请先选择巡逻点位' })
    this.checkin(point)
  },

  checkin(point) {
    this.setData({ submitting: true, error: '' })
    const submit = (location) => {
      request({
        url: '/patrol/checkin',
        method: 'POST',
        data: {
          pointId: point.id,
          method: 'GPS',
          longitude: location ? location.longitude : undefined,
          latitude: location ? location.latitude : undefined,
          abnormalDesc: this.data.remark || undefined
        }
      }).then(() => {
        wx.showToast({ title: '打卡成功', icon: 'success' })
        this.setData({ selectedPointId: 0, remark: '' })
        this.loadRecords()
      }).catch((e) => {
        this.setData({ error: e.message || '打卡失败' })
      }).finally(() => {
        this.setData({ submitting: false })
      })
    }
    wx.getLocation({
      type: 'gcj02',
      success: (res) => submit(res),
      fail: () => submit(null)
    })
  },

  async loadRecords() {
    try {
      const user = app.globalData.user
      const data = await request({
        url: '/patrol/records',
        params: { page: 1, size: 20, guardId: user ? user.id : undefined },
        silent: true
      })
      this.setData({
        records: (data.records || []).map((item) => Object.assign({}, item, {
          statusText: util.statusLabel(item.status),
          statusClass: util.statusClass(item.status),
          checkTimeText: util.formatDate(item.checkTime)
        }))
      })
    } catch (e) {
      this.setData({ records: [] })
    }
  }
})
