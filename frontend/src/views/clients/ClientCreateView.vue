<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'

const router = useRouter()
const name = ref('')
const loading = ref(false)
const error = ref('')

async function handleSubmit() {
  if (!name.value) { error.value = 'Client name is required'; return }
  loading.value = true
  error.value = ''
  try {
    await api.post('/clients', { name: name.value })
    router.push('/')
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to create client'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-container class="d-flex justify-center pa-6">
    <v-card width="480">
      <v-card-title class="pa-4">
        <div class="text-h6 font-weight-bold">New Client</div>
        <div class="text-body-2 text-grey">Create a new client record</div>
      </v-card-title>
      <v-card-text>
        <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable>
          {{ error }}
        </v-alert>
        <v-form @submit.prevent="handleSubmit">
          <v-text-field v-model="name" label="Client Name" variant="outlined"
            autofocus :disabled="loading" />
          <div class="d-flex ga-3 mt-4">
            <v-btn type="submit" color="primary" :loading="loading" prepend-icon="mdi-check">
              Create
            </v-btn>
            <v-btn variant="outlined" @click="router.push('/')">Cancel</v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </v-container>
</template>
