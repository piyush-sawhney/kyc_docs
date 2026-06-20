import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/client'

export const useSetupStore = defineStore('setup', () => {
  const needsSetup = ref(false)
  const initialized = ref(false)

  async function checkStatus() {
    try {
      const { data } = await api.get('/setup/status')
      needsSetup.value = (data as Record<string, boolean>).needsSetup
    } catch {
      needsSetup.value = true
    } finally {
      initialized.value = true
    }
  }

  async function setupInit(email: string, fullName: string) {
    const { data } = await api.post('/setup/init', { email, fullName })
    needsSetup.value = false
    return data
  }

  async function setupComplete(setupToken: string, totpCode: string) {
    const { data } = await api.post('/setup/complete', { setupToken, totpCode })
    needsSetup.value = false
    return data
  }

  return { needsSetup, initialized, checkStatus, setupInit, setupComplete }
})
