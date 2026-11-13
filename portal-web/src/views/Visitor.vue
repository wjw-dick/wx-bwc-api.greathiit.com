<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="base" class="crumb">← {{ t('common.back') }}</router-link>
        <h1>{{ t('visitor.title') }}</h1>
        <p>{{ t('visitor.subtitle') }}</p>
      </div>
    </section>

    <div class="container layout">
      <section class="card form-card">
        <div v-if="success" class="success">
          <div class="success-icon">✓</div>
          <h2>{{ t('visitor.success') }}</h2>
          <p class="muted">{{ t('visitor.successTip') }}</p>
          <dl class="result">
            <div><dt>{{ t('visitor.bookingNo') }}</dt><dd>#{{ result.id }}</dd></div>
            <div><dt>{{ t('visitor.name') }}</dt><dd>{{ result.visitorName }}</dd></div>
            <div><dt>{{ t('visitor.phone') }}</dt><dd>{{ result.phoneMask || '-' }}</dd></div>
            <div><dt>{{ t('visitor.idCard') }}</dt><dd>{{ result.idCardMask || '-' }}</dd></div>
            <div><dt>{{ t('visitor.visitTime') }}</dt><dd>{{ result.visitTime }}</dd></div>
            <div><dt>{{ t('visitor.status') }}</dt><dd>{{ statusLabel(result.status) }}</dd></div>
          </dl>
          <p class="mask-note">{{ t('visitor.maskNote') }}</p>
          <p class="mask-note">{{ t('visitor.bookingNoTip') }}</p>
          <div class="success-actions">
            <router-link :to="queryLink" class="btn primary">{{ t('visitor.queryNow') }}</router-link>
            <button class="btn ghost" @click="reset">{{ t('visitor.again') }}</button>
            <router-link :to="base" class="btn ghost">{{ t('nav.home') }}</router-link>
          </div>
        </div>

        <form v-else @submit.prevent="submit">
          <div class="form-grid">
            <label class="field">
              <span>{{ t('visitor.name') }} <em>*</em></span>
              <input v-model.trim="form.visitorName" maxlength="30" required :placeholder="t('visitor.name')" />
            </label>
            <label class="field">
              <span>{{ t('visitor.phone') }} <em>*</em></span>
              <input v-model.trim="form.phone" inputmode="tel" maxlength="11" required :placeholder="locale.startsWith('en') ? '11-digit mobile' : '11 位手机号'" />
            </label>
            <label class="field">
              <span>{{ t('visitor.idCard') }}</span>
              <input v-model.trim="form.idCard" maxlength="18" :placeholder="locale.startsWith('en') ? '18-digit ID (optional)' : '18 位身份证号（选填）'" />
              <small class="hint">{{ t('visitor.idCardHint') }}<template v-if="idCardMask"> · {{ idCardMask }}</template></small>
            </label>
            <label class="field">
              <span>{{ t('visitor.visitee') }}</span>
              <select v-model="form.visitUserId">
                <option value="">{{ t('visitor.visiteeNone') }}</option>
                <option v-for="person in visitees" :key="person.id" :value="person.id">
                  {{ person.name }}{{ person.dept ? ' · ' + person.dept : '' }}
                </option>
              </select>
              <small class="hint">{{ t('visitor.visiteeHint') }}</small>
            </label>
            <label class="field">
              <span>{{ t('visitor.reason') }} <em>*</em></span>
              <select v-model="form.reason" required>
                <option value="" disabled>{{ locale.startsWith('en') ? 'Select a purpose' : '请选择来访事由' }}</option>
                <option v-for="item in reasons" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label class="field">
              <span>{{ t('visitor.accompany') }}</span>
              <input v-model.number="form.accompanyCount" type="number" min="0" max="20" />
            </label>
            <label class="field">
              <span>{{ t('visitor.visitTime') }} <em>*</em></span>
              <input v-model="form.visitTime" type="datetime-local" required />
            </label>
            <label class="field">
              <span>{{ t('visitor.leaveTime') }}</span>
              <input v-model="form.leaveTime" type="datetime-local" />
            </label>
            <label class="field">
              <span>{{ t('visitor.plate') }}</span>
              <input v-model.trim="form.plateNo" maxlength="10" placeholder="京A12345" />
            </label>
            <label class="field">
              <span>{{ t('visitor.remark') }}</span>
              <input v-model.trim="form.remark" maxlength="200" :placeholder="locale.startsWith('en') ? 'Optional' : '选填'" />
            </label>
          </div>

          <p v-if="error" class="error">{{ error }}</p>
          <button class="btn primary block" :disabled="submitting" type="submit">
            {{ submitting ? t('common.loading') : t('visitor.submit') }}
          </button>
        </form>
      </section>

      <aside class="side">
        <div class="card side-card">
          <h3>{{ locale.startsWith('en') ? 'Visit steps' : '预约流程' }}</h3>
          <ol class="steps">
            <li><b>1</b><div><strong>{{ locale.startsWith('en') ? 'Submit online' : '在线提交' }}</strong><p>{{ locale.startsWith('en') ? 'Fill in visitor and host information.' : '填写访客信息与被访人。' }}</p></div></li>
            <li><b>2</b><div><strong>{{ locale.startsWith('en') ? 'Host approval' : '被访人审批' }}</strong><p>{{ locale.startsWith('en') ? 'The host reviews and approves.' : '被访人收到提醒并审批。' }}</p></div></li>
            <li><b>3</b><div><strong>{{ locale.startsWith('en') ? 'Pass code' : '凭码入校' }}</strong><p>{{ locale.startsWith('en') ? 'Enter with the pass code in your time slot.' : '在预约时段凭通行码入校。' }}</p></div></li>
          </ol>
        </div>
        <div class="card side-card notice">
          <h3>{{ locale.startsWith('en') ? 'Notice' : '温馨提示' }}</h3>
          <p>{{ locale.startsWith('en') ? 'ID numbers are encrypted at rest and only masked values are shown. Please arrive within your booked time.' : '身份证号加密存储、页面仅展示脱敏信息；请按预约时段到校，配合门卫核验。' }}</p>
          <p v-if="store.state.tenant?.dutyPhone" class="duty">{{ t('common.contact') }}：{{ store.state.tenant.dutyPhone }}</p>
          <router-link :to="`${base}/visitor-query`" class="query-link">{{ t('visitor.query') }} →</router-link>
        </div>
      </aside>
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
const success = ref(false)
const error = ref('')
const result = ref<any>({})
const visitees = ref<any[]>([])
const form = reactive<any>({
  visitorName: '', phone: '', idCard: '', reason: '', visitTime: '', leaveTime: '',
  accompanyCount: 0, plateNo: '', visitUserId: '', remark: ''
})
const queryLink = computed(() => `/${route.params.code || 'DEMO'}/visitor-query?id=${result.value.id || ''}`)
const selectedHost = computed(() => visitees.value.find((person) => person.id === form.visitUserId))

const reasons = computed(() => locale.value.startsWith('en')
  ? ['Business visit', 'Parent visit', 'Academic exchange', 'Interview', 'Delivery', 'Other']
  : ['公务来访', '家长来访', '学术交流', '面试应聘', '物流配送', '其他'])

const idCardMask = computed(() => {
  const value = String(form.idCard || '')
  if (value.length < 8) return ''
  return `${value.slice(0, 3)}${'*'.repeat(Math.max(0, value.length - 7))}${value.slice(-4)}`
})

useSeo({
  title: () => t('visitor.title'),
  description: () => t('visitor.subtitle'),
  path: () => route.path
})

function mask(value: string) {
  if (!value) return ''
  if (value.length <= 7) return '*'.repeat(value.length)
  return `${value.slice(0, 3)}${'*'.repeat(value.length - 7)}${value.slice(-4)}`
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
    EXPIRED: locale.value.startsWith('en') ? 'Expired' : '已过期',
    CANCELLED: locale.value.startsWith('en') ? 'Cancelled' : '已取消'
  }
  return map[status] || status
}

async function submit() {
  error.value = ''
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    error.value = t('visitor.invalidPhone')
    return
  }
  if (form.idCard && !/^\d{17}[\dXx]$/.test(form.idCard)) {
    error.value = t('visitor.invalidIdCard')
    return
  }
  submitting.value = true
  try {
    const payload = {
      visitorName: form.visitorName,
      phone: form.phone,
      idCard: form.idCard || undefined,
      reason: form.reason,
      visitTime: toBackendTime(form.visitTime),
      leaveTime: form.leaveTime ? toBackendTime(form.leaveTime) : undefined,
      accompanyCount: Number(form.accompanyCount || 0),
      plateNo: form.plateNo || undefined,
      visitUserId: form.visitUserId || undefined,
      visitUserName: selectedHost.value?.name || undefined,
      remark: form.remark || undefined,
      source: 'H5'
    }
    const data: any = await api.post(`/tenant/${route.params.code}/visitor-appointment`, payload)
    result.value = {
      ...data,
      phoneMask: data.phoneMask || mask(form.phone),
      idCardMask: data.idCardMask || mask(form.idCard)
    }
    success.value = true
  } catch (e: any) {
    error.value = e.message || t('error.load')
  } finally {
    submitting.value = false
  }
}

function reset() {
  success.value = false
  result.value = {}
  Object.assign(form, {
    visitorName: '', phone: '', idCard: '', reason: '', visitTime: '', leaveTime: '',
    accompanyCount: 0, plateNo: '', visitUserId: '', remark: ''
  })
}

async function loadVisitees() {
  try {
    visitees.value = (await api.get(`/tenant/${route.params.code}/visitees`)) as any
  } catch (e) {
    visitees.value = []
  }
}

onMounted(loadVisitees)
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 40px 0 56px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(26px, 4vw, 36px); }
.page-hero p { margin: 0; color: #c7d7ee; }
.layout { display: grid; grid-template-columns: 1.6fr 0.8fr; gap: 20px; margin-top: -28px; }
.card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 30px rgba(15, 42, 82, 0.06); }
.form-card { padding: 26px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 18px; }
.field { display: flex; flex-direction: column; gap: 7px; font-size: 13px; color: #334155; }
.field em { color: #ef4444; font-style: normal; }
.field input, .field select {
  height: 44px; border: 1px solid #dbe5f2; border-radius: 10px; padding: 0 12px; font-size: 14px;
  background: #fbfdff; color: var(--ink); outline: none; transition: border 0.15s ease;
  font-family: inherit;
}
.field input:focus, .field select:focus { border-color: #7db0f6; background: #fff; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.09); }
.hint { color: #94a3b8; font-size: 11px; line-height: 1.45; }
.error { background: #fff1f2; color: #be123c; border-radius: 10px; padding: 10px 14px; font-size: 13px; }
.form-card .btn { margin-top: 18px; }
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
.query-link { display: inline-block; margin-top: 6px; color: var(--blue); font-size: 13px; font-weight: 600; }
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
.mask-note { font-size: 11px; color: #94a3b8; margin: 12px 0 0; }
.success-actions { display: flex; gap: 10px; justify-content: center; margin-top: 18px; flex-wrap: wrap; }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .form-card { padding: 20px; }
}
@media (max-width: 560px) {
  .form-grid, .result { grid-template-columns: 1fr; }
}
</style>
