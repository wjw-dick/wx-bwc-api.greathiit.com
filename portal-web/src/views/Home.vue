<template>
  <div>
    <section class="hero">
      <div class="container hero-grid">
        <div class="hero-copy">
          <span class="badge">{{ t('home.badge') }}</span>
          <h1>{{ store.state.tenant?.name || t('nav.home') }}</h1>
          <p class="lead">{{ store.state.tenant?.intro || t('home.subtitle') }}</p>
          <div class="hero-actions">
            <router-link :to="`${base}/visitor`" class="btn primary">{{ t('home.visitorCta') }} →</router-link>
            <router-link :to="`${base}/lost`" class="btn ghost">{{ t('home.lostCta') }}</router-link>
          </div>
          <div class="hero-meta">
            <div><strong>{{ notices.length }}</strong><span>{{ locale.startsWith('en') ? 'News' : '安全资讯' }}</span></div>
            <div><strong>{{ lostItems.length }}</strong><span>{{ locale.startsWith('en') ? 'Lost items' : '待认领物品' }}</span></div>
            <div><strong>24h</strong><span>{{ locale.startsWith('en') ? 'Duty' : '值班守护' }}</span></div>
          </div>
        </div>
        <div class="hero-visual">
          <div class="shield">
            <div class="shield-inner">
              <span class="shield-title">{{ locale.startsWith('en') ? 'SAFE' : '平安' }}</span>
              <span class="shield-sub">CAMPUS</span>
            </div>
          </div>
          <div class="float-card card-a">
            <span class="dot green" />
            {{ locale.startsWith('en') ? 'Gate access online' : '门禁通行正常' }}
          </div>
          <div class="float-card card-b">
            <span class="dot amber" />
            {{ locale.startsWith('en') ? '24/7 patrol' : '24 小时巡逻' }}
          </div>
        </div>
      </div>
    </section>

    <section class="container quick-grid">
      <router-link v-for="item in quickLinks" :key="item.path" :to="item.path" class="quick-card">
        <span class="quick-icon" :class="item.tone">{{ item.icon }}</span>
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
        </div>
        <span class="arrow">→</span>
      </router-link>
    </section>

    <section class="container section">
      <div class="section-head">
        <div>
          <h2>{{ t('home.notices') }}</h2>
          <p class="muted">{{ t('news.subtitle') }}</p>
        </div>
        <router-link :to="`${base}/news`" class="more">{{ t('common.more') }} →</router-link>
      </div>
      <div v-if="loading" class="empty">{{ t('common.loading') }}</div>
      <div v-else-if="!notices.length" class="empty">{{ t('home.empty') }}</div>
      <div v-else class="notice-grid">
        <router-link v-for="item in notices.slice(0, 6)" :key="item.id" :to="`${base}/news/${item.id}`" class="notice-card">
          <span class="tag">{{ categoryLabel(item.category) }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ strip(item.content).slice(0, 90) }}</p>
          <footer>
            <span>{{ formatDate(item.publishTime) }}</span>
            <span>{{ item.views || 0 }} {{ t('news.views') }}</span>
          </footer>
        </router-link>
      </div>
    </section>

    <section v-if="warnings.length" class="container section">
      <div class="section-head">
        <h2>{{ t('home.warnings') }}</h2>
      </div>
      <div class="warning-list">
        <div v-for="item in warnings.slice(0, 3)" :key="item.id" class="warning-item">
          <span class="warn-icon">!</span>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ strip(item.content).slice(0, 130) }}</p>
          </div>
          <span class="muted">{{ formatDate(item.publishTime) }}</span>
        </div>
      </div>
    </section>

    <section class="container section">
      <div class="section-head">
        <div>
          <h2>{{ t('home.lost') }}</h2>
          <p class="muted">{{ t('lost.subtitle') }}</p>
        </div>
        <router-link :to="`${base}/lost`" class="more">{{ t('common.more') }} →</router-link>
      </div>
      <div v-if="!lostItems.length" class="empty">{{ t('lost.empty') }}</div>
      <div v-else class="lost-grid">
        <router-link v-for="item in lostItems.slice(0, 4)" :key="item.id" :to="`${base}/lost`" class="lost-card">
          <div class="lost-photo" :style="photoStyle(item.photos)">
            <span v-if="!item.photos">{{ item.name?.slice(0, 1) }}</span>
          </div>
          <div class="lost-body">
            <h3>{{ item.name }}</h3>
            <p class="muted">{{ item.location || '-' }}</p>
            <span class="tag">{{ categoryLabel(item.category) }}</span>
          </div>
        </router-link>
      </div>
    </section>
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
const notices = ref<any[]>([])
const warnings = ref<any[]>([])
const lostItems = ref<any[]>([])
const loading = ref(true)

const quickLinks = computed(() => [
  {
    path: `${base.value}/visitor`,
    title: t('nav.visitor'),
    desc: locale.value.startsWith('en') ? 'Submit a visit request online' : '在线提交来访申请，审批后凭码入校',
    icon: '访',
    tone: 'blue'
  },
  {
    path: `${base.value}/lost`,
    title: t('nav.lost'),
    desc: locale.value.startsWith('en') ? 'Browse and register found items' : '浏览待认领物品，登记拾获信息',
    icon: '失',
    tone: 'amber'
  },
  {
    path: `${base.value}/news`,
    title: t('nav.news'),
    desc: locale.value.startsWith('en') ? 'Safety news and prevention tips' : '安全动态、通知公告与防范提示',
    icon: '讯',
    tone: 'green'
  },
  {
    path: `${base.value}/school`,
    title: t('nav.school'),
    desc: locale.value.startsWith('en') ? 'School safety system and contacts' : '学校安全体系介绍与联系方式',
    icon: '校',
    tone: 'navy'
  }
])

useSeo({
  title: () => store.state.tenant?.name || '校园安全门户',
  description: () => store.state.tenant?.intro || t('home.subtitle'),
  path: () => route.path
})

async function load() {
  loading.value = true
  try {
    const code = route.params.code
    const [n, w, l] = await Promise.all([
      api.get(`/tenant/${code}/notices`, { params: { limit: 12 } }),
      api.get(`/tenant/${code}/warnings`),
      api.get(`/tenant/${code}/lost`)
    ]) as any[]
    notices.value = n || []
    warnings.value = w || []
    lostItems.value = l || []
  } catch (e) {
    notices.value = []
  } finally {
    loading.value = false
  }
}

function strip(html: string) {
  return String(html || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
}

function formatDate(value: string) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function categoryLabel(category: string) {
  const map: Record<string, string> = {
    ELECTRONICS: locale.value.startsWith('en') ? 'Electronics' : '电子产品',
    CARD: locale.value.startsWith('en') ? 'Card' : '证件卡类',
    CLOTHING: locale.value.startsWith('en') ? 'Clothing' : '衣物',
    BOOK: locale.value.startsWith('en') ? 'Books' : '书籍',
    OTHER: locale.value.startsWith('en') ? 'Other' : '其他',
    NOTICE: locale.value.startsWith('en') ? 'Notice' : '通知',
    SAFETY: locale.value.startsWith('en') ? 'Safety' : '安全提示'
  }
  return map[category] || category || (locale.value.startsWith('en') ? 'Other' : '其他')
}

function photoStyle(photos: string) {
  if (!photos) return {}
  const first = String(photos).split(',')[0]
  return { backgroundImage: `url(${first})`, backgroundSize: 'cover', backgroundPosition: 'center' }
}

onMounted(async () => {
  await store.load(String(route.params.code || 'DEMO'))
  await load()
})
</script>

<style scoped>
.hero {
  background:
    radial-gradient(900px 400px at 85% -10%, rgba(59, 130, 246, 0.22), transparent 60%),
    linear-gradient(145deg, #0b2547 0%, #0f2a52 45%, #123a6d 100%);
  color: #fff; padding: 58px 0 70px; overflow: hidden;
}
.hero-grid { display: grid; grid-template-columns: 1.15fr 0.85fr; gap: 40px; align-items: center; }
.badge {
  display: inline-block; padding: 6px 14px; border-radius: 999px; font-size: 12px;
  background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.18);
  letter-spacing: 0.5px; margin-bottom: 18px;
}
.hero h1 { font-size: clamp(30px, 4.2vw, 48px); line-height: 1.15; margin: 0 0 16px; letter-spacing: -0.5px; }
.lead { color: #c7d7ee; font-size: 16px; line-height: 1.7; max-width: 560px; margin: 0 0 26px; }
.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.hero .btn.ghost { background: rgba(255, 255, 255, 0.08); color: #fff; border-color: rgba(255, 255, 255, 0.22); }
.hero .btn.ghost:hover { background: rgba(255, 255, 255, 0.14); }
.hero-meta { display: flex; gap: 36px; margin-top: 34px; }
.hero-meta div { display: flex; flex-direction: column; gap: 2px; }
.hero-meta strong { font-size: 24px; color: #fff; }
.hero-meta span { font-size: 12px; color: #9fb8d8; }
.hero-visual { position: relative; display: grid; place-items: center; min-height: 300px; }
.shield {
  width: 230px; height: 260px; border-radius: 26px;
  background: linear-gradient(160deg, rgba(96, 165, 250, 0.25), rgba(30, 64, 175, 0.25));
  border: 1px solid rgba(147, 197, 253, 0.28);
  display: grid; place-items: center; backdrop-filter: blur(8px);
  box-shadow: 0 30px 80px rgba(2, 12, 30, 0.35);
  clip-path: polygon(50% 0%, 100% 16%, 100% 68%, 50% 100%, 0% 68%, 0% 16%);
}
.shield-inner { text-align: center; }
.shield-title { display: block; font-size: 44px; font-weight: 800; letter-spacing: 6px; }
.shield-sub { font-size: 12px; letter-spacing: 5px; color: #a9c6ee; }
.float-card {
  position: absolute; background: rgba(255, 255, 255, 0.95); color: #0f2a52;
  border-radius: 12px; padding: 10px 14px; font-size: 12px; font-weight: 600;
  display: flex; align-items: center; gap: 8px; box-shadow: 0 14px 30px rgba(3, 16, 38, 0.24);
}
.card-a { top: 18px; right: 0; }
.card-b { bottom: 24px; left: 0; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.green { background: #10b981; }
.dot.amber { background: #f59e0b; }

.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-top: -34px; position: relative; z-index: 2; }
.quick-card {
  background: #fff; border-radius: var(--radius); padding: 18px; display: flex; align-items: center; gap: 12px;
  border: 1px solid var(--line); box-shadow: 0 12px 26px rgba(15, 42, 82, 0.06); transition: all 0.18s ease;
}
.quick-card:hover { transform: translateY(-3px); box-shadow: 0 18px 36px rgba(15, 42, 82, 0.12); }
.quick-icon {
  width: 42px; height: 42px; border-radius: 12px; display: grid; place-items: center;
  font-weight: 800; font-size: 16px; flex-shrink: 0;
}
.quick-icon.blue { background: #e8f1ff; color: #2563eb; }
.quick-icon.amber { background: #fff4dd; color: #c47a06; }
.quick-icon.green { background: #e6f8f0; color: #0f9d63; }
.quick-icon.navy { background: #e8edf7; color: #0f2a52; }
.quick-card div { min-width: 0; }
.quick-card strong { font-size: 15px; }
.quick-card p { margin: 4px 0 0; font-size: 12px; color: var(--muted); line-height: 1.5; }
.arrow { margin-left: auto; color: #9db4d2; }

.section { margin-top: 46px; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.section-head h2 { font-size: 22px; margin: 0 0 4px; color: var(--navy); }
.section-head p { margin: 0; font-size: 13px; }
.more { color: var(--blue); font-size: 13px; font-weight: 600; }
.notice-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.notice-card {
  background: #fff; border: 1px solid var(--line); border-radius: var(--radius); padding: 18px;
  display: flex; flex-direction: column; gap: 10px; transition: all 0.18s ease;
}
.notice-card:hover { border-color: #bcd2f4; transform: translateY(-2px); }
.notice-card h3 { margin: 0; font-size: 16px; line-height: 1.45; color: var(--navy); }
.notice-card p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.65; flex: 1; }
.notice-card footer { display: flex; justify-content: space-between; color: #94a3b8; font-size: 12px; }
.tag {
  align-self: flex-start; background: var(--sky); color: var(--blue); border-radius: 999px;
  padding: 3px 10px; font-size: 11px; font-weight: 600;
}
.warning-list { display: flex; flex-direction: column; gap: 10px; }
.warning-item {
  background: #fff; border: 1px solid #ffe5c2; border-left: 4px solid #f59e0b; border-radius: 12px;
  padding: 14px 16px; display: flex; align-items: flex-start; gap: 12px;
}
.warning-item > div { flex: 1; }
.warning-item strong { color: #8a4b00; }
.warning-item p { margin: 4px 0 0; color: #7c5a1d; font-size: 13px; line-height: 1.6; }
.warn-icon {
  width: 24px; height: 24px; border-radius: 50%; background: #f59e0b; color: #fff;
  display: grid; place-items: center; font-weight: 800; flex-shrink: 0;
}
.lost-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.lost-card { background: #fff; border: 1px solid var(--line); border-radius: var(--radius); overflow: hidden; }
.lost-photo {
  height: 130px; background: linear-gradient(135deg, #e8f1ff, #dbeafe); display: grid; place-items: center;
  color: #2563eb; font-size: 32px; font-weight: 800;
}
.lost-body { padding: 14px; }
.lost-body h3 { margin: 0 0 4px; font-size: 15px; }
.lost-body p { margin: 0 0 10px; font-size: 12px; }
.empty { background: #fff; border: 1px dashed var(--line); border-radius: var(--radius); padding: 34px; text-align: center; color: var(--muted); }

@media (max-width: 980px) {
  .quick-grid, .lost-grid { grid-template-columns: repeat(2, 1fr); }
  .notice-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-grid { grid-template-columns: 1fr; }
  .hero-visual { display: none; }
}
@media (max-width: 600px) {
  .hero { padding: 42px 0 54px; }
  .quick-grid, .notice-grid, .lost-grid { grid-template-columns: 1fr; }
  .hero-meta { gap: 22px; }
  .warning-item > .muted { display: none; }
}
</style>
