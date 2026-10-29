<template>
  <div v-loading="loading">
    <div class="page-card hero">
      <div>
        <h2 class="page-title">{{ greeting }}，{{ auth.displayName }}</h2>
        <p class="page-subtitle">
          {{ auth.schoolName }} · 数据实时统计 · 更新时间 {{ data.generatedAt || '-' }}
        </p>
      </div>
      <div class="flex">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Download" @click="exportReport">导出总览</el-button>
      </div>
    </div>

    <div class="stat-grid mt-16">
      <div v-for="card in primaryCards" :key="card.label" class="stat-card" :class="card.tone">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="15">
        <div class="page-card">
          <div class="toolbar">
            <h3 class="section-title">安全运行概览</h3>
            <div class="spacer" />
            <el-tag type="success" effect="plain">租户隔离生效</el-tag>
          </div>
          <ChartBox :option="barOption" height="320px" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="9">
        <div class="page-card">
          <h3 class="section-title">闭环率</h3>
          <ChartBox :option="rateOption" height="320px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="8">
        <div class="page-card">
          <h3 class="section-title">今日待办</h3>
          <div class="todo" v-for="todo in todos" :key="todo.label" @click="go(todo.path)">
            <span>{{ todo.label }}</span>
            <div class="flex">
              <el-tag :type="todo.value > 0 ? 'warning' : 'success'" effect="plain">{{ todo.value }}</el-tag>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="16">
        <div class="page-card">
          <h3 class="section-title">快捷入口</h3>
          <div class="quick-grid">
            <div v-for="entry in entries" :key="entry.path" class="quick" @click="go(entry.path)">
              <el-icon :size="22"><component :is="entry.icon" /></el-icon>
              <span>{{ entry.label }}</span>
            </div>
          </div>
          <el-alert
            class="mt-16"
            type="info"
            :closable="false"
            show-icon
            title="多租户隔离说明"
            description="当前账号仅能访问所属学校的业务数据；平台账号可在顶部切换学校进行只读下钻。"
          />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Download, Refresh } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'
import { useAuthStore } from '@/store/auth'
import ChartBox from '@/components/ChartBox.vue'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const data = ref<Record<string, any>>({})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const primaryCards = computed(() => [
  { label: '今日访客', value: data.value.visitorToday ?? 0, hint: `累计预约 ${data.value.visitorTotal ?? 0}`, tone: 'blue' },
  { label: '待审预约', value: data.value.visitorPending ?? 0, hint: '门禁访客待处理', tone: 'orange' },
  { label: '今日车行', value: data.value.vehicleLogToday ?? 0, hint: `识别记录 ${data.value.plateLogToday ?? 0}`, tone: 'purple' },
  { label: '在园车辆', value: data.value.inPark ?? 0, hint: '当前场内车辆', tone: 'green' },
  { label: '未闭环事件', value: data.value.eventOpen ?? 0, hint: `事件总数 ${data.value.eventTotal ?? 0}`, tone: 'red' },
  { label: '未闭环隐患', value: data.value.hazardOpen ?? 0, hint: `整改率 ${data.value.hazardRate ?? 0}%`, tone: 'orange' },
  { label: '今日巡逻打卡', value: data.value.patrolRecordToday ?? 0, hint: `任务完成率 ${data.value.patrolRate ?? 0}%`, tone: 'blue' },
  { label: '待处理报备', value: data.value.reportPending ?? 0, hint: `设备故障 ${data.value.deviceRepairOpen ?? 0}`, tone: 'purple' }
])

const barOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['访客', '车辆', '事件', '隐患', '巡逻', '失物', '报备', '设备'],
    axisLine: { lineStyle: { color: '#cbd5e1' } }
  },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } } },
  series: [{
    type: 'bar',
    barWidth: 22,
    itemStyle: {
      borderRadius: [6, 6, 0, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#93c5fd' }] }
    },
    data: [
      data.value.visitorTotal ?? 0,
      data.value.vehicleTotal ?? 0,
      data.value.eventTotal ?? 0,
      data.value.hazardTotal ?? 0,
      data.value.patrolTaskTotal ?? 0,
      data.value.lostTotal ?? 0,
      data.value.reportPending ?? 0,
      data.value.deviceTotal ?? 0
    ]
  }]
}))

const rateOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, icon: 'circle' },
  series: [{
    type: 'pie',
    radius: ['46%', '72%'],
    itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: [
      { name: '隐患整改率', value: data.value.hazardRate ?? 0, itemStyle: { color: '#f59e0b' } },
      { name: '消防完好率', value: data.value.fireIntactRate ?? 0, itemStyle: { color: '#ef4444' } },
      { name: '巡逻完成率', value: data.value.patrolRate ?? 0, itemStyle: { color: '#3b82f6' } },
      { name: '失物认领率', value: data.value.lostClaimRate ?? 0, itemStyle: { color: '#10b981' } }
    ]
  }]
}))

const todos = computed(() => [
  { label: '访客待审批', value: data.value.visitorPending ?? 0, path: '/visitor/appointments' },
  { label: '隐患待整改', value: data.value.hazardOpen ?? 0, path: '/fire/hazards' },
  { label: '报备待审批', value: data.value.reportPending ?? 0, path: '/report/applies' },
  { label: '设备待维修', value: data.value.deviceRepairOpen ?? 0, path: '/device/repairs' },
  { label: '后勤待处理反馈', value: data.value.feedbackPending ?? 0, path: '/logistics/feedback' },
  { label: '应急未关闭事件', value: data.value.emergencyOpen ?? 0, path: '/emergency/events' }
])

const entries = [
  { label: '访客预约', path: '/visitor/appointments', icon: 'User' },
  { label: '车辆管理', path: '/vehicle/vehicles', icon: 'Van' },
  { label: '消防隐患', path: '/fire/hazards', icon: 'AlarmClock' },
  { label: '巡逻统计', path: '/patrol/stats', icon: 'Position' },
  { label: '失物招领', path: '/lost/items', icon: 'Search' },
  { label: '报表中心', path: '/report-center/tenant', icon: 'DataAnalysis' },
  { label: '用户管理', path: '/system/users', icon: 'UserFilled' },
  { label: '审计日志', path: '/system/audit', icon: 'Tickets' }
]

async function load() {
  loading.value = true
  try {
    data.value = (await http.get('/dashboard')) as any
  } finally {
    loading.value = false
  }
}

async function exportReport() {
  await download('/reports/export', { module: 'overview' }, '校园保卫总览.xlsx')
  ElMessage.success('已导出')
}

function go(path: string) {
  router.push(path)
}

onMounted(load)
</script>

<style scoped>
.hero { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
.stat-card.blue { border-left: 3px solid #3b82f6; }
.stat-card.orange { border-left: 3px solid #f59e0b; }
.stat-card.purple { border-left: 3px solid #8b5cf6; }
.stat-card.green { border-left: 3px solid #10b981; }
.stat-card.red { border-left: 3px solid #ef4444; }
.todo {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 4px; border-bottom: 1px solid #f1f5f9; cursor: pointer;
}
.todo:last-child { border-bottom: none; }
.todo:hover { color: var(--bwc-primary); }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.quick {
  border: 1px solid #eef2f7; border-radius: 10px; padding: 18px 10px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 8px; cursor: pointer;
  color: #334155; background: #fbfdff;
}
.quick:hover { border-color: #93c5fd; color: var(--bwc-primary); background: #eff6ff; }
@media (max-width: 900px) {
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
