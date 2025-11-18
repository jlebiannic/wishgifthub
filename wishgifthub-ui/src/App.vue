<script setup lang="ts">
import {useTheme} from 'vuetify'
import {RouterView} from 'vue-router'
import {useAuthStore} from '@/stores/auth'

const theme = useTheme()
const authStore = useAuthStore()

function toggleTheme() {
  theme.global.name.value = theme.global.current.value.dark ? 'light' : 'dark'
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
        <v-chip
          v-if="authStore.isAuthenticated"
          class="mr-2"
          prepend-icon="mdi-account-circle"
        >
          {{ authStore.user?.username }}
        </v-chip>
        <v-btn icon @click="toggleTheme">
          <v-icon>mdi-theme-light-dark</v-icon>
        </v-btn>
      </template>
    </v-app-bar>

    <v-main>
      <RouterView />
    </v-main>
  </v-app>
</template>


