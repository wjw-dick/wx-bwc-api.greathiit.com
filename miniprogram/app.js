const config = require('./utils/config')
const i18n = require('./utils/i18n')
const { request } = require('./utils/request')

App({
  globalData: {
    token: '',
    user: null,
    tenant: null,
    menus: [],
    permissions: [],
    tenantCode: 'DEMO',
    locale: 'zh-CN',
    config
  },

  onLaunch() {
    this.globalData.token = wx.getStorageSync('bwc_token') || ''
    this.globalData.user = wx.getStorageSync('bwc_user') || null
    this.globalData.tenant = wx.getStorageSync('bwc_tenant') || null
    this.globalData.permissions = wx.getStorageSync('bwc_permissions') || []
    this.globalData.tenantCode = wx.getStorageSync('bwc_tenant_code') || (this.globalData.tenant && this.globalData.tenant.code) || 'DEMO'
    this.globalData.locale = i18n.getLocale()
    this.loadMenus()
  },

  setSession(data) {
    this.globalData.token = data.token
    this.globalData.user = data.user
    this.globalData.tenant = data.tenant
    this.globalData.permissions = data.permissions || []
    this.globalData.tenantCode = (data.tenant && data.tenant.code) || this.globalData.tenantCode
    wx.setStorageSync('bwc_token', data.token)
    wx.setStorageSync('bwc_user', data.user)
    wx.setStorageSync('bwc_tenant', data.tenant)
    wx.setStorageSync('bwc_permissions', this.globalData.permissions)
    wx.setStorageSync('bwc_tenant_code', this.globalData.tenantCode)
    this.loadMenus()
  },

  clearSession() {
    this.globalData.token = ''
    this.globalData.user = null
    this.globalData.tenant = null
    this.globalData.permissions = []
    wx.removeStorageSync('bwc_token')
    wx.removeStorageSync('bwc_user')
    wx.removeStorageSync('bwc_tenant')
    wx.removeStorageSync('bwc_permissions')
  },

  isLoggedIn() {
    return !!this.globalData.token
  },

  hasPerm(permission) {
    const list = this.globalData.permissions || []
    return list.indexOf('*') >= 0 || list.indexOf(permission) >= 0
  },

  async loadMenus() {
    try {
      const menus = await request({
        url: `/public/tenant/${this.globalData.tenantCode}/menus`,
        params: { platform: 'miniprogram' },
        auth: false
      })
      this.globalData.menus = menus || []
      this.applyTabBar()
    } catch (e) {
      // 未配置学校或网络异常时保留默认 tabBar
    }
  },

  applyTabBar() {
    const menus = this.globalData.menus || []
    const zh = this.globalData.locale !== 'en-US'
    menus.forEach((menu, index) => {
      if (index > 4 || !menu) return
      const text = zh ? menu.name : (menu.nameEn || menu.name)
      if (!text) return
      wx.setTabBarItem({ index, text, fail() {} })
    })
  },

  setLocale(locale) {
    this.globalData.locale = locale
    i18n.setLocale(locale)
    this.applyTabBar()
  }
})
