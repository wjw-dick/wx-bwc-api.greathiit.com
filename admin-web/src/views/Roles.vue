<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">角色权限</h2>
        <div class="page-subtitle">两级 RBAC：系统角色固定，租户角色可自定义并分配菜单权限</div>
      </div>
      <div class="spacer" />
      <el-button type="success" :icon="Plus" @click="openCreate">新增角色</el-button>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="roleCode" label="角色编码" width="160" />
      <el-table-column prop="roleName" label="角色名称" min-width="160" />
      <el-table-column prop="dataScope" label="数据范围" width="120" />
      <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openMenus(row)">菜单授权</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="角色编码">
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="如 FIRE_MANAGER" />
        </el-form-item>
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" class="full">
            <el-option label="本校数据" value="TENANT" />
            <el-option label="本人数据" value="SELF" />
            <el-option label="全部数据" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="menuVisible" :title="`菜单授权 · ${currentRole.roleName || ''}`" size="440px">
      <el-alert type="info" show-icon :closable="false" title="勾选菜单后保存，用户重新登录或刷新后生效" class="mb-12" />
      <el-tree
        ref="treeRef"
        :data="menuTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="checkedMenus"
      />
      <template #footer>
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMenus">保存授权</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import http from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const menuVisible = ref(false)
const rows = ref<any[]>([])
const menuTree = ref<any[]>([])
const checkedMenus = ref<number[]>([])
const currentRole = ref<any>({})
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
const form = reactive<any>({ id: null, roleCode: '', roleName: '', dataScope: 'TENANT', remark: '' })

async function load() {
  loading.value = true
  try {
    rows.value = (await http.get('/users/roles')) as any
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, roleCode: '', roleName: '', dataScope: 'TENANT', remark: '' })
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
      await http.put(`/users/roles/${form.id}`, form)
    } else {
      await http.post('/users/roles', form)
    }
    ElMessage.success('角色已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await http.delete(`/users/roles/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function openMenus(row: any) {
  currentRole.value = row
  checkedMenus.value = (await http.get(`/users/roles/${row.id}/menus`)) as any
  if (!menuTree.value.length) {
    menuTree.value = (await http.get('/users/roles/menu-options')) as any
  }
  menuVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys(checkedMenus.value)
}

async function saveMenus() {
  const checked = treeRef.value?.getCheckedKeys(false) as number[] || []
  const half = treeRef.value?.getHalfCheckedKeys() as number[] || []
  saving.value = true
  try {
    await http.put(`/users/roles/${currentRole.value.id}/menus`, [...checked, ...half])
    ElMessage.success('菜单授权已保存')
    menuVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
