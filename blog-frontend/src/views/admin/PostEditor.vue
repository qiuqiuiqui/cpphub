<template>
  <n-card :title="isEdit ? '编辑文章' : '新建文章'">
    <n-form :model="form" label-placement="top">
      <n-form-item label="标题" required>
        <n-input v-model:value="form.title" placeholder="文章标题" />
      </n-form-item>
      <n-form-item v-if="!isEdit" label="Slug（留空自动生成）">
        <n-input v-model:value="form.slug" placeholder="my-post-slug" />
      </n-form-item>
      <n-form-item label="标签（逗号分隔）">
        <n-input v-model:value="tagsInput" placeholder="C++, 智能指针" />
      </n-form-item>
      <n-form-item label="状态">
        <n-select v-model:value="form.status" :options="[
          { label: '草稿', value: 'DRAFT' },
          { label: '已发布', value: 'PUBLISHED' }
        ]" />
      </n-form-item>
      <n-form-item label="正文 (Markdown)">
        <n-grid :cols="2" :x-gap="12" responsive="screen">
          <n-grid-item>
            <n-input
              v-model:value="form.contentMd"
              type="textarea"
              :rows="22"
              placeholder="# 标题&#10;&#10;在这里写 Markdown..."
              style="font-family: 'Fira Code', Consolas, monospace;"
            />
          </n-grid-item>
          <n-grid-item>
            <div class="preview markdown-body" v-html="renderedHtml"></div>
          </n-grid-item>
        </n-grid>
      </n-form-item>
      <n-space>
        <n-button type="primary" @click="onSubmit" :loading="saving">保存</n-button>
        <n-button @click="$router.push({ name: 'admin-posts' })">取消</n-button>
      </n-space>
    </n-form>
  </n-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NCard, NForm, NFormItem, NInput, NSelect, NButton, NSpace, NGrid, NGridItem, useMessage
} from 'naive-ui'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { getPostBySlug } from '@/api/posts'
import { createPost, updatePost, type PostUpsertPayload } from '@/api/adminPosts'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const saving = ref(false)

const isEdit = computed(() => !!route.params.id)
const editId = computed(() => Number(route.params.id))

const form = reactive<PostUpsertPayload>({
  title: '',
  slug: '',
  contentMd: '',
  tags: [],
  status: 'DRAFT'
})
const tagsInput = ref('')

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre><code class="hljs language-${lang}">${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch {}
    }
    return `<pre><code class="hljs">${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const renderedHtml = computed(() => md.render(form.contentMd || ''))

async function loadExisting() {
  if (!isEdit.value) return
  // We need the post; backend provides only by-slug in public API. For simplicity in MVP,
  // admin edits load by fetching the post list and finding by id, then fetching by slug.
  const list = await (await import('@/api/posts')).listPublishedPosts(1, 200)
  const found = list.data.records.find((r) => r.id === editId.value)
  if (!found) {
    message.error('文章不存在')
    router.push({ name: 'admin-posts' })
    return
  }
  const detail = await getPostBySlug(found.slug)
  // We don't have the raw markdown in public detail; for MVP editing, allow re-fetching
  // via admin. For now, leave content empty and warn user.
  form.title = detail.data.title
  form.slug = detail.data.slug
  form.tags = detail.data.tags
  form.status = detail.data.publishedAt ? 'PUBLISHED' : 'DRAFT'
  tagsInput.value = (detail.data.tags || []).join(', ')
  message.warning('编辑模式目前不支持编辑 Markdown 正文（待后端扩展 /api/admin/posts/{id}）。')
}

onMounted(loadExisting)

async function onSubmit() {
  if (!form.title.trim() || !form.contentMd.trim()) {
    message.error('标题和正文不能为空')
    return
  }
  saving.value = true
  try {
    form.tags = tagsInput.value.split(',').map((t) => t.trim()).filter(Boolean)
    if (isEdit.value) {
      await updatePost(editId.value, form)
    } else {
      await createPost(form)
    }
    message.success('保存成功')
    router.push({ name: 'admin-posts' })
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.preview {
  background: #fff;
  border: 1px solid #e0e0e6;
  border-radius: 4px;
  padding: 12px 16px;
  height: 460px;
  overflow-y: auto;
}
</style>