<template>
  <div class="layout">
    <header class="header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="logo">⚙️</span>
          <span class="brand-name">C++ Hub</span>
        </router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/search">搜索</router-link>
          <router-link to="/admin/posts" v-if="auth.isAdmin()">管理</router-link>
          <router-link to="/admin/login" v-else>登录</router-link>
        </nav>
      </div>
    </header>
    <main class="main">
      <router-view />
    </main>
    <footer class="footer">
      <div class="container">
        <p>© 2026 C++ Hub · 个人 C++ 学习笔记与技术分享</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/useAuth'
const auth = useAuthStore()
onMounted(() => auth.fetchMe())
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.header {
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 18px;
  color: #2080f0;
}
.brand:hover {
  text-decoration: none;
}
.logo {
  font-size: 22px;
}
.nav {
  display: flex;
  gap: 20px;
}
.nav a {
  color: #555;
}
.nav a.router-link-exact-active {
  color: #2080f0;
  font-weight: 600;
}
.main {
  flex: 1;
}
.footer {
  background: #f5f5f5;
  border-top: 1px solid #eee;
  padding: 20px 0;
  text-align: center;
  color: #999;
  font-size: 13px;
}
</style>