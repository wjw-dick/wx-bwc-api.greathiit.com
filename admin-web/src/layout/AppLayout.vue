<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-brand">
        <div class="logo">校</div>
        <div v-if="!collapsed" class="brand-text">
          <strong>校园保卫</strong>
          <span>Campus Security</span>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="#0f1c33"
          text-color="#cbd5e1"
          active-text-color="#ffffff"
          router
          unique-opened
        >
          <MenuItem :menus="visibleMenus" />
        </el-menu>
      </el-scrollbar>
      <div v-if="!collapsed" class="sidebar-foot">
        <span>合法域名</span>
        <strong>wx-bwc-api.greathiit.com</strong>
      </div>
    </aside>

    <section class="main">
      <header class="header">
        <div class="left">
          <el-button text circle @click="collapsed = !collapsed">
            <el-icon :size="20"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ auth.schoolName }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
          <el-tag v-if="auth.isPlatform && auth.tenantHeader" type="warning" effect="plain" size="small">
            下钻租户只读
          </el-tag>
        </div>
        <div class="right">
          <el-select
            v-if="auth.isPlatform"
            v-model="tenantHeader"
            class="tenant-switch"
            placeholder="全部学校"
            clearable
            filterable
            @change="switchTenant"
          >
            <el-option v-for="item in auth.tenantOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="localeModel" class="lang-switch" @change="changeLocale">
            <el-option label="简体中文" value="zh-CN" />
            <el-option label="English" value="en-US" />
          </el-select>
          <el-badge :value="unread" :hidden="unread === 0" :max="99">
            <el-button text circle @click="showMessages = true">
              <el-icon :size="19"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="onUserCommand">
            <span class="user-chip">
              <el-avatar :size="30">{{ auth.displayName.slice(0, 1) }}</el-avatar>
              <span class="user-name">{{ auth.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">账号信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </section>

    <el-drawer v-model="showMessages" title="消息中心" size="420px">
      <div v-loading="loadingMessages">
        <el-empty v-if="!messages.length" description="暂无消息" />
        <div v-for="msg in messages" :key="msg.id" class="message-item" @click="readMessage(msg)">
          <div class="flex">
            <el-tag size="small" :type="msg.readFlag ? 'info' : 'danger'">{{ msg.readFlag ? '已读' : '未读' }}</el-tag>
            <strong>{{ msg.title }}</strong>
          </div>
          <p>{{ msg.content }}</p>
          <span class="muted">{{ msg.createTime }}</span>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="showPassword" title="修改密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="旧密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="passwordForm.confirm" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPassword = false">取消</el-button>
        <el-button type="primary" @click="changePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Fold, Expand, Bell, ArrowDown } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore, type MenuNode } from '@/store/auth'
import MenuItem from '@/components/MenuItem.vue'
import http from '@/api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { locale } = useI18n()
const collapsed = ref(false)
const tenantHeader = ref<number | null>(auth.tenantHeader)
const showMessages = ref(false)
const showPassword = ref(false)
const messages = ref<any[]>([])
const loadingMessages = ref(false)
const unread = ref(0)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const localeModel = ref(locale.value)

const visibleMenus = computed<MenuNode[]>(() => {
  const filter = (nodes: MenuNode[]): MenuNode[] => nodes
    .filter((menu) => auth.isPlatform || !menu.path?.startsWith('/platform'))
    .map((menu) => ({ ...menu, children: menu.children ? filter(menu.children) : [] }))
    .filter((menu) => menu.path || (menu.children && menu.children.length))
  return filter(auth.menus || [])
})

const pageTitle = computed(() => {
  const found = findMenu(auth.menus, route.path)
  return found ? (locale.value.startsWith('en') && found.nameEn ? found.nameEn : found.name) : '工作台'
})

function findMenu(nodes: MenuNode[], path: string): MenuNode | null {
  for (const node of nodes || []) {
    if (node.path === path) return node
    if (node.children) {
      const found = findMenu(node.children, path)
      if (found) return found
    }
  }
  return null
}

function switchTenant(value: number | null) {
  auth.setTenantHeader(value)
  ElMessage.success(value ? '已切换到租户只读视图' : '已切换到全部学校视图')
  router.replace({ path: route.path, query: { ...route.query, _t: Date.now() } })
  setTimeout(() => window.location.reload(), 100)
}

function changeLocale(value: string) {
  auth.setLocale(value)
  locale.value = value
}

async function loadMessages() {
  loadingMessages.value = true
  try {
    const data: any = await http.get('/system/messages', { params: { page: 1, size: 30 } })
    messages.value = data.records || []
    unread.value = data.records?.filter((m: any) => !m.readFlag).length || 0
  } finally {
    loadingMessages.value = false
  }
}

async function readMessage(msg: any) {
  if (!msg.readFlag) {
    await http.post(`/system/messages/${msg.id}/read`)
    msg.readFlag = 1
    unread.value = Math.max(0, unread.value - 1)
  }
  ElMessageBox.alert(msg.content || '', msg.title, { confirmButtonText: '知道了' })
}

async function changePassword() {
  if (passwordForm.newPassword !== passwordForm.confirm) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  await http.post('/auth/change-password', {
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword
  })
  ElMessage.success('密码已修改，请重新登录')
  showPassword.value = false
  auth.logout()
  router.push('/login')
}

function onUserCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  } else if (command === 'password') {
    showPassword.value = true
  } else {
    ElMessageBox.alert(
      `账号：${auth.user?.username}\n姓名：${auth.user?.realName || '-'}\n角色：${auth.user?.roleCode}\n租户：${auth.tenant?.name || '平台'}`,
      '账号信息'
    )
  }
}

watch(showMessages, (value) => { if (value) loadMessages() })

onMounted(async () => {
  if (!auth.menus.length) await auth.loadMe()
  if (auth.isPlatform) await auth.loadTenantOptions()
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.sidebar {
  width: 236px; background: #0f1c33; display: flex; flex-direction: column;
  transition: width 0.2s ease; flex-shrink: 0; position: sticky; top: 0; height: 100vh;
}
.sidebar.collapsed { width: 64px; }
.sidebar-brand { display: flex; align-items: center; gap: 12px; padding: 18px 16px; color: #fff; }
.logo {
  width: 34px; height: 34px; border-radius: 9px; display: grid; place-items: center;
  background: linear-gradient(135deg, #60a5fa, #2563eb); font-weight: 800;
}
.brand-text { display: flex; flex-direction: column; line-height: 1.3; }
.brand-text strong { font-size: 15px; }
.brand-text span { font-size: 11px; color: #64748b; }
.menu-scroll { flex: 1; }
.sidebar :deep(.el-menu) { border-right: none; }
.sidebar :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(31, 111, 235, 0.9), rgba(31, 111, 235, 0.5));
  border-radius: 0 8px 8px 0;
}
.sidebar-foot {
  padding: 14px 16px; border-top: 1px solid rgba(148, 163, 184, 0.12);
  display: flex; flex-direction: column; gap: 3px; font-size: 11px; color: #64748b;
}
.sidebar-foot strong { color: #94a3b8; font-weight: 500; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.header {
  height: 62px; background: #fff; display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; border-bottom: 1px solid #eef2f7; position: sticky; top: 0; z-index: 10;
}
.header .left, .header .right { display: flex; align-items: center; gap: 12px; }
.tenant-switch { width: 170px; }
.lang-switch { width: 108px; }
.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #334155; }
.user-name { font-size: 13px; max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content { flex: 1; padding: 18px 20px 32px; }
.message-item { padding: 12px 0; border-bottom: 1px solid #f1f5f9; cursor: pointer; }
.message-item p { color: #64748b; font-size: 13px; margin: 8px 0 4px; }
@media (max-width: 900px) {
  .sidebar { position: fixed; z-index: 20; }
  .header .left :deep(.el-breadcrumb) { display: none; }
  .tenant-switch { width: 120px; }
}
</style>
