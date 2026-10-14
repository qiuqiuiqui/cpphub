<template>
  <n-card :title="isEdit ? '编辑文章' : '新建文章'">
    <n-form :model="form" label-placement="top">
      <n-form-item label="标题" required>
        <n-input v-model:value="form.title" placeholder="文章标题" />
      </n-form-item>
      <n-form-item label="Slug（留空自动生成）">
        <n-input :value="form.slug" @update:value="(v) => form.slug = v" placeholder="my-post-slug" :disabled="isEdit" />
      </n-form-item>
      <n-form-item>
        <template #label>
          <span style="display: inline-flex; align-items: center; gap: 8px;">
            <span>所属专栏</span>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button
                  circle
                  size="tiny"
                  quaternary
                  type="primary"
                  title="前往新建专栏"
                  aria-label="前往新建专栏"
                  @click="goToCategoryManager"
                >
                  <template #icon>
                    <n-icon :component="CreateOutline" />
                  </template>
                </n-button>
              </template>
              前往新建专栏
            </n-tooltip>
          </span>
        </template>
        <n-space align="center" style="width: 100%;">
          <n-select
            v-model:value="selectedLevel.level1"
            :options="level1Options"
            clearable
            placeholder="一级专栏"
            style="width: 180px"
            @update:value="onLevelChange('level1', $event)"
          />
          <n-select
            v-model:value="selectedLevel.level2"
            :options="level2Options"
            clearable
            placeholder="二级专栏"
            :disabled="!selectedLevel.level1"
            style="width: 180px"
            @update:value="onLevelChange('level2', $event)"
          />
          <n-select
            v-model:value="selectedLevel.level3"
            :options="level3Options"
            clearable
            placeholder="三级专栏"
            :disabled="!selectedLevel.level2"
            style="width: 180px"
            @update:value="onLevelChange('level3', $event)"
          />
        </n-space>
      </n-form-item>
      <n-form-item label="标签">
        <div class="tag-editor">
          <div class="tag-input-row">
            <div
              class="tag-display"
              tabindex="-1"
              @keydown.prevent
              @keyup.prevent
              @keypress.prevent
            >
              <template v-if="selectedTags.length">
                <n-tag
                  v-for="tag in selectedTags"
                  :key="tag.id"
                  :color="{ color: tag.color || '#4098FC', textColor: '#fff', borderColor: tag.color || '#4098FC' }"
                  closable
                  @close="removeSelectedTag(tag.id)"
                  class="tag-chip"
                >
                  {{ tag.name }}
                </n-tag>
              </template>
              <span v-else class="tag-placeholder">请从右侧添加标签</span>
            </div>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button circle quaternary type="primary" aria-label="选择标签" @click="openTagPicker">
                  <template #icon>
                    <n-icon :component="PricetagsOutline" />
                  </template>
                </n-button>
              </template>
              选择标签
            </n-tooltip>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button circle quaternary type="primary" aria-label="自建标签" @click="openCreateTagDialog">
                  <template #icon>
                    <n-icon :component="AddCircleOutline" />
                  </template>
                </n-button>
              </template>
              自建标签
            </n-tooltip>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button
                  circle
                  quaternary
                  type="error"
                  aria-label="清除标签"
                  :disabled="selectedTags.length === 0"
                  @click="clearSelectedTags"
                >
                  <template #icon>
                    <n-icon :component="TrashOutline" />
                  </template>
                </n-button>
              </template>
              清除标签
            </n-tooltip>
          </div>

          <n-modal
            v-model:show="tagPickerOpen"
            preset="card"
            title="选择标签"
            style="max-width: 520px;"
          >
            <n-input
              v-model:value="tagSearchKeyword"
              placeholder="搜索标签"
              clearable
              style="margin-bottom: 12px;"
            />
            <n-empty v-if="filteredTagOptions.length === 0" description="没有可选标签" />
            <div v-else class="tag-picker-grid">
              <n-tag
                v-for="option in filteredTagOptions"
                :key="option.value as number"
                :color="{ color: pickTagColor(option.value as number) + '22', textColor: pickTagColor(option.value as number), borderColor: pickTagColor(option.value as number) }"
                :bordered="false"
                checkable
                :checked="selectedTagIds.includes(option.value as number)"
                @update:checked="(checked: boolean) => toggleTagByOption(option.value as number, checked)"
                style="cursor: pointer;"
              >
                {{ option.label }}
              </n-tag>
            </div>
          </n-modal>
        </div>
      </n-form-item>

      <n-modal v-model:show="showTagDialog" preset="dialog" title="新增标签" positive-text="保存" negative-text="取消" @positive-click="saveNewTag">
        <n-form :model="tagDraft" label-placement="top">
          <n-form-item label="标签名称">
            <n-input v-model:value="tagDraft.name" placeholder="例如：C++ / Rust" />
          </n-form-item>
          <n-form-item label="标签颜色">
            <n-color-picker v-model:value="tagDraft.color" :modes="['hex']" />
          </n-form-item>
        </n-form>
      </n-modal>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NCard, NForm, NFormItem, NInput, NSelect, NButton, NSpace, NGrid, NGridItem, useMessage, NTooltip, NIcon,
  NTag, NModal, NColorPicker, NEmpty
} from 'naive-ui'
import {
  AddCircleOutline,
  CreateOutline,
  PricetagsOutline,
  TrashOutline
} from '@vicons/ionicons5'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { createPost, updatePost, getAdminPost, type PostUpsertPayload } from '@/api/adminPosts'
import { listCategories, type Category } from '@/api/categories'
import { createTag, listTags, type TagOption } from '@/api/tags'

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
const categories = ref<Category[]>([])
const allTags = ref<TagOption[]>([])
const selectedTagIds = ref<number[]>([])
const tagPickerOpen = ref(false)
const tagSearchKeyword = ref('')
const tagInput = ref('')
const showTagDialog = ref(false)
const tagDraft = reactive({ name: '', color: '#4098FC' })
const selectedLevel = reactive({
  level1: null as number | null,
  level2: null as number | null,
  level3: null as number | null,
})

const categoryMap = computed(() => new Map(categories.value.map((category) => [category.id, category])))
const tagOptions = computed(() => allTags.value.map((tag) => ({ label: tag.name, value: tag.id })))
const filteredTagOptions = computed(() => {
  const keyword = tagSearchKeyword.value.trim().toLowerCase()
  return tagOptions.value.filter((option) => {
    if (!keyword) return true
    return (option.label as string).toLowerCase().includes(keyword)
  })
})
const selectedTags = computed(() => {
  const map = new Map(allTags.value.map((tag) => [tag.id, tag]))
  return selectedTagIds.value
    .map((id) => map.get(id))
    .filter((tag): tag is TagOption => Boolean(tag))
})
const maxCategoryDepth = 3

const level1Options = computed(() => categories.value
  .filter((category) => (category.depth ?? 1) === 1)
  .map((category) => ({ label: category.name, value: category.id })))

const level2Options = computed(() => categories.value
  .filter((category) => (category.depth ?? 1) === 2 && category.parentId === selectedLevel.level1)
  .map((category) => ({ label: category.name, value: category.id })))

const level3Options = computed(() => categories.value
  .filter((category) => (category.depth ?? 1) === 3 && category.parentId === selectedLevel.level2)
  .map((category) => ({ label: category.name, value: category.id })))

function resolveCategoryPath(categoryId: number | null): Array<number | null> {
  if (categoryId == null) return [null, null, null]

  const path: Array<number | null> = [null, null, null]
  const chain: number[] = []
  let currentId: number | null = categoryId

  while (currentId != null) {
    const current = categoryMap.value.get(currentId)
    if (!current) break
    chain.unshift(currentId)
    currentId = current.parentId ?? null
  }

  for (let index = 0; index < chain.length && index < path.length; index += 1) {
    path[index] = chain[index]
  }

  return path
}

function updateCategoryFromSelection() {
  if (selectedLevel.level3 != null) {
    form.categoryId = selectedLevel.level3
    return
  }
  if (selectedLevel.level2 != null) {
    form.categoryId = selectedLevel.level2
    return
  }
  if (selectedLevel.level1 != null) {
    form.categoryId = selectedLevel.level1
    return
  }
  form.categoryId = null
}

function onLevelChange(levelName: 'level1' | 'level2' | 'level3', value: number | null) {
  if (levelName === 'level1') {
    selectedLevel.level2 = null
    selectedLevel.level3 = null
  }
  if (levelName === 'level2') {
    selectedLevel.level3 = null
  }
  updateCategoryFromSelection()
}

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

function syncSelectedTagIdsFromForm() {
  const map = new Map(allTags.value.map((tag) => [tag.name, tag.id]))
  selectedTagIds.value = form.tags
    .map((name) => map.get(name))
    .filter((id): id is number => typeof id === 'number')
}

function pickTagColor(id: number) {
  return allTags.value.find((tag) => tag.id === id)?.color || '#4098FC'
}

function openTagPicker() {
  tagSearchKeyword.value = ''
  tagPickerOpen.value = true
}

function openCreateTagDialog() {
  const trimmed = tagInput.value.trim()
  tagDraft.name = trimmed
  tagDraft.color = '#4098FC'
  showTagDialog.value = true
}

function toggleTagByOption(id: number, checked: boolean) {
  if (checked) {
    if (selectedTagIds.value.includes(id)) return
    selectedTagIds.value = [...selectedTagIds.value, id]
  } else {
    selectedTagIds.value = selectedTagIds.value.filter((item) => item !== id)
  }
  syncFormTagsFromSelection()
}

async function handleTagInputSubmit() {
  const name = tagInput.value.trim()
  if (!name) {
    message.warning('请先输入标签名称')
    return
  }
  const existing = allTags.value.find((tag) => tag.name === name)
  if (existing) {
    toggleTagByOption(existing.id, true)
    tagInput.value = ''
    message.success(`已选择标签：${name}`)
    return
  }
  try {
    const res = await createTag({ name, color: tagDraft.color || '#4098FC' })
    const created = res.data
    allTags.value = [created, ...allTags.value.filter((tag) => tag.id !== created.id)]
    if (!selectedTagIds.value.includes(created.id)) {
      selectedTagIds.value = [...selectedTagIds.value, created.id]
    }
    syncFormTagsFromSelection()
    tagInput.value = ''
    message.success(`已新建并选择标签：${created.name}`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '新增标签失败')
  }
}

function removeSelectedTag(id: number) {
  selectedTagIds.value = selectedTagIds.value.filter((item) => item !== id)
  syncFormTagsFromSelection()
}

function clearSelectedTags() {
  if (selectedTagIds.value.length === 0) return
  selectedTagIds.value = []
  syncFormTagsFromSelection()
}

function syncFormTagsFromSelection() {
  const map = new Map(allTags.value.map((tag) => [tag.id, tag.name]))
  form.tags = selectedTagIds.value
    .map((id) => map.get(id))
    .filter((name): name is string => Boolean(name))
}

async function loadTags() {
  const res = await listTags()
  allTags.value = res.data || []
  syncSelectedTagIdsFromForm()
}

async function saveNewTag() {
  const name = tagDraft.name.trim()
  if (!name) {
    message.warning('标签名称不能为空')
    return
  }

  try {
    const res = await createTag({ name, color: tagDraft.color || '#4098FC' })
    const created = res.data
    allTags.value = [created, ...allTags.value.filter((tag) => tag.id !== created.id)]
    if (!selectedTagIds.value.includes(created.id)) {
      selectedTagIds.value = [...selectedTagIds.value, created.id]
    }
    syncFormTagsFromSelection()
    showTagDialog.value = false
    tagDraft.name = ''
    tagDraft.color = '#4098FC'
    message.success('已新增标签')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '新增标签失败')
  }
}

async function loadCategories() {
  const res = await listCategories()
  categories.value = res.data || []
}

function syncSelectedCategoryPath(categoryId: number | null) {
  const [level1, level2, level3] = resolveCategoryPath(categoryId)
  selectedLevel.level1 = level1 ?? null
  selectedLevel.level2 = level2 ?? null
  selectedLevel.level3 = level3 ?? null
  updateCategoryFromSelection()
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
    syncSelectedCategoryPath(form.categoryId)
    form.status = (p.status as any) || 'DRAFT'
    form.tags = Array.from(new Set((p.tags || []).filter(Boolean)))
    syncSelectedTagIdsFromForm()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载失败')
    router.push({ name: 'admin-posts' })
  }
}

watch(
  () => [selectedLevel.level1, selectedLevel.level2, selectedLevel.level3],
  () => {
    updateCategoryFromSelection()
  },
  { flush: 'sync' }
)

onMounted(async () => {
  await loadCategories()
  await loadTags()
  if (!isEdit.value) {
    syncSelectedCategoryPath(form.categoryId)
  }
  await loadExisting()
})

async function onSave(targetStatus: 'DRAFT' | 'PUBLISHED') {
  if (!form.title.trim() || !form.contentMd.trim()) {
    message.error('标题和正文不能为空')
    return
  }
  const tags = Array.from(new Set(form.tags.map((t) => t.trim()).filter(Boolean)))
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

.tag-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tag-input-row {
  display: flex;
  align-items: stretch;
  gap: 8px;
  width: 100%;
}

.tag-display {
  flex: 1;
  min-width: 320px;
  min-height: 40px;
  padding: 6px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  cursor: default;
  user-select: none;
}

.tag-display:focus {
  outline: none;
}

.tag-display .tag-chip {
  margin: 0;
}

.tag-placeholder {
  color: #b8b8b8;
  font-size: 14px;
}

.tag-picker-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-input-row .n-button + .n-button {
  margin-left: 0;
}
</style>
