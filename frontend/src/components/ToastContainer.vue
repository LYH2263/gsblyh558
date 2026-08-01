<script setup>
import { useToast } from '../composables/useToast'

const { toasts, removeToast } = useToast()
</script>

<template>
  <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 3000;">
    <transition-group name="toast-fade">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast show align-items-center text-white border-0 mb-2"
        :class="`bg-${toast.type}`"
        role="alert"
        aria-live="assertive"
        aria-atomic="true"
      >
        <div class="d-flex">
          <div class="toast-body">
            {{ toast.message }}
          </div>
          <button
            type="button"
            class="btn-close btn-close-white me-2 m-auto"
            @click="removeToast(toast.id)"
            aria-label="Close"
          ></button>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
