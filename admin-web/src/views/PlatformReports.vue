<template>
  <div v-loading="loading">
    <div class="page-card toolbar">
      <div>
        <h2 class="page-title">跨校报表</h2>
        <div class="page-subtitle">平台级跨学校汇总 · 支持逐校下钻查看</div>
      </div>
      <div class="spacer" />
      <el-button :icon="Refresh" @click="load">刷新</el-button>
      <el-button type="primary" :icon="Download" @click="exportData">导出跨校报表</el-button>
    </div>

    <div class="stat-grid">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </div>
    </div>

    <div class="page-card mt-16">
      <h3 class="section-title">学校运行指标</h3>
      <el-table :data="schools" stripe border show-summary>
        <el-table-column prop="tenantName" label="学校" min-width="160" fixed />
        <el-table-column prop="tenantCode" label="编码" width="110" />
        <el-table-column prop="userCount" label="用户数" width="90" />
        <el-table-column prop="visitorCount" label="访客预约" width="100" />
        <el-table-column prop="visitToday" label="今日到访" width="100" />
        <el-table-column prop="vehicleCount" label="车辆" width="85" />
        <el-table-column prop="plateLogCount" label="识别记录" width="100" />
        <el-table-column prop="eventCount" label="事件" width="80" />
        <el-table-column prop="hazardCount" label="隐患" width="80" />
        <el-table-column label="整改率" width="95">
          <template #default="{ row }">{{ row.hazardRate }}%</template>
        </el-table-column>
        <el-table-column label="巡逻率" width="95">
          <template #default="{ row }">{{ row.patrolRate }}%</template>
        </el-table-column>
        <el-table-column label="消防完好率" width="110">
          <template #default="{ row }">{{ row.fireIntactRate }}%</template>
        </el-table-column>
        <el-table-column label="认领率" width="90">
          <template #default="{ row }">{{ row.lostClaimRate }}%</template>
        </el-table-column>
        <el-table-column prop="alarmCount" label="火警" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'

const loading = ref(false)
const schools = ref<any[]>([])

const cards = computed(() => {
  const sum = (key: string) => schools.value.reduce((acc, item) => acc + Number(item[key] || 0), 0)
  return [
    { label: '学校数量', value: schools.value.length, hint: '全部租户' },
    { label: '用户总数', value: sum('userCount'), hint: '含各校管理员' },
    { label: '访客预约', value: sum('visitorCount'), hint: `今日到访 ${sum('visitToday')}` },
    { label: '车辆总数', value: sum('vehicleCount'), hint: `识别 ${sum('plateLogCount')}` },
    { label: '治安事件', value: sum('eventCount'), hint: `火警 ${sum('alarmCount')}` },
    { label: '消防隐患', value: sum('hazardCount'), hint: '全部隐患' }
  ]
})

async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/dashboard/platform')
    schools.value = data.schools || []
  } finally {
    loading.value = false
  }
}

async function exportData() {
  await download('/reports/export', { module: 'overview' }, '跨校报表.xlsx')
  ElMessage.success('已导出')
}

onMounted(load)
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 12px; }
</style>
