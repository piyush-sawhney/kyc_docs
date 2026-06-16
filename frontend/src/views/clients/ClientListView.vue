<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'

const router = useRouter()
const clients = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await api.get('/clients')
    clients.value = data
  } catch { /* ignore */ }
  loading.value = false
})

function initials(name: string) {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}
</script>

<template>
  <div class="p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <div class="d-flex align-items-center mb-3">
      <div>
        <h5 class="fw-bold mb-0" style="color: #1E293B;">Clients</h5>
        <small class="text-muted">{{ clients.length }} client(s)</small>
      </div>
      <div class="ms-auto">
        <button class="btn btn-primary" @click="router.push('/clients/new')">
          <i class="bi bi-plus me-1"></i> New Client
        </button>
      </div>
    </div>

    <div class="card border-0 shadow-sm" style="border-radius: 12px;">
      <div class="table-responsive">
        <table class="table mb-0">
          <thead>
            <tr>
              <th>Client</th>
              <th>Created</th>
              <th class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in clients" :key="c.id">
              <td>
                <div class="d-flex align-items-center gap-3">
                  <div class="avatar-initials" style="background: rgba(30,58,95,0.08); color: #1E3A5F;">
                    {{ initials(c.name) }}
                  </div>
                  <span class="fw-medium" style="color: #1E293B;">{{ c.name }}</span>
                </div>
              </td>
              <td>
                <small class="text-muted">{{ new Date(c.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) }}</small>
              </td>
              <td class="text-end">
                <button class="btn btn-sm btn-soft-primary" @click="router.push(`/clients/${c.id}`)">
                  View Details <i class="bi bi-chevron-right ms-1"></i>
                </button>
              </td>
            </tr>
            <tr v-if="clients.length === 0">
              <td colspan="3">
                <div class="empty-state">
                  <i class="bi bi-people"></i>
                  <p class="small text-muted mb-2">No clients yet</p>
                  <button class="btn btn-primary btn-sm" @click="router.push('/clients/new')">
                    <i class="bi bi-plus me-1"></i> Create your first client
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
