<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="`${base}/news`" class="crumb">← {{ t('nav.news') }}</router-link>
        <h1 v-if="notice">{{ notice.title }}</h1>
        <p v-if="notice" class="meta">{{ formatDate(notice.publishTime) }} · {{ t('news.views') }} {{ notice.views || 0 }}</p>
      </div>
    </section>
    <div class="container">
      <article v-if="notice" class="article" v-html="safeContent" />
      <div v-else class="empty">{{ loading ? t('common.loading') : t('news.empty') }}</div>
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
const { t } = useI18n()
const store = usePortalStore()
const base = computed(() => `/${route.params.code || 'DEMO'}`)
const notice = ref<any>(null)
const loading = ref(true)

const safeContent = computed(() => String(notice.value?.content || '')
  .replace(/<script[\s\S]*?<\/script>/gi, '')
  .replace(/ on\w+="[^"]*"/gi, ''))

useSeo({
  title: () => notice.value?.title || t('news.detail'),
  description: () => String(notice.value?.content || '').replace(/<[^>]+>/g, '').slice(0, 120),
  path: () => route.path
})

async function load() {
  loading.value = true
  try {
    notice.value = await api.get(`/tenant/${route.params.code}/notices/${route.params.id}`)
  } catch (e) {
    notice.value = null
  } finally {
    loading.value = false
  }
}

function formatDate(value: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  await load()
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 40px 0 70px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(24px, 3.6vw, 34px); line-height: 1.35; max-width: 860px; }
.meta { margin: 0; color: #a9c6ee; font-size: 13px; }
.article {
  background: #fff; border: 1px solid var(--line); border-radius: var(--radius); padding: 34px clamp(20px, 4vw, 46px);
  margin-top: -36px; position: relative; z-index: 2; line-height: 1.9; color: #334155; font-size: 15px;
  box-shadow: 0 18px 40px rgba(15, 42, 82, 0.08); min-height: 220px;
}
.article :deep(img) { max-width: 100%; border-radius: 10px; }
.article :deep(h2), .article :deep(h3) { color: var(--navy); }
.article :deep(blockquote) { border-left: 4px solid var(--blue); margin: 16px 0; padding: 8px 16px; background: #f8fbff; color: var(--muted); }
.empty { background: #fff; border: 1px dashed var(--line); border-radius: var(--radius); padding: 50px; text-align: center; color: var(--muted); margin-top: 20px; }
</style>
