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
const errorSnackbar = ref(false)
const errorMsg = ref('')
function showError(msg: string) {
  errorMsg.value = msg
  errorSnackbar.value = true
}

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
  { name: 'New Document Type', id: '__new__' },
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
    showError(err?.response?.data?.message || 'Failed to clear document image')
    singleDeleteDialog.value = false
  }
  deleting.value = false
}

async function executeBatchDelete() {
  try {
    console.log('[BatchDelete] selected keys:', Object.keys(selected.value), 'groups count:', groups.value.length)
    const ids: string[] = []
    for (const g of groups.value) {
      if (g.documentGroupId && selected.value[g.documentGroupId]) {
        if (g.front && !g.front.isDeleted) ids.push(g.front.id)
        if (g.back && !g.back.isDeleted) ids.push(g.back.id)
      }
    }
    console.log('[BatchDelete] doc IDs to delete:', ids)
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
    showError(err?.response?.data?.message || 'Failed to delete documents')
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
    showError(err?.message || 'Failed to generate PDF')
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

function handleTypeChange(val: string | null, group?: any, nr?: NewRow) {
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

function uploadForRow(nr: NewRow, side: 'front' | 'back') {
  if (!nr.documentTypeId || !nr.documentNumber) return
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png'
  input.onchange = () => {
    const file = input.files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/')) {
      showError('Only JPEG and PNG images are allowed')
      return
    }
    editingUploadFile.value = file
    editingUploadCb.value = async (blob: Blob, filename: string) => {
      const fd = new FormData()
      fd.append('file', blob, filename)
      fd.append('documentTypeId', nr.documentTypeId!)
      fd.append('documentNumber', nr.documentNumber)
      fd.append('side', side)
      if (nr.expiryDate) fd.append('expiryDate', nr.expiryDate)
      try {
        await api.post(`/clients/${props.clientId}/documents`, fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        cancelRow(nr)
        await load()
        emit('needRefresh')
      } catch (err: any) {
        showError(err?.response?.data?.message || 'Failed to upload document')
      }
    }
  }
  input.click()
}

function uploadExisting(side: 'front' | 'back', groupId?: string) {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png'
  input.onchange = () => {
    const file = input.files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/')) {
      showError('Only JPEG and PNG images are allowed')
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
      fd.append('side', side)
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
        showError(err?.response?.data?.message || 'Failed to re-upload document')
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
    <v-card class="rounded-xl" flat>
      <div class="d-flex align-center px-5 py-2">
        <v-spacer />
        <v-fade-transition>
          <v-chip v-if="selectedCount" color="primary" variant="tonal" size="small" label
            closable @click:close="clearSelection" class="font-weight-medium">
            {{ selectedCount }} selected
          </v-chip>
        </v-fade-transition>
      </div>

      <v-divider />

      <v-table class="premium-table">
        <thead>
          <tr>
            <th class="text-center col-check">
              <v-checkbox :model-value="allSelected" @update:model-value="toggleSelectAll"
                hide-details density="compact" color="primary" />
            </th>
            <th class="col-type">Document Type</th>
            <th class="col-number">Document Number</th>
            <th class="col-date">Expiry Date</th>
            <th class="text-center col-side">Front</th>
            <th class="text-center col-side">Back</th>
            <th class="text-center col-meta"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="g in groups" :key="g.documentGroupId" class="premium-row"
            :class="{ 'row-selected': selected[g.documentGroupId] }">
            <td class="text-center col-check">
              <v-checkbox :model-value="!!selected[g.documentGroupId]"
                @update:model-value="toggleSelect(g.documentGroupId)"
                hide-details density="compact" color="primary" />
            </td>
            <td class="col-type">
              <v-autocomplete :model-value="g.documentType?.id || null"
                @update:model-value="handleTypeChange($event, g, undefined)"
                :items="autoCompleteItems" item-title="name" item-value="id"
                variant="plain" density="compact" hide-details
                color="primary" class="cell-input" />
            </td>
            <td class="col-number">
              <div class="d-flex align-center ga-1">
                <span class="number-masked text-body-2 font-weight-medium"
                  :class="{ 'text-grey-darken-2': !revealedNumbers[g.documentGroupId], 'text-primary': revealedNumbers[g.documentGroupId] }">
                  {{ revealedNumbers[g.documentGroupId] || '••••' + (g.documentNumber || '').toUpperCase() }}
                </span>
                <v-btn icon variant="text" size="x-small" color="grey"
                  :title="revealedNumbers[g.documentGroupId] ? 'Hide number' : 'Reveal full number'"
                  @click="revealedNumbers[g.documentGroupId] ? hideNumber(g.documentGroupId) : revealNumber(g.documentGroupId, getDocId(g))">
                  <v-icon size="14" :color="revealedNumbers[g.documentGroupId] ? 'primary' : 'grey'">
                    {{ revealedNumbers[g.documentGroupId] ? 'mdi-eye-off-outline' : 'mdi-eye-outline' }}
                  </v-icon>
                </v-btn>
              </div>
            </td>
            <td class="col-date">
              <div v-if="!isEditingDate(g, 'expiryDate')" class="editable-cell" @click="startEditDate(g, 'expiryDate')">
                <span class="date-text">{{ formatDate(g.front?.expiryDate || g.back?.expiryDate) }}</span>
                <v-icon size="11" color="grey-lighten-2" class="ml-1 edit-hint">mdi-pencil</v-icon>
              </div>
              <v-text-field v-else :model-value="editingDates[dateKey(g, 'expiryDate')]"
                @update:model-value="updateDateEdit(g, 'expiryDate', $event)"
                type="date" variant="plain" density="compact" hide-details
                color="primary" autofocus class="cell-input"
                @blur="saveDate(g, 'expiryDate')"
                @change="saveDate(g, 'expiryDate')" />
            </td>
            <td class="text-center col-side">
              <template v-if="g.front && !g.front.isDeleted && g.front.fileSize != null">
                <div class="d-flex align-center justify-center ga-0 flex-nowrap">
                  <v-chip color="success" variant="tonal" size="x-small" label class="flex-shrink-0 font-weight-medium px-1">
                    <v-icon start size="11">mdi-check-circle</v-icon> Uploaded
                  </v-chip>
                  <v-btn icon variant="text" size="small" title="Re-upload"
                    @click="uploadExisting('front', g.documentGroupId)">
                    <v-icon size="18" color="primary">mdi-cloud-upload-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Edit image"
                    @click="emit('editImage', g.front.id)">
                    <v-icon size="18" color="info">mdi-image-edit-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Preview"
                    @click="emit('previewImage', g.front.id, g.documentType?.name, g.documentNumber)">
                    <v-icon size="18" color="accent">mdi-eye-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Download"
                    @click="downloadDoc(g.front.id, `front_${g.documentNumber || g.documentGroupId}.jpg`)">
                    <v-icon size="18" color="grey">mdi-download-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Delete"
                    @click="requestDelete(g.front.id, 'Front')">
                    <v-icon size="18" color="error">mdi-trash-can-outline</v-icon>
                  </v-btn>
                </div>
              </template>
              <template v-else>
                <v-btn icon variant="tonal" size="small" color="primary" title="Upload Front"
                  @click="uploadExisting('front', g.documentGroupId)">
                  <v-icon size="18">mdi-cloud-upload</v-icon>
                </v-btn>
              </template>
            </td>
            <td class="text-center col-side">
              <template v-if="g.back && !g.back.isDeleted && g.back.fileSize != null">
                <div class="d-flex align-center justify-center ga-0 flex-nowrap">
                  <v-chip color="success" variant="tonal" size="x-small" label class="flex-shrink-0 font-weight-medium px-1">
                    <v-icon start size="11">mdi-check-circle</v-icon> Uploaded
                  </v-chip>
                  <v-btn icon variant="text" size="small" title="Re-upload"
                    @click="uploadExisting('back', g.documentGroupId)">
                    <v-icon size="18" color="primary">mdi-cloud-upload-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Edit image"
                    @click="emit('editImage', g.back.id)">
                    <v-icon size="18" color="info">mdi-image-edit-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Preview"
                    @click="emit('previewImage', g.back.id, g.documentType?.name, g.documentNumber)">
                    <v-icon size="18" color="accent">mdi-eye-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Download"
                    @click="downloadDoc(g.back.id, `back_${g.documentNumber || g.documentGroupId}.jpg`)">
                    <v-icon size="18" color="grey">mdi-download-outline</v-icon>
                  </v-btn>
                  <v-btn icon variant="text" size="small" title="Delete"
                    @click="requestDelete(g.back.id, 'Back')">
                    <v-icon size="18" color="error">mdi-trash-can-outline</v-icon>
                  </v-btn>
                </div>
              </template>
              <template v-else>
                <v-btn icon variant="tonal" size="small" color="primary" title="Upload Back"
                  @click="uploadExisting('back', g.documentGroupId)">
                  <v-icon size="18">mdi-cloud-upload</v-icon>
                </v-btn>
              </template>
            </td>
            <td class="text-center col-meta">
              <v-btn icon variant="text" size="small" title="Metadata"
                @click="metadataDocId = getDocId(g)">
                <v-icon size="18" color="grey-lighten-1">mdi-information-outline</v-icon>
              </v-btn>
            </td>
          </tr>

          <tr v-for="nr in newRows" :key="nr._key" class="new-row">
            <td class="col-check"></td>
            <td class="col-type">
              <v-autocomplete v-model="nr.documentTypeId"
                @update:model-value="handleTypeChange($event, undefined, nr)"
                :items="autoCompleteItems" item-title="name" item-value="id"
                variant="outlined" density="compact" hide-details
                color="primary" placeholder="Select type" />
            </td>
            <td class="col-number">
              <div class="d-flex flex-column">
                <v-text-field v-model="nr.documentNumber" variant="outlined"
                  density="compact" hide-details placeholder="Document number"
                  :error="!!nr.dupWarning" color="primary"
                  @input="onDocNumberInput(nr)" />
                <div v-if="nr.dupWarning" class="d-flex align-center ga-1 mt-1">
                  <v-icon color="warning" size="12">mdi-alert</v-icon>
                  <span class="text-caption text-warning">{{ nr.dupWarning }}</span>
                </div>
              </div>
            </td>
            <td class="col-date">
              <v-text-field v-model="nr.expiryDate" type="date" variant="outlined"
                density="compact" hide-details color="primary" />
            </td>
            <td class="text-center col-side">
              <v-btn icon variant="tonal" size="small" color="primary" title="Upload Front"
                @click="uploadForRow(nr, 'front')"
                :disabled="!nr.documentTypeId || !nr.documentNumber">
                <v-icon size="18">mdi-cloud-upload</v-icon>
              </v-btn>
            </td>
            <td class="text-center col-side">
              <v-btn icon variant="tonal" size="small" color="primary" title="Upload Back"
                @click="uploadForRow(nr, 'back')"
                :disabled="!nr.documentTypeId || !nr.documentNumber">
                <v-icon size="18">mdi-cloud-upload</v-icon>
              </v-btn>
            </td>
            <td class="text-center col-meta">
              <v-btn icon variant="text" color="grey" size="small" title="Cancel"
                @click="cancelRow(nr)">
                <v-icon size="18">mdi-close</v-icon>
              </v-btn>
            </td>
          </tr>

          <tr v-if="groups.length === 0 && newRows.length === 0">
            <td colspan="7">
              <div class="d-flex flex-column align-center pa-10">
                <v-icon size="44" class="mb-3 text-grey-lighten-2">mdi-file-document-outline</v-icon>
                <div class="text-body-2 text-grey font-weight-medium">No documents yet</div>
                <div class="text-caption text-grey mt-1">
                  {{ props.clientId ? 'Click "Add Row" below to get started' : 'Select a client from the sidebar' }}
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </v-table>

      <v-divider />

      <div class="d-flex align-center px-5 py-3">
                <div class="d-flex align-center ga-2">
          <v-btn color="primary" variant="tonal" size="default" class="px-4 font-weight-medium"
            prepend-icon="mdi-plus" @click.stop="addRow">
            Add Row
          </v-btn>
          <v-btn color="error" variant="tonal" size="default" class="px-4 font-weight-medium"
            prepend-icon="mdi-delete-outline"
            :disabled="selectedCount === 0" @click="batchDeleteDialog = true">
            Delete
          </v-btn>
          <v-btn color="primary" variant="tonal" size="default" class="px-4 font-weight-medium"
            prepend-icon="mdi-file-pdf-box"
            :disabled="selectedCount === 0" :loading="pdfGenerating"
            @click="pdfConfigDialog = true">
            Generate PDF
          </v-btn>
        </div>
      </div>
    </v-card>

    <MetadataDialog v-if="metadataDocId" :document-id="metadataDocId"
      @close="metadataDocId = null" />

    <v-dialog v-model="singleDeleteDialog" max-width="380">
      <v-card>
        <v-card-text class="pa-6 text-center">
          <v-avatar color="error" variant="tonal" size="48" class="mb-3">
            <v-icon color="error" size="28">mdi-alert-circle</v-icon>
          </v-avatar>
          <div class="text-body-1 font-weight-medium mb-1">Delete {{ deleteSide }} Document</div>
          <div class="text-body-2 text-grey">This will permanently delete this image. This cannot be undone.</div>
        </v-card-text>
        <v-card-actions class="pa-4 pt-0 justify-center ga-2">
          <v-btn color="error" @click="executeDelete" :loading="deleting"
            prepend-icon="mdi-delete" variant="tonal">Delete</v-btn>
          <v-btn variant="text" @click="singleDeleteDialog = false">Cancel</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="batchDeleteDialog" max-width="380">
      <v-card>
        <v-card-text class="pa-6 text-center">
          <v-avatar color="error" variant="tonal" size="48" class="mb-3">
            <v-icon color="error" size="28">mdi-alert-circle</v-icon>
          </v-avatar>
          <div class="text-body-1 font-weight-medium mb-1">Delete Documents</div>
          <div class="text-body-2 text-grey">
            Delete {{ selectedCount }} document row(s) permanently? This cannot be undone.
          </div>
        </v-card-text>
        <v-card-actions class="pa-4 pt-0 justify-center ga-2">
          <v-btn color="error" @click="executeBatchDelete" :loading="deleting"
            prepend-icon="mdi-delete" variant="tonal">Delete</v-btn>
          <v-btn variant="text" @click="batchDeleteDialog = false">Cancel</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="typeAddDialog" max-width="400">
      <v-card class="pa-2">
        <v-card-title class="pa-4 pb-2">Add Document Type</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-text-field v-model="newTypeName" label="Document Type Name"
            autofocus @keyup.enter="handleAddType" />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-btn color="primary" :loading="addingType" @click="handleAddType"
            prepend-icon="mdi-check" variant="tonal">Create</v-btn>
          <v-btn variant="text" @click="typeAddDialog = false">Cancel</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ImageEditor v-if="editingUploadFile" :image-src="editingUploadFile"
      @close="editingUploadFile = null; editingUploadCb = null"
      @save="onUploadSave" />

    <PdfConfigDialog v-if="pdfConfigDialog" @close="pdfConfigDialog = false"
      @generate="handleGeneratePdf" />

    <PdfPreviewDialog v-if="pdfBlob" :pdf-blob="pdfBlob"
      @close="pdfBlob = null" />

    <v-snackbar v-model="errorSnackbar" color="error" variant="tonal" location="top" :timeout="4000">
      {{ errorMsg }}
    </v-snackbar>
  </div>
</template>

<style scoped>
.premium-table {
  border: none;
  font-size: 13px;
}

.premium-table thead th {
  background: rgb(248, 250, 252);
  border-bottom: 1px solid rgb(226, 232, 240) !important;
  padding: 10px 14px !important;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-size: 10.5px;
  color: rgb(100, 116, 139);
  font-weight: 600;
  vertical-align: middle;
}

.premium-table tbody td {
  padding: 8px 14px !important;
  border-bottom: 1px solid rgb(241, 245, 249);
  vertical-align: middle;
}

.premium-table tbody tr:last-child td {
  border-bottom: none;
}

.premium-row {
  transition: background 0.12s ease;
}

.premium-row:hover {
  background: rgba(30, 58, 95, 0.025);
}

.premium-row.row-selected {
  background: rgba(30, 58, 95, 0.05);
}

.new-row td {
  background: rgba(30, 58, 95, 0.03);
  border-bottom: 1px solid rgba(30, 58, 95, 0.08) !important;
}

.col-check { width: 36px; min-width: 36px; }
.col-type { width: 170px; min-width: 160px; }
.col-number { width: 170px; min-width: 150px; }
.col-date { width: 130px; min-width: 120px; }
.col-side { width: 220px; min-width: 200px; }
.col-meta { width: 44px; min-width: 44px; }

.number-masked {
  font-family: 'Inter', monospace;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

:deep(.cell-input) {
  font-size: 13px;
}

:deep(.cell-input .v-field) {
  min-height: 28px;
  box-shadow: none;
}

:deep(.cell-input .v-field__input) {
  padding: 1px 4px;
  min-height: 24px;
  font-size: 13px;
}

:deep(.cell-input .v-field__overlay) {
  background: transparent;
}

:deep(.cell-input:hover .v-field__overlay) {
  background: rgba(30, 58, 95, 0.035);
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
</style>
