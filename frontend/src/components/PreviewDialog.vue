<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/client'

const props = defineProps<{
  groupId?: string
  docId?: string
  docTypeName?: string
  docNumber?: string
}>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(true)
const imageUrl = ref('')
const group = ref<any>(null)
const frontUrl = ref('')
const backUrl = ref('')
const activeTab = ref<'front' | 'back'>('front')

const scale = ref(1)
const panX = ref(0)
const panY = ref(0)
const isPanning = ref(false)
const panStartX = ref(0)
const panStartY = ref(0)

const MIN_SCALE = 0.25
const MAX_SCALE = 5
const ZOOM_STEP = 0.25

function resetView() {
  scale.value = 1
  panX.value = 0
  panY.value = 0
}

function zoomIn() {
  scale.value = Math.min(MAX_SCALE, +(scale.value + ZOOM_STEP).toFixed(2))
}

function zoomOut() {
  const next = +(scale.value - ZOOM_STEP).toFixed(2)
  scale.value = Math.max(MIN_SCALE, next)
}

function handleWheel(e: WheelEvent) {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP
  const next = +(scale.value + delta).toFixed(2)
  scale.value = Math.max(MIN_SCALE, Math.min(MAX_SCALE, next))
}

function onMouseDown(e: MouseEvent) {
  if (scale.value <= 1) return
  isPanning.value = true
  panStartX.value = e.clientX - panX.value
  panStartY.value = e.clientY - panY.value
}

function onMouseMove(e: MouseEvent) {
  if (!isPanning.value) return
  panX.value = e.clientX - panStartX.value
  panY.value = e.clientY - panStartY.value
}

function onMouseUp() {
  isPanning.value = false
}

async function load() {
  if (props.docId) {
    await loadSingle()
  } else if (props.groupId) {
    await loadGroup()
  }
  loading.value = false
}

async function loadSingle() {
  try {
    const res = await api.get(`/documents/${props.docId}/download`, { responseType: 'blob' })
    imageUrl.value = URL.createObjectURL(res.data)
  } catch { /* ignore */ }
}

async function loadGroup() {
  try {
    const { data } = await api.get(`/documents/group/${props.groupId}`)
    group.value = data
    if (data.front && !data.front.isDeleted) {
      const res = await api.get(`/documents/${data.front.id}/download`, { responseType: 'blob' })
      frontUrl.value = URL.createObjectURL(res.data)
    }
    if (data.back && !data.back.isDeleted) {
      const res = await api.get(`/documents/${data.back.id}/download`, { responseType: 'blob' })
      backUrl.value = URL.createObjectURL(res.data)
    }
  } catch { /* ignore */ }
}

onMounted(load)
</script>

<template>
  <div class="modal-backdrop fade show"></div>
  <div class="modal d-block" tabindex="-1" style="overflow: hidden;">
    <div class="modal-dialog modal-fullscreen p-0 m-0" style="max-width: 100vw;">
      <div class="modal-content border-0" style="height: 100vh; border-radius: 0; background: rgb(248, 250, 252);">
        <div class="d-flex align-items-center px-3 border-bottom bg-white" style="flex-shrink: 0; min-height: 48px;">
          <button class="btn btn-sm btn-link text-decoration-none text-dark me-2" @click="emit('close')">
            <i class="bi bi-x" style="font-size: 20px;"></i>
          </button>
          <small class="fw-medium flex-grow-1">
            {{ docId ? 'Image Preview' : 'Document Preview' }}
          </small>
          <span v-if="docId && docTypeName" class="badge bg-soft-primary me-2">{{ docTypeName }}</span>
          <span v-else-if="group?.documentType" class="badge bg-soft-primary">{{ group.documentType.name }}</span>
        </div>

        <div v-if="loading" class="d-flex align-items-center justify-content-center" style="flex: 1;">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <template v-else-if="docId">
          <div v-if="docNumber || docTypeName" class="px-3 py-2 bg-white border-bottom">
            <div class="d-flex align-items-center gap-3">
              <small v-if="docNumber" class="text-muted">
                <i class="bi bi-card-text me-1"></i> {{ docNumber }}
              </small>
              <small v-if="docTypeName" class="text-muted">
                <i class="bi bi-file-earmark me-1"></i> {{ docTypeName }}
              </small>
            </div>
          </div>

          <div class="image-viewport" @wheel.prevent="handleWheel"
            @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp"
            :class="{ panning: isPanning }">
            <img v-if="imageUrl" :src="imageUrl"
              :style="{ transform: `translate(${panX}px, ${panY}px) scale(${scale})`, cursor: scale > 1 ? 'grab' : 'default' }"
              class="preview-image" draggable="false" />
            <div v-else class="text-muted">Image could not be loaded</div>
          </div>
        </template>

        <template v-else-if="group">
          <div class="px-3 py-2 bg-white border-bottom">
            <div class="d-flex align-items-center gap-3">
              <small class="text-muted">
                <i class="bi bi-card-text me-1"></i> {{ group.documentNumber }}
              </small>
              <small class="text-muted">
                <i class="bi bi-file-earmark me-1"></i> {{ group.documentType?.name }}
              </small>
            </div>
          </div>

          <div class="d-flex gap-3 px-3 py-2 bg-white border-bottom" style="flex-shrink: 0;">
            <button class="btn btn-sm" :class="activeTab === 'front' ? 'btn-primary' : 'btn-outline-secondary'"
              :disabled="!frontUrl" @click="activeTab = 'front'">
              <i class="bi bi-file-image me-1"></i> Front Side
            </button>
            <button class="btn btn-sm" :class="activeTab === 'back' ? 'btn-primary' : 'btn-outline-secondary'"
              :disabled="!backUrl" @click="activeTab = 'back'">
              <i class="bi bi-file-image me-1"></i> Back Side
            </button>
          </div>

          <div class="image-viewport" @wheel.prevent="handleWheel"
            @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp"
            :class="{ panning: isPanning }">
            <img v-if="activeTab === 'front' && frontUrl" :src="frontUrl"
              :style="{ transform: `translate(${panX}px, ${panY}px) scale(${scale})`, cursor: scale > 1 ? 'grab' : 'default' }"
              class="preview-image" draggable="false" />
            <img v-else-if="activeTab === 'back' && backUrl" :src="backUrl"
              :style="{ transform: `translate(${panX}px, ${panY}px) scale(${scale})`, cursor: scale > 1 ? 'grab' : 'default' }"
              class="preview-image" draggable="false" />
            <div v-else class="text-muted">No {{ activeTab }} image available</div>
          </div>
        </template>

        <div class="zoom-controls">
          <div class="d-flex align-items-center gap-1">
            <button class="btn btn-sm btn-light border rounded-circle p-1 d-flex align-items-center justify-content-center"
              style="width: 28px; height: 28px;" :disabled="scale <= MIN_SCALE" @click="zoomOut" title="Zoom out">
              <i class="bi bi-dash"></i>
            </button>
            <span class="zoom-level small fw-medium mx-1">{{ Math.round(scale * 100) }}%</span>
            <button class="btn btn-sm btn-light border rounded-circle p-1 d-flex align-items-center justify-content-center"
              style="width: 28px; height: 28px;" :disabled="scale >= MAX_SCALE" @click="zoomIn" title="Zoom in">
              <i class="bi bi-plus"></i>
            </button>
            <button class="btn btn-sm btn-link text-decoration-none ms-1" @click="resetView" title="Reset zoom">
              <i class="bi bi-arrows-angle-expand"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.image-viewport {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: rgb(241, 245, 249);
  position: relative;
  min-height: 300px;
}
.image-viewport.panning {
  cursor: grabbing !important;
}
.preview-image {
  max-width: min(80vw, 900px);
  max-height: 70vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
  transition: transform 0.08s ease;
  will-change: transform;
}
.zoom-controls {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  border-radius: 100px;
  padding: 6px 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
  border: 1px solid rgb(226, 232, 240);
  z-index: 1060;
}
.zoom-level {
  min-width: 36px;
  text-align: center;
  color: rgb(71, 85, 105);
  font-variant-numeric: tabular-nums;
}
</style>
