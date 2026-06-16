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
  [...clients.value]
    .filter((c) => !c.isDeleted)
    .sort((a, b) => a.name.localeCompare(b.name))
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
    await load()
    emit('select', data.id)
    emit('refresh')
  } catch { /* ignore */ }
  newClientLoading.value = false
}
</script>

<template>
  <div class="d-flex flex-column" style="height: 100%;">
    <div class="p-3 pb-0">
      <div class="d-flex align-items-center gap-2 mb-2">
        <i class="bi bi-people" style="color: #1E3A5F; font-size: 14px;"></i>
        <small class="fw-semibold" style="color: #1E293B;">Clients</small>
        <span class="ms-auto"><small class="text-muted fw-medium">{{ filteredClients.length }}</small></span>
      </div>

      <div v-if="addingClient" class="mb-2">
        <div class="input-group input-group-sm">
          <input ref="newInputRef" type="text" class="form-control" v-model="newClientName"
            placeholder="Enter client name..." :disabled="newClientLoading"
            @keyup.enter="confirmAdd" @blur="confirmAdd" @keyup.escape="addingClient = false" />
          <span v-if="newClientLoading" class="input-group-text"><span class="spinner-border spinner-border-sm"></span></span>
        </div>
      </div>

      <div class="d-flex gap-1">
        <div class="input-group input-group-sm flex-grow-1">
          <span class="input-group-text"><i class="bi bi-search" style="font-size: 12px;"></i></span>
          <input type="text" class="form-control" v-model="search" placeholder="Search clients or docs..." />
          <button v-if="search" class="btn btn-outline-secondary" @click="search = ''"><i class="bi bi-x"></i></button>
        </div>
        <button class="btn btn-sm btn-primary flex-shrink-0" @click="startAdd" title="New client">
          <i class="bi bi-plus"></i>
        </button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-y-auto p-1 mt-1">
      <div v-for="c in filteredClients" :key="c.id"
        class="d-flex align-items-center gap-2 px-2 py-2 rounded-3 client-item mb-0 cursor-pointer"
        :class="{ 'bg-soft-primary': c.id === selectedClientId }"
        @click="emit('select', c.id)">
        <div class="avatar-initials"
          :style="c.id === selectedClientId ? 'background: rgba(30,58,95,0.12); color: #1E3A5F;' : 'background: #F1F5F9; color: #64748B;'"
          style="width: 32px; height: 32px; font-size: 13px;">
          {{ c.name.charAt(0).toUpperCase() }}
        </div>
        <span class="small fw-medium flex-grow-1 text-truncate"
          :style="c.id === selectedClientId ? 'color: #1E3A5F;' : 'color: #1E293B;'">
          {{ c.name }}
        </span>
      </div>

      <div v-if="!filteredClients.length && !addingClient" class="text-center py-5">
        <i class="bi bi-person-x" style="font-size: 28px; color: #CBD5E1;"></i>
        <div><small class="text-muted">No clients found</small></div>
      </div>
    </div>
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
</style>
