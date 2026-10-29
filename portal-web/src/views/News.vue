<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="base" class="crumb">← {{ t('common.back') }}</router-link>
        <h1>{{ t('news.title') }}</h1>
        <p>{{ t('news.subtitle') }}</p>
      </div>
    </section>

    <div class="container content">
      <div v-if="loading" class="empty">{{ t('common.loading') }}</div>
      <div v-else-if="!notices.length" class="empty">{{ t('news.empty') }}</div>
      <div v-else class="list">
        <router-link v-for="item in notices" :key="item.id" :to="`${base}/news/${item.id}`" class="news-item">
          <div class="date">
            <strong>{{ dayOf(item.publishTime) }}</strong>
            <span>{{ monthOf(item.publishTime) }}</span>
          </div>
          <div class="body">
            <div class="top">
              <span class="tag">{{ item.category || 'NOTICE' }}</span>
              <span class="muted">{{ formatDate(item.publishTime) }}</span>
            </div>
            <h2>{{ item.title }}</h2>
            <p>{{ strip(item.content).slice(0, 160) }}</p>
            <footer>
              <span>{{ t('news.views') }} {{ item.views || 0 }}</span>
              <span class="more">{{ t('common.viewDetail') }} →</span>
            </footer>
          </div>
        </router-link>
      </div>
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
const notices = ref<any[]>([])
const loading = ref(true)

useSeo({ title: () => t('news.title'), description: () => t('news.subtitle'), path: () => route.path })

async function load() {
  loading.value = true
  try {
    notices.value = (await api.get(`/tenant/${route.params.code}/notices`, { params: { limit: 50 } })) as any
  } finally {
    loading.value = false
  }
}

function strip(html: string) {
  return String(html || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
}
function formatDate(value: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}
function dayOf(value: string) {
  return value ? String(value).slice(8, 10) : '--'
}
function monthOf(value: string) {
  return value ? String(value).slice(5, 7) : '--'
}

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  await load()
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 40px 0 56px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(26px, 4vw, 36px); }
.page-hero p { margin: 0; color: #c7d7ee; }
.content { margin-top: -26px; position: relative; z-index: 2; }
.list { display: flex; flex-direction: column; gap: 14px; }
.news-item {
  background: #fff; border: 1px solid var(--line); border-radius: var(--radius); padding: 18px;
  display: flex; gap: 18px; transition: all 0.18s ease;
}
.news-item:hover { border-color: #bcd2f4; transform: translateY(-1px); box-shadow: 0 12px 26px rgba(15, 42, 82, 0.07); }
.date {
  width: 62px; height: 66px; border-radius: 12px; background: var(--sky); color: var(--blue);
  display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0;
}
.date strong { font-size: 22px; line-height: 1; }
.date span { font-size: 11px; margin-top: 4px; }
.body { flex: 1; min-width: 0; }
.top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.tag { background: #eef4ff; color: var(--blue); border-radius: 999px; padding: 3px 9px; font-size: 10px; font-weight: 700; }
.body h2 { margin: 8px 0 6px; font-size: 17px; color: var(--navy); }
.body p { margin: 0 0 10px; color: var(--muted); font-size: 13px; line-height: 1.65; }
footer { display: flex; justify-content: space-between; font-size: 12px; color: #94a3b8; }
.more { color: var(--blue); font-weight: 600; }
.empty { background: #fff; border: 1px dashed var(--line); border-radius: var(--radius); padding: 40px; text-align: center; color: var(--muted); }
@media (max-width: 600px) { .date { display: none; } }
</style>
