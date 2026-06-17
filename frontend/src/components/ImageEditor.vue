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
  if (!currentCoords.value) {
    currentCoords.value = cropperRef.value?.getCoordinates() ?? { width: 0, height: 0, left: 0, top: 0 }
  }
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
  if (!cropperRef.value || !isReady.value || zoomLevel.value >= MAX_ZOOM) return
  isZooming.value = true
  cropperRef.value.zoom(ZOOM_FACTOR)
  zoomLevel.value = Math.min(MAX_ZOOM, zoomLevel.value * ZOOM_FACTOR)
  isZooming.value = false
}

function zoomOut() {
  if (!cropperRef.value || !isReady.value || zoomLevel.value <= MIN_ZOOM) return
  isZooming.value = true
  cropperRef.value.zoom(1 / ZOOM_FACTOR)
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
  <div class="modal-backdrop fade show"></div>
  <div class="modal d-block" tabindex="-1" style="overflow: hidden;">
    <div class="modal-dialog modal-fullscreen p-0 m-0" style="max-width: 100vw;">
      <div class="modal-content border-0" style="height: 100vh; border-radius: 0; background: rgb(248, 250, 252);">
        <div class="d-flex align-items-center px-3 border-bottom bg-white" style="flex-shrink: 0; min-height: 48px; z-index: 10;">
          <button class="btn btn-sm btn-link text-decoration-none text-dark me-2" @click="handleClose">
            <i class="bi bi-x" style="font-size: 20px;"></i>
          </button>
          <small class="fw-medium flex-grow-1">Image Editor</small>
          <button class="btn btn-sm btn-primary" :disabled="loading || saving" @click="handleSave">
            <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
            <i class="bi bi-floppy me-1"></i> Save
          </button>
        </div>

        <div class="editor-body">
          <div class="tool-sidebar">
            <div class="d-flex flex-column align-items-center gap-1">
              <button class="tool-btn" :class="{ 'tool-active': activeTool === 'crop' }"
                @click="setTool('crop')" :disabled="loading">
                <i class="bi bi-crop" style="font-size: 20px;"></i>
                <span class="tool-label">Crop</span>
              </button>
              <button class="tool-btn" :class="{ 'tool-active': activeTool === 'rotate' }"
                @click="setTool('rotate')" :disabled="loading">
                <i class="bi bi-arrow-clockwise" style="font-size: 20px;"></i>
                <span class="tool-label">Rotate</span>
              </button>
              <button class="tool-btn" :class="{ 'tool-active': activeTool === 'adjust' }"
                @click="setTool('adjust')" :disabled="loading">
                <i class="bi bi-sliders" style="font-size: 20px;"></i>
                <span class="tool-label">Adjust</span>
              </button>
            </div>
            <hr class="mx-2 my-2" />
            <button class="tool-btn" @click="resetAll" :disabled="loading">
              <i class="bi bi-arrow-counterclockwise" style="font-size: 20px;"></i>
              <span class="tool-label">Reset</span>
            </button>
          </div>

          <div class="canvas-area">
            <div v-if="loading" class="spinner-border text-primary" role="status"></div>

            <template v-if="imageUrl">
              <div class="cropper-wrapper" :style="filterStyle" @wheel.prevent="handleWheel">
                <Cropper
                  ref="cropperRef"
                  :src="imageUrl"
                  :stencil-props="{ aspectRatio: null }"
                  :resize-image="{ wheel: false }"
                  image-restriction="none"
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
              <button class="btn btn-sm btn-light border rounded-circle p-1 d-flex align-items-center justify-content-center"
                style="width: 26px; height: 26px;" :disabled="zoomLevel <= MIN_ZOOM"
                @click="zoomOut" title="Zoom out">
                <i class="bi bi-dash"></i>
              </button>
              <span class="zoom-text">{{ Math.round(zoomLevel * 100) }}%</span>
              <button class="btn btn-sm btn-light border rounded-circle p-1 d-flex align-items-center justify-content-center"
                style="width: 26px; height: 26px;" :disabled="zoomLevel >= MAX_ZOOM"
                @click="zoomIn" title="Zoom in">
                <i class="bi bi-plus"></i>
              </button>
              <button class="btn btn-sm btn-link text-decoration-none p-0" @click="resetZoom" title="Fit to screen">
                <i class="bi bi-arrows-angle-expand" style="font-size: 12px;"></i>
              </button>
            </div>
          </div>

          <div class="right-sidebar">
            <template v-if="activeTool === 'crop'">
              <div class="sidebar-section">
                <div class="sidebar-section-title">Crop</div>
                <small class="text-muted d-block text-center mt-2">Drag to select crop area</small>
              </div>
            </template>

            <template v-if="activeTool === 'rotate'">
              <div class="sidebar-section">
                <div class="sidebar-section-title">Rotation</div>
                <div class="d-flex gap-2 justify-content-center mt-2">
                  <button class="btn btn-sm btn-outline-primary" @click="handleRotateCCW" title="Rotate 90° CCW">
                    <i class="bi bi-arrow-counterclockwise"></i>
                  </button>
                  <button class="btn btn-sm btn-outline-primary" @click="handleRotateCW" title="Rotate 90° CW">
                    <i class="bi bi-arrow-clockwise"></i>
                  </button>
                </div>
                <div class="d-flex align-items-center gap-1 mt-2">
                  <input type="number" class="form-control form-control-sm text-center" :value="rotation"
                    min="0" max="360"
                    @input="(e) => { const n = Number((e.target as HTMLInputElement).value); rotation = ((n % 360) + 360) % 360; dirty = true }"
                    @change="setRotationFromInput(($event.target as HTMLInputElement).value)" />
                  <small class="text-muted">deg</small>
                </div>
              </div>
              <hr class="mx-3" />
              <div class="sidebar-section">
                <div class="sidebar-section-title">Flip</div>
                <div class="d-flex gap-2 justify-content-center mt-2">
                  <button class="btn btn-sm" :class="flipH ? 'btn-primary' : 'btn-outline-secondary'"
                    @click="toggleFlipH" title="Flip horizontal">
                    <i class="bi bi-arrow-left-right"></i>
                  </button>
                  <button class="btn btn-sm" :class="flipV ? 'btn-primary' : 'btn-outline-secondary'"
                    @click="toggleFlipV" title="Flip vertical">
                    <i class="bi bi-arrow-up-down"></i>
                  </button>
                </div>
              </div>
            </template>

            <template v-if="activeTool === 'adjust'">
              <div class="sidebar-section">
                <div class="sidebar-section-title">Brightness</div>
                <div class="d-flex align-items-center gap-2 mt-1">
                  <input type="range" class="form-range flex-grow-1" :min="20" :max="200" :step="1"
                    :value="brightness" @input="handleBrightness(Number(($event.target as HTMLInputElement).value))" />
                  <input type="number" class="form-control form-control-sm sidebar-input" :value="brightness"
                    min="20" max="200"
                    @input="handleBrightness(Math.max(20, Math.min(200, Number(($event.target as HTMLInputElement).value))))" />
                  <small class="text-muted">%</small>
                </div>
              </div>
              <hr class="mx-3" />
              <div class="sidebar-section">
                <div class="sidebar-section-title">Contrast</div>
                <div class="d-flex align-items-center gap-2 mt-1">
                  <input type="range" class="form-range flex-grow-1" :min="20" :max="200" :step="1"
                    :value="contrast" @input="handleContrast(Number(($event.target as HTMLInputElement).value))" />
                  <input type="number" class="form-control form-control-sm sidebar-input" :value="contrast"
                    min="20" max="200"
                    @input="handleContrast(Math.max(20, Math.min(200, Number(($event.target as HTMLInputElement).value))))" />
                  <small class="text-muted">%</small>
                </div>
              </div>
              <hr class="mx-3" />
              <div class="sidebar-section">
                <div class="sidebar-section-title">Sharpness</div>
                <div class="d-flex align-items-center gap-2 mt-1">
                  <input type="range" class="form-range flex-grow-1" :min="0" :max="100" :step="1"
                    :value="sharpness" @input="handleSharpness(Number(($event.target as HTMLInputElement).value))" />
                  <input type="number" class="form-control form-control-sm sidebar-input" :value="sharpness"
                    min="0" max="100"
                    @input="handleSharpness(Math.max(0, Math.min(100, Number(($event.target as HTMLInputElement).value))))" />
                </div>
              </div>
            </template>
          </div>
        </div>

        <div class="modal-backdrop fade show" v-if="showConfirmClose"></div>
        <div class="modal d-block" tabindex="-1" v-if="showConfirmClose">
          <div class="modal-dialog modal-dialog-centered" style="max-width: 360px;">
            <div class="modal-content border-0 shadow">
              <div class="modal-body text-center p-4">
                <div class="d-inline-flex align-items-center justify-content-center mb-3"
                  style="width: 48px; height: 48px; border-radius: 50%; background: rgba(245,158,11,0.1);">
                  <i class="bi bi-exclamation-circle" style="font-size: 24px; color: #F59E0B;"></i>
                </div>
                <h6 class="fw-semibold mb-1">Discard changes?</h6>
                <p class="small text-muted mb-0">You have unsaved edits. Discard them?</p>
              </div>
              <div class="modal-footer border-0 justify-content-center pt-0 pb-4">
                <button class="btn btn-danger" @click="confirmClose">
                  <i class="bi bi-x me-1"></i> Discard
                </button>
                <button class="btn btn-outline-secondary" @click="showConfirmClose = false">Keep editing</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
.tool-btn {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: pointer;
  transition: background 0.12s;
  color: #475569;
}
.tool-btn:hover { background: rgba(30, 58, 95, 0.06); }
.tool-btn.tool-active { color: #1E3A5F; background: rgba(30, 58, 95, 0.08); }
.tool-btn:disabled { opacity: 0.5; cursor: default; }
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
  background-color: #f1f5f9;
  background-image: repeating-conic-gradient(#e2e8f0 0% 25%, transparent 0% 50%);
  background-size: 16px 16px;
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
.sidebar-section { padding: 16px; }
.sidebar-section-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(100, 116, 139);
  text-align: center;
}
.sidebar-input { width: 56px; flex-shrink: 0; text-align: center; }
.form-range { height: 6px; }
:deep(.vue-advanced-cropper__background) {
  background: transparent;
}
</style>
