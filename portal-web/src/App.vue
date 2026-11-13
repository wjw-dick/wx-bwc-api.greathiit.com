<template>
  <div class="portal">
    <header class="topbar" :class="{ scrolled }">
      <div class="container topbar-inner">
        <router-link :to="`/${code}`" class="brand">
          <img v-if="store.state.tenant?.logo" :src="store.state.tenant.logo" alt="logo" class="logo-img" />
          <span v-else class="logo-badge">安</span>
          <span class="brand-text">
            <strong>{{ store.tenantName }}</strong>
            <small>{{ locale.startsWith('en') ? 'Campus Security Portal' : '校园安全门户' }}</small>
          </span>
        </router-link>

        <nav class="desktop-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="top-actions">
          <button class="lang-btn" @click="toggleLocale">
            {{ locale.startsWith('en') ? '中文' : 'EN' }}
          </button>
          <button class="menu-btn" @click="drawer = !drawer" aria-label="menu">
            <span /><span /><span />
          </button>
        </div>
      </div>

      <transition name="slide">
        <div v-if="drawer" class="mobile-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="mobile-link"
            :class="{ active: isActive(item.path) }"
            @click="drawer = false"
          >
            {{ item.label }}
          </router-link>
        </div>
      </transition>
    </header>

    <main class="main">
      <div v-if="store.state.error" class="container error-box">
        <h2>{{ t('error.tenant') }}</h2>
        <p class="muted">{{ store.state.error }}</p>
        <router-link class="btn primary" to="/DEMO">{{ t('common.back') }}</router-link>
      </div>
      <router-view v-else :key="`${code}-${route.fullPath}`" />
    </main>

    <footer class="footer">
      <div class="container footer-inner">
        <div>
          <strong>{{ store.tenantName }}</strong>
          <p>{{ locale.startsWith('en') ? 'Campus Security · Safe Campus, Shared Responsibility' : '校园保卫 · 平安校园，人人有责' }}</p>
        </div>
        <div class="footer-meta">
          <span v-if="store.state.tenant?.dutyPhone">{{ t('common.contact') }}：{{ store.state.tenant.dutyPhone }}</span>
          <span>wx-bwc-api.greathiit.com</span>
          <span>© {{ year }} Campus Security</span>
        </div>
      </div>
    </footer>

    <transition name="fade">
      <router-link v-if="showFab" :to="`/${code}/visitor`" class="fab">
        {{ locale.startsWith('en') ? 'Book' : '预约' }}
      </router-link>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePortalStore, menuPath } from '@/store/portal'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const store = usePortalStore()
const drawer = ref(false)
const scrolled = ref(false)
const year = new Date().getFullYear()

const code = computed(() => String(route.params.code || 'DEMO'))
const showFab = computed(() => code.value && !route.path.includes('/visitor'))

const defaultNav = computed(() => [
  { path: `/${code.value}`, label: t('nav.home'), key: '/' },
  { path: `/${code.value}/visitor`, label: t('nav.visitor'), key: '/visitor' },
  { path: `/${code.value}/vehicle`, label: t('nav.vehicle'), key: '/vehicle' },
  { path: `/${code.value}/safety`, label: t('nav.safety'), key: '/safety' },
  { path: `/${code.value}/lost`, label: t('nav.lost'), key: '/lost' },
  { path: `/${code.value}/news`, label: t('nav.news'), key: '/news' },
  { path: `/${code.value}/school`, label: t('nav.school'), key: '/school' }
])

const navItems = computed(() => {
  const menus = store.state.menus || []
  if (!menus.length) return defaultNav.value
  const items = menus
    .filter((m) => m.path)
    .map((m) => ({
      path: menuPath(code.value, m.path),
      label: locale.value.startsWith('en') && m.nameEn ? m.nameEn : m.name,
      key: m.path
    }))
  // 菜单模板未升级时补齐公开门户固定入口，保证预约/上报链路可达
  for (const item of defaultNav.value) {
    if (!items.some((exist) => exist.key === item.key || exist.path === item.path)) {
      items.push(item)
    }
  }
  return items
})

function isActive(path: string) {
  if (path === `/${code.value}`) return route.path === path
  return route.path.startsWith(path)
}

function toggleLocale() {
  const next = locale.value.startsWith('en') ? 'zh-CN' : 'en-US'
  locale.value = next
  localStorage.setItem('bwc_portal_locale', next)
  router.replace({ query: { ...route.query, lang: next } })
}

async function sync() {
  await store.load(code.value)
}

watch(code, sync)

onMounted(() => {
  sync()
  window.addEventListener('scroll', () => { scrolled.value = window.scrollY > 12 })
})
</script>

<style>
:root {
  --navy: #0f2a52;
  --navy-deep: #081a33;
  --blue: #2563eb;
  --sky: #e8f1ff;
  --gold: #f5b942;
  --ink: #16233a;
  --muted: #64748b;
  --line: #e6edf6;
  --bg: #f6f9fe;
  --radius: 16px;
}
* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg);
  color: var(--ink);
  -webkit-font-smoothing: antialiased;
}
a { color: inherit; text-decoration: none; }
.container { width: min(1180px, 92vw); margin: 0 auto; }
.muted { color: var(--muted); }
.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 44px; padding: 0 22px; border-radius: 999px; border: 1px solid transparent;
  font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.18s ease;
}
.btn.primary { background: var(--blue); color: #fff; box-shadow: 0 10px 24px rgba(37, 99, 235, 0.24); }
.btn.primary:hover { transform: translateY(-1px); background: #1d4ed8; }
.btn.ghost { background: #fff; border-color: var(--line); color: var(--navy); }
.btn.ghost:hover { border-color: #bcd2f4; }
.btn.block { width: 100%; }

.topbar {
  position: sticky; top: 0; z-index: 50; background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px); border-bottom: 1px solid transparent; transition: all 0.2s ease;
}
.topbar.scrolled { border-color: var(--line); box-shadow: 0 8px 24px rgba(15, 42, 82, 0.06); }
.topbar-inner { display: flex; align-items: center; gap: 26px; height: 70px; }
.brand { display: flex; align-items: center; gap: 11px; min-width: 0; }
.logo-badge, .logo-img {
  width: 38px; height: 38px; border-radius: 11px; flex-shrink: 0;
}
.logo-badge {
  display: grid; place-items: center; font-weight: 800; color: #fff;
  background: linear-gradient(135deg, #2f6fed, #0f2a52); letter-spacing: 1px;
}
.logo-img { object-fit: contain; background: #fff; border: 1px solid var(--line); }
.brand-text { display: flex; flex-direction: column; line-height: 1.25; min-width: 0; }
.brand-text strong { font-size: 16px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.brand-text small { color: var(--muted); font-size: 11px; letter-spacing: 0.6px; }
.desktop-nav { display: flex; align-items: center; gap: 6px; margin-left: auto; }
.nav-link { padding: 9px 14px; border-radius: 999px; color: #334155; font-size: 14px; font-weight: 500; }
.nav-link:hover { background: #f1f6ff; color: var(--blue); }
.nav-link.active { background: var(--sky); color: var(--blue); font-weight: 700; }
.top-actions { display: flex; align-items: center; gap: 8px; }
.lang-btn {
  border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 7px 12px;
  font-size: 12px; color: var(--navy); cursor: pointer;
}
.menu-btn { display: none; border: none; background: transparent; flex-direction: column; gap: 4px; padding: 8px; cursor: pointer; }
.menu-btn span { width: 20px; height: 2px; background: var(--navy); border-radius: 2px; }
.mobile-nav { display: none; padding: 8px 4vw 16px; border-top: 1px solid var(--line); flex-direction: column; gap: 4px; }
.mobile-link { padding: 12px 14px; border-radius: 10px; font-size: 15px; }
.mobile-link.active { background: var(--sky); color: var(--blue); font-weight: 700; }

.main { min-height: calc(100vh - 70px - 140px); padding-bottom: 40px; }
.footer { background: var(--navy-deep); color: #cbd5e1; padding: 34px 0; margin-top: 30px; }
.footer-inner { display: flex; justify-content: space-between; gap: 24px; flex-wrap: wrap; }
.footer strong { color: #fff; font-size: 16px; }
.footer p { margin: 8px 0 0; color: #94a3b8; font-size: 13px; }
.footer-meta { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: #94a3b8; text-align: right; }
.error-box { padding: 80px 0; text-align: center; }
.error-box h2 { color: var(--navy); }
.fab {
  position: fixed; right: 18px; bottom: 24px; z-index: 40;
  background: linear-gradient(135deg, #2f6fed, #1d4ed8); color: #fff;
  padding: 13px 20px; border-radius: 999px; font-weight: 700; font-size: 14px;
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.35);
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-6px); }

@media (max-width: 900px) {
  .desktop-nav { display: none; }
  .menu-btn { display: flex; }
  .mobile-nav { display: flex; }
  .footer-meta { text-align: left; }
  .brand-text small { display: none; }
}
</style>
