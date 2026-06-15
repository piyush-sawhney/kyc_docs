<script setup lang="ts">
import { ref } from 'vue'
import api from '../api/client'
import DocumentTypeSelector from './DocumentTypeSelector.vue'

const props = defineProps<{ clientId: string }>()
const emit = defineEmits<{ close: []; done: [] }>()

const documentTypeId = ref<string | null>(null)
const documentNumber = ref('')
const issueDate = ref('')
const expiryDate = ref('')
const side = ref<'front' | 'back'>('front')
const file = ref<File | null>(null)
const saving = ref(false)

async function handleSubmit() {
  if (!file.value || !documentTypeId.value || !documentNumber.value) return
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)
    fd.append('documentTypeId', documentTypeId.value)
    fd.append('documentNumber', documentNumber.value)
    fd.append('side', side.value)
    if (issueDate.value) fd.append('issueDate', issueDate.value)
    if (expiryDate.value) fd.append('expiryDate', expiryDate.value)
    await api.post(`/clients/${props.clientId}/documents`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    emit('done')
  } catch { /* ignore */ }
  saving.value = false
}
</script>

<template>
  <v-dialog :model-value="true" max-width="560">
    <v-card>
      <v-card-title class="pa-4">
        <div class="text-h6">Add Document</div>
        <div class="text-body-2 text-grey">Upload a new document for this client</div>
      </v-card-title>

      <v-card-text>
        <DocumentTypeSelector v-model="documentTypeId" label="Document Type" />
        <v-text-field v-model="documentNumber" label="Document Number"
          variant="outlined" class="mt-3 mb-3" />
        <v-row dense>
          <v-col cols="6">
            <v-text-field v-model="issueDate" label="Issue Date" type="date" variant="outlined" />
          </v-col>
          <v-col cols="6">
            <v-text-field v-model="expiryDate" label="Expiry Date" type="date" variant="outlined" />
          </v-col>
        </v-row>
        <v-radio-group v-model="side" inline hide-details class="mb-3 mt-2">
          <v-radio label="Front Side" value="front" />
          <v-radio label="Back Side" value="back" />
        </v-radio-group>
        <v-file-input v-model="file" label="Image (JPEG/PNG)"
          accept=".jpg,.jpeg,.png" prepend-icon="mdi-camera" variant="outlined" />
      </v-card-text>

      <v-card-actions class="pa-4 pt-0">
        <v-btn color="primary" :loading="saving" @click="handleSubmit" prepend-icon="mdi-upload">
          Upload
        </v-btn>
        <v-btn variant="text" @click="emit('close')">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
