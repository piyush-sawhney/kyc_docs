import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/client'

interface User {
  id: string
  email: string
  fullName: string
  role: string
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

  async function loginInit(email: string) {
    const { data } = await api.post('/auth/login-init', { email })
    return data
  }

  async function verifyTotp(email: string, totpCode: string) {
    const { data } = await api.post('/auth/login', { email, totpCode })
    user.value = data.user
    return data
  }

  async function totpEnroll(enrollToken: string, totpCode: string) {
    const { data } = await api.post('/auth/totp/enroll', { enrollToken, totpCode })
    user.value = data.user
    return data
  }

  async function recoveryLogin(email: string, code: string) {
    const { data } = await api.post('/auth/recovery', { email, code })
    user.value = data.user
    return data
  }

  async function fetchRecoveryStatus() {
    try {
      const { data } = await api.get('/auth/recovery-codes/status')
      return data.hasUnusedCodes
    } catch {
      return true
    }
  }

  async function reEnroll() {
    const { data } = await api.post('/auth/totp/re-enroll')
    return data
  }

  async function reEnrollVerify(totpCode: string) {
    const { data } = await api.post('/auth/totp/re-enroll/verify', { totpCode })
    return data
  }

  async function logout() {
    await api.post('/auth/logout')
    user.value = null
  }

  return { user, initialized, fetchUser, loginInit, verifyTotp, totpEnroll, recoveryLogin, fetchRecoveryStatus, reEnroll, reEnrollVerify, logout }
})
