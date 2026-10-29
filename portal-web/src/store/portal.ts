import { reactive, computed } from 'vue'
import api from '@/api'

export interface TenantInfo {
  id?: number
  name?: string
  code?: string
  type?: string
  status?: string
  intro?: string
  dutyPhone?: string
  logo?: string
  config?: Record<string, string>
}

export interface H5Menu {
  id: number
  name: string
  nameEn?: string
  path: string
  sort?: number
}

const state = reactive({
  code: 'DEMO',
  tenant: null as TenantInfo | null,
  menus: [] as H5Menu[],
  loading: false,
  error: '',
  loadedCode: ''
})

export function usePortalStore() {
  const tenantName = computed(() => state.tenant?.name || '校园安全门户')

  async function load(code: string, force = false) {
    if (!code) return
    state.code = code
    if (!force && state.loadedCode === code) return
    state.loading = true
    state.error = ''
    try {
      const [tenant, menus] = await Promise.all([
        api.get(`/tenant/${code}`),
        api.get(`/tenant/${code}/menus`, { params: { platform: 'h5' } })
      ]) as any[]
      state.tenant = tenant
      state.menus = menus || []
      state.loadedCode = code
    } catch (e: any) {
      state.tenant = null
      state.menus = []
      state.error = e.message || '加载失败'
    } finally {
      state.loading = false
    }
  }

  return { state, tenantName, load }
}

export function menuPath(code: string, path: string) {
  if (!path || path === '/') return `/${code}`
  if (path.startsWith('/t/')) return path
  return `/${code}${path.startsWith('/') ? path : `/${path}`}`
}
