<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api/client'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const codes = ref<any[]>([])
const loading = ref(true)
const generating = ref(false)
const newCodes = ref<string[]>([])

const isOnboarding = computed(() => !!route.query.onboard)
const onboardingToken = computed(() => route.query.onboard as string | undefined)

async function load() {
  try {
    const { data } = await api.get('/auth/recovery-codes')
    codes.value = data.codes
  } catch { /* ignore */ }
  loading.value = false
}

async function generate() {
  generating.value = true
  try {
    const { data } = await api.post('/auth/recovery-codes/generate')
    newCodes.value = data.recoveryCodes
    load()
  } catch { /* ignore */ }
  generating.value = false
}

async function adminOnboard() {
  if (!onboardingToken.value) return
  generating.value = true
  try {
    const data = await auth.adminOnboarding(onboardingToken.value)
    newCodes.value = data.recoveryCodes
  } catch { /* ignore */ }
  generating.value = false
}

function downloadCodes() {
  const blob = new Blob([newCodes.value.join('\n')], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
}

function proceed() {
  router.push('/')
}

async function copyCode(code: string) {
  try {
    await navigator.clipboard.writeText(code)
  } catch { /* ignore */ }
}

onMounted(async () => {
  if (isOnboarding.value) {
    await adminOnboard()
    return
  }
  await load()
})
</script>

<template>
  <div>
    <div class="p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
      <template v-if="!isOnboarding">
        <h5 class="fw-bold mb-1" style="color: #1E293B;">Recovery Codes</h5>
        <p class="text-muted small mb-3">Use these codes to log in if you forget your password</p>

        <div class="card border-0 shadow-sm" style="max-width: 560px; border-radius: 12px;">
          <div class="card-body">
            <div v-if="newCodes.length" class="alert alert-success d-flex align-items-center py-2 px-3 small" role="alert">
              <i class="bi bi-check-circle me-2"></i> New codes generated! Download or copy them now.
            </div>

            <div v-if="newCodes.length" class="card bg-light border-0 mb-3">
              <div class="card-body py-3 text-center">
                <div v-for="(code, i) in newCodes" :key="i"
                  class="d-flex align-items-center justify-content-center gap-2 py-1">
                  <code class="fs-5 fw-bold" style="color: #1E3A5F; letter-spacing: 1px;">{{ code }}</code>
                  <button class="btn btn-sm btn-outline-secondary border-0" @click="copyCode(code)" title="Copy code">
                    <i class="bi bi-clipboard"></i>
                  </button>
                </div>
              </div>
            </div>

            <div class="d-flex gap-2 mb-3">
              <button v-if="newCodes.length" class="btn btn-outline-primary" @click="downloadCodes">
                <i class="bi bi-download me-1"></i> Download Codes
              </button>
              <button class="btn btn-warning" :disabled="generating" @click="generate">
                <span v-if="generating" class="spinner-border spinner-border-sm me-2"></span>
                <i class="bi bi-arrow-clockwise me-1"></i> Generate New Codes
              </button>
            </div>

            <hr class="my-3" />

            <h6 class="fw-semibold text-muted mb-2 small text-uppercase">Code Status</h6>
            <div v-if="codes.length === 0 && !loading" class="text-center py-3 text-muted small">
              No recovery codes generated yet.
            </div>
            <div v-else class="list-group list-group-flush">
              <div v-for="c in codes" :key="c.id" class="list-group-item d-flex align-items-center gap-2 px-0">
                <i :class="c.isUsed ? 'bi bi-x-circle text-muted' : 'bi bi-check-circle text-success'"></i>
                <small class="text-muted">{{ c.isUsed ? 'Used' : 'Available' }} — {{ new Date(c.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) }}</small>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="d-flex align-items-center justify-content-center" style="min-height: calc(100vh - 56px);">
          <div class="card border-0 shadow-sm" style="max-width: 480px; border-radius: 12px;">
            <div class="card-body text-center p-4">
              <div class="d-inline-flex align-items-center justify-content-center mb-3"
                style="width: 64px; height: 64px; border-radius: 50%; background: rgba(245,158,11,0.1);">
                <i class="bi bi-shield-lock" style="font-size: 32px; color: #F59E0B;"></i>
              </div>
              <h5 class="fw-bold mb-2" style="color: #1E293B;">Admin Onboarding</h5>

              <div v-if="generating" class="py-4">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="text-muted small mt-2">Setting up recovery codes...</p>
              </div>

              <template v-if="newCodes.length && !generating">
                <div class="alert alert-warning d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
                  <i class="bi bi-exclamation-triangle me-2"></i>
                  Save these codes. Each code can only be used once.
                </div>

                <div class="card bg-light border-0 mb-3">
                  <div class="card-body py-3">
                    <div v-for="(code, i) in newCodes" :key="i"
                      class="d-flex align-items-center justify-content-center gap-2 py-1">
                      <code class="fs-5 fw-bold" style="color: #1E3A5F; letter-spacing: 1px;">{{ code }}</code>
                      <button class="btn btn-sm btn-outline-secondary border-0" @click="copyCode(code)" title="Copy code">
                        <i class="bi bi-clipboard"></i>
                      </button>
                    </div>
                  </div>
                </div>

                <button class="btn btn-primary btn-lg w-100" @click="downloadCodes">
                  <i class="bi bi-download me-1"></i> Download Recovery Codes
                </button>
              </template>

              <button v-if="newCodes.length && !generating" class="btn btn-link text-decoration-none mt-2" @click="proceed">
                Continue to Dashboard <i class="bi bi-arrow-right ms-1"></i>
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
