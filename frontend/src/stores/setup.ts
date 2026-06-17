import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/client'

export const useSetupStore = defineStore('setup', () => {
  const needsSetup = ref(false)
  const initialized = ref(false)

  async function checkStatus() {
    try {
      const { data } = await api.get('/setup/status')
      needsSetup.value = data.needsSetup
    } catch {
      needsSetup.value = false
    } finally {
      initialized.value = true
    }
  }

  async function setupInit(email: string, fullName: string) {
    const { data } = await api.post('/setup/init', { email, fullName })
    needsSetup.value = false
    return data
  }

  async function setupVerify(setupToken: string, totpCode: string) {
    const { data } = await api.post('/setup/verify', { setupToken, totpCode })
    return data
  }

  return { needsSetup, initialized, checkStatus, setupInit, setupVerify }
})
