<script setup lang="ts">
import type {Invitation} from '@/stores/group'

defineProps<{
  invitations: Invitation[]
  isLoading: boolean
}>()

const statusColors = {
  ACCEPTED: 'success',
  PENDING: 'warning',
  REJECTED: 'error',
}

const statusLabels = {
  ACCEPTED: 'Accepté',
  PENDING: 'En attente',
  REJECTED: 'Refusé',
}

const statusIcons = {
  ACCEPTED: 'mdi-check-circle',
  PENDING: 'mdi-clock-outline',
  REJECTED: 'mdi-close-circle',
}
</script>

<template>
  <v-dialog max-width="700" scrollable>
    <v-card>
      <v-card-title class="d-flex align-center justify-space-between pa-4">
        <div class="text-h6">
          <v-icon class="mr-2">mdi-account-multiple</v-icon>
          Invitations du groupe
        </div>
      </v-card-title>

      <v-divider />

      <v-card-text class="pa-4" style="max-height: 500px">
        <v-progress-linear v-if="isLoading" indeterminate color="primary" class="mb-4" />

        <div v-if="!isLoading && invitations.length === 0" class="text-center py-8">
          <v-icon size="64" color="grey-lighten-1">mdi-email-outline</v-icon>
          <div class="text-h6 text-medium-emphasis mt-4">Aucune invitation</div>
        </div>

        <v-list v-else lines="two">
          <v-list-item
            v-for="invitation in invitations"
            :key="invitation.id"
            class="mb-2 rounded border"
          >
            <template v-slot:prepend>
              <v-avatar :color="statusColors[invitation.status]">
                <v-icon :icon="statusIcons[invitation.status]" color="white" />
              </v-avatar>
            </template>

            <v-list-item-title>
              {{ invitation.invitedUsername }}
            </v-list-item-title>

            <v-list-item-subtitle>
              {{ invitation.invitedUserEmail }}
            </v-list-item-subtitle>

            <template v-slot:append>
              <v-chip
                :color="statusColors[invitation.status]"
                size="small"
                variant="flat"
              >
                {{ statusLabels[invitation.status] }}
              </v-chip>
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>

      <v-divider />

      <v-card-actions class="pa-4">
        <v-spacer />
        <v-btn color="primary" variant="text" @click="$emit('close')">
          Fermer
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

