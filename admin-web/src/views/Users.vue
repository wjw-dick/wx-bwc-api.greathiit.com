<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">用户管理</h2>
        <div class="page-subtitle">学校账号 · 角色分配 · 手机号/证件加密 · 批量导入导出</div>
      </div>
      <div class="spacer" />
      <el-input v-model="keyword" placeholder="用户名/姓名/手机号" clearable style="width: 200px" @keyup.enter="reload" />
      <el-select v-model="roleCode" clearable placeholder="角色" style="width: 140px" @change="reload">
        <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.roleCode" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
      <el-button :icon="Download" @click="exportData">导出</el-button>
      <el-upload :show-file-list="false" :http-request="importData" accept=".xlsx,.xls">
        <el-button :icon="Upload">导入</el-button>
      </el-upload>
      <el-button type="success" :icon="Plus" @click="openCreate">新增用户</el-button>
    </div>

    <el-alert
      v-if="auth.isPlatform && !auth.tenantHeader"
      type="warning"
      show-icon
      :closable="false"
      title="平台账号查看全部学校用户；新增用户前请先在顶部选择学校"
      class="mb-12"
    />

    <el-table :data="rows" stripe border>
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="roleCode" label="角色" width="130" />
      <el-table-column prop="userType" label="类型" width="100" />
      <el-table-column prop="dept" label="部门" min-width="130" />
      <el-table-column prop="phoneMask" label="手机号" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">
            {{ row.status === 'NORMAL' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑用户' : '新增用户'" width="620px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" required>
              <el-input v-model="form.username" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleCode" class="full" filterable>
                <el-option v-for="role in roles" :key="role.id" :label="`${role.roleName}（${role.roleCode}）`" :value="role.roleCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户类型">
              <el-select v-model="form.userType" class="full">
                <el-option label="教职工" value="STAFF" />
                <el-option label="学生" value="STUDENT" />
                <el-option label="保安" value="GUARD" />
                <el-option label="访客" value="VISITOR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门"><el-input v-model="form.dept" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="加密存储" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号"><el-input v-model="form.idCard" placeholder="加密存储" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" class="full">
                <el-option label="正常" value="NORMAL" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="!form.id" :span="12">
            <el-form-item label="初始密码">
              <el-input v-model="form.password" type="password" show-password placeholder="默认 Admin@123" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { Download, Plus, Search, Upload } from '@element-plus/icons-vue'
import http, { download } from '@/api/http'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const keyword = ref('')
const roleCode = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const rows = ref<any[]>([])
const roles = ref<any[]>([])
const form = reactive<any>({
  id: null, username: '', realName: '', roleCode: 'STAFF', userType: 'STAFF',
  dept: '', phone: '', idCard: '', status: 'NORMAL', password: ''
})

async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/users', {
      params: {
        page: page.value, size: size.value,
        keyword: keyword.value || undefined, roleCode: roleCode.value || undefined
      }
    })
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

async function loadRoles() {
  roles.value = (await http.get('/users/roles')) as any
}

function openCreate() {
  Object.assign(form, {
    id: null, username: '', realName: '', roleCode: 'STAFF', userType: 'STAFF',
    dept: '', phone: '', idCard: '', status: 'NORMAL', password: ''
  })
  visible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { ...row, phone: '', idCard: '', password: '' })
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) {
      await http.put(`/users/${form.id}`, form)
    } else {
      await http.post('/users', form)
    }
    ElMessage.success('用户已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function resetPassword(row: any) {
  const { value } = await ElMessageBox.prompt(`为 ${row.username} 设置新密码`, '重置密码', {
    inputValue: 'Admin@123'
  })
  await http.post(`/users/${row.id}/reset-password`, { password: value })
  ElMessage.success('密码已重置')
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await http.delete(`/users/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function exportData() {
  await download('/users/export', undefined, '用户列表.xlsx')
  ElMessage.success('已导出')
}

async function importData(options: any) {
  const data = new FormData()
  data.append('file', options.file)
  try {
    const result: any = await http.post('/users/import', data, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(`导入完成：成功 ${result.success}，失败 ${result.failed}；默认密码 ${result.defaultPassword}`)
    if (result.failures?.length) {
      ElMessageBox.alert(result.failures.join('\n'), '导入失败明细')
    }
    load()
  } catch (e) {
    ElMessage.error('导入失败')
  }
}

onMounted(async () => {
  await loadRoles()
  await load()
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
