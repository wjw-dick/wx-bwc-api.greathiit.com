import { createI18n } from 'vue-i18n'

const zh = {
  common: {
    search: '搜索', reset: '重置', add: '新增', edit: '编辑', delete: '删除', detail: '详情',
    save: '保存', cancel: '取消', export: '导出', confirm: '确定', loading: '加载中',
    actions: '操作', total: '共 {total} 条', success: '操作成功', keyword: '关键字'
  },
  nav: {
    dashboard: '工作台', platform: '平台管理', system: '系统管理', visitor: '门禁访客',
    vehicle: '车辆管理', event: '治安事件', fire: '消防管理', patrol: '巡逻管理',
    lost: '失物招领', education: '安全宣传', report: '报备审批', device: '设备运维',
    emergency: '应急管理', logistics: '后勤管理', reports: '报表中心'
  },
  login: {
    title: '校园保卫安防管理平台', subtitle: 'SaaS 多租户 · 门禁访客 · 消防隐患 · 巡逻防控',
    username: '账号', password: '密码', tenantCode: '学校编码（可选）', platform: '平台端',
    tenant: '学校端', submit: '登 录', tip: '默认密码 Admin{\'@\'}123'
  }
}

const en = {
  common: {
    search: 'Search', reset: 'Reset', add: 'Add', edit: 'Edit', delete: 'Delete', detail: 'Detail',
    save: 'Save', cancel: 'Cancel', export: 'Export', confirm: 'Confirm', loading: 'Loading',
    actions: 'Actions', total: 'Total {total}', success: 'Success', keyword: 'Keyword'
  },
  nav: {
    dashboard: 'Dashboard', platform: 'Platform', system: 'System', visitor: 'Visitors',
    vehicle: 'Vehicles', event: 'Events', fire: 'Fire', patrol: 'Patrol', lost: 'Lost & Found',
    education: 'Education', report: 'Approvals', device: 'Devices', emergency: 'Emergency',
    logistics: 'Logistics', reports: 'Reports'
  },
  login: {
    title: 'Campus Security Platform', subtitle: 'Multi-tenant SaaS · Access · Fire Safety · Patrol',
    username: 'Username', password: 'Password', tenantCode: 'School code (optional)', platform: 'Platform',
    tenant: 'School', submit: 'Sign in', tip: 'Default password Admin{\'@\'}123'
  }
}

export default createI18n({
  legacy: false,
  locale: localStorage.getItem('bwc_locale') || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: { 'zh-CN': zh, 'en-US': en }
})
