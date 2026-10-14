<template>
  <n-card content-style="padding: 16px;">
    <template #header>
      <span style="font-weight: 600;">栏目管理</span>
    </template>

    <n-alert v-if="loadError" type="error" :title="'加载失败'" closable @close="loadError = ''" style="margin-bottom: 12px;">
      {{ loadError }}
    </n-alert>

    <n-spin :show="loading">

    <div class="manager-toolbar">
      <div class="breadcrumb-wrap">
        <button v-if="currentPath.length > 0" class="crumb go-up" @click="goToParent">↑ 上级</button>
        <button class="crumb root" @click="goToPath([])">/</button>
        <template v-for="(item, index) in breadcrumbs" :key="item.id">
          <span class="crumb-separator">/</span>
          <button class="crumb" @click="goToPath(item.path)">{{ item.name }}</button>
        </template>
      </div>

      <div class="toolbar-actions">
        <n-button :disabled="selectedIds.length === 0" size="small" @click="deleteSelected">删除选中</n-button>
        <n-button type="primary" size="small" :disabled="currentDepth >= maxCategoryDepth" @click="addCurrentLevel">
          新增{{ currentDepth === 0 ? '一级' : currentDepth === 1 ? '二级' : '三级' }}专栏
        </n-button>
      </div>
    </div>

    <div v-if="currentDepth >= maxCategoryDepth" class="limit-tip">
      最多三级栏目，当前已达到最大层级，无法继续新增。
    </div>
</n-spin>

    <div class="list-wrap">
      <div class="list-header">
        <span>已有栏目</span>
        <span class="count">{{ visibleCategories.length }} 项</span>
      </div>

      <div v-if="visibleCategories.length === 0" class="empty-state">
        当前目录为空
      </div>

      <div v-else class="category-list">
        <div
          v-for="category in visibleCategories"
          :key="category.id"
          class="category-item"
          :class="{ selected: selectedIds.includes(category.id) }"
        >
          <n-checkbox
            :checked="selectedIds.includes(category.id)"
            @update:checked="(checked) => toggleSelect(category.id, checked)"
          />

          <div class="category-content" @click="enterCategory(category)">
            <div class="category-main">
              <span class="category-name">{{ category.name }}</span>
              <span class="level-badge">{{ getLevelLabel(category.depth ?? 1) }}</span>
            </div>
          </div>

          <div class="item-actions">
            <n-button title="新增子栏目" size="tiny" circle :disabled="(category.depth ?? 1) >= maxCategoryDepth" @click.stop="addChild(category)">
              +
            </n-button>
            <n-button title="修改标题" size="tiny" circle @click.stop="renameCategory(category)">✎</n-button>
            <n-button title="删除" size="tiny" circle @click.stop="remove(category.id)">×</n-button>
          </div>
        </div>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import {
  NAlert, NButton, NCard, NCheckbox, NInput, NSpin, useDialog, useMessage
} from 'naive-ui'
import { createCategory, deleteCategory, listAdminCategories, updateCategory, type Category } from '@/api/categories'

const message = useMessage()
const dialog = useDialog()
const categories = ref<Category[]>([])
const selectedIds = ref<number[]>([])
const currentPath = ref<number[]>([])
const loading = ref(false)
const loadError = ref('')

const maxCategoryDepth = 3
const currentDepth = computed(() => currentPath.value.length)
const currentParentId = computed(() => currentPath.value[currentPath.value.length - 1] ?? null)

const categoryMap = computed(() => new Map(categories.value.map((category) => [category.id, category])))

const breadcrumbs = computed(() => {
  const result: Array<{ id: number; name: string; path: number[] }> = []
  let path: number[] = []

  for (const id of currentPath.value) {
    const item = categoryMap.value.get(id)
    if (!item) break
    path = [...path, id]
    result.push({ id: item.id, name: item.name, path: [...path] })
  }

  return result
})

const visibleCategories = computed(() => {
  return [...categories.value]
    .filter((category) => category.parentId === currentParentId.value)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
})

function getLevelLabel(depth: number) {
  if (depth === 1) return '一级'
  if (depth === 2) return '二级'
  return '三级'
}

function askForName(title: string, placeholder: string, initialValue = ''): Promise<string | null> {
  return new Promise((resolve) => {
    const inputRef = ref<InstanceType<typeof NInput> | null>(null)
    const valueRef = ref(initialValue)

    const inputView = ref(valueRef.value)
    const tryConfirm = () => {
      const trimmed = valueRef.value.trim()
      if (!trimmed) {
        message.warning('名称不能为空')
        return
      }
      resolve(trimmed)
      dialogReactive.destroy()
    }

    const dialogReactive = dialog.create({
      title,
      showIcon: false,
      positiveText: '保存',
      negativeText: '取消',
      closable: true,
      maskClosable: false,
      content: () =>
        h(NInput, {
          ref: inputRef,
          value: inputView.value,
          placeholder,
          autofocus: true,
          clearable: true,
          'onUpdate:value': (value: string) => {
            inputView.value = value
            valueRef.value = value
          },
          onKeyup: (event: KeyboardEvent) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              tryConfirm()
            }
          }
        }),
      onPositiveClick: () => {
        const trimmed = valueRef.value.trim()
        if (!trimmed) {
          message.warning('名称不能为空')
          return false
        }
        resolve(trimmed)
        return true
      },
      onNegativeClick: () => {
        resolve(null)
      },
      onClose: () => {
        resolve(null)
      }
    })

    setTimeout(() => {
      const inst = inputRef.value as unknown as { focus?: () => void } | null
      inst?.focus?.()
    }, 50)
  })
}

function confirmAction(title: string, content: string): Promise<boolean> {
  return new Promise((resolve) => {
    dialog.warning({
      title,
      content,
      positiveText: '确认',
      negativeText: '取消',
      closable: true,
      maskClosable: false,
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false)
    })
  })
}

function goToPath(path: number[]) {
  currentPath.value = [...path]
  selectedIds.value = []
}

function goToParent() {
  if (currentPath.value.length === 0) return
  currentPath.value = currentPath.value.slice(0, -1)
  selectedIds.value = []
}

function enterCategory(category: Category) {
  const depth = category.depth ?? 1
  if (depth >= maxCategoryDepth) {
    return
  }
  currentPath.value = [...currentPath.value, category.id]
  selectedIds.value = []
}

function toggleSelect(id: number, checked: boolean) {
  if (checked) {
    selectedIds.value = [...new Set([...selectedIds.value, id])]
    return
  }
  selectedIds.value = selectedIds.value.filter((item) => item !== id)
}

async function addCurrentLevel() {
  if (currentDepth.value >= maxCategoryDepth) {
    message.warning('最多三级栏目，当前已达到最大层级')
    return
  }
  const name = await askForName('新增栏目', '请输入栏目名称')
  if (!name) {
    return
  }
  try {
    await createCategory({
      name,
      parentId: currentParentId.value,
      enabled: true
    })
    message.success('栏目已创建')
    await load()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '创建失败')
  }
}

async function addChild(category: Category) {
  if ((category.depth ?? 1) >= maxCategoryDepth) {
    message.warning('最多三级栏目，当前栏目不能再新增子栏目')
    return
  }
  const name = await askForName('新增子栏目', '请输入子栏目名称')
  if (!name) {
    return
  }
  try {
    await createCategory({
      name,
      parentId: category.id,
      enabled: true
    })
    message.success('子栏目已创建')
    await load()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '创建失败')
  }
}

async function renameCategory(category: Category) {
  const nextName = await askForName('修改栏目名称', '请输入新的栏目名称', category.name)
  if (!nextName) {
    return
  }
  try {
    await updateCategory(category.id, { name: nextName })
    message.success('栏目已更新')
    await load()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '更新失败')
  }
}

async function deleteSelected() {
  if (selectedIds.value.length === 0) {
    return
  }
  const confirmed = await confirmAction(
    '删除所选栏目',
    `确认删除 ${selectedIds.value.length} 个栏目吗？此操作不可撤销。`
  )
  if (!confirmed) {
    return
  }

  try {
    for (const id of selectedIds.value) {
      await deleteCategory(id)
    }
    selectedIds.value = []
    message.success('已删除选中的栏目')
    await load()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

async function remove(id: number) {
  const confirmed = await confirmAction('删除栏目', '确认删除当前栏目吗？此操作不可撤销。')
  if (!confirmed) {
    return
  }
  try {
    await deleteCategory(id)
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
    message.success('栏目已删除')
    await load()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listAdminCategories()
    categories.value = res.data || []
  } catch (error: any) {
    loadError.value = error?.response?.data?.message || error?.message || '栏目数据加载失败'
    message.error(loadError.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.manager-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #ececf3;
  border-radius: 12px;
  background: #fafbff;
  margin-bottom: 16px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.crumb {
  border: none;
  background: transparent;
  color: #2d3a4a;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 13px;
}

.go-up {
  background: #eef3ff;
  color: #2747b3;
  font-weight: 600;
}

.crumb:hover {
  background: #eef3ff;
}

.crumb.root {
  font-weight: 700;
}

.crumb-separator {
  color: #9aa4b2;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.list-wrap {
  border: 1px solid #ececf3;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f5;
  background: #fafafa;
  font-weight: 600;
}

.count {
  color: #7a8192;
  font-size: 12px;
  font-weight: 500;
}

.empty-state {
  padding: 32px 16px;
  text-align: center;
  color: #7a8192;
}

.category-list {
  display: flex;
  flex-direction: column;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f5;
  transition: background 0.2s ease;
}

.category-item:hover {
  background: #fafcff;
}

.category-item.selected {
  background: #f4f8ff;
}

.category-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.category-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-name {
  font-size: 15px;
  font-weight: 600;
  color: #1d2430;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef3ff;
  color: #446fe7;
  font-size: 11px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.limit-tip {
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #fff7e6;
  color: #a05f00;
  border: 1px solid #f4d79b;
  font-size: 13px;
}
</style>
