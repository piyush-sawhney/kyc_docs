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
    }
    if (!isAdmin.value && auth.user?.id) {
      params.userId = auth.user.id
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
  activityHeight.value = Math.max(120, Math.min(600, resizeStartH + delta))
}
function onResizeEnd() {
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

function actionBadgeClass(action: string): string {
  if (['CREATE', 'LOGIN', 'RESTORE', 'REACTIVATE'].includes(action)) return 'bg-soft-success'
  if (['UPDATE', 'ROLE_CHANGE', 'PASSWORD_CHANGE', 'RECOVERY_CODES'].includes(action)) return 'bg-soft-warning'
  if (['DELETE', 'DEACTIVATE'].includes(action)) return 'bg-soft-danger'
  if (action === 'RESET_PASSWORD') return 'bg-soft-warning'
  return 'bg-soft-secondary'
}

function actionIconClass(action: string): string {
  if (['CREATE', 'LOGIN', 'RESTORE', 'REACTIVATE'].includes(action)) return 'bi bi-plus-circle'
  if (['UPDATE', 'ROLE_CHANGE', 'PASSWORD_CHANGE', 'RECOVERY_CODES'].includes(action)) return 'bi bi-pencil-circle'
  if (['DELETE'].includes(action)) return 'bi bi-trash'
  if (action === 'DEACTIVATE') return 'bi bi-person-x'
  if (action === 'RESET_PASSWORD') return 'bi bi-key'
  return 'bi bi-circle'
}

function actionColor(action: string): string {
  if (['CREATE', 'LOGIN', 'RESTORE', 'REACTIVATE'].includes(action)) return '#16A34A'
  if (['UPDATE', 'ROLE_CHANGE', 'PASSWORD_CHANGE', 'RECOVERY_CODES'].includes(action)) return '#F59E0B'
  if (['DELETE', 'DEACTIVATE'].includes(action)) return '#DC2626'
  return '#4F46E5'
}

function relativeTime(ts: string) {
  const diff = Date.now() - new Date(ts).getTime()
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d ago`
  return new Date(ts).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

function initials(name: string) {
  if (!name) return '?'
  return name.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2)
}
</script>

<template>
  <div class="card border-0 shadow-sm" style="border-radius: 12px;">
    <div class="d-flex align-items-center px-3 py-2">
      <i class="bi bi-shield-account me-2" style="color: #1E3A5F; font-size: 16px;"></i>
      <small class="fw-semibold" style="color: #1E293B;">Activity</small>
      <span v-if="total" class="badge bg-soft-primary ms-2">{{ total }}</span>
      <span class="ms-auto">
        <span v-if="isAdmin && !clientId" class="badge bg-soft-info">All activity</span>
        <span v-else-if="!isAdmin && !clientId" class="badge bg-soft-info">My activity</span>
      </span>
    </div>

    <div class="resize-handle" @mousedown.prevent="onResizeStart" />

    <div ref="containerRef" class="activity-list" :style="{ maxHeight: activityHeight + 'px' }" @scroll="onScroll">
      <div v-if="errorMsg" class="text-center py-4">
        <i class="bi bi-exclamation-circle" style="font-size: 28px; color: #DC2626;"></i>
        <div><small class="text-muted">{{ errorMsg }}</small></div>
      </div>
      <div v-else-if="entries.length === 0 && !loading" class="text-center py-4">
        <i class="bi bi-shield-slash" style="font-size: 28px; color: #CBD5E1;"></i>
        <div><small class="text-muted">No activity yet</small></div>
      </div>

      <div>
        <div v-for="entry in entries" :key="entry.id" class="activity-item">
          <div class="d-flex align-items-start gap-2">
            <div class="avatar-initials avatar-initials-sm flex-shrink-0 mt-1"
              style="background: #F1F5F9; color: #64748B;">
              {{ initials(entry.userFullName) }}
            </div>
            <div class="flex-grow-1 min-w-0">
              <small style="color: #1E293B;">{{ entry.description || `${entry.action} ${entry.entityType}` }}</small>
              <div class="d-flex align-items-center gap-1 mt-0">
                <small class="fw-medium text-muted">{{ entry.userFullName || 'System' }}</small>
                <small class="text-muted">·</small>
                <small class="text-muted">{{ relativeTime(entry.createdAt) }}</small>
              </div>
            </div>
            <i :class="actionIconClass(entry.action)" :style="{ color: actionColor(entry.action), fontSize: '14px' }" class="flex-shrink-0 mt-1"></i>
          </div>
        </div>
      </div>

      <div v-if="loading" class="d-flex align-items-center justify-content-center py-3">
        <div class="spinner-border spinner-border-sm text-primary me-2"></div>
        <small class="text-muted">Loading...</small>
      </div>

      <div v-if="allLoaded && entries.length > 0" class="text-center py-2">
        <small class="text-muted">All activity loaded</small>
      </div>
    </div>
  </div>
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
  padding: 8px 12px;
  border-bottom: 1px solid rgb(241, 245, 249);
  transition: background 0.15s ease;
}
.activity-item:hover {
  background: rgba(30, 58, 95, 0.02);
}
.activity-item:last-child {
  border-bottom: none;
}
</style>
