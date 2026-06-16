<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  modelValue: string
  label?: string
  disabled?: boolean
  showStrength?: boolean
  hint?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const showPassword = ref(false)

function strength(value: string): { level: number; label: string; color: string } {
  if (!value) return { level: 0, label: '', color: '' }
  let score = 0
  if (value.length >= 8) score++
  if (value.length >= 12) score++
  if (/[A-Z]/.test(value)) score++
  if (/[a-z]/.test(value)) score++
  if (/[0-9]/.test(value)) score++
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value)) score++

  if (score <= 2) return { level: 1, label: 'Weak', color: '#DC2626' }
  if (score <= 3) return { level: 2, label: 'Fair', color: '#F59E0B' }
  if (score <= 4) return { level: 3, label: 'Good', color: '#0EA5E9' }
  return { level: 4, label: 'Strong', color: '#16A34A' }
}

const pwStrength = computed(() => strength(props.modelValue))
</script>

<template>
  <div>
    <div class="mb-1">
      <label class="form-label">{{ label || 'Password' }}</label>
      <div class="input-group">
        <input
          :type="showPassword ? 'text' : 'password'"
          class="form-control"
          :value="modelValue"
          @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
          :disabled="disabled"
          :placeholder="label || 'Password'" />
        <button class="btn btn-outline-secondary" type="button" @click="showPassword = !showPassword"
          :disabled="disabled" tabindex="-1">
          <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
        </button>
      </div>
      <div v-if="hint" class="form-text">{{ hint }}</div>
    </div>

    <div v-if="showStrength && modelValue" class="mt-2">
      <div class="password-strength" :style="{ width: (pwStrength.level / 4) * 100 + '%', background: pwStrength.color }"></div>
      <small :style="{ color: pwStrength.color }" class="fw-medium">{{ pwStrength.label }}</small>
      <small v-if="pwStrength.level < 4" class="text-muted ms-1">— 8+ chars, upper, lower, number & special</small>
    </div>
  </div>
</template>
