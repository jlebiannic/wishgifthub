<script setup lang="ts">
import {ref} from 'vue'
import {useAuthStore} from '@/stores/auth'

const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const showPassword = ref(false)

const emit = defineEmits<{
  loginSuccess: []
}>()

async function handleLogin() {
  const success = await authStore.login(username.value, password.value)
  if (success) {
    emit('loginSuccess')
  }
}
</script>

<template>
  <v-card class="mx-auto" max-width="500" elevation="8">
    <v-card-title class="text-h5 text-center pa-6">
      <v-icon size="large" color="primary" class="mr-2">mdi-login</v-icon>
      Connexion
    </v-card-title>

    <v-card-text class="px-6 pb-2">
      <v-alert
        v-if="authStore.error"
        type="error"
        variant="tonal"
        closable
        class="mb-4"
      >
        {{ authStore.error }}
      </v-alert>

      <v-alert type="info" variant="tonal" class="mb-6">
        <div class="text-body-2">
          <v-icon size="small" class="mr-1">mdi-information</v-icon>
          La connexion est réservée aux administrateurs
        </div>
      </v-alert>

      <v-form @submit.prevent="handleLogin">
        <v-text-field
          v-model="username"
          label="Identifiant"
          prepend-inner-icon="mdi-account"
          variant="outlined"
          :disabled="authStore.isLoading"
          required
          class="mb-3"
        />

        <v-text-field
          v-model="password"
          label="Mot de passe"
          prepend-inner-icon="mdi-lock"
          :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
          :type="showPassword ? 'text' : 'password'"
          variant="outlined"
          :disabled="authStore.isLoading"
          required
          @click:append-inner="showPassword = !showPassword"
        />
      </v-form>
    </v-card-text>

    <v-card-actions class="px-6 pb-6">
      <v-btn
        color="primary"
        size="large"
        block
        :loading="authStore.isLoading"
        @click="handleLogin"
      >
        Se connecter
      </v-btn>
    </v-card-actions>

    <v-divider />

    <v-card-text class="text-center py-3">
      <v-btn
        variant="text"
        size="small"
        color="info"
        prepend-icon="mdi-information-outline"
      >
        En savoir plus sur les rôles
      </v-btn>
    </v-card-text>
  </v-card>
</template>

