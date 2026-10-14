import request, { type ApiResult, type PageResult } from '@/utils/request'

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

export interface PostDetail {
  id: number
  title: string
  slug: string
  contentMd: string
  contentHtml: string
  tocJson: string
  status: string
  categoryId?: number | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string | null
  wordCount: number
  viewCount: number
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

export const listTags = () => request.get<any, ApiResult<any[]>>('/tags')