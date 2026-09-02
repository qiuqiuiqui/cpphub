import request, { type ApiResult } from '@/utils/request'

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResponse {
  username: string
  role: string
}

export const login = (data: LoginPayload) =>
  request.post<any, ApiResult<LoginResponse>>('/admin/login', data)

export const logout = () => request.post<any, ApiResult<null>>('/admin/logout')

export const me = () => request.get<any, ApiResult<LoginResponse>>('/admin/me')