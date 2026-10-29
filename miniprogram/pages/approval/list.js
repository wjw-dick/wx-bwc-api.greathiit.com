const app = getApp()
const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    groups: [],
    stats: { visitor: 0, vehicle: 0, claim: 0, report: 0, monitor: 0 },
    loading: false,
    loggedIn: false,
    total: 0
  },

  onShow() {
    const loggedIn = app.isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    const tasks = [
      request({ url: '/visitor/appointments', params: { page: 1, size: 20, status: 'PENDING' }, silent: true }),
      request({ url: '/vehicle/appointments', params: { page: 1, size: 20, status: 'PENDING' }, silent: true }),
      request({ url: '/lost/claims', params: { page: 1, size: 20, status: 'PENDING' }, silent: true }),
      request({ url: '/report/applies', params: { page: 1, size: 20, status: 'PENDING' }, silent: true }),
      request({ url: '/report/monitor-access', params: { page: 1, size: 20, status: 'PENDING' }, silent: true })
    ]
    const results = await Promise.allSettled(tasks)
    const pick = (index) => (results[index].status === 'fulfilled' && results[index].value.records) || []

    const visitor = pick(0).map((item) => Object.assign({}, item, {
      kind: 'visitor',
      title: `${item.visitorName} 来访`,
      subtitle: `${util.formatDate(item.visitTime)} · ${item.reason || '来访'}`,
      extra: `电话 ${item.phoneMask || '-'}${item.plateNo ? ' · 车牌 ' + item.plateNo : ''}`
    }))
    const vehicle = pick(1).map((item) => Object.assign({}, item, {
      kind: 'vehicle',
      title: `车辆 ${item.plateNo}`,
      subtitle: `${util.formatDate(item.visitTime)} · ${item.visitorName || '驾驶人'}`,
      extra: `电话 ${item.phoneMask || '-'}`
    }))
    const claim = pick(2).map((item) => Object.assign({}, item, {
      kind: 'claim',
      title: `${item.claimant} 申请认领`,
      subtitle: `物品 #${item.itemId} · ${util.formatDate(item.claimTime)}`,
      extra: `电话 ${item.phoneMask || '-'}`
    }))
    const report = pick(3).map((item) => Object.assign({}, item, {
      kind: 'report',
      title: item.title,
      subtitle: `${item.type || '报备'} · ${util.formatDate(item.startTime)}`,
      extra: item.location || ''
    }))
    const monitor = pick(4).map((item) => Object.assign({}, item, {
      kind: 'monitor',
      title: item.applyNo || `调阅申请 #${item.id}`,
      subtitle: `${item.area || '-'} · ${item.timeRange || '-'}`,
      extra: item.reason || ''
    }))

    const groups = [
      { key: 'visitor', name: '访客审批', items: visitor },
      { key: 'vehicle', name: '车辆预约审批', items: vehicle },
      { key: 'claim', name: '失物认领审批', items: claim },
      { key: 'report', name: '报备审批', items: report },
      { key: 'monitor', name: '监控调取审批', items: monitor }
    ].filter((group) => group.items.length)

    this.setData({
      groups,
      stats: {
        visitor: visitor.length,
        vehicle: vehicle.length,
        claim: claim.length,
        report: report.length,
        monitor: monitor.length
      },
      total: visitor.length + vehicle.length + claim.length + report.length + monitor.length
    })
    this.setData({ loading: false })
  },

  async act(e) {
    const { kind, id, approved } = e.currentTarget.dataset
    const isApprove = approved === 'true'
    wx.showModal({
      title: isApprove ? '确认通过' : '确认驳回',
      content: '操作将记录审批日志并通知申请人。',
      success: async (res) => {
        if (!res.confirm) return
        let url = ''
        if (kind === 'visitor') url = `/visitor/appointments/${id}/approve`
        if (kind === 'vehicle') url = `/vehicle/appointments/${id}/approve`
        if (kind === 'claim') url = `/lost/claims/${id}/approve`
        if (kind === 'report') url = `/report/applies/${id}/approve`
        try {
          await request({ url, method: 'POST', data: { approved: isApprove } })
          wx.showToast({ title: isApprove ? '已通过' : '已驳回', icon: 'success' })
          this.load()
        } catch (err) {
          wx.showToast({ title: err.message || '审批失败', icon: 'none' })
        }
      }
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
