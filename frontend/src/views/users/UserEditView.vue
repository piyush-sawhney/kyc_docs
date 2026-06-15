<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../api/client'

const route = useRoute()
const router = useRouter()
const user = ref<any>(null)
const allPermissions = ref<any[]>([])
const userPermissionIds = ref<string[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')

async function load() {
  try {
    const [usr, perms, userPerms] = await Promise.all([
      api.get(`/users/${route.params.id}`),
      api.get('/permissions'),
      api.get(`/permissions/user/${route.params.id}`),
    ])
    user.value = usr.data
    allPermissions.value = perms.data
    userPermissionIds.value = userPerms.data.map((p: any) => p.permissionId)
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

async function resetPassword() {
  const confirmed = confirm('Reset password? User will need to set a new one on next login.')
  if (!confirmed) return
  try {
    const { data } = await api.post(`/users/${route.params.id}/reset-password`)
    alert(`Temporary password: ${data.tempPassword}`)
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


</script>

<template>
  <div>
    <div class="d-flex align-center mb-4">
      <div>
        <div class="text-h5 font-weight-bold">Edit User: {{ user?.fullName }}</div>
        <div class="text-body-2 text-grey">{{ user?.email }}</div>
      </div>
      <v-spacer />
      <v-btn color="warning" variant="outlined" prepend-icon="mdi-key-variant" @click="resetPassword">
        Reset Password
      </v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">{{ error }}</v-alert>

    <v-card :loading="loading">
      <v-card-text>
        <div v-if="user?.role === 'admin'" class="mb-4">
          <v-alert type="info" variant="tonal" density="compact" icon="mdi-shield-account">
            Admin users have full access. Permissions are not enforced.
          </v-alert>
        </div>

        <template v-for="(perms, group) in groupedPermissions" :key="group">
          <div class="text-subtitle-2 font-weight-bold text-grey mt-3 mb-2">{{ group }}</div>
          <v-row dense>
            <v-col v-for="p in perms" :key="p.id" cols="12" sm="6" md="4">
              <v-checkbox v-model="userPermissionIds" :value="p.id" :label="p.label"
                density="compact" hide-details :disabled="user?.role === 'admin'" />
            </v-col>
          </v-row>
        </template>

        <div class="d-flex ga-3 mt-6">
          <v-btn color="primary" :loading="saving" @click="savePermissions" prepend-icon="mdi-check">
            Save Permissions
          </v-btn>
          <v-btn variant="outlined" @click="router.push('/users')">Back</v-btn>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>
