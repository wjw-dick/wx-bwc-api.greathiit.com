const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    locale: 'zh-CN',
    loggedIn: false,
    user: null,
    tenant: null,
    stats: null,
    notices: [],
    warnings: [],
    loading: true,
    quickLinks: [
      { key: 'visitor', title: '访客预约', icon: '访', path: '/pages/visitor/create' },
      { key: 'vehicle', title: '车辆预约', icon: '车', path: '/pages/vehicle/create' },
      { key: 'lost', title: '失物招领', icon: '失', path: '/pages/lost/index' },
      { key: 'report', title: '报修反馈', icon: '报', path: '/pages/report/index' },
      { key: 'education', title: '安全教育', icon: '学', path: '/pages/education/index' },
      { key: 'approval', title: '审批中心', icon: '审', path: '/pages/approval/list' },
      { key: 'news', title: '安全资讯', icon: '讯', path: '/pages/news/index' },
      { key: 'message', title: '消息中心', icon: '信', path: '/pages/message/index' }
    ]
  },

  onShow() {
    const locale = app.globalData.locale || 'zh-CN'
    const user = app.globalData.user
    const tenant = app.globalData.tenant
    this.setData({
      locale,
      loggedIn: app.isLoggedIn(),
      user,
      tenant,
      notices: this.data.notices,
      warnings: this.data.warnings
    })
    this.loadPublic()
    if (app.isLoggedIn()) this.loadDashboard()
    app.applyTabBar()
  },

  onPullDownRefresh() {
    Promise.all([this.loadPublic(), app.isLoggedIn() ? this.loadDashboard() : Promise.resolve()])
      .finally(() => wx.stopPullDownRefresh())
  },

  async loadPublic() {
    const code = app.globalData.tenantCode || 'DEMO'
    this.setData({ loading: true })
    try {
      const [tenant, notices, warnings] = await Promise.all([
        request({ url: `/public/tenant/${code}`, auth: false }),
        request({ url: `/public/tenant/${code}/notices`, params: { limit: 5 }, auth: false, silent: true }),
        request({ url: `/public/tenant/${code}/warnings`, auth: false, silent: true })
      ])
      app.globalData.tenant = tenant
      wx.setStorageSync('bwc_tenant', tenant)
      this.setData({ tenant, notices: notices || [], warnings: warnings || [] })
    } catch (e) {
      // 公开门户不可用时保留登录态数据
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadDashboard() {
    try {
      const stats = await request({ url: '/dashboard', silent: true })
      this.setData({ stats })
    } catch (e) {
      this.setData({ stats: null })
    }
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  openLink(e) {
    const path = e.currentTarget.dataset.path
    if (!path) return
    const tabs = ['/pages/visitor/list', '/pages/lost/index', '/pages/message/index', '/pages/profile/index']
    if (tabs.indexOf(path) >= 0) {
      wx.switchTab({ url: path })
    } else {
      wx.navigateTo({ url: path })
    }
  },

  openNotice(e) {
    wx.navigateTo({ url: `/pages/news/detail?id=${e.currentTarget.dataset.id}` })
  },

  openVisitorList() {
    wx.switchTab({ url: '/pages/visitor/list' })
  },

  formatDate(value) {
    return util.formatDate(value)
  }
})
