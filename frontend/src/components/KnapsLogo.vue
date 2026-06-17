<template>
  <div class="logo-container" :class="size">
    <img class="logo-graphic" src="/logo.png" alt="KNAPS" width="40" height="40" />
    <svg class="logo-text-svg" viewBox="0 0 360 140" xmlns="http://www.w3.org/2000/svg">
      <text x="0" y="82"
        font-family="Arial, Helvetica, sans-serif"
        font-size="96" font-weight="bold"
        fill="#FF0000"
        textLength="360" lengthAdjust="spacing">KNAPS</text>
      <line x1="0" y1="94" x2="360" y2="94" stroke="#0000FF" stroke-width="2.5" />
      <text x="0" y="120"
        font-family="Arial, Helvetica, sans-serif"
        font-size="20" font-weight="normal"
        fill="#0000FF"
        textLength="360" lengthAdjust="spacing">FINANCIAL SERVICES</text>
      <line x1="0" y1="130" x2="360" y2="130" stroke="#0000FF" stroke-width="2.5" />
    </svg>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ size?: 'sm' | 'md' }>(), { size: 'md' })
</script>

<style scoped>
.logo-container {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  line-height: 0;
}
.logo-graphic {
  object-fit: contain;
}
.logo-text-svg {
  flex-shrink: 0;
  display: block;
}
.md .logo-graphic { width: 66px; height: 66px; }
.md .logo-text-svg { width: 170px; height: 66px; }
.sm .logo-graphic { width: 43px; height: 43px; }
.sm .logo-text-svg { width: 110px; height: 43px; }
</style>
