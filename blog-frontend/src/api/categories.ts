import request, { type ApiResult } from '@/utils/request'

export interface Category {
  id: number
  name: string
  slug: string
  parentId: number | null
  sort: number
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export const listCategories = () =>
  request.get<any, ApiResult<Category[]>>('/categories')

export const getCategoryBySlug = (slug: string) =>
  request.get<any, ApiResult<Category>>(`/categories/${encodeURIComponent(slug)}`)

export const listCategoryPosts = (slug: string) =>
  request.get<any, ApiResult<any[]>>(`/categories/${encodeURIComponent(slug)}/posts`)

export const listAdminCategories = () =>
  request.get<any, ApiResult<Category[]>>('/admin/categories')

export const createCategory = (data: Partial<Category>) =>
  request.post<any, ApiResult<Category>>('/admin/categories', data)

export const updateCategory = (id: number, data: Partial<Category>) =>
  request.put<any, ApiResult<Category>>(`/admin/categories/${id}`, data)

export const deleteCategory = (id: number) =>
  request.delete<any, ApiResult<null>>(`/admin/categories/${id}`)
