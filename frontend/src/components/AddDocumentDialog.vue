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
const errorMsg = ref('')

async function handleSubmit() {
  if (!file.value || !documentTypeId.value || !documentNumber.value) return
  if (!file.value.type.startsWith('image/')) {
    errorMsg.value = 'Only JPEG and PNG images are allowed'
    return
  }
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
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message || 'Upload failed'
  }
  saving.value = false
}
</script>

<template>
  <div class="modal-backdrop fade show"></div>
  <div class="modal d-block" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 560px;">
      <div class="modal-content border-0 shadow">
        <div class="modal-header border-0 pb-0">
          <div>
            <h6 class="fw-bold mb-0" style="color: #1E293B;">Add Document</h6>
            <small class="text-muted">Upload a new document for this client</small>
          </div>
          <button type="button" class="btn-close" @click="emit('close')"></button>
        </div>

        <div class="modal-body">
          <DocumentTypeSelector v-model="documentTypeId" label="Document Type" />
          <div class="mt-3 mb-3">
            <input type="text" class="form-control" v-model="documentNumber" placeholder="Document Number" />
          </div>
          <div class="row g-2">
            <div class="col-6">
              <label class="form-label small text-muted">Issue Date</label>
              <input type="date" class="form-control" v-model="issueDate" />
            </div>
            <div class="col-6">
              <label class="form-label small text-muted">Expiry Date</label>
              <input type="date" class="form-control" v-model="expiryDate" />
            </div>
          </div>
          <div class="d-flex gap-3 mt-2 mb-3">
            <div class="form-check">
              <input type="radio" class="form-check-input" id="sideFront" value="front" v-model="side" />
              <label class="form-check-label small" for="sideFront">Front Side</label>
            </div>
            <div class="form-check">
              <input type="radio" class="form-check-input" id="sideBack" value="back" v-model="side" />
              <label class="form-check-label small" for="sideBack">Back Side</label>
            </div>
          </div>
          <div>
            <label class="form-label small text-muted">Image (JPEG/PNG)</label>
            <input type="file" class="form-control" accept=".jpg,.jpeg,.png"
              @change="file = ($event.target as HTMLInputElement).files?.[0] || null" />
          </div>

          <div v-if="errorMsg" class="alert alert-danger py-2 px-3 small mt-3 mb-0">{{ errorMsg }}</div>
        </div>

        <div class="modal-footer border-0 pt-0">
          <button class="btn btn-primary" :disabled="saving" @click="handleSubmit">
            <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
            <i class="bi bi-upload me-1"></i> Upload
          </button>
          <button class="btn btn-outline-secondary" @click="emit('close')">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>
