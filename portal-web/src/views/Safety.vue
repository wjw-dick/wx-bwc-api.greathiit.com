<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="base" class="crumb">← {{ t('common.back') }}</router-link>
        <h1>{{ t('safety.title') }}</h1>
        <p>{{ t('safety.subtitle') }}</p>
      </div>
    </section>

    <div class="container layout">
      <section class="card form-card">
        <div class="tabs">
          <button
            v-for="item in categories"
            :key="item.value"
            type="button"
            class="tab"
            :class="{ active: form.category === item.value }"
            @click="switchCategory(item.value)"
          >
            <span class="tab-icon" :class="item.tone">{{ item.icon }}</span>
            {{ item.label }}
          </button>
        </div>

        <div v-if="success" class="success">
          <div class="success-icon">✓</div>
          <h2>{{ t('safety.success') }}</h2>
          <p class="muted">{{ success.message }}</p>
          <dl class="result">
            <div><dt>{{ t('safety.reportNo') }}</dt><dd>{{ success.reportNo }}</dd></div>
            <div><dt>{{ t('safety.category') }}</dt><dd>{{ categoryLabel(success.category) }}</dd></div>
            <div><dt>{{ t('safety.reporter') }}</dt><dd>{{ success.reporter }}</dd></div>
            <div><dt>{{ t('safety.status') }}</dt><dd>{{ t('safety.statusReported') }}</dd></div>
          </dl>
          <p class="query-tip">{{ t('safety.queryTip') }}</p>
          <div class="success-actions">
            <button class="btn primary" @click="reset">{{ t('safety.again') }}</button>
            <button class="btn ghost" @click="scrollToQuery">{{ t('safety.queryNow') }}</button>
            <router-link :to="base" class="btn ghost">{{ t('nav.home') }}</router-link>
          </div>
        </div>

        <form v-else @submit.prevent="submit">
          <div class="form-grid">
            <label class="field">
              <span>{{ t('safety.reporter') }} <em>*</em></span>
              <input v-model.trim="form.reporter" maxlength="30" required :placeholder="t('safety.reporterHint')" />
            </label>
            <label class="field">
              <span>{{ t('safety.phone') }} <em>*</em></span>
              <input v-model.trim="form.phone" inputmode="tel" maxlength="11" required :placeholder="t('safety.phoneHint')" />
            </label>
            <label class="field">
              <span>{{ t('safety.location') }} <em>*</em></span>
              <input v-model.trim="form.location" maxlength="100" required :placeholder="t('safety.locationHint')" />
            </label>
            <label v-if="form.category !== 'DEVICE'" class="field">
              <span>{{ t('safety.type') }}</span>
              <select v-model="form.type">
                <option v-for="item in types" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label v-else class="field">
              <span>{{ t('safety.deviceName') }}</span>
              <input v-model.trim="form.deviceName" maxlength="60" :placeholder="t('safety.deviceNameHint')" />
            </label>
            <label v-if="form.category !== 'DEVICE'" class="field">
              <span>{{ t('safety.level') }}</span>
              <select v-model="form.level">
                <option v-for="item in levels" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
          </div>
          <label class="field block">
            <span>{{ t('safety.description') }} <em>*</em></span>
            <textarea v-model.trim="form.description" rows="4" maxlength="500" required :placeholder="t('safety.descriptionHint')" />
          </label>

          <p v-if="error" class="error">{{ error }}</p>
          <button class="btn primary block" :disabled="submitting" type="submit">
            {{ submitting ? t('common.loading') : t('safety.submit') }}
          </button>
        </form>
      </section>

      <aside class="side">
        <div class="card side-card">
          <h3>{{ t('safety.processTitle') }}</h3>
          <ol class="steps">
            <li><b>1</b><div><strong>{{ t('safety.stepReport') }}</strong><p>{{ t('safety.stepReportTip') }}</p></div></li>
            <li><b>2</b><div><strong>{{ t('safety.stepHandle') }}</strong><p>{{ t('safety.stepHandleTip') }}</p></div></li>
            <li><b>3</b><div><strong>{{ t('safety.stepTrack') }}</strong><p>{{ t('safety.stepTrackTip') }}</p></div></li>
          </ol>
        </div>
        <div class="card side-card notice">
          <p>{{ t('safety.notice') }}</p>
          <p v-if="store.state.tenant?.dutyPhone" class="duty">{{ t('common.contact') }}：{{ store.state.tenant.dutyPhone }}</p>
        </div>
      </aside>
    </div>

    <div ref="querySection" class="container query-wrap">
      <section class="card query-card">
        <div class="query-head">
          <div>
            <h2>{{ t('safety.queryTitle') }}</h2>
            <p class="muted">{{ t('safety.querySubtitle') }}</p>
          </div>
        </div>
        <form class="query-form" @submit.prevent="query">
          <label class="field">
            <span>{{ t('safety.category') }}</span>
            <select v-model="queryForm.category">
              <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>{{ t('safety.reportNo') }}</span>
            <input v-model.trim="queryForm.reportNo" :placeholder="t('safety.reportNoHint')" />
          </label>
          <label class="field">
            <span>{{ t('safety.phone') }}</span>
            <input v-model.trim="queryForm.phone" inputmode="tel" maxlength="11" :placeholder="t('safety.phoneHint')" />
          </label>
          <label v-if="queryForm.category !== 'SECURITY'" class="field">
            <span>{{ t('safety.reporter') }}</span>
            <input v-model.trim="queryForm.reporter" maxlength="30" :placeholder="t('safety.reporterHint')" />
          </label>
          <button class="btn primary" type="submit" :disabled="queryLoading">
            {{ queryLoading ? t('common.loading') : t('common.search') }}
          </button>
        </form>
        <p v-if="queryError" class="error">{{ queryError }}</p>
        <div v-if="queryResult" class="result-card">
          <div class="result-head">
            <div>
              <h3>{{ queryResult.reportNo }}</h3>
              <p class="muted">{{ queryResult.createTime || '-' }}</p>
            </div>
            <span class="status" :class="queryResult.status === 'CLOSED' || queryResult.status === 'DONE' ? 'ok' : 'wait'">
              {{ queryResult.statusText }}
            </span>
          </div>
          <p v-if="queryResult.result" class="reply">{{ queryResult.result }}</p>
          <p v-else class="muted">{{ t('safety.waitHandle') }}</p>
        </div>
      </section>
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
const submitting = ref(false)
const success = ref<any>(null)
const error = ref('')
const querySection = ref<HTMLElement | null>(null)
const queryLoading = ref(false)
const queryError = ref('')
const queryResult = ref<any>(null)

const form = reactive({
  category: 'SECURITY', reporter: '', phone: '', location: '', type: '', deviceName: '',
  level: 'GENERAL', description: ''
})
const queryForm = reactive({ category: 'SECURITY', reportNo: '', phone: '', reporter: '' })

const categories = computed(() => [
  { value: 'SECURITY', label: locale.value.startsWith('en') ? 'Security incident' : '治安事件', icon: '警', tone: 'blue' },
  { value: 'FIRE', label: locale.value.startsWith('en') ? 'Fire hazard' : '消防隐患', icon: '火', tone: 'amber' },
  { value: 'DEVICE', label: locale.value.startsWith('en') ? 'Device fault' : '设备故障', icon: '修', tone: 'green' }
])

const securityTypes = computed(() => locale.value.startsWith('en')
  ? ['Theft', 'Dispute', 'Assistance', 'Suspicious person', 'Other']
  : ['失窃', '纠纷', '求助', '可疑人员', '其他'])
const fireTypes = computed(() => locale.value.startsWith('en')
  ? ['Fire facility', 'Evacuation route', 'Illegal electricity', 'Other']
  : ['消防设施', '疏散通道', '违规用电', '其他'])
const types = computed(() => form.category === 'FIRE' ? fireTypes.value : securityTypes.value)
const levels = computed(() => form.category === 'FIRE'
  ? [
      { value: 'GENERAL', label: locale.value.startsWith('en') ? 'General' : '一般隐患' },
      { value: 'MAJOR', label: locale.value.startsWith('en') ? 'Major' : '较大隐患' },
      { value: 'CRITICAL', label: locale.value.startsWith('en') ? 'Critical' : '重大隐患' }
    ]
  : [
      { value: 'NORMAL', label: locale.value.startsWith('en') ? 'Normal' : '一般' },
      { value: 'URGENT', label: locale.value.startsWith('en') ? 'Urgent' : '紧急' },
      { value: 'CRITICAL', label: locale.value.startsWith('en') ? 'Critical' : '重大' }
    ])

useSeo({
  title: () => t('safety.title'),
  description: () => t('safety.subtitle'),
  path: () => route.path
})

function switchCategory(value: string) {
  form.category = value
  form.type = ''
  form.level = value === 'FIRE' ? 'GENERAL' : 'NORMAL'
  error.value = ''
}

function categoryLabel(value: string) {
  return categories.value.find((item) => item.value === value)?.label || value
}

async function submit() {
  error.value = ''
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    error.value = t('safety.invalidPhone')
    return
  }
  submitting.value = true
  try {
    const payload: any = {
      category: form.category,
      reporter: form.reporter,
      phone: form.phone,
      location: form.location,
      description: form.description,
      type: form.type || undefined,
      deviceName: form.deviceName || undefined
    }
    if (form.category === 'FIRE') {
      payload.level = form.level
    } else if (form.category === 'SECURITY') {
      payload.urgency = form.level
    }
    const data: any = await api.post(`/tenant/${route.params.code}/safety-report`, payload)
    success.value = { ...data, reporter: form.reporter }
  } catch (e: any) {
    error.value = e.message || t('error.load')
  } finally {
    submitting.value = false
  }
}

function reset() {
  success.value = null
  Object.assign(form, {
    category: 'SECURITY', reporter: '', phone: '', location: '', type: '',
    deviceName: '', level: 'NORMAL', description: ''
  })
}

function scrollToQuery() {
  queryForm.category = success.value?.category || 'SECURITY'
  queryForm.reportNo = success.value?.reportNo || ''
  queryForm.phone = form.phone
  queryForm.reporter = form.reporter
  querySection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function query() {
  queryError.value = ''
  queryResult.value = null
  if (!queryForm.reportNo || !/^1[3-9]\d{9}$/.test(queryForm.phone)) {
    queryError.value = t('safety.queryInvalid')
    return
  }
  queryLoading.value = true
  try {
    queryResult.value = await api.get(`/tenant/${route.params.code}/safety-report/query`, {
      params: {
        category: queryForm.category,
        reportNo: queryForm.reportNo,
        phone: queryForm.phone,
        reporter: queryForm.category === 'SECURITY' ? undefined : queryForm.reporter
      }
    })
  } catch (e: any) {
    queryError.value = e.message || t('error.load')
  } finally {
    queryLoading.value = false
  }
}

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  form.level = 'NORMAL'
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 40px 0 56px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(26px, 4vw, 36px); }
.page-hero p { margin: 0; color: #c7d7ee; max-width: 620px; line-height: 1.65; }
.layout { display: grid; grid-template-columns: 1.6fr 0.8fr; gap: 20px; margin-top: -28px; }
.card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 30px rgba(15, 42, 82, 0.06); }
.form-card { padding: 24px 26px 26px; }
.tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 22px; }
.tab {
  display: flex; align-items: center; justify-content: center; gap: 8px; height: 46px;
  border: 1px solid var(--line); border-radius: 12px; background: #fbfdff; color: #475569;
  font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.16s ease;
}
.tab.active { border-color: #9dc3fa; background: #eef6ff; color: var(--blue); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08); }
.tab-icon {
  width: 26px; height: 26px; border-radius: 8px; display: grid; place-items: center;
  font-size: 12px; font-weight: 800;
}
.tab-icon.blue { background: #e8f1ff; color: #2563eb; }
.tab-icon.amber { background: #fff4dd; color: #c47a06; }
.tab-icon.green { background: #e6f8f0; color: #0f9d63; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 18px; }
.field { display: flex; flex-direction: column; gap: 7px; font-size: 13px; color: #334155; }
.field.block { margin-top: 16px; }
.field em { color: #ef4444; font-style: normal; }
.field input, .field select, .field textarea {
  border: 1px solid #dbe5f2; border-radius: 10px; padding: 0 12px; font-size: 14px;
  background: #fbfdff; color: var(--ink); outline: none; font-family: inherit;
}
.field input, .field select { height: 44px; }
.field textarea { padding: 10px 12px; resize: vertical; }
.field input:focus, .field select:focus, .field textarea:focus {
  border-color: #7db0f6; background: #fff; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.09);
}
.form-card .btn.block { margin-top: 18px; }
.error { background: #fff1f2; color: #be123c; border-radius: 10px; padding: 10px 14px; font-size: 13px; }
.side { display: flex; flex-direction: column; gap: 16px; }
.side-card { padding: 20px; }
.side-card h3 { margin: 0 0 14px; font-size: 15px; color: var(--navy); }
.steps { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 16px; }
.steps li { display: flex; gap: 12px; }
.steps b {
  width: 26px; height: 26px; border-radius: 50%; background: var(--sky); color: var(--blue);
  display: grid; place-items: center; font-size: 12px; flex-shrink: 0;
}
.steps p { margin: 3px 0 0; font-size: 12px; color: var(--muted); line-height: 1.55; }
.steps strong { font-size: 13px; }
.notice p { font-size: 13px; color: var(--muted); line-height: 1.7; margin: 0 0 8px; }
.duty { color: var(--blue) !important; font-weight: 600; }
.success { text-align: center; padding: 10px 4px; }
.success-icon {
  width: 62px; height: 62px; border-radius: 50%; background: #e6f8f0; color: #0f9d63;
  display: grid; place-items: center; font-size: 30px; margin: 0 auto 14px;
}
.success h2 { margin: 0 0 6px; color: var(--navy); }
.result { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; text-align: left; margin: 22px 0 0; }
.result > div { background: #f8fbff; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; }
.result dt { color: var(--muted); font-size: 11px; }
.result dd { margin: 4px 0 0; font-weight: 600; font-size: 14px; word-break: break-all; }
.success-actions { display: flex; gap: 10px; justify-content: center; margin-top: 18px; flex-wrap: wrap; }
.query-tip { margin: 14px 0 0; font-size: 12px; color: #94a3b8; }
.query-wrap { margin-top: 26px; }
.query-card { padding: 22px 24px; }
.query-head h2 { margin: 0; font-size: 20px; color: var(--navy); }
.query-head p { margin: 5px 0 16px; font-size: 13px; }
.query-form { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr auto; gap: 12px; align-items: end; }
.query-form .btn { height: 44px; }
.result-card { margin-top: 18px; border-top: 1px solid var(--line); padding-top: 16px; }
.result-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.result-head h3 { margin: 0; font-size: 17px; color: var(--navy); }
.result-head p { margin: 4px 0 0; font-size: 12px; }
.status { border-radius: 999px; padding: 5px 12px; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.status.ok { background: #e6f8f0; color: #0f9d63; }
.status.wait { background: #fff4dd; color: #c47a06; }
.reply { margin: 14px 0 0; background: #f8fbff; border: 1px solid var(--line); border-radius: 10px; padding: 12px; font-size: 13px; line-height: 1.7; color: #334155; }
@media (max-width: 980px) {
  .layout { grid-template-columns: 1fr; }
  .query-form { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 620px) {
  .form-grid, .tabs, .result, .query-form { grid-template-columns: 1fr; }
  .form-card, .query-card { padding: 18px; }
}
</style>
