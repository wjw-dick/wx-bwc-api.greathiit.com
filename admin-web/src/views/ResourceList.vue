<template>
  <div class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">{{ def.title }}</h2>
        <div class="page-subtitle">模块：{{ def.module }} · 资源：{{ resource }}</div>
      </div>
      <div class="spacer" />
      <el-input v-model="keyword" :placeholder="t('common.keyword')" clearable style="width: 220px" @keyup.enter="reload" />
      <el-select v-if="statusField" v-model="status" clearable placeholder="状态" style="width: 140px" @change="reload">
        <el-option v-for="opt in statusField.options" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="reload">{{ t('common.search') }}</el-button>
      <el-button :icon="Refresh" @click="reset">{{ t('common.reset') }}</el-button>
      <el-button v-if="canWrite && !readOnlyResource" type="success" :icon="Plus" @click="openCreate">{{ t('common.add') }}</el-button>
      <el-button v-if="resource === 'visitor-appointment'" :icon="Download" @click="exportVisitor">导出</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe border style="width: 100%">
      <el-table-column type="index" width="54" />
      <el-table-column
        v-for="field in tableFields"
        :key="field.prop"
        :prop="field.prop"
        :label="field.label"
        :min-width="columnWidth(field)"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <el-tag v-if="isStatus(field)" :type="statusType(row[field.prop])" size="small">
            {{ optionLabel(field, row[field.prop]) }}
          </el-tag>
          <el-image
            v-else-if="field.type === 'image' && row[field.prop]"
            :src="firstImage(row[field.prop])"
            :preview-src-list="row[field.prop].split(',')"
            fit="cover"
            style="width: 46px; height: 46px; border-radius: 6px"
          />
          <span v-else>{{ display(field, row[field.prop]) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="165" />
      <el-table-column :label="t('common.actions')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
          <el-button v-if="canWrite && !readOnlyResource" link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-dropdown @command="(cmd: string) => runAction(cmd, row)">
            <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="action in actions" :key="action.key" :command="action.key">
                  {{ action.label }}
                </el-dropdown-item>
                <el-dropdown-item v-if="canWrite && !readOnlyResource" command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="reload"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? t('common.edit') + ' · ' + def.title : t('common.add') + ' · ' + def.title" width="760px" destroy-on-close>
      <el-form :model="form" label-width="110px" label-position="top">
        <el-row :gutter="16">
          <el-col v-for="field in formFields" :key="field.prop" :span="field.type === 'textarea' || field.type === 'image' ? 24 : 12">
            <el-form-item :label="field.label">
              <el-select v-if="field.options" v-model="form[field.prop]" clearable filterable allow-create class="full">
                <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" class="full" :controls="false" />
              <el-date-picker v-else-if="field.type === 'date'" v-model="form[field.prop]" type="date" value-format="YYYY-MM-DD" class="full" />
              <el-date-picker v-else-if="field.type === 'datetime'" v-model="form[field.prop]" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="full" />
              <el-switch v-else-if="field.type === 'switch'" v-model="form[field.prop]" :active-value="1" :inactive-value="0" />
              <UploadCard v-else-if="field.type === 'image'" v-model="form[field.prop]" :biz-type="resource" multiple />
              <RichEditor v-else-if="useRichText(field)" v-model="form[field.prop]" />
              <el-input v-else-if="field.type === 'textarea'" v-model="form[field.prop]" type="textarea" :rows="3" />
              <el-input v-else v-model="form[field.prop]" clearable />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="def.title + '详情'" size="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item v-for="field in detailFields" :key="field.prop" :label="field.label">
          {{ display(field, detailRow[field.prop]) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailRow.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="qrVisible" title="设施二维码（一物一码）" width="360px" align-center>
      <div style="text-align: center">
        <img v-if="qrUrl" :src="qrUrl" style="width: 260px; height: 260px" />
        <p class="muted">{{ qrTitle }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Download, ArrowDown } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import http, { download } from '@/api/http'
import { resources, type FieldDef } from '@/config/resources'
import { useAuthStore } from '@/store/auth'
import UploadCard from '@/components/UploadCard.vue'
import RichEditor from '@/components/RichEditor.vue'

const props = defineProps<{ resource: string }>()
const { t } = useI18n()
const auth = useAuthStore()
const def = computed(() => resources[props.resource] || { key: props.resource, module: 'system', title: props.resource, fields: [] })
const canWrite = computed(() => !(auth.isPlatform && auth.tenantHeader))
const readOnlyResource = computed(() => [
  'visit-record', 'vehicle-log', 'plate-log', 'audit-log', 'message-log', 'message'
].includes(props.resource))

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const status = ref('')
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const detailRow = ref<any>({})
const form = reactive<any>({})
const qrVisible = ref(false)
const qrUrl = ref('')
const qrTitle = ref('')

const statusField = computed<FieldDef | undefined>(() => def.value.fields.find((f) => f.prop === 'status'))
const tableFields = computed(() => def.value.fields
  .filter((f) => !f.hideInTable && f.type !== 'textarea' && f.type !== 'image' && !['description', 'content', 'configJson', 'itemsJson'].includes(f.prop))
  .slice(0, 9))
const formFields = computed(() => def.value.fields)
const detailFields = computed(() => def.value.fields.filter((f) => f.type !== 'image'))

const actions = computed(() => {
  switch (props.resource) {
    case 'visitor-appointment':
      return [
        { key: 'approve', label: '审批通过' },
        { key: 'reject', label: '驳回' },
        { key: 'qr', label: '查看通行码' },
        { key: 'check', label: '通行核验' },
        { key: 'cancel', label: '取消预约' }
      ]
    case 'temp-entry':
      return [{ key: 'out', label: '离校登记' }]
    case 'late-return':
      return [{ key: 'confirm', label: '确认晚归' }]
    case 'vehicle-appointment':
      return [{ key: 'approve', label: '审批通过' }, { key: 'reject', label: '驳回' }, { key: 'retry', label: '重试车牌下发' }]
    case 'vehicle':
      return [
        { key: 'approve', label: '备案通过' },
        { key: 'reject', label: '备案驳回' },
        { key: 'issue', label: '下发固定车牌' },
        { key: 'remove', label: '注销车牌' }
      ]
    case 'violation':
      return [{ key: 'handle', label: '处理完成' }]
    case 'security-event':
      return [
        { key: 'assign', label: '分派处置' },
        { key: 'handle', label: '记录处置' },
        { key: 'close', label: '结案归档' }
      ]
    case 'hazard':
      return [
        { key: 'assign', label: '分派整改' },
        { key: 'rectify', label: '提交整改' },
        { key: 'accept', label: '验收通过' },
        { key: 'rejectAccept', label: '验收退回' },
        { key: 'pdf', label: '整改通知单' }
      ]
    case 'device-repair':
      return [{ key: 'assign', label: '分派维修' }, { key: 'follow', label: '维修跟进' }, { key: 'finish', label: '完成维修' }]
    case 'feedback':
      return [{ key: 'handle', label: '处理反馈' }]
    case 'emergency-supply':
      return [{ key: 'borrow', label: '物资领用' }, { key: 'return', label: '物资归还' }]
    case 'lost-claim':
      return [{ key: 'approve', label: '核验通过' }, { key: 'reject', label: '驳回认领' }]
    case 'lost-disposal':
      return [{ key: 'approve', label: '处置通过' }, { key: 'reject', label: '处置驳回' }]
    case 'report-apply':
      return [{ key: 'approve', label: '审批通过' }, { key: 'reject', label: '驳回' }, { key: 'pdf', label: '打印审批单' }]
    case 'monitor-access':
      return [
        { key: 'approve', label: '初审通过' },
        { key: 'leader', label: '校领导终审' },
        { key: 'reject', label: '驳回' },
        { key: 'access', label: '调取登记' }
      ]
    case 'emergency-event':
      return [{ key: 'respond', label: '记录响应' }]
    case 'fire-facility':
      return [{ key: 'qr', label: '查看二维码' }]
    default:
      return []
  }
})

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (status.value) params.status = status.value
    const data: any = await http.get(`/crud/${props.resource}`, { params })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function reset() {
  keyword.value = ''
  status.value = ''
  reload()
}

function openCreate() {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, defaultForm())
  formVisible.value = true
}

function defaultForm() {
  const data: any = {}
  for (const field of def.value.fields) {
    // 状态留空，由后端/数据库按业务默认值落库（如访客预约 PENDING、公告 PUBLISHED）
    if (field.type === 'number') data[field.prop] = field.prop.toLowerCase().includes('count') ? 0 : undefined
    if (field.type === 'switch') data[field.prop] = 1
  }
  return data
}

function openEdit(row: any) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  formVisible.value = true
}

function openDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (!form.id && props.resource === 'temp-entry') {
      await http.post('/visitor/temp-entry', {
        visitorName: form.visitorName,
        phone: form.phone,
        idCard: form.idCard,
        reason: form.reason,
        visitUser: form.visitUser
      })
    } else if (!form.id && props.resource === 'late-return') {
      await http.post('/visitor/late-return', {
        studentName: form.studentName,
        dorm: form.dorm,
        type: form.type,
        reason: form.reason,
        recordTime: form.recordTime
      })
    } else if (form.id) {
      await http.put(`/crud/${props.resource}/${form.id}`, form)
    } else {
      await http.post(`/crud/${props.resource}`, form)
    }
    ElMessage.success(t('common.success'))
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function runAction(command: string, row: any) {
  if (command === 'delete') {
    await ElMessageBox.confirm('删除后不可恢复，确认删除？', '危险操作', { type: 'warning' })
    await http.delete(`/crud/${props.resource}/${row.id}`)
    ElMessage.success('已删除')
    load()
    return
  }
  if (command === 'check') {
    const { value } = await ElMessageBox.prompt('请输入访客通行码（扫码结果）', '通行核验', { inputPlaceholder: 'PASS...' })
    const data: any = await http.post('/visitor/check', { passCode: value, gate: '后台门岗', method: 'MANUAL' })
    ElMessage.success(data.direction === 'IN' ? '已登记入校' : '已登记离校')
    load()
    return
  }
  if (command === 'out') {
    await http.post(`/visitor/temp-entry/${row.id}/out`)
    ElMessage.success('已完成离校登记')
    load()
    return
  }
  if (command === 'confirm') {
    await http.post(`/visitor/late-return/${row.id}/confirm`)
    ElMessage.success('已确认晚归')
    load()
    return
  }
  if (command === 'retry') {
    await http.post(`/vehicle/appointments/${row.id}/retry`)
    ElMessage.success('已重新下发临时车牌')
    load()
    return
  }
  if (command === 'access') {
    const { value } = await ElMessageBox.prompt('请输入调取登记（谁调取、调取内容）', '调取登记', { inputType: 'textarea' })
    await http.post(`/report/monitor-access/${row.id}/access`, { accessRecord: value })
    ElMessage.success('已登记调取记录')
    load()
    return
  }
  if (command === 'assign') {
    if (props.resource === 'security-event') {
      const { value } = await ElMessageBox.prompt('请输入处置人姓名', '分派处置', { inputPlaceholder: '保安队长' })
      await http.post(`/event/${row.id}/assign`, { assignToName: value })
    } else if (props.resource === 'hazard') {
      const { value } = await ElMessageBox.prompt('请输入整改责任人姓名', '分派整改', { inputPlaceholder: '后勤维修' })
      const days = await ElMessageBox.prompt('请输入整改期限（天）', '整改期限', {
        inputValue: '7', inputPattern: /^\d+$/, inputErrorMessage: '请输入数字天数'
      })
      await http.post(`/fire/hazards/${row.id}/assign`, { assignToName: value, days: Number(days.value) })
    } else if (props.resource === 'device-repair') {
      const { value } = await ElMessageBox.prompt('请输入维修人姓名/班组', '分派维修')
      await http.post(`/device/repairs/${row.id}/assign`, { assignTo: value })
    }
    ElMessage.success('已分派')
    load()
    return
  }
  if (command === 'handle') {
    if (props.resource === 'security-event') {
      const { value } = await ElMessageBox.prompt('请输入现场处置情况', '记录处置', { inputType: 'textarea' })
      await http.post(`/event/${row.id}/handle`, { result: value })
    } else if (props.resource === 'feedback') {
      const { value } = await ElMessageBox.prompt('请输入处理结果', '处理反馈', { inputType: 'textarea' })
      await http.post(`/logistics/feedback/${row.id}/handle`, { status: 'DONE', result: value })
    } else if (props.resource === 'violation') {
      const { value } = await ElMessageBox.prompt('请输入违停处理结果', '处理违停', { inputType: 'textarea' })
      await http.post(`/vehicle/violations/${row.id}/handle`, { result: value })
    }
    ElMessage.success('已记录')
    load()
    return
  }
  if (command === 'close') {
    await ElMessageBox.confirm('结案后事件归档，确认结案？', '结案归档', { type: 'warning' })
    await http.post(`/event/${row.id}/close`, {})
    ElMessage.success('已结案')
    load()
    return
  }
  if (command === 'rectify') {
    const { value } = await ElMessageBox.prompt('请输入整改方案与完成情况', '提交整改', { inputType: 'textarea' })
    await http.post(`/fire/hazards/${row.id}/rectify`, { plan: value })
    ElMessage.success('已提交整改')
    load()
    return
  }
  if (command === 'accept' || command === 'rejectAccept') {
    const approved = command === 'accept'
    let remark = ''
    if (approved) {
      await ElMessageBox.confirm('确认整改到位并销号？', '验收通过', { type: 'warning' })
    } else {
      const res = await ElMessageBox.prompt('请输入退回原因', '验收退回')
      remark = res.value
    }
    await http.post(`/fire/hazards/${row.id}/accept`, { approved, remark })
    ElMessage.success(approved ? '已验收销号' : '已退回整改')
    load()
    return
  }
  if (command === 'follow') {
    const { value } = await ElMessageBox.prompt('请输入维修跟进情况', '维修跟进', { inputType: 'textarea' })
    await http.post(`/device/repairs/${row.id}/follow`, { content: value })
    ElMessage.success('已记录跟进')
    load()
    return
  }
  if (command === 'borrow' || command === 'return') {
    const { value } = await ElMessageBox.prompt('请输入数量', command === 'borrow' ? '物资领用' : '物资归还', {
      inputValue: '1', inputPattern: /^\d+$/, inputErrorMessage: '请输入数字'
    })
    await http.post(`/emergency/supplies/${row.id}/${command === 'borrow' ? 'borrow' : 'return'}`, { quantity: Number(value) })
    ElMessage.success(command === 'borrow' ? '已领用' : '已归还')
    load()
    return
  }
  if (command === 'approve' || command === 'reject') {
    const approved = command === 'approve'
    if (props.resource === 'vehicle-appointment') {
      await http.post(`/vehicle/appointments/${row.id}/approve`, { approved })
    } else if (props.resource === 'report-apply') {
      await http.post(`/report/applies/${row.id}/approve`, { approved })
    } else if (props.resource === 'monitor-access') {
      await http.post(`/report/monitor-access/${row.id}/approve`, { approved, stage: 'DEPT' })
    } else if (props.resource === 'lost-claim') {
      await http.post(`/lost/claims/${row.id}/approve`, { approved })
    } else if (props.resource === 'lost-disposal') {
      await http.post(`/lost/disposals/${row.id}/approve`, { approved })
    } else if (props.resource === 'vehicle') {
      await http.post(`/vehicle/vehicles/${row.id}/approve`, { approved })
    } else {
      await http.post(`/visitor/appointments/${row.id}/approve`, { approved })
    }
    ElMessage.success(approved ? '已通过' : '已驳回')
    load()
  } else if (command === 'leader') {
    await http.post(`/report/monitor-access/${row.id}/approve`, { approved: true, stage: 'LEADER' })
    ElMessage.success('终审通过')
    load()
  } else if (command === 'issue') {
    await http.post(`/vehicle/vehicles/${row.id}/issue`)
    ElMessage.success('固定车牌已下发')
    load()
  } else if (command === 'remove') {
    await http.post(`/vehicle/vehicles/${row.id}/remove`)
    ElMessage.success('车牌已注销')
    load()
  } else if (command === 'cancel') {
    await http.post(`/visitor/appointments/${row.id}/cancel`)
    ElMessage.success('已取消')
    load()
  } else if (command === 'qr') {
    if (props.resource === 'fire-facility') {
      const blob: any = await http.get(`/fire/facilities/${row.id}/qr`, { responseType: 'blob' })
      qrUrl.value = URL.createObjectURL(blob)
      qrTitle.value = row.facilityNo + ' / ' + (row.location || '')
    } else {
      const data: any = await http.get(`/visitor/appointments/${row.id}/qr`)
      qrUrl.value = `data:image/png;base64,${data.qrBase64}`
      qrTitle.value = data.passCode
    }
    qrVisible.value = true
  } else if (command === 'pdf') {
    const url = props.resource === 'hazard' ? `/print/hazard/${row.id}` : `/print/report/${row.id}`
    await download(url, undefined, `${props.resource}-${row.id}.pdf`)
  } else if (command === 'finish') {
    const { value } = await ElMessageBox.prompt('请输入维修结果', '完成维修', { inputType: 'textarea' })
    await http.post(`/device/repairs/${row.id}/finish`, { result: value })
    ElMessage.success('已完成')
    load()
  } else if (command === 'respond') {
    const { value } = await ElMessageBox.prompt('请输入应急响应记录', '记录响应', { inputType: 'textarea' })
    await http.post(`/emergency/events/${row.id}/respond`, { response: value })
    ElMessage.success('已记录')
    load()
  }
}

function exportVisitor() {
  download('/visitor/export', { status: status.value || undefined }, '访客台账.xlsx')
}

function isStatus(field: FieldDef) {
  return field.prop === 'status' || !!field.options
}

function optionLabel(field: FieldDef, value: any) {
  const found = field.options?.find((o) => o.value === value)
  return found ? found.label : value
}

function statusType(value: string) {
  if (!value) return 'info'
  if (['NORMAL', 'APPROVED', 'DONE', 'CLOSED', 'SENT', 'PUBLISHED', 'ACTIVE'].includes(value)) return 'success'
  if (['PENDING', 'ASSIGNED', 'HANDLING', 'RECTIFYING', 'PENDING_ACCEPT', 'QUEUED'].includes(value)) return 'warning'
  if (['REJECTED', 'DISABLED', 'EXPIRED', 'OVERDUE', 'FAILED'].includes(value)) return 'danger'
  return 'info'
}

function display(field: FieldDef, value: any) {
  if (value === null || value === undefined || value === '') return '-'
  if (field.options) return optionLabel(field, value)
  if (typeof value === 'boolean') return value ? '是' : '否'
  return value
}

function firstImage(value: string) {
  return String(value).split(',')[0]
}

function columnWidth(field: FieldDef) {
  if (field.type === 'datetime') return 160
  if (field.type === 'date') return 120
  return 130
}

function useRichText(field: FieldDef) {
  return field.prop === 'content' && ['notice', 'safety-course', 'warning', 'material', 'emergency-plan'].includes(props.resource)
}

watch(() => props.resource, () => { page.value = 1; keyword.value = ''; status.value = ''; load() })
onMounted(load)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
