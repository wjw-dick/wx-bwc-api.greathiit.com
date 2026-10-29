import axios from 'axios'

const http = axios.create({ baseURL: '/api/public', timeout: 20000 })

http.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) return data.data
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => {
    const message = error?.response?.data?.message || error.message || '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export default http
