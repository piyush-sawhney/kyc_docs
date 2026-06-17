<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import api from '../../api/client'

const route = useRoute()
const router = useRouter()
const user = ref<any>(null)
const allPermissions = ref<any[]>([])
const userPermissionIds = ref<string[]>([])
const selectedRole = ref<'user' | 'admin'>('user')
const loading = ref(true)
const saving = ref(false)
const savingRole = ref(false)
const error = ref('')
const totalUsers = ref(0)
const roleConfirmDialog = ref(false)
const pendingRole = ref<'user' | 'admin'>('user')
const recoveryDialog = ref(false)
const recoveryCodes = ref<string[]>([])
const generatingCodes = ref(false)
const codesConfirmed = ref(false)
const roleChangeLoading = ref(false)
const roleChangeError = ref('')

async function load() {
  try {
    const [usr, perms, userPerms, allUsers] = await Promise.all([
      api.get(`/users/${route.params.id}`),
      api.get('/permissions'),
      api.get(`/permissions/user/${route.params.id}`),
      api.get('/users'),
    ])
    user.value = usr.data
    selectedRole.value = usr.data.role
    allPermissions.value = perms.data
    userPermissionIds.value = userPerms.data.map((p: any) => p.permissionId)
    totalUsers.value = allUsers.data.length
  } catch { /* ignore */ }
  loading.value = false
}

onMounted(load)

async function savePermissions() {
  saving.value = true
  error.value = ''
  try {
    await api.post(`/permissions/user/${route.params.id}`, { permissionIds: userPermissionIds.value })
    router.push('/users')
  } catch {
    error.value = 'Failed to save permissions'
  } finally {
    saving.value = false
  }
}

function confirmRoleChange() {
  if (selectedRole.value === user.value?.role) return
  pendingRole.value = selectedRole.value
  roleConfirmDialog.value = true
}

async function executeRoleChange() {
  roleChangeLoading.value = true
  roleChangeError.value = ''
  try {
    await api.patch(`/users/${route.params.id}/role`, { role: pendingRole.value })
    if (user.value) user.value.role = pendingRole.value
    selectedRole.value = pendingRole.value
    roleConfirmDialog.value = false
  } catch (err: any) {
    roleChangeError.value = err?.response?.data?.message || 'Failed to update role'
  } finally {
    roleChangeLoading.value = false
  }
}

async function resetPassword() {
  const confirmed = confirm('Reset password? User will need to set a new one on next login.')
  if (!confirmed) return
  try {
    const { data } = await api.post(`/users/${route.params.id}/reset-password`)
    alert(`Temporary password: ${data.tempPassword}`)
  } catch { /* ignore */ }
}

async function generateRecoveryCodes() {
  generatingCodes.value = true
  try {
    const { data } = await api.post(`/users/${route.params.id}/recovery-codes`)
    recoveryCodes.value = data.recoveryCodes
    codesConfirmed.value = false
    recoveryDialog.value = true
  } catch { /* ignore */ }
  generatingCodes.value = false
}

function downloadCodes() {
  const blob = new Blob([recoveryCodes.value.join('\n')], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `recovery-codes-${user.value?.fullName?.replace(/\s+/g, '-') || 'user'}.txt`
  a.click()
  URL.revokeObjectURL(url)
  codesConfirmed.value = true
}

async function copyCode(code: string) {
  try {
    await navigator.clipboard.writeText(code)
  } catch { /* ignore */ }
}

const groupedPermissions = computed(() => {
  const groups: Record<string, any[]> = {}
  for (const p of allPermissions.value) {
    if (!groups[p.group]) groups[p.group] = []
    groups[p.group].push(p)
  }
  return groups
})

const auth = useAuthStore()
const isOwnProfile = computed(() => {
  return user.value?.id === auth.user?.id
})

function toggleSelectAll(perms: any[]) {
  const ids = perms.map(p => p.id)
  const allSelected = ids.every(id => userPermissionIds.value.includes(id))
  if (allSelected) {
    userPermissionIds.value = userPermissionIds.value.filter(id => !ids.includes(id))
  } else {
    userPermissionIds.value = [...new Set([...userPermissionIds.value, ...ids])]
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
        <h5 class="fw-bold mb-0" style="color: #1E293B;">Edit User: {{ user?.fullName }}</h5>
        <small class="text-muted">{{ user?.email }}</small>
      </div>
      <div class="ms-auto d-flex gap-2">
        <button v-if="user?.role === 'admin'" class="btn btn-outline-secondary" @click="generateRecoveryCodes" :disabled="generatingCodes">
          <span v-if="generatingCodes" class="spinner-border spinner-border-sm me-1"></span>
          <i class="bi bi-shield-key me-1"></i> Recovery Codes
        </button>
        <button class="btn btn-outline-warning" @click="resetPassword">
          <i class="bi bi-key me-1"></i> Reset Password
        </button>
      </div>
    </div>

    <div v-if="error" class="alert alert-danger py-2 px-3 small mb-3">{{ error }}</div>

    <div class="row g-4">
      <div class="col-12 col-md-5">
        <div class="card border-0 shadow-sm" style="border-radius: 12px;">
          <div class="card-body" v-if="!loading">
            <h6 class="fw-semibold mb-3" style="color: #1E293B;">Account Settings</h6>

            <div class="d-flex align-items-center gap-3 mb-4">
              <div class="avatar-initials" style="background: rgba(30,58,95,0.08); color: #1E3A5F; width: 48px; height: 48px; font-size: 20px;">
                {{ initials(user?.fullName) }}
              </div>
              <div>
                <div class="fw-medium" style="color: #1E293B;">{{ user?.fullName }}</div>
                <small class="text-muted">{{ user?.email }}</small>
              </div>
            </div>

            <hr />

            <div class="mb-3">
              <label class="form-label small fw-medium text-muted">Role</label>
              <div class="d-flex gap-2">
                <select class="form-select" style="max-width: 160px;" v-model="selectedRole"
                  :disabled="savingRole">
                  <option value="user">User</option>
                  <option value="admin">Admin</option>
                </select>
                <button class="btn btn-primary btn-sm"
                  :disabled="selectedRole === user?.role || savingRole"
                  @click="confirmRoleChange">
                  Update Role
                </button>
              </div>
            </div>

            <div v-if="selectedRole === 'admin'" class="alert alert-warning d-flex align-items-center py-2 px-3 small" role="alert">
              <i class="bi bi-shield-account me-2"></i> Admins have full system access and bypass all permission checks.
            </div>

            <hr />

            <div class="mb-2">
              <small class="fw-medium text-muted">Status</small>
              <div class="d-flex gap-2 mt-1">
                <span v-if="user?.isActive" class="badge bg-soft-success">Active</span>
                <span v-else class="badge bg-soft-danger">Inactive</span>
                <span v-if="user?.mustChangePassword" class="badge bg-soft-warning">Password change required</span>
              </div>
            </div>

            <hr />

            <small class="text-muted">Created: {{ user?.createdAt ? new Date(user.createdAt).toLocaleString() : '-' }}</small>
          </div>
          <div v-else class="card-body text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
          </div>
        </div>
      </div>

      <div class="col-12 col-md-7">
        <div class="card border-0 shadow-sm" style="border-radius: 12px;">
          <div class="card-body" v-if="!loading">
            <div class="d-flex align-items-center mb-3">
              <h6 class="fw-semibold mb-0" style="color: #1E293B;">Permissions</h6>
              <span class="ms-auto badge bg-soft-primary">{{ userPermissionIds.length }} / {{ allPermissions.length }} selected</span>
            </div>

            <div v-if="user?.role === 'admin'" class="alert alert-info d-flex align-items-center py-2 px-3 small mb-3" role="alert">
              <i class="bi bi-info-circle me-2"></i> Admins auto-bypass all permission checks. Permission assignments below are stored but not enforced.
            </div>

            <template v-for="(perms, group) in groupedPermissions" :key="group">
              <div class="d-flex align-items-center mt-3 mb-1">
                <span class="fw-semibold small" style="color: #475569;">{{ group }}</span>
                <button class="btn btn-sm btn-link text-decoration-none p-0 ms-auto small"
                  @click="toggleSelectAll(perms)">
                  {{ perms.every(p => userPermissionIds.includes(p.id)) ? 'Deselect All' : 'Select All' }}
                </button>
              </div>
              <div class="row g-2 mb-1">
                <div v-for="p in perms" :key="p.id" class="col-12 col-sm-6">
                  <div class="form-check">
                    <input type="checkbox" class="form-check-input" :id="'perm-' + p.id"
                      :value="p.id" v-model="userPermissionIds" :disabled="user?.role === 'admin'" />
                    <label class="form-check-label small" :for="'perm-' + p.id">{{ p.label }}</label>
                  </div>
                </div>
              </div>
            </template>

            <hr class="my-4" />

            <div class="d-flex gap-2">
              <button class="btn btn-primary" :disabled="saving" @click="savePermissions">
                <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
                <i class="bi bi-check me-1"></i> Save Permissions
              </button>
              <button class="btn btn-outline-secondary" @click="router.push('/users')">Back</button>
            </div>
          </div>
          <div v-else class="card-body text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
          </div>
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
              Change <strong>{{ user?.fullName }}</strong>'s role from
              <strong>{{ user?.role }}</strong> to <strong>{{ pendingRole }}</strong>?
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

    <!-- Recovery Codes Modal -->
    <div class="modal-backdrop fade show" v-if="recoveryDialog"></div>
    <div class="modal d-block" tabindex="-1" v-if="recoveryDialog">
      <div class="modal-dialog modal-dialog-centered" style="max-width: 480px;">
        <div class="modal-content border-0 shadow">
          <div class="modal-body text-center p-4">
            <div class="d-inline-flex align-items-center justify-content-center mb-3"
              style="width: 56px; height: 56px; border-radius: 50%; background: rgba(245,158,11,0.1);">
              <i class="bi bi-shield-key" style="font-size: 28px; color: #F59E0B;"></i>
            </div>
            <h6 class="fw-bold mb-1">Recovery Codes for {{ user?.fullName }}</h6>
            <div class="alert alert-warning d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
              <i class="bi bi-exclamation-triangle me-2"></i>
              Old codes have been invalidated. Save these new codes. Each code can only be used once.
            </div>
            <div class="card bg-light border-0 mb-3">
              <div class="card-body py-3">
                <div v-for="(code, i) in recoveryCodes" :key="i"
                  class="d-flex align-items-center justify-content-center gap-2 py-1">
                  <code class="fs-5 fw-bold" style="color: #1E3A5F; letter-spacing: 1px;">{{ code }}</code>
                  <button class="btn btn-sm btn-outline-secondary border-0" @click="copyCode(code)" title="Copy code">
                    <i class="bi bi-clipboard"></i>
                  </button>
                </div>
              </div>
            </div>
            <div class="d-flex gap-2">
              <button class="btn btn-outline-primary flex-fill" @click="downloadCodes">
                <i class="bi bi-download me-1"></i> Download Codes
              </button>
              <button class="btn btn-primary flex-fill" @click="recoveryDialog = false">
                <i class="bi bi-check me-1"></i> Done
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
