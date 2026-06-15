<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/client'

const model = defineModel<string | null>()
const props = withDefaults(defineProps<{ label?: string }>(), { label: 'Document Type' })

const items = ref<any[]>([])
const addDialog = ref(false)
const newName = ref('')
const adding = ref(false)

async function load() {
  try {
    const { data } = await api.get('/document-types')
    items.value = data
  } catch { /* ignore */ }
}

onMounted(load)

const addDialogKey = ref(0)
function openAdd() {
  newName.value = ''
  addDialogKey.value++
  addDialog.value = true
}

async function handleAdd() {
  if (!newName.value) return
  adding.value = true
  try {
    const { data } = await api.post('/document-types', { name: newName.value })
    items.value.push(data)
    model.value = data.id
    addDialog.value = false
  } catch { /* ignore */ }
  adding.value = false
}
</script>

<template>
  <div class="d-flex align-center ga-2">
    <v-select v-model="model" :items="items" item-title="name" item-value="id"
      :label="label" variant="outlined" hide-details class="flex-grow-1" />
    <v-btn icon size="small" color="primary" variant="tonal" @click="openAdd" title="Add document type">
      <v-icon>mdi-plus</v-icon>
    </v-btn>
  </div>

  <v-dialog :key="addDialogKey" v-model="addDialog" max-width="400">
    <v-card>
      <v-card-title class="pa-4">Add Document Type</v-card-title>
      <v-card-text>
        <v-text-field v-model="newName" label="Document Type Name" variant="outlined"
          autofocus @keyup.enter="handleAdd" />
      </v-card-text>
      <v-card-actions class="pa-4 pt-0">
        <v-btn color="primary" :loading="adding" @click="handleAdd" prepend-icon="mdi-check">Create</v-btn>
        <v-btn variant="text" @click="addDialog = false">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
