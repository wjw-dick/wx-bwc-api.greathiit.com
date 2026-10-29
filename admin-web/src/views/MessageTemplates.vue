<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">消息模板</h2>
        <div class="page-subtitle">公众号模板消息 / 小程序订阅消息 / 站内消息模板管理</div>
      </div>
      <div class="spacer" />
      <el-input v-model="keyword" placeholder="搜索模板" clearable style="width: 200px" />
      <el-button type="success" :icon="Plus" @click="openCreate">新增模板</el-button>
    </div>

    <el-table :data="filtered" stripe border>
      <el-table-column prop="bizType" label="业务类型" width="150" />
      <el-table-column prop="channel" label="渠道" width="110" />
      <el-table-column prop="templateId" label="模板ID" min-width="180" show-overflow-tooltip />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="fieldsJson" label="字段映射" min-width="220" show-overflow-tooltip />
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
            {{ row.enabled === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑模板' : '新增模板'" width="600px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="业务类型">
              <el-select v-model="form.bizType" class="full" filterable allow-create>
                <el-option v-for="item in bizTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道">
              <el-select v-model="form.channel" class="full">
                <el-option label="公众号模板消息" value="MP" />
                <el-option label="小程序订阅消息" value="MINI" />
                <el-option label="站内消息" value="SITE" />
                <el-option label="短信" value="SMS" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板ID"><el-input v-model="form.templateId" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否启用">
              <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="字段映射 JSON">
              <el-input v-model="form.fieldsJson" type="textarea" :rows="5" placeholder='{"first":"您有新的访客预约","keyword1":"访客姓名"}' />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const keyword = ref('')
const rows = ref<any[]>([])
const form = reactive<any>({ id: null, bizType: 'VISITOR', channel: 'MP', templateId: '', title: '', fieldsJson: '', enabled: 1 })

const bizTypes = [
  { label: '访客预约', value: 'VISITOR' },
  { label: '访客审批', value: 'VISITOR_APPROVE' },
  { label: '车辆预约', value: 'VEHICLE' },
  { label: '隐患整改', value: 'HAZARD' },
  { label: '巡逻异常', value: 'PATROL' },
  { label: '失物认领', value: 'LOST' },
  { label: '报备审批', value: 'REPORT' },
  { label: '设备维修', value: 'DEVICE' },
  { label: '应急事件', value: 'EMERGENCY' }
]

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter((row) =>
    [row.bizType, row.title, row.templateId].some((v) => String(v || '').toLowerCase().includes(kw)))
})

async function load() {
  loading.value = true
  try {
    rows.value = (await http.get('/system/message-templates')) as any
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, bizType: 'VISITOR', channel: 'MP', templateId: '', title: '', fieldsJson: '', enabled: 1 })
  visible.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) {
      await http.put(`/system/message-templates/${form.id}`, form)
    } else {
      await http.post('/system/message-templates', form)
    }
    ElMessage.success('模板已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除模板「${row.title}」？`, '提示', { type: 'warning' })
  await http.delete(`/system/message-templates/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
