<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api/client'

const logs = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const itemsPerPage = ref(25)
const entityTypeFilter = ref('')
const searchQuery = ref('')
const loading = ref(true)
const entityTypes = ['', 'user', 'client', 'document', 'document_type', 'auth']

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, limit: itemsPerPage.value }
    if (entityTypeFilter.value) params.entityType = entityTypeFilter.value
    if (searchQuery.value) params.search = searchQuery.value
    const { data } = await api.get('/audit-logs', { params })
    logs.value = data.data
    total.value = data.total
  } catch { /* ignore */ }
  loading.value = false
}

function actionColor(action: string): string {
  if (['CREATE','LOGIN','RESTORE','REACTIVATE'].includes(action)) return 'bg-soft-success'
  if (['UPDATE','ROLE_CHANGE','PASSWORD_CHANGE','RECOVERY_CODES'].includes(action)) return 'bg-soft-warning'
  if (['DELETE','DEACTIVATE'].includes(action)) return 'bg-soft-danger'
  if (action === 'RESET_PASSWORD') return 'bg-soft-warning'
  return 'bg-soft-secondary'
}

function actionBadgeStyle(action: string): string {
  if (['CREATE','LOGIN','RESTORE','REACTIVATE'].includes(action)) return 'color: #16A34A'
  if (['UPDATE','ROLE_CHANGE','PASSWORD_CHANGE','RECOVERY_CODES'].includes(action)) return 'color: #F59E0B'
  if (['DELETE','DEACTIVATE'].includes(action)) return 'color: #DC2626'
  return 'color: #4F46E5'
}

function formatTimestamp(ts: string) {
  return new Date(ts).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function initials(name: string) {
  if (!name) return 'S'
  return name.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2)
}

onMounted(load)
</script>

<template>
  <div class="p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <h5 class="fw-bold mb-1" style="color: #1E293B;">Audit Logs</h5>
    <p class="text-muted small mb-3">Immutable record of all system actions</p>

    <div class="card border-0 shadow-sm mb-3" style="border-radius: 12px;">
      <div class="card-body d-flex gap-2 align-items-center flex-wrap">
        <select class="form-select" style="max-width: 180px;" v-model="entityTypeFilter">
          <option value="">All Entity Types</option>
          <option v-for="t in entityTypes.filter(e => e)" :key="t" :value="t">{{ t }}</option>
        </select>
        <div class="input-group" style="max-width: 280px;">
          <span class="input-group-text"><i class="bi bi-search"></i></span>
          <input type="text" class="form-control" v-model="searchQuery" placeholder="Search descriptions..." />
        </div>
        <button class="btn btn-primary" @click="load">
          <i class="bi bi-funnel me-1"></i> Filter
        </button>
        <span class="ms-auto text-muted small">{{ total }} entries</span>
      </div>
    </div>

    <div class="card border-0 shadow-sm" style="border-radius: 12px;">
      <div class="table-responsive">
        <table class="table mb-0">
          <thead>
            <tr>
              <th style="width: 140px;">Time</th>
              <th style="width: 140px;">User</th>
              <th style="width: 110px;">Action</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in logs" :key="item.id">
              <td><small class="text-muted">{{ formatTimestamp(item.createdAt) }}</small></td>
              <td>
                <div class="d-flex align-items-center gap-2">
                  <div class="avatar-initials avatar-initials-sm" style="background: #F1F5F9; color: #64748B;">
                    {{ initials(item.userFullName) }}
                  </div>
                  <small class="fw-medium" style="color: #1E293B;">{{ item.userFullName || 'System' }}</small>
                </div>
              </td>
              <td>
                <span class="badge" :class="actionColor(item.action)" :style="actionBadgeStyle(item.action)">
                  {{ item.action }}
                </span>
              </td>
              <td>
                <small>{{ item.description || `${item.entityType} ${item.action.toLowerCase()}` }}</small>
              </td>
            </tr>
            <tr v-if="logs.length === 0">
              <td colspan="4">
                <div class="empty-state">
                  <i class="bi bi-shield-exclamation"></i>
                  <p class="small text-muted mb-0">No audit logs found</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="card-footer d-flex align-items-center">
        <div class="ms-auto d-flex align-items-center gap-2">
          <button class="btn btn-sm btn-outline-secondary" :disabled="page <= 1" @click="page--; load()">
            <i class="bi bi-chevron-left me-1"></i> Previous
          </button>
          <small class="text-muted">Page {{ page }}</small>
          <button class="btn btn-sm btn-outline-secondary" :disabled="page * itemsPerPage >= total" @click="page++; load()">
            Next <i class="bi bi-chevron-right ms-1"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
