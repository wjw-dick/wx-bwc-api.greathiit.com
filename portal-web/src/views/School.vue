<template>
  <div class="page">
    <section class="page-hero">
      <div class="container">
        <router-link :to="base" class="crumb">← {{ t('common.back') }}</router-link>
        <h1>{{ store.state.tenant?.name }}</h1>
        <p>{{ store.state.tenant?.intro || t('school.intro') }}</p>
      </div>
    </section>

    <div class="container layout">
      <section class="card main-card">
        <h2>{{ t('school.intro') }}</h2>
        <p class="muted">
          {{ locale.startsWith('en')
            ? 'The campus security system covers access control, visitor management, vehicle recognition, fire safety, patrol, emergency response and safety education. It is designed to keep every member of the campus community safe.'
            : '学校安全体系覆盖门禁访客、车辆识别、消防管理、巡逻防控、应急联动与安全教育等业务，形成“人防、物防、技防”一体化防护网络，守护校园每一处角落。' }}
        </p>
        <div class="pillars">
          <div v-for="item in pillars" :key="item.title" class="pillar">
            <span class="icon">{{ item.icon }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <aside class="side">
        <div class="card side-card">
          <h3>{{ t('school.contact') }}</h3>
          <div class="contact-row">
            <span>{{ t('school.duty') }}</span>
            <strong>{{ store.state.tenant?.dutyPhone || '-' }}</strong>
          </div>
          <div class="contact-row">
            <span>{{ locale.startsWith('en') ? 'School code' : '学校编码' }}</span>
            <strong>{{ store.state.tenant?.code || '-' }}</strong>
          </div>
          <div class="contact-row">
            <span>{{ locale.startsWith('en') ? 'Status' : '运行状态' }}</span>
            <strong class="ok">{{ locale.startsWith('en') ? 'Normal' : '正常' }}</strong>
          </div>
        </div>
        <div class="card side-card">
          <h3>{{ locale.startsWith('en') ? 'Quick access' : '快捷入口' }}</h3>
          <router-link :to="`${base}/visitor`" class="quick">{{ t('nav.visitor') }} →</router-link>
          <router-link :to="`${base}/lost`" class="quick">{{ t('nav.lost') }} →</router-link>
          <router-link :to="`${base}/news`" class="quick">{{ t('nav.news') }} →</router-link>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePortalStore } from '@/store/portal'
import { useSeo } from '@/composables/useSeo'

const route = useRoute()
const { t, locale } = useI18n()
const store = usePortalStore()
const base = computed(() => `/${route.params.code || 'DEMO'}`)

const pillars = computed(() => [
  { icon: '门', title: locale.value.startsWith('en') ? 'Access & visitors' : '门禁访客', desc: locale.value.startsWith('en') ? 'Online booking, pass codes and blacklist control.' : '预约审批、通行码核验与黑名单拦截。' },
  { icon: '车', title: locale.value.startsWith('en') ? 'Vehicle recognition' : '车辆识别', desc: locale.value.startsWith('en') ? 'Plate recognition, fixed and temporary passes.' : '车牌识别对接、固定与临时车牌下发。' },
  { icon: '消', title: locale.value.startsWith('en') ? 'Fire safety' : '消防管理', desc: locale.value.startsWith('en') ? 'One-code-per-asset inspection and hazard closure.' : '一物一码巡检、隐患整改闭环。' },
  { icon: '巡', title: locale.value.startsWith('en') ? 'Patrol & response' : '巡逻应急', desc: locale.value.startsWith('en') ? 'QR/GPS check-in, incident dispatch and emergency plans.' : '扫码/GPS 打卡、事件工单与应急预案。' }
])

useSeo({
  title: () => store.state.tenant?.name || t('school.title'),
  description: () => store.state.tenant?.intro || t('school.intro'),
  path: () => route.path
})
</script>

<style scoped>
.page-hero { background: linear-gradient(145deg, #0b2547, #123a6d); color: #fff; padding: 46px 0 66px; }
.crumb { color: #a9c6ee; font-size: 13px; }
.page-hero h1 { margin: 16px 0 8px; font-size: clamp(28px, 4vw, 40px); }
.page-hero p { margin: 0; color: #c7d7ee; max-width: 680px; line-height: 1.7; }
.layout { display: grid; grid-template-columns: 1.7fr 0.8fr; gap: 18px; margin-top: -30px; position: relative; z-index: 2; }
.card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 30px rgba(15, 42, 82, 0.06); }
.main-card { padding: 28px; }
.main-card h2 { margin: 0 0 10px; color: var(--navy); font-size: 20px; }
.main-card > p { line-height: 1.85; font-size: 14px; }
.pillars { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 22px; }
.pillar { display: flex; gap: 12px; background: #f8fbff; border: 1px solid var(--line); border-radius: 12px; padding: 16px; }
.icon {
  width: 38px; height: 38px; border-radius: 10px; background: var(--sky); color: var(--blue);
  display: grid; place-items: center; font-weight: 800; flex-shrink: 0;
}
.pillar strong { font-size: 14px; }
.pillar p { margin: 4px 0 0; font-size: 12px; color: var(--muted); line-height: 1.6; }
.side { display: flex; flex-direction: column; gap: 16px; }
.side-card { padding: 20px; }
.side-card h3 { margin: 0 0 14px; font-size: 15px; color: var(--navy); }
.contact-row { display: flex; justify-content: space-between; gap: 12px; padding: 9px 0; border-bottom: 1px dashed #eef3fa; font-size: 13px; }
.contact-row span { color: var(--muted); }
.contact-row .ok { color: #0f9d63; }
.quick { display: block; padding: 11px 0; border-bottom: 1px dashed #eef3fa; font-size: 14px; color: #334155; }
.quick:hover { color: var(--blue); }
@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } .pillars { grid-template-columns: 1fr; } }
</style>
