<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api/client'

const logs = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const itemsPerPage = ref(25)
const filter = ref('')
const loading = ref(true)
const entityTypes = ['', 'user', 'client', 'document', 'document_type']

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, limit: itemsPerPage.value }
    if (filter.value) params.entityType = filter.value
    const { data } = await api.get('/audit-logs', { params })
    logs.value = data.data
    total.value = data.total
  } catch { /* ignore */ }
  loading.value = false
}

function actionColor(action: string) {
  if (action === 'CREATE') return 'success'
  if (action === 'UPDATE') return 'warning'
  if (action === 'DELETE') return 'error'
  return 'grey'
}

onMounted(load)
</script>

<template>
  <div>
    <div class="text-h5 font-weight-bold mb-1">Audit Logs</div>
    <div class="text-body-2 text-grey mb-4">Immutable record of all system actions</div>

    <v-card class="mb-4">
      <v-card-text class="d-flex ga-2 pa-4">
        <v-select v-model="filter" :items="entityTypes" label="Entity Type" density="compact" hide-details
          class="flex-shrink-0" style="max-width:200px" />
        <v-btn color="primary" @click="load" prepend-icon="mdi-filter" variant="tonal">Filter</v-btn>
      </v-card-text>
    </v-card>

    <v-card>
      <v-data-table :headers="[
        { title: 'Time', key: 'createdAt', sortable: true },
        { title: 'User', key: 'userId' },
        { title: 'Action', key: 'action' },
        { title: 'Entity', key: 'entityType' },
        { title: 'Details', key: 'newValues', sortable: false },
      ]" :items="logs" :loading="loading" :items-per-page="itemsPerPage" class="elevation-0">
        <template #item.createdAt="{ value }">
          <span class="text-caption">{{ new Date(value).toLocaleString() }}</span>
        </template>
        <template #item.userId="{ value }">
          <span class="text-caption">{{ value ? value.slice(0, 8) + '...' : 'system' }}</span>
        </template>
        <template #item.action="{ value }">
          <v-chip :color="actionColor(value)" size="x-small">{{ value }}</v-chip>
        </template>
        <template #item.entityType="{ value }">
          <span class="text-caption">{{ value }}</span>
        </template>
        <template #item.newValues="{ value }">
          <span class="text-caption text-grey">{{ value ? JSON.stringify(value).slice(0, 100) : '-' }}</span>
        </template>
        <template #no-data>
          <div class="pa-4 text-grey">No audit logs yet</div>
        </template>
      </v-data-table>
      <v-card-text class="text-caption text-grey text-end pa-2">
        {{ total }} total entries
      </v-card-text>
    </v-card>
  </div>
</template>
