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
  <v-dialog :model-value="true" max-width="420" @click:outside="emit('close')">
    <v-card>
      <v-card-title class="pa-4 d-flex align-center">
        <v-icon start color="primary">mdi-information-outline</v-icon>
        Document Metadata
        <v-spacer />
        <v-btn icon size="small" variant="text" @click="emit('close')">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-card-text v-if="loading" class="d-flex justify-center pa-6">
        <v-progress-circular indeterminate size="24" />
      </v-card-text>
      <v-card-text v-else-if="metadata" class="pa-4 pt-0">
        <v-list lines="two" density="compact">
          <v-list-item title="Created At" :subtitle="formatDate(metadata.createdAt)" prepend-icon="mdi-calendar-plus" />
          <v-list-item title="Last Updated At" :subtitle="formatDate(metadata.updatedAt)" prepend-icon="mdi-calendar-edit" />
          <v-divider class="my-2" />
          <v-list-item title="Created By" :subtitle="metadata.createdBy?.fullName || '-'" prepend-icon="mdi-account-plus"
            :subtitle-html="metadata.createdBy ? `${metadata.createdBy.fullName}<br><small>${metadata.createdBy.email}</small>` : '-'" />
          <v-list-item title="Updated By" :subtitle="metadata.updatedBy?.fullName || '-'" prepend-icon="mdi-account-edit"
            :subtitle-html="metadata.updatedBy ? `${metadata.updatedBy.fullName}<br><small>${metadata.updatedBy.email}</small>` : '-'" />
        </v-list>
      </v-card-text>
      <v-card-text v-else class="pa-6 text-center text-grey">
        Metadata not available
      </v-card-text>
    </v-card>
  </v-dialog>
</template>
