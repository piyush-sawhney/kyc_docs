<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSetupStore } from '../stores/setup'
import { useAuthStore } from '../stores/auth'
import KnapsLogo from '../components/KnapsLogo.vue'

const router = useRouter()
const setup = useSetupStore()
const auth = useAuthStore()
const step = ref<1 | 2 | 3>(1)
const email = ref('')
const fullName = ref('')
const totpCode = ref('')
const setupToken = ref('')
const qrDataUrl = ref('')
const loading = ref(false)
const error = ref('')
const recoveryCodes = ref<string[]>([])
const setupTokenResult = ref('')
const setupUserResult = ref<Record<string, unknown> | null>(null)

async function handleInit() {
  loading.value = true
  error.value = ''
  try {
    const result = await setup.setupInit(email.value, fullName.value)
    qrDataUrl.value = result.qrDataUrl
    setupToken.value = result.setupToken
    step.value = 2
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Setup failed'
  } finally {
    loading.value = false
  }
}

async function handleComplete() {
  loading.value = true
  error.value = ''
  try {
    const result = await setup.setupComplete(setupToken.value, totpCode.value)
    setupTokenResult.value = result.token
    setupUserResult.value = result.user as Record<string, unknown>
    recoveryCodes.value = result.recoveryCodes
    step.value = 3
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Setup failed'
  } finally {
    loading.value = false
  }
}

function handleConfirmAndGo() {
  if (setupTokenResult.value && setupUserResult.value) {
    auth.token = setupTokenResult.value
    localStorage.setItem('token', setupTokenResult.value)
    auth.user = setupUserResult.value as { id: string; email: string; fullName: string; role: string }
  }
  const blob = new Blob([recoveryCodes.value.join('\n')], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
  router.push('/')
}

async function copyCode(code: string) {
  try {
    await navigator.clipboard.writeText(code)
  } catch { /* ignore */ }
}
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-center"
      style="min-height: 100vh; background: linear-gradient(135deg, #1E3A5F 0%, #0F172A 100%);">
      <div class="card border-0 shadow-lg" style="max-width: 520px; width: 100%; border-radius: 16px;">
        <div class="card-body p-5">
          <div v-if="step === 1" class="text-center">
            <div class="d-flex justify-content-center mb-3">
              <KnapsLogo size="md" />
            </div>
            <h5 class="fw-bold" style="color: #1E3A5F;">First-Time Setup</h5>
            <p class="text-muted small mb-4">Create the administrator account to get started</p>

            <div class="alert alert-info d-flex align-items-center py-2 px-3 small mb-4 text-start" role="alert">
              <i class="bi bi-info-circle me-2"></i> No users exist yet. Set up your admin account to get started.
            </div>

            <form @submit.prevent="handleInit">
              <div class="mb-3 text-start">
                <label class="form-label">Full Name</label>
                <input type="text" class="form-control" v-model="fullName" :disabled="loading" />
              </div>
              <div class="mb-3 text-start">
                <label class="form-label">Email</label>
                <input type="email" class="form-control" v-model="email" :disabled="loading" />
              </div>

              <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

              <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                Continue <i class="bi bi-arrow-right ms-1"></i>
              </button>
            </form>
          </div>

          <div v-if="step === 2" class="text-center">
            <div class="d-flex justify-content-center mb-3">
              <KnapsLogo size="md" />
            </div>
            <h5 class="fw-bold" style="color: #1E3A5F;">Set Up Authenticator</h5>
            <p class="text-muted small mb-3">Scan this QR code with Google Authenticator</p>

            <div class="d-flex justify-content-center mb-3">
              <img :src="qrDataUrl" alt="TOTP QR Code" style="width: 200px; height: 200px; border-radius: 8px;" />
            </div>

            <form @submit.prevent="handleComplete">
              <div class="mb-3">
                <label class="form-label text-start d-block">Enter 6-digit code from app</label>
                <input type="text" class="form-control text-center fs-5" v-model="totpCode"
                  placeholder="000000" maxlength="6" inputmode="numeric" pattern="[0-9]*" required />
              </div>

              <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

              <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                <i class="bi bi-check me-1"></i> Verify & Complete Setup
              </button>
            </form>
          </div>

          <div v-if="step === 3" class="text-center">
            <div class="d-inline-flex align-items-center justify-content-center mb-3"
              style="width: 56px; height: 56px; border-radius: 50%; background: rgba(22,163,74,0.1);">
              <i class="bi bi-check-circle" style="font-size: 28px; color: #16A34A;"></i>
            </div>
            <h5 class="fw-bold" style="color: #16A34A;">Setup Complete!</h5>
            <p class="text-muted small mb-3">Download your recovery codes below.</p>

            <div class="alert alert-warning d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
              <i class="bi bi-exclamation-triangle me-2"></i>
              Save these codes. Each code can only be used once.
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

            <button class="btn btn-primary btn-lg w-100" @click="handleConfirmAndGo">
              <i class="bi bi-download me-1"></i> Download Recovery Codes
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
