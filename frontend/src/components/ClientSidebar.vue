<script setup lang="ts">
import { ref, watch, computed, onMounted, nextTick } from 'vue'
import api from '../api/client'

const props = defineProps<{ selectedClientId: string | null }>()
const emit = defineEmits<{ select: [id: string]; refresh: [] }>()

const clients = ref<any[]>([])
const search = ref('')
const loading = ref(false)
const matchingDocClientIds = ref<Set<string>>(new Set())

const sortedClients = computed(() =>
  [...clients.value].sort((a, b) => a.name.localeCompare(b.name))
)

const filteredClients = computed(() => {
  const source = sortedClients.value
  if (!search.value) return source
  const lower = search.value.toLowerCase()
  return source.filter((c) => {
    const nameMatch = c.name.toLowerCase().includes(lower)
    const docMatch = matchingDocClientIds.value.has(c.id)
    return nameMatch || docMatch
  })
})

const addingClient = ref(false)
const newClientName = ref('')
const newClientLoading = ref(false)
const newInputRef = ref<HTMLElement | null>(null)

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/clients')
    clients.value = data
  } catch { /* ignore */ }
  loading.value = false
}

let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(search, () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(async () => {
    if (!search.value) {
      matchingDocClientIds.value = new Set()
      return
    }
    try {
      const { data } = await api.get('/clients')
      const lower = search.value.toLowerCase()
      const ids = new Set<string>()
      for (const c of data) {
        if (c.name.toLowerCase().includes(lower)) ids.add(c.id)
      }
      try {
        await api.get(`/clients/search-by-doc/${encodeURIComponent(search.value)}`)
      } catch {
        const allDocs = await Promise.all(
          clients.value.map(async (c) => {
            try {
              const { data: groups } = await api.get(`/clients/${c.id}/documents/grouped`)
              return groups
            } catch { return [] }
          }),
        )
        for (const group of allDocs.flat()) {
          if (group.documentNumber?.toLowerCase().includes(lower)) {
            ids.add(group.clientId || '')
          }
        }
      }
      matchingDocClientIds.value = ids
    } catch { /* ignore */ }
  }, 300)
})

onMounted(load)

async function startAdd() {
  addingClient.value = true
  newClientName.value = ''
  await nextTick()
  newInputRef.value?.focus()
}

async function confirmAdd() {
  const name = newClientName.value.trim()
  if (!name) { addingClient.value = false; return }
  newClientLoading.value = true
  try {
    const { data } = await api.post('/clients', { name })
    addingClient.value = false
    load()
    emit('select', data.id)
    emit('refresh')
  } catch { /* ignore */ }
  newClientLoading.value = false
}
</script>

<template>
  <div class="d-flex flex-column" style="height: 100%;">
    <div class="pa-3 pb-0">
      <div class="d-flex align-center ga-2 mb-2">
        <v-icon color="primary" size="small">mdi-account-group-outline</v-icon>
        <span class="text-subtitle-2 font-weight-semibold text-grey-darken-1">Clients</span>
        <v-spacer />
        <span class="text-caption text-grey font-weight-medium">{{ filteredClients.length }}</span>
      </div>

      <!-- New client input at top -->
      <div v-if="addingClient" class="mb-2">
        <v-text-field ref="newInputRef" v-model="newClientName"
          placeholder="Enter client name..." variant="outlined" density="compact"
          hide-details autofocus :loading="newClientLoading"
          @keyup.enter="confirmAdd" @blur="confirmAdd"
          @keyup.escape="addingClient = false"
          prepend-inner-icon="mdi-plus" />
      </div>

      <div class="d-flex align-center ga-2">
        <v-text-field v-model="search" prepend-inner-icon="mdi-magnify"
          placeholder="Search clients or docs..." density="compact" variant="solo-filled"
          hide-details flat class="flex-grow-1" clearable />
        <v-btn size="small" color="primary" variant="tonal"
          icon="mdi-plus" @click="startAdd"
          class="flex-shrink-0" title="New client" />
      </div>
    </div>

    <v-list density="compact" nav class="flex-grow-1 overflow-y-auto pa-1 mt-1">
      <v-list-item v-for="c in filteredClients" :key="c.id"
        :title="c.name" :active="c.id === selectedClientId"
        @click="emit('select', c.id)" class="client-item rounded-lg mb-0">
        <template #prepend>
          <v-avatar :color="c.id === selectedClientId ? 'primary' : 'grey-lighten-3'"
            size="32" variant="tonal" class="mr-1">
            <span class="font-weight-medium text-body-2"
              :class="c.id === selectedClientId ? 'text-primary' : 'text-grey'">
              {{ c.name.charAt(0).toUpperCase() }}
            </span>
          </v-avatar>
        </template>
        <template #title>
          <span class="text-body-1 font-weight-medium" :class="c.id === selectedClientId ? 'text-primary' : 'text-grey-darken-1'">
            {{ c.name }}
          </span>
        </template>
      </v-list-item>

      <div v-if="!filteredClients.length && !addingClient" class="pa-6 text-center">
        <v-icon size="32" class="mb-2 text-grey-lighten-1">mdi-account-off-outline</v-icon>
        <div class="text-caption text-grey">No clients found</div>
      </div>
    </v-list>
  </div>
</template>

<style scoped>
.client-item {
  transition: all 0.12s ease;
  margin-bottom: 1px;
}

.client-item:hover {
  background: rgba(30, 58, 95, 0.04);
}

.client-item.v-list-item--active {
  background: rgba(30, 58, 95, 0.06);
}
</style>
