import request, { type ApiResult, type PageResult } from '@/utils/request'
import type { PostDetail } from './posts'

export interface PostSummary {
  id: number
  title: string
  slug: string
  status: string
  categoryId?: number | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string | null
  wordCount: number
  viewCount: number
  tags?: string[]
}

export interface PostUpsertPayload {
  id?: number
  title: string
  slug?: string
  contentMd: string
  tags: string[]
  categoryId?: number | null
  status?: string
}

export const listAdminPosts = (
  page = 1,
  size = 50,
  status?: 'DRAFT' | 'PUBLISHED' | 'ALL' | string
) =>
  request.get<any, ApiResult<PageResult<PostSummary>>>('/admin/posts', {
    params: { page, size, ...(status && status !== 'ALL' ? { status } : {}) }
  })

export const getAdminPost = (id: number) =>
  request.get<any, ApiResult<PostDetail>>(`/admin/posts/${id}`)

export const createPost = (data: PostUpsertPayload) =>
  request.post<any, ApiResult<PostDetail>>('/admin/posts', data)

export const updatePost = (id: number, data: PostUpsertPayload) =>
  request.put<any, ApiResult<PostDetail>>(`/admin/posts/${id}`, data)

export const deletePost = (id: number) =>
  request.delete<any, ApiResult<null>>(`/admin/posts/${id}`)
