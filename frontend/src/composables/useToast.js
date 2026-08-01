import { ref } from 'vue'

const toasts = ref([])

let idCounter = 0

export function useToast() {
  const addToast = (message, type = 'info', duration = 3000) => {
    const id = idCounter++
    const toast = { id, message, type, duration }
    toasts.value.push(toast)

    if (duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }
  }

  const removeToast = (id) => {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) {
      toasts.value.splice(index, 1)
    }
  }

  const success = (msg, duration) => addToast(msg, 'success', duration)
  const error = (msg, duration) => addToast(msg, 'danger', duration)
  const info = (msg, duration) => addToast(msg, 'info', duration)
  const warning = (msg, duration) => addToast(msg, 'warning', duration)

  return {
    toasts,
    addToast,
    removeToast,
    success,
    error,
    info,
    warning
  }
}
