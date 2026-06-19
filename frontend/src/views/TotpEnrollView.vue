<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import KnapsLogo from '../components/KnapsLogo.vue'
import api from '../api/client'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const totpCode = ref('')
const qrDataUrl = ref('')
const loading = ref(false)
const error = ref('')
const recoveryCodes = ref<string[]>([])
const step = ref<'qr' | 'codes'>('qr')

onMounted(async () => {
  const token = route.query.token as string
  const email = route.query.email as string
  if (!token) {
    router.push('/login')
    return
  }
  try {
    const { data } = await api.get('/auth/qr', { params: { email } })
    qrDataUrl.value = data.qrDataUrl
  } catch {
    error.value = 'Invalid or expired enrollment link. Please log in again.'
    setTimeout(() => router.push('/login'), 2000)
  }
})

async function handleEnroll() {
  loading.value = true
  error.value = ''
  try {
    const token = route.query.token as string
    const result = await auth.totpEnroll(token, totpCode.value)
    if (result.recoveryCodes) {
      recoveryCodes.value = result.recoveryCodes
      step.value = 'codes'
    } else {
      router.push('/')
    }
  } catch (err: any) {
    error.value = err?.response?.data?.message || 'Invalid code'
  } finally {
    loading.value = false
  }
}

function downloadCodes() {
  const blob = new Blob([recoveryCodes.value.join('\n')], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
}

function goToDashboard() {
  router.push('/')
}

async function copyCode(code: string) {
  try {
    await navigator.clipboard.writeText(code)
  } catch { /* ignore */ }
}
</script>

<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center"
    style="background: linear-gradient(135deg, #1E3A5F 0%, #0F172A 100%);">
    <div class="card border-0 shadow-lg" style="max-width: 480px; width: 100%; border-radius: 16px;">
      <div class="card-body p-5 text-center">
        <template v-if="step === 'qr'">
          <div class="d-flex justify-content-center mb-3">
            <KnapsLogo size="md" />
          </div>
          <h5 class="fw-bold" style="color: #1E3A5F;">Set Up Authenticator</h5>
          <p class="text-muted small mb-3">Scan this QR code with Google Authenticator</p>

          <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

          <div v-if="qrDataUrl" class="d-flex justify-content-center mb-3">
            <img :src="qrDataUrl" alt="TOTP QR Code" style="width: 200px; height: 200px; border-radius: 8px;" />
          </div>
          <div v-else class="d-flex justify-content-center mb-3">
            <div class="spinner-border text-primary" role="status"></div>
          </div>

          <form @submit.prevent="handleEnroll">
            <div class="mb-3">
              <label class="form-label text-start d-block">Enter 6-digit code from app</label>
              <input type="text" class="form-control text-center fs-5" v-model="totpCode"
                placeholder="000000" maxlength="6" inputmode="numeric" pattern="[0-9]*" required />
            </div>
            <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading || !qrDataUrl">
              <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
              <i class="bi bi-check me-1"></i> Verify & Complete
            </button>
          </form>
        </template>

        <template v-else>
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 56px; height: 56px; border-radius: 50%; background: rgba(22,163,74,0.1);">
            <i class="bi bi-check-circle" style="font-size: 28px; color: #16A34A;"></i>
          </div>
          <h5 class="fw-bold" style="color: #16A34A;">Enrollment Complete!</h5>

          <div class="alert alert-warning d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            Save these recovery codes. Each code can be used once.
          </div>

          <div class="card bg-light border-0 mb-3">
            <div class="card-body py-3">
              <div v-for="(code, i) in recoveryCodes" :key="i"
                class="d-flex align-items-center justify-content-center gap-2 py-1">
                <code class="fs-5 fw-bold" style="color: #1E3A5F; letter-spacing: 1px;">{{ code }}</code>
                <button class="btn btn-sm btn-outline-secondary border-0" @click="copyCode(code)" title="Copy code">
                  <i class="bi bi-clipboard"></i>
                </button>
              </div>
            </div>
          </div>

          <div class="d-flex gap-2">
            <button class="btn btn-outline-primary flex-fill" @click="downloadCodes">
              <i class="bi bi-download me-1"></i> Download Codes
            </button>
            <button class="btn btn-primary flex-fill" @click="goToDashboard">
              <i class="bi bi-box-arrow-in-right me-1"></i> Go to Dashboard
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
