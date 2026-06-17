<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api/client'
import PasswordField from '../components/PasswordField.vue'

const router = useRouter()
const auth = useAuthStore()
const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const showRecoveryReset = ref(false)
const recoveryEmail = ref('')
const recoveryCode = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const recoveryResetLoading = ref(false)
const recoveryResetSuccess = ref(false)

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    const result = await auth.login(email.value, password.value)
    if (result.recoveryCodesMissing) {
      router.push('/recovery-codes?force=true')
    } else if (result.mustChangePassword) {
      router.push('/change-password')
    } else {
      router.push('/')
    }
  } catch {
    error.value = 'Invalid email or password'
  } finally {
    loading.value = false
  }
}

async function handleRecoveryReset() {
  if (newPassword.value !== confirmPassword.value) {
    error.value = 'Passwords do not match'
    return
  }
  recoveryResetLoading.value = true
  error.value = ''
  try {
    const { data } = await api.post('/auth/recovery/reset-password', {
      email: recoveryEmail.value,
      recoveryCode: recoveryCode.value,
      newPassword: newPassword.value,
    })
    recoveryResetSuccess.value = true
    error.value = ''
    setTimeout(() => {
      showRecoveryReset.value = false
      recoveryResetSuccess.value = false
      email.value = recoveryEmail.value
      recoveryEmail.value = ''
      recoveryCode.value = ''
      newPassword.value = ''
      confirmPassword.value = ''
    }, 3000)
  } catch (err: any) {
    error.value = err?.response?.data?.message || 'Failed to reset password'
  } finally {
    recoveryResetLoading.value = false
  }
}

function toggleRecoveryReset() {
  showRecoveryReset.value = !showRecoveryReset.value
  error.value = ''
  recoveryResetSuccess.value = false
}
</script>

<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center"
    style="background: linear-gradient(135deg, #1E3A5F 0%, #0F172A 100%);">
    <div class="card shadow-lg border-0" style="width: 400px; border-radius: 16px;">
      <div class="card-body p-5 text-center">
        <div class="d-inline-flex align-items-center justify-content-center mb-3"
          style="width: 56px; height: 56px; border-radius: 50%; background: rgba(30,58,95,0.08);">
          <i class="bi bi-file-earmark-check" style="font-size: 28px; color: #1E3A5F;"></i>
        </div>
        <h4 class="fw-bold" style="color: #1E3A5F;">KYC Docs</h4>
        <p class="text-muted small mb-4">Document Management System</p>

        <div v-if="error" class="alert alert-danger d-flex align-items-center py-2 px-3 small" role="alert">
          <i class="bi bi-exclamation-circle me-2"></i> {{ error }}
          <button type="button" class="btn-close ms-auto" @click="error = ''" style="font-size: 12px;"></button>
        </div>

        <template v-if="!showRecoveryReset">
          <form @submit.prevent="handleLogin">
            <div class="mb-3">
              <label class="form-label text-start d-block">Email</label>
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-envelope"></i></span>
                <input type="email" class="form-control" v-model="email" placeholder="Email" required />
              </div>
            </div>
            <div class="mb-4">
              <PasswordField v-model="password" label="Password" />
            </div>
            <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"></span>
              Sign In
            </button>
          </form>
        </template>

        <template v-else>
          <form @submit.prevent="handleRecoveryReset">
            <div class="mb-3">
              <label class="form-label text-start d-block">Email</label>
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-envelope"></i></span>
                <input type="email" class="form-control" v-model="recoveryEmail" placeholder="Email" required />
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label text-start d-block">Recovery Code</label>
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-shield-key"></i></span>
                <input type="text" class="form-control" v-model="recoveryCode" placeholder="Enter recovery code" required
                  style="text-transform: uppercase; letter-spacing: 2px;" />
              </div>
            </div>
            <div class="mb-3">
              <PasswordField v-model="newPassword" label="New Password" :disabled="recoveryResetLoading" show-strength />
            </div>
            <div class="mb-4">
              <PasswordField v-model="confirmPassword" label="Confirm New Password" :disabled="recoveryResetLoading" />
            </div>
            <div v-if="recoveryResetSuccess" class="alert alert-success py-2 px-3 small">
              <i class="bi bi-check-circle me-1"></i> Password reset! Redirecting to login...
            </div>
            <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="recoveryResetLoading || recoveryResetSuccess">
              <span v-if="recoveryResetLoading" class="spinner-border spinner-border-sm me-2"></span>
              Reset Password
            </button>
          </form>
        </template>

        <div class="mt-3">
          <button class="btn btn-link btn-sm text-decoration-none p-0" @click="toggleRecoveryReset">
            <i :class="showRecoveryReset ? 'bi bi-arrow-left' : 'bi bi-key'" class="me-1"></i>
            {{ showRecoveryReset ? 'Back to login' : 'Reset password with recovery code' }}
          </button>
        </div>

        <hr class="my-4" />
        <small class="text-muted">KYC Docs v1.0</small>
      </div>
    </div>
  </div>
</template>
