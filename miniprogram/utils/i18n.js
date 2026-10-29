const messages = {
  'zh-CN': {
    home: '首页', visitor: '访客', lost: '失物', message: '消息', profile: '我的',
    login: '登录', logout: '退出登录', submit: '提交', cancel: '取消', confirm: '确定',
    loading: '加载中…', empty: '暂无数据', success: '操作成功', search: '搜索',
    changePassword: '修改密码', language: '语言', english: 'English', chinese: '简体中文',
    visitorTitle: '访客预约', lostTitle: '失物招领', vehicleTitle: '车辆预约',
    reportTitle: '报修与反馈', educationTitle: '安全教育', approvalTitle: '审批中心',
    newsTitle: '安全资讯', messages: '消息中心', profileTitle: '个人中心'
  },
  'en-US': {
    home: 'Home', visitor: 'Visitor', lost: 'Lost', message: 'Messages', profile: 'Me',
    login: 'Sign in', logout: 'Sign out', submit: 'Submit', cancel: 'Cancel', confirm: 'Confirm',
    loading: 'Loading…', empty: 'No data', success: 'Success', search: 'Search',
    changePassword: 'Change password', language: 'Language', english: 'English', chinese: '简体中文',
    visitorTitle: 'Visitor booking', lostTitle: 'Lost & Found', vehicleTitle: 'Vehicle booking',
    reportTitle: 'Report & feedback', educationTitle: 'Safety education', approvalTitle: 'Approvals',
    newsTitle: 'Safety news', messages: 'Messages', profileTitle: 'Profile'
  }
}

function getLocale() {
  return wx.getStorageSync('bwc_locale') || 'zh-CN'
}

function setLocale(locale) {
  wx.setStorageSync('bwc_locale', locale)
}

function t(key) {
  const locale = getLocale()
  return (messages[locale] && messages[locale][key]) || messages['zh-CN'][key] || key
}

module.exports = { t, getLocale, setLocale, messages }
