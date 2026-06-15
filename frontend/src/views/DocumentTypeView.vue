<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/client'

const types = ref<any[]>([])
const newName = ref('')
const loading = ref(true)
const error = ref('')

async function load() {
  try {
    const { data } = await api.get('/document-types')
    types.value = data
  } catch { /* ignore */ }
  loading.value = false
}

async function add() {
  if (!newName.value) return
  error.value = ''
  try {
    await api.post('/document-types', { name: newName.value })
    newName.value = ''
    load()
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to add'
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="text-h5 font-weight-bold mb-1">Document Types</div>
    <div class="text-body-2 text-grey mb-4">Manage the document categories used in the system</div>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable>{{ error }}</v-alert>

    <v-card max-width="560">
      <v-card-text class="d-flex ga-2 pb-0">
        <v-text-field v-model="newName" label="New document type" hide-details density="compact"
          @keyup.enter="add" />
        <v-btn color="primary" @click="add" prepend-icon="mdi-plus">Add</v-btn>
      </v-card-text>

      <v-list v-if="types.length" lines="one">
        <v-list-item v-for="t in types" :key="t.id">
          <template #prepend>
            <v-icon color="primary">mdi-file-document</v-icon>
          </template>
          <v-list-item-title>{{ t.name }}</v-list-item-title>
          <template #append>
            <v-chip :color="t.isActive ? 'success' : 'grey'" size="x-small">
              {{ t.isActive ? 'Active' : 'Inactive' }}
            </v-chip>
          </template>
        </v-list-item>
      </v-list>
      <div v-else class="pa-4 text-center text-grey">No document types defined</div>
    </v-card>
  </div>
</template>
