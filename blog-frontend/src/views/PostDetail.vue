<template>
  <div class="container">
    <n-spin :show="loading">
      <article v-if="post" class="post">
        <h1 class="title">{{ post.title }}</h1>
        <div class="meta">
          <span>📅 {{ formatDate(post.publishedAt || post.createdAt) }}</span>
          <span v-if="post.tags.length">
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

const route = useRoute()
const post = ref<PostDetail | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getPostBySlug(route.params.slug as string)
    post.value = res.data
    // Client-side highlight (server already pre-rendered, this is a safety net)
    requestAnimationFrame(() => {
      document.querySelectorAll<HTMLElement>('.markdown-body pre code').forEach((el) => {
        hljs.highlightElement(el)
      })
    })
  } catch {
    post.value = null
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