<template>
  <template v-for="menu in menus" :key="menu.id">
    <el-sub-menu v-if="menu.children && menu.children.length" :index="String(menu.id)">
      <template #title>
        <el-icon><component :is="menu.icon || 'Menu'" /></el-icon>
        <span>{{ label(menu) }}</span>
      </template>
      <MenuItem :menus="menu.children" />
    </el-sub-menu>
    <el-menu-item v-else :index="menu.path">
      <el-icon><component :is="menu.icon || 'Document'" /></el-icon>
      <template #title>{{ label(menu) }}</template>
    </el-menu-item>
  </template>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { MenuNode } from '@/store/auth'

defineProps<{ menus: MenuNode[] }>()
const { locale } = useI18n()
function label(menu: MenuNode) {
  return locale.value.startsWith('en') && menu.nameEn ? menu.nameEn : menu.name
}
</script>
