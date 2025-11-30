<script setup lang="ts">
import {ref} from 'vue'
import {useGroupStore} from '@/stores/group'

const groupStore = useGroupStore()

const dialog = ref(false)
const groupName = ref('')

const emit = defineEmits<{
  groupCreated: []
}>()

async function handleCreateGroup() {
  const success = await groupStore.createGroup(groupName.value, 'noël')
  if (success) {
    dialog.value = false
    groupName.value = ''
    emit('groupCreated')
  }
}

function closeDialog() {
  dialog.value = false
  groupName.value = ''
}
</script>

<template>
  <div>
    <v-btn
      color="primary"
      size="large"
      prepend-icon="mdi-plus-circle"
      @click="dialog = true"
    >
      Créer un groupe
    </v-btn>

    <v-dialog v-model="dialog" max-width="600">
      <v-card>
        <v-card-title class="pa-4">
          <div class="text-h6">
            <v-icon class="mr-2">mdi-account-group</v-icon>
            Créer un nouveau groupe Noël
          </div>
        </v-card-title>

        <v-divider />

        <v-card-text class="pa-4">
          <v-alert
            v-if="groupStore.error"
            type="error"
            variant="tonal"
            closable
            class="mb-4"
          >
            {{ groupStore.error }}
          </v-alert>

          <v-form @submit.prevent="handleCreateGroup">
            <v-text-field
              v-model="groupName"
              label="Nom du groupe"
              prepend-inner-icon="mdi-account-group"
              variant="outlined"
              :disabled="groupStore.isLoading"
              required
              placeholder="Ex: Noël en famille 2025"
              hint="Le type du groupe est automatiquement défini sur 'noël'"
              persistent-hint
            />
          </v-form>
        </v-card-text>

        <v-divider />

        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn
            variant="text"
            :disabled="groupStore.isLoading"
            @click="closeDialog"
          >
            Annuler
          </v-btn>
          <v-btn
            color="primary"
            :loading="groupStore.isLoading"
            @click="handleCreateGroup"
          >
            Créer
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

