<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{ clientId: string | null }>()
const auth = useAuthStore()

const entries = ref<any[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const allLoaded = computed(() => entries.value.length >= total.value && total.value > 0)
const containerRef = ref<HTMLElement | null>(null)

const isAdmin = computed(() => auth.user?.role === 'admin')

const activityHeight = ref(240)
const errorMsg = ref('')

async function loadPage(reset = false) {
  if (loading.value) return
  loading.value = true
  if (reset) {
    page.value = 1
    entries.value = []
  }
  try {
    const params: Record<string, any> = {
      page: page.value,
      limit: 20,
    }
    if (props.clientId) {
      params.entityId = props.clientId
    } else if (!isAdmin.value) {
      loading.value = false
      return
    }
    const { data } = await api.get('/audit-logs', { params })
    if (reset) {
      entries.value = data.data
    } else {
      entries.value.push(...data.data)
    }
    total.value = data.total
    errorMsg.value = ''
  } catch (err: any) {
    console.error('Failed to load audit logs', err)
    errorMsg.value = err?.response?.data?.message || 'Failed to load activity'
    if (reset) entries.value = []
  }
  loading.value = false
}

watch(() => props.clientId, () => loadPage(true))

onMounted(() => loadPage(true))

function onScroll() {
  if (!containerRef.value || loading.value || allLoaded.value) return
  const el = containerRef.value
  const threshold = 100
  if (el.scrollHeight - el.scrollTop - el.clientHeight < threshold) {
    page.value++
    loadPage()
  }
}

let resizeStartY = 0
let resizeStartH = 0
function onResizeStart(e: MouseEvent) {
  resizeStartY = e.clientY
  resizeStartH = activityHeight.value
  document.addEventListener('mousemove', onResizeMove)
  document.addEventListener('mouseup', onResizeEnd)
  document.body.style.cursor = 'row-resize'
  document.body.style.userSelect = 'none'
}
function onResizeMove(e: MouseEvent) {
  const delta = resizeStartY - e.clientY
  activityHeight.value = Math.max(120, Math.min(600, resizeStartH - delta))
}
function onResizeEnd() {
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

function actionIcon(action: string) {
  switch (action) {
    case 'CREATE': return { icon: 'mdi-plus-circle', color: 'success' }
    case 'UPDATE': return { icon: 'mdi-pencil-circle', color: 'info' }
    case 'DELETE': return { icon: 'mdi-trash-can', color: 'error' }
    default: return { icon: 'mdi-circle-small', color: 'grey' }
  }
}

function actionLabel(entry: any) {
  const type = entry.entityType === 'client' ? 'client' : 'document'
  switch (entry.action) {
    case 'CREATE': return `Created ${type}`
    case 'UPDATE': return `Updated ${type}`
    case 'DELETE': return `Deleted ${type}`
    default: return `${entry.action} ${type}`
  }
}

function formatTimestamp(ts: string) {
  const d = new Date(ts)
  return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <v-card class="rounded-xl" flat>
    <div class="d-flex align-center px-4 py-3">
      <v-icon color="primary" size="20" class="mr-2">mdi-shield-account</v-icon>
      <span class="text-subtitle-2 font-weight-semibold text-grey-darken-2">Activity</span>
      <v-chip v-if="total" size="x-small" variant="tonal" color="primary" label class="font-weight-medium ml-2">
        {{ total }}
      </v-chip>
      <v-spacer />
      <v-chip v-if="isAdmin && !clientId" size="x-small" variant="tonal" color="info" label class="font-weight-medium">
        All activity
      </v-chip>
    </div>
    <v-divider />

    <div class="resize-handle" @mousedown.prevent="onResizeStart" />

    <div ref="containerRef" class="activity-list" :style="{ maxHeight: activityHeight + 'px' }" @scroll="onScroll">
      <div v-if="errorMsg" class="d-flex flex-column align-center pa-6">
        <v-icon size="36" class="mb-2 text-error">mdi-alert-circle-outline</v-icon>
        <div class="text-caption text-grey">{{ errorMsg }}</div>
      </div>
      <div v-else-if="entries.length === 0 && !loading" class="d-flex flex-column align-center pa-6">
        <v-icon size="36" class="mb-2 text-grey-lighten-2">mdi-shield-off-outline</v-icon>
        <div class="text-caption text-grey">No activity yet</div>
      </div>

      <div v-for="entry in entries" :key="entry.id" class="activity-item">
        <div class="d-flex align-start ga-3">
          <v-icon :color="actionIcon(entry.action).color" size="20" class="flex-shrink-0 mt-1">
            {{ actionIcon(entry.action).icon }}
          </v-icon>
          <div class="flex-grow-1 min-w-0">
            <div class="text-body-2 font-weight-medium text-grey-darken-2">
              {{ actionLabel(entry) }}
            </div>
            <div class="text-caption text-grey mt-0 d-flex align-center ga-2">
              <span>{{ entry.userFullName || 'System' }}</span>
              <span>·</span>
              <span>{{ formatTimestamp(entry.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="d-flex align-center justify-center pa-4">
        <v-progress-circular indeterminate size="20" width="2" color="primary" />
        <span class="text-caption text-grey ml-2">Loading...</span>
      </div>

      <div v-if="allLoaded && entries.length > 0" class="text-center pa-3">
        <span class="text-caption text-grey-lighten-1">All activity loaded</span>
      </div>
    </div>
  </v-card>
</template>

<style scoped>
.resize-handle {
  height: 6px;
  cursor: row-resize;
  background: transparent;
  position: relative;
  transition: background 0.15s ease;
}

.resize-handle::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 32px;
  height: 3px;
  border-radius: 2px;
  background: rgb(203, 213, 225);
  transition: background 0.15s ease, width 0.15s ease;
}

.resize-handle:hover::after,
.resize-handle:active::after {
  background: rgb(100, 116, 139);
  width: 48px;
}

.activity-list {
  overflow-y: auto;
  overflow-x: hidden;
}

.activity-item {
  padding: 8px 16px;
  border-bottom: 1px solid rgb(241, 245, 249);
  transition: background 0.1s ease;
}

.activity-item:hover {
  background: rgba(30, 58, 95, 0.02);
}

.activity-item:last-child {
  border-bottom: none;
}
</style>
