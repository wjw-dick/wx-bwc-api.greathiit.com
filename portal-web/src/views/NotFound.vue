<template>
  <div class="container notfound">
    <span class="code">404</span>
    <h1>{{ locale.startsWith('en') ? 'Page not found' : '页面不存在' }}</h1>
    <p>{{ locale.startsWith('en') ? 'The page may have moved or the link is invalid.' : '页面可能已移动，或链接地址不正确。' }}</p>
    <router-link to="/DEMO" class="btn primary">{{ locale.startsWith('en') ? 'Back to portal' : '返回门户首页' }}</router-link>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { locale } = useI18n()
</script>

<style scoped>
.notfound { padding: 100px 0; text-align: center; }
.code { font-size: 80px; font-weight: 800; color: #dbeafe; letter-spacing: 4px; }
h1 { color: var(--navy); margin: 8px 0; }
p { color: var(--muted); margin-bottom: 24px; }
</style>
