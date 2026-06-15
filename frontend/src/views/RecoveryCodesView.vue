<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/client'
const codes = ref<any[]>([])
const loading = ref(true)
const generating = ref(false)
const newCodes = ref<string[]>([])

async function load() {
  try {
    const { data } = await api.get('/auth/recovery-codes')
    codes.value = data
  } catch { /* ignore */ }
  loading.value = false
}

async function generate() {
  generating.value = true
  try {
    const { data } = await api.post('/auth/recovery-codes')
    newCodes.value = data.recoveryCodes
    load()
  } catch { /* ignore */ }
  generating.value = false
}

function downloadCodes() {
  const blob = new Blob([newCodes.value.join('\n')], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'recovery-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<template>
  <v-container class="d-flex justify-center">
    <v-card width="560">
      <v-card-title class="pa-4">
        <div class="text-h5 font-weight-bold">Recovery Codes</div>
        <div class="text-body-2 text-grey">Use these codes to log in if you forget your password</div>
      </v-card-title>

      <v-card-text>
        <v-alert v-if="newCodes.length" type="success" variant="tonal" density="compact" class="mb-4" icon="mdi-check-circle">
          New codes generated! Download them now.
        </v-alert>

        <v-card v-if="newCodes.length" variant="outlined" class="mb-4">
          <v-card-text class="text-center">
            <div v-for="(code, i) in newCodes" :key="i" class="text-monospace font-weight-bold py-1 text-h6">
              {{ code }}
            </div>
          </v-card-text>
        </v-card>

        <div class="d-flex ga-3 mb-4">
          <v-btn v-if="newCodes.length" color="primary" variant="outlined" @click="downloadCodes" prepend-icon="mdi-download">
            Download Codes
          </v-btn>
          <v-btn color="warning" :loading="generating" @click="generate" prepend-icon="mdi-refresh">
            Generate New Codes
          </v-btn>
        </div>

        <v-divider class="mb-3" />

        <div class="text-subtitle-2 text-grey mb-2">Used Codes</div>
        <v-list v-if="codes.length" density="compact">
          <v-list-item v-for="c in codes" :key="c.id">
            <template #prepend>
              <v-icon :color="c.isUsed ? 'grey' : 'success'">
                {{ c.isUsed ? 'mdi-close-circle' : 'mdi-check-circle' }}
              </v-icon>
            </template>
            <v-list-item-title class="text-caption">
              {{ c.isUsed ? 'Used' : 'Available' }} — {{ new Date(c.createdAt).toLocaleDateString() }}
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
  </v-container>
</template>
