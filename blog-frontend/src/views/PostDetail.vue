<template>
  <div class="container">
    <n-spin :show="loading">
      <article v-if="post" class="post">
        <h1 class="title">{{ post.title }}</h1>
        <div class="meta">
          <span>📅 发布：{{ formatDate(post.publishedAt || post.createdAt) }}</span>
          <span>🕒 更新：{{ formatDate(post.updatedAt || post.publishedAt || post.createdAt) }}</span>
          <span>📝 {{ post.wordCount ?? 0 }} 字</span>
          <span>👁 {{ post.viewCount ?? 0 }} 阅读</span>
          <span v-if="categoryPath" class="category-path">📂 {{ categoryPath }}</span>
          <span v-if="post.tags && post.tags.length">
            🏷️
            <router-link
              v-for="t in post.tags"
              :key="t"
              :to="{ name: 'tag-archive', params: { name: t } }"
              class="tag"
            >{{ t }}</router-link>
          </span>
        </div>
        <div class="markdown-body" v-html="post.contentHtml"></div>
      </article>
      <div v-else-if="!loading" class="empty">文章不存在或已被删除。</div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NSpin } from 'naive-ui'
import hljs from 'highlight.js'
import { getPostBySlug, type PostDetail } from '@/api/posts'
import { listCategories, type Category } from '@/api/categories'

const route = useRoute()
const post = ref<PostDetail | null>(null)
const loading = ref(false)
const categories = ref<Category[]>([])
const categoryPath = ref('')

function resolveCategoryPath(categoryId?: number | null): string {
  if (!categoryId) return ''
  const map = new Map<number, Category>()
  categories.value.forEach((category) => map.set(category.id, category))

  const path: string[] = []
  let current: Category | undefined = map.get(categoryId)
  while (current) {
    path.unshift(current.name)
    current = current.parentId != null ? map.get(current.parentId) : undefined
  }

  return path.length ? `/${path.join('/')}` : ''
}

async function load() {
  loading.value = true
  try {
    const [postRes, categoryRes] = await Promise.all([
      getPostBySlug(route.params.slug as string),
      listCategories()
    ])
    post.value = postRes.data
    categories.value = categoryRes.data || []
    categoryPath.value = resolveCategoryPath(post.value?.categoryId)
    requestAnimationFrame(() => {
      document.querySelectorAll<HTMLElement>('.markdown-body pre code').forEach((el) => {
        hljs.highlightElement(el)
      })
    })
  } catch {
    post.value = null
    categoryPath.value = ''
  } finally {
    loading.value = false
  }
}

watch(() => route.params.slug, load, { immediate: true })

function formatDate(iso: string | null) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('zh-CN')
}
</script>

<style scoped>
.post {
  background: #fff;
  padding: 32px 40px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.title {
  font-size: 28px;
  margin: 0 0 12px;
}
.meta {
  color: #888;
  font-size: 13px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}
.category-path {
  color: #5b7db8;
  font-weight: 500;
}
.tag {
  margin-right: 6px;
  background: #f0f7ff;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}
.empty {
  text-align: center;
  padding: 80px 0;
  color: #999;
}
</style>