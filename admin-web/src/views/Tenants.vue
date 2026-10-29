<template>
  <div v-loading="loading" class="page-card">
    <div class="toolbar">
      <div>
        <h2 class="page-title">租户管理</h2>
        <div class="page-subtitle">学校开通 · 配额 · 状态 · 续期，开通时自动初始化角色与管理员</div>
      </div>
      <div class="spacer" />
      <el-input v-model="keyword" placeholder="学校名称/编码" clearable style="width: 220px" @keyup.enter="reload" />
      <el-select v-model="status" clearable placeholder="状态" style="width: 130px" @change="reload">
        <el-option label="正常" value="NORMAL" />
        <el-option label="停用" value="DISABLED" />
        <el-option label="到期" value="EXPIRED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
      <el-button type="success" :icon="Plus" :disabled="!auth.isPlatform" @click="openCreate">开通学校</el-button>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="name" label="学校名称" min-width="170" />
      <el-table-column prop="code" label="学校编码" width="110" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userQuota" label="用户配额" width="100" />
      <el-table-column prop="contact" label="联系人" width="110" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="expireTime" label="到期时间" width="170" />
      <el-table-column prop="createTime" label="开通时间" width="170" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!auth.isPlatform" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" :disabled="!auth.isPlatform" @click="openRenew(row)">续期</el-button>
          <el-button
            link
            :type="row.status === 'NORMAL' ? 'warning' : 'success'"
            :disabled="!auth.isPlatform"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'NORMAL' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="drill(row)">下钻</el-button>
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

    <el-dialog v-model="visible" :title="form.id ? '编辑学校' : '开通新学校'" width="640px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学校名称" required><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学校编码" required>
              <el-input v-model="form.code" placeholder="如 DEMO，建议大写字母" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学校类型">
              <el-select v-model="form.type" class="full">
                <el-option label="高校" value="UNIVERSITY" />
                <el-option label="中学" value="MIDDLE" />
                <el-option label="小学" value="PRIMARY" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户配额">
              <el-input-number v-model="form.userQuota" :min="1" class="full" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期时间">
              <el-date-picker v-model="form.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" class="full">
                <el-option label="正常" value="NORMAL" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert
          v-if="!form.id"
          type="info"
          :closable="false"
          show-icon
          title="默认管理员"
          description="开通后将自动创建学校管理员账号 admin（默认密码 Admin@123）及内置角色。"
        />
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renewVisible" title="学校续期" width="420px">
      <el-form label-position="top">
        <el-form-item label="新的到期时间">
          <el-date-picker v-model="renewForm.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="full" />
        </el-form-item>
        <el-form-item label="用户配额">
          <el-input-number v-model="renewForm.userQuota" :min="1" class="full" :controls="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="renew">确认续期</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import http from '@/api/http'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const renewVisible = ref(false)
const keyword = ref('')
const status = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const rows = ref<any[]>([])
const form = reactive<any>({
  id: null, name: '', code: '', type: 'UNIVERSITY', status: 'NORMAL',
  userQuota: 100, contact: '', phone: '', expireTime: '', remark: ''
})
const renewForm = reactive<any>({ id: null, expireTime: '', userQuota: 100 })

function statusLabel(value: string) {
  return ({ NORMAL: '正常', DISABLED: '停用', EXPIRED: '到期' } as Record<string, string>)[value] || value
}

function statusType(value: string) {
  if (value === 'NORMAL') return 'success'
  if (value === 'EXPIRED') return 'warning'
  return 'danger'
}

async function load() {
  loading.value = true
  try {
    const data: any = await http.get('/platform/tenants', {
      params: { page: page.value, size: size.value, keyword: keyword.value || undefined, status: status.value || undefined }
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

function openCreate() {
  Object.assign(form, {
    id: null, name: '', code: '', type: 'UNIVERSITY', status: 'NORMAL',
    userQuota: 100, contact: '', phone: '', expireTime: '', remark: ''
  })
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
      await http.put(`/platform/tenants/${form.id}`, form)
    } else {
      await http.post('/platform/tenants', form)
    }
    ElMessage.success('学校信息已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openRenew(row: any) {
  renewForm.id = row.id
  renewForm.expireTime = row.expireTime
  renewForm.userQuota = row.userQuota || 100
  renewVisible.value = true
}

async function renew() {
  saving.value = true
  try {
    await http.post(`/platform/tenants/${renewForm.id}/renew`, {
      expireTime: renewForm.expireTime,
      userQuota: renewForm.userQuota
    })
    ElMessage.success('续期成功')
    renewVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: any) {
  const next = row.status === 'NORMAL' ? 'DISABLED' : 'NORMAL'
  await ElMessageBox.confirm(`确认${next === 'DISABLED' ? '停用' : '启用'}「${row.name}」？`, '提示', { type: 'warning' })
  await http.post(`/platform/tenants/${row.id}/status`, { status: next })
  ElMessage.success('状态已更新')
  load()
}

function drill(row: any) {
  auth.setTenantHeader(row.id)
  ElMessage.success(`已切换到 ${row.name}（只读）`)
  router.push('/dashboard')
  setTimeout(() => window.location.reload(), 120)
}

onMounted(load)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
