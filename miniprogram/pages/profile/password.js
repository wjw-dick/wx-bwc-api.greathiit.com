const app = getApp()
const { request } = require('../../utils/request')

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    submitting: false,
    error: ''
  },

  onLoad() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
    }
  },

  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value, error: '' })
  },

  async submit() {
    const { oldPassword, newPassword, confirmPassword } = this.data
    if (!oldPassword) return this.setData({ error: '请输入原密码' })
    if (!newPassword || newPassword.length < 8 || newPassword.length > 32) {
      return this.setData({ error: '新密码长度需为 8-32 位' })
    }
    if (newPassword !== confirmPassword) return this.setData({ error: '两次输入的新密码不一致' })
    if (!/[A-Za-z]/.test(newPassword) || !/\d/.test(newPassword)) {
      return this.setData({ error: '新密码需同时包含字母和数字' })
    }
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/auth/change-password',
        method: 'POST',
        data: { oldPassword, newPassword }
      })
      wx.showModal({
        title: '密码已修改',
        content: '请使用新密码重新登录。',
        showCancel: false,
        success: () => {
          app.clearSession()
          wx.reLaunch({ url: '/pages/login/index' })
        }
      })
    } catch (e) {
      this.setData({ error: e.message || '修改失败' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
