<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import KnapsLogo from '../components/KnapsLogo.vue'

const router = useRouter()
const auth = useAuthStore()
const step = ref<'email' | 'totp'>('email')
const email = ref('')
const totpCode = ref('')
const loading = ref(false)
const error = ref('')
const showRecovery = ref(false)
const recoveryEmail = ref('')
const recoveryCode = ref('')
const recoveryLoading = ref(false)
const recoverySuccess = ref(false)
const qrDataUrl = ref('')
const enrollToken = ref('')
const totpAttempts = ref(0)
const totpLocked = ref(false)

async function handleEmailSubmit() {
  loading.value = true
  error.value = ''
  try {
    const result = await auth.loginInit(email.value)
    if (result.enrolled) {
      step.value = 'totp'
    } else {
      qrDataUrl.value = result.qrDataUrl
      enrollToken.value = result.enrollToken
      router.push(`/totp-enroll?token=${enrollToken.value}`)
    }
  } catch (err: any) {
    error.value = err?.response?.data?.message || 'Invalid email'
  } finally {
    loading.value = false
  }
}

async function handleTotpSubmit() {
  loading.value = true
  error.value = ''
  try {
    const result = await auth.verifyTotp(email.value, totpCode.value)
    totpAttempts.value = 0
    if (result.recoveryCodesMissing) {
      router.push('/recovery-codes?force=true')
    } else {
      router.push('/')
    }
  } catch (err: any) {
    totpAttempts.value++
    if (totpAttempts.value >= 3) {
      totpLocked.value = true
      setTimeout(() => {
        totpLocked.value = false
        totpAttempts.value = 0
        step.value = 'email'
        error.value = ''
      }, 3000)
    } else {
      error.value = `Invalid code. ${3 - totpAttempts.value} attempt(s) remaining.`
    }
  } finally {
    loading.value = false
  }
}

async function handleRecoveryLogin() {
  recoveryLoading.value = true
  error.value = ''
  try {
    const result = await auth.recoveryLogin(recoveryEmail.value, recoveryCode.value)
    if (result.recoveryCodesMissing) {
      router.push('/recovery-codes?force=true')
    } else {
      router.push('/')
    }
  } catch (err: any) {
    error.value = err?.response?.data?.message || 'Invalid recovery code'
  } finally {
    recoveryLoading.value = false
  }
}
</script>

<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center"
    style="background: linear-gradient(135deg, #1E3A5F 0%, #0F172A 100%);">
    <div class="card shadow-lg border-0" style="width: 400px; border-radius: 16px;">
      <div class="card-body p-5 text-center">
        <div class="d-flex justify-content-center mb-3">
          <KnapsLogo size="md" />
        </div>
        <p class="text-muted small mb-4">Document Management System</p>

        <div v-if="error" class="alert alert-danger d-flex align-items-center py-2 px-3 small" role="alert">
          <i class="bi bi-exclamation-circle me-2"></i> {{ error }}
          <button type="button" class="btn-close ms-auto" @click="error = ''" style="font-size: 12px;"></button>
        </div>

        <template v-if="!showRecovery">
          <template v-if="step === 'email'">
            <form @submit.prevent="handleEmailSubmit">
              <div class="mb-4">
                <label class="form-label text-start d-block">Email</label>
                <div class="input-group">
                  <span class="input-group-text"><i class="bi bi-envelope"></i></span>
                  <input type="email" class="form-control" v-model="email" placeholder="you@example.com" required />
                </div>
              </div>
              <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"></span>
                Continue
              </button>
            </form>
          </template>

          <template v-else>
            <div v-if="totpLocked" class="alert alert-info d-flex align-items-center py-2 px-3 small" role="alert">
              <i class="bi bi-info-circle me-2"></i> Too many incorrect attempts. Redirecting to login...
            </div>
            <form v-else @submit.prevent="handleTotpSubmit">
              <div class="mb-4">
                <label class="form-label text-start d-block">Authenticator Code</label>
                <div class="input-group">
                  <span class="input-group-text"><i class="bi bi-shield-lock"></i></span>
                  <input type="text" class="form-control" v-model="totpCode" placeholder="000000"
                    maxlength="6" inputmode="numeric" pattern="[0-9]*" required />
                </div>
              </div>
              <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"></span>
                Sign In
              </button>
            </form>
          </template>
        </template>

        <template v-else>
          <form @submit.prevent="handleRecoveryLogin">
            <div class="mb-3">
              <label class="form-label text-start d-block">Email</label>
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-envelope"></i></span>
                <input type="email" class="form-control" v-model="recoveryEmail" placeholder="Email" required />
              </div>
            </div>
            <div class="mb-4">
              <label class="form-label text-start d-block">Recovery Code</label>
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-shield-key"></i></span>
                <input type="text" class="form-control" v-model="recoveryCode" placeholder="Enter recovery code" required
                  style="letter-spacing: 2px;" @input="recoveryCode = recoveryCode.toUpperCase()" />
              </div>
            </div>
            <div v-if="recoverySuccess" class="alert alert-success py-2 px-3 small">
              <i class="bi bi-check-circle me-1"></i> Logging in...
            </div>
            <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="recoveryLoading">
              <span v-if="recoveryLoading" class="spinner-border spinner-border-sm me-2"></span>
              Sign In with Recovery Code
            </button>
          </form>
        </template>

        <div class="mt-3">
          <button class="btn btn-link btn-sm text-decoration-none p-0" @click="showRecovery = !showRecovery; error = ''">
            <i :class="showRecovery ? 'bi bi-arrow-left' : 'bi bi-key'" class="me-1"></i>
            {{ showRecovery ? 'Back to login' : 'Sign in with recovery code' }}
          </button>
        </div>

        <hr class="my-4" />
      </div>
    </div>
  </div>
</template>
