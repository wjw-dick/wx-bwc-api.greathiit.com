<template>
  <div v-loading="loading">
    <div class="page-card toolbar">
      <div>
        <h2 class="page-title">访客统计</h2>
        <div class="page-subtitle">预约审批、到访趋势、通行码使用情况</div>
      </div>
      <div class="spacer" />
      <el-date-picker
        v-model="range"
        type="datetimerange"
        value-format="YYYY-MM-DD HH:mm:ss"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        style="width: 360px"
      />
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      <el-button :icon="Download" @click="exportData">导出台账</el-button>
    </div>

    <div class="stat-grid mt-16">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="15">
        <div class="page-card">
          <h3 class="section-title">预约到访趋势</h3>
          <ChartBox :option="trendOption" height="320px" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="9">
        <div class="page-card">
          <h3 class="section-title">审批状态分布</h3>
          <ChartBox :option="statusOption" height="320px" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'

const loading = ref(false)
const data = ref<Record<string, any>>({})
const range = ref<[string, string] | null>(null)

const cards = computed(() => [
  { label: '预约总数', value: data.value.total ?? 0, hint: '全部预约记录' },
  { label: '待审批', value: data.value.pending ?? 0, hint: '需要及时处理' },
  { label: '已通过', value: data.value.approved ?? 0, hint: `通过率 ${data.value.passRate ?? 0}%` },
  { label: '已驳回', value: data.value.rejected ?? 0, hint: '不合规来访' },
  {
    label: '今日到访',
    value: (data.value.todayVisits && data.value.todayVisits[0]?.total) || 0,
    hint: '门岗扫码通行'
  }
])

const byDay = computed<any[]>(() => data.value.byDay || [])
const byStatus = computed<any[]>(() => data.value.byStatus || [])

const statusLabels: Record<string, string> = {
  PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回',
  CANCELLED: '已取消', EXPIRED: '已过期'
}

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 24, top: 30, bottom: 50 },
  xAxis: {
    type: 'category',
    data: byDay.value.map((d: any) => String(d.day || '').slice(5, 10)),
    axisLabel: { rotate: 30 }
  },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } } },
  series: [{
    type: 'line',
    smooth: true,
    symbolSize: 7,
    areaStyle: { color: 'rgba(59,130,246,0.14)' },
    lineStyle: { color: '#3b82f6', width: 3 },
    itemStyle: { color: '#3b82f6' },
    data: byDay.value.map((d: any) => d.total)
  }]
}))

const statusOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, icon: 'circle' },
  color: ['#f59e0b', '#10b981', '#ef4444', '#94a3b8', '#8b5cf6'],
  series: [{
    type: 'pie',
    radius: ['44%', '70%'],
    itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: byStatus.value.map((d: any) => ({
      name: statusLabels[d.status] || d.status,
      value: d.total
    }))
  }]
}))

async function load() {
  loading.value = true
  try {
    const params: any = {}
    if (range.value) {
      params.start = range.value[0]
      params.end = range.value[1]
    }
    data.value = (await http.get('/visitor/stats', { params })) as any
  } finally {
    loading.value = false
  }
}

async function exportData() {
  await download('/visitor/export', undefined, '访客台账.xlsx')
  ElMessage.success('已导出')
}

onMounted(load)
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 12px; }
</style>
