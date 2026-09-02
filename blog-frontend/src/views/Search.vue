<template>
  <div class="container">
    <h1 class="page-title">搜索</h1>
    <n-input
      v-model:value="query"
      placeholder="输入关键字"
      clearable
      @keyup.enter="doSearch"
      style="max-width: 500px;"
    >
      <template #suffix>
        <n-button type="primary" @click="doSearch">搜索</n-button>
      </template>
    </n-input>
    <n-spin :show="loading" style="margin-top: 20px;">
      <div v-if="results.length === 0 && hasSearched && !loading" class="empty">没有匹配结果。</div>
      <article v-for="p in results" :key="p.id" class="card">
        <h2 class="post-title">
          <router-link :to="{ name: 'post-detail', params: { slug: p.slug } }">{{ p.title }}</router-link>
        </h2>
        <div class="meta">📅 {{ formatDate(p.publishedAt || p.createdAt) }}</div>
      </article>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { NInput, NButton, NSpin } from 'naive-ui'
import { searchPosts, type PostSummary } from '@/api/posts'

const query = ref('')
const results = ref<PostSummary[]>([])
const loading = ref(false)
const hasSearched = ref(false)

async function doSearch() {
  if (!query.value.trim()) return
  loading.value = true
  hasSearched.value = true
  try {
    const res = await searchPosts(query.value, 50)
    results.value = res.data
  } finally {
    loading.value = false
  }
}

function formatDate(iso: string | null) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 18px; }
.empty { padding: 40px 0; text-align: center; color: #999; }
.card { background: #fff; padding: 18px 22px; border-radius: 8px; margin-bottom: 12px; }
.post-title { margin: 0 0 6px; font-size: 18px; }
.meta { color: #888; font-size: 12px; }
</style>