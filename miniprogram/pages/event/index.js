const app = getApp()
const { request, upload } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'report',
    types: ['失窃', '纠纷', '求助', '可疑人员', '其他'],
    urgency: ['NORMAL', 'URGENT', 'CRITICAL'],
    urgencyLabels: ['一般', '紧急', '重大'],
    form: { typeIndex: 0, urgencyIndex: 0, location: '', description: '', photos: '' },
    list: [],
    loading: false,
    submitting: false,
    error: ''
  },

  onShow() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab, error: '' })
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value, error: '' })
  },

  onType(e) {
    this.setData({ 'form.typeIndex': Number(e.detail.value) })
  },

  onUrgency(e) {
    this.setData({ 'form.urgencyIndex': Number(e.detail.value) })
  },

  async choosePhoto() {
    wx.chooseMedia({
      count: 3,
      mediaType: ['image'],
      success: async (res) => {
        const paths = res.tempFiles.map((item) => item.tempFilePath)
        try {
          const urls = []
          for (const path of paths) {
            const data = await upload(path, 'event')
            urls.push(data.url)
          }
          const photos = [this.data.form.photos].filter(Boolean).concat(urls).join(',')
          this.setData({ 'form.photos': photos })
        } catch (e) {
          wx.showToast({ title: e.message || '上传失败', icon: 'none' })
        }
      }
    })
  },

  async submit() {
    const f = this.data.form
    if (!f.location) return this.setData({ error: '请填写事发位置' })
    if (!f.description) return this.setData({ error: '请描述现场情况' })
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/event/report',
        method: 'POST',
        data: {
          eventType: this.data.types[f.typeIndex],
          urgency: this.data.urgency[f.urgencyIndex],
          location: f.location,
          description: f.description,
          photos: f.photos || undefined,
          source: 'MINI'
        }
      })
      wx.showToast({ title: '上报成功', icon: 'success' })
      this.setData({ form: { typeIndex: 0, urgencyIndex: 0, location: '', description: '', photos: '' } })
      this.setData({ tab: 'list' })
      this.load()
    } catch (e) {
      this.setData({ error: e.message || '上报失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async load() {
    try {
      const data = await request({ url: '/event/events', params: { page: 1, size: 20 }, silent: true })
      this.setData({
        list: (data.records || []).map((item) => Object.assign({}, item, {
          statusText: util.statusLabel(item.status),
          statusClass: util.statusClass(item.status),
          createTimeText: util.formatDate(item.createTime)
        }))
      })
    } catch (e) {
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  handle(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '记录处置',
      editable: true,
      placeholderText: '请输入现场处置情况',
      success: async (res) => {
        if (!res.confirm || !res.content) return
        try {
          await request({ url: `/event/${id}/handle`, method: 'POST', data: { result: res.content } })
          wx.showToast({ title: '已记录', icon: 'success' })
          this.load()
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    })
  },

  close(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '结案归档',
      content: '确认该事件已处置完毕并结案？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request({ url: `/event/${id}/close`, method: 'POST', data: {} })
          wx.showToast({ title: '已结案', icon: 'success' })
          this.load()
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' })
        }
      }
    })
  }
})
