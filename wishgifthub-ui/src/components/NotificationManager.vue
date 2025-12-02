<script setup lang="ts">
import {useNotificationStore} from '@/stores/notification'

const notificationStore = useNotificationStore()

function getColor(type: string): string {
  switch (type) {
    case 'success':
      return 'success'
    case 'error':
      return 'error'
    case 'warning':
      return 'warning'
    case 'info':
      return 'info'
    default:
      return 'primary'
  }
}

function getIcon(type: string): string {
  switch (type) {
    case 'success':
      return 'mdi-check-circle'
    case 'error':
      return 'mdi-alert-circle'
    case 'warning':
      return 'mdi-alert'
    case 'info':
      return 'mdi-information'
    default:
      return 'mdi-bell'
  }
}
</script>

<template>
  <div class="notification-container">
    <v-snackbar
      v-for="notification in notificationStore.notifications"
      :key="notification.id"
      :model-value="true"
      :color="getColor(notification.type)"
      :timeout="notification.timeout"
      location="top right"
      variant="elevated"
      class="mb-2"
      @update:model-value="notificationStore.removeNotification(notification.id)"
    >
      <div class="d-flex align-center">
        <v-icon :icon="getIcon(notification.type)" class="mr-3" />
        <span>{{ notification.message }}</span>
      </div>

      <template v-slot:actions>
        <v-btn
          icon="mdi-close"
          size="small"
          variant="text"
          @click="notificationStore.removeNotification(notification.id)"
        />
      </template>
    </v-snackbar>
  </div>
</template>

<style scoped>
.notification-container {
  position: fixed;
  top: 80px;
  right: 16px;
  z-index: 9999;
  pointer-events: none;
}

.notification-container :deep(.v-snackbar) {
  pointer-events: all;
  position: relative !important;
  margin-bottom: 8px;
}
</style>

