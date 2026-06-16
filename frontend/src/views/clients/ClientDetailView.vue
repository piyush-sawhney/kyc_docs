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
const errorMsg = ref('')
const docTypes = ref<any[]>([])

async function load() {
  try {
    const [cli, docs, types] = await Promise.all([
      api.get(`/clients/${route.params.id}`),
      api.get(`/clients/${route.params.id}/documents`),
      api.get('/document-types'),
    ])
    client.value = cli.data
    documents.value = docs.data
    docTypes.value = types.data
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message || 'Failed to load client'
  }
  loading.value = false
}

async function handleUpload() {
  if (!selectedFile.value || !selectedDocType.value || !selectedDocNumber.value) return
  if (!selectedFile.value.type.startsWith('image/')) {
    errorMsg.value = 'Only JPEG and PNG images are allowed'
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
  }
}

function handleDownload(docId: string) {
  window.open(`http://localhost:3000/api/documents/${docId}/download`, '_blank')
}

onMounted(load)

function formatSize(bytes: number) {
  return bytes ? Math.round(bytes / 1024) + ' KB' : '-'
}
</script>

<template>
  <div class="p-4" style="background: #F1F5F9; min-height: calc(100vh - 56px);">
    <div class="d-flex align-items-center mb-3">
      <div>
        <h5 class="fw-bold mb-0" style="color: #1E293B;">{{ client?.name }}</h5>
        <small class="text-muted">Client ID: {{ client?.id }}</small>
      </div>
      <div class="ms-auto d-flex gap-2">
        <button class="btn btn-primary" @click="showUpload = !showUpload">
          <i class="bi bi-upload me-1"></i> Upload Document
        </button>
        <button class="btn btn-outline-secondary" @click="router.push('/clients')">Back</button>
      </div>
    </div>

    <div v-if="errorMsg" class="alert alert-danger py-2 px-3 small mb-3">{{ errorMsg }}</div>

    <transition name="fade">
      <div v-if="showUpload" class="card border-0 shadow-sm mb-3" style="background: #F8FAFC; border-radius: 12px;">
        <div class="card-body">
          <h6 class="fw-semibold mb-3" style="color: #1E293B;">Upload Document</h6>
          <div class="row g-3">
            <div class="col-12 col-sm-4">
              <input type="text" class="form-control" v-model="selectedDocNumber"
                placeholder="Document Number" :disabled="uploadLoading" />
            </div>
            <div class="col-12 col-sm-4">
              <select class="form-select" v-model="selectedDocType" :disabled="uploadLoading">
                <option value="">Select type...</option>
                <option v-for="t in docTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
              </select>
            </div>
            <div class="col-12 col-sm-4">
              <input type="file" class="form-control" accept=".jpg,.jpeg,.png"
                @change="selectedFile = ($event.target as HTMLInputElement).files?.[0] || null"
                :disabled="uploadLoading" />
            </div>
          </div>
          <div class="d-flex gap-2 mt-3">
            <button class="btn btn-success" :disabled="uploadLoading" @click="handleUpload">
              <span v-if="uploadLoading" class="spinner-border spinner-border-sm me-1"></span>
              <i class="bi bi-check me-1"></i> Upload
            </button>
            <button class="btn btn-outline-secondary" @click="showUpload = false">Cancel</button>
          </div>
        </div>
      </div>
    </transition>

    <div class="card border-0 shadow-sm" style="border-radius: 12px;">
      <div class="table-responsive">
        <table class="table mb-0">
          <thead>
            <tr>
              <th>Document Number</th>
              <th>File</th>
              <th>Size</th>
              <th>Status</th>
              <th class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in documents" :key="item.id">
              <td><span class="fw-medium" style="color: #1E293B;">{{ item.documentNumber }}</span></td>
              <td><small>{{ item.originalFilename }}</small></td>
              <td><small class="text-muted">{{ formatSize(item.fileSize) }}</small></td>
              <td>
                <span v-if="item.isDeleted" class="badge bg-soft-secondary">Deleted</span>
                <span v-else class="badge bg-soft-success">Active</span>
              </td>
              <td class="text-end">
                <template v-if="!item.isDeleted">
                  <button class="btn btn-sm btn-soft-primary" title="Download" @click="handleDownload(item.id)">
                    <i class="bi bi-download"></i>
                  </button>
                  <button class="btn btn-sm btn-soft-secondary" title="Edit" @click="openEditor(item)">
                    <i class="bi bi-image"></i>
                  </button>
                  <button class="btn btn-sm btn-soft-danger" title="Delete" @click="handleDelete(item.id)">
                    <i class="bi bi-trash"></i>
                  </button>
                </template>
              </td>
            </tr>
            <tr v-if="documents.length === 0">
              <td colspan="5">
                <div class="empty-state">
                  <i class="bi bi-file-earmark"></i>
                  <p class="small text-muted mb-0">No documents uploaded for this client.</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="editorLoading" class="text-center py-3">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <ImageEditor v-if="showEditor && editingImageFile" :image-src="editingImageFile"
      @close="showEditor = false; editingImageFile = null; editingImageDocId = null"
      @save="onEditorSave" />
  </div>
</template>
