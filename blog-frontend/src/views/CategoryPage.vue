<template>
  <div class="page-shell">
    <aside class="sidebar">
      <h3>分类导航</h3>
      <div v-if="tree.length === 0" class="empty">暂无分类</div>
      <ul class="tree">
        <li v-for="node in tree" :key="node.id" class="node">
          <router-link
            :to="{ name: 'category-detail', params: { slug: node.slug } }"
            :class="['node-title', { active: node.slug === currentSlug }]"
          >
            {{ node.name }}
          </router-link>
          <ul v-if="node.children?.length" class="children">
            <li v-for="child in node.children" :key="child.id">
              <router-link
                :to="{ name: 'category-detail', params: { slug: child.slug } }"
                :class="['node-title', { active: child.slug === currentSlug }]"
              >
                {{ child.name }}
              </router-link>
            </li>
          </ul>
        </li>
      </ul>
    </aside>

    <main class="content">
      <div class="header-block">
        <h1>{{ currentCategory?.name || '分类' }}</h1>
        <p>按主题浏览文章列表</p>
      </div>

      <div v-if="children.length" class="subnav">
        <router-link
          v-for="child in children"
          :key="child.id"
          :to="{ name: 'category-detail', params: { slug: child.slug } }"
          class="pill"
          :class="{ active: child.slug === currentSlug }"
        >
          {{ child.name }}
        </router-link>
      </div>

      <div v-if="posts.length === 0" class="empty-panel">该分类下暂无文章。</div>
      <div v-else class="post-list">
        <article v-for="post in posts" :key="post.id" class="post-card">
          <h2>
            <router-link :to="{ name: 'post-detail', params: { slug: post.slug } }">{{ post.title }}</router-link>
          </h2>
          <div class="meta">
            <span>📅 {{ post.publishedAt ? new Date(post.publishedAt).toLocaleDateString('zh-CN') : '—' }}</span>
            <span>📝 {{ post.wordCount ?? 0 }} 字</span>
            <span>👁 {{ post.viewCount ?? 0 }} 阅读</span>
          </div>
        </article>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCategoryBySlug, listCategories, listCategoryPosts, type Category } from '@/api/categories'

const route = useRoute()
const currentSlug = computed(() => String(route.params.slug || ''))
const categories = ref<Category[]>([])
const posts = ref<any[]>([])
const currentCategory = ref<Category | null>(null)

type CategoryTreeNode = Category & { children?: CategoryTreeNode[] }

const tree = computed(() => {
  const map = new Map<number, CategoryTreeNode>()
  const roots: CategoryTreeNode[] = []
  categories.value.forEach((c) => {
    const node: CategoryTreeNode = { ...c, children: [] }
    map.set(c.id, node)
    if (c.parentId == null) roots.push(node)
  })
  categories.value.forEach((c) => {
    if (c.parentId != null) {
      const parent = map.get(c.parentId)
      if (parent) {
        parent.children = parent.children || []
        parent.children.push({ ...c, children: [] })
      }
    }
  })
  return roots
})

const children = computed(() => {
  const all = categories.value.filter((c) => c.parentId != null)
  return all.filter((c) => c.parentId === currentCategory.value?.id)
})

async function load() {
  const res = await listCategories()
  categories.value = res.data || []
  if (currentSlug.value) {
    const catRes = await getCategoryBySlug(currentSlug.value)
    currentCategory.value = catRes.data || null
    const listRes = await listCategoryPosts(currentSlug.value)
    posts.value = listRes.data || []
  } else {
    currentCategory.value = null
    posts.value = []
  }
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<style scoped>
.page-shell {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  min-height: calc(100vh - 160px);
}
.sidebar,
.content {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 20px;
}
.sidebar h3 {
  margin: 0 0 16px;
}
.tree,
.children {
  list-style: none;
  padding-left: 0;
  margin: 0;
}
.node {
  margin-bottom: 12px;
}
.children {
  margin-top: 8px;
  padding-left: 14px;
  border-left: 1px solid #eee;
}
.node-title {
  color: #333;
  text-decoration: none;
  display: inline-block;
  padding: 6px 8px;
  border-radius: 6px;
}
.node-title.active {
  background: #eff6ff;
  color: #2080f0;
  font-weight: 600;
}
.header-block {
  margin-bottom: 18px;
}
.header-block h1 {
  margin: 0 0 8px;
}
.subnav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}
.pill {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 999px;
  background: #f5f7fb;
  color: #333;
  text-decoration: none;
}
.pill.active {
  color: #fff;
  background: #2080f0;
}
.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.post-card {
  padding: 18px 20px;
  border: 1px solid #eee;
  border-radius: 10px;
  background: #fafafa;
}
.post-card h2 {
  margin: 0 0 8px;
  font-size: 20px;
}
.post-card a {
  color: #2c3e50;
  text-decoration: none;
}
.meta {
  color: #888;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
}
.empty,
.empty-panel {
  color: #999;
  padding: 24px 0;
}
@media (max-width: 900px) {
  .page-shell {
    grid-template-columns: 1fr;
  }
}
</style>
