<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <span class="crumb">{{ t('share.source') }}</span>
        <h1>{{ t('share.title') }}</h1>
      </div>
    </section>
    <div class="container">
      <div v-if="loading" class="card empty">{{ t('common.loading') }}</div>
      <div v-else-if="data" class="card share-card">
        <span class="tag">{{ typeLabel }}</span>
        <h2>{{ data.title }}</h2>
        <p>{{ data.description }}</p>
        <dl>
          <div><dt>{{ locale.startsWith('en') ? 'Reference' : '来源标识' }}</dt><dd>{{ data.source }}</dd></div>
          <div><dt>{{ locale.startsWith('en') ? 'Item ID' : '对象编号' }}</dt><dd>#{{ data.id }}</dd></div>
        </dl>
        <div class="actions">
          <router-link :to="target" class="btn primary">{{ t('share.open') }}</router-link>
          <router-link :to="base" class="btn ghost">{{ t('nav.home') }}</router-link>
        </div>
      </div>
      <div v-else class="card empty">{{ t('error.load') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '@/api'
import { usePortalStore } from '@/store/portal'
import { useSeo } from '@/composables/useSeo'

const route = useRoute()
const { t, locale } = useI18n()
const store = usePortalStore()
const base = computed(() => `/${route.params.code || 'DEMO'}`)
const data = ref<any>(null)
const loading = ref(true)
const type = computed(() => String(route.params.type || ''))

const typeLabel = computed(() => {
  const map: Record<string, string> = {
    visitor: locale.value.startsWith('en') ? 'Visitor booking' : '访客预约',
    lost: locale.value.startsWith('en') ? 'Lost & Found' : '失物招领',
    warning: locale.value.startsWith('en') ? 'Safety alert' : '安全预警'
  }
  return map[type.value] || (locale.value.startsWith('en') ? 'Campus portal' : '校园门户')
})

const target = computed(() => {
  if (type.value === 'visitor') return `${base.value}/visitor`
  if (type.value === 'lost') return `${base.value}/lost`
  if (type.value === 'warning') return `${base.value}/news/${route.params.id}`
  return base.value
})

useSeo({
  title: () => data.value?.title || t('share.title'),
  description: () => data.value?.description || '',
  path: () => route.path
})

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  try {
    data.value = await api.get(`/tenant/${route.params.code}/share/${route.params.type}/${route.params.id}`)
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 44px 0 66px; }
.crumb { color: #a9c6ee; font-size: 12px; letter-spacing: 1px; }
.page-hero h1 { margin: 14px 0 0; font-size: clamp(26px, 4vw, 36px); }
.card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); margin-top: -32px; position: relative; z-index: 2; }
.share-card { padding: 30px; max-width: 720px; }
.tag { background: var(--sky); color: var(--blue); border-radius: 999px; padding: 4px 12px; font-size: 11px; font-weight: 700; }
.share-card h2 { margin: 14px 0 8px; color: var(--navy); font-size: 24px; }
.share-card > p { color: var(--muted); line-height: 1.75; }
dl { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 22px 0; }
dl > div { background: #f8fbff; border: 1px solid var(--line); border-radius: 10px; padding: 10px 14px; }
dt { font-size: 11px; color: var(--muted); }
dd { margin: 4px 0 0; font-weight: 600; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.empty { padding: 50px; text-align: center; color: var(--muted); }
@media (max-width: 560px) { dl { grid-template-columns: 1fr; } }
</style>
