<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
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
    if (result.mustChangePassword) {
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
  <v-container class="fill-height d-flex align-center justify-center" style="background: linear-gradient(135deg, #1E3A5F 0%, #0F172A 100%);">
    <v-card width="400" class="pa-8 rounded-xl" elevation="0">
      <v-card-item class="pa-0 mb-6">
        <div class="d-flex flex-column align-center">
          <v-avatar color="primary" size="56" class="mb-3" variant="tonal">
            <v-icon size="30" color="primary">mdi-file-document-check</v-icon>
          </v-avatar>
          <div class="text-h5 font-weight-bold text-primary">KYC Docs</div>
          <div class="text-caption text-grey mt-1">Document Management System</div>
        </div>
      </v-card-item>

      <v-card-text class="pa-0">
        <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4 rounded-lg" closable>
          {{ error }}
        </v-alert>

        <v-form @submit.prevent="handleLogin">
          <v-text-field v-model="email" label="Email" type="email"
            prepend-inner-icon="mdi-email-outline" class="mb-3" />
          <v-text-field v-model="password" label="Password" type="password"
            prepend-inner-icon="mdi-lock-outline" class="mb-4" />
          <v-btn type="submit" color="primary" block size="large" :loading="loading" variant="tonal">
            Sign In
          </v-btn>
        </v-form>
      </v-card-text>

      <v-card-text class="pa-0 mt-4 text-center">
        <v-divider class="mb-3" />
        <small class="text-grey">KYC Docs v1.0</small>
      </v-card-text>
    </v-card>
  </v-container>
</template>
