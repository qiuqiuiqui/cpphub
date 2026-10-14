<template>
  <div class="container">
    <section class="category-hero">
      <h1 class="page-title">学习专栏</h1>
      <div class="category-grid">
        <router-link v-for="c in rootCategories" :key="c.id" :to="{ name: 'category-detail', params: { slug: c.slug } }" class="category-card">
          {{ c.name }}
        </router-link>
      </div>
    </section>

    <h2 class="page-title secondary">最新文章</h2>
    <n-spin :show="loading">
      <div v-if="posts.length === 0 && !loading" class="empty">
        还没有发布的文章。
        <router-link to="/admin/posts/new">去写一篇 →</router-link>
      </div>
      <article v-for="p in posts" :key="p.id" class="card">
        <h2 class="post-title">
          <router-link :to="{ name: 'post-detail', params: { slug: p.slug } }">
            {{ p.title }}
          </router-link>
        </h2>
        <div class="meta">
          <span>📅 发布：{{ formatDate(p.publishedAt || p.createdAt) }}</span>
          <span>🕒 更新：{{ formatDate(p.updatedAt || p.createdAt) }}</span>
          <span>📝 {{ p.wordCount ?? 0 }} 字</span>
          <span>👁 {{ p.viewCount ?? 0 }} 阅读</span>
          <span v-if="getCategoryPath(p.categoryId)" class="category-path">📂 {{ getCategoryPath(p.categoryId) }}</span>
          <span v-if="p.tags && p.tags.length">
            🏷️
            <router-link
              v-for="t in p.tags"
              :key="t"
              :to="{ name: 'tag-archive', params: { name: t } }"
              class="tag"
              :style="{ background: (tagColorMap[t] || '#4098FC') + '22', color: tagColorMap[t] || '#4098FC', border: '1px solid ' + (tagColorMap[t] || '#4098FC') }"
            >{{ t }}</router-link>
          </span>
        </div>
      </article>
      <n-pagination
        v-if="total > size"
        v-model:page="page"
        :page-count="Math.ceil(total / size)"
        style="margin-top: 24px; justify-content: center;"
      />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { NSpin, NPagination } from 'naive-ui'
import { listPublishedPosts, type PostSummary } from '@/api/posts'
import { listCategories, type Category } from '@/api/categories'
import { listTags, type TagOption } from '@/api/tags'

const posts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const rootCategories = ref<Category[]>([])
const categories = ref<Category[]>([])
const tagColorMap = ref<Record<string, string>>({})

function getCategoryPath(categoryId?: number | null): string {
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
    const [postRes, categoryRes, tagRes] = await Promise.all([
      listPublishedPosts(page.value, size),
      listCategories(),
      listTags()
    ])
    posts.value = postRes.data.records
    total.value = postRes.data.total
    categories.value = categoryRes.data || []
    rootCategories.value = categories.value.filter((c) => c.parentId == null)
    const tags = (tagRes.data || []) as TagOption[]
    tagColorMap.value = tags.reduce<Record<string, string>>((map, tag) => {
      map[tag.name] = tag.color || '#4098FC'
      return map
    }, {})
  } finally {
    loading.value = false
  }
}

watch(page, load, { immediate: true })

function formatDate(iso: string | null) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.page-title {
  margin-bottom: 24px;
  font-size: 24px;
}
.page-title.secondary {
  font-size: 20px;
  margin-top: 36px;
}
.category-hero {
  margin-bottom: 24px;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.category-card {
  display: block;
  padding: 18px 16px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eff6ff 0%, #e0f2fe 100%);
  color: #1f3a5f;
  text-decoration: none;
  font-weight: 600;
}
.empty {
  padding: 60px 0;
  text-align: center;
  color: #999;
}
.card {
  background: #fff;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}
.card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.post-title {
  margin: 0 0 8px;
  font-size: 20px;
}
.post-title a {
  color: #2c3e50;
}
.meta {
  color: #888;
  font-size: 13px;
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
</style>