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
    const { data } = await api.post('/clients', { name: name.value })
    router.push(`/?selectClient=${data.id}`)
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to create client'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="p-4 d-flex flex-column align-items-center" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <h5 class="fw-bold mb-1" style="color: #1E293B;">New Client</h5>
    <p class="text-muted small mb-4">Create a new client record</p>

    <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3" style="max-width: 480px; width: 100%;">{{ error }}</div>

    <div class="card border-0 shadow-sm" style="max-width: 480px; width: 100%; border-radius: 12px;">
      <div class="card-body">
        <form @submit.prevent="handleSubmit">
          <div class="mb-3">
            <label class="form-label">Client Name</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-person"></i></span>
              <input type="text" class="form-control" v-model="name" placeholder="Client Name" autofocus :disabled="loading" />
            </div>
          </div>
          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
              <i class="bi bi-check me-1"></i> Create Client
            </button>
            <button type="button" class="btn btn-outline-secondary" @click="router.push('/')">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
