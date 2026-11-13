const app = getApp()
const { request, upload } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'inspect',
    qrCode: '',
    inspectForm: { result: 'NORMAL', remark: '', photos: '' },
    hazardForm: { levelIndex: 0, location: '', description: '', photos: '' },
    levels: ['一般隐患', '较大隐患', '重大隐患'],
    levelValues: ['GENERAL', 'MAJOR', 'CRITICAL'],
    hazards: [],
    submitting: false,
    error: ''
  },

  onShow() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    this.loadHazards()
  },

  onPullDownRefresh() {
    this.loadHazards().finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab, error: '' })
  },

  scanFacility() {
    wx.scanCode({
      success: (res) => this.setData({ qrCode: res.result, error: '' })
    })
  },

  onInspectInput(e) {
    this.setData({ [`inspectForm.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  onResult(e) {
    this.setData({ 'inspectForm.result': e.currentTarget.dataset.value })
  },

  async chooseInspectPhoto() {
    const photos = await this.pickPhotos('fire')
    if (photos) this.setData({ 'inspectForm.photos': photos })
  },

  async submitInspect() {
    if (!this.data.qrCode) return this.setData({ error: '请先扫描消防设施二维码' })
    this.setData({ submitting: true, error: '' })
    try {
      const data = await request({
        url: '/fire/inspect',
        method: 'POST',
        data: {
          qrCode: this.data.qrCode,
          result: this.data.inspectForm.result,
          remark: this.data.inspectForm.remark || undefined,
          photos: this.data.inspectForm.photos || undefined
        }
      })
      const hazard = data.hazard
      wx.showModal({
        title: '巡检完成',
        content: hazard ? `发现异常，已生成隐患单 ${hazard.hazardNo}` : '设施状态正常，已记录巡检',
        showCancel: false
      })
      this.setData({ qrCode: '', inspectForm: { result: 'NORMAL', remark: '', photos: '' } })
      this.loadHazards()
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  onHazardInput(e) {
    this.setData({ [`hazardForm.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  onLevel(e) {
    this.setData({ 'hazardForm.levelIndex': Number(e.detail.value) })
  },

  async chooseHazardPhoto() {
    const photos = await this.pickPhotos('hazard')
    if (photos) this.setData({ 'hazardForm.photos': photos })
  },

  async submitHazard() {
    const f = this.data.hazardForm
    if (!f.location) return this.setData({ error: '请填写隐患位置' })
    if (!f.description) return this.setData({ error: '请描述隐患情况' })
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/fire/hazards',
        method: 'POST',
        data: {
          hazardLevel: this.data.levelValues[f.levelIndex],
          location: f.location,
          description: f.description,
          photos: f.photos || undefined,
          source: 'MINI'
        }
      })
      wx.showToast({ title: '隐患已上报', icon: 'success' })
      this.setData({ hazardForm: { levelIndex: 0, location: '', description: '', photos: '' } })
      this.loadHazards()
    } catch (e) {
      this.setData({ error: e.message || '上报失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async pickPhotos(bizType) {
    return new Promise((resolve) => {
      wx.chooseMedia({
        count: 3,
        mediaType: ['image'],
        success: async (res) => {
          try {
            const urls = []
            for (const item of res.tempFiles) {
              const data = await upload(item.tempFilePath, bizType)
              urls.push(data.url)
            }
            resolve(urls.join(','))
          } catch (e) {
            wx.showToast({ title: e.message || '上传失败', icon: 'none' })
            resolve('')
          }
        },
        fail: () => resolve('')
      })
    })
  },

  async loadHazards() {
    try {
      const data = await request({ url: '/fire/hazards', params: { page: 1, size: 20 }, silent: true })
      this.setData({
        hazards: (data.records || []).map((item) => Object.assign({}, item, {
          statusText: util.statusLabel(item.status),
          statusClass: util.statusClass(item.status),
          reportTimeText: util.formatDate(item.reportTime)
        }))
      })
    } catch (e) {
      this.setData({ hazards: [] })
    }
  }
})
