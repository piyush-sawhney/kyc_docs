<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import KnapsLogo from '../components/KnapsLogo.vue'

const router = useRouter()
const auth = useAuthStore()
const step = ref<'email' | 'totp' | 'recovery'>('email')
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
const needsConfirm = ref(false)
const confirmToken = ref('')
const recoveryCodes = ref<string[]>([])
const resumeUser = ref<Record<string, unknown> | null>(null)

async function handleEmailSubmit() {
  loading.value = true
  error.value = ''
  try {
    const result = await auth.loginInit(email.value)
    needsConfirm.value = result.needsConfirm || false
    if (result.enrolled) {
      step.value = 'totp'
    } else {
      qrDataUrl.value = result.qrDataUrl
      enrollToken.value = result.enrollToken
      router.push(`/totp-enroll?token=${enrollToken.value}&email=${email.value}`)
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
    if (needsConfirm.value) {
      const result = await auth.resumeSetup(email.value, totpCode.value)
      confirmToken.value = result.token
      recoveryCodes.value = result.recoveryCodes
      resumeUser.value = result.user
      step.value = 'recovery'
    } else {
      const result = await auth.verifyTotp(email.value, totpCode.value)
      totpAttempts.value = 0
      if (result.requiresOnboarding) {
        router.push(`/recovery-codes?onboard=${result.onboardingToken}`)
      } else {
        router.push('/')
      }
    }
  } catch (err: any) {
    if (!needsConfirm.value) {
      totpAttempts.value++
      if (totpAttempts.value >= 3) {
        totpLocked.value = true
        setTimeout(() => {
          totpLocked.value = false
          totpAttempts.value = 0
          step.value = 'email'
          error.value = ''
        }, 3000)
        return
      }
    }
    error.value = `Invalid code.${!needsConfirm.value ? ` ${3 - totpAttempts.value} attempt(s) remaining.` : ''}`
  } finally {
    loading.value = false
  }
}

async function downloadAndGo() {
  auth.token = confirmToken.value
  localStorage.setItem('token', confirmToken.value)
  auth.user = resumeUser.value as { id: string; email: string; fullName: string; role: string }

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

async function handleRecoveryLogin() {
  recoveryLoading.value = true
  error.value = ''
  try {
    const result = await auth.recoveryLogin(recoveryEmail.value, recoveryCode.value)
    if (result.requiresOnboarding) {
      router.push(`/recovery-codes?onboard=${result.onboardingToken}`)
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
    <div class="card shadow-lg border-0" style="width: 440px; border-radius: 16px;">
      <div class="card-body p-5 text-center">
        <div class="d-flex justify-content-center mb-3">
          <KnapsLogo size="md" />
        </div>

        <template v-if="step === 'recovery'">
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 56px; height: 56px; border-radius: 50%; background: rgba(22, 163, 74, 0.1);">
            <i class="bi bi-check-circle" style="font-size: 28px; color: #16A34A;"></i>
          </div>
          <h5 class="fw-bold" style="color: #16A34A;">Verification Successful</h5>

          <div class="alert alert-warning d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            Save these recovery codes. They are your backup login method. Each code can be used once.
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

          <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

          <button class="btn btn-primary w-100 btn-lg" @click="downloadAndGo">
            <i class="bi bi-download me-1"></i> Download Recovery Codes
          </button>
        </template>

        <template v-else>
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
                <div v-if="needsConfirm" class="alert alert-info d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
                  <i class="bi bi-info-circle me-2"></i>
                  Your setup was interrupted. Verify with TOTP to download recovery codes and complete setup.
                </div>
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
                  {{ needsConfirm ? 'Verify' : 'Sign In' }}
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
        </template>

        <hr class="my-4" />
      </div>
    </div>
  </div>
</template>
