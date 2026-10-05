<template>
  <div class="login-page">
    <n-card title="登录管理后台" class="login-card">
      <div class="top-bar">
        <router-link to="/" class="back-home">← 返回首页</router-link>
      </div>
      <n-form ref="formRef" :model="form" :rules="rules" @keyup.enter="onSubmit">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="form.username" placeholder="admin" />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input v-model:value="form.password" type="password" show-password-on="click" placeholder="admin123" />
        </n-form-item>
        <n-button type="primary" block @click="onSubmit" :loading="loading">登录</n-button>
      </n-form>
      <p class="hint">默认账号：admin / admin123</p>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/useAuth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const message = useMessage()
const loading = ref(false)

const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

async function onSubmit() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/admin/posts'
    router.push(redirect)
  } catch (e: any) {
    message.error(e?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 380px;
  border-radius: 12px;
}
.top-bar {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 12px;
}
.back-home {
  color: #2080f0;
  font-size: 13px;
  text-decoration: none;
}
.back-home:hover {
  text-decoration: underline;
}
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
  text-align: center;
}
</style>