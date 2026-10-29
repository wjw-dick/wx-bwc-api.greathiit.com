<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">平台参数</h2>
        <div class="page-subtitle">仅平台管理员可修改，保存后即时生效</div>
      </div>
      <div class="spacer" />
      <el-input v-model="keyword" placeholder="搜索参数" clearable style="width: 220px" />
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-table :data="filtered" stripe border>
      <el-table-column prop="paramKey" label="参数键" min-width="220" />
      <el-table-column prop="paramName" label="参数名称" min-width="160" />
      <el-table-column prop="paramValue" label="参数值" min-width="220" show-overflow-tooltip />
      <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="修改平台参数" width="480px">
      <el-form label-position="top">
        <el-form-item label="参数键"><el-input v-model="form.paramKey" disabled /></el-form-item>
        <el-form-item label="参数名称"><el-input v-model="form.paramName" disabled /></el-form-item>
        <el-form-item label="参数值">
          <el-input v-model="form.paramValue" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" type="textarea" :rows="2" disabled /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import http from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const keyword = ref('')
const rows = ref<any[]>([])
const form = reactive<any>({ paramKey: '', paramName: '', paramValue: '', remark: '' })

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter((row) =>
    String(row.paramKey || '').toLowerCase().includes(kw) || String(row.paramName || '').toLowerCase().includes(kw))
})

async function load() {
  loading.value = true
  try {
    rows.value = (await http.get('/system/params')) as any
  } finally {
    loading.value = false
  }
}

function openEdit(row: any) {
  Object.assign(form, row)
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    await http.put(`/system/params/${form.paramKey}`, { paramValue: form.paramValue })
    ElMessage.success('参数已保存并生效')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
