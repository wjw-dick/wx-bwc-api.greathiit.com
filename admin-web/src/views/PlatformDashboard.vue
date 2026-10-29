<template>
  <div v-loading="loading">
    <div class="page-card hero">
      <div>
        <h2 class="page-title">平台全部学校看板</h2>
        <p class="page-subtitle">跨校汇总 · 支持按学校只读下钻 · 数据更新时间 {{ data.generatedAt || '-' }}</p>
      </div>
      <div class="flex">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Download" @click="exportReport">导出跨校报表</el-button>
      </div>
    </div>

    <div class="stat-grid mt-16">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="14">
        <div class="page-card">
          <h3 class="section-title">各校访客与车辆</h3>
          <ChartBox :option="barOption" height="330px" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="page-card">
          <h3 class="section-title">各校闭环率</h3>
          <ChartBox :option="rateOption" height="330px" />
        </div>
      </el-col>
    </el-row>

    <div class="page-card mt-16">
      <div class="toolbar">
        <h3 class="section-title">学校明细</h3>
        <div class="spacer" />
        <el-input v-model="keyword" placeholder="搜索学校名称/编码" clearable style="width: 220px" />
      </div>
      <el-table :data="filteredSchools" stripe border>
        <el-table-column prop="tenantName" label="学校" min-width="160" />
        <el-table-column prop="tenantCode" label="编码" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'NORMAL' ? 'success' : 'danger'" size="small">
              {{ row.status === 'NORMAL' ? '正常' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户" width="80" />
        <el-table-column prop="visitorCount" label="访客" width="80" />
        <el-table-column prop="visitToday" label="今日到访" width="95" />
        <el-table-column prop="vehicleCount" label="车辆" width="80" />
        <el-table-column prop="eventCount" label="事件" width="80" />
        <el-table-column label="隐患整改率" width="110">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.hazardRate || 0)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column label="巡逻完成率" width="110">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.patrolRate || 0)" :stroke-width="8" color="#10b981" />
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="到期时间" width="165" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="drill(row)">只读下钻</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'
import { useAuthStore } from '@/store/auth'
import ChartBox from '@/components/ChartBox.vue'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const data = ref<Record<string, any>>({ schools: [] })

const schools = computed<any[]>(() => data.value.schools || [])
const filteredSchools = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return schools.value
  return schools.value.filter((s) =>
    String(s.tenantName || '').toLowerCase().includes(kw) || String(s.tenantCode || '').toLowerCase().includes(kw))
})

const cards = computed(() => {
  const list = schools.value
  const sum = (key: string) => list.reduce((acc, item) => acc + Number(item[key] || 0), 0)
  return [
    { label: '学校总数', value: list.length, hint: `正常 ${list.filter((s) => s.status === 'NORMAL').length}` },
    { label: '用户总数', value: sum('userCount'), hint: '全部学校账号' },
    { label: '访客总数', value: sum('visitorCount'), hint: `今日到访 ${sum('visitToday')}` },
    { label: '车辆总数', value: sum('vehicleCount'), hint: `识别记录 ${sum('plateLogCount')}` },
    { label: '事件总数', value: sum('eventCount'), hint: `火警 ${sum('alarmCount')}` },
    { label: '平均隐患整改率', value: `${avg('hazardRate')}%`, hint: '跨校平均' }
  ]
})

function avg(key: string) {
  if (!schools.value.length) return 0
  return Math.round(schools.value.reduce((acc, item) => acc + Number(item[key] || 0), 0) / schools.value.length * 10) / 10
}

const barOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  grid: { left: 40, right: 20, top: 30, bottom: 50 },
  xAxis: { type: 'category', data: schools.value.map((s) => s.tenantCode), axisLabel: { rotate: 30 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } } },
  series: [
    { name: '访客', type: 'bar', barWidth: 14, itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] }, data: schools.value.map((s) => s.visitorCount) },
    { name: '车辆', type: 'bar', barWidth: 14, itemStyle: { color: '#8b5cf6', borderRadius: [4, 4, 0, 0] }, data: schools.value.map((s) => s.vehicleCount) },
    { name: '事件', type: 'bar', barWidth: 14, itemStyle: { color: '#f59e0b', borderRadius: [4, 4, 0, 0] }, data: schools.value.map((s) => s.eventCount) }
  ]
}))

const rateOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v}%` },
  legend: { bottom: 0 },
  grid: { left: 40, right: 20, top: 30, bottom: 50 },
  xAxis: { type: 'category', data: schools.value.map((s) => s.tenantCode), axisLabel: { rotate: 30 } },
  yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#eef2f7' } } },
  series: [
    { name: '隐患整改率', type: 'line', smooth: true, data: schools.value.map((s) => s.hazardRate), itemStyle: { color: '#f59e0b' } },
    { name: '巡逻完成率', type: 'line', smooth: true, data: schools.value.map((s) => s.patrolRate), itemStyle: { color: '#10b981' } },
    { name: '消防完好率', type: 'line', smooth: true, data: schools.value.map((s) => s.fireIntactRate), itemStyle: { color: '#ef4444' } }
  ]
}))

async function load() {
  loading.value = true
  try {
    data.value = (await http.get('/dashboard/platform')) as any
  } finally {
    loading.value = false
  }
}

async function exportReport() {
  await download('/reports/export', { module: 'overview' }, '平台跨校汇总.xlsx')
  ElMessage.success('已导出')
}

function drill(row: any) {
  auth.setTenantHeader(row.tenantId)
  ElMessage.success(`已切换到 ${row.tenantName}（只读）`)
  router.push('/dashboard')
  setTimeout(() => window.location.reload(), 120)
}

onMounted(load)
</script>

<style scoped>
.hero { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
</style>
