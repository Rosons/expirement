<script setup lang="ts">
import { globalToastState } from '../services/notifications/error-toast-service';
</script>

<template>
  <Teleport to="body">
    <Transition name="global-top-toast">
      <div
        v-if="globalToastState.visible"
        class="global-top-toast"
        role="alert"
        aria-live="assertive"
      >
        <p class="global-top-toast__text">
          {{ globalToastState.text }}
        </p>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.global-top-toast {
  position: fixed;
  z-index: 99999;
  left: 50%;
  top: max(0.625rem, env(safe-area-inset-top, 0px));
  transform: translateX(-50%);
  max-width: min(34rem, calc(100vw - 1.75rem));
  padding: 0.7rem 1.15rem 0.72rem;
  border-radius: 1.125rem;
  text-align: center;
  color: #334155;
  font-weight: 500;
  letter-spacing: 0.015em;
  background: linear-gradient(
    165deg,
    rgba(255, 255, 255, 0.97) 0%,
    rgba(255, 250, 250, 0.94) 45%,
    rgba(254, 247, 247, 0.96) 100%
  );
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.95),
    0 1px 0 rgba(248, 113, 113, 0.18),
    0 10px 40px -12px rgba(15, 23, 42, 0.18),
    0 4px 14px -6px rgba(220, 38, 38, 0.12);
  backdrop-filter: blur(14px) saturate(1.2);
  -webkit-backdrop-filter: blur(14px) saturate(1.2);
  pointer-events: none;
}

.global-top-toast__text {
  margin: 0;
  font-size: 0.8125rem;
  line-height: 1.55;
  word-break: break-word;
}

.global-top-toast-enter-active {
  transition:
    opacity 0.26s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.global-top-toast-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.2s ease;
}

.global-top-toast-enter-from,
.global-top-toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-0.65rem) scale(0.98);
}
</style>
