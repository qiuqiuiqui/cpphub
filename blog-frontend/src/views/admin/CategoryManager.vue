<template>
  <n-card>
    <template #header>
      <span style="font-weight: 600;">栏目管理</span>
    </template>

    <n-form :model="form" label-placement="top" class="form-wrap" @submit.prevent="onCreate">
      <n-grid :cols="3" :x-gap="16" :y-gap="16" responsive="screen">
        <n-grid-item>
          <n-form-item label="栏目名称">
            <n-input v-model:value="form.name" placeholder="例如：Java" />
          </n-form-item>
        </n-grid-item>
        <n-grid-item>
          <n-form-item label="Slug">
            <n-input v-model:value="form.slug" placeholder="java" />
          </n-form-item>
        </n-grid-item>
        <n-grid-item>
          <n-form-item label="排序">
            <n-input-number v-model:value="form.sort" :min="0" />
          </n-form-item>
        </n-grid-item>
        <n-grid-item :span="3">
          <n-form-item label="上级栏目">
            <n-select
              v-model:value="form.parentId"
              :options="parentOptions"
              clearable
              placeholder="请选择上级栏目（可为空）"
            />
          </n-form-item>
        </n-grid-item>
      </n-grid>
      <n-space>
        <n-button type="primary" @click="onCreate">新增栏目</n-button>
      </n-space>
    </n-form>

    <div class="tree-wrap">
      <h3>已有栏目</h3>
      <ul class="tree-list">
        <li v-for="node in tree" :key="node.id" class="node">
          <div class="node-row">
            <div class="node-main">
              <span class="node-name">{{ node.name }}</span>
              <span class="node-badge">{{ node.parentId == null ? '一级' : '二级' }}</span>
            </div>
            <n-button size="tiny" @click="remove(node.id)">删除</n-button>
          </div>

          <ul v-if="node.children?.length" class="children">
            <li v-for="child in node.children" :key="child.id" class="node child-child">
              <div class="node-row child-row">
                <div class="node-main">
                  <span class="node-name">{{ child.name }}</span>
                  <span class="node-badge">子栏目</span>
                </div>
                <n-button size="tiny" @click="remove(child.id)">删除</n-button>
              </div>

              <ul v-if="child.children?.length" class="children grand-children">
                <li v-for="grand in child.children" :key="grand.id" class="node-row grand-row">
                  <div class="node-main">
                    <span class="node-name">{{ grand.name }}</span>
                    <span class="node-badge">三级</span>
                  </div>
                  <n-button size="tiny" @click="remove(grand.id)">删除</n-button>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  NCard, NForm, NFormItem, NInput, NInputNumber, NButton, NSpace, NSelect, NGrid, NGridItem, useMessage
} from 'naive-ui'
import { createCategory, deleteCategory, listAdminCategories, type Category } from '@/api/categories'

const message = useMessage()
const categories = ref<Category[]>([])
const form = reactive({ name: '', slug: '', parentId: null as number | null, sort: 0 })

type CategoryTreeNode = Category & { children?: CategoryTreeNode[] }

const parentOptions = computed(() => categories.value.map((c) => ({ label: c.name, value: c.id })))
const tree = computed(() => {
  const map = new Map<number, CategoryTreeNode>()
  const roots: CategoryTreeNode[] = []

  categories.value.forEach((c) => {
    map.set(c.id, { ...c, children: [] })
  })

  categories.value.forEach((c) => {
    if (c.parentId == null) {
      roots.push(map.get(c.id)!)
      return
    }

    const parent = map.get(c.parentId)
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(map.get(c.id)!)
    }
  })

  return roots
})

async function load() {
  const res = await listAdminCategories()
  categories.value = res.data || []
}

async function onCreate() {
  if (!form.name.trim()) {
    message.error('栏目名称不能为空')
    return
  }
  await createCategory({
    name: form.name.trim(),
    slug: form.slug.trim() || form.name.trim(),
    parentId: form.parentId,
    sort: form.sort,
    enabled: true
  })
  form.name = ''
  form.slug = ''
  form.parentId = null
  form.sort = 0
  message.success('栏目已创建')
  await load()
}

async function remove(id: number) {
  await deleteCategory(id)
  message.success('栏目已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.form-wrap {
  margin-bottom: 24px;
}
.tree-wrap {
  margin-top: 20px;
}
.tree-list {
  list-style: none;
  padding-left: 0;
  margin: 16px 0 0;
}
.node {
  margin-bottom: 10px;
}
.node-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
}
.node-main {
  display: flex;
  align-items: center;
  gap: 8px;
}
.node-name {
  font-weight: 600;
}
.node-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 11px;
  background: #eef5ff;
  color: #2f6fe5;
}
.child-row {
  margin-left: 18px;
  margin-top: 8px;
}
.child-child {
  margin-left: 18px;
}
.grand-children {
  margin-top: 8px;
  padding-left: 18px;
}
.grand-row {
  margin-left: 0;
}
.children {
  list-style: none;
  padding-left: 0;
}
</style>
