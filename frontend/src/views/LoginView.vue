<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import PasswordField from '../components/PasswordField.vue'

const router = useRouter()
const auth = useAuthStore()
const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

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

        <hr class="my-4" />
        <small class="text-muted">KYC Docs v1.0</small>
      </div>
    </div>
  </div>
</template>
