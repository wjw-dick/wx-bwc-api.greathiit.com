const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'course',
    courses: [],
    exams: [],
    records: [],
    loading: false,
    detail: null,
    detailVisible: false,
    loggedIn: false
  },

  onShow() {
    const loggedIn = app.isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) {
      this.load()
    }
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  async load() {
    if (!app.isLoggedIn()) return
    this.setData({ loading: true })
    try {
      const [courses, exams, records] = await Promise.all([
        request({ url: '/education/courses', params: { page: 1, size: 50 }, silent: true }),
        request({ url: '/education/exams', params: { page: 1, size: 50, status: 'PUBLISHED' }, silent: true }),
        request({ url: '/education/my-records', silent: true })
      ])
      this.setData({
        courses: (courses.records || []).map((item) => Object.assign({}, item, {
          publishTimeText: util.formatDate(item.publishTime)
        })),
        exams: (exams.records || []).map((item) => Object.assign({}, item, {
          timeText: item.startTime ? `${util.formatDate(item.startTime)} ~ ${util.formatDate(item.endTime)}` : '长期有效'
        })),
        records: (records || []).map((item) => Object.assign({}, item, {
          submitTimeText: util.formatDate(item.submitTime),
          passText: item.passFlag === 1 ? '通过' : '未通过',
          passClass: item.passFlag === 1 ? 'ok' : 'danger'
        }))
      })
    } catch (e) {
      // 无权限时保留空态
    } finally {
      this.setData({ loading: false })
    }
  },

  openCourse(e) {
    const id = e.currentTarget.dataset.id
    const course = this.data.courses.find((item) => item.id === id)
    if (course) this.setData({ detail: course, detailVisible: true })
  },

  closeDetail() {
    this.setData({ detailVisible: false })
  },

  noop() {},

  async study(e) {
    const id = e.currentTarget.dataset.id
    try {
      await request({
        url: `/education/courses/${id}/study`,
        method: 'POST',
        data: { progress: 100 }
      })
      wx.showToast({ title: '已记录学习进度', icon: 'success' })
    } catch (err) {
      wx.showToast({ title: err.message || '记录失败', icon: 'none' })
    }
  },

  goExam(e) {
    wx.navigateTo({ url: `/pages/education/exam?id=${e.currentTarget.dataset.id}` })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  }
})
