import {ref} from 'vue'
import {defineStore} from 'pinia'

export interface Notification {
  id: string
  message: string
  type: 'success' | 'error' | 'warning' | 'info'
  timeout?: number
}

/**
 * Store gérant les notifications/snackbars de l'application
 */
export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])

  /**
   * Affiche une notification de succès
   */
  function showSuccess(message: string, timeout = 3000) {
    addNotification({
      id: generateId(),
      message,
      type: 'success',
      timeout
    })
  }

  /**
   * Affiche une notification d'erreur
   */
  function showError(message: string, timeout = 5000) {
    addNotification({
      id: generateId(),
      message,
      type: 'error',
      timeout
    })
  }

  /**
   * Affiche une notification d'avertissement
   */
  function showWarning(message: string, timeout = 4000) {
    addNotification({
      id: generateId(),
      message,
      type: 'warning',
      timeout
    })
  }

  /**
   * Affiche une notification d'information
   */
  function showInfo(message: string, timeout = 3000) {
    addNotification({
      id: generateId(),
      message,
      type: 'info',
      timeout
    })
  }

  /**
   * Ajoute une notification à la liste
   */
  function addNotification(notification: Notification) {
    notifications.value.push(notification)

    // Auto-suppression après le timeout
    if (notification.timeout && notification.timeout > 0) {
      setTimeout(() => {
        removeNotification(notification.id)
      }, notification.timeout)
    }
  }

  /**
   * Supprime une notification
   */
  function removeNotification(id: string) {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index !== -1) {
      notifications.value.splice(index, 1)
    }
  }

  /**
   * Génère un ID unique pour une notification
   */
  function generateId(): string {
    return `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }

  return {
    notifications,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    removeNotification
  }
})

