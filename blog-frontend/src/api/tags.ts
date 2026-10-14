import request, { type ApiResult } from '@/utils/request'

export interface TagOption {
  id: number
  name: string
  color: string
  useCount?: number
  createdAt?: string
  updatedAt?: string | null
}

export const listTags = () => request.get<any, ApiResult<TagOption[]>>('/tags')

export const listAdminTags = () => request.get<any, ApiResult<TagOption[]>>('/admin/tags')

export const createTag = (data: { name: string; color: string }) =>
  request.post<any, ApiResult<TagOption>>('/admin/tags', data)

export const updateTag = (id: number, data: { name?: string; color?: string }) =>
  request.put<any, ApiResult<TagOption>>(`/admin/tags/${id}`, data)

export const deleteTag = (id: number) => request.delete<any, ApiResult<null>>(`/admin/tags/${id}`)
