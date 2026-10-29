const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    loggedIn: false,
    status: '',
    tabs: [
      { value: '', label: '全部' },
      { value: 'PENDING', label: '待审批' },
      { value: 'APPROVED', label: '已通过' },
      { value: 'EXPIRED', label: '已过期' }
    ],
    list: [],
    page: 1,
    size: 10,
    total: 0,
    loading: false,
    qrVisible: false,
    qrImage: '',
    qrTitle: ''
  },

  onShow() {
    const loggedIn = app.isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) {
      this.reload()
    } else {
      this.setData({ list: [] })
    }
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.list.length >= this.data.total) return
    this.setData({ page: this.data.page + 1 })
    this.load()
  },

  switchTab(e) {
    this.setData({ status: e.currentTarget.dataset.value })
    this.reload()
  },

  reload() {
    this.setData({ page: 1, list: [] })
    return this.load()
  },

  async load() {
    if (!app.isLoggedIn()) return
    this.setData({ loading: true })
    try {
      const data = await request({
        url: '/visitor/appointments',
        params: { page: this.data.page, size: this.data.size, status: this.data.status || undefined }
      })
      const rows = (data.records || []).map((item) => Object.assign({}, item, {
        statusText: util.statusLabel(item.status),
        statusClass: util.statusClass(item.status),
        visitTimeText: util.formatDate(item.visitTime),
        phoneMaskText: item.phoneMask || util.mask(item.phone),
        idCardMaskText: item.idCardMask || util.mask(item.idCard)
      }))
      this.setData({
        list: this.data.page === 1 ? rows : this.data.list.concat(rows),
        total: data.total || rows.length
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  goCreate() {
    if (!app.isLoggedIn()) {
      wx.navigateTo({ url: '/pages/login/index' })
      return
    }
    wx.navigateTo({ url: '/pages/visitor/create' })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  async approve(e) {
    const id = e.currentTarget.dataset.id
    const approved = e.currentTarget.dataset.approved === 'true'
    wx.showModal({
      title: approved ? '审批通过' : '驳回预约',
      content: approved ? '确认允许该访客入校？' : '确认驳回该访客预约？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request({
            url: `/visitor/appointments/${id}/approve`,
            method: 'POST',
            data: { approved }
          })
          wx.showToast({ title: approved ? '已通过' : '已驳回', icon: 'success' })
          this.reload()
        } catch (err) {
          wx.showToast({ title: err.message || '无审批权限', icon: 'none' })
        }
      }
    })
  },

  async showQr(e) {
    const id = e.currentTarget.dataset.id
    try {
      const data = await request({ url: `/visitor/appointments/${id}/qr` })
      const filePath = `${wx.env.USER_DATA_PATH}/pass-${id}.png`
      wx.getFileSystemManager().writeFile({
        filePath,
        data: data.qrBase64,
        encoding: 'base64',
        success: () => {
          this.setData({ qrVisible: true, qrImage: filePath, qrTitle: data.passCode || '通行码' })
        },
        fail: () => wx.showToast({ title: '通行码生成失败', icon: 'none' })
      })
    } catch (err) {
      wx.showToast({ title: err.message || '暂无通行码', icon: 'none' })
    }
  },

  closeQr() {
    this.setData({ qrVisible: false })
  },

  noop() {},

  scanCheck() {
    wx.scanCode({
      success: async (res) => {
        try {
          const result = await request({
            url: '/visitor/check',
            method: 'POST',
            data: { passCode: res.result, gate: '小程序门岗', method: 'QR' }
          })
          wx.showModal({
            title: result.pass ? '核验通过' : '核验未通过',
            content: result.message || '已记录本次核验',
            showCancel: false
          })
        } catch (err) {
          wx.showToast({ title: err.message || '核验失败', icon: 'none' })
        }
      }
    })
  }
})
