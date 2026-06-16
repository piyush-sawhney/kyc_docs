<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'
import PasswordField from '../../components/PasswordField.vue'

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
  <div class="p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <h5 class="fw-bold mb-1" style="color: #1E293B;">Create User</h5>
    <p class="text-muted small mb-3">Add a new user to the system</p>

    <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

    <div class="card border-0 shadow-sm" style="max-width: 520px; border-radius: 12px;">
      <div class="card-body">
        <form @submit.prevent="handleSubmit">
          <div class="mb-3">
            <label class="form-label">Full Name</label>
            <input type="text" class="form-control" v-model="fullName" :disabled="loading" />
          </div>
          <div class="mb-3">
            <label class="form-label">Email</label>
            <input type="email" class="form-control" v-model="email" :disabled="loading" />
          </div>
          <div class="mb-3">
            <PasswordField v-model="password" label="Password" :disabled="loading" show-strength />
          </div>
          <div class="mb-4">
            <label class="form-label">Role</label>
            <select class="form-select" v-model="role" :disabled="loading">
              <option value="user">User</option>
              <option value="admin">Admin</option>
            </select>
          </div>
          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
              <i class="bi bi-check me-1"></i> Create User
            </button>
            <button type="button" class="btn btn-outline-secondary" @click="router.push('/users')">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
