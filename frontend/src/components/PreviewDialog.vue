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
  <v-dialog :model-value="true" fullscreen transition="dialog-bottom-transition">
    <v-card class="preview-dialog">
      <v-toolbar color="transparent" density="compact" class="preview-toolbar">
        <v-btn icon="mdi-close" variant="text" @click="emit('close')" />
        <v-toolbar-title class="text-body-1 font-weight-medium">
          {{ docId ? 'Image Preview' : 'Document Preview' }}
        </v-toolbar-title>
        <v-chip v-if="docId && docTypeName" color="primary" variant="tonal" size="small" label class="font-weight-medium">
          {{ docTypeName }}
        </v-chip>
        <v-chip v-else-if="group?.documentType" color="primary" variant="tonal" size="small" label class="font-weight-medium">
          {{ group.documentType.name }}
        </v-chip>
        <v-spacer />
      </v-toolbar>

      <v-card-text v-if="loading" class="d-flex align-center justify-center" style="flex:1;">
        <v-progress-circular indeterminate size="32" width="3" color="primary" />
      </v-card-text>

      <template v-else-if="docId">
        <div class="preview-info" v-if="docNumber || docTypeName">
          <div class="d-flex align-center ga-4">
            <span v-if="docNumber" class="text-body-2 text-grey-darken-1">
              <v-icon start size="14" color="grey">mdi-card-text</v-icon>
              {{ docNumber }}
            </span>
            <span v-if="docTypeName" class="text-body-2 text-grey-darken-1">
              <v-icon start size="14" color="grey">mdi-file-document</v-icon>
              {{ docTypeName }}
            </span>
          </div>
        </div>

        <div class="image-viewport" @wheel.prevent="handleWheel"
          @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp"
          :class="{ panning: isPanning }">
          <img v-if="imageUrl" :src="imageUrl"
            :style="{ transform: `translate(${panX}px, ${panY}px) scale(${scale})`, cursor: scale > 1 ? 'grab' : 'default' }"
            class="preview-image" draggable="false" />
          <div v-else class="text-grey">Image could not be loaded</div>
        </div>
      </template>

      <template v-else-if="group">
        <div class="preview-info">
          <div class="d-flex align-center ga-4">
            <span class="text-body-2 text-grey-darken-1">
              <v-icon start size="14" color="grey">mdi-card-text</v-icon>
              {{ group.documentNumber }}
            </span>
            <span class="text-body-2 text-grey-darken-1">
              <v-icon start size="14" color="grey">mdi-file-document</v-icon>
              {{ group.documentType?.name }}
            </span>
          </div>
        </div>

        <v-tabs v-model="activeTab" color="primary" class="px-4 preview-tabs" density="compact">
          <v-tab value="front" :disabled="!frontUrl" class="text-caption font-weight-medium">
            <v-icon start size="14">mdi-file-image</v-icon> Front Side
          </v-tab>
          <v-tab value="back" :disabled="!backUrl" class="text-caption font-weight-medium">
            <v-icon start size="14">mdi-file-image</v-icon> Back Side
          </v-tab>
        </v-tabs>

        <v-window v-model="activeTab" class="flex-grow-1 d-flex flex-column">
          <v-window-item value="front" class="flex-grow-1 d-flex flex-column">
            <div class="image-viewport" @wheel.prevent="handleWheel"
              @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp"
              :class="{ panning: isPanning }">
              <img v-if="frontUrl" :src="frontUrl"
                :style="{ transform: `translate(${panX}px, ${panY}px) scale(${scale})`, cursor: scale > 1 ? 'grab' : 'default' }"
                class="preview-image" draggable="false" />
              <div v-else class="text-grey">No front image available</div>
            </div>
          </v-window-item>
          <v-window-item value="back" class="flex-grow-1 d-flex flex-column">
            <div class="image-viewport" @wheel.prevent="handleWheel"
              @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp"
              :class="{ panning: isPanning }">
              <img v-if="backUrl" :src="backUrl"
                :style="{ transform: `translate(${panX}px, ${panY}px) scale(${scale})`, cursor: scale > 1 ? 'grab' : 'default' }"
                class="preview-image" draggable="false" />
              <div v-else class="text-grey">No back image available</div>
            </div>
          </v-window-item>
        </v-window>
      </template>

      <div class="zoom-controls">
        <div class="d-flex align-center ga-1">
          <v-btn icon variant="tonal" size="x-small" color="grey" @click="zoomOut" :disabled="scale <= MIN_SCALE" title="Zoom out">
            <v-icon size="16">mdi-minus</v-icon>
          </v-btn>
          <span class="zoom-level text-caption font-weight-medium mx-1">{{ Math.round(scale * 100) }}%</span>
          <v-btn icon variant="tonal" size="x-small" color="grey" @click="zoomIn" :disabled="scale >= MAX_SCALE" title="Zoom in">
            <v-icon size="16">mdi-plus</v-icon>
          </v-btn>
          <v-btn icon variant="text" size="x-small" color="grey" @click="resetView" title="Reset zoom" class="ml-1">
            <v-icon size="14">mdi-fit-to-screen-outline</v-icon>
          </v-btn>
        </div>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.preview-dialog {
  display: flex;
  flex-direction: column;
  background: rgb(248, 250, 252);
  height: 100%;
}

.preview-toolbar {
  border-bottom: 1px solid rgb(226, 232, 240);
  flex-shrink: 0;
  padding: 0 8px;
}

.preview-info {
  padding: 10px 20px;
  background: white;
  border-bottom: 1px solid rgb(241, 245, 249);
  flex-shrink: 0;
}

.preview-tabs {
  flex-shrink: 0;
  background: white;
  border-bottom: 1px solid rgb(241, 245, 249);
}

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
  z-index: 100;
}

.zoom-level {
  min-width: 36px;
  text-align: center;
  color: rgb(71, 85, 105);
  font-variant-numeric: tabular-nums;
}
</style>
