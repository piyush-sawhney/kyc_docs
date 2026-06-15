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
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4">
      <div>
        <div class="text-h5 font-weight-bold">Clients</div>
        <div class="text-body-2 text-grey">{{ clients.length }} client(s)</div>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="router.push('/clients/new')">
        New Client
      </v-btn>
    </div>

    <v-card>
      <v-data-table :headers="[
        { title: 'Name', key: 'name', sortable: true },
        { title: 'Created', key: 'createdAt', sortable: true },
        { title: 'Actions', key: 'actions', sortable: false, align: 'end' },
      ]" :items="clients" :loading="loading" class="elevation-0">
        <template #item.createdAt="{ value }">
          {{ new Date(value).toLocaleDateString() }}
        </template>
        <template #item.actions="{ item }">
          <v-btn variant="text" color="primary" size="small" @click="router.push(`/clients/${item.id}`)">
            View
          </v-btn>
        </template>
        <template #no-data>
          <div class="pa-4 text-grey">No clients yet. Create your first client.</div>
        </template>
      </v-data-table>
    </v-card>
  </div>
</template>
