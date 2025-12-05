<script setup lang="ts">
import {computed, ref} from 'vue'
import {AVATARS, getAvatarById} from '@/utils/avatars'

const props = defineProps<{
  modelValue?: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const dialog = ref(false)
const selectedAvatarId = ref(props.modelValue || null)

const selectedAvatar = computed(() => getAvatarById(selectedAvatarId.value))

function selectAvatar(avatarId: string) {
  selectedAvatarId.value = avatarId
  emit('update:modelValue', avatarId)
  dialog.value = false
}

function clearAvatar() {
  selectedAvatarId.value = null
  emit('update:modelValue', null as any)
}

function openDialog() {
  dialog.value = true
}
</script>

<template>
  <div class="avatar-selector-container">
    <div class="d-flex gap-2">
      <!-- Bouton pour ouvrir le sélecteur -->
      <v-btn
        variant="tonal"
        color="primary"
        @click="openDialog"
        class="avatar-selector-button flex-grow-1"
        size="large"
      >
        <div v-if="selectedAvatar" class="d-flex align-center">
          <div
            class="avatar-preview mr-3"
            :style="{ backgroundColor: selectedAvatar.color }"
          >
            {{ selectedAvatar.emoji }}
          </div>
          <span class="text-body-1">{{ selectedAvatar.name }}</span>
        </div>
        <div v-else class="d-flex align-center">
          <v-icon class="mr-3" size="large">mdi-account-circle</v-icon>
          <span class="text-body-1">Sélectionner un avatar</span>
        </div>
      </v-btn>

      <!-- Bouton pour supprimer l'avatar -->
      <v-btn
        v-if="selectedAvatar"
        variant="text"
        color="error"
        @click="clearAvatar"
        icon
        size="large"
        class="avatar-clear-button"
      >
        <v-icon>mdi-close-circle</v-icon>
        <v-tooltip activator="parent" location="top">
          Supprimer l'avatar
        </v-tooltip>
      </v-btn>
    </div>

    <!-- Dialog de sélection -->
    <v-dialog v-model="dialog" max-width="700px" scrollable>
      <v-card>
        <v-card-title class="d-flex align-center pa-4 bg-primary">
          <v-icon class="mr-2" color="white">mdi-account-group</v-icon>
          <span class="text-white">Choisir un avatar</span>
        </v-card-title>

        <v-divider />

        <v-card-text class="pa-6" style="max-height: 70vh">
          <v-row>
            <v-col
              v-for="avatar in AVATARS"
              :key="avatar.id"
              cols="4"
              sm="3"
              md="2"
              class="d-flex justify-center"
            >
              <v-tooltip :text="avatar.name" location="top">
                <template v-slot:activator="{ props: tooltipProps }">
                  <div
                    v-bind="tooltipProps"
                    class="avatar-option-wrapper"
                    :class="{ 'avatar-selected': selectedAvatarId === avatar.id }"
                    @click="selectAvatar(avatar.id)"
                  >
                    <div
                      class="avatar-display"
                      :style="{ backgroundColor: avatar.color }"
                    >
                      {{ avatar.emoji }}
                    </div>
                  </div>
                </template>
              </v-tooltip>
            </v-col>
          </v-row>
        </v-card-text>

        <v-divider />

        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn
            variant="text"
            @click="dialog = false"
          >
            Annuler
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.avatar-selector-container {
  width: 100%;
}

.gap-2 {
  gap: 8px;
}

.avatar-selector-button {
  min-height: 56px;
  justify-content: flex-start !important;
  padding: 12px 16px !important;
}

.avatar-clear-button {
  min-height: 56px;
  flex-shrink: 0;
}

.avatar-preview {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.avatar-option-wrapper {
  cursor: pointer;
  padding: 8px;
  border-radius: 16px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
}

.avatar-option-wrapper:hover {
  transform: scale(1.15);
  background-color: rgba(0, 0, 0, 0.05);
}

.avatar-selected {
  background-color: rgba(33, 150, 243, 0.15) !important;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.4) !important;
}

.avatar-selected:hover {
  background-color: rgba(33, 150, 243, 0.2) !important;
}

.avatar-display {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.4);
}

.avatar-option-wrapper:hover .avatar-display {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
}

@media (max-width: 600px) {
  .avatar-display {
    width: 48px;
    height: 48px;
    font-size: 32px;
  }
}
</style>

