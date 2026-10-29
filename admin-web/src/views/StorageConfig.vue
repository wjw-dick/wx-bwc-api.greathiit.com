<template>
  <div v-loading="loading">
    <div class="page-card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">对象存储配置</h2>
          <div class="page-subtitle">本地 / MinIO / 阿里云 OSS / 腾讯云 COS / 七牛云，AK/SK 加密存储</div>
        </div>
        <div class="spacer" />
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="configs" border stripe>
        <el-table-column prop="driver" label="驱动" width="140">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ driverLabel(row.driver) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配置摘要" min-width="360">
          <template #default="{ row }">
            <span class="muted">{{ summarize(row.config) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastTestTime" label="最近测试" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card mt-16">
      <h3 class="section-title">{{ form.id ? '编辑存储驱动' : '配置存储驱动' }}</h3>
      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :md="8">
            <el-form-item label="存储驱动">
              <el-select v-model="form.driver" class="full" @change="onDriverChange">
                <el-option v-for="driver in drivers" :key="driver" :label="driverLabel(driver)" :value="driver" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-for="field in fields" :key="field.key" :xs="24" :md="8">
            <el-form-item :label="field.label">
              <el-input
                v-model="form.config[field.key]"
                :type="field.secret ? 'password' : 'text'"
                :show-password="field.secret"
                :placeholder="field.placeholder"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="flex">
          <el-button :loading="testing" :icon="Connection" @click="test">连接测试</el-button>
          <el-button type="primary" :loading="saving" :icon="Check" @click="save">保存并启用</el-button>
          <span v-if="testMessage" class="muted">{{ testMessage }}</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Connection, Refresh } from '@element-plus/icons-vue'
import http from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const testMessage = ref('')
const configs = ref<any[]>([])
const drivers = ref<string[]>(['local', 'minio', 'aliyun-oss', 'tencent-cos', 'qiniu'])
const form = reactive<any>({ id: null, driver: 'local', config: {} as Record<string, any> })

const driverFields: Record<string, Array<{ key: string; label: string; secret?: boolean; placeholder?: string }>> = {
  local: [],
  minio: [
    { key: 'endpoint', label: 'Endpoint', placeholder: 'http://127.0.0.1:9000' },
    { key: 'accessKey', label: 'Access Key' },
    { key: 'secretKey', label: 'Secret Key', secret: true },
    { key: 'bucket', label: 'Bucket', placeholder: 'bwc' },
    { key: 'publicUrl', label: '访问域名', placeholder: 'https://cdn.example.com/bwc' },
    { key: 'autoCreateBucket', label: '自动创建 Bucket', placeholder: 'true' }
  ],
  'aliyun-oss': [
    { key: 'endpoint', label: 'Endpoint', placeholder: 'oss-cn-hangzhou.aliyuncs.com' },
    { key: 'bucket', label: 'Bucket' },
    { key: 'accessKey', label: 'AccessKeyId' },
    { key: 'secretKey', label: 'AccessKeySecret', secret: true },
    { key: 'publicUrl', label: '访问域名' }
  ],
  'tencent-cos': [
    { key: 'endpoint', label: 'Endpoint', placeholder: 'cos.ap-guangzhou.myqcloud.com' },
    { key: 'bucket', label: 'Bucket' },
    { key: 'accessKey', label: 'SecretId' },
    { key: 'secretKey', label: 'SecretKey', secret: true },
    { key: 'publicUrl', label: '访问域名' }
  ],
  qiniu: [
    { key: 'endpoint', label: '上传域名' },
    { key: 'bucket', label: 'Bucket' },
    { key: 'accessKey', label: 'AccessKey' },
    { key: 'secretKey', label: 'SecretKey', secret: true },
    { key: 'publicUrl', label: '访问域名' }
  ]
}

const fields = computed(() => driverFields[form.driver] || [])

function driverLabel(driver: string) {
  return ({ local: '本地存储', minio: 'MinIO', 'aliyun-oss': '阿里云 OSS', 'tencent-cos': '腾讯云 COS', qiniu: '七牛云' } as Record<string, string>)[driver] || driver
}

function summarize(config: any) {
  if (!config) return '-'
  try {
    const data = typeof config === 'string' ? JSON.parse(config) : config
    return Object.entries(data).map(([k, v]) => `${k}=${v}`).join('  ')
  } catch (e) {
    return String(config)
  }
}

async function load() {
  loading.value = true
  try {
    const [list, driverList]: any = await Promise.all([
      http.get('/system/storage'),
      http.get('/system/storage/drivers')
    ])
    configs.value = list || []
    if (Array.isArray(driverList) && driverList.length) drivers.value = driverList
  } finally {
    loading.value = false
  }
}

function onDriverChange() {
  form.config = {}
}

function edit(row: any) {
  form.id = row.id
  form.driver = row.driver
  try {
    form.config = typeof row.config === 'string' ? JSON.parse(row.config) : { ...row.config }
  } catch (e) {
    form.config = {}
  }
}

async function test() {
  testing.value = true
  testMessage.value = ''
  try {
    const result: any = await http.post('/system/storage/test', { driver: form.driver, config: form.config })
    testMessage.value = result.message || ''
    ElMessage[result.success ? 'success' : 'warning'](result.message || '测试完成')
  } finally {
    testing.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await http.post('/system/storage/save', { driver: form.driver, config: form.config })
    ElMessage.success('存储配置已保存并启用')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 12px; }
</style>
