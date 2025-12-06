<script setup lang="ts">
import {useTheme} from 'vuetify'
import {RouterView} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {ref} from 'vue'
import NotificationManager from '@/components/NotificationManager.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import {AVATARS} from '@/utils/avatars'

const theme = useTheme()
const authStore = useAuthStore()
const showAvatarMenu = ref(false)
const showAvatarDialog = ref(false)
const selectedAvatarId = ref<string | null>(null)

function toggleTheme() {
  theme.global.name.value = theme.global.current.value.dark ? 'light' : 'dark'
}

function openAvatarDialog() {
  selectedAvatarId.value = authStore.user?.avatarId || null
  showAvatarDialog.value = true
  showAvatarMenu.value = false
}

async function selectAvatar(avatarId: string) {
  selectedAvatarId.value = avatarId
  await authStore.updateAvatar(avatarId)
  showAvatarDialog.value = false
}

async function clearAvatar() {
  selectedAvatarId.value = null
  await authStore.updateAvatar(null)
  showAvatarDialog.value = false
}
</script>

<template>
  <v-app>
    <v-app-bar color="primary" elevation="2">
      <template v-slot:prepend>
        <v-icon size="large">mdi-gift-outline</v-icon>
      </template>
      <v-app-bar-title class="font-weight-bold">WishGiftHub</v-app-bar-title>
      <template v-slot:append>
        <v-menu v-if="authStore.isAuthenticated" v-model="showAvatarMenu" :close-on-content-click="false">
          <template v-slot:activator="{ props }">
            <v-btn v-bind="props" icon class="mr-2">
              <UserAvatar
                :avatar-id="authStore.user?.avatarId"
                :size="40"
              />
            </v-btn>
          </template>
          <v-card min-width="300">
            <v-card-text>
              <div class="text-center mb-3">
                <UserAvatar
                  :avatar-id="authStore.user?.avatarId"
                  :size="80"
                  class="mx-auto mb-3"
                />
                <div class="text-h6">{{ authStore.user?.username }}</div>
                <div class="text-caption text-medium-emphasis">{{ authStore.user?.email }}</div>
              </div>
              <v-divider class="my-3" />
              <v-btn
                color="primary"
                variant="tonal"
                block
                prepend-icon="mdi-account-circle"
                @click="openAvatarDialog"
              >
                Changer l'avatar
              </v-btn>
            </v-card-text>
          </v-card>
        </v-menu>

        <v-btn icon @click="toggleTheme">
          <v-icon>mdi-theme-light-dark</v-icon>
        </v-btn>
      </template>
    </v-app-bar>

    <v-main>
      <RouterView />
    </v-main>

    <!-- Gestionnaire de notifications -->
    <NotificationManager />

    <!-- Dialogue de confirmation -->
    <ConfirmDialog />

    <!-- Sélecteur d'avatar -->
    <v-dialog v-model="showAvatarDialog" max-width="700px" scrollable>
      <v-card>
        <v-card-title class="d-flex align-center pa-4 bg-primary">
          <v-icon class="mr-2" color="white">mdi-account-circle</v-icon>
          <span class="text-white">Choisir votre avatar</span>
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
          <v-btn
            v-if="authStore.user?.avatarId"
            variant="text"
            color="error"
            prepend-icon="mdi-delete"
            @click="clearAvatar"
          >
            Supprimer l'avatar
          </v-btn>
          <v-spacer />
          <v-btn
            variant="text"
            @click="showAvatarDialog = false"
          >
            Fermer
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<style scoped>
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


