<template>
  <n-card :title="isEdit ? '编辑文章' : '新建文章'">
    <n-form :model="form" label-placement="top">
      <n-form-item label="标题" required>
        <n-input v-model:value="form.title" placeholder="文章标题" />
      </n-form-item>
      <n-form-item label="Slug（留空自动生成）">
        <n-input :value="form.slug" @update:value="(v) => form.slug = v" placeholder="my-post-slug" :disabled="isEdit" />
      </n-form-item>
      <n-form-item label="所属专栏">
        <n-select
          v-model:value="form.categoryId"
          :options="categoryOptions"
          clearable
          placeholder="请选择专栏，未选择将自动归入“其它”"
        />
      </n-form-item>
      <n-form-item label="新建专栏">
        <n-button size="small" @click="goToCategoryManager">前往栏目管理</n-button>
      </n-form-item>
      <n-form-item label="标签（逗号分隔）">
        <n-input v-model:value="tagsInput" placeholder="C++, 智能指针" />
      </n-form-item>
      <!-- 状态由底部按钮控制：保存草稿或保存并发布 -->
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
        <n-button @click="onSave('DRAFT')" :loading="savingDraft">保存草稿</n-button>
        <n-button type="primary" @click="onSave('PUBLISHED')" :loading="savingPublish">
          {{ isEdit && form.status === 'PUBLISHED' ? '更新已发布' : '直接发布' }}
        </n-button>
        <n-button @click="goBackToList">返回列表</n-button>
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
import { createPost, updatePost, getAdminPost, type PostUpsertPayload } from '@/api/adminPosts'
import { listCategories } from '@/api/categories'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const savingDraft = ref(false)
const savingPublish = ref(false)

const isEdit = computed(() => !!route.params.id)
const editId = computed(() => Number(route.params.id))

const form = reactive<PostUpsertPayload & { categoryId: number | null }>({
  title: '',
  slug: '',
  contentMd: '',
  tags: [],
  categoryId: null,
  status: 'DRAFT'
})
const tagsInput = ref('')
const categoryOptions = ref<Array<{ label: string; value: number }>>([])

const md: MarkdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: (str: string, lang?: string): string => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre><code class="hljs language-${lang}">${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch {}
    }
    return `<pre><code class="hljs">${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const renderedHtml = computed(() => md.render(form.contentMd || ''))

function getReturnRouteName(targetStatus?: 'DRAFT' | 'PUBLISHED') {
  const from = String(route.query.from || '')
  if (targetStatus === 'DRAFT') return 'admin-posts-drafts'
  if (targetStatus === 'PUBLISHED') return 'admin-posts-published'
  if (from === 'drafts') return 'admin-posts-drafts'
  if (from === 'published') return 'admin-posts-published'
  return form.status === 'PUBLISHED' ? 'admin-posts-published' : 'admin-posts-drafts'
}

function goBackToList() {
  router.push({ name: getReturnRouteName() })
}

function goToCategoryManager() {
  router.push({ name: 'admin-categories' })
}

async function loadCategories() {
  const res = await listCategories()
  categoryOptions.value = (res.data || []).map((category) => ({
    label: category.name,
    value: category.id
  }))
}

async function loadExisting() {
  if (!isEdit.value) return
  try {
    const res = await getAdminPost(editId.value)
    const p = res.data
    form.title = p.title
    form.slug = p.slug
    form.contentMd = p.contentMd ?? ''
    form.categoryId = p.categoryId ?? null
    form.status = (p.status as any) || 'DRAFT'
    tagsInput.value = (p.tags || []).join(', ')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载失败')
    router.push({ name: 'admin-posts' })
  }
}

onMounted(async () => {
  await loadCategories()
  await loadExisting()
})

async function onSave(targetStatus: 'DRAFT' | 'PUBLISHED') {
  if (!form.title.trim() || !form.contentMd.trim()) {
    message.error('标题和正文不能为空')
    return
  }
  const tags = tagsInput.value.split(',').map((t) => t.trim()).filter(Boolean)
  const payload: PostUpsertPayload = {
    title: form.title.trim(),
    slug: form.slug?.trim() || undefined,
    contentMd: form.contentMd,
    tags,
    categoryId: form.categoryId ?? undefined,
    status: targetStatus
  }
  const setter = targetStatus === 'PUBLISHED' ? (v: boolean) => (savingPublish.value = v) : (v: boolean) => (savingDraft.value = v)
  setter(true)
  try {
    if (isEdit.value) {
      await updatePost(editId.value, payload)
    } else {
      await createPost(payload)
    }
    if (targetStatus === 'PUBLISHED') {
      message.success('已发布，已返回发布文章列表')
      router.push({ name: 'admin-posts-published' })
    } else {
      message.success('已保存到草稿箱，文章已从已发布列表移出')
      router.push({ name: 'admin-posts-drafts' })
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally {
    setter(false)
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
