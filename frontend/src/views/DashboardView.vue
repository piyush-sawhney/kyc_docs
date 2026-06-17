<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api/client'
import ClientSidebar from '../components/ClientSidebar.vue'
import DocumentTable from '../components/DocumentTable.vue'
import AuditTrail from '../components/AuditTrail.vue'
import PreviewDialog from '../components/PreviewDialog.vue'
import ImageEditor from '../components/ImageEditor.vue'

const selectedClientId = ref<string | null>(null)
const clientInfo = ref<any>(null)
const previewGroupId = ref<string | null>(null)
const previewDoc = ref<{ docId: string; docTypeName?: string; docNumber?: string } | null>(null)
const editingImageFile = ref<File | null>(null)
const editingImageDocId = ref<string | null>(null)
const editingImageLoading = ref(false)
const activeDocCount = ref(0)
const editingName = ref(false)
const editNameValue = ref('')
const editNameLoading = ref(false)
const nameInputRef = ref<HTMLElement | null>(null)
const sidebarWidth = ref(280)
const deleteClientDialog = ref(false)
const deleteClientLoading = ref(false)
const errorMsg = ref('')
const sidebarKey = ref(0)
const route = useRoute()
const router = useRouter()

onMounted(() => {
  if (route.query.selectClient) {
    selectedClientId.value = route.query.selectClient as string
    router.replace({ query: {} })
  }
})

watch(selectedClientId, async (id) => {
  if (!id) { clientInfo.value = null; return }
  try {
    const { data } = await api.get(`/clients/${id}`)
    clientInfo.value = data
  } catch (err) { console.error('Failed to load client', err); clientInfo.value = null }
})

function onSelectClient(id: string) {
  selectedClientId.value = id
}

function onPreview(groupId: string) {
  previewGroupId.value = groupId
}

function onPreviewImage(docId: string, docTypeName?: string, docNumber?: string) {
  previewDoc.value = { docId, docTypeName, docNumber }
}

async function onEditImage(docId: string) {
  editingImageLoading.value = true
  try {
    const res = await api.get(`/documents/${docId}/download`, { responseType: 'blob' })
    editingImageFile.value = new File([res.data], 'document.jpg', { type: res.data.type })
    editingImageDocId.value = docId
  } catch (err) {
    console.error('Failed to load image for editing', err)
  }
  editingImageLoading.value = false
}

function onUpdateCount(count: number) {
  activeDocCount.value = count
}

async function onImageSave(blob: Blob, filename: string) {
  const docId = editingImageDocId.value
  editingImageFile.value = null
  editingImageDocId.value = null
  if (!docId) return
  try {
    const fd = new FormData()
    fd.append('file', blob, filename)
    await api.put(`/documents/${docId}`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  } catch (err) {
    console.error('Failed to save edited image', err)
    errorMsg.value = 'Failed to save image'
  }
}

function startEditName() {
  if (!clientInfo.value) return
  editNameValue.value = clientInfo.value.name
  editingName.value = true
  nextTick(() => nameInputRef.value?.focus())
}

async function saveName() {
  if (!clientInfo.value || !editNameValue.value.trim()) {
    editingName.value = false
    return
  }
  editNameLoading.value = true
  try {
    await api.patch(`/clients/${clientInfo.value.id}`, { name: editNameValue.value.trim() })
    clientInfo.value.name = editNameValue.value.trim()
    editingName.value = false
  } catch (err) { console.error('Failed to update client name', err) }
  editNameLoading.value = false
}

function cancelEditName() {
  editingName.value = false
}

function handleAvatarClick() {
  if (!selectedClientId.value) return
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = async (e) => {
      const base64 = e.target?.result as string
      try {
        await api.patch(`/clients/${selectedClientId.value}`, { avatar: base64 })
        const { data } = await api.get(`/clients/${selectedClientId.value}`)
        clientInfo.value = data
      } catch (err) { console.error('Failed to update avatar', err) }
    }
    reader.readAsDataURL(file)
  }
  input.click()
}

function openDeleteClient() {
  deleteClientDialog.value = true
}

async function confirmDeleteClient() {
  if (!selectedClientId.value) return
  deleteClientLoading.value = true
  try {
    await api.delete(`/clients/${selectedClientId.value}`)
    deleteClientDialog.value = false
    selectedClientId.value = null
    clientInfo.value = null
    activeDocCount.value = 0
    sidebarKey.value++
  } catch (err: any) {
    console.error('Failed to delete client', err)
    errorMsg.value = err?.response?.data?.message || 'Failed to delete client'
    deleteClientDialog.value = false
  }
  deleteClientLoading.value = false
}

let resizeStartX = 0
let resizeStartW = 0
function onResizeStart(e: MouseEvent) {
  resizeStartX = e.clientX
  resizeStartW = sidebarWidth.value
  document.addEventListener('mousemove', onResizeMove)
  document.addEventListener('mouseup', onResizeEnd)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}
function onResizeMove(e: MouseEvent) {
  const delta = e.clientX - resizeStartX
  sidebarWidth.value = Math.max(220, Math.min(500, resizeStartW + delta))
}
function onResizeEnd() {
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}
</script>

<template>
  <div class="dashboard-layout">
    <div class="card border-0 shadow-sm sidebar-card" :style="{ width: sidebarWidth + 'px' }">
      <ClientSidebar :key="sidebarKey" :selected-client-id="selectedClientId" @select="onSelectClient" />
    </div>

    <div class="resize-handle" @mousedown.prevent="onResizeStart" />

    <div class="flex-grow-1 d-flex flex-column content-area">
      <div v-if="clientInfo" class="flex-shrink-0 mb-3">
        <div class="card border-0 shadow-sm px-4 py-3" style="border-radius: 12px;">
          <div class="d-flex align-items-center">
            <div class="d-flex align-items-center gap-3">
              <div class="position-relative cursor-pointer" @click="handleAvatarClick" style="cursor: pointer;">
                <div class="avatar-initials" style="width: 56px; height: 56px; font-size: 22px; background: rgba(30,58,95,0.08); color: #1E3A5F;">
                  <img v-if="clientInfo.avatar" :src="clientInfo.avatar" class="w-100 h-100 rounded-3" style="object-fit: cover;" />
                  <i v-else class="bi bi-person" style="font-size: 28px;"></i>
                </div>
                <button class="avatar-edit-btn btn btn-sm btn-light rounded-circle p-1 border shadow-sm"
                  title="Update photo">
                  <i class="bi bi-camera" style="font-size: 10px;"></i>
                </button>
              </div>
              <div v-if="!editingName" class="d-flex align-items-center gap-2">
                <h5 class="fw-semibold mb-0" style="color: #1E293B;">{{ clientInfo.name }}</h5>
                <button class="btn btn-sm btn-soft-secondary border-0" title="Edit name" @click="startEditName">
                  <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-soft-danger border-0" title="Delete client" @click="openDeleteClient">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
              <div v-else class="d-flex align-items-center gap-2">
                <div class="input-group input-group-sm" style="max-width: 240px;">
                  <input ref="nameInputRef" type="text" class="form-control" v-model="editNameValue"
                    :disabled="editNameLoading" @keyup.enter="saveName" @keyup.escape="cancelEditName" @blur="saveName" />
                  <span v-if="editNameLoading" class="input-group-text"><span class="spinner-border spinner-border-sm"></span></span>
                </div>
              </div>
            </div>
            <div class="ms-auto">
              <div class="text-center">
                <div class="fw-bold" style="font-size: 20px; color: #1E3A5F;">{{ activeDocCount }}</div>
                <small class="text-muted fw-medium">Active Documents</small>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="selectedClientId" class="flex-grow-1">
        <DocumentTable :client-id="selectedClientId"
          @preview="onPreview" @preview-image="onPreviewImage" @edit-image="onEditImage"
          @update-count="onUpdateCount" />
      </div>
      <div v-else class="flex-grow-1 d-flex align-items-center justify-content-center">
        <div class="text-center" style="max-width: 380px;">
          <div class="d-inline-flex align-items-center justify-content-center mb-3"
            style="width: 72px; height: 72px; border-radius: 50%; background: rgba(30,58,95,0.06);">
            <i class="bi bi-file-earmark-plus" style="font-size: 32px; color: #94A3B8;"></i>
          </div>
          <h6 class="fw-semibold mb-1" style="color: #1E293B;">No client selected</h6>
          <p class="small text-muted mb-3">Select a client from the sidebar or create a new one to start uploading documents.</p>
          <router-link to="/clients/new" class="btn btn-sm btn-primary">
            <i class="bi bi-plus me-1"></i> Create Client
          </router-link>
        </div>
      </div>

      <div class="mt-auto pt-3">
        <AuditTrail :client-id="selectedClientId" />
      </div>
    </div>

    <PreviewDialog v-if="previewGroupId" :group-id="previewGroupId"
      @close="previewGroupId = null" />

    <PreviewDialog v-if="previewDoc" :doc-id="previewDoc.docId"
      :doc-type-name="previewDoc.docTypeName" :doc-number="previewDoc.docNumber"
      @close="previewDoc = null" />

    <ImageEditor v-if="editingImageFile" :image-src="editingImageFile"
      @close="editingImageFile = null; editingImageDocId = null"
      @save="onImageSave" />

    <div v-if="editingImageLoading" class="text-center py-3">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <div class="modal-backdrop fade show" v-if="deleteClientDialog"></div>
    <div class="modal d-block" tabindex="-1" v-if="deleteClientDialog">
      <div class="modal-dialog modal-dialog-centered" style="max-width: 400px;">
        <div class="modal-content border-0 shadow">
          <div class="modal-body text-center p-4">
            <div class="d-inline-flex align-items-center justify-content-center mb-3"
              style="width: 48px; height: 48px; border-radius: 50%; background: rgba(220,38,38,0.1);">
              <i class="bi bi-exclamation-circle" style="font-size: 24px; color: #DC2626;"></i>
            </div>
            <h6 class="fw-semibold mb-1">Delete Client</h6>
            <p class="small text-muted mb-0">
              Delete <strong>{{ clientInfo?.name }}</strong> and all associated documents? This can be undone by an admin.
            </p>
          </div>
          <div class="modal-footer border-0 justify-content-center pt-0 pb-4">
            <button class="btn btn-danger" :disabled="deleteClientLoading" @click="confirmDeleteClient">
              <span v-if="deleteClientLoading" class="spinner-border spinner-border-sm me-1"></span>
              <i class="bi bi-trash me-1"></i> Delete
            </button>
            <button class="btn btn-outline-secondary" @click="deleteClientDialog = false">Cancel</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="errorMsg" class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 1060;">
      <div class="toast show align-items-center text-bg-danger border-0" role="alert">
        <div class="d-flex">
          <div class="toast-body">{{ errorMsg }}</div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" @click="errorMsg = ''"></button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-layout {
  display: flex;
  height: calc(100vh - 56px);
  gap: 0;
  padding: 16px;
  background: rgb(241, 245, 249);
}
.sidebar-card {
  height: 100%;
  overflow: hidden;
  flex-shrink: 0;
}
.resize-handle {
  width: 6px;
  cursor: col-resize;
  background: transparent;
  position: relative;
  flex-shrink: 0;
  margin: 0 4px;
}
.resize-handle::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  height: 32px;
  width: 3px;
  border-radius: 2px;
  background: rgb(203, 213, 225);
  transition: background 0.15s ease, height 0.15s ease;
}
.resize-handle:hover::after,
.resize-handle:active::after {
  background: rgb(100, 116, 139);
  height: 48px;
}
.content-area {
  overflow-y: auto;
  overflow-x: auto;
  height: 100%;
  scrollbar-gutter: stable;
}
.avatar-edit-btn {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
