<template>
  <div v-loading="loading">
    <div class="page-card toolbar">
      <div>
        <h2 class="page-title">月度报表</h2>
        <div class="page-subtitle">按月汇总治安、消防、访客、巡逻等数据，支持生成与 PDF 打印</div>
      </div>
      <div class="spacer" />
      <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="选择月份" style="width: 160px" />
      <el-button type="primary" :icon="MagicStick" :loading="generating" @click="generate">生成月报</el-button>
    </div>

    <div class="stat-grid">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </div>
    </div>

    <div class="page-card mt-16">
      <h3 class="section-title">已生成月报</h3>
      <el-table :data="reports" stripe border>
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column label="数据摘要" min-width="360">
          <template #default="{ row }">
            <div class="summary">
              <el-tag v-for="item in summarize(row)" :key="item.key" size="small" effect="plain">
                {{ item.label }}：{{ item.value }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="preview(row)">查看</el-button>
            <el-button link type="primary" @click="downloadPdf(row.month)">PDF</el-button>
            <el-button link type="primary" @click="regenerate(row.month)">重新生成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="detailVisible" :title="`月报详情 · ${detail.month || ''}`" size="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item v-for="item in detailRows" :key="item.key" :label="item.label">
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'

const loading = ref(false)
const generating = ref(false)
const month = ref(new Date().toISOString().slice(0, 7))
const reports = ref<any[]>([])
const stats = ref<Record<string, any>>({})
const detailVisible = ref(false)
const detail = ref<Record<string, any>>({})

const cards = computed(() => [
  { label: '今日排班', value: stats.value.scheduleToday ?? 0, hint: '保安值班安排' },
  { label: '今日考勤', value: stats.value.attendanceToday ?? 0, hint: '打卡记录' },
  { label: '本月绩效', value: stats.value.performanceMonth ?? 0, hint: '已录入绩效' },
  { label: '待处理反馈', value: stats.value.feedbackPending ?? 0, hint: `累计 ${stats.value.feedbackTotal ?? 0}` },
  { label: '已生成月报', value: reports.value.length, hint: '按月归档' }
])

const labels: Record<string, string> = {
  visitorTotal: '访客预约', visitorToday: '今日访客', visitorPending: '待审批',
  vehicleTotal: '备案车辆', vehicleLogToday: '今日车行', plateLogToday: '车牌识别',
  inPark: '在场车辆', eventTotal: '治安事件', eventOpen: '未结案',
  hazardTotal: '隐患总数', hazardOpen: '未闭环', hazardRate: '整改率(%)',
  fireFacilityTotal: '消防设施', fireIntactRate: '完好率(%)', patrolTaskTotal: '巡逻任务',
  patrolRate: '巡逻完成率(%)', lostTotal: '失物', lostOpen: '待认领', lostClaimRate: '认领率(%)'
}

function parseContent(row: any): Record<string, any> {
  try {
    return JSON.parse(row.contentJson || '{}')
  } catch (e) {
    return {}
  }
}

function summarize(row: any) {
  const content = parseContent(row)
  const overview = content.overview || {}
  return ['visitorTotal', 'vehicleTotal', 'eventTotal', 'hazardTotal', 'patrolRate', 'lostClaimRate']
    .filter((key) => overview[key] !== undefined)
    .map((key) => ({ key, label: labels[key] || key, value: overview[key] }))
}

const detailRows = computed(() => {
  const content = parseContent(detail.value)
  const overview = content.overview || {}
  return Object.entries(overview)
    .filter(([key, value]) => !['generatedAt', 'tenantId'].includes(key) && (typeof value === 'number' || typeof value === 'string'))
    .map(([key, value]) => ({ key, label: labels[key] || key, value }))
})

async function load() {
  loading.value = true
  try {
    const [list, stat]: any = await Promise.all([
      http.get('/logistics/monthly'),
      http.get('/logistics/stats')
    ])
    reports.value = list || []
    stats.value = stat || {}
  } finally {
    loading.value = false
  }
}

async function generate() {
  generating.value = true
  try {
    await http.post('/logistics/monthly/generate', { month: month.value })
    ElMessage.success(`${month.value} 月报已生成`)
    await load()
  } finally {
    generating.value = false
  }
}

async function regenerate(value: string) {
  month.value = value
  await generate()
}

function preview(row: any) {
  detail.value = row
  detailVisible.value = true
}

async function downloadPdf(value: string) {
  await download(`/logistics/monthly/${value}/pdf`, undefined, `校园保卫月报-${value}.pdf`)
  ElMessage.success('PDF 已下载')
}

onMounted(load)
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 12px; }
.summary { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
