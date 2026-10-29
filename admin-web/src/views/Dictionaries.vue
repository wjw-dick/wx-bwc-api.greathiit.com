<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">数据字典</h2>
        <div class="page-subtitle">全局字典由平台维护，租户字典支持学校自定义扩展</div>
      </div>
      <div class="spacer" />
      <el-input v-model="keyword" placeholder="搜索字典" clearable style="width: 200px" />
      <el-select v-model="typeFilter" clearable placeholder="字典类型" style="width: 160px">
        <el-option v-for="type in types" :key="type" :label="type" :value="type" />
      </el-select>
      <el-button type="success" :icon="Plus" @click="openCreate">新增字典</el-button>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="全局字典" name="global">
        <el-table :data="filterRows(globalRows)" stripe border>
          <el-table-column prop="dictType" label="字典类型" min-width="160" />
          <el-table-column prop="dictLabel" label="显示名称" min-width="150" />
          <el-table-column prop="dictValue" label="字典值" min-width="140" />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">
                {{ row.status === 'NORMAL' ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row, 'global')">编辑</el-button>
              <el-button link type="danger" @click="remove(row, 'global')">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="租户字典" name="tenant">
        <el-table :data="filterRows(tenantRows)" stripe border>
          <el-table-column prop="dictType" label="字典类型" min-width="160" />
          <el-table-column prop="dictLabel" label="显示名称" min-width="150" />
          <el-table-column prop="dictValue" label="字典值" min-width="140" />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">
                {{ row.status === 'NORMAL' ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row, 'tenant')">编辑</el-button>
              <el-button link type="danger" @click="remove(row, 'tenant')">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="visible" :title="form.id ? '编辑字典' : '新增字典'" width="520px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字典类型">
              <el-input v-model="form.dictType" placeholder="如 visitor_status" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示名称">
              <el-input v-model="form.dictLabel" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典值">
              <el-input v-model="form.dictValue" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" class="full" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" class="full">
                <el-option label="正常" value="NORMAL" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const tab = ref('global')
const keyword = ref('')
const typeFilter = ref('')
const globalRows = ref<any[]>([])
const tenantRows = ref<any[]>([])
const form = reactive<any>({ id: null, dictType: '', dictLabel: '', dictValue: '', sort: 0, status: 'NORMAL', remark: '' })

const types = computed(() => Array.from(new Set([...globalRows.value, ...tenantRows.value].map((r) => r.dictType))).filter(Boolean))

function filterRows(rows: any[]) {
  return rows.filter((row) => {
    const kw = keyword.value.trim().toLowerCase()
    const matchKeyword = !kw || [row.dictType, row.dictLabel, row.dictValue, row.remark]
      .some((v) => String(v || '').toLowerCase().includes(kw))
    const matchType = !typeFilter.value || row.dictType === typeFilter.value
    return matchKeyword && matchType
  })
}

async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/system/dicts')
    globalRows.value = data.global || []
    tenantRows.value = data.tenant || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, dictType: typeFilter.value || '', dictLabel: '', dictValue: '', sort: 0, status: 'NORMAL', remark: '' })
  visible.value = true
}

function openEdit(row: any, scope: string) {
  Object.assign(form, row)
  ;(form as any).__scope = scope
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    const scope = form.id ? (form as any).__scope : (auth.isPlatform && tab.value === 'global' ? 'global' : 'tenant')
    if (form.id) {
      await http.put(`/system/dicts/${scope}/${form.id}`, form)
    } else {
      await http.post(`/system/dicts/${scope}`, form)
    }
    ElMessage.success('已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any, scope: string) {
  await ElMessageBox.confirm(`确认删除字典「${row.dictLabel}」？`, '提示', { type: 'warning' })
  await http.delete(`/system/dicts/${scope}/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
