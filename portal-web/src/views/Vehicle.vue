<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="base" class="crumb">← {{ t('common.back') }}</router-link>
        <h1>{{ t('vehicle.title') }}</h1>
        <p>{{ t('vehicle.subtitle') }}</p>
      </div>
    </section>

    <div class="container layout">
      <section class="card form-card">
        <div v-if="success" class="success">
          <div class="success-icon">✓</div>
          <h2>{{ t('vehicle.success') }}</h2>
          <p class="muted">{{ success.message }}</p>
          <dl class="result">
            <div><dt>{{ t('vehicle.bookingNo') }}</dt><dd>#{{ success.id }}</dd></div>
            <div><dt>{{ t('vehicle.plate') }}</dt><dd>{{ success.plateNo }}</dd></div>
            <div><dt>{{ t('vehicle.phone') }}</dt><dd>{{ success.phoneMask }}</dd></div>
            <div><dt>{{ t('vehicle.visitTime') }}</dt><dd>{{ success.visitTime }}</dd></div>
          </dl>
          <p class="mask-note">{{ t('vehicle.bookingNoTip') }}</p>
          <div class="success-actions">
            <button class="btn primary" @click="scrollToQuery">{{ t('vehicle.queryNow') }}</button>
            <button class="btn ghost" @click="reset">{{ t('vehicle.again') }}</button>
            <router-link :to="base" class="btn ghost">{{ t('nav.home') }}</router-link>
          </div>
        </div>

        <form v-else @submit.prevent="submit">
          <div class="form-grid">
            <label class="field">
              <span>{{ t('vehicle.plate') }} <em>*</em></span>
              <input v-model.trim="form.plateNo" maxlength="10" required placeholder="京A12345" @input="upperPlate" />
            </label>
            <label class="field">
              <span>{{ t('vehicle.driver') }} <em>*</em></span>
              <input v-model.trim="form.visitorName" maxlength="30" required :placeholder="t('vehicle.driverHint')" />
            </label>
            <label class="field">
              <span>{{ t('vehicle.phone') }} <em>*</em></span>
              <input v-model.trim="form.phone" inputmode="tel" maxlength="11" required :placeholder="t('vehicle.phoneHint')" />
            </label>
            <label class="field">
              <span>{{ t('vehicle.reason') }}</span>
              <select v-model="form.reason">
                <option v-for="item in reasons" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label class="field">
              <span>{{ t('vehicle.visitTime') }} <em>*</em></span>
              <input v-model="form.visitTime" type="datetime-local" required />
            </label>
            <label class="field">
              <span>{{ t('vehicle.leaveTime') }}</span>
              <input v-model="form.leaveTime" type="datetime-local" />
            </label>
            <label class="field span-2">
              <span>{{ t('vehicle.remark') }}</span>
              <input v-model.trim="form.remark" maxlength="200" :placeholder="t('vehicle.remarkHint')" />
            </label>
          </div>
          <p v-if="error" class="error">{{ error }}</p>
          <button class="btn primary block" :disabled="submitting" type="submit">
            {{ submitting ? t('common.loading') : t('vehicle.submit') }}
          </button>
        </form>
      </section>

      <aside class="side">
        <div class="card side-card">
          <h3>{{ t('vehicle.processTitle') }}</h3>
          <ol class="steps">
            <li><b>1</b><div><strong>{{ t('vehicle.stepBook') }}</strong><p>{{ t('vehicle.stepBookTip') }}</p></div></li>
            <li><b>2</b><div><strong>{{ t('vehicle.stepApprove') }}</strong><p>{{ t('vehicle.stepApproveTip') }}</p></div></li>
            <li><b>3</b><div><strong>{{ t('vehicle.stepEnter') }}</strong><p>{{ t('vehicle.stepEnterTip') }}</p></div></li>
          </ol>
        </div>
        <div class="card side-card notice">
          <p>{{ t('vehicle.notice') }}</p>
          <p v-if="store.state.tenant?.dutyPhone" class="duty">{{ t('common.contact') }}：{{ store.state.tenant.dutyPhone }}</p>
        </div>
      </aside>
    </div>

    <div ref="querySection" class="container query-wrap">
      <section class="card query-card">
        <h2>{{ t('vehicle.queryTitle') }}</h2>
        <p class="muted">{{ t('vehicle.querySubtitle') }}</p>
        <form class="query-form" @submit.prevent="query">
          <label class="field">
            <span>{{ t('vehicle.bookingNo') }}</span>
            <input v-model.trim="queryForm.id" inputmode="numeric" :placeholder="t('vehicle.bookingNoHint')" />
          </label>
          <label class="field">
            <span>{{ t('vehicle.phone') }}</span>
            <input v-model.trim="queryForm.phone" inputmode="tel" maxlength="11" :placeholder="t('vehicle.phoneHint')" />
          </label>
          <button class="btn primary" type="submit" :disabled="queryLoading">
            {{ queryLoading ? t('common.loading') : t('common.search') }}
          </button>
        </form>
        <p v-if="queryError" class="error">{{ queryError }}</p>
        <div v-if="queryResult" class="result-card">
          <div class="result-head">
            <div>
              <h3>{{ queryResult.plateNo }}</h3>
              <p class="muted">{{ queryResult.visitTime }} → {{ queryResult.leaveTime || '-' }}</p>
            </div>
            <span class="status" :class="queryResult.status === 'APPROVED' ? 'ok' : 'wait'">
              {{ statusLabel(queryResult.status) }}
            </span>
          </div>
          <p class="muted auth-line">{{ t('vehicle.authStatus') }}：{{ authLabel(queryResult.authStatus) }}</p>
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
const querySection = ref<HTMLElement | null>(null)
const error = ref('')
const queryLoading = ref(false)
const queryError = ref('')
const queryResult = ref<any>(null)
const form = reactive({
  plateNo: '', visitorName: '', phone: '', reason: '', visitTime: '', leaveTime: '', remark: ''
})
const queryForm = reactive({ id: '', phone: '' })

const reasons = computed(() => locale.value.startsWith('en')
  ? ['Official vehicle', 'Parent pickup', 'Delivery', 'Maintenance', 'Visitor vehicle', 'Other']
  : ['公务车辆', '家长接送', '物流配送', '施工维修', '访客车辆', '其他'])

useSeo({
  title: () => t('vehicle.title'),
  description: () => t('vehicle.subtitle'),
  path: () => route.path
})

function upperPlate() {
  form.plateNo = String(form.plateNo || '').replace(/\s/g, '').toUpperCase()
}

function toBackendTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ') + (value.length === 16 ? ':00' : '')
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: locale.value.startsWith('en') ? 'Pending approval' : '待审批',
    APPROVED: locale.value.startsWith('en') ? 'Approved' : '已通过',
    REJECTED: locale.value.startsWith('en') ? 'Rejected' : '已驳回',
    CANCELLED: locale.value.startsWith('en') ? 'Cancelled' : '已取消'
  }
  return map[status] || status || '-'
}

function authLabel(status: string) {
  const map: Record<string, string> = {
    NONE: locale.value.startsWith('en') ? 'Not issued' : '未下发',
    PENDING: locale.value.startsWith('en') ? 'Issuing' : '下发中',
    ACTIVE: locale.value.startsWith('en') ? 'Active' : '已生效',
    FAILED: locale.value.startsWith('en') ? 'Failed' : '下发失败'
  }
  return map[status] || status || '-'
}

async function submit() {
  error.value = ''
  const plate = form.plateNo.replace(/\s/g, '').toUpperCase()
  if (!/^[\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}$/.test(plate) && !/^[A-Z0-9]{6,8}$/.test(plate)) {
    error.value = t('vehicle.invalidPlate')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    error.value = t('vehicle.invalidPhone')
    return
  }
  if (!form.visitTime) {
    error.value = t('vehicle.invalidTime')
    return
  }
  submitting.value = true
  try {
    const data: any = await api.post(`/tenant/${route.params.code}/vehicle-appointment`, {
      plateNo: plate,
      visitorName: form.visitorName,
      phone: form.phone,
      reason: form.reason || undefined,
      visitTime: toBackendTime(form.visitTime),
      leaveTime: form.leaveTime ? toBackendTime(form.leaveTime) : undefined,
      remark: form.remark || undefined
    })
    success.value = data
  } catch (e: any) {
    error.value = e.message || t('error.load')
  } finally {
    submitting.value = false
  }
}

function reset() {
  success.value = null
  Object.assign(form, {
    plateNo: '', visitorName: '', phone: '', reason: '', visitTime: '', leaveTime: '', remark: ''
  })
}

function scrollToQuery() {
  queryForm.id = String(success.value?.id || '')
  queryForm.phone = form.phone
  queryError.value = ''
  queryResult.value = null
  querySection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function query() {
  queryError.value = ''
  queryResult.value = null
  if (!queryForm.id || !/^1[3-9]\d{9}$/.test(queryForm.phone)) {
    queryError.value = t('vehicle.queryInvalid')
    return
  }
  queryLoading.value = true
  try {
    queryResult.value = await api.get(`/tenant/${route.params.code}/vehicle-appointment/query`, {
      params: { id: queryForm.id, phone: queryForm.phone }
    })
  } catch (e: any) {
    queryError.value = e.message || t('error.load')
  } finally {
    queryLoading.value = false
  }
}

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  const now = new Date(Date.now() + 2 * 3600 * 1000)
  const pad = (n: number) => String(n).padStart(2, '0')
  form.visitTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:00`
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 40px 0 56px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(26px, 4vw, 36px); }
.page-hero p { margin: 0; color: #c7d7ee; max-width: 620px; line-height: 1.65; }
.layout { display: grid; grid-template-columns: 1.6fr 0.8fr; gap: 20px; margin-top: -28px; }
.card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 30px rgba(15, 42, 82, 0.06); }
.form-card { padding: 26px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 18px; }
.span-2 { grid-column: span 2; }
.field { display: flex; flex-direction: column; gap: 7px; font-size: 13px; color: #334155; }
.field em { color: #ef4444; font-style: normal; }
.field input, .field select {
  height: 44px; border: 1px solid #dbe5f2; border-radius: 10px; padding: 0 12px; font-size: 14px;
  background: #fbfdff; color: var(--ink); outline: none; font-family: inherit;
}
.field input:focus, .field select:focus { border-color: #7db0f6; background: #fff; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.09); }
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
.query-wrap { margin-top: 26px; }
.query-card { padding: 22px 24px; }
.query-card h2 { margin: 0; font-size: 20px; color: var(--navy); }
.query-card > p { margin: 5px 0 16px; font-size: 13px; }
.query-form { display: grid; grid-template-columns: 1fr 1fr auto; gap: 12px; align-items: end; }
.query-form .btn { height: 44px; }
.result-card { margin-top: 18px; border-top: 1px solid var(--line); padding-top: 16px; }
.result-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.result-head h3 { margin: 0; font-size: 17px; color: var(--navy); }
.result-head p { margin: 4px 0 0; font-size: 12px; }
.status { border-radius: 999px; padding: 5px 12px; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.status.ok { background: #e6f8f0; color: #0f9d63; }
.status.wait { background: #fff4dd; color: #c47a06; }
.auth-line { margin: 12px 0 0; font-size: 13px; }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .form-card { padding: 20px; }
  .query-form { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .form-grid, .result { grid-template-columns: 1fr; }
  .span-2 { grid-column: span 1; }
}
</style>
