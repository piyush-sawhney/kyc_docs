<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../api/client'
import ImageEditor from '../../components/ImageEditor.vue'

const route = useRoute()
const router = useRouter()
const client = ref<any>(null)
const documents = ref<any[]>([])
const loading = ref(true)
const showUpload = ref(false)
const selectedDocType = ref('')
const selectedDocNumber = ref('')
const selectedFile = ref<File | null>(null)
const editingImageFile = ref<File | null>(null)
const editingImageDocId = ref<string | null>(null)
const showEditor = ref(false)
const editorLoading = ref(false)
const uploadLoading = ref(false)
const errorSnackbar = ref(false)
const errorMsg = ref('')

async function load() {
  try {
    const { data } = await api.get(`/clients/${route.params.id}`)
    client.value = data
    const { data: docs } = await api.get(`/clients/${route.params.id}/documents`)
    documents.value = docs
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message || 'Failed to load client'
    errorSnackbar.value = true
  }
  loading.value = false
}

async function handleUpload() {
  if (!selectedFile.value || !selectedDocType.value || !selectedDocNumber.value) return
  if (!selectedFile.value.type.startsWith('image/')) {
    errorMsg.value = 'Only JPEG and PNG images are allowed'
    errorSnackbar.value = true
    return
  }
  uploadLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('documentTypeId', selectedDocType.value)
    fd.append('documentNumber', selectedDocNumber.value)
    await api.post(`/clients/${route.params.id}/documents`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    showUpload.value = false
    selectedFile.value = null
    selectedDocType.value = ''
    selectedDocNumber.value = ''
    load()
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message || 'Upload failed'
    errorSnackbar.value = true
  }
  uploadLoading.value = false
}

async function handleDelete(docId: string) {
  if (!confirm('Delete this document?')) return
  await api.delete(`/documents/${docId}`)
  load()
}

async function openEditor(doc: any) {
  editorLoading.value = true
  try {
    const res = await api.get(`/documents/${doc.id}/download`, { responseType: 'blob' })
    editingImageFile.value = new File([res.data], 'document.jpg', { type: res.data.type })
    editingImageDocId.value = doc.id
    showEditor.value = true
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message || 'Failed to load image for editing'
    errorSnackbar.value = true
  }
  editorLoading.value = false
}

async function onEditorSave(blob: Blob, filename: string) {
  const docId = editingImageDocId.value
  editingImageFile.value = null
  editingImageDocId.value = null
  showEditor.value = false
  if (!docId) return
  try {
    const fd = new FormData()
    fd.append('file', blob, filename)
    await api.put(`/documents/${docId}`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    load()
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message || 'Failed to save edited image'
    errorSnackbar.value = true
  }
}

function handleDownload(docId: string) {
  window.open(`http://localhost:3000/api/documents/${docId}/download`, '_blank')
}

onMounted(load)
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4">
      <div>
        <div class="text-h5 font-weight-bold">{{ client?.name }}</div>
        <div class="text-body-2 text-grey">Client ID: {{ client?.id }}</div>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-upload" @click="showUpload = !showUpload">
        Upload Document
      </v-btn>
      <v-btn variant="outlined" class="ml-2" @click="router.push('/clients')">Back</v-btn>
    </div>

    <v-expand-transition>
      <v-card v-if="showUpload" class="mb-4" color="grey-lighten-4">
        <v-card-text>
          <div class="text-subtitle-2 font-weight-bold mb-3">Upload Document</div>
          <v-row dense>
            <v-col cols="12" sm="4">
              <v-text-field v-model="selectedDocNumber" label="Document Number" density="compact" hide-details />
            </v-col>
            <v-col cols="12" sm="4">
              <v-file-input v-model="selectedFile" label="File (JPEG/PNG)" accept=".jpg,.jpeg,.png"
                density="compact" hide-details prepend-icon="mdi-camera" />
            </v-col>
            <v-col cols="12" sm="4" class="d-flex align-center">
              <v-btn color="success" :loading="uploadLoading" @click="handleUpload" prepend-icon="mdi-check">
                Upload
              </v-btn>
              <v-btn variant="text" class="ml-2" @click="showUpload = false">Cancel</v-btn>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-expand-transition>

    <v-card>
      <v-data-table :headers="[
        { title: 'Document Number', key: 'documentNumber', sortable: true },
        { title: 'File', key: 'originalFilename' },
        { title: 'Size', key: 'fileSize' },
        { title: 'Status', key: 'isDeleted' },
        { title: 'Actions', key: 'actions', sortable: false, align: 'end' },
      ]" :items="documents" :loading="loading" class="elevation-0">
        <template #item.fileSize="{ value }">
          {{ value ? Math.round(value / 1024) + ' KB' : '-' }}
        </template>
        <template #item.isDeleted="{ value }">
          <v-chip v-if="value" color="grey" size="x-small">Deleted</v-chip>
          <v-chip v-else color="success" size="x-small">Active</v-chip>
        </template>
        <template #item.actions="{ item }">
          <template v-if="!item.isDeleted">
            <v-btn variant="text" color="primary" size="x-small" icon="mdi-download" @click="handleDownload(item.id)" />
            <v-btn variant="text" color="secondary" size="x-small" icon="mdi-image-edit" @click="openEditor(item)" />
            <v-btn variant="text" color="error" size="x-small" icon="mdi-delete" @click="handleDelete(item.id)" />
          </template>
        </template>
        <template #no-data>
          <div class="pa-4 text-grey">No documents uploaded for this client.</div>
        </template>
      </v-data-table>
    </v-card>

    <ImageEditor v-if="showEditor && editingImageFile" :image-src="editingImageFile"
      @close="showEditor = false; editingImageFile = null; editingImageDocId = null"
      @save="onEditorSave" />
    <v-progress-circular v-if="editorLoading" indeterminate size="24" width="2" color="primary"
      class="ma-auto" />

    <v-snackbar v-model="errorSnackbar" color="error" variant="tonal" location="top" :timeout="4000">
      {{ errorMsg }}
    </v-snackbar>
  </div>
</template>
