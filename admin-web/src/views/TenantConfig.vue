<template>
  <div v-loading="loading">
    <div class="page-card toolbar">
      <div>
        <h2 class="page-title">学校配置</h2>
        <div class="page-subtitle">{{ auth.schoolName }} · 校门开放时间、访客规则、通知与品牌配置</div>
      </div>
      <div class="spacer" />
      <el-button :icon="Plus" @click="addRow">新增配置项</el-button>
      <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存配置</el-button>
    </div>

    <div class="page-card">
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="敏感字段说明"
        description="密钥类配置保存时自动加密，页面仅展示掩码；留空或保持掩码不会覆盖原值。"
        class="mb-12"
      />
      <el-table :data="rows" border>
        <el-table-column label="配置键" width="300">
          <template #default="{ row }">
            <el-input v-model="row.key" placeholder="如 visitor.needApproval" />
          </template>
        </el-table-column>
        <el-table-column label="配置值" min-width="360">
          <template #default="{ row }">
            <el-input v-model="row.value" :type="row.secret ? 'password' : 'text'" show-password />
          </template>
        </el-table-column>
        <el-table-column label="说明" width="220">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="选填" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Plus } from '@element-plus/icons-vue'
import http from '@/api/http'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const rows = ref<Array<{ key: string; value: string; remark: string; secret: boolean }>>([])

function isSecret(key: string) {
  const lower = key.toLowerCase()
  return lower.includes('secret') || lower.includes('password') || lower.endsWith('_key') || lower.includes('private')
}

async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/system/tenant-config')
    rows.value = Object.entries(data || {}).map(([key, value]) => ({
      key,
      value: value === null || value === undefined ? '' : String(value),
      remark: '',
      secret: isSecret(key)
    }))
  } finally {
    loading.value = false
  }
}

function addRow() {
  rows.value.push({ key: '', value: '', remark: '', secret: false })
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
}

async function save() {
  const payload: Record<string, string> = {}
  for (const row of rows.value) {
    if (!row.key.trim()) continue
    payload[row.key.trim()] = row.value
  }
  saving.value = true
  try {
    await http.put('/system/tenant-config', payload)
    ElMessage.success('学校配置已保存')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
