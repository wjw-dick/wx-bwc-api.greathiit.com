const app = getApp()
const subscribe = require('../../utils/subscribe')

Page({
  data: {
    loggedIn: false,
    user: null,
    tenant: null,
    locale: 'zh-CN',
    avatarText: '安',
    agreementVisible: false,
    aboutVisible: false,
    links: [
      { key: 'approval', label: '审批中心', icon: '审', path: '/pages/approval/list' },
      { key: 'education', label: '安全教育', icon: '学', path: '/pages/education/index' },
      { key: 'report', label: '报修反馈', icon: '修', path: '/pages/report/index' },
      { key: 'message', label: '消息中心', icon: '信', tab: '/pages/message/index' },
      { key: 'password', label: '修改密码', icon: '密', path: '/pages/profile/password' }
    ]
  },

  onShow() {
    const user = app.globalData.user
    this.setData({
      loggedIn: app.isLoggedIn(),
      user,
      tenant: app.globalData.tenant,
      locale: app.globalData.locale || 'zh-CN',
      avatarText: user && user.realName ? String(user.realName).slice(0, 1) : '安'
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  openLink(e) {
    const link = this.data.links.find((item) => item.key === e.currentTarget.dataset.key)
    if (!link) return
    if (link.tab) {
      wx.switchTab({ url: link.tab })
    } else {
      wx.navigateTo({ url: link.path })
    }
  },

  toggleLocale() {
    const locale = this.data.locale === 'zh-CN' ? 'en-US' : 'zh-CN'
    app.setLocale(locale)
    this.setData({ locale })
    wx.showToast({ title: locale === 'zh-CN' ? '已切换为中文' : 'Switched to English', icon: 'none' })
  },

  showAgreement() {
    this.setData({ agreementVisible: true })
  },

  async subscribeNotice() {
    const result = await subscribe.requestSubscribe(['TODO', 'EVENT', 'WARNING'])
    if (result.ok) {
      wx.showToast({ title: '已开启消息通知', icon: 'success' })
    } else {
      wx.showToast({ title: result.reason || '未开启通知', icon: 'none' })
    }
  },

  showAbout() {
    this.setData({ aboutVisible: true })
  },

  closeModal() {
    this.setData({ agreementVisible: false, aboutVisible: false })
  },

  noop() {},

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确认退出当前账号？',
      success: (res) => {
        if (!res.confirm) return
        app.clearSession()
        this.setData({ loggedIn: false, user: null })
        wx.reLaunch({ url: '/pages/login/index' })
      }
    })
  }
})
