<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">菜单可视化配置</h2>
        <div class="page-subtitle">树形结构、显示隐藏、图标与路径配置，平台端统一维护</div>
      </div>
      <div class="spacer" />
      <el-radio-group v-model="platform" @change="load">
        <el-radio-button value="admin">管理端</el-radio-button>
        <el-radio-button value="h5">H5 门户</el-radio-button>
        <el-radio-button value="mp">小程序</el-radio-button>
      </el-radio-group>
      <el-button type="success" :icon="Plus" @click="openCreate(null)">新增根菜单</el-button>
    </div>

    <el-alert
      v-if="!auth.isPlatform"
      type="warning"
      show-icon
      :closable="false"
      title="菜单维护仅平台管理员可操作"
      class="mb-12"
    />

    <el-table :data="tree" row-key="id" :tree-props="{ children: 'children' }" border default-expand-all>
      <el-table-column prop="name" label="菜单名称" min-width="180">
        <template #default="{ row }">
          <span class="flex">
            <el-icon><component :is="row.icon || 'Document'" /></el-icon>
            {{ row.name }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="nameEn" label="英文名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="path" label="路径" min-width="170" show-overflow-tooltip />
      <el-table-column prop="permission" label="权限标识" min-width="150" show-overflow-tooltip />
      <el-table-column prop="menuType" label="类型" width="90" />
      <el-table-column prop="sort" label="排序" width="75" />
      <el-table-column label="显示" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.visible === 1"
            :disabled="!auth.isPlatform"
            @change="(value: boolean) => toggleVisible(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCreate(row)">子菜单</el-button>
          <el-button link type="primary" :disabled="!auth.isPlatform" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="!auth.isPlatform" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="visible" :title="form.id ? '编辑菜单' : '新增菜单'" size="480px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜单名称"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="英文名称"><el-input v-model="form.nameEn" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="parentOptions"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                check-strictly
                clearable
                class="full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单类型">
              <el-select v-model="form.menuType" class="full">
                <el-option label="目录" value="DIR" />
                <el-option label="菜单" value="MENU" />
                <el-option label="按钮" value="BUTTON" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="路由路径"><el-input v-model="form.path" placeholder="/visitor/appointments" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标"><el-input v-model="form.icon" placeholder="Element Plus 图标名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" class="full" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="权限标识"><el-input v-model="form.permission" placeholder="visitor:read" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否显示">
              <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可视化配置 JSON">
              <el-input v-model="form.configJson" type="textarea" :rows="5" placeholder='{"width":236,"theme":"dark"}' />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '@/api/http'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const platform = ref('admin')
const rows = ref<any[]>([])
const form = reactive<any>({
  id: null, platform: 'admin', parentId: 0, name: '', nameEn: '', path: '', icon: 'Document',
  sort: 99, menuType: 'MENU', permission: '', visible: 1, configJson: ''
})

const tree = computed(() => buildTree(rows.value))
const parentOptions = computed(() => [{ id: 0, name: '根目录', children: buildTree(rows.value) }])

function buildTree(list: any[], parentId = 0): any[] {
  return list
    .filter((item) => Number(item.parentId || 0) === Number(parentId))
    .sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0))
    .map((item) => ({ ...item, children: buildTree(list, item.id) }))
}

async function load() {
  loading.value = true
  try {
    rows.value = (await http.get('/system/menus', { params: { platform: platform.value } })) as any
  } finally {
    loading.value = false
  }
}

function reset(parent: any) {
  Object.assign(form, {
    id: null, platform: platform.value, parentId: parent ? parent.id : 0, name: '', nameEn: '', path: '',
    icon: 'Document', sort: 99, menuType: 'MENU', permission: '', visible: 1, configJson: ''
  })
}

function openCreate(parent: any) {
  reset(parent)
  visible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { ...row, children: undefined })
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = { ...form }
    delete payload.children
    if (payload.id) {
      await http.put(`/system/menus/${payload.id}`, payload)
    } else {
      await http.post('/system/menus', payload)
    }
    ElMessage.success('菜单已保存，刷新页面后生效')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleVisible(row: any, value: boolean) {
  await http.post(`/system/menus/${row.id}/visible`, { visible: value ? 1 : 0 })
  row.visible = value ? 1 : 0
  ElMessage.success(value ? '已显示' : '已隐藏')
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除菜单「${row.name}」及其子菜单？`, '提示', { type: 'warning' })
  await http.delete(`/system/menus/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
