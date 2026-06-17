<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const totpCode = ref('')
const qrDataUrl = ref('')
const loading = ref(false)
const error = ref('')
const success = ref(false)
const step = ref<'qr' | 'verify'>('qr')

async function initReEnroll() {
  loading.value = true
  error.value = ''
  try {
    const result = await auth.reEnroll()
    qrDataUrl.value = result.qrDataUrl
    step.value = 'verify'
  } catch (err: any) {
    error.value = err?.response?.data?.message || 'Failed to initialize re-enrollment'
  } finally {
    loading.value = false
  }
}

async function verifyReEnroll() {
  loading.value = true
  error.value = ''
  try {
    await auth.reEnrollVerify(totpCode.value)
    success.value = true
    setTimeout(() => router.push('/'), 2000)
  } catch (err: any) {
    error.value = err?.response?.data?.message || 'Invalid code'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="d-flex justify-content-center p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <div class="card border-0 shadow-sm" style="max-width: 480px; width: 100%; border-radius: 12px; height: fit-content; margin-top: 2rem;">
      <div class="card-body p-4 text-center">
        <div v-if="step === 'qr' && !qrDataUrl">
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 56px; height: 56px; border-radius: 50%; background: rgba(30,58,95,0.08);">
            <i class="bi bi-google" style="font-size: 28px; color: #1E3A5F;"></i>
          </div>
          <h5 class="fw-bold mb-1" style="color: #1E293B;">Re-enroll Authenticator</h5>
          <p class="text-muted small mb-3">Generate a new TOTP secret for your Google Authenticator app.</p>

          <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

          <button class="btn btn-primary btn-lg w-100" :disabled="loading" @click="initReEnroll">
            <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
            <i class="bi bi-arrow-clockwise me-1"></i> Generate New QR Code
          </button>
        </div>

        <div v-if="step === 'verify' && qrDataUrl">
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 56px; height: 56px; border-radius: 50%; background: rgba(30,58,95,0.08);">
            <i class="bi bi-google" style="font-size: 28px; color: #1E3A5F;"></i>
          </div>
          <h5 class="fw-bold mb-1" style="color: #1E293B;">Scan New QR Code</h5>
          <p class="text-muted small mb-3">Scan with Google Authenticator, then enter the code below.</p>

          <div class="d-flex justify-content-center mb-3">
            <img :src="qrDataUrl" alt="TOTP QR Code" style="width: 180px; height: 180px; border-radius: 8px;" />
          </div>

          <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>
          <div v-if="success" class="alert alert-success py-2 px-3 small">
            <i class="bi bi-check-circle me-1"></i> Authenticator re-enrolled! Redirecting...
          </div>

          <form @submit.prevent="verifyReEnroll">
            <div class="mb-3">
              <input type="text" class="form-control text-center fs-5" v-model="totpCode"
                placeholder="000000" maxlength="6" inputmode="numeric" pattern="[0-9]*" required />
            </div>
            <button type="submit" class="btn btn-primary w-100 btn-lg" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
              <i class="bi bi-check me-1"></i> Verify & Save
            </button>
          </form>

          <button class="btn btn-link text-muted small mt-2" @click="step = 'qr'; qrDataUrl = ''; error = ''">
            Start over
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
