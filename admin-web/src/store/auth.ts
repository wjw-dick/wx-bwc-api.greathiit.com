import { defineStore } from 'pinia'
import http from '@/api/http'

export interface MenuNode {
  id: number
  name: string
  nameEn?: string
  path: string
  icon?: string
  sort?: number
  permission?: string
  menuType?: string
  configJson?: string
  children?: MenuNode[]
}

interface AuthState {
  token: string
  user: Record<string, any> | null
  tenant: Record<string, any> | null
  menus: MenuNode[]
  permissions: string[]
  languages: Array<Record<string, any>>
  tenantHeader: number | null
  tenantOptions: Array<Record<string, any>>
  locale: string
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('bwc_token') || '',
    user: JSON.parse(localStorage.getItem('bwc_user') || 'null'),
    tenant: JSON.parse(localStorage.getItem('bwc_tenant') || 'null'),
    menus: JSON.parse(localStorage.getItem('bwc_menus') || '[]'),
    permissions: JSON.parse(localStorage.getItem('bwc_permissions') || '[]'),
    languages: JSON.parse(localStorage.getItem('bwc_languages') || '[]'),
    tenantHeader: Number(localStorage.getItem('bwc_tenant_header') || 0) || null,
    tenantOptions: [],
    locale: localStorage.getItem('bwc_locale') || 'zh-CN'
  }),
  getters: {
    isPlatform: (state) => !!state.user?.platform,
    isSystem: (state) => !!state.user?.platform && state.user?.roleCode === 'SYSTEM',
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
    schoolName: (state) => state.tenant?.name || '平台管理端',
    hasPerm: (state) => (permission: string) => {
      if (state.permissions.includes('*')) return true
      return state.permissions.includes(permission)
    },
    menuPaths: (state) => {
      const paths: string[] = []
      const walk = (nodes: MenuNode[]) => {
        nodes.forEach((node) => {
          if (node.path) paths.push(node.path)
          if (node.children?.length) walk(node.children)
        })
      }
      walk(state.menus || [])
      return paths
    }
  },
  actions: {
    async login(payload: { username: string; password: string; loginType?: string; tenantCode?: string }) {
      const data: any = await http.post('/auth/login', payload)
      this.applySession(data)
      return data
    },
    applySession(data: any) {
      this.token = data.token
      this.user = data.user
      this.tenant = data.tenant || null
      this.menus = data.menus || []
      this.permissions = data.permissions || []
      this.languages = data.languages || []
      this.persist()
    },
    async loadMe() {
      if (!this.token) return
      try {
        const data: any = await http.get('/auth/me')
        this.user = data.user || this.user
        this.tenant = data.tenant || this.tenant
        this.menus = data.menus || this.menus
        this.permissions = data.permissions || this.permissions
        this.persist()
      } catch (e) {
        // http 层已处理 401
      }
    },
    async loadTenantOptions() {
      if (!this.isPlatform) return
      this.tenantOptions = (await http.get('/platform/tenants/options')) as any
    },
    setTenantHeader(tenantId: number | null) {
      this.tenantHeader = tenantId
      localStorage.setItem('bwc_tenant_header', tenantId ? String(tenantId) : '')
    },
    setLocale(locale: string) {
      this.locale = locale
      localStorage.setItem('bwc_locale', locale)
    },
    logout() {
      this.token = ''
      this.user = null
      this.tenant = null
      this.menus = []
      this.permissions = []
      this.tenantHeader = null
      localStorage.removeItem('bwc_token')
      localStorage.removeItem('bwc_user')
      localStorage.removeItem('bwc_tenant')
      localStorage.removeItem('bwc_menus')
      localStorage.removeItem('bwc_permissions')
      localStorage.removeItem('bwc_languages')
      localStorage.removeItem('bwc_tenant_header')
    },
    persist() {
      localStorage.setItem('bwc_token', this.token)
      localStorage.setItem('bwc_user', JSON.stringify(this.user))
      localStorage.setItem('bwc_tenant', JSON.stringify(this.tenant))
      localStorage.setItem('bwc_menus', JSON.stringify(this.menus))
      localStorage.setItem('bwc_permissions', JSON.stringify(this.permissions))
      localStorage.setItem('bwc_languages', JSON.stringify(this.languages))
    }
  }
})
