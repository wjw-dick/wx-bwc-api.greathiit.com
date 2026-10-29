<template>
  <div v-loading="loading">
    <div class="page-card toolbar">
      <div>
        <h2 class="page-title">巡逻统计</h2>
        <div class="page-subtitle">扫码/GPS 打卡 · 点位覆盖 · 异常上报</div>
      </div>
      <div class="spacer" />
      <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 170px" />
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
    </div>

    <div class="stat-grid mt-16">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="10">
        <div class="page-card">
          <h3 class="section-title">打卡状态</h3>
          <ChartBox :option="pieOption" height="300px" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="14">
        <div class="page-card">
          <h3 class="section-title">点位覆盖率</h3>
          <div class="coverage">
            <el-progress type="dashboard" :percentage="Number(data.completeRate || 0)" :width="150" color="#10b981" />
            <div>
              <p class="muted">已覆盖点位：{{ data.pointCovered ?? 0 }} / {{ data.pointTotal ?? 0 }}</p>
              <p class="muted">准时率：{{ data.onTimeRate ?? 0 }}%</p>
              <p class="muted">统计日期：{{ data.date || date || '-' }}</p>
            </div>
          </div>
          <el-divider />
          <el-table :data="records" size="small" stripe>
            <el-table-column prop="guardName" label="保安" width="110" />
            <el-table-column prop="pointName" label="点位" min-width="130" />
            <el-table-column prop="checkTime" label="打卡时间" width="165" />
            <el-table-column prop="method" label="方式" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'NORMAL' ? 'success' : row.status === 'LATE' ? 'warning' : 'danger'" size="small">
                  {{ row.status === 'NORMAL' ? '正常' : row.status === 'LATE' ? '迟到' : '异常' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import http from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'

const loading = ref(false)
const date = ref<string>(new Date().toISOString().slice(0, 10))
const data = ref<Record<string, any>>({})
const records = ref<any[]>([])

const cards = computed(() => [
  { label: '打卡总数', value: data.value.total ?? 0, hint: '当日全部打卡' },
  { label: '正常', value: data.value.normal ?? 0, hint: `准时率 ${data.value.onTimeRate ?? 0}%` },
  { label: '迟到', value: data.value.late ?? 0, hint: '超时打卡' },
  { label: '异常', value: data.value.abnormal ?? 0, hint: '含异常上报' },
  { label: '点位覆盖', value: `${data.value.pointCovered ?? 0}/${data.value.pointTotal ?? 0}`, hint: `覆盖率 ${data.value.completeRate ?? 0}%` }
])

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, icon: 'circle' },
  color: ['#10b981', '#f59e0b', '#ef4444'],
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: [
      { name: '正常', value: data.value.normal ?? 0 },
      { name: '迟到', value: data.value.late ?? 0 },
      { name: '异常', value: data.value.abnormal ?? 0 }
    ]
  }]
}))

async function load() {
  loading.value = true
  try {
    data.value = (await http.get('/patrol/stats', { params: { date: date.value } })) as any
    const page: any = await http.get('/patrol/records', { params: { page: 1, size: 10, date: date.value } })
    records.value = page.records || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 12px; }
.coverage { display: flex; align-items: center; gap: 28px; flex-wrap: wrap; }
</style>
