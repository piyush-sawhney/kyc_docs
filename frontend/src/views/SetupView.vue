<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSetupStore } from '../stores/setup'
import PasswordField from '../components/PasswordField.vue'

const router = useRouter()
const setup = useSetupStore()
const step = ref(1)
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const fullName = ref('')
const loading = ref(false)
const error = ref('')
const recoveryCodes = ref<string[]>([])

async function handleSetup() {
  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const result = await setup.setup(email.value, password.value, fullName.value)
    recoveryCodes.value = result.recoveryCodes
    step.value = 3
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Setup failed'
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

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="d-flex align-items-center justify-content-center"
    style="min-height: 100vh; background: linear-gradient(135deg, #1E3A5F 0%, #0F172A 100%);">
    <div class="card border-0 shadow-lg" style="max-width: 520px; width: 100%; border-radius: 16px;">
      <div class="card-body p-5">
        <div v-if="step === 1" class="text-center">
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 56px; height: 56px; border-radius: 50%; background: rgba(30, 58, 95, 0.08);">
            <i class="bi bi-tools" style="font-size: 28px; color: #1E3A5F;"></i>
          </div>
          <h5 class="fw-bold" style="color: #1E3A5F;">First-Time Setup</h5>
          <p class="text-muted small mb-4">Create the administrator account</p>

          <div class="alert alert-info d-flex align-items-center py-2 px-3 small mb-4 text-start" role="alert">
            <i class="bi bi-info-circle me-2"></i> No users exist yet. Set up your admin account to get started.
          </div>

          <form @submit.prevent="step = 2">
            <div class="mb-3 text-start">
              <label class="form-label">Full Name</label>
              <input type="text" class="form-control" v-model="fullName" :disabled="loading" />
            </div>
            <div class="mb-3 text-start">
              <label class="form-label">Email</label>
              <input type="email" class="form-control" v-model="email" :disabled="loading" />
            </div>
            <div class="mb-3 text-start">
              <PasswordField v-model="password" label="Password" :disabled="loading" show-strength />
            </div>
            <div class="mb-3 text-start">
              <PasswordField v-model="confirmPassword" label="Confirm Password" :disabled="loading" />
            </div>

            <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

            <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
              Continue <i class="bi bi-arrow-right ms-1"></i>
            </button>
          </form>
        </div>

        <div v-if="step === 2" class="text-center">
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 56px; height: 56px; border-radius: 50%; background: rgba(30, 58, 95, 0.08);">
            <i class="bi bi-shield-check" style="font-size: 28px; color: #1E3A5F;"></i>
          </div>
          <h5 class="fw-bold" style="color: #1E3A5F;">Review & Confirm</h5>

          <div class="text-start bg-light rounded-3 p-3 mb-4">
            <div class="d-flex align-items-center gap-2 mb-2">
              <i class="bi bi-person" style="color: #1E3A5F;"></i>
              <span><small class="text-muted me-1">Full Name:</small><span class="fw-medium">{{ fullName }}</span></span>
            </div>
            <div class="d-flex align-items-center gap-2 mb-2">
              <i class="bi bi-envelope" style="color: #1E3A5F;"></i>
              <span><small class="text-muted me-1">Email:</small><span class="fw-medium">{{ email }}</span></span>
            </div>
            <div class="d-flex align-items-center gap-2">
              <i class="bi bi-shield-account" style="color: #1E3A5F;"></i>
              <span><small class="text-muted me-1">Role:</small><span class="fw-medium">Administrator (full access)</span></span>
            </div>
          </div>

          <button class="btn btn-primary w-100 btn-lg mb-2" :disabled="loading" @click="handleSetup">
            <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
            <i class="bi bi-check me-1"></i> Complete Setup
          </button>
          <button class="btn btn-link text-muted small w-100" @click="step = 1">Back</button>
        </div>

        <div v-if="step === 3" class="text-center">
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 56px; height: 56px; border-radius: 50%; background: rgba(22, 163, 74, 0.1);">
            <i class="bi bi-check-circle" style="font-size: 28px; color: #16A34A;"></i>
          </div>
          <h5 class="fw-bold" style="color: #16A34A;">Setup Complete!</h5>

          <div class="alert alert-warning d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            Save these recovery codes. They are your backup login method. Each code can be used once.
          </div>

          <div class="card bg-light border-0 mb-3">
            <div class="card-body py-3">
              <div v-for="(code, i) in recoveryCodes" :key="i"
                class="d-flex align-items-center justify-content-center gap-2 py-1">
                <code class="fs-5 fw-bold" style="color: #1E3A5F; letter-spacing: 1px;">{{ code }}</code>
                <button class="btn btn-sm btn-outline-secondary border-0" @click="navigator.clipboard.writeText(code)" title="Copy code">
                  <i class="bi bi-clipboard"></i>
                </button>
              </div>
            </div>
          </div>

          <div class="d-flex gap-2">
            <button class="btn btn-outline-primary flex-fill" @click="downloadCodes">
              <i class="bi bi-download me-1"></i> Download Codes
            </button>
            <button class="btn btn-primary flex-fill" @click="goToLogin">
              <i class="bi bi-box-arrow-in-right me-1"></i> Go to Login
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
