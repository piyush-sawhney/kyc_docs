<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'

const router = useRouter()
const email = ref('')
const password = ref('')
const fullName = ref('')
const role = ref('user')
const loading = ref(false)
const error = ref('')

async function handleSubmit() {
  loading.value = true
  error.value = ''
  try {
    await api.post('/users', { email: email.value, password: password.value, fullName: fullName.value, role: role.value })
    router.push('/users')
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to create user'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <div class="text-h5 font-weight-bold mb-1">Create User</div>
    <div class="text-body-2 text-grey mb-6">Add a new user to the system</div>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable>
      {{ error }}
    </v-alert>

    <v-card max-width="520">
      <v-card-text>
        <v-form @submit.prevent="handleSubmit">
          <v-text-field v-model="fullName" label="Full Name" :disabled="loading" class="mb-3" />
          <v-text-field v-model="email" label="Email" type="email" :disabled="loading" class="mb-3" />
          <v-text-field v-model="password" label="Password" type="password" :disabled="loading" class="mb-3" />
          <v-select v-model="role" :items="['user', 'admin']" label="Role" :disabled="loading" class="mb-4" />

          <div class="d-flex ga-3">
            <v-btn type="submit" color="primary" :loading="loading" prepend-icon="mdi-check">
              Create User
            </v-btn>
            <v-btn variant="outlined" @click="router.push('/users')">Cancel</v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>
