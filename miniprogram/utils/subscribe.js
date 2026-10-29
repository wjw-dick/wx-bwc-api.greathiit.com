const { request } = require('./request')

let templateCache = null
let templateCacheAt = 0

function tenantCode() {
  const app = getApp()
  return (app && app.globalData && app.globalData.tenantCode) || wx.getStorageSync('bwc_tenant_code') || 'DEMO'
}

async function loadTemplates(force) {
  if (!force && templateCache && Date.now() - templateCacheAt < 10 * 60 * 1000) {
    return templateCache
  }
  try {
    const list = await request({
      url: `/public/tenant/${tenantCode()}/subscribe-templates`,
      auth: false,
      silent: true
    })
    templateCache = (list || []).filter((item) => item && item.templateId)
    templateCacheAt = Date.now()
  } catch (e) {
    templateCache = templateCache || []
  }
  return templateCache
}

/**
 * 请求小程序订阅消息授权。必须由用户点击触发；失败时不打断业务。
 * @param {string[]} bizTypes 业务类型，如 ['TODO','EVENT']；为空表示全部可用模板
 * @returns {Promise<{ok:boolean, accepted:string[], reason?:string}>}
 */
async function requestSubscribe(bizTypes) {
  if (!wx.requestSubscribeMessage) {
    return { ok: false, accepted: [], reason: '当前微信版本不支持订阅消息' }
  }
  const templates = await loadTemplates()
  const filtered = templates.filter((item) => !bizTypes || !bizTypes.length || bizTypes.indexOf(item.bizType) >= 0)
  const tmplIds = filtered.map((item) => item.templateId).filter((id, index, arr) => arr.indexOf(id) === index).slice(0, 3)
  if (!tmplIds.length) {
    return { ok: false, accepted: [], reason: '学校尚未配置小程序订阅消息模板' }
  }
  return new Promise((resolve) => {
    wx.requestSubscribeMessage({
      tmplIds,
      success(res) {
        const accepted = tmplIds.filter((id) => res[id] === 'accept')
        resolve({ ok: accepted.length > 0, accepted, result: res })
      },
      fail(err) {
        resolve({ ok: false, accepted: [], reason: (err && err.errMsg) || '订阅授权未完成' })
      }
    })
  })
}

module.exports = { requestSubscribe, loadTemplates }
