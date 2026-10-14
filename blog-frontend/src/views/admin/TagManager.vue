<template>
  <n-card>
    <template #header>
      <span style="font-weight: 600;">标签管理</span>
    </template>

    <div class="toolbar">
      <n-button type="primary" @click="openCreateDialog">新增标签</n-button>
    </div>

    <div v-if="tags.length === 0" class="empty-state">暂无标签</div>
    <div v-else class="tag-list">
      <div v-for="tag in tags" :key="tag.id" class="tag-row">
        <div class="tag-name-block">
          <span class="color-swatch" :style="{ background: tag.color || '#4098FC' }" />
          <span>{{ tag.name }}</span>
        </div>
        <div class="tag-meta">{{ tag.useCount ?? 0 }} 篇文章</div>
        <div class="tag-actions">
          <n-button size="small" @click="openEditDialog(tag)">编辑</n-button>
          <n-button size="small" type="error" secondary @click="removeTag(tag.id)">删除</n-button>
        </div>
      </div>
    </div>

    <n-modal v-model:show="showDialog" preset="dialog" :title="editingId ? '编辑标签' : '新增标签'" positive-text="保存" negative-text="取消" @positive-click="submitTag">
      <n-form :model="form" label-placement="top" label-width="80">
        <n-form-item label="名称">
          <n-input v-model:value="form.name" placeholder="例如 C++ / Rust" />
        </n-form-item>
        <n-form-item label="颜色">
          <n-color-picker v-model:value="form.color" :modes="['hex']" />
        </n-form-item>
      </n-form>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { NButton, NCard, NColorPicker, NForm, NFormItem, NInput, NModal, useMessage } from 'naive-ui'
import { createTag, deleteTag, listAdminTags, updateTag, type TagOption } from '@/api/tags'

const message = useMessage()
const tags = ref<TagOption[]>([])
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', color: '#4098FC' })

async function loadTags() {
  const res = await listAdminTags()
  tags.value = res.data || []
}

function openCreateDialog() {
  editingId.value = null
  form.name = ''
  form.color = '#4098FC'
  showDialog.value = true
}

function openEditDialog(tag: TagOption) {
  editingId.value = tag.id
  form.name = tag.name
  form.color = tag.color || '#4098FC'
  showDialog.value = true
}

async function submitTag() {
  const name = form.name.trim()
  if (!name) {
    message.warning('标签名称不能为空')
    return
  }

  try {
    if (editingId.value) {
      const res = await updateTag(editingId.value, { name, color: form.color })
      message.success('标签已更新')
      const index = tags.value.findIndex((item) => item.id === res.data.id)
      if (index >= 0) tags.value.splice(index, 1, res.data)
    } else {
      const res = await createTag({ name, color: form.color })
      message.success('标签已创建')
      tags.value.unshift(res.data)
    }
    showDialog.value = false
    await loadTags()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  }
}

async function removeTag(id: number) {
  if (!window.confirm('确认删除这个标签吗？它将从所有文章中移除。')) {
    return
  }

  try {
    await deleteTag(id)
    message.success('标签已删除')
    tags.value = tags.value.filter((tag) => tag.id !== id)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

onMounted(loadTags)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.tag-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.tag-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
}
.tag-name-block {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.color-swatch {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.08);
}
.tag-meta {
  color: #666;
  font-size: 13px;
}
.tag-actions {
  display: flex;
  gap: 8px;
}
.empty-state {
  color: #999;
  padding: 20px 0;
}
</style>
