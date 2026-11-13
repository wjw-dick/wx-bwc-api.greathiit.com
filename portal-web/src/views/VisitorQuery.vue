<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="base" class="crumb">← {{ t('common.back') }}</router-link>
        <h1>{{ t('visitor.queryTitle') }}</h1>
        <p>{{ t('visitor.querySubtitle') }}</p>
      </div>
    </section>

    <div class="container layout">
      <section class="card form-card">
        <form class="query-form" @submit.prevent="query">
          <label class="field">
            <span>{{ t('visitor.bookingNo') }}</span>
            <input v-model.trim="form.id" inputmode="numeric" maxlength="12" :placeholder="t('visitor.bookingNoHint')" />
          </label>
          <label class="field">
            <span>{{ t('visitor.phone') }}</span>
            <input v-model.trim="form.phone" inputmode="tel" maxlength="11" :placeholder="t('visitor.phoneHint')" />
          </label>
          <button class="btn primary" :disabled="loading" type="submit">
            {{ loading ? t('common.loading') : t('visitor.queryNow') }}
          </button>
        </form>
        <p v-if="error" class="error">{{ error }}</p>

        <div v-if="result" class="result-card">
          <div class="result-head">
            <div>
              <h2>{{ result.visitorName }}</h2>
              <p class="muted">{{ result.visitTime }} · {{ result.visitUserName || t('visitor.visiteeNone') }}</p>
            </div>
            <span class="status" :class="statusClass(result.status)">{{ statusLabel(result.status) }}</span>
          </div>

          <dl class="result">
            <div><dt>{{ t('visitor.bookingNo') }}</dt><dd>#{{ result.id }}</dd></div>
            <div><dt>{{ t('visitor.phone') }}</dt><dd>{{ result.phoneMask || '-' }}</dd></div>
            <div><dt>{{ t('visitor.idCard') }}</dt><dd>{{ result.idCardMask || '-' }}</dd></div>
            <div><dt>{{ t('visitor.leaveTime') }}</dt><dd>{{ result.leaveTime || '-' }}</dd></div>
          </dl>

          <div v-if="result.status === 'APPROVED' && result.passCode" class="pass">
            <div class="pass-code">
              <span>{{ t('visitor.passCode') }}</span>
              <strong>{{ result.passCode }}</strong>
              <small>{{ t('visitor.passExpire') }}：{{ result.passExpireTime || '-' }}</small>
            </div>
            <img v-if="result.qrBase64" :src="`data:image/png;base64,${result.qrBase64}`" class="pass-qr" :alt="result.passCode" />
          </div>
          <p v-else class="hint-box">{{ statusHint(result.status) }}</p>
          <p v-if="result.approveRemark" class="muted remark">{{ t('visitor.approveRemark') }}：{{ result.approveRemark }}</p>
        </div>
      </section>

      <aside class="side">
        <div class="card side-card">
          <h3>{{ t('visitor.querySteps') }}</h3>
          <ol class="steps">
            <li><b>1</b><div><strong>{{ t('visitor.stepBook') }}</strong><p>{{ t('visitor.stepBookTip') }}</p></div></li>
            <li><b>2</b><div><strong>{{ t('visitor.stepQuery') }}</strong><p>{{ t('visitor.stepQueryTip') }}</p></div></li>
            <li><b>3</b><div><strong>{{ t('visitor.stepEnter') }}</strong><p>{{ t('visitor.stepEnterTip') }}</p></div></li>
          </ol>
        </div>
        <div class="card side-card notice">
          <p>{{ t('visitor.queryNotice') }}</p>
          <p v-if="store.state.tenant?.dutyPhone" class="duty">{{ t('common.contact') }}：{{ store.state.tenant.dutyPhone }}</p>
          <router-link :to="`${base}/visitor`" class="query-link">{{ t('visitor.again') }} →</router-link>
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
const form = reactive({ id: String(route.query.id || ''), phone: String(route.query.phone || '') })
const result = ref<any>(null)
const loading = ref(false)
const error = ref('')

useSeo({
  title: () => t('visitor.queryTitle'),
  description: () => t('visitor.querySubtitle'),
  path: () => route.path
})

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: locale.value.startsWith('en') ? 'Pending approval' : '待审批',
    APPROVED: locale.value.startsWith('en') ? 'Approved' : '已通过',
    REJECTED: locale.value.startsWith('en') ? 'Rejected' : '已驳回',
    EXPIRED: locale.value.startsWith('en') ? 'Expired' : '已过期',
    CANCELLED: locale.value.startsWith('en') ? 'Cancelled' : '已取消'
  }
  return map[status] || status || '-'
}

function statusClass(status: string) {
  if (status === 'APPROVED') return 'ok'
  if (status === 'PENDING') return 'wait'
  return 'bad'
}

function statusHint(status: string) {
  if (status === 'PENDING') return t('visitor.hintPending')
  if (status === 'REJECTED') return t('visitor.hintRejected')
  if (status === 'EXPIRED') return t('visitor.hintExpired')
  if (status === 'CANCELLED') return t('visitor.hintCancelled')
  return t('visitor.hintNoPass')
}

async function query() {
  error.value = ''
  result.value = null
  if (!form.id || !/^1[3-9]\d{9}$/.test(form.phone)) {
    error.value = t('visitor.queryInvalid')
    return
  }
  loading.value = true
  try {
    result.value = await api.get(`/tenant/${route.params.code}/visitor-appointment/query`, {
      params: { id: form.id, phone: form.phone }
    })
  } catch (e: any) {
    error.value = e.message || t('error.load')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  if (form.id && form.phone) query()
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 40px 0 56px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(26px, 4vw, 36px); }
.page-hero p { margin: 0; color: #c7d7ee; }
.layout { display: grid; grid-template-columns: 1.6fr 0.8fr; gap: 20px; margin-top: -28px; }
.card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 30px rgba(15, 42, 82, 0.06); }
.form-card { padding: 26px; }
.query-form { display: grid; grid-template-columns: 1fr 1fr auto; gap: 14px; align-items: end; }
.field { display: flex; flex-direction: column; gap: 7px; font-size: 13px; color: #334155; }
.field input {
  height: 44px; border: 1px solid #dbe5f2; border-radius: 10px; padding: 0 12px; font-size: 14px;
  background: #fbfdff; color: var(--ink); outline: none; font-family: inherit;
}
.field input:focus { border-color: #7db0f6; background: #fff; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.09); }
.query-form .btn { height: 44px; }
.error { background: #fff1f2; color: #be123c; border-radius: 10px; padding: 10px 14px; font-size: 13px; margin: 14px 0 0; }
.result-card { margin-top: 22px; border-top: 1px solid var(--line); padding-top: 20px; }
.result-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.result-head h2 { margin: 0; font-size: 20px; color: var(--navy); }
.result-head p { margin: 5px 0 0; font-size: 13px; }
.status { border-radius: 999px; padding: 5px 12px; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.status.ok { background: #e6f8f0; color: #0f9d63; }
.status.wait { background: #fff4dd; color: #c47a06; }
.status.bad { background: #fff1f2; color: #be123c; }
.result { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 18px 0 0; }
.result > div { background: #f8fbff; border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px; }
.result dt { color: var(--muted); font-size: 11px; }
.result dd { margin: 4px 0 0; font-weight: 600; font-size: 14px; word-break: break-all; }
.pass {
  margin-top: 18px; display: flex; align-items: center; gap: 22px; padding: 18px;
  background: linear-gradient(135deg, #eef6ff, #f8fbff); border: 1px solid #d6e8ff; border-radius: 14px;
}
.pass-code { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 0; }
.pass-code span { color: var(--muted); font-size: 12px; }
.pass-code strong { font-size: 26px; letter-spacing: 2px; color: var(--blue); word-break: break-all; }
.pass-code small { color: #8496ab; font-size: 12px; }
.pass-qr { width: 150px; height: 150px; border-radius: 10px; background: #fff; padding: 6px; border: 1px solid var(--line); }
.hint-box { margin-top: 16px; background: #f8fbff; border: 1px dashed var(--line); border-radius: 12px; padding: 14px; color: var(--muted); font-size: 13px; line-height: 1.7; }
.remark { margin-top: 12px; font-size: 13px; }
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
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .form-card { padding: 20px; }
  .query-form { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .result { grid-template-columns: 1fr; }
  .pass { flex-direction: column; align-items: flex-start; }
}
</style>
