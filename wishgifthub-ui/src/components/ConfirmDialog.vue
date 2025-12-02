<script setup lang="ts">
import {useConfirmStore} from '@/stores/confirm'

const confirmStore = useConfirmStore()
</script>

<template>
  <v-dialog
    v-model="confirmStore.isOpen"
    max-width="500"
    persistent
  >
    <v-card>
      <v-card-title class="d-flex align-center pa-4 bg-surface-variant">
        <v-icon
          :icon="confirmStore.options.icon"
          :color="confirmStore.options.iconColor"
          size="large"
          class="mr-3"
        />
        <span class="text-h6">{{ confirmStore.options.title }}</span>
      </v-card-title>

      <v-card-text class="pa-6">
        <p class="text-body-1">{{ confirmStore.options.message }}</p>
      </v-card-text>

      <v-divider />

      <v-card-actions class="pa-4">
        <v-spacer />
        <v-btn
          variant="text"
          @click="confirmStore.handleCancel"
        >
          {{ confirmStore.options.cancelText }}
        </v-btn>
        <v-btn
          :color="confirmStore.options.confirmColor"
          variant="elevated"
          @click="confirmStore.handleConfirm"
        >
          {{ confirmStore.options.confirmText }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

