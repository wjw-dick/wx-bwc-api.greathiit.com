const app = getApp()
const { request } = require('../../utils/request')
const config = require('../../utils/config')

Page({
  data: {
    username: '',
    password: '',
    tenantCode: 'DEMO',
    loading: false,
    error: '',
    locale: 'zh-CN',
    wxBound: false
  },

  onLoad() {
    this.setData({
      tenantCode: app.globalData.tenantCode || 'DEMO',
      locale: app.globalData.locale || 'zh-CN'
    })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value, error: '' })
  },

  toggleLocale() {
    const locale = this.data.locale === 'zh-CN' ? 'en-US' : 'zh-CN'
    app.setLocale(locale)
    this.setData({ locale })
  },

  wxLogin() {
    if (!this.data.username || !this.data.password) {
      this.setData({ error: '请输入账号和密码' })
      return
    }
    this.setData({ loading: true, error: '' })
    wx.login({
      success: async (loginRes) => {
        try {
          const result = await request({
            url: '/auth/wx-bind',
            method: 'POST',
            auth: false,
            data: {
              username: this.data.username,
              password: this.data.password,
              code: loginRes.code,
              appType: config.APP_TYPE,
              tenantCode: this.data.tenantCode || app.globalData.tenantCode
            }
          })
          this.finishLogin(result, true)
        } catch (e) {
          // 微信未配置或 code2Session 不可用时，降级为账号密码登录，保证业务可用
          await this.passwordLogin()
        }
      },
      fail: async () => {
        await this.passwordLogin()
      }
    })
  },

  async passwordLogin() {
    try {
      const result = await request({
        url: '/auth/login',
        method: 'POST',
        auth: false,
        data: {
          username: this.data.username,
          password: this.data.password,
          tenantCode: this.data.tenantCode,
          loginType: 'tenant'
        }
      })
      this.finishLogin(result, false)
    } catch (e) {
      this.setData({ error: e.message || '登录失败', loading: false })
    }
  },

  finishLogin(result, wxBound) {
    app.setSession(result)
    this.setData({ loading: false, wxBound })
    wx.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      wx.switchTab({ url: '/pages/index/index' })
    }, 500)
  }
})
