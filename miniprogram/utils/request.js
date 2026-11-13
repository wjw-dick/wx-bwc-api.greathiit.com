const config = require('./config')

function baseUrl() {
  return config.BASE_URL
}

function request(options) {
  const app = getApp()
  const token = (app && app.globalData && app.globalData.token) || wx.getStorageSync('bwc_token')
  const auth = options.auth !== false
  const header = Object.assign({ 'Content-Type': 'application/json' }, options.header || {})
  if (auth && token) {
    header.Authorization = `Bearer ${token}`
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl()}${options.url}`,
      method: options.method || 'GET',
      data: options.data || options.params || {},
      header,
      timeout: options.timeout || 20000,
      success(res) {
        const body = res.data
        if (res.statusCode === 401) {
          if (app) app.clearSession()
          wx.reLaunch({ url: '/pages/login/index' })
          reject(new Error('登录已过期'))
          return
        }
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 0) {
            resolve(body.data)
          } else {
            if (!options.silent) {
              wx.showToast({ title: body.message || '请求失败', icon: 'none' })
            }
            reject(new Error(body.message || '请求失败'))
          }
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(body)
        } else {
          if (!options.silent) {
            wx.showToast({ title: (body && body.message) || `HTTP ${res.statusCode}`, icon: 'none' })
          }
          reject(new Error((body && body.message) || `HTTP ${res.statusCode}`))
        }
      },
      fail(err) {
        if (!options.silent) {
          wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

function upload(filePath, bizType) {
  const app = getApp()
  const token = (app && app.globalData && app.globalData.token) || wx.getStorageSync('bwc_token')
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${baseUrl()}/system/upload/user`,
      filePath,
      name: 'file',
      formData: { bizType: bizType || 'miniprogram' },
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success(res) {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 0) resolve(body.data)
          else reject(new Error(body.message || '上传失败'))
        } catch (e) {
          reject(new Error('上传失败'))
        }
      },
      fail: reject
    })
  })
}

module.exports = { request, upload, baseUrl }
