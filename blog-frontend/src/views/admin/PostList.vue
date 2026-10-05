<template>
  <div class="admin-shell" :class="{ 'is-collapsed': isSidebarCollapsed }">
    <aside class="category-sidebar" :class="{ collapsed: isSidebarCollapsed }">
      <div v-if="!isSidebarCollapsed" class="sidebar-inner">
        <div class="sidebar-header">
          <h3>专栏快速筛选</h3>
          <n-button size="tiny" quaternary @click="selectedCategoryId = null">全部</n-button>
        </div>

        <div class="category-list">
          <button
            type="button"
            class="category-item root-item"
            :class="{ active: selectedCategoryId === null }"
            @click="selectedCategoryId = null"
          >
            <span>全部</span>
            <n-tag size="small" :type="selectedCategoryId === null ? 'primary' : 'default'">
              {{ posts.length }}
            </n-tag>
          </button>

          <div v-for="node in categoryTree" :key="node.id" class="category-branch">
            <button
              type="button"
              class="category-item"
              :class="{ active: selectedCategoryId === node.id }"
              :style="{ marginLeft: `${node.depth * 12}px` }"
              @click="selectedCategoryId = node.id"
            >
              <span class="category-label-wrap">
                <span class="tree-dot" v-if="node.depth > 0"></span>
                <span>{{ node.name }}</span>
              </span>
              <n-tag size="small" :type="selectedCategoryId === node.id ? 'primary' : 'default'">
                {{ node.count }}
              </n-tag>
            </button>

            <div v-if="node.children?.length" class="category-children">
              <template v-for="child in node.children" :key="child.id">
                <button
                  type="button"
                  class="category-item category-child"
                  :class="{ active: selectedCategoryId === child.id }"
                  :style="{ marginLeft: `${(node.depth + 1) * 12}px` }"
                  @click="selectedCategoryId = child.id"
                >
                  <span class="category-label-wrap">
                    <span class="tree-dot" v-if="child.depth > 0"></span>
                    <span>{{ child.name }}</span>
                  </span>
                  <n-tag size="small" :type="selectedCategoryId === child.id ? 'primary' : 'default'">
                    {{ child.count }}
                  </n-tag>
                </button>

                <div v-if="child.children?.length" class="category-children" style="margin-left: 12px;">
                  <button
                    v-for="grandchild in child.children"
                    :key="grandchild.id"
                    type="button"
                    class="category-item category-child"
                    :class="{ active: selectedCategoryId === grandchild.id }"
                    :style="{ marginLeft: `${(child.depth + 1) * 12}px` }"
                    @click="selectedCategoryId = grandchild.id"
                  >
                    <span class="category-label-wrap">
                      <span class="tree-dot" v-if="grandchild.depth > 0"></span>
                      <span>{{ grandchild.name }}</span>
                    </span>
                    <n-tag size="small" :type="selectedCategoryId === grandchild.id ? 'primary' : 'default'">
                      {{ grandchild.count }}
                    </n-tag>
                  </button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <button
      type="button"
      class="sidebar-toggle"
      :title="isSidebarCollapsed ? '展开专栏筛选' : '收起专栏筛选'"
      @click="isSidebarCollapsed = !isSidebarCollapsed"
    >
      {{ isSidebarCollapsed ? '▶' : '◀' }}
    </button>

    <div class="content-panel">
      <n-card>
        <template #header>
          <span style="font-weight: 600;">{{ pageTitle }}</span>
        </template>
        <template #header-extra>
          <n-space>
            <n-select
              v-model:value="selectedCategoryId"
              clearable
              size="small"
              :options="categorySelectOptions"
              placeholder="按专栏筛选"
              style="width: 180px;"
            />
            <n-button @click="loadAll" size="small">刷新</n-button>
            <n-button v-if="selectedTag" size="small" type="info" ghost @click="clearTag">已筛选：{{ selectedTag }}（清除）</n-button>
            <n-button type="primary" @click="onNew">新建文章</n-button>
            <n-button v-if="selectedRowKeys.length > 0" type="error" size="small" @click="onBatchDelete">批量删除 (已选 {{ selectedRowKeys.length }})</n-button>
          </n-space>
        </template>

        <n-tabs v-model:value="activeTab" type="line" @update:value="onTabChange">
          <n-tab-pane name="ALL" :tab="`全部 (${counts.ALL})`" />
          <n-tab-pane name="DRAFT" :tab="`草稿箱 (${counts.DRAFT})`" />
          <n-tab-pane name="PUBLISHED" :tab="`已发布 (${counts.PUBLISHED})`" />
        </n-tabs>

        <n-spin :show="loading">
          <n-empty v-if="!loading && filteredPosts.length === 0" :description="emptyDesc" style="padding: 40px 0;" />
          <n-data-table
            v-else
            :columns="columns"
            :data="sortedPosts"
            :pagination="false"
            :bordered="false"
            striped
            :scroll-x="1600"
            :row-key="row => row.id"
            v-model:selected-row-keys="selectedRowKeys"
          />
        </n-spin>
      </n-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NCard, NSpace, NButton, NSpin, NDataTable, NTag, NTabs, NTabPane, NEmpty, NCheckbox, NSelect, useMessage,
  type DataTableColumns
} from 'naive-ui'
import { listAdminPosts, deletePost, type PostSummary } from '@/api/adminPosts'
import { listCategories, type Category } from '@/api/categories'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const posts = ref<PostSummary[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const activeTab = ref<'ALL' | 'DRAFT' | 'PUBLISHED'>('ALL')
const selectedCategoryId = ref<number | null>(null)
const isSidebarCollapsed = ref(false)

const counts = ref({ ALL: 0, DRAFT: 0, PUBLISHED: 0 })
const emptyDesc = ref('暂无文章')

type CategoryTreeNode = Category & {
  depth: number
  count: number
  children: CategoryTreeNode[]
}

const categoryTree = computed<CategoryTreeNode[]>(() => {
  const countMap = new Map<number | null, number>()
  posts.value.forEach((post) => {
    const key = post.categoryId ?? null
    countMap.set(key, (countMap.get(key) ?? 0) + 1)
  })

  const map = new Map<number, CategoryTreeNode>()
  categories.value.forEach((category) => {
    map.set(category.id, {
      ...category,
      depth: 0,
      count: countMap.get(category.id) ?? 0,
      children: []
    })
  })

  const roots: CategoryTreeNode[] = []
  categories.value.forEach((category) => {
    const node = map.get(category.id)
    if (!node) return

    if (category.parentId == null) {
      roots.push(node)
      return
    }

    const parent = map.get(category.parentId)
    if (parent) {
      parent.children.push(node)
    }
  })

  const walk = (node: CategoryTreeNode, depth = 0) => {
    node.depth = depth
    const childCount = node.children.reduce((sum, child) => sum + walk(child, depth + 1).count, 0)
    node.count += childCount
    return node
  }

  roots.forEach((root) => walk(root, 0))

  return roots
    .slice()
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
    .map((node) => ({ ...node, children: node.children.slice().sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)) }))
})

const categorySelectOptions = computed(() => categories.value.map((category) => ({
  label: category.name,
  value: category.id
})))

const filteredPosts = computed(() => {
  if (selectedCategoryId.value == null) return posts.value

  const selectedIds = new Set<number>()
  const walkSelected = (nodes: CategoryTreeNode[]) => {
    nodes.forEach((node) => {
      if (node.id === selectedCategoryId.value) {
        selectedIds.add(node.id)
        const collect = (current: CategoryTreeNode) => {
          selectedIds.add(current.id)
          current.children.forEach((child) => collect(child))
        }
        collect(node)
        return
      }
      if (node.children.length) walkSelected(node.children)
    })
  }

  walkSelected(categoryTree.value)

  if (selectedIds.size === 0) {
    return posts.value.filter((post) => (post.categoryId ?? null) === selectedCategoryId.value)
  }

  return posts.value.filter((post) => {
    const categoryId = post.categoryId ?? null
    return categoryId != null && selectedIds.has(categoryId)
  })
})

async function loadPosts() {
  loading.value = true
  try {
    const res = await listAdminPosts(1, 200, activeTab.value)
    posts.value = res.data.records
    emptyDesc.value =
      activeTab.value === 'DRAFT'
        ? '草稿箱为空——开始写一篇新文章吧'
        : activeTab.value === 'PUBLISHED'
        ? '还没有发布过文章'
        : '暂无文章'
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  try {
    const [allRes, draftRes, pubRes] = await Promise.all([
      listAdminPosts(1, 500, 'ALL'),
      listAdminPosts(1, 500, 'DRAFT'),
      listAdminPosts(1, 500, 'PUBLISHED')
    ])
    counts.value = {
      ALL: allRes.data.total,
      DRAFT: draftRes.data.total,
      PUBLISHED: pubRes.data.total
    }
  } catch {
    // non-fatal
  }
}

async function loadAll() {
  await loadPosts()
  await loadCounts()
}

async function onTabChange() {
  await loadPosts()
}

function onNew() {
  router.push({ name: 'admin-post-new' })
}


async function loadCategories() {
  try {
    const res = await listCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

onMounted(async () => {
  const n = String(route.name || '')
  if (n === 'admin-posts-published') activeTab.value = 'PUBLISHED'
  else if (n === 'admin-posts-drafts') activeTab.value = 'DRAFT'
  else activeTab.value = 'ALL'
  await loadCategories()
  await loadAll()
})

watch(() => route.name, (v) => {
  const n = String(v || '')
  if (n === 'admin-posts-published') activeTab.value = 'PUBLISHED'
  else if (n === 'admin-posts-drafts') activeTab.value = 'DRAFT'
  else activeTab.value = 'ALL'
  loadAll()
})

import { computed } from 'vue'

const selectedRowKeys = ref<number[]>([])
const sortBy = ref<string | null>(null)
const sortDir = ref<'asc' | 'desc'>('asc')
const selectedTag = ref<string | null>(null)

const activeTabTitle = computed(() => (activeTab.value === 'PUBLISHED' ? '发布文章管理' : activeTab.value === 'DRAFT' ? '草稿箱' : '文章管理'))
const pageTitle = computed(() => {
  const categoryName = categories.value.find((category) => category.id === selectedCategoryId.value)?.name
  const categoryText = categoryName ? ` · ${categoryName}` : ''
  return selectedTag.value ? `标签：${selectedTag.value}${categoryText} · ${activeTabTitle.value}` : `${activeTabTitle.value}${categoryText}`
})

function setTag(tag: string) {
  if (selectedTag.value === tag) selectedTag.value = null
  else selectedTag.value = tag
}

function clearTag() {
  selectedTag.value = null
}

function setSort(field: string) {
  if (sortBy.value === field) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = field
    sortDir.value = 'asc'
  }
}

const sortedPosts = computed(() => {
  let arr = filteredPosts.value.slice()
  if (selectedTag.value) {
    arr = arr.filter(p => (p.tags || []).includes(selectedTag.value!))
  }
  if (!sortBy.value) return arr
  const dir = sortDir.value === 'asc' ? 1 : -1
  arr.sort((a: any, b: any) => {
    const av = a[sortBy.value as keyof PostSummary]
    const bv = b[sortBy.value as keyof PostSummary]
    if (av == null && bv == null) return 0
    if (av == null) return -1 * dir
    if (bv == null) return 1 * dir
    if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir
    const sa = String(av)
    const sb = String(bv)
    return sa.localeCompare(sb) * dir
  })
  return arr
})

const columns: DataTableColumns<PostSummary> = [
  {
    title: () => h(NCheckbox, {
      checked: selectedRowKeys.value.length > 0 && selectedRowKeys.value.length === posts.value.length,
      'onUpdate:checked': (v: boolean) => {
        if (v) selectedRowKeys.value = posts.value.map(p => p.id)
        else selectedRowKeys.value = []
      }
    }),
    key: 'select',
    width: 60,
    render: (row) =>
      h(NCheckbox, {
        checked: selectedRowKeys.value.includes(row.id),
        'onUpdate:checked': (v: boolean) => {
          if (v) selectedRowKeys.value = Array.from(new Set([...selectedRowKeys.value, row.id]))
          else selectedRowKeys.value = selectedRowKeys.value.filter(i => i !== row.id)
        }
      })
  },
  { title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('title') }, ['标题', sortBy.value === 'title' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']), key: 'title', ellipsis: { tooltip: true } },
  {
    title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('status') }, ['状态', sortBy.value === 'status' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']),
    key: 'status',
    width: 100,
    render: (row) =>
      h(NTag, { type: row.status === 'PUBLISHED' ? 'success' : 'default', size: 'small' }, { default: () => row.status })
  },
  // tags
  {
    title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('tags') }, ['标签', sortBy.value === 'tags' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']),
    key: 'tags',
    width: 200,
    render: (row) => h(NSpace, {}, () => (row.tags || []).map((t: string) => h(NTag, {
      size: 'small',
      style: 'cursor:pointer;',
      type: selectedTag.value === t ? 'info' : undefined,
      onClick: () => setTag(t)
    }, { default: () => t })))
  },
  {
    title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('createdAt') }, ['创建时间', sortBy.value === 'createdAt' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']),
    key: 'createdAt',
    width: 170,
    render: (row) => new Date(row.createdAt).toLocaleString('zh-CN')
  },
  {
    title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('updatedAt') }, ['最后修改', sortBy.value === 'updatedAt' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']),
    key: 'updatedAt',
    width: 170,
    render: (row) => (row.updatedAt ? new Date(row.updatedAt).toLocaleString('zh-CN') : '—')
  },
  {
    title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('publishedAt') }, ['发布时间', sortBy.value === 'publishedAt' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']),
    key: 'publishedAt',
    width: 170,
    render: (row) => (row.publishedAt ? new Date(row.publishedAt).toLocaleString('zh-CN') : '—')
  },
  {
    title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('wordCount') }, ['字数', sortBy.value === 'wordCount' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']),
    key: 'wordCount',
    width: 90,
    render: (row) => `${row.wordCount ?? 0}`
  },
  {
    title: () => h('a', { style: 'cursor:pointer;', onClick: () => setSort('viewCount') }, ['阅读量', sortBy.value === 'viewCount' ? (sortDir.value === 'asc' ? ' ↑' : ' ↓') : '']),
    key: 'viewCount',
    width: 90,
    render: (row) => `${row.viewCount ?? 0}`
  },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    render: (row) =>
      h(NSpace, {}, () => [
        h(
          NButton,
          {
            size: 'small',
            onClick: () => router.push({
              name: 'admin-post-edit',
              params: { id: row.id },
              query: { from: activeTab.value === 'DRAFT' ? 'drafts' : 'published' }
            })
          },
          { default: () => '编辑' }
        ),
        row.status === 'DRAFT'
          ? h(
              NButton,
              {
                size: 'small',
                type: 'primary',
                ghost: true,
                onClick: () => onPublish(row)
              },
              { default: () => '发布' }
            )
          : null,
        h(
          NButton,
          {
            size: 'small',
            type: 'error',
            onClick: () => onDelete(row)
          },
          { default: () => '删除' }
        )
      ])
  }
]

async function onPublish(row: PostSummary) {
  try {
    const { getAdminPost, updatePost } = await import('@/api/adminPosts')
    const detail = await getAdminPost(row.id)
    await updatePost(row.id, {
      title: detail.data.title,
      slug: detail.data.slug,
      contentMd: detail.data.contentMd ?? '',
      tags: detail.data.tags,
      categoryId: detail.data.categoryId ?? null,
      status: 'PUBLISHED'
    })
    message.success('已发布')
    await loadAll()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '发布失败')
  }
}

async function onDelete(row: PostSummary) {
  if (!confirm(`确认删除「${row.title}」？`)) return
  try {
    await deletePost(row.id)
    message.success('已删除')
    await loadAll()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '删除失败')
  }
}

async function onBatchDelete() {
  if (!confirm(`确认删除已选的 ${selectedRowKeys.value.length} 篇文章？`)) return
  try {
    for (const id of selectedRowKeys.value) {
      // ignore individual errors to continue
      try {
        await deletePost(id)
      } catch {}
    }
    message.success('批量删除完成')
    selectedRowKeys.value = []
    await loadAll()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '批量删除失败')
  }
}
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: minmax(220px, 280px) 22px minmax(0, 1fr);
  gap: 0;
  align-items: stretch;
  position: relative;
}

.admin-shell.is-collapsed {
  grid-template-columns: 0 22px minmax(0, 1fr);
}

.category-sidebar {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 16px;
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 120px);
  overflow: auto;
  transition: all 0.2s ease;
  min-width: 0;
}

.category-sidebar.collapsed {
  padding: 0;
  border: none;
  background: transparent;
  overflow: hidden;
  min-width: 0;
}

.sidebar-inner {
  width: 100%;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-branch {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-children {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.category-item {
  width: 100%;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
  color: #333;
  transition: all 0.2s ease;
  text-align: left;
}

.category-item:hover,
.category-item.active {
  background: #eff6ff;
  border-color: #b3d4ff;
  color: #1765d3;
}

.category-item.category-child {
  background: #fff;
}

.category-label-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.tree-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c2d7ff;
  flex-shrink: 0;
}

.root-item {
  margin-bottom: 4px;
}

.sidebar-toggle {
  align-self: center;
  justify-self: center;
  width: 22px;
  height: 72px;
  border: 1px solid #d9d9d9;
  border-left: none;
  background: #fff;
  color: #666;
  cursor: pointer;
  border-radius: 0 8px 8px 0;
  font-size: 12px;
  padding: 0;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.sidebar-toggle:hover {
  background: #f5f9ff;
  color: #1765d3;
}

.content-panel {
  min-width: 0;
  overflow: hidden;
}

@media (max-width: 980px) {
  .admin-shell,
  .admin-shell.is-collapsed {
    grid-template-columns: 1fr;
  }

  .category-sidebar {
    position: static;
    max-height: none;
  }

  .sidebar-toggle {
    display: none;
  }
}
</style>
