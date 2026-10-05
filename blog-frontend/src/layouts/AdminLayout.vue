<template>
  <n-layout class="admin-layout" has-sider>
    <n-layout-sider bordered :width="220" content-style="padding: 0;">
      <div class="logo">C++ Hub 管理后台</div>
      <n-menu
        :options="menuOptions"
        :value="activeKey"
        @update:value="handleMenuSelect"
      />
    </n-layout-sider>
    <n-layout>
      <n-layout-header bordered class="header">
        <span class="title">{{ route.meta.title || '管理' }}</span>
        <n-space>
          <n-tag type="info">{{ auth.username }}</n-tag>
          <n-button size="small" @click="onLogout">退出登录</n-button>
        </n-space>
      </n-layout-header>
      <n-layout-content class="content">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NLayout, NLayoutSider, NLayoutHeader, NLayoutContent, NMenu, NSpace, NTag, NButton, useMessage } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { useAuthStore } from '@/stores/useAuth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const message = useMessage()

const menuOptions: MenuOption[] = [
  { label: '新建文章', key: 'admin-post-new' },
  {
    label: '文章管理',
    key: 'article-management',
    children: [
      { label: '发布文章管理', key: 'admin-posts-published' },
      { label: '草稿箱管理', key: 'admin-posts-drafts' }
    ]
  },
  { label: '栏目管理', key: 'admin-categories' }
]

const activeKey = computed(() => {
  const name = String(route.name || '')
  if (name === 'admin-post-new') return 'admin-post-new'
  if (name === 'admin-posts-published') return 'admin-posts-published'
  if (name === 'admin-posts-drafts') return 'admin-posts-drafts'
  if (name === 'admin-categories') return 'admin-categories'
  return 'admin-posts-published'
})

function handleMenuSelect(key: string) {
  if (!key || key === 'article-management') return
  router.push({ name: key as any })
}

async function onLogout() {
  await auth.logout()
  message.success('已退出登录')
  router.push({ name: 'admin-login' })
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.logo {
  padding: 16px;
  font-weight: 700;
  font-size: 15px;
  color: #2080f0;
  border-bottom: 1px solid #eee;
}
.header {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
}
.title {
  font-weight: 600;
}
.content {
  padding: 24px;
  background: #fafafa;
}
</style>