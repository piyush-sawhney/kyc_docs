<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  pdfBlob: Blob
}>()

const emit = defineEmits<{
  close: []
}>()

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
  <v-dialog :model-value="true" fullscreen transition="dialog-bottom-transition">
    <v-card class="preview-card">
      <v-toolbar color="transparent" density="compact">
        <v-btn icon="mdi-close" variant="text" @click="emit('close')" />
        <v-toolbar-title class="text-body-1 font-weight-medium">PDF Preview</v-toolbar-title>
        <v-spacer />
        <v-btn color="primary" variant="tonal" prepend-icon="mdi-download"
          class="font-weight-medium px-4" @click="handleDownload">
          Download
        </v-btn>
      </v-toolbar>

      <div v-if="pdfUrl" class="preview-body">
        <iframe :src="pdfUrl" class="preview-iframe" title="PDF Preview" />
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.preview-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: rgb(248, 250, 252);
}

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
