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
      <el-button v-if="canWrite" type="success" :icon="Plus" @click="openCreate">{{ t('common.add') }}</el-button>
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
          <el-button v-if="canWrite" link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-dropdown @command="(cmd: string) => runAction(cmd, row)">
            <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="action in actions" :key="action.key" :command="action.key">
                  {{ action.label }}
                </el-dropdown-item>
                <el-dropdown-item v-if="canWrite" command="delete" divided>删除</el-dropdown-item>
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
        <el-descriptions-item v-for="field in tableFields" :key="field.prop" :label="field.label">
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
  .filter((f) => f.type !== 'textarea' && f.type !== 'image' && !['description', 'content', 'configJson', 'itemsJson'].includes(f.prop))
  .slice(0, 9))
const formFields = computed(() => def.value.fields)

const actions = computed(() => {
  switch (props.resource) {
    case 'visitor-appointment':
      return [
        { key: 'approve', label: '审批通过' },
        { key: 'reject', label: '驳回' },
        { key: 'qr', label: '查看通行码' },
        { key: 'cancel', label: '取消预约' }
      ]
    case 'vehicle-appointment':
      return [{ key: 'approve', label: '审批通过' }, { key: 'reject', label: '驳回' }]
    case 'vehicle':
      return [{ key: 'issue', label: '下发固定车牌' }, { key: 'remove', label: '注销车牌' }]
    case 'report-apply':
      return [{ key: 'approve', label: '审批通过' }, { key: 'reject', label: '驳回' }, { key: 'pdf', label: '打印审批单' }]
    case 'monitor-access':
      return [{ key: 'approve', label: '初审通过' }, { key: 'leader', label: '校领导终审' }, { key: 'reject', label: '驳回' }]
    case 'device-repair':
      return [{ key: 'finish', label: '完成维修' }]
    case 'emergency-event':
      return [{ key: 'respond', label: '记录响应' }]
    case 'fire-facility':
      return [{ key: 'qr', label: '查看二维码' }]
    case 'hazard':
      return [{ key: 'pdf', label: '整改通知单' }]
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
    if (['status'].includes(field.prop)) data[field.prop] = 'NORMAL'
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
    if (form.id) {
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
  if (command === 'approve' || command === 'reject') {
    const approved = command === 'approve'
    await http.post(`/visitor/appointments/${row.id}/approve`, { approved }).catch(async () => {
      if (props.resource === 'vehicle-appointment') {
        await http.post(`/vehicle/appointments/${row.id}/approve`, { approved })
      } else if (props.resource === 'report-apply') {
        await http.post(`/report/applies/${row.id}/approve`, { approved })
      } else if (props.resource === 'monitor-access') {
        await http.post(`/report/monitor-access/${row.id}/approve`, { approved, stage: 'DEPT' })
      }
    })
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
