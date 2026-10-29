<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">审计日志</h2>
        <div class="page-subtitle">登录、增删改、审批、导出等关键操作全程留痕</div>
      </div>
      <div class="spacer" />
      <el-input v-model="keyword" placeholder="操作人/对象/详情" clearable style="width: 190px" @keyup.enter="reload" />
      <el-input v-model="module" placeholder="模块" clearable style="width: 120px" @keyup.enter="reload" />
      <el-input v-model="action" placeholder="动作" clearable style="width: 120px" @keyup.enter="reload" />
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始"
        end-placeholder="结束"
        style="width: 250px"
      />
      <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
      <el-button :icon="Download" @click="exportData">导出</el-button>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="createTime" label="时间" width="175" />
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="module" label="模块" width="100" />
      <el-table-column prop="action" label="动作" width="130" />
      <el-table-column prop="target" label="对象" min-width="140" show-overflow-tooltip />
      <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="userAgent" label="User-Agent" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'

const loading = ref(false)
const keyword = ref('')
const module = ref('')
const action = ref('')
const range = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      module: module.value || undefined,
      action: action.value || undefined
    }
    if (range.value) {
      params.start = `${range.value[0]} 00:00:00`
      params.end = `${range.value[1]} 23:59:59`
    }
    const data: any = await http.get('/system/audit', { params })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

async function exportData() {
  await download('/system/audit/export', undefined, '审计日志.xlsx')
  ElMessage.success('已导出')
}

onMounted(load)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
