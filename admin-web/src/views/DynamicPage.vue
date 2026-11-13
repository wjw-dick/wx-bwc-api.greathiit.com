<template>
  <component :is="current" v-if="current" :resource="resource" />
  <div v-else class="page-card">
    <el-empty description="页面不存在或暂无访问权限">
      <el-button type="primary" @click="router.push('/dashboard')">返回工作台</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { menuResource, specialViews } from '@/config/resources'
import ResourceList from '@/views/ResourceList.vue'

const route = useRoute()
const router = useRouter()

const specialComponents: Record<string, Component> = {
  Dashboard: defineAsyncComponent(() => import('@/views/Dashboard.vue')),
  PlatformDashboard: defineAsyncComponent(() => import('@/views/PlatformDashboard.vue')),
  VisitorStats: defineAsyncComponent(() => import('@/views/VisitorStats.vue')),
  PatrolStats: defineAsyncComponent(() => import('@/views/PatrolStats.vue')),
  TenantReports: defineAsyncComponent(() => import('@/views/TenantReports.vue')),
  PlatformReports: defineAsyncComponent(() => import('@/views/PlatformReports.vue')),
  MonthlyReports: defineAsyncComponent(() => import('@/views/MonthlyReports.vue')),
  PlatformParams: defineAsyncComponent(() => import('@/views/PlatformParams.vue')),
  TenantConfig: defineAsyncComponent(() => import('@/views/TenantConfig.vue')),
  Dictionaries: defineAsyncComponent(() => import('@/views/Dictionaries.vue')),
  Menus: defineAsyncComponent(() => import('@/views/Menus.vue')),
  Languages: defineAsyncComponent(() => import('@/views/Languages.vue')),
  StorageConfig: defineAsyncComponent(() => import('@/views/StorageConfig.vue')),
  WechatConfig: defineAsyncComponent(() => import('@/views/WechatConfig.vue')),
  MessageTemplates: defineAsyncComponent(() => import('@/views/MessageTemplates.vue')),
  Tenants: defineAsyncComponent(() => import('@/views/Tenants.vue')),
  Users: defineAsyncComponent(() => import('@/views/Users.vue')),
  Roles: defineAsyncComponent(() => import('@/views/Roles.vue')),
  AuditLogs: defineAsyncComponent(() => import('@/views/AuditLogs.vue')),
  PlateConfig: defineAsyncComponent(() => import('@/views/PlateConfig.vue')),
  GateCheck: defineAsyncComponent(() => import('@/views/GateCheck.vue'))
}

const extraSpecial: Record<string, string> = {
  '/platform/tenants': 'Tenants',
  '/platform/audit': 'AuditLogs',
  '/system/users': 'Users',
  '/system/roles': 'Roles',
  '/vehicle/plate-config': 'PlateConfig'
}

const path = computed(() => route.path)
const specialName = computed(() => specialViews[path.value] || extraSpecial[path.value])
const resource = computed(() => menuResource[path.value] || '')
const current = computed<Component | null>(() => {
  if (specialName.value && specialComponents[specialName.value]) {
    return specialComponents[specialName.value]
  }
  if (resource.value) return ResourceList
  return null
})
</script>
