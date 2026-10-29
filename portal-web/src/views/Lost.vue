<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="base" class="crumb">← {{ t('common.back') }}</router-link>
        <h1>{{ t('lost.title') }}</h1>
        <p>{{ t('lost.subtitle') }}</p>
      </div>
    </section>

    <div class="container">
      <div class="toolbar">
        <input v-model.trim="keyword" class="search" :placeholder="t('lost.keyword')" @keyup.enter="load" />
        <select v-model="category" @change="load">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <button class="btn ghost" @click="load">{{ t('common.search') }}</button>
        <button class="btn primary" @click="showReport = true">+ {{ t('lost.report') }}</button>
      </div>

      <div v-if="loading" class="empty">{{ t('common.loading') }}</div>
      <div v-else-if="!items.length" class="empty">{{ t('lost.empty') }}</div>
      <div v-else class="list">
        <article v-for="item in items" :key="item.id" class="lost-item">
          <div class="photo" :style="photoStyle(item.photos)">
            <span v-if="!item.photos">{{ item.name?.slice(0, 1) }}</span>
          </div>
          <div class="body">
            <div class="top">
              <h3>{{ item.name }}</h3>
              <span class="tag">{{ categoryLabel(item.category) }}</span>
            </div>
            <p class="desc">{{ item.description || '-' }}</p>
            <div class="meta">
              <span>{{ t('lost.location') }}：{{ item.location || '-' }}</span>
              <span>{{ t('lost.foundAt') }}：{{ formatDate(item.foundTime) }}</span>
              <span>{{ t('lost.itemNo') }}：{{ item.itemNo }}</span>
              <span v-if="item.storageLocation">{{ t('lost.storageLabel') }}：{{ item.storageLocation }}</span>
            </div>
          </div>
        </article>
      </div>

      <section v-if="notices.length" class="notices">
        <h2>{{ locale.startsWith('en') ? 'Lost & Found notices' : '失物招领公告' }}</h2>
        <div class="notice" v-for="notice in notices.slice(0, 5)" :key="notice.id">
          <strong>{{ notice.title }}</strong>
          <p>{{ notice.content }}</p>
          <span class="muted">{{ formatDate(notice.publishTime) }}</span>
        </div>
      </section>
    </div>

    <div v-if="showReport" class="modal-mask" @click.self="showReport = false">
      <div class="modal">
        <h2>{{ t('lost.reportTitle') }}</h2>
        <form @submit.prevent="submitReport">
          <label class="field"><span>{{ t('lost.itemName') }} *</span><input v-model.trim="report.name" required maxlength="60" /></label>
          <label class="field">
            <span>{{ t('lost.category') }}</span>
            <select v-model="report.category">
              <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label class="field"><span>{{ t('lost.location') }}</span><input v-model.trim="report.location" maxlength="100" /></label>
          <label class="field"><span>{{ t('lost.description') }}</span><textarea v-model.trim="report.description" rows="3" maxlength="300" /></label>
          <label class="field"><span>{{ t('lost.finder') }}</span><input v-model.trim="report.finder" maxlength="30" /></label>
          <label class="field"><span>{{ t('lost.storage') }}</span><input v-model.trim="report.storageLocation" maxlength="100" /></label>
          <p v-if="reportError" class="error">{{ reportError }}</p>
          <p v-if="reportSuccess" class="success">{{ t('lost.reportSuccess') }}</p>
          <div class="modal-actions">
            <button type="button" class="btn ghost" @click="showReport = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn primary" :disabled="reporting">{{ t('common.submit') }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '@/api'
import { usePortalStore } from '@/store/portal'
import { useSeo } from '@/composables/useSeo'

const route = useRoute()
const { t, locale } = useI18n()
const store = usePortalStore()
const base = computed(() => `/${route.params.code || 'DEMO'}`)
const items = ref<any[]>([])
const notices = ref<any[]>([])
const loading = ref(true)
const keyword = ref('')
const category = ref('')
const showReport = ref(false)
const reporting = ref(false)
const reportError = ref('')
const reportSuccess = ref(false)
const report = reactive<any>({
  name: '', category: 'OTHER', location: '', description: '', finder: '', storageLocation: ''
})

const categories = computed(() => [
  { value: 'ELECTRONICS', label: locale.value.startsWith('en') ? 'Electronics' : '电子产品' },
  { value: 'CARD', label: locale.value.startsWith('en') ? 'Cards & certificates' : '证件卡类' },
  { value: 'CLOTHING', label: locale.value.startsWith('en') ? 'Clothing' : '衣物' },
  { value: 'BOOK', label: locale.value.startsWith('en') ? 'Books' : '书籍资料' },
  { value: 'OTHER', label: locale.value.startsWith('en') ? 'Other' : '其他' }
])

useSeo({
  title: () => t('lost.title'),
  description: () => t('lost.subtitle'),
  path: () => route.path
})

async function load() {
  loading.value = true
  try {
    const data: any = await api.get(`/tenant/${route.params.code}/lost`, {
      params: { keyword: keyword.value || undefined, category: category.value || undefined }
    })
    items.value = data || []
  } finally {
    loading.value = false
  }
}

async function loadNotices() {
  try {
    notices.value = (await api.get(`/tenant/${route.params.code}/lost-notices`)) as any
  } catch (e) {
    notices.value = []
  }
}

async function submitReport() {
  reportError.value = ''
  reportSuccess.value = false
  reporting.value = true
  try {
    await api.post(`/tenant/${route.params.code}/lost-report`, report)
    reportSuccess.value = true
    setTimeout(() => {
      showReport.value = false
      reportSuccess.value = false
      Object.assign(report, { name: '', category: 'OTHER', location: '', description: '', finder: '', storageLocation: '' })
      load()
    }, 1200)
  } catch (e: any) {
    reportError.value = e.message || t('error.load')
  } finally {
    reporting.value = false
  }
}

function formatDate(value: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function categoryLabel(value: string) {
  return categories.value.find((item) => item.value === value)?.label || value || '-'
}

function photoStyle(photos: string) {
  if (!photos) return {}
  return { backgroundImage: `url(${String(photos).split(',')[0]})`, backgroundSize: 'cover', backgroundPosition: 'center' }
}

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  await Promise.all([load(), loadNotices()])
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 40px 0 56px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(26px, 4vw, 36px); }
.page-hero p { margin: 0; color: #c7d7ee; max-width: 620px; line-height: 1.65; }
.toolbar {
  margin-top: -24px; background: #fff; border: 1px solid var(--line); border-radius: var(--radius);
  padding: 14px; display: flex; gap: 10px; flex-wrap: wrap; align-items: center;
  box-shadow: 0 12px 30px rgba(15, 42, 82, 0.06); position: relative; z-index: 2;
}
.search, .toolbar select {
  height: 42px; border: 1px solid #dbe5f2; border-radius: 10px; padding: 0 12px;
  background: #fbfdff; font-size: 14px; font-family: inherit; outline: none;
}
.search { flex: 1; min-width: 200px; }
.toolbar .btn { height: 42px; }
.list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-top: 22px; }
.lost-item { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); display: flex; overflow: hidden; }
.photo {
  width: 150px; flex-shrink: 0; background: linear-gradient(135deg, #e8f1ff, #dbeafe);
  display: grid; place-items: center; font-size: 34px; color: #2563eb; font-weight: 800;
}
.body { padding: 16px 18px; min-width: 0; flex: 1; }
.top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.top h3 { margin: 0; font-size: 16px; }
.tag { background: var(--sky); color: var(--blue); border-radius: 999px; padding: 3px 10px; font-size: 11px; font-weight: 600; }
.desc { color: var(--muted); font-size: 13px; line-height: 1.6; margin: 8px 0; }
.meta { display: flex; flex-direction: column; gap: 3px; font-size: 12px; color: #8496ab; }
.notices { margin-top: 34px; }
.notices h2 { font-size: 18px; color: var(--navy); }
.notice { background: #fff; border: 1px solid var(--line); border-radius: 12px; padding: 14px 16px; margin-bottom: 10px; }
.notice p { margin: 6px 0; color: var(--muted); font-size: 13px; line-height: 1.6; }
.empty { background: #fff; border: 1px dashed var(--line); border-radius: var(--radius); padding: 40px; text-align: center; color: var(--muted); margin-top: 22px; }
.modal-mask {
  position: fixed; inset: 0; background: rgba(8, 22, 43, 0.55); z-index: 100;
  display: grid; place-items: center; padding: 18px;
}
.modal { background: #fff; border-radius: 18px; padding: 24px; width: min(520px, 100%); max-height: 90vh; overflow: auto; }
.modal h2 { margin: 0 0 16px; font-size: 20px; color: var(--navy); }
.field { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #334155; margin-bottom: 12px; }
.field input, .field select, .field textarea {
  border: 1px solid #dbe5f2; border-radius: 10px; padding: 10px 12px; font-size: 14px;
  background: #fbfdff; font-family: inherit; outline: none;
}
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
.error { color: #be123c; font-size: 13px; }
.success { color: #0f9d63; font-size: 13px; }
@media (max-width: 820px) { .list { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .photo { width: 96px; } }
</style>
