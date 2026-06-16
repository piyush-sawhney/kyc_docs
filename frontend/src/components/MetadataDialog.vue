<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/client'

const props = defineProps<{ documentId: string }>()
const emit = defineEmits<{ close: [] }>()

const metadata = ref<any>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await api.get(`/documents/${props.documentId}/metadata`)
    metadata.value = data
  } catch { /* ignore */ }
  loading.value = false
})

function formatDate(date: string) {
  if (!date) return '-'
  return new Date(date).toLocaleString('en-IN')
}
</script>

<template>
  <div class="modal-backdrop fade show"></div>
  <div class="modal d-block" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 420px;">
      <div class="modal-content border-0 shadow" @click:outside="emit('close')">
        <div class="modal-header border-0 pb-2">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-info-circle" style="color: #1E3A5F;"></i>
            <h6 class="fw-bold mb-0" style="color: #1E293B;">Document Metadata</h6>
          </div>
          <button type="button" class="btn-close" @click="emit('close')"></button>
        </div>

        <div class="modal-body pt-0">
          <div v-if="loading" class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
          </div>
          <div v-else-if="metadata">
            <div class="list-group list-group-flush">
              <div class="list-group-item px-0 d-flex justify-content-between align-items-start">
                <div>
                  <small class="fw-medium d-block">Created At</small>
                  <small class="text-muted">{{ formatDate(metadata.createdAt) }}</small>
                </div>
                <i class="bi bi-calendar-plus text-muted"></i>
              </div>
              <div class="list-group-item px-0 d-flex justify-content-between align-items-start">
                <div>
                  <small class="fw-medium d-block">Last Updated At</small>
                  <small class="text-muted">{{ formatDate(metadata.updatedAt) }}</small>
                </div>
                <i class="bi bi-calendar-edit text-muted"></i>
              </div>
              <hr class="my-2" />
              <div class="list-group-item px-0 d-flex justify-content-between align-items-start">
                <div>
                  <small class="fw-medium d-block">Created By</small>
                  <small class="text-muted">{{ metadata.createdBy?.fullName || '-' }}</small>
                  <small v-if="metadata.createdBy" class="text-muted d-block">{{ metadata.createdBy.email }}</small>
                </div>
                <i class="bi bi-person-plus text-muted"></i>
              </div>
              <div class="list-group-item px-0 d-flex justify-content-between align-items-start">
                <div>
                  <small class="fw-medium d-block">Updated By</small>
                  <small class="text-muted">{{ metadata.updatedBy?.fullName || '-' }}</small>
                  <small v-if="metadata.updatedBy" class="text-muted d-block">{{ metadata.updatedBy.email }}</small>
                </div>
                <i class="bi bi-person-gear text-muted"></i>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-4 text-muted small">
            Metadata not available
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
