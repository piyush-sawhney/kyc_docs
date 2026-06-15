<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Cropper } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'

const props = defineProps<{
  imageSrc: string | File
  filename?: string
}>()

const emit = defineEmits<{
  close: []
  save: [blob: Blob, filename: string]
}>()

const cropperRef = ref<InstanceType<typeof Cropper> | null>(null)

const imageUrl = ref('')
const loading = ref(true)
const saving = ref(false)
const activeTool = ref<'crop' | 'rotate' | 'adjust'>('crop')
const showConfirmClose = ref(false)
const dirty = ref(false)
const isReady = ref(false)

const brightness = ref(100)
const contrast = ref(100)
const sharpness = ref(0)
const rotation = ref(0)
const flipH = ref(false)
const flipV = ref(false)

const zoomLevel = ref(1)

const MIN_ZOOM = 0.1
const MAX_ZOOM = 5
const ZOOM_FACTOR = 1.25

const isZooming = ref(false)
const currentCoords = ref<{ width: number; height: number; left: number; top: number } | null>(null)

const filterStyle = computed(() => {
  if (activeTool.value !== 'adjust') return {}
  const b = (brightness.value / 100).toFixed(2)
  const c = (contrast.value / 100).toFixed(2)
  return { filter: `brightness(${b}) contrast(${c})` }
})

onMounted(() => {
  if (props.imageSrc instanceof File) {
    imageUrl.value = URL.createObjectURL(props.imageSrc)
  } else {
    imageUrl.value = props.imageSrc
  }
})

function onReady() {
  loading.value = false
  isReady.value = true
}

function onError() {
  loading.value = false
}

function onChange({ coordinates }: { coordinates: { width: number; height: number; left: number; top: number } }) {
  currentCoords.value = coordinates
  if (!isZooming.value && isReady.value) {
    dirty.value = true
  }
}

function setTool(tool: 'crop' | 'rotate' | 'adjust') {
  activeTool.value = tool
}

function handleRotateCW() {
  rotation.value = (rotation.value + 90) % 360
  cropperRef.value?.rotate(90)
  dirty.value = true
}

function handleRotateCCW() {
  rotation.value = (rotation.value - 90 + 360) % 360
  cropperRef.value?.rotate(-90)
  dirty.value = true
}

function setRotationFromInput(val: string) {
  const n = parseInt(val, 10)
  if (!isNaN(n) && n >= 0 && n <= 360) {
    const diff = n - rotation.value
    rotation.value = n
    cropperRef.value?.rotate(diff)
    dirty.value = true
  }
}

function toggleFlipH() {
  flipH.value = !flipH.value
  cropperRef.value?.flip(flipH.value, flipV.value)
  dirty.value = true
}

function toggleFlipV() {
  flipV.value = !flipV.value
  cropperRef.value?.flip(flipH.value, flipV.value)
  dirty.value = true
}

function handleBrightness(val: number) {
  brightness.value = val
  dirty.value = true
}

function handleContrast(val: number) {
  contrast.value = val
  dirty.value = true
}

function handleSharpness(val: number) {
  sharpness.value = val
  dirty.value = true
}

function applyUnsharpMask(data: Uint8ClampedArray, w: number, h: number, strength: number) {
  if (strength === 0) return
  const weight = strength / 200
  const output = new Uint8ClampedArray(data)
  for (let y = 1; y < h - 1; y++) {
    for (let x = 1; x < w - 1; x++) {
      const idx = (y * w + x) * 4
      for (let c = 0; c < 3; c++) {
        let sum = 0
        for (let ky = -1; ky <= 1; ky++) {
          for (let kx = -1; kx <= 1; kx++) {
            const nIdx = ((y + ky) * w + (x + kx)) * 4 + c
            const kernel = (ky === 0 && kx === 0) ? (1 + 8 * weight) : (-weight)
            sum += output[nIdx] * kernel
          }
        }
        data[idx + c] = Math.max(0, Math.min(255, Math.round(sum)))
      }
    }
  }
}

function applyBrightnessContrast(data: Uint8ClampedArray, bFactor: number, cFactor: number) {
  for (let i = 0; i < data.length; i += 4) {
    for (let c = 0; c < 3; c++) {
      let p = data[i + c] / 255
      p = (p - 0.5) * cFactor + 0.5
      p = p * bFactor
      data[i + c] = Math.max(0, Math.min(255, Math.round(p * 255)))
    }
  }
}

async function getProcessedBlob(): Promise<Blob> {
  if (!cropperRef.value) throw new Error('Cropper not initialized')
  const result = cropperRef.value.getResult()
  if (!result?.canvas) throw new Error('No canvas result')

  const canvas = result.canvas
  const ctx = canvas.getContext('2d')!

  const bFactor = brightness.value / 100
  const cFactor = contrast.value / 100

  if (bFactor !== 1 || cFactor !== 1 || sharpness.value > 0) {
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    applyBrightnessContrast(imageData.data, bFactor, cFactor)
    applyUnsharpMask(imageData.data, canvas.width, canvas.height, sharpness.value)
    ctx.putImageData(imageData, 0, 0)
  }

  return new Promise((resolve) => {
    canvas.toBlob((blob) => {
      resolve(blob || new Blob())
    }, 'image/jpeg', 0.88)
  })
}

async function handleSave() {
  if (saving.value) return
  saving.value = true
  try {
    const blob = await getProcessedBlob()
    emit('save', blob, props.filename || 'document.jpg')
  } catch (err) {
    console.error('Failed to process image', err)
  }
  saving.value = false
}

function handleClose() {
  if (dirty.value) {
    showConfirmClose.value = true
  } else {
    cleanup()
    emit('close')
  }
}

function confirmClose() {
  showConfirmClose.value = false
  cleanup()
  emit('close')
}

function cleanup() {
  if (imageUrl.value && props.imageSrc instanceof File) {
    URL.revokeObjectURL(imageUrl.value)
  }
}

function resetAll() {
  brightness.value = 100
  contrast.value = 100
  sharpness.value = 0
  rotation.value = 0
  flipH.value = false
  flipV.value = false
  zoomLevel.value = 1
  dirty.value = false
  activeTool.value = 'crop'
  isZooming.value = true
  cropperRef.value?.reset()
  isZooming.value = false
}

function zoomIn() {
  if (!cropperRef.value || !currentCoords.value || zoomLevel.value >= MAX_ZOOM) return

  isZooming.value = true
  const coords = currentCoords.value
  cropperRef.value.zoom(ZOOM_FACTOR)
  cropperRef.value.setCoordinates(coords, { autoZoom: false })
  zoomLevel.value = Math.min(MAX_ZOOM, zoomLevel.value * ZOOM_FACTOR)
  isZooming.value = false
}

function zoomOut() {
  if (!cropperRef.value || !currentCoords.value || zoomLevel.value <= MIN_ZOOM) return

  isZooming.value = true
  const coords = currentCoords.value
  cropperRef.value.zoom(1 / ZOOM_FACTOR)
  cropperRef.value.setCoordinates(coords, { autoZoom: false })
  zoomLevel.value = Math.max(MIN_ZOOM, zoomLevel.value / ZOOM_FACTOR)
  isZooming.value = false
}

function resetZoom() {
  if (!cropperRef.value) return
  isZooming.value = true
  cropperRef.value.reset()
  zoomLevel.value = 1
  isZooming.value = false
}

function handleWheel(e: WheelEvent) {
  e.preventDefault()
  if (e.deltaY > 0) {
    zoomOut()
  } else {
    zoomIn()
  }
}
</script>

<template>
  <v-dialog :model-value="true" fullscreen transition="dialog-bottom-transition">
    <v-card class="editor-dialog">
      <v-toolbar color="transparent" density="compact" class="editor-toolbar">
        <v-btn icon="mdi-close" variant="text" @click="handleClose" />
        <v-toolbar-title class="text-body-1 font-weight-medium">Image Editor</v-toolbar-title>
        <v-spacer />
        <v-btn color="primary" :loading="saving" :disabled="loading"
          prepend-icon="mdi-content-save" variant="tonal" class="font-weight-medium px-4"
          @click="handleSave">
          Save
        </v-btn>
      </v-toolbar>

      <div class="editor-body">
        <div class="tool-sidebar">
          <div class="tool-group">
            <v-btn :color="activeTool === 'crop' ? 'primary' : 'grey-darken-1'"
              variant="text" size="small" class="tool-btn" @click="setTool('crop')"
              :disabled="loading" stacked>
              <v-icon size="22">mdi-crop</v-icon>
              <span class="tool-label">Crop</span>
            </v-btn>
            <v-btn :color="activeTool === 'rotate' ? 'primary' : 'grey-darken-1'"
              variant="text" size="small" class="tool-btn" @click="setTool('rotate')"
              :disabled="loading" stacked>
              <v-icon size="22">mdi-rotate-right</v-icon>
              <span class="tool-label">Rotate</span>
            </v-btn>
            <v-btn :color="activeTool === 'adjust' ? 'primary' : 'grey-darken-1'"
              variant="text" size="small" class="tool-btn" @click="setTool('adjust')"
              :disabled="loading" stacked>
              <v-icon size="22">mdi-tune</v-icon>
              <span class="tool-label">Adjust</span>
            </v-btn>
          </div>
          <v-divider class="mx-2 my-2" />
          <v-btn color="grey-darken-1" variant="text" size="small" class="tool-btn"
            @click="resetAll" :disabled="loading" stacked>
            <v-icon size="22">mdi-restore</v-icon>
            <span class="tool-label">Reset</span>
          </v-btn>
        </div>

        <div class="canvas-area">
          <v-progress-circular v-if="loading" indeterminate size="36" width="3" color="primary" />

          <template v-if="imageUrl">
            <div class="cropper-wrapper" :style="filterStyle" @wheel="handleWheel">
              <Cropper
                ref="cropperRef"
                :src="imageUrl"
                :stencil-props="{ aspectRatio: null }"
                :resize-image="{ wheel: false }"
                :check-orientation="true"
                :transitions="true"
                class="cropper-component"
                @ready="onReady"
                @error="onError"
                @change="onChange"
              />
            </div>
          </template>

          <div v-if="!loading" class="zoom-pill">
            <v-btn icon variant="tonal" size="x-small" color="grey" @click="zoomOut"
              :disabled="zoomLevel <= MIN_ZOOM" title="Zoom out">
              <v-icon size="14">mdi-minus</v-icon>
            </v-btn>
            <span class="zoom-text">{{ Math.round(zoomLevel * 100) }}%</span>
            <v-btn icon variant="tonal" size="x-small" color="grey" @click="zoomIn"
              :disabled="zoomLevel >= MAX_ZOOM" title="Zoom in">
              <v-icon size="14">mdi-plus</v-icon>
            </v-btn>
            <v-btn icon variant="text" size="x-small" color="grey" @click="resetZoom" title="Fit to screen">
              <v-icon size="13">mdi-fit-to-screen-outline</v-icon>
            </v-btn>
          </div>
        </div>

        <div class="right-sidebar">
          <template v-if="activeTool === 'crop'">
            <div class="sidebar-section">
              <div class="sidebar-section-title">Crop</div>
              <div class="text-caption text-grey mt-2 text-center">Drag to select crop area</div>
            </div>
          </template>

          <template v-if="activeTool === 'rotate'">
            <div class="sidebar-section">
              <div class="sidebar-section-title">Rotation</div>
              <div class="d-flex ga-2 justify-center mt-2">
                <v-btn icon variant="tonal" size="small" color="primary" @click="handleRotateCCW" title="Rotate 90° CCW">
                  <v-icon>mdi-rotate-left</v-icon>
                </v-btn>
                <v-btn icon variant="tonal" size="small" color="primary" @click="handleRotateCW" title="Rotate 90° CW">
                  <v-icon>mdi-rotate-right</v-icon>
                </v-btn>
              </div>
              <div class="d-flex align-center ga-2 mt-2">
                <v-text-field v-model.number="rotation" type="number" min="0" max="360" hide-details
                  density="compact" variant="outlined" class="angle-input"
                  @update:model-value="(v: any) => { const n = Number(v); rotation = ((n % 360) + 360) % 360; dirty = true }"
                  @blur="setRotationFromInput" />
                <span class="text-caption text-grey">deg</span>
              </div>
            </div>
            <v-divider class="mx-3" />
            <div class="sidebar-section">
              <div class="sidebar-section-title">Flip</div>
              <div class="d-flex ga-2 justify-center mt-2">
                <v-btn icon variant="tonal" size="small" color="secondary" @click="toggleFlipH"
                  :class="{ 'v-btn--active': flipH }" title="Flip horizontal">
                  <v-icon>mdi-flip-horizontal</v-icon>
                </v-btn>
                <v-btn icon variant="tonal" size="small" color="secondary" @click="toggleFlipV"
                  :class="{ 'v-btn--active': flipV }" title="Flip vertical">
                  <v-icon>mdi-flip-vertical</v-icon>
                </v-btn>
              </div>
            </div>
          </template>

          <template v-if="activeTool === 'adjust'">
            <div class="sidebar-section">
              <div class="sidebar-section-title">Brightness</div>
              <div class="d-flex align-center ga-2 mt-1">
                <v-slider v-model="brightness" :min="20" :max="200" :step="1"
                  density="compact" hide-details class="sidebar-slider"
                  @update:model-value="handleBrightness" />
                <v-text-field v-model.number="brightness" type="number" min="20" max="200" hide-details
                  density="compact" variant="outlined" class="sidebar-input"
                  @update:model-value="(v: any) => handleBrightness(Math.max(20, Math.min(200, Number(v))))" />
                <span class="text-caption text-grey" style="min-width:10px">%</span>
              </div>
            </div>
            <v-divider class="mx-3" />
            <div class="sidebar-section">
              <div class="sidebar-section-title">Contrast</div>
              <div class="d-flex align-center ga-2 mt-1">
                <v-slider v-model="contrast" :min="20" :max="200" :step="1"
                  density="compact" hide-details class="sidebar-slider"
                  @update:model-value="handleContrast" />
                <v-text-field v-model.number="contrast" type="number" min="20" max="200" hide-details
                  density="compact" variant="outlined" class="sidebar-input"
                  @update:model-value="(v: any) => handleContrast(Math.max(20, Math.min(200, Number(v))))" />
                <span class="text-caption text-grey" style="min-width:10px">%</span>
              </div>
            </div>
            <v-divider class="mx-3" />
            <div class="sidebar-section">
              <div class="sidebar-section-title">Sharpness</div>
              <div class="d-flex align-center ga-2 mt-1">
                <v-slider v-model="sharpness" :min="0" :max="100" :step="1"
                  density="compact" hide-details class="sidebar-slider"
                  @update:model-value="handleSharpness" />
                <v-text-field v-model.number="sharpness" type="number" min="0" max="100" hide-details
                  density="compact" variant="outlined" class="sidebar-input"
                  @update:model-value="(v: any) => handleSharpness(Math.max(0, Math.min(100, Number(v))))" />
              </div>
            </div>
          </template>
        </div>
      </div>

      <v-dialog v-model="showConfirmClose" max-width="360">
        <v-card>
          <v-card-text class="pa-6 text-center">
            <v-avatar color="warning" variant="tonal" size="48" class="mb-3">
              <v-icon color="warning" size="28">mdi-alert-circle</v-icon>
            </v-avatar>
            <div class="text-body-1 font-weight-medium mb-1">Discard changes?</div>
            <div class="text-body-2 text-grey">You have unsaved edits. Discard them?</div>
          </v-card-text>
          <v-card-actions class="pa-4 pt-0 justify-center ga-2">
            <v-btn color="error" variant="tonal" prepend-icon="mdi-close" @click="confirmClose">
              Discard
            </v-btn>
            <v-btn variant="text" @click="showConfirmClose = false">Keep editing</v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.editor-dialog {
  display: flex;
  flex-direction: column;
  background: rgb(248, 250, 252);
  height: 100%;
}

.editor-toolbar {
  border-bottom: 1px solid rgb(226, 232, 240);
  flex-shrink: 0;
  padding: 0 8px;
  z-index: 10;
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.tool-sidebar {
  width: 64px;
  flex-shrink: 0;
  background: white;
  border-right: 1px solid rgb(226, 232, 240);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  gap: 4px;
}

.tool-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.tool-btn {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  min-width: auto;
}

.tool-btn:hover {
  background: rgba(30, 58, 95, 0.06);
}

.tool-label {
  font-size: 9px;
  line-height: 1;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  margin-top: 2px;
}

.canvas-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(241, 245, 249);
  position: relative;
  overflow: hidden;
  min-height: 300px;
}

.cropper-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cropper-component {
  width: 100%;
  height: 100%;
}

.zoom-pill {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  border-radius: 100px;
  padding: 4px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgb(226, 232, 240);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 5;
}

.zoom-text {
  font-size: 12px;
  font-weight: 600;
  min-width: 36px;
  text-align: center;
  color: rgb(71, 85, 105);
  font-variant-numeric: tabular-nums;
}

.right-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: white;
  border-left: 1px solid rgb(226, 232, 240);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 0;
}

.sidebar-section {
  padding: 16px;
}

.sidebar-section-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(100, 116, 139);
  text-align: center;
}

.sidebar-slider {
  flex: 1;
  min-width: 0;
}

.sidebar-input {
  width: 56px;
  flex-shrink: 0;
}

:deep(.sidebar-input .v-field) {
  min-height: 28px;
  font-size: 12px;
}

:deep(.sidebar-input .v-field__input) {
  padding: 2px 4px;
  text-align: center;
}

:deep(.sidebar-input input) {
  text-align: center;
}

.angle-input {
  flex: 1;
}

:deep(.angle-input .v-field) {
  min-height: 28px;
  font-size: 13px;
}

:deep(.angle-input .v-field__input) {
  padding: 2px 4px;
  text-align: center;
}

:deep(.angle-input input) {
  text-align: center;
}

:deep(.v-slider-track__background) {
  opacity: 0.3;
}
</style>
