<script setup lang="ts">
import { ref } from 'vue'
import type { PdfConfig } from '../utils/generatePdf'

const emit = defineEmits<{
  close: []
  generate: [config: PdfConfig]
}>()

const imagesPerPage = ref(9)
const marginTop = ref(15)
const marginBottom = ref(15)
const marginLeft = ref(15)
const marginRight = ref(15)
const orientation = ref<'portrait' | 'landscape'>('portrait')

function handleGenerate() {
  emit('generate', {
    imagesPerPage: imagesPerPage.value,
    marginTop: marginTop.value,
    marginBottom: marginBottom.value,
    marginLeft: marginLeft.value,
    marginRight: marginRight.value,
    orientation: orientation.value,
  })
}
</script>

<template>
  <v-dialog :model-value="true" max-width="480" @click:outside="emit('close')">
    <v-card>
      <v-card-title class="pa-4">
        <div class="text-h6">Generate PDF</div>
        <div class="text-body-2 text-grey">Configure the PDF layout for selected documents</div>
      </v-card-title>

      <v-card-text>
        <v-row dense>
          <v-col cols="6">
            <v-select v-model="imagesPerPage" :items="[1,2,3,4,6,9]" label="Images per page"
              variant="outlined" hide-details density="compact" />
          </v-col>
          <v-col cols="6">
            <v-select v-model="orientation" :items="['portrait','landscape']" label="Orientation"
              variant="outlined" hide-details density="compact" />
          </v-col>
        </v-row>

        <div class="text-body-2 font-weight-medium mt-4 mb-2">Margins (mm)</div>
        <v-row dense>
          <v-col cols="3">
            <v-text-field v-model.number="marginTop" type="number" label="Top" min="5" max="50"
              variant="outlined" hide-details density="compact" />
          </v-col>
          <v-col cols="3">
            <v-text-field v-model.number="marginBottom" type="number" label="Bottom" min="5" max="50"
              variant="outlined" hide-details density="compact" />
          </v-col>
          <v-col cols="3">
            <v-text-field v-model.number="marginLeft" type="number" label="Left" min="5" max="50"
              variant="outlined" hide-details density="compact" />
          </v-col>
          <v-col cols="3">
            <v-text-field v-model.number="marginRight" type="number" label="Right" min="5" max="50"
              variant="outlined" hide-details density="compact" />
          </v-col>
        </v-row>
      </v-card-text>

      <v-card-actions class="pa-4 pt-0">
        <v-btn color="primary" @click="handleGenerate" prepend-icon="mdi-file-pdf-box">
          Generate
        </v-btn>
        <v-btn variant="text" @click="emit('close')">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
