<template>
  <n-card title="文章管理">
    <n-space style="margin-bottom: 16px;">
      <n-button type="primary" @click="$router.push({ name: 'admin-post-new' })">新建文章</n-button>
      <n-button @click="load">刷新</n-button>
    </n-space>
    <n-spin :show="loading">
      <n-data-table
        :columns="columns"
        :data="posts"
        :pagination="false"
        :bordered="false"
        striped
      />
    </n-spin>
  </n-card>
</template>

<script setup lang="ts">
import { onMounted, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NSpace, NButton, NSpin, NDataTable, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { listPublishedPosts, type PostSummary } from '@/api/posts'
import { deletePost } from '@/api/adminPosts'

const router = useRouter()
const message = useMessage()
const posts = ref<PostSummary[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await listPublishedPosts(1, 100)
    posts.value = res.data.records
  } finally {
    loading.value = false
  }
}

onMounted(load)

const columns: DataTableColumns<PostSummary> = [
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) =>
      h(NTag, { type: row.status === 'PUBLISHED' ? 'success' : 'default', size: 'small' }, { default: () => row.status })
  },
  {
    title: '发布时间',
    key: 'publishedAt',
    width: 180,
    render: (row) => (row.publishedAt ? new Date(row.publishedAt).toLocaleString('zh-CN') : '—')
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (row) =>
      h(NSpace, {}, () => [
        h(
          NButton,
          {
            size: 'small',
            onClick: () => router.push({ name: 'admin-post-edit', params: { id: row.id } })
          },
          { default: () => '编辑' }
        ),
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

async function onDelete(row: PostSummary) {
  if (!confirm(`确认删除「${row.title}」？`)) return
  try {
    await deletePost(row.id)
    message.success('已删除')
    load()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '删除失败')
  }
}
</script>