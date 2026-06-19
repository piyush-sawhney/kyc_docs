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
  const token = ref<string | null>(null)
  const initialized = ref(false)

  async function fetchUser() {
    const saved = localStorage.getItem('token')
    if (!saved) {
      initialized.value = true
      return
    }
    token.value = saved
    try {
      const { data } = await api.get('/auth/me')
      user.value = data as User
    } catch {
      user.value = null
      token.value = null
      localStorage.removeItem('token')
    } finally {
      initialized.value = true
    }
  }

  async function loginInit(email: string) {
    const { data } = await api.post('/auth/login/init', { email })
    return data
  }

  async function verifyTotp(email: string, totpCode: string) {
    const { data } = await api.post('/auth/login', { email, totpCode })
    token.value = data.token as string
    localStorage.setItem('token', data.token as string)
    user.value = data.user as User
    return data
  }

  async function totpEnroll(enrollToken: string, totpCode: string) {
    const { data } = await api.post('/auth/enroll', { enrollToken, totpCode })
    token.value = data.token as string
    localStorage.setItem('token', data.token as string)
    user.value = data.user as User
    return data
  }

  async function recoveryLogin(email: string, code: string) {
    const { data } = await api.post('/auth/login/recovery', { email, recoveryCode: code })
    token.value = data.token as string
    localStorage.setItem('token', data.token as string)
    user.value = data.user as User
    return data
  }

  async function fetchRecoveryStatus() {
    try {
      const { data } = await api.get('/auth/recovery-codes/status')
      return (data as Record<string, boolean>).hasUnusedCodes
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

  async function resumeSetup(email: string, totpCode: string) {
    const { data } = await api.post('/auth/resume-setup', { email, totpCode })
    return data
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } finally {
      user.value = null
      token.value = null
      localStorage.removeItem('token')
    }
  }

  return { user, token, initialized, fetchUser, loginInit, verifyTotp, totpEnroll, recoveryLogin, resumeSetup, fetchRecoveryStatus, reEnroll, reEnrollVerify, logout }
})
