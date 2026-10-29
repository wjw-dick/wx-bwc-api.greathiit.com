import { createI18n } from 'vue-i18n'

const zh = {
  nav: {
    home: '首页', visitor: '访客预约', lost: '失物招领', news: '安全资讯', school: '学校门户'
  },
  common: {
    loading: '加载中…', submit: '提交', cancel: '取消', search: '搜索', reset: '重置',
    back: '返回', more: '查看更多', required: '必填', success: '操作成功',
    contact: '值班电话', viewDetail: '查看详情', all: '全部'
  },
  home: {
    badge: '平安校园 · 智慧安防',
    title: '校园安全，从每一次到访开始',
    subtitle: '访客预约、失物招领、安全资讯与预警公告，一站直达。',
    visitorCta: '访客预约',
    lostCta: '失物招领',
    notices: '安全资讯',
    warnings: '预警公告',
    lost: '失物招领',
    intro: '学校安全体系',
    empty: '暂无数据'
  },
  visitor: {
    title: '访客预约',
    subtitle: '未注册访客可直接预约，审批通过后凭通行码入校',
    name: '访客姓名',
    phone: '手机号',
    idCard: '身份证号',
    idCardHint: '仅用于身份核验，系统加密存储且页面不会展示完整号码',
    reason: '来访事由',
    visitTime: '来访时间',
    leaveTime: '离校时间',
    accompany: '随行人数',
    plate: '车牌号（可选）',
    visitee: '被访人',
    remark: '备注',
    submit: '提交预约',
    success: '预约提交成功',
    successTip: '请等待被访人审批，审批通过后凭通行码在预约时段内入校。',
    again: '再次预约',
    invalidIdCard: '请输入正确的 18 位身份证号',
    invalidPhone: '请输入正确的手机号',
    maskNote: '提交后仅展示脱敏信息',
    status: '预约状态'
  },
  lost: {
    title: '失物招领',
    subtitle: '捡到物品请及时登记，失主可凭物品编号到保卫处认领',
    keyword: '搜索物品名称/地点',
    category: '物品分类',
    empty: '暂时没有待认领物品',
    report: '捡拾登记',
    reportTitle: '登记拾获物品',
    itemName: '物品名称',
    location: '拾获地点',
    description: '物品描述',
    finder: '拾获人',
    storage: '存放地点',
    reportSuccess: '登记成功，请将物品交至保卫处失物招领点。',
    foundAt: '拾获时间',
    itemNo: '编号',
    storageLabel: '存放地点'
  },
  news: {
    title: '安全资讯',
    subtitle: '校园安全动态、通知公告与防范提示',
    detail: '资讯详情',
    views: '阅读',
    empty: '暂无资讯'
  },
  school: {
    title: '学校门户',
    duty: '24 小时值班电话',
    intro: '学校安全体系',
    contact: '联系我们'
  },
  share: {
    title: '分享详情',
    open: '打开完整页面',
    source: '来源：微信分享'
  },
  error: {
    load: '加载失败，请稍后重试',
    tenant: '学校门户不存在或已停用'
  }
}

const en = {
  nav: {
    home: 'Home', visitor: 'Visitor', lost: 'Lost & Found', news: 'News', school: 'School'
  },
  common: {
    loading: 'Loading…', submit: 'Submit', cancel: 'Cancel', search: 'Search', reset: 'Reset',
    back: 'Back', more: 'View more', required: 'Required', success: 'Success',
    contact: 'Duty phone', viewDetail: 'Details', all: 'All'
  },
  home: {
    badge: 'Safe Campus · Smart Security',
    title: 'Campus safety starts with every visit',
    subtitle: 'Visitor booking, lost & found, safety news and alerts in one place.',
    visitorCta: 'Book a visit',
    lostCta: 'Lost & Found',
    notices: 'Safety news',
    warnings: 'Alerts',
    lost: 'Lost & Found',
    intro: 'Campus safety system',
    empty: 'No data'
  },
  visitor: {
    title: 'Visitor Booking',
    subtitle: 'No account required. Enter the campus with your pass code after approval.',
    name: 'Visitor name',
    phone: 'Mobile',
    idCard: 'ID number',
    idCardHint: 'Used for identity verification only. Encrypted at rest and never fully displayed.',
    reason: 'Purpose',
    visitTime: 'Visit time',
    leaveTime: 'Leave time',
    accompany: 'Accompanying',
    plate: 'Plate number (optional)',
    visitee: 'Host',
    remark: 'Remark',
    submit: 'Submit booking',
    success: 'Booking submitted',
    successTip: 'Please wait for the host to approve. Enter the campus with the pass code within your booked time.',
    again: 'Book again',
    invalidIdCard: 'Please enter a valid 18-digit ID number',
    invalidPhone: 'Please enter a valid mobile number',
    maskNote: 'Only masked information is displayed after submission',
    status: 'Status'
  },
  lost: {
    title: 'Lost & Found',
    subtitle: 'Register found items. Owners can claim them at the security office with the item number.',
    keyword: 'Search item or location',
    category: 'Category',
    empty: 'No items waiting to be claimed',
    report: 'Register found item',
    reportTitle: 'Register a found item',
    itemName: 'Item name',
    location: 'Found location',
    description: 'Description',
    finder: 'Finder',
    storage: 'Storage location',
    reportSuccess: 'Registered. Please deliver the item to the security office.',
    foundAt: 'Found at',
    itemNo: 'No.',
    storageLabel: 'Storage'
  },
  news: {
    title: 'Safety News',
    subtitle: 'Campus safety updates, notices and prevention tips',
    detail: 'Article',
    views: 'views',
    empty: 'No articles'
  },
  school: {
    title: 'School Portal',
    duty: '24/7 duty phone',
    intro: 'Campus safety system',
    contact: 'Contact us'
  },
  share: {
    title: 'Shared item',
    open: 'Open full page',
    source: 'Source: WeChat share'
  },
  error: {
    load: 'Failed to load, please try again later',
    tenant: 'School portal not found or disabled'
  }
}

const saved = localStorage.getItem('bwc_portal_locale')
const query = new URLSearchParams(location.search).get('lang')
const browserLang = (navigator.language || '').toLowerCase()
const locale = query === 'en-US' || query === 'zh-CN'
  ? query
  : (saved || (browserLang.startsWith('en') ? 'en-US' : 'zh-CN'))

export default createI18n({
  legacy: false,
  locale,
  fallbackLocale: 'zh-CN',
  messages: { 'zh-CN': zh, 'en-US': en }
})
