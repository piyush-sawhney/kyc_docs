<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSetupStore } from '../stores/setup'

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
  <v-container class="fill-height d-flex align-center justify-center">
    <v-card width="520" class="pa-6">
      <v-window v-model="step">
        <v-window-item :value="1">
          <v-card-title class="text-center pa-0 mb-6">
            <v-icon size="48" color="primary" class="mb-2">mdi-wrench</v-icon>
            <div class="text-h5 font-weight-bold">First-Time Setup</div>
            <div class="text-body-2 text-grey">Create the administrator account</div>
          </v-card-title>

          <v-card-text class="pa-0">
            <v-alert type="info" variant="tonal" density="compact" class="mb-4">
              No users exist yet. Set up your admin account to get started.
            </v-alert>
            <v-form @submit.prevent="step = 2">
              <v-text-field v-model="fullName" label="Full Name" variant="outlined" :disabled="loading" />
              <v-text-field v-model="email" label="Email" type="email" variant="outlined" :disabled="loading" class="mt-3" />
              <v-text-field v-model="password" label="Password" type="password" variant="outlined" :disabled="loading" class="mt-3" />
              <v-text-field v-model="confirmPassword" label="Confirm Password" type="password" variant="outlined"
                :disabled="loading" class="mt-3" />
              <v-btn type="submit" color="primary" block size="large" class="mt-4">Continue</v-btn>
            </v-form>
          </v-card-text>
        </v-window-item>

        <v-window-item :value="2">
          <v-card-title class="text-center pa-0 mb-6">
            <v-icon size="48" color="primary" class="mb-2">mdi-shield-check</v-icon>
            <div class="text-h5 font-weight-bold">Review & Confirm</div>
          </v-card-title>

          <v-card-text class="pa-0">
            <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable>
              {{ error }}
            </v-alert>

            <v-list lines="two">
              <v-list-item title="Full Name" :subtitle="fullName" prepend-icon="mdi-account" />
              <v-list-item title="Email" :subtitle="email" prepend-icon="mdi-email" />
              <v-list-item title="Role" subtitle="Administrator (full access)" prepend-icon="mdi-shield-account" />
            </v-list>

            <div class="d-flex ga-3 mt-4">
              <v-btn color="primary" :loading="loading" @click="handleSetup" prepend-icon="mdi-check" block>
                Complete Setup
              </v-btn>
            </div>
            <v-btn variant="text" class="mt-2" @click="step = 1" block>Back</v-btn>
          </v-card-text>
        </v-window-item>

        <v-window-item :value="3">
          <v-card-title class="text-center pa-0 mb-6">
            <v-icon size="48" color="success" class="mb-2">mdi-check-circle</v-icon>
            <div class="text-h5 font-weight-bold text-success">Setup Complete!</div>
          </v-card-title>

          <v-card-text class="pa-0">
            <v-alert type="warning" variant="tonal" density="compact" class="mb-4" icon="mdi-alert">
              Save these recovery codes. They are your backup login method. Each code can be used once.
            </v-alert>

            <v-card variant="outlined" class="mb-4">
              <v-card-text class="text-center">
                <div v-for="(code, i) in recoveryCodes" :key="i" class="text-monospace font-weight-bold py-1">
                  {{ code }}
                </div>
              </v-card-text>
            </v-card>

            <div class="d-flex ga-3">
              <v-btn color="primary" variant="outlined" @click="downloadCodes" prepend-icon="mdi-download">
                Download Codes
              </v-btn>
              <v-btn color="primary" @click="goToLogin" prepend-icon="mdi-login">
                Go to Login
              </v-btn>
            </div>
          </v-card-text>
        </v-window-item>
      </v-window>
    </v-card>
  </v-container>
</template>
