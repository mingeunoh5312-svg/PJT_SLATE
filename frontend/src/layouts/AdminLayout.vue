<script setup>
import AppLayout from './AppLayout.vue'
</script>

<template>
  <AppLayout v-bind="$attrs" section="admin">
    <slot />
  </AppLayout>
</template>
