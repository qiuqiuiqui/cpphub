import axios, { AxiosError, type AxiosInstance } from 'axios'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

instance.interceptors.response.use(
  (res) => res.data,
  (error: AxiosError<any>) => {
    const status = error.response?.status
    const msg = error.response?.data?.message ?? error.message
    // eslint-disable-next-line no-console
    console.error(`[API ERROR ${status}] ${msg}`)
    return Promise.reject(error)
  }
)

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export default instance