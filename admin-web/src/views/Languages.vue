<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">多语言配置</h2>
        <div class="page-subtitle">中英文切换 · 语言启用状态与排序（仅平台管理员可维护）</div>
      </div>
      <div class="spacer" />
      <el-button type="success" :icon="Plus" :disabled="!auth.isPlatform" @click="openCreate">新增语言</el-button>
    </div>

    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="当前已支持：简体中文（zh-CN）、English（en-US）"
      description="语言配置用于管理端、H5 门户、小程序与 APP 的界面语言开关；业务字典类内容请到数据字典维护。"
      class="mb-12"
    />

    <el-table :data="rows" stripe border>
      <el-table-column prop="langCode" label="语言编码" width="150" />
      <el-table-column prop="langName" label="语言名称" min-width="180" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
            {{ row.enabled === 1 ? '已启用' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="175" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!auth.isPlatform" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="!auth.isPlatform" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑语言' : '新增语言'" width="440px">
      <el-form :model="form" label-position="top">
        <el-form-item label="语言编码">
          <el-input v-model="form.langCode" placeholder="zh-CN / en-US" />
        </el-form-item>
        <el-form-item label="语言名称">
          <el-input v-model="form.langName" placeholder="简体中文 / English" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" class="full" :controls="false" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '@/api/http'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const rows = ref<any[]>([])
const form = reactive<any>({ id: null, langCode: '', langName: '', enabled: 1, sort: 99 })

async function load() {
  loading.value = true
  try {
    rows.value = (await http.get('/system/languages')) as any
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, langCode: '', langName: '', enabled: 1, sort: 99 })
  visible.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) {
      await http.put(`/system/languages/${form.id}`, form)
    } else {
      await http.post('/system/languages', form)
    }
    ElMessage.success('已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除语言「${row.langName}」？`, '提示', { type: 'warning' })
  await http.delete(`/system/languages/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
