<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
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
const errorSnackbar = ref(false)
const errorMsg = ref('')
function showError(msg: string) {
  errorMsg.value = msg
  errorSnackbar.value = true
}

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
    showError('Failed to save image')
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
  } catch (err: any) {
    console.error('Failed to delete client', err)
    showError(err?.response?.data?.message || 'Failed to delete client')
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
    <v-card class="sidebar-card border rounded-xl" :style="{ width: sidebarWidth + 'px' }">
      <ClientSidebar :selected-client-id="selectedClientId" @select="onSelectClient" />
    </v-card>

    <div class="resize-handle" @mousedown.prevent="onResizeStart" />

    <div class="flex-grow-1 d-flex flex-column content-area">
      <div v-if="clientInfo" class="flex-shrink-0 mb-3">
        <v-card class="rounded-xl px-5 py-4" flat>
          <div class="d-flex align-center">
            <div class="d-flex align-center ga-4">
              <v-avatar size="56" color="primary" variant="tonal" class="cursor-pointer" @click="handleAvatarClick">
                <v-img v-if="clientInfo.avatar" :src="clientInfo.avatar" cover />
                <v-icon v-else size="28" color="primary">mdi-account</v-icon>
                <v-btn icon size="x-small" color="primary" variant="tonal"
                  class="avatar-edit-btn" title="Update photo">
                  <v-icon size="12">mdi-camera</v-icon>
                </v-btn>
              </v-avatar>
                <div v-if="!editingName" class="d-flex align-center ga-2">
                  <div class="text-h5 font-weight-semibold text-grey-darken-3">{{ clientInfo.name }}</div>
                  <v-btn icon variant="text" size="small" color="grey" title="Edit name"
                    @click="startEditName">
                    <v-icon size="16">mdi-pencil</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" color="error" title="Delete client"
                    @click="openDeleteClient">
                    <v-icon size="16">mdi-delete-outline</v-icon>
                  </v-btn>
                </div>
              <div v-else class="d-flex align-center ga-2">
                <v-text-field ref="nameInputRef" v-model="editNameValue" variant="outlined"
                  density="compact" hide-details single-line class="name-input"
                  :loading="editNameLoading"
                  @keyup.enter="saveName" @keyup.escape="cancelEditName" @blur="saveName" />
              </div>
            </div>
            <v-spacer />
            <div class="d-flex align-center ga-4">
              <div class="text-center">
                <div class="text-h6 font-weight-bold text-primary">{{ activeDocCount }}</div>
                <div class="text-caption text-grey font-weight-medium">Active Documents</div>
              </div>
            </div>
          </div>
        </v-card>
      </div>

      <DocumentTable :client-id="selectedClientId"
        @preview="onPreview" @preview-image="onPreviewImage" @edit-image="onEditImage"
        @update-count="onUpdateCount" />

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
    <v-progress-circular v-if="editingImageLoading" indeterminate size="24" width="2" color="primary"
      class="ma-auto" />

    <v-dialog v-model="deleteClientDialog" max-width="400">
      <v-card>
        <v-card-text class="pa-6 text-center">
          <v-avatar color="error" variant="tonal" size="48" class="mb-3">
            <v-icon color="error" size="28">mdi-alert-circle</v-icon>
          </v-avatar>
          <div class="text-body-1 font-weight-medium mb-1">Delete Client</div>
          <div class="text-body-2 text-grey">
            Delete <strong>{{ clientInfo?.name }}</strong> and all associated documents permanently? This cannot be undone.
          </div>
        </v-card-text>
        <v-card-actions class="pa-4 pt-0 justify-center ga-2">
          <v-btn color="error" :loading="deleteClientLoading" @click="confirmDeleteClient"
            prepend-icon="mdi-delete" variant="tonal">Delete</v-btn>
          <v-btn variant="text" @click="deleteClientDialog = false">Cancel</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="errorSnackbar" color="error" variant="tonal" location="top" :timeout="4000">
      {{ errorMsg }}
    </v-snackbar>
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
  background: white;
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
  overflow-x: hidden;
  height: 100%;
}

.avatar-edit-btn {
  position: absolute;
  bottom: -2px;
  right: -2px;
}

.name-input {
  min-width: 200px;
}
</style>
