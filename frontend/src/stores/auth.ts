import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/client'

interface User {
  id: string
  email: string
  fullName: string
  role: string
  isActive?: boolean
  mustChangePassword?: boolean
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const initialized = ref(false)

  async function fetchUser() {
    try {
      const { data } = await api.get('/auth/me')
      user.value = data
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(email: string, password: string) {
    const { data } = await api.post('/auth/login', { email, password })
    user.value = data.user
    return data
  }

  async function recoveryLogin(email: string, code: string) {
    const { data } = await api.post('/auth/recovery', { email, code })
    user.value = data.user
    return data
  }

  async function changePassword(currentPassword: string, newPassword: string) {
    await api.post('/auth/change-password', { currentPassword, newPassword })
    if (user.value) user.value.mustChangePassword = false
  }

  async function logout() {
    await api.post('/auth/logout')
    user.value = null
  }

  return { user, initialized, fetchUser, login, recoveryLogin, changePassword, logout }
})
