function pad(value) {
  return value < 10 ? `0${value}` : `${value}`
}

function formatDate(value, withTime) {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  return withTime === false ? text.slice(0, 10) : text.slice(0, 16)
}

function mask(value) {
  const text = String(value || '')
  if (!text) return ''
  if (text.length <= 7) return '*'.repeat(text.length)
  return `${text.slice(0, 3)}${'*'.repeat(text.length - 7)}${text.slice(-4)}`
}

function nowString() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function statusLabel(status) {
  const map = {
    PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消',
    EXPIRED: '已过期', NORMAL: '正常', HANDLING: '处理中', DONE: '已完成',
    CLOSED: '已关闭', FOUND: '待认领', CLAIMED: '已认领', DISABLED: '停用',
    PUBLISHED: '已发布', ASSIGNED: '已分派', OVERDUE: '逾期'
  }
  return map[status] || status || '-'
}

function statusClass(status) {
  if (['APPROVED', 'NORMAL', 'DONE', 'CLOSED', 'CLAIMED', 'PUBLISHED'].indexOf(status) >= 0) return 'ok'
  if (['PENDING', 'HANDLING', 'ASSIGNED', 'OVERDUE'].indexOf(status) >= 0) return 'warn'
  if (['REJECTED', 'EXPIRED', 'DISABLED'].indexOf(status) >= 0) return 'danger'
  return 'muted'
}

module.exports = { formatDate, mask, nowString, statusLabel, statusClass, pad }
