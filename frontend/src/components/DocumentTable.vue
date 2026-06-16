<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import api from '../api/client'
import MetadataDialog from './MetadataDialog.vue'
import ImageEditor from './ImageEditor.vue'
import PdfConfigDialog from './PdfConfigDialog.vue'
import PdfPreviewDialog from './PdfPreviewDialog.vue'
import { generatePdf, type PdfConfig } from '../utils/generatePdf'

const emit = defineEmits<{
  preview: [groupId: string]
  previewImage: [docId: string, docTypeName?: string, docNumber?: string]
  editImage: [docId: string]
  needRefresh: []
  updateCount: [count: number]
}>()

const props = defineProps<{ clientId: string | null }>()

const groups = ref<any[]>([])
const documentTypes = ref<any[]>([])
const loading = ref(false)
const selected = ref<Record<string, boolean>>({})
const selectedCount = computed(() => Object.keys(selected.value).length)
const batchDeleteDialog = ref(false)
const singleDeleteDialog = ref(false)
const deleteDocId = ref('')
const deleteSide = ref('')
const deleting = ref(false)
const metadataDocId = ref<string | null>(null)
const typeAddDialog = ref(false)
const newTypeName = ref('')
const addingType = ref(false)
const errorMsg = ref('')

const pdfConfigDialog = ref(false)
const pdfGenerating = ref(false)
const pdfBlob = ref<Blob | null>(null)

const editingUploadFile = ref<File | null>(null)
const editingUploadCb = ref<((blob: Blob, filename: string) => Promise<void>) | null>(null)
async function onUploadSave(blob: Blob, filename: string) {
  const cb = editingUploadCb.value
  editingUploadFile.value = null
  editingUploadCb.value = null
  if (cb) await cb(blob, filename)
}

const autoCompleteItems = computed(() => [
  ...documentTypes.value,
  { name: '+ New Document Type', id: '__new__' },
])

interface NewRow {
  _key: number
  documentTypeId: string | null
  documentNumber: string
  expiryDate: string
  dupWarning: string | null
}

let nextKey = 1
const newRows = ref<NewRow[]>([])
const editingDates = ref<Record<string, string>>({})

const activeDocCount = computed(() => {
  let count = 0
  for (const g of groups.value) {
    if ((g.front && !g.front.isDeleted) || (g.back && !g.back.isDeleted)) count++
  }
  return count
})

onMounted(() => loadDocTypes())

watch(() => props.clientId, () => {
  selected.value = {}
  newRows.value = []
  editingDates.value = {}
  load()
})

async function loadDocTypes() {
  try {
    const { data } = await api.get('/document-types')
    documentTypes.value = data
  } catch (err) { console.error('Failed to load document types', err) }
}

async function load() {
  if (!props.clientId) return
  loading.value = true
  try {
    const { data } = await api.get(`/clients/${props.clientId}/documents/grouped`)
    const filtered = (data || []).filter((g: any) =>
      (g.front && !g.front.isDeleted) || (g.back && !g.back.isDeleted)
    )
    groups.value = filtered
    const visibleIds = new Set(filtered.map((g: any) => g.documentGroupId))
    const next: Record<string, boolean> = {}
    for (const key of Object.keys(selected.value)) {
      if (visibleIds.has(key)) next[key] = selected.value[key]
    }
    selected.value = next
    emit('updateCount', activeDocCount.value)
  } catch (err) { console.error('Failed to load documents', err) }
  loading.value = false
}

function toggleSelectAll() {
  if (selectedCount.value === groups.value.length) {
    selected.value = {}
  } else {
    const obj: Record<string, boolean> = {}
    for (const g of groups.value) obj[g.documentGroupId] = true
    selected.value = obj
  }
}

function toggleSelect(gid: string) {
  selected.value = { ...selected.value, [gid]: !selected.value[gid] }
}

function clearSelection() {
  selected.value = {}
}

const allSelected = computed(() =>
  groups.value.length > 0 && selectedCount.value === groups.value.length
)

function requestDelete(docId: string, side: string) {
  deleteDocId.value = docId
  deleteSide.value = side
  singleDeleteDialog.value = true
}

async function executeDelete() {
  if (!deleteDocId.value) return
  deleting.value = true
  try {
    await api.patch(`/documents/${deleteDocId.value}/clear-image`)
    singleDeleteDialog.value = false
    deleteDocId.value = ''
    deleteSide.value = ''
    await load()
    emit('needRefresh')
  } catch (err: any) {
    console.error('Failed to clear document image', err)
    errorMsg.value = err?.response?.data?.message || 'Failed to clear document image'
    singleDeleteDialog.value = false
  }
  deleting.value = false
}

async function executeBatchDelete() {
  try {
    const ids: string[] = []
    for (const g of groups.value) {
      if (g.documentGroupId && selected.value[g.documentGroupId]) {
        if (g.front && !g.front.isDeleted) ids.push(g.front.id)
        if (g.back && !g.back.isDeleted) ids.push(g.back.id)
      }
    }
    if (ids.length === 0) {
      batchDeleteDialog.value = false
      clearSelection()
      await load()
      emit('needRefresh')
      return
    }
    deleting.value = true
    await Promise.all(ids.map((id) => api.delete(`/documents/${id}`)))
    batchDeleteDialog.value = false
    clearSelection()
    await load()
    emit('needRefresh')
  } catch (err: any) {
    console.error('[BatchDelete] error:', err)
    errorMsg.value = err?.response?.data?.message || 'Failed to delete documents'
    batchDeleteDialog.value = false
    clearSelection()
  }
  deleting.value = false
}

async function handleGeneratePdf(config: PdfConfig) {
  const docIds: string[] = []
  for (const g of groups.value) {
    if (g.documentGroupId && selected.value[g.documentGroupId]) {
      if (g.front && !g.front.isDeleted && g.front.fileSize != null) docIds.push(g.front.id)
      if (g.back && !g.back.isDeleted && g.back.fileSize != null) docIds.push(g.back.id)
    }
  }
  if (docIds.length === 0) return
  pdfConfigDialog.value = false
  pdfGenerating.value = true
  try {
    const blobs = await Promise.all(
      docIds.map((id) =>
        api.get(`/documents/${id}/download`, { responseType: 'blob' }).then((r) => r.data),
      ),
    )
    const images = blobs.map((blob) => ({ blob }))
    pdfBlob.value = await generatePdf(images, config)
  } catch (err: any) {
    errorMsg.value = err?.message || 'Failed to generate PDF'
  }
  pdfGenerating.value = false
}

let dupTimeout: ReturnType<typeof setTimeout> | null = null
async function checkDuplicate(nr: NewRow) {
  if (!nr.documentNumber) { nr.dupWarning = null; return }
  try {
    const { data } = await api.get(`/documents/check-number/${encodeURIComponent(nr.documentNumber)}`)
    nr.dupWarning = data.exists ? `Already linked to ${data.clientName}` : null
  } catch { nr.dupWarning = null }
}

function onDocNumberInput(nr: NewRow) {
  nr.documentNumber = nr.documentNumber.toUpperCase()
  if (dupTimeout) clearTimeout(dupTimeout)
  dupTimeout = setTimeout(() => checkDuplicate(nr), 400)
}

const saveTimeouts = new Map<string, ReturnType<typeof setTimeout>>()

function scheduleSave(group: any, field: string, value: any) {
  const gid = group.documentGroupId
  if (!gid) return
  const docId = group.front?.id || group.back?.id
  if (!docId) return
  if (saveTimeouts.has(gid)) clearTimeout(saveTimeouts.get(gid)!)
  saveTimeouts.set(gid, setTimeout(async () => {
    saveTimeouts.delete(gid)
    if (field === 'documentNumber') value = value?.toUpperCase()
    const single: Record<string, any> = {}
    if (field === 'documentType') {
      single.documentTypeId = value?.id || null
      group.documentType = value || null
    } else {
      single[field] = value || null
    }
    try { await api.patch(`/documents/${docId}/metadata`, single) } catch (err) { console.error('Failed to save metadata', err) }
  }, 500))
}

function startEditDate(group: any, field: 'issueDate' | 'expiryDate') {
  const key = `${group.documentGroupId}-${field}`
  editingDates.value[key] = group.front?.[field] || group.back?.[field] || ''
}

function saveDate(group: any, field: 'issueDate' | 'expiryDate') {
  const key = `${group.documentGroupId}-${field}`
  const val = editingDates.value[key]
  if (group.front) group.front[field] = val
  if (group.back) group.back[field] = val
  scheduleSave(group, field, val)
  delete editingDates.value[key]
}

function isEditingDate(group: any, field: string): boolean {
  return `${group.documentGroupId}-${field}` in editingDates.value
}

function dateKey(group: any, field: string): string {
  return `${group.documentGroupId}-${field}`
}

function updateDateEdit(group: any, field: string, val: string) {
  editingDates.value[`${group.documentGroupId}-${field}`] = val
}

function addRow() {
  newRows.value.push({
    _key: nextKey++,
    documentTypeId: null,
    documentNumber: '',
    expiryDate: '',
    dupWarning: null,
  })
}

function cancelRow(nr: NewRow) {
  newRows.value = newRows.value.filter((r) => r._key !== nr._key)
}

function handleTypeChange(ev: Event, group?: any, nr?: NewRow) {
  const val = (ev.target as HTMLSelectElement).value || null
  if (val === '__new__') {
    newTypeName.value = ''
    typeAddDialog.value = true
    return
  }
  if (nr) {
    nr.documentTypeId = val
    return
  }
  if (group && val) {
    const newType = documentTypes.value.find((t: any) => t.id === val) || null
    scheduleSave(group, 'documentType', newType)
  }
}

async function handleAddType() {
  if (!newTypeName.value) return
  addingType.value = true
  try {
    const { data } = await api.post('/document-types', { name: newTypeName.value })
    documentTypes.value.push(data)
    typeAddDialog.value = false
  } catch (err) { console.error('Failed to add document type', err) }
  addingType.value = false
}

function uploadForRow(nr: NewRow, sideVal: 'front' | 'back') {
  if (!nr.documentTypeId || !nr.documentNumber) return
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png'
  input.onchange = () => {
    const file = input.files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/')) {
      errorMsg.value = 'Only JPEG and PNG images are allowed'
      return
    }
    editingUploadFile.value = file
    editingUploadCb.value = async (blob: Blob, filename: string) => {
      const fd = new FormData()
      fd.append('file', blob, filename)
      fd.append('documentTypeId', nr.documentTypeId!)
      fd.append('documentNumber', nr.documentNumber)
      fd.append('side', sideVal)
      if (nr.expiryDate) fd.append('expiryDate', nr.expiryDate)
      try {
        await api.post(`/clients/${props.clientId}/documents`, fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        cancelRow(nr)
        await load()
        emit('needRefresh')
      } catch (err: any) {
        errorMsg.value = err?.response?.data?.message || 'Failed to upload document'
      }
    }
  }
  input.click()
}

function uploadExisting(sideVal: 'front' | 'back', groupId?: string) {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png'
  input.onchange = () => {
    const file = input.files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/')) {
      errorMsg.value = 'Only JPEG and PNG images are allowed'
      return
    }
    const existing = groups.value.find((g: any) => g.documentGroupId === groupId)
    if (!existing) return
    editingUploadFile.value = file
    editingUploadCb.value = async (blob: Blob, filename: string) => {
      const fd = new FormData()
      fd.append('file', blob, filename)
      fd.append('documentTypeId', existing.documentType?.id || '')
      fd.append('documentNumber', existing.documentNumber || '')
      fd.append('side', sideVal)
      if (groupId) fd.append('documentGroupId', groupId)
      const expiryDate = existing.front?.expiryDate || existing.back?.expiryDate
      if (expiryDate) fd.append('expiryDate', expiryDate)
      try {
        await api.post(`/clients/${props.clientId}/documents`, fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        await load()
        emit('needRefresh')
      } catch (err: any) {
        errorMsg.value = err?.response?.data?.message || 'Failed to re-upload document'
      }
    }
  }
  input.click()
}

function formatDate(d: string | null | undefined) {
  if (!d) return '—'
  const parts = d.split('T')[0].split('-')
  if (parts.length !== 3) return d
  return `${parts[2]}-${parts[1]}-${parts[0]}`
}

function getDocId(group: any) {
  return group.front?.id || group.back?.id || ''
}

async function downloadDoc(id: string, filename: string) {
  try {
    const res = await api.get(`/documents/${id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = filename || 'document'
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) { console.error('Failed to download document', err) }
}

const revealedNumbers = ref<Record<string, string>>({})
const revealTimers: Record<string, ReturnType<typeof setTimeout>> = {}

async function revealNumber(groupId: string, docId: string) {
  if (revealedNumbers.value[groupId]) return
  try {
    const { data } = await api.get(`/documents/${docId}/decrypt-number`)
    revealedNumbers.value = { ...revealedNumbers.value, [groupId]: (data || '').toUpperCase() }
    if (revealTimers[groupId]) clearTimeout(revealTimers[groupId])
    revealTimers[groupId] = setTimeout(() => {
      const next = { ...revealedNumbers.value }
      delete next[groupId]
      revealedNumbers.value = next
    }, 5000)
  } catch (err) { console.error('Failed to reveal number', err) }
}

function hideNumber(groupId: string) {
  const next = { ...revealedNumbers.value }
  delete next[groupId]
  revealedNumbers.value = next
}
</script>

<template>
  <div>
    <div class="card border-0 shadow-sm" style="border-radius: 12px;">
      <div class="d-flex align-items-center px-3 py-1 border-bottom">
        <div class="ms-auto">
          <span v-if="selectedCount" class="badge bg-soft-primary me-2">{{ selectedCount }} selected</span>
        </div>
      </div>

      <div class="table-wrapper">
        <table class="table mb-0" style="min-width: 994px;">
          <thead>
            <tr>
              <th class="col-check text-center">
                <input type="checkbox" class="form-check-input m-0 position-static" :checked="allSelected"
                  @change="toggleSelectAll" />
              </th>
              <th style="width: 170px;">Document Type</th>
              <th style="width: 170px;">Document Number</th>
              <th style="width: 130px;">Expiry Date</th>
              <th class="text-center" style="width: 220px;">Front</th>
              <th class="text-center" style="width: 220px;">Back</th>
              <th class="text-center" style="width: 44px;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="g in groups" :key="g.documentGroupId"
              :class="{ 'row-selected': selected[g.documentGroupId] }">
              <td class="col-check text-center">
                <input type="checkbox" class="form-check-input m-0 position-static"
                  :checked="!!selected[g.documentGroupId]"
                  @change="toggleSelect(g.documentGroupId)" />
              </td>
              <td>
                <select class="form-select form-select-sm border-0 bg-transparent px-1"
                  :value="g.documentType?.id || ''"
                  @change="handleTypeChange($event, g, undefined)">
                  <option value="" disabled>Select type</option>
                  <option v-for="t in documentTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
                  <option disabled>──────────</option>
                  <option value="__new__">+ New Document Type</option>
                </select>
              </td>
              <td>
                <div class="d-flex align-items-center gap-1">
                  <span class="number-masked small fw-medium"
                    :class="revealedNumbers[g.documentGroupId] ? 'text-primary' : ''">
                    {{ revealedNumbers[g.documentGroupId] || '••••' + (g.documentNumber || '').toUpperCase() }}
                  </span>
                  <button class="btn btn-sm btn-link text-decoration-none p-0"
                    :title="revealedNumbers[g.documentGroupId] ? 'Hide number' : 'Reveal full number'"
                    @click="revealedNumbers[g.documentGroupId] ? hideNumber(g.documentGroupId) : revealNumber(g.documentGroupId, getDocId(g))">
                    <i class="bi" :class="revealedNumbers[g.documentGroupId] ? 'bi-eye-slash' : 'bi-eye'"
                      :style="{ color: revealedNumbers[g.documentGroupId] ? '#1E3A5F' : '#94A3B8', fontSize: '12px' }"></i>
                  </button>
                </div>
              </td>
              <td>
                <div v-if="!isEditingDate(g, 'expiryDate')" class="editable-cell" @click="startEditDate(g, 'expiryDate')">
                  <span class="date-text">{{ formatDate(g.front?.expiryDate || g.back?.expiryDate) }}</span>
                  <i class="bi bi-pencil ms-1 edit-hint" style="font-size: 10px; color: #CBD5E1;"></i>
                </div>
                <input v-else type="date" class="form-control form-control-sm"
                  :value="editingDates[dateKey(g, 'expiryDate')]"
                  @input="updateDateEdit(g, 'expiryDate', ($event.target as HTMLInputElement).value)"
                  @blur="saveDate(g, 'expiryDate')"
                  @change="saveDate(g, 'expiryDate')" autofocus />
              </td>
              <td class="text-center">
                <template v-if="g.front && !g.front.isDeleted && g.front.fileSize != null">
                  <div class="d-flex align-items-center justify-content-center gap-0 flex-nowrap">
                    <span class="badge bg-soft-success me-1" style="font-size: 10px;">
                      <i class="bi bi-check-circle me-1" style="font-size: 9px;"></i> Uploaded
                    </span>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Re-upload"
                      @click="uploadExisting('front', g.documentGroupId)">
                      <i class="bi bi-cloud-upload" style="color: #1E3A5F; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Edit image"
                      @click="emit('editImage', g.front.id)">
                      <i class="bi bi-pencil-square" style="color: #0284C7; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Preview"
                      @click="emit('previewImage', g.front.id, g.documentType?.name, g.documentNumber)">
                      <i class="bi bi-eye" style="color: #6366F1; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Download"
                      @click="downloadDoc(g.front.id, `front_${g.documentNumber || g.documentGroupId}.jpg`)">
                      <i class="bi bi-download" style="color: #64748B; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Delete"
                      @click="requestDelete(g.front.id, 'Front')">
                      <i class="bi bi-trash" style="color: #DC2626; font-size: 14px;"></i>
                    </button>
                  </div>
                </template>
                <template v-else>
                  <button class="btn btn-sm btn-outline-primary" title="Upload Front"
                    @click="uploadExisting('front', g.documentGroupId)">
                    <i class="bi bi-cloud-upload"></i>
                  </button>
                </template>
              </td>
              <td class="text-center">
                <template v-if="g.back && !g.back.isDeleted && g.back.fileSize != null">
                  <div class="d-flex align-items-center justify-content-center gap-0 flex-nowrap">
                    <span class="badge bg-soft-success me-1" style="font-size: 10px;">
                      <i class="bi bi-check-circle me-1" style="font-size: 9px;"></i> Uploaded
                    </span>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Re-upload"
                      @click="uploadExisting('back', g.documentGroupId)">
                      <i class="bi bi-cloud-upload" style="color: #1E3A5F; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Edit image"
                      @click="emit('editImage', g.back.id)">
                      <i class="bi bi-pencil-square" style="color: #0284C7; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Preview"
                      @click="emit('previewImage', g.back.id, g.documentType?.name, g.documentNumber)">
                      <i class="bi bi-eye" style="color: #6366F1; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Download"
                      @click="downloadDoc(g.back.id, `back_${g.documentNumber || g.documentGroupId}.jpg`)">
                      <i class="bi bi-download" style="color: #64748B; font-size: 14px;"></i>
                    </button>
                    <button class="btn btn-sm btn-link text-decoration-none p-0 px-1" title="Delete"
                      @click="requestDelete(g.back.id, 'Back')">
                      <i class="bi bi-trash" style="color: #DC2626; font-size: 14px;"></i>
                    </button>
                  </div>
                </template>
                <template v-else>
                  <button class="btn btn-sm btn-outline-primary" title="Upload Back"
                    @click="uploadExisting('back', g.documentGroupId)">
                    <i class="bi bi-cloud-upload"></i>
                  </button>
                </template>
              </td>
              <td class="text-center">
                <button class="btn btn-sm btn-link text-decoration-none p-0" title="Metadata"
                  @click="metadataDocId = getDocId(g)">
                  <i class="bi bi-info-circle" style="color: #CBD5E1; font-size: 14px;"></i>
                </button>
              </td>
            </tr>

            <tr v-for="nr in newRows" :key="nr._key" class="new-row">
              <td></td>
              <td>
                <select class="form-select form-select-sm" v-model="nr.documentTypeId"
                  @change="handleTypeChange($event, undefined, nr)">
                  <option value="" disabled>Select type</option>
                  <option v-for="t in documentTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
                  <option disabled>──────────</option>
                  <option value="__new__">+ New Document Type</option>
                </select>
              </td>
              <td>
                <div>
                  <input type="text" class="form-control form-control-sm" v-model="nr.documentNumber"
                    placeholder="Document number" :class="{ 'is-invalid': !!nr.dupWarning }"
                    @input="onDocNumberInput(nr)" />
                  <div v-if="nr.dupWarning" class="invalid-feedback d-block small">
                    <i class="bi bi-exclamation-triangle me-1"></i>{{ nr.dupWarning }}
                  </div>
                </div>
              </td>
              <td>
                <input type="date" class="form-control form-control-sm" v-model="nr.expiryDate" />
              </td>
              <td class="text-center">
                <button class="btn btn-sm btn-outline-primary" title="Upload Front"
                  :disabled="!nr.documentTypeId || !nr.documentNumber"
                  @click="uploadForRow(nr, 'front')">
                  <i class="bi bi-cloud-upload"></i>
                </button>
              </td>
              <td class="text-center">
                <button class="btn btn-sm btn-outline-primary" title="Upload Back"
                  :disabled="!nr.documentTypeId || !nr.documentNumber"
                  @click="uploadForRow(nr, 'back')">
                  <i class="bi bi-cloud-upload"></i>
                </button>
              </td>
              <td class="text-center">
                <button class="btn btn-sm btn-link text-decoration-none p-0 text-muted" title="Cancel"
                  @click="cancelRow(nr)">
                  <i class="bi bi-x" style="font-size: 16px;"></i>
                </button>
              </td>
            </tr>

            <tr v-if="groups.length === 0 && newRows.length === 0">
              <td colspan="7">
                <div class="empty-state">
                  <i class="bi bi-file-earmark-text"></i>
                  <p class="small text-muted fw-medium mb-1">No documents yet</p>
                  <p class="small text-muted mb-0">
                    {{ props.clientId ? 'Click "Add Row" below to get started' : 'Select a client from the sidebar' }}
                  </p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="d-flex align-items-center px-3 py-2 border-top">
        <div class="d-flex gap-2">
          <button class="btn btn-sm btn-primary" @click="addRow">
            <i class="bi bi-plus me-1"></i> Add Row
          </button>
          <button class="btn btn-sm btn-outline-danger" :disabled="selectedCount === 0"
            @click="batchDeleteDialog = true">
            <i class="bi bi-trash me-1"></i> Delete
          </button>
          <button class="btn btn-sm btn-outline-primary" :disabled="selectedCount === 0 || pdfGenerating"
            @click="pdfConfigDialog = true">
            <span v-if="pdfGenerating" class="spinner-border spinner-border-sm me-1"></span>
            <i class="bi bi-filetype-pdf me-1"></i> Generate PDF
          </button>
        </div>
      </div>
    </div>

    <MetadataDialog v-if="metadataDocId" :document-id="metadataDocId"
      @close="metadataDocId = null" />

    <div class="modal-backdrop fade show" v-if="singleDeleteDialog"></div>
    <div class="modal d-block" tabindex="-1" v-if="singleDeleteDialog">
      <div class="modal-dialog modal-dialog-centered" style="max-width: 380px;">
        <div class="modal-content border-0 shadow">
          <div class="modal-body text-center p-4">
            <div class="d-inline-flex align-items-center justify-content-center mb-3"
              style="width: 48px; height: 48px; border-radius: 50%; background: rgba(220,38,38,0.1);">
              <i class="bi bi-exclamation-circle" style="font-size: 24px; color: #DC2626;"></i>
            </div>
            <h6 class="fw-semibold mb-1">Delete {{ deleteSide }} Document</h6>
            <p class="small text-muted mb-0">This will permanently delete this image. This cannot be undone.</p>
          </div>
          <div class="modal-footer border-0 justify-content-center pt-0 pb-4">
            <button class="btn btn-danger" :disabled="deleting" @click="executeDelete">
              <span v-if="deleting" class="spinner-border spinner-border-sm me-1"></span>
              <i class="bi bi-trash me-1"></i> Delete
            </button>
            <button class="btn btn-outline-secondary" @click="singleDeleteDialog = false">Cancel</button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-backdrop fade show" v-if="batchDeleteDialog"></div>
    <div class="modal d-block" tabindex="-1" v-if="batchDeleteDialog">
      <div class="modal-dialog modal-dialog-centered" style="max-width: 380px;">
        <div class="modal-content border-0 shadow">
          <div class="modal-body text-center p-4">
            <div class="d-inline-flex align-items-center justify-content-center mb-3"
              style="width: 48px; height: 48px; border-radius: 50%; background: rgba(220,38,38,0.1);">
              <i class="bi bi-exclamation-circle" style="font-size: 24px; color: #DC2626;"></i>
            </div>
            <h6 class="fw-semibold mb-1">Delete Documents</h6>
            <p class="small text-muted mb-0">
              Delete {{ selectedCount }} document row(s) permanently? This cannot be undone.
            </p>
          </div>
          <div class="modal-footer border-0 justify-content-center pt-0 pb-4">
            <button class="btn btn-danger" :disabled="deleting" @click="executeBatchDelete">
              <span v-if="deleting" class="spinner-border spinner-border-sm me-1"></span>
              <i class="bi bi-trash me-1"></i> Delete
            </button>
            <button class="btn btn-outline-secondary" @click="batchDeleteDialog = false">Cancel</button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-backdrop fade show" v-if="typeAddDialog"></div>
    <div class="modal d-block" tabindex="-1" v-if="typeAddDialog">
      <div class="modal-dialog modal-dialog-centered" style="max-width: 400px;">
        <div class="modal-content border-0 shadow">
          <div class="modal-header border-0 pb-0">
            <h6 class="fw-bold mb-0">Add Document Type</h6>
          </div>
          <div class="modal-body">
            <input type="text" class="form-control" v-model="newTypeName"
              placeholder="Document Type Name" @keyup.enter="handleAddType" autofocus />
          </div>
          <div class="modal-footer border-0 pt-0">
            <button class="btn btn-primary" :disabled="addingType" @click="handleAddType">
              <span v-if="addingType" class="spinner-border spinner-border-sm me-1"></span>
              <i class="bi bi-check me-1"></i> Create
            </button>
            <button class="btn btn-outline-secondary" @click="typeAddDialog = false">Cancel</button>
          </div>
        </div>
      </div>
    </div>

    <ImageEditor v-if="editingUploadFile" :image-src="editingUploadFile"
      @close="editingUploadFile = null; editingUploadCb = null"
      @save="onUploadSave" />

    <PdfConfigDialog v-if="pdfConfigDialog" @close="pdfConfigDialog = false"
      @generate="handleGeneratePdf" />

    <PdfPreviewDialog v-if="pdfBlob" :pdf-blob="pdfBlob"
      @close="pdfBlob = null" />

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
.table thead {
  position: sticky;
  top: 0;
  z-index: 2;
}
.table-wrapper {
  overflow: clip;
}
.table tbody tr.row-selected {
  background: rgba(var(--bs-primary-rgb), 0.05);
}
.new-row td {
  background: rgba(var(--bs-primary-rgb), 0.03);
  border-bottom: 1px solid rgba(var(--bs-primary-rgb), 0.08) !important;
}
.number-masked {
  font-family: 'Inter', monospace;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.editable-cell {
  display: flex;
  align-items: center;
  cursor: pointer;
  border-radius: 4px;
  padding: 2px 6px;
  min-height: 28px;
  transition: background 0.12s ease;
}
.editable-cell:hover {
  background: rgba(30, 58, 95, 0.05);
}
.date-text {
  font-family: 'Inter', monospace;
  font-size: 13px;
  font-weight: 500;
  color: rgb(71, 85, 105);
  letter-spacing: 0.03em;
  font-variant-numeric: tabular-nums;
}
.edit-hint {
  opacity: 0;
  transition: opacity 0.12s ease;
}
.editable-cell:hover .edit-hint {
  opacity: 1;
}
.form-select-sm {
  font-size: 13px;
}
.form-control-sm {
  font-size: 13px;
}
</style>
