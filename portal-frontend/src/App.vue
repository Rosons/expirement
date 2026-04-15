<script setup lang="ts">
import GlobalErrorToast from './components/GlobalErrorToast.vue';
</script>

<template>
  <div class="app-shell">
    <RouterView />
    <GlobalErrorToast />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}
</style>
