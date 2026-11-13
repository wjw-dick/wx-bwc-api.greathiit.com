const app = getApp()
const { request, upload } = require('../../utils/request')
const util = require('../../utils/util')

const STATUS = { PENDING: '待处理', HANDLING: '处理中', DONE: '已处理' }

Page({
  data: {
    form: {
      plateNo: '', location: '', violateDate: '', violateClock: '', description: '', photos: []
    },
    list: [],
    error: '',
    submitting: false,
    loading: false,
    uploading: false
  },

  onLoad() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/index' })
      return
    }
    const d = new Date()
    this.setData({
      'form.violateDate': `${d.getFullYear()}-${util.pad(d.getMonth() + 1)}-${util.pad(d.getDate())}`,
      'form.violateClock': `${util.pad(d.getHours())}:${util.pad(d.getMinutes())}`
    })
  },

  onShow() {
    if (app.isLoggedIn()) this.loadMy()
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    let value = e.detail.value
    if (field === 'plateNo') value = String(value).toUpperCase()
    this.setData({ [`form.${field}`]: value, error: '' })
  },

  onDate(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value })
  },

  choosePhoto() {
    const remain = 3 - this.data.form.photos.length
    if (remain <= 0) return
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: async (res) => {
        this.setData({ uploading: true })
        try {
          const urls = []
          for (const file of res.tempFiles) {
            urls.push(await upload(file.tempFilePath, 'violation'))
          }
          this.setData({ 'form.photos': this.data.form.photos.concat(urls) })
        } catch (e) {
          wx.showToast({ title: e.message || '图片上传失败', icon: 'none' })
        } finally {
          this.setData({ uploading: false })
        }
      }
    })
  },

  removePhoto(e) {
    const index = Number(e.currentTarget.dataset.index)
    const photos = this.data.form.photos.slice()
    photos.splice(index, 1)
    this.setData({ 'form.photos': photos })
  },

  async submit() {
    const f = this.data.form
    const plate = String(f.plateNo || '').replace(/\s/g, '').toUpperCase()
    if (!/^[\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}$/.test(plate) && !/^[A-Z0-9]{6,8}$/.test(plate)) {
      return this.setData({ error: '请输入正确的车牌号，如 京A12345' })
    }
    if (!f.location) return this.setData({ error: '请填写违停地点' })
    this.setData({ submitting: true, error: '' })
    try {
      await request({
        url: '/vehicle/violations/report',
        method: 'POST',
        data: {
          plateNo: plate,
          location: f.location,
          violateTime: f.violateDate && f.violateClock ? `${f.violateDate} ${f.violateClock}:00` : undefined,
          description: f.description || undefined,
          photos: f.photos.length ? f.photos.join(',') : undefined
        }
      })
      wx.showToast({ title: '上报成功', icon: 'success' })
      this.setData({ form: { plateNo: '', location: '', violateDate: f.violateDate, violateClock: f.violateClock, description: '', photos: [] } })
      this.loadMy()
    } catch (e) {
      this.setData({ error: e.message || '上报失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadMy() {
    this.setData({ loading: true })
    try {
      const list = await request({ url: '/vehicle/violations/my', silent: true })
      this.setData({
        list: (list || []).map((item) => Object.assign({}, item, {
          statusText: STATUS[item.handleStatus] || item.handleStatus || '-'
        }))
      })
    } catch (e) {
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  }
})
