<template>
  <div class="container">
    <h1 class="page-title">标签：{{ tagName }}</h1>
    <n-spin :show="loading">
      <div v-if="posts.length === 0 && !loading" class="empty">该标签下还没有文章。</div>
      <article v-for="p in posts" :key="p.id" class="card">
        <h2 class="post-title">
          <router-link :to="{ name: 'post-detail', params: { slug: p.slug } }">{{ p.title }}</router-link>
        </h2>
        <div class="meta">📅 {{ formatDate(p.publishedAt || p.createdAt) }}</div>
      </article>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NSpin } from 'naive-ui'
import { listPublishedPosts, type PostSummary } from '@/api/posts'

const route = useRoute()
const posts = ref<PostSummary[]>([])
const loading = ref(false)
const tagName = ref('')

async function load() {
  tagName.value = route.params.name as string
  loading.value = true
  try {
    const res = await listPublishedPosts(1, 50, tagName.value)
    posts.value = res.data.records
  } finally {
    loading.value = false
  }
}

watch(() => route.params.name, load, { immediate: true })

function formatDate(iso: string | null) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 18px; }
.empty { padding: 60px 0; text-align: center; color: #999; }
.card { background: #fff; padding: 18px 22px; border-radius: 8px; margin-bottom: 12px; }
.post-title { margin: 0 0 6px; font-size: 18px; }
.meta { color: #888; font-size: 12px; }
</style>