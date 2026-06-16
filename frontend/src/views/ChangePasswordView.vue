<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import PasswordField from '../components/PasswordField.vue'

const router = useRouter()
const auth = useAuthStore()
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')
const success = ref(false)

async function handleChange() {
  if (newPassword.value !== confirmPassword.value) {
    error.value = 'Passwords do not match'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.changePassword(currentPassword.value, newPassword.value)
    success.value = true
    setTimeout(() => router.push('/'), 2000)
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to change password'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="d-flex justify-content-center p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <div class="card border-0 shadow-sm" style="max-width: 480px; width: 100%; border-radius: 12px; height: fit-content; margin-top: 2rem;">
      <div class="card-body p-4">
        <h5 class="fw-bold mb-1" style="color: #1E293B;">Change Password</h5>
        <p class="text-muted small mb-3">Your password must be at least 8 characters with uppercase, lowercase, number and special character.</p>

        <div v-if="auth.user?.mustChangePassword" class="alert alert-warning d-flex align-items-center py-2 px-3 small" role="alert">
          <i class="bi bi-exclamation-triangle me-2"></i> You must change your password before continuing.
        </div>
        <div v-if="error" class="alert alert-danger py-2 px-3 small">{{ error }}</div>
        <div v-if="success" class="alert alert-success py-2 px-3 small">
          <i class="bi bi-check-circle me-1"></i> Password changed! Redirecting...
        </div>

        <form @submit.prevent="handleChange">
          <div class="mb-3">
            <PasswordField v-model="currentPassword" label="Current Password" :disabled="loading" />
          </div>
          <div class="mb-3">
            <PasswordField v-model="newPassword" label="New Password" :disabled="loading" show-strength />
          </div>
          <div class="mb-4">
            <PasswordField v-model="confirmPassword" label="Confirm New Password" :disabled="loading" />
          </div>
          <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
            Change Password
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
