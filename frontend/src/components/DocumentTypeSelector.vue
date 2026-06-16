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
  <div class="d-flex align-items-center gap-2">
    <select class="form-select flex-grow-1" :value="model"
      @change="model = ($event.target as HTMLSelectElement).value || null">
      <option :value="null" disabled>{{ label }}</option>
      <option v-for="item in items" :key="item.id" :value="item.id">{{ item.name }}</option>
    </select>
    <button class="btn btn-sm btn-outline-primary flex-shrink-0" @click="openAdd" title="Add document type">
      <i class="bi bi-plus"></i>
    </button>
  </div>

  <div class="modal-backdrop fade show" v-if="addDialog"></div>
  <div class="modal d-block" tabindex="-1" :key="addDialogKey" v-if="addDialog">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 400px;">
      <div class="modal-content border-0 shadow">
        <div class="modal-header border-0 pb-0">
          <h6 class="fw-bold mb-0">Add Document Type</h6>
          <button type="button" class="btn-close" @click="addDialog = false"></button>
        </div>
        <div class="modal-body">
          <input type="text" class="form-control" v-model="newName"
            placeholder="Document Type Name" @keyup.enter="handleAdd" autofocus />
        </div>
        <div class="modal-footer border-0 pt-0">
          <button class="btn btn-primary" :disabled="adding" @click="handleAdd">
            <span v-if="adding" class="spinner-border spinner-border-sm me-1"></span>
            <i class="bi bi-check me-1"></i> Create
          </button>
          <button class="btn btn-outline-secondary" @click="addDialog = false">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>
