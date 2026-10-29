<template>
  <div v-loading="loading">
    <div class="page-card toolbar">
      <div>
        <h2 class="page-title">租户报表</h2>
        <div class="page-subtitle">本校安全运行汇总 · 按模块统计 · 支持 Excel 导出</div>
      </div>
      <div class="spacer" />
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
      <el-button type="primary" :icon="Search" @click="loadModule">查询</el-button>
      <el-button :icon="Download" @click="exportData">导出</el-button>
    </div>

    <div class="stat-grid">
      <div v-for="card in overviewCards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </div>
    </div>

    <div class="page-card mt-16">
      <div class="toolbar">
        <h3 class="section-title">模块统计</h3>
        <div class="spacer" />
        <el-radio-group v-model="module" @change="loadModule">
          <el-radio-button value="visitor">访客</el-radio-button>
          <el-radio-button value="vehicle">车辆</el-radio-button>
          <el-radio-button value="event">事件</el-radio-button>
          <el-radio-button value="fire">消防</el-radio-button>
          <el-radio-button value="patrol">巡逻</el-radio-button>
          <el-radio-button value="lost">失物</el-radio-button>
          <el-radio-button value="report">报备</el-radio-button>
        </el-radio-group>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="12">
          <ChartBox :option="chartOption" height="330px" />
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-descriptions :column="1" border>
            <el-descriptions-item v-for="item in moduleRows" :key="item.key" :label="labelOf(item.key)">
              {{ item.value }}
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'

const loading = ref(false)
const module = ref('visitor')
const range = ref<[string, string] | null>(null)
const overview = ref<Record<string, any>>({})
const moduleData = ref<Record<string, any>>({})

const overviewCards = computed(() => [
  { label: '访客预约', value: overview.value.visitorTotal ?? 0, hint: `今日 ${overview.value.visitorToday ?? 0}` },
  { label: '车辆总数', value: overview.value.vehicleTotal ?? 0, hint: `今日车行 ${overview.value.vehicleLogToday ?? 0}` },
  { label: '治安事件', value: overview.value.eventTotal ?? 0, hint: `未闭环 ${overview.value.eventOpen ?? 0}` },
  { label: '消防隐患', value: overview.value.hazardTotal ?? 0, hint: `整改率 ${overview.value.hazardRate ?? 0}%` },
  { label: '巡逻完成率', value: `${overview.value.patrolRate ?? 0}%`, hint: `今日打卡 ${overview.value.patrolRecordToday ?? 0}` },
  { label: '失物认领率', value: `${overview.value.lostClaimRate ?? 0}%`, hint: `待认领 ${overview.value.lostOpen ?? 0}` }
])

const moduleRows = computed(() => Object.entries(moduleData.value)
  .filter(([key, value]) => !['module', 'start', 'end'].includes(key) && (typeof value === 'number' || typeof value === 'string'))
  .map(([key, value]) => ({ key, value })))

const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 40 },
  xAxis: {
    type: 'category',
    data: moduleRows.value.map((r) => labelOf(r.key)),
    axisLabel: { rotate: 30, interval: 0 }
  },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } } },
  series: [{
    type: 'bar',
    barWidth: 24,
    itemStyle: {
      borderRadius: [6, 6, 0, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#bfdbfe' }] }
    },
    data: moduleRows.value.map((r) => (typeof r.value === 'number' ? r.value : 0))
  }]
}))

const labels: Record<string, string> = {
  total: '总数', approved: '已通过', pending: '待处理', rejected: '已驳回',
  records: '通行记录', logs: '车行记录', plateLogs: '车牌识别', violations: '违章',
  closed: '已关闭', open: '未闭环', facilities: '设施数', hazards: '隐患数', alarms: '报警数',
  tasks: '巡逻任务', points: '点位数', normal: '正常', abnormal: '异常', late: '迟到',
  items: '失物数', claimed: '已认领', claims: '认领申请', applies: '报备数',
  onTimeRate: '准时率', completeRate: '完成率', claimRate: '认领率', intactRate: '完好率'
}

function labelOf(key: string) {
  return labels[key] || key
}

async function loadOverview() {
  overview.value = (await http.get('/reports/tenant')) as any
}

async function loadModule() {
  loading.value = true
  try {
    const params: any = {}
    if (range.value) {
      params.start = range.value[0]
      params.end = range.value[1]
    }
    moduleData.value = (await http.get(`/reports/${module.value}`, { params })) as any
  } finally {
    loading.value = false
  }
}

async function exportData() {
  const params: any = { module: module.value }
  if (range.value) {
    params.start = range.value[0]
    params.end = range.value[1]
  }
  await download('/reports/export', params, `租户报表-${module.value}.xlsx`)
  ElMessage.success('已导出')
}

onMounted(async () => {
  await loadOverview()
  await loadModule()
})
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
</style>
