<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{ pdfBlob: Blob }>()
const emit = defineEmits<{ close: [] }>()

const pdfUrl = ref('')

onMounted(() => {
  pdfUrl.value = URL.createObjectURL(props.pdfBlob)
})

onUnmounted(() => {
  if (pdfUrl.value) URL.revokeObjectURL(pdfUrl.value)
})

function handleDownload() {
  const a = document.createElement('a')
  a.href = pdfUrl.value
  a.download = 'documents.pdf'
  a.click()
}
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
          <small class="fw-medium flex-grow-1">PDF Preview</small>
          <button class="btn btn-sm btn-primary" @click="handleDownload">
            <i class="bi bi-download me-1"></i> Download
          </button>
        </div>

        <div v-if="pdfUrl" class="preview-body">
          <iframe :src="pdfUrl" class="preview-iframe" title="PDF Preview" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.preview-body {
  flex: 1;
  min-height: 0;
  display: flex;
}
.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
