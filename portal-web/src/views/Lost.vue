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
        <button class="btn ghost" @click="showClaimQuery = true">{{ t('lost.claimQuery') }}</button>
        <button class="btn ghost" @click="showLostPost = true">{{ t('lost.lostPost') }}</button>
        <button class="btn primary" @click="showReport = true">+ {{ t('lost.report') }}</button>
      </div>
      <p class="list-hint">{{ t('lost.listHint') }}</p>

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
              <div class="top-actions">
                <span class="tag">{{ categoryLabel(item.category) }}</span>
                <button class="claim-btn" type="button" @click="openClaim(item)">{{ t('lost.claim') }}</button>
              </div>
            </div>
            <p class="desc">{{ item.description || '-' }}</p>
            <div class="meta">
              <span>{{ t('lost.location') }}：{{ item.location || '-' }}</span>
              <span>{{ t('lost.foundAt') }}：{{ formatDate(item.foundTime) }}</span>
              <span>{{ t('lost.itemNo') }}：{{ item.itemNo }}</span>
              <span v-if="item.finder">{{ t('lost.finder') }}：{{ item.finder }}</span>
              <span v-if="item.finderPhoneMask">{{ t('lost.finderPhone') }}：{{ item.finderPhoneMask }}</span>
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

    <div v-if="showClaimQuery" class="modal-mask" @click.self="showClaimQuery = false">
      <div class="modal">
        <h2>{{ t('lost.claimQuery') }}</h2>
        <form @submit.prevent="queryClaim">
          <label class="field"><span>{{ t('lost.claimNo') }} *</span><input v-model.trim="claimQuery.id" inputmode="numeric" required :placeholder="t('lost.claimNoHint')" /></label>
          <label class="field"><span>{{ t('lost.claimPhone') }} *</span><input v-model.trim="claimQuery.phone" inputmode="tel" maxlength="11" required :placeholder="t('lost.claimPhoneHint')" /></label>
          <p v-if="claimQueryError" class="error">{{ claimQueryError }}</p>
          <div v-if="claimQueryResult" class="claim-result">
            <div class="between">
              <strong>{{ claimQueryResult.itemName || '-' }}</strong>
              <span class="claim-status" :class="claimQueryResult.status === 'APPROVED' ? 'ok' : 'wait'">{{ claimQueryResult.statusText }}</span>
            </div>
            <p class="muted">编号：{{ claimQueryResult.itemNo || '-' }} · {{ formatDate(claimQueryResult.claimTime) }}</p>
            <p v-if="claimQueryResult.remark" class="muted">{{ claimQueryResult.remark }}</p>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn ghost" @click="showClaimQuery = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn primary" :disabled="claimQuerying">{{ t('common.search') }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showClaim" class="modal-mask" @click.self="showClaim = false">
      <div class="modal">
        <h2>{{ t('lost.claimTitle') }}</h2>
        <p class="muted claim-item">{{ claimItem?.name }} · {{ claimItem?.itemNo }}</p>
        <form @submit.prevent="submitClaim">
          <label class="field"><span>{{ t('lost.claimant') }} *</span><input v-model.trim="claimForm.claimant" required maxlength="30" /></label>
          <label class="field"><span>{{ t('lost.claimPhone') }} *</span><input v-model.trim="claimForm.phone" inputmode="tel" maxlength="11" required /></label>
          <label class="field"><span>{{ t('lost.claimIdCard') }}</span><input v-model.trim="claimForm.idCard" maxlength="18" /></label>
          <label class="field"><span>{{ t('lost.claimDesc') }}</span><textarea v-model.trim="claimForm.remark" rows="3" maxlength="200" :placeholder="t('lost.claimDescHint')" /></label>
          <p v-if="claimError" class="error">{{ claimError }}</p>
          <div v-if="claimSuccess" class="claim-success">
            <p class="success">{{ t('lost.claimSuccess') }}</p>
            <p class="claim-no">{{ t('lost.claimNo') }}：#{{ claimResult?.id }}</p>
            <button type="button" class="btn ghost" @click="queryFromClaim">{{ t('lost.claimQueryNow') }}</button>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn ghost" @click="showClaim = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn primary" :disabled="claiming">{{ t('common.submit') }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showLostPost" class="modal-mask" @click.self="showLostPost = false">
      <div class="modal">
        <h2>{{ t('lost.lostPostTitle') }}</h2>
        <p class="muted claim-item">{{ t('lost.lostPostTip') }}</p>
        <form @submit.prevent="submitLostPost">
          <label class="field"><span>{{ t('lost.itemName') }} *</span><input v-model.trim="lostPost.name" required maxlength="60" /></label>
          <label class="field"><span>{{ t('lost.location') }}</span><input v-model.trim="lostPost.location" maxlength="100" :placeholder="t('lost.lostLocationHint')" /></label>
          <label class="field"><span>{{ t('lost.lostTime') }}</span><input v-model="lostPost.lostTime" type="datetime-local" /></label>
          <label class="field"><span>{{ t('lost.description') }}</span><textarea v-model.trim="lostPost.description" rows="3" maxlength="300" /></label>
          <label class="field"><span>{{ t('lost.reporter') }} *</span><input v-model.trim="lostPost.reporter" required maxlength="30" /></label>
          <label class="field"><span>{{ t('lost.claimPhone') }} *</span><input v-model.trim="lostPost.phone" inputmode="tel" maxlength="11" required /></label>
          <p v-if="lostPostError" class="error">{{ lostPostError }}</p>
          <p v-if="lostPostSuccess" class="success">{{ t('lost.lostPostSuccess') }}</p>
          <div class="modal-actions">
            <button type="button" class="btn ghost" @click="showLostPost = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn primary" :disabled="lostPosting">{{ t('common.submit') }}</button>
          </div>
        </form>
      </div>
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
          <label class="field"><span>{{ t('lost.finderPhone') }}</span><input v-model.trim="report.phone" inputmode="tel" maxlength="11" :placeholder="t('lost.finderPhoneHint')" /></label>
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
  name: '', category: 'OTHER', location: '', description: '', finder: '', phone: '', storageLocation: ''
})
const showLostPost = ref(false)
const lostPosting = ref(false)
const lostPostError = ref('')
const lostPostSuccess = ref(false)
const lostPost = reactive<any>({ name: '', location: '', lostTime: '', description: '', reporter: '', phone: '' })
const showClaim = ref(false)
const claimItem = ref<any>(null)
const claiming = ref(false)
const claimError = ref('')
const claimSuccess = ref(false)
const claimForm = reactive<any>({ claimant: '', phone: '', idCard: '', remark: '' })
const claimResult = ref<any>(null)
const showClaimQuery = ref(false)
const claimQuerying = ref(false)
const claimQueryError = ref('')
const claimQueryResult = ref<any>(null)
const claimQuery = reactive({ id: '', phone: '' })

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
  if (report.phone && !/^1[3-9]\d{9}$/.test(report.phone)) {
    reportError.value = t('lost.claimInvalidPhone')
    return
  }
  reporting.value = true
  try {
    await api.post(`/tenant/${route.params.code}/lost-report`, report)
    reportSuccess.value = true
    setTimeout(() => {
      showReport.value = false
      reportSuccess.value = false
      Object.assign(report, { name: '', category: 'OTHER', location: '', description: '', finder: '', phone: '', storageLocation: '' })
      load()
    }, 1200)
  } catch (e: any) {
    reportError.value = e.message || t('error.load')
  } finally {
    reporting.value = false
  }
}

function openClaim(item: any) {
  claimItem.value = item
  claimError.value = ''
  claimSuccess.value = false
  claimResult.value = null
  Object.assign(claimForm, { claimant: '', phone: '', idCard: '', remark: '' })
  showClaim.value = true
}

async function submitClaim() {
  claimError.value = ''
  claimSuccess.value = false
  if (!/^1[3-9]\d{9}$/.test(claimForm.phone)) {
    claimError.value = t('lost.claimInvalidPhone')
    return
  }
  claiming.value = true
  try {
    claimResult.value = await api.post(`/tenant/${route.params.code}/lost-claim`, {
      itemId: claimItem.value.id,
      claimant: claimForm.claimant,
      phone: claimForm.phone,
      idCard: claimForm.idCard || undefined,
      remark: claimForm.remark || undefined
    })
    claimSuccess.value = true
  } catch (e: any) {
    claimError.value = e.message || t('error.load')
  } finally {
    claiming.value = false
  }
}

function queryFromClaim() {
  showClaim.value = false
  claimQuery.id = String(claimResult.value?.id || '')
  claimQuery.phone = claimForm.phone
  claimQueryError.value = ''
  claimQueryResult.value = null
  showClaimQuery.value = true
}

async function submitLostPost() {
  lostPostError.value = ''
  lostPostSuccess.value = false
  if (!/^1[3-9]\d{9}$/.test(lostPost.phone)) {
    lostPostError.value = t('lost.claimInvalidPhone')
    return
  }
  lostPosting.value = true
  try {
    await api.post(`/tenant/${route.params.code}/lost-post`, {
      name: lostPost.name,
      location: lostPost.location || undefined,
      lostTime: lostPost.lostTime ? String(lostPost.lostTime).replace('T', ' ') : undefined,
      description: lostPost.description || undefined,
      reporter: lostPost.reporter,
      phone: lostPost.phone
    })
    lostPostSuccess.value = true
    setTimeout(() => {
      showLostPost.value = false
      lostPostSuccess.value = false
      Object.assign(lostPost, { name: '', location: '', lostTime: '', description: '', reporter: '', phone: '' })
      loadNotices()
    }, 1400)
  } catch (e: any) {
    lostPostError.value = e.message || t('error.load')
  } finally {
    lostPosting.value = false
  }
}

async function queryClaim() {
  claimQueryError.value = ''
  claimQueryResult.value = null
  if (!claimQuery.id || !/^1[3-9]\d{9}$/.test(claimQuery.phone)) {
    claimQueryError.value = t('lost.claimInvalidPhone')
    return
  }
  claimQuerying.value = true
  try {
    const raw = String(claimQuery.id || '').trim()
    const params: any = { phone: claimQuery.phone }
    if (/^\d+$/.test(raw)) {
      params.id = raw
    } else {
      params.itemNo = raw
    }
    claimQueryResult.value = await api.get(`/tenant/${route.params.code}/lost-claim/query`, { params })
  } catch (e: any) {
    claimQueryError.value = e.message || t('error.load')
  } finally {
    claimQuerying.value = false
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
.list-hint { margin: 14px 2px 0; font-size: 12px; color: #8496ab; }
.claim-success { background: #f0fbf6; border: 1px solid #c9efdd; border-radius: 10px; padding: 10px 12px; margin-bottom: 10px; }
.claim-success .success { margin: 0; }
.claim-no { margin: 6px 0 10px; font-size: 14px; font-weight: 700; color: #0f9d63; }
.list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-top: 22px; }
.lost-item { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); display: flex; overflow: hidden; }
.photo {
  width: 150px; flex-shrink: 0; background: linear-gradient(135deg, #e8f1ff, #dbeafe);
  display: grid; place-items: center; font-size: 34px; color: #2563eb; font-weight: 800;
}
.body { padding: 16px 18px; min-width: 0; flex: 1; }
.top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.top h3 { margin: 0; font-size: 16px; }
.top-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.tag { background: var(--sky); color: var(--blue); border-radius: 999px; padding: 3px 10px; font-size: 11px; font-weight: 600; }
.claim-btn {
  border: 1px solid #bcd2f4; background: #f4f8ff; color: var(--blue); border-radius: 999px;
  padding: 4px 12px; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.16s ease;
}
.claim-btn:hover { background: var(--blue); color: #fff; border-color: var(--blue); }
.claim-item { margin: -6px 0 14px; font-size: 13px; }
.claim-result { background: #f8fbff; border: 1px solid var(--line); border-radius: 10px; padding: 12px 14px; margin-bottom: 10px; }
.claim-result .between { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.claim-result p { margin: 6px 0 0; font-size: 12px; }
.claim-status { border-radius: 999px; padding: 3px 10px; font-size: 12px; font-weight: 700; }
.claim-status.ok { background: #e6f8f0; color: #0f9d63; }
.claim-status.wait { background: #fff4dd; color: #c47a06; }
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
