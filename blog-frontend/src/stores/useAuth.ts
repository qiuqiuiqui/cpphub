import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const username = ref<string | null>(null)
  const role = ref<string | null>(null)
  const ready = ref(false)

  async function fetchMe() {
    try {
      const res = await authApi.me()
      username.value = res.data.username
      role.value = res.data.role
    } catch {
      username.value = null
      role.value = null
    } finally {
      ready.value = true
    }
  }

  async function login(usernameInput: string, password: string) {
    const res = await authApi.login({ username: usernameInput, password })
    username.value = res.data.username
    role.value = res.data.role
    return res
  }

  async function logout() {
    await authApi.logout()
    username.value = null
    role.value = null
  }

  const isAdmin = () => role.value === 'ADMIN'

  return { username, role, ready, fetchMe, login, logout, isAdmin }
})