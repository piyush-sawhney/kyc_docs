<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'

const router = useRouter()
const users = ref<any[]>([])
const deletedUsers = ref<any[]>([])
const loading = ref(true)
const search = ref('')
const showDeleted = ref(false)
const roleConfirmDialog = ref(false)
const pendingUser = ref<any>(null)
const pendingRole = ref<string>('user')
const roleChangeLoading = ref(false)
const roleChangeError = ref('')

const filteredUsers = computed(() => {
  if (!search.value) return users.value
  const q = search.value.toLowerCase()
  return users.value.filter(
    (u) => u.fullName?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q),
  )
})

const activeCount = computed(() => users.value.filter(u => u.isActive).length)

const toggleError = ref('')

onMounted(async () => {
  try {
    const [active, deleted] = await Promise.all([
      api.get('/users'),
      api.get('/users/deleted'),
    ])
    users.value = active.data.users
    deletedUsers.value = deleted.data.users
  } catch { /* ignore */ }
  loading.value = false
})

async function toggleActive(user: any) {
  toggleError.value = ''
  try {
    if (user.isActive) {
      await api.post(`/users/${user.id}/deactivate`)
    } else {
      await api.post(`/users/${user.id}/reactivate`)
    }
    user.isActive = !user.isActive
  } catch (err: any) {
    toggleError.value = err?.response?.data?.message || 'Failed to toggle user status'
  }
}

async function softDeleteUser(user: any) {
  const confirmed = confirm(`Delete user "${user.fullName}"? They will be permanently disabled. This can be undone.`)
  if (!confirmed) return
  try {
    await api.delete(`/users/${user.id}`)
    users.value = users.value.filter((u) => u.id !== user.id)
    const { data } = await api.get('/users/deleted')
    deletedUsers.value = data.users
  } catch { /* ignore */ }
}

async function restoreUser(userId: string) {
  const confirmed = confirm('Restore this user? They will be reactivated.')
  if (!confirmed) return
  try {
    await api.post(`/users/${userId}/restore`)
    const [active, deleted] = await Promise.all([
      api.get('/users'),
      api.get('/users/deleted'),
    ])
    users.value = active.data.users
    deletedUsers.value = deleted.data.users
  } catch { /* ignore */ }
}

function openRoleConfirm(user: any, newRole: string) {
  pendingUser.value = user
  pendingRole.value = newRole
  roleChangeError.value = ''
  roleConfirmDialog.value = true
}

async function executeRoleChange() {
  if (!pendingUser.value) return
  roleChangeLoading.value = true
  roleChangeError.value = ''
  try {
    await api.patch(`/users/${pendingUser.value.id}/role`, { role: pendingRole.value })
    pendingUser.value.role = pendingRole.value
    roleConfirmDialog.value = false
    pendingUser.value = null
  } catch (err: any) {
    roleChangeError.value = err?.response?.data?.message || 'Failed to update role'
  } finally {
    roleChangeLoading.value = false
  }
}

function initials(name: string) {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}
</script>

<template>
  <div class="p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <div class="d-flex align-items-center mb-3">
      <div>
        <h5 class="fw-bold mb-0" style="color: #1E293B;">Users</h5>
        <small class="text-muted">{{ users.length }} active user(s)</small>
      </div>
      <div class="ms-auto d-flex align-items-center gap-2">
        <div class="input-group" style="max-width: 280px;">
          <span class="input-group-text"><i class="bi bi-search"></i></span>
          <input type="text" class="form-control" v-model="search" placeholder="Search by name or email..." />
          <button v-if="search" class="btn btn-outline-secondary" @click="search = ''"><i class="bi bi-x"></i></button>
        </div>
        <button class="btn btn-sm btn-primary" @click="router.push('/users/new')">
          <i class="bi bi-plus me-1"></i> New User
        </button>
      </div>
    </div>

    <div v-if="toggleError" class="alert alert-danger d-flex align-items-center py-2 px-3 small mb-3" role="alert">
      <i class="bi bi-exclamation-circle me-2"></i> {{ toggleError }}
      <button class="btn-close ms-auto" @click="toggleError = ''" style="font-size: 12px;"></button>
    </div>

    <div class="card border-0 shadow-sm" style="border-radius: 12px;">
      <div class="table-responsive">
        <table class="table mb-0">
          <thead>
            <tr>
              <th>User</th>
              <th>Role</th>
              <th>Status</th>
              <th class="text-end" style="width: 200px;">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id" :class="{ 'opacity-50': !u.isActive }">
              <td>
                <div class="d-flex align-items-center gap-3">
                  <div class="avatar-initials" :style="u.isActive ? 'background: rgba(30,58,95,0.08); color: #1E3A5F;' : 'background: #F1F5F9; color: #94A3B8;'">
                    {{ initials(u.fullName) }}
                  </div>
                  <div>
                    <a class="fw-medium text-decoration-none" style="color: #1E293B; cursor: pointer;"
                      @click="router.push(`/users/${u.id}`)">{{ u.fullName }}</a>
                    <div><small class="text-muted">{{ u.email }}</small></div>
                  </div>
                </div>
              </td>
              <td>
                <select class="form-select form-select-sm" style="max-width: 110px;"
                  :value="u.role"
                  @change="openRoleConfirm(u, ($event.target as HTMLSelectElement).value)">
                  <option value="user">User</option>
                  <option value="admin">Admin</option>
                </select>
              </td>
              <td>
                <span v-if="u.isActive" class="badge bg-soft-success">Active</span>
                <span v-else class="badge bg-soft-danger">Inactive</span>
              </td>
              <td class="text-end">
                <div class="d-flex align-items-center justify-content-end gap-1">
                  <button class="btn btn-sm btn-soft-primary" title="Edit user permissions & recovery codes"
                    @click="router.push(`/users/${u.id}`)">
                    <i class="bi bi-shield-account me-1"></i> Permissions
                  </button>
                  <button class="btn btn-sm" :class="u.isActive ? 'btn-soft-danger' : 'btn-soft-success'"
                    :title="u.isActive ? 'Deactivate' : 'Reactivate'"
                    @click="toggleActive(u)">
                    <i :class="u.isActive ? 'bi bi-person-x' : 'bi bi-person-check'"></i>
                  </button>
                  <button class="btn btn-sm btn-soft-secondary" title="Delete user"
                    @click="softDeleteUser(u)">
                    <i class="bi bi-trash"></i>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredUsers.length === 0">
              <td colspan="4">
                <div class="empty-state">
                  <i class="bi bi-person-x"></i>
                  <p class="small text-muted mb-0">No users found</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="deletedUsers.length > 0" class="card border-0 shadow-sm mt-3 overflow-hidden" style="border-radius: 12px;">
      <div class="card-header bg-white d-flex align-items-center gap-2 py-2 px-3" role="button" @click="showDeleted = !showDeleted"
        style="cursor: pointer;">
        <i class="bi bi-trash text-muted"></i>
        <small class="fw-medium text-muted">Deleted Users ({{ deletedUsers.length }})</small>
        <i class="bi ms-auto" :class="showDeleted ? 'bi-chevron-up' : 'bi-chevron-down'" style="color: #94A3B8;"></i>
      </div>
      <div v-if="showDeleted">
        <div class="table-responsive">
          <table class="table mb-0">
            <thead>
              <tr>
                <th>User</th>
                <th>Deleted At</th>
                <th class="text-end" style="width: 100px;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in deletedUsers" :key="u.id" class="opacity-50">
                <td>
                  <div class="d-flex align-items-center gap-3">
                    <div class="avatar-initials" style="background: #F1F5F9; color: #94A3B8;">
                      {{ initials(u.fullName) }}
                    </div>
                    <div>
                      <span class="fw-medium" style="color: #1E293B;">{{ u.fullName }}</span>
                      <div><small class="text-muted">{{ u.email }}</small></div>
                    </div>
                  </div>
                </td>
                <td><small class="text-muted">{{ u.deletedAt ? new Date(u.deletedAt).toLocaleString() : '-' }}</small></td>
                <td class="text-end">
                  <button class="btn btn-sm btn-soft-success" title="Restore user" @click="restoreUser(u.id)">
                    <i class="bi bi-arrow-counterclockwise"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Role Change Confirmation Modal -->
    <div class="modal-backdrop fade show" v-if="roleConfirmDialog"></div>
    <div class="modal d-block" tabindex="-1" v-if="roleConfirmDialog">
      <div class="modal-dialog modal-dialog-centered" style="max-width: 400px;">
        <div class="modal-content border-0 shadow">
          <div class="modal-body text-center p-4">
            <div class="d-inline-flex align-items-center justify-content-center mb-3"
              style="width: 48px; height: 48px; border-radius: 50%; background: rgba(245,158,11,0.1);">
              <i class="bi bi-shield-account" style="font-size: 24px; color: #F59E0B;"></i>
            </div>
            <h6 class="fw-semibold mb-1">Change User Role</h6>
            <p class="small text-muted mb-0">
              Change <strong>{{ pendingUser?.fullName }}</strong>'s role from
              <strong>{{ pendingUser?.role }}</strong> to <strong>{{ pendingRole }}</strong>?
            </p>
            <p v-if="pendingRole === 'user'" class="small text-warning mt-2 mb-0">
              <i class="bi bi-exclamation-triangle me-1"></i>This user will lose all admin privileges.
            </p>
            <div v-if="roleChangeError" class="alert alert-danger py-2 px-3 small mt-3 mb-0">{{ roleChangeError }}</div>
          </div>
          <div class="modal-footer border-0 justify-content-center pt-0 pb-4">
            <button class="btn btn-primary" :disabled="roleChangeLoading" @click="executeRoleChange">
              <span v-if="roleChangeLoading" class="spinner-border spinner-border-sm me-1"></span>
              <i class="bi bi-check me-1"></i> Confirm Change
            </button>
            <button class="btn btn-outline-secondary" @click="roleConfirmDialog = false">Cancel</button>
          </div>
        </div>
      </div>
    </div>

    
  </div>
</template>
