<template>
  <div v-loading="loading">
    <div class="page-card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">车牌识别接口配置</h2>
          <div class="page-subtitle">第三方停车平台对接 · 密钥加密 · 连接测试 · 相机同步 · 白名单下发</div>
        </div>
        <div class="spacer" />
        <el-tag :type="config.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
          {{ config.status === 'ENABLED' ? '已启用' : '未启用' }}
        </el-tag>
      </div>

      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :md="10">
            <el-form-item label="接口 BASE 地址">
              <el-input v-model="form.baseUrl" placeholder="https://parking.example.com" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="API Key">
              <el-input v-model="form.apiKey" type="password" show-password placeholder="留空或保持掩码不修改" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="6">
            <el-form-item label="MQTT 状态">
              <el-input :model-value="config.mqttConnected ? '已连接' : '未连接'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="flex">
          <el-button :loading="testing" :icon="Connection" @click="test">连接测试</el-button>
          <el-button type="primary" :loading="saving" :icon="Check" @click="save">保存配置</el-button>
          <span class="muted">最近测试：{{ config.lastTestTime || '-' }}</span>
        </div>
      </el-form>
    </div>

    <div class="page-card mt-16">
      <div class="toolbar">
        <h3 class="section-title">相机列表</h3>
        <div class="spacer" />
        <el-button :icon="Refresh" :loading="syncing" @click="syncCameras">同步相机</el-button>
      </div>
      <el-table :data="cameras" border stripe>
        <el-table-column prop="camId" label="相机编号" width="140" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="campus" label="校区/区域" width="140" />
        <el-table-column prop="camType" label="类型" width="110" />
        <el-table-column prop="syncStatus" label="同步状态" width="110" />
        <el-table-column prop="syncTime" label="同步时间" width="170" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card mt-16">
      <h3 class="section-title">车牌授权操作</h3>
      <p class="muted">对接未配置或第三方不可用时，接口会返回明确错误，不影响系统内车辆台账与预约流程。</p>
      <el-row :gutter="12">
        <el-col :xs="24" :md="6">
          <el-input v-model="plateForm.plateNo" placeholder="车牌号，如 京A12345" />
        </el-col>
        <el-col :xs="24" :md="6">
          <el-button type="primary" plain @click="authAction('add')">下发单车牌</el-button>
        </el-col>
        <el-col :xs="24" :md="6">
          <el-button type="success" plain @click="authAction('add-all')">全相机下发</el-button>
        </el-col>
        <el-col :xs="24" :md="6">
          <el-button type="danger" plain @click="authAction('del')">删除车牌授权</el-button>
        </el-col>
      </el-row>
      <div class="mt-16">
        <el-input v-model="authResult" type="textarea" :rows="5" readonly placeholder="接口返回结果" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Connection, Refresh } from '@element-plus/icons-vue'
import http from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const syncing = ref(false)
const config = ref<Record<string, any>>({})
const cameras = ref<any[]>([])
const form = reactive<any>({ baseUrl: '', apiKey: '' })
const plateForm = reactive({ plateNo: '' })
const authResult = ref('')

async function load() {
  loading.value = true
  try {
    config.value = (await http.get('/plate/config')) as any
    form.baseUrl = config.value.baseUrl || ''
    form.apiKey = config.value.apiKeyMask || ''
    cameras.value = (await http.get('/plate/cameras')) as any
  } finally {
    loading.value = false
  }
}

async function test() {
  testing.value = true
  try {
    const result: any = await http.post('/plate/test', { baseUrl: form.baseUrl, apiKey: form.apiKey })
    ElMessage[result.success ? 'success' : 'warning'](result.success ? '连接成功' : (result.message || '连接失败'))
    config.value.mqttConnected = result.mqttConnected
    await load()
  } finally {
    testing.value = false
  }
}

async function save() {
  saving.value = true
  try {
    config.value = (await http.put('/plate/config', {
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      tested: !!config.value.lastTestTime,
      mqttConnected: !!config.value.mqttConnected
    })) as any
    ElMessage.success('车牌接口配置已保存')
  } finally {
    saving.value = false
  }
}

async function syncCameras() {
  syncing.value = true
  try {
    cameras.value = (await http.post('/plate/cameras/sync')) as any
    ElMessage.success('相机同步完成')
  } finally {
    syncing.value = false
  }
}

async function authAction(action: string) {
  if (!plateForm.plateNo) {
    ElMessage.warning('请输入车牌号')
    return
  }
  const result: any = await http.post(`/plate/auth/${action}`, { plateNo: plateForm.plateNo })
  authResult.value = JSON.stringify(result, null, 2)
  ElMessage.success('操作已提交')
}

onMounted(load)
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
</style>
