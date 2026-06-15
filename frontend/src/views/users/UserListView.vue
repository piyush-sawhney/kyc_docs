<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'

const router = useRouter()
const users = ref<any[]>([])
const loading = ref(true)
const search = ref('')

const filteredUsers = computed(() => {
  if (!search.value) return users.value
  const q = search.value.toLowerCase()
  return users.value.filter(
    (u) => u.fullName?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q),
  )
})

onMounted(async () => {
  try {
    const { data } = await api.get('/users')
    users.value = data
  } catch { /* ignore */ }
  loading.value = false
})

async function toggleActive(user: any) {
  try {
    if (user.isActive) {
      await api.post(`/users/${user.id}/deactivate`)
    } else {
      await api.post(`/users/${user.id}/reactivate`)
    }
    user.isActive = !user.isActive
  } catch { /* ignore */ }
}

async function resetPassword(userId: string) {
  const confirmed = confirm('Reset password for this user? They will need to set a new one on next login.')
  if (!confirmed) return
  try {
    const { data } = await api.post(`/users/${userId}/reset-password`)
    alert(`Temporary password: ${data.tempPassword}\n\nPlease share this with the user.`)
  } catch { /* ignore */ }
}
</script>

<template>
  <div class="pa-6" style="background: rgb(241, 245, 249); min-height: calc(100vh - 56px);">
    <!-- Header -->
    <div class="d-flex align-center mb-4">
      <div>
        <div class="text-h6 font-weight-bold text-grey-darken-3">Users</div>
        <div class="text-caption text-grey mt-1">{{ users.length }} registered user(s)</div>
      </div>
      <v-spacer />
      <v-text-field v-model="search" prepend-inner-icon="mdi-magnify"
        placeholder="Search by name or email..." density="compact" variant="solo-filled"
        hide-details flat class="flex-shrink-0 mr-3" style="max-width: 280px;" clearable />
      <v-btn color="primary" variant="tonal" prepend-icon="mdi-plus" @click="router.push('/users/new')">
        New User
      </v-btn>
    </div>

    <!-- User cards -->
    <v-card class="rounded-xl" flat>
      <v-table class="user-table">
        <thead>
          <tr>
            <th class="font-weight-semibold text-caption text-grey">User</th>
            <th class="font-weight-semibold text-caption text-grey">Role</th>
            <th class="font-weight-semibold text-caption text-grey">Status</th>
            <th class="font-weight-semibold text-caption text-grey">Password</th>
            <th class="text-center font-weight-semibold text-caption text-grey" style="width: 120px;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in filteredUsers" :key="u.id" class="user-row"
            :class="{ 'row-inactive': !u.isActive }">
            <td>
              <div class="d-flex align-center ga-3">
                <v-avatar :color="u.isActive ? 'primary' : 'grey-lighten-3'" size="36" variant="tonal">
                  <span class="font-weight-medium text-body-2"
                    :class="u.isActive ? 'text-primary' : 'text-grey'">
                    {{ (u.fullName || '?').charAt(0).toUpperCase() }}
                  </span>
                </v-avatar>
                <div>
                  <div class="text-body-2 font-weight-medium text-grey-darken-2">{{ u.fullName }}</div>
                  <div class="text-caption text-grey">{{ u.email }}</div>
                </div>
              </div>
            </td>
            <td>
              <v-chip :color="u.role === 'admin' ? 'secondary' : 'grey'"
                variant="tonal" size="x-small" label class="font-weight-medium">
                <v-icon start size="11">mdi-shield-account</v-icon>
                {{ u.role }}
              </v-chip>
            </td>
            <td>
              <v-chip v-if="u.isActive" color="success" variant="tonal" size="x-small" label class="font-weight-medium">
                <v-icon start size="11">mdi-check-circle</v-icon> Active
              </v-chip>
              <v-chip v-else color="error" variant="tonal" size="x-small" label class="font-weight-medium">
                <v-icon start size="11">mdi-minus-circle</v-icon> Inactive
              </v-chip>
            </td>
            <td>
              <v-chip v-if="u.mustChangePassword" color="warning" variant="tonal" size="x-small" label class="font-weight-medium">
                <v-icon start size="11">mdi-key-variant</v-icon> Pending
              </v-chip>
              <span v-else class="text-caption text-grey">Set</span>
            </td>
            <td class="text-center">
              <div class="d-flex align-center justify-center ga-0">
                <v-btn icon variant="text" size="small" color="primary" title="Permissions"
                  @click="router.push(`/users/${u.id}`)">
                  <v-icon size="18">mdi-shield-account</v-icon>
                </v-btn>
                <v-btn icon variant="text" size="small" color="warning" title="Reset password"
                  @click="resetPassword(u.id)">
                  <v-icon size="18">mdi-key-variant</v-icon>
                </v-btn>
                <v-btn v-if="u.role !== 'admin'"
                  icon variant="text" size="small"
                  :color="u.isActive ? 'error' : 'success'"
                  :title="u.isActive ? 'Deactivate' : 'Reactivate'"
                  @click="toggleActive(u)">
                  <v-icon size="18">{{ u.isActive ? 'mdi-account-off' : 'mdi-account-check' }}</v-icon>
                </v-btn>
              </div>
            </td>
          </tr>
          <tr v-if="filteredUsers.length === 0">
            <td colspan="5">
              <div class="d-flex flex-column align-center pa-8">
                <v-icon size="40" class="mb-2 text-grey-lighten-2">mdi-account-off-outline</v-icon>
                <div class="text-body-2 text-grey">No users found</div>
              </div>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card>
  </div>
</template>

<style scoped>
.user-table {
  border: none;
  font-size: 13px;
}

.user-table thead th {
  background: rgb(248, 250, 252);
  border-bottom: 1px solid rgb(226, 232, 240) !important;
  padding: 10px 16px !important;
  letter-spacing: 0.04em;
  font-size: 10.5px;
  font-weight: 600;
  vertical-align: middle;
}

.user-table tbody td {
  padding: 10px 16px !important;
  border-bottom: 1px solid rgb(241, 245, 249);
  vertical-align: middle;
}

.user-table tbody tr:last-child td {
  border-bottom: none;
}

.user-row {
  transition: background 0.12s ease;
}

.user-row:hover {
  background: rgba(30, 58, 95, 0.025);
}

.user-row.row-inactive {
  opacity: 0.7;
}
</style>
