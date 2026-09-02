import request, { type ApiResult, type PageResult } from '@/utils/request'

export interface PostSummary {
  id: number
  title: string
  slug: string
  status: string
  publishedAt: string | null
  createdAt: string
  tags?: string[]
}

export interface PostDetail {
  id: number
  title: string
  slug: string
  contentHtml: string
  tocJson: string
  publishedAt: string | null
  createdAt: string
  updatedAt: string
  tags: string[]
}

export const listPublishedPosts = (page = 1, size = 10, tag?: string) =>
  request.get<any, ApiResult<PageResult<PostSummary>>>('/posts', {
    params: { page, size, tag }
  })

export const getPostBySlug = (slug: string) =>
  request.get<any, ApiResult<PostDetail>>(`/posts/${encodeURIComponent(slug)}`)

export const searchPosts = (q: string, limit = 20) =>
  request.get<any, ApiResult<PostSummary[]>>('/posts/search', { params: { q, limit } })

export const listTags = () => request.get<any, ApiResult<string[]>>('/tags')