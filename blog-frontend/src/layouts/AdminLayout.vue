<template>
  <n-layout class="admin-layout" has-sider>
    <n-layout-sider bordered :width="220" content-style="padding: 0;">
      <div class="logo">C++ Hub 管理后台</div>
      <n-menu
        :options="menuOptions"
        :value="activeKey"
        @update:value="(k) => router.push({ name: k })"
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
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NLayout, NLayoutSider, NLayoutHeader, NLayoutContent, NMenu, NSpace, NTag, NButton, useMessage } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { useAuthStore } from '@/stores/useAuth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const message = useMessage()

const menuOptions: MenuOption[] = [
  { label: '文章管理', key: 'admin-posts' },
  { label: '新建文章', key: 'admin-post-new' },
  { label: 'C++ 环境检测', key: 'admin-env-check' }
]

const activeKey = computed(() => (route.name as string) || 'admin-posts')

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