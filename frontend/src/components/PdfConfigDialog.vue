<script setup lang="ts">
import { ref } from 'vue'
import type { PdfConfig } from '../utils/generatePdf'

const emit = defineEmits<{ close: []; generate: [config: PdfConfig] }>()

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
  <div class="modal-backdrop fade show"></div>
  <div class="modal d-block" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 480px;">
      <div class="modal-content border-0 shadow" @click:outside="emit('close')">
        <div class="modal-header border-0 pb-0">
          <div>
            <h6 class="fw-bold mb-0" style="color: #1E293B;">Generate PDF</h6>
            <small class="text-muted">Configure the PDF layout for selected documents</small>
          </div>
          <button type="button" class="btn-close" @click="emit('close')"></button>
        </div>

        <div class="modal-body">
          <div class="row g-2">
            <div class="col-6">
              <label class="form-label small text-muted">Images per page</label>
              <select class="form-select" v-model.number="imagesPerPage">
                <option :value="1">1</option>
                <option :value="2">2</option>
                <option :value="3">3</option>
                <option :value="4">4</option>
                <option :value="6">6</option>
                <option :value="9">9</option>
              </select>
            </div>
            <div class="col-6">
              <label class="form-label small text-muted">Orientation</label>
              <select class="form-select" v-model="orientation">
                <option value="portrait">Portrait</option>
                <option value="landscape">Landscape</option>
              </select>
            </div>
          </div>

          <div class="mt-3 mb-1"><small class="fw-medium text-muted">Margins (mm)</small></div>
          <div class="row g-2">
            <div class="col-3">
              <label class="form-label small text-muted">Top</label>
              <input type="number" class="form-control form-control-sm" v-model.number="marginTop" min="5" max="50" />
            </div>
            <div class="col-3">
              <label class="form-label small text-muted">Bottom</label>
              <input type="number" class="form-control form-control-sm" v-model.number="marginBottom" min="5" max="50" />
            </div>
            <div class="col-3">
              <label class="form-label small text-muted">Left</label>
              <input type="number" class="form-control form-control-sm" v-model.number="marginLeft" min="5" max="50" />
            </div>
            <div class="col-3">
              <label class="form-label small text-muted">Right</label>
              <input type="number" class="form-control form-control-sm" v-model.number="marginRight" min="5" max="50" />
            </div>
          </div>
        </div>

        <div class="modal-footer border-0 pt-0">
          <button class="btn btn-primary" @click="handleGenerate">
            <i class="bi bi-filetype-pdf me-1"></i> Generate
          </button>
          <button class="btn btn-outline-secondary" @click="emit('close')">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>
