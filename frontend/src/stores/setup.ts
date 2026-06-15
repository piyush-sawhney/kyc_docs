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

  async function setup(email: string, password: string, fullName: string) {
    const { data } = await api.post('/setup', { email, password, fullName })
    needsSetup.value = false
    return data
  }

  return { needsSetup, initialized, checkStatus, setup }
})
