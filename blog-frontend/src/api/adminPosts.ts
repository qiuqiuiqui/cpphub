import request, { type ApiResult } from '@/utils/request'
import type { PostDetail } from './posts'

export interface PostUpsertPayload {
  id?: number
  title: string
  slug?: string
  contentMd: string
  tags: string[]
  status?: string
}

export const createPost = (data: PostUpsertPayload) =>
  request.post<any, ApiResult<PostDetail>>('/admin/posts', data)

export const updatePost = (id: number, data: PostUpsertPayload) =>
  request.put<any, ApiResult<PostDetail>>(`/admin/posts/${id}`, data)

export const deletePost = (id: number) =>
  request.delete<any, ApiResult<null>>(`/admin/posts/${id}`)