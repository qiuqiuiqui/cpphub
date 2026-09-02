<template>
  <div class="container">
    <h1 class="page-title">最新文章</h1>
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
          <span>📅 {{ formatDate(p.publishedAt || p.createdAt) }}</span>
          <span v-if="p.tags && p.tags.length">
            🏷️
            <router-link
              v-for="t in p.tags"
              :key="t"
              :to="{ name: 'tag-archive', params: { name: t } }"
              class="tag"
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

const posts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await listPublishedPosts(page.value, size)
    posts.value = res.data.records
    total.value = res.data.total
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
}
.tag {
  margin-right: 6px;
  background: #f0f7ff;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}
</style>