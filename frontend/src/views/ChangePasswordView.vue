<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

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
  <v-container class="d-flex justify-center">
    <v-card width="480">
      <v-card-title class="pa-4">
        <div class="text-h5 font-weight-bold">Change Password</div>
      </v-card-title>

      <v-card-text>
        <v-alert v-if="auth.user?.mustChangePassword" type="warning" variant="tonal" density="compact" class="mb-4" icon="mdi-alert">
          You must change your password before continuing.
        </v-alert>
        <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable>{{ error }}</v-alert>
        <v-alert v-if="success" type="success" variant="tonal" density="compact" class="mb-4">Password changed! Redirecting...</v-alert>

        <v-form @submit.prevent="handleChange">
          <v-text-field v-model="currentPassword" label="Current Password" type="password" :disabled="loading" class="mb-3" />
          <v-text-field v-model="newPassword" label="New Password" type="password" :disabled="loading" class="mb-3" />
          <v-text-field v-model="confirmPassword" label="Confirm New Password" type="password" :disabled="loading" class="mb-4" />
          <v-btn type="submit" color="primary" :loading="loading" block>Change Password</v-btn>
        </v-form>
      </v-card-text>
    </v-card>
  </v-container>
</template>
