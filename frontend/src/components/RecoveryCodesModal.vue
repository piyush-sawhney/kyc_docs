<script setup lang="ts">
const props = withDefaults(defineProps<{
  show: boolean
  recoveryCodes: string[]
  warningMessage?: string
  downloadButtonText?: string
}>(), {
  warningMessage: 'Save these codes. Each code can only be used once.',
  downloadButtonText: 'Download Recovery Codes',
})

const emit = defineEmits<{
  continue: []
  close: []
}>()

function downloadCodes() {
  const blob = new Blob([props.recoveryCodes.join('\n')], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
  emit('continue')
}

async function copyCode(code: string) {
  try {
    await navigator.clipboard.writeText(code)
  } catch { /* ignore */ }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="recovery-codes-modal-wrapper">
      <div class="modal-backdrop fade show"></div>
      <div class="modal d-block" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered" style="max-width: 480px;">
          <div class="modal-content border-0 shadow">
            <div class="modal-body text-center p-4">
              <div class="d-inline-flex align-items-center justify-content-center mb-3"
                style="width: 64px; height: 64px; border-radius: 50%; background: rgba(245,158,11,0.1);">
                <i class="bi bi-shield-lock" style="font-size: 32px; color: #F59E0B;"></i>
              </div>
              <h6 class="fw-bold mb-2" style="color: #1E293B;">Recovery Codes</h6>
              <div class="alert alert-warning d-flex align-items-center py-2 px-3 small mb-3 text-start" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>
                {{ warningMessage }}
              </div>

              <div class="card bg-light border-0 mb-3">
                <div class="card-body py-3">
                  <div v-for="(code, i) in recoveryCodes" :key="i"
                    class="d-flex align-items-center justify-content-center gap-2 py-1">
                    <code class="fs-5 fw-bold" style="color: #1E3A5F; letter-spacing: 1px;">{{ code }}</code>
                    <button class="btn btn-sm btn-outline-secondary border-0" @click="copyCode(code)" title="Copy code">
                      <i class="bi bi-clipboard"></i>
                    </button>
                  </div>
                </div>
              </div>

              <button class="btn btn-primary btn-lg w-100" @click="downloadCodes">
                <i class="bi bi-download me-1"></i> {{ downloadButtonText }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
