import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import router from '@/router'

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.token}`
    if (auth.tenantHeader) {
      config.headers['X-Tenant-Id'] = String(auth.tenantHeader)
    }
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) {
        return data.data
      }
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => {
    const status = error?.response?.status
    const message = error?.response?.data?.message || error.message || '网络异常'
    if (status === 401) {
      const auth = useAuthStore()
      const onLoginPage = router.currentRoute.value.path === '/login'
      const serverMessage = error?.response?.data?.message
      auth.logout()
      if (!onLoginPage) {
        router.push('/login')
      }
      ElMessage.error(serverMessage || (onLoginPage ? '账号或密码错误，请检查后重试' : '登录已失效，请重新登录'))
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export function download(url: string, params?: Record<string, unknown>, fileName?: string) {
  return http.get(url, { params, responseType: 'blob' } as AxiosRequestConfig).then((blob: any) => {
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName || 'download'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(objectUrl)
  })
}

export async function uploadFile(file: File, bizType = 'common') {
  const form = new FormData()
  form.append('file', file)
  form.append('bizType', bizType)
  return http.post('/system/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<any>
}

export default http
