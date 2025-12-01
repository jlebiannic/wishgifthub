<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {getApiClient} from '@/api/client'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isLoading = ref(true)
const error = ref<string | null>(null)
const invitationEmail = ref<string>('')

onMounted(async () => {
  const token = route.params.token as string

  if (!token) {
    error.value = 'Token d\'invitation manquant'
    isLoading.value = false
    return
  }

  try {
    // Appeler l'API pour accepter l'invitation
    const apiClient = getApiClient()
    const response = await apiClient.accept(token)

    if (response.data) {
      invitationEmail.value = response.data.email

      // Connecter automatiquement l'utilisateur avec le JWT retourné
      if (response.data.jwtToken) {
        await authStore.loginWithToken(response.data.jwtToken, response.data.email)

        // Rediriger vers la page d'accueil après un court délai
        setTimeout(() => {
          router.push('/')
        }, 2000)
      } else {
        error.value = 'Erreur lors de la connexion automatique'
        isLoading.value = false
      }
    }
  } catch (err: any) {
    if (err.response?.status === 404) {
      error.value = 'Invitation non trouvée ou expirée'
    } else if (err.response?.status === 400) {
      error.value = 'Cette invitation a déjà été acceptée'
    } else {
      error.value = err.response?.data?.message || 'Erreur lors de l\'acceptation de l\'invitation'
    }
    isLoading.value = false
  }
})
</script>

<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="6" lg="4">
        <v-card elevation="8" class="mx-auto">
          <v-card-title class="text-h5 text-center pa-6 bg-primary">
            <v-icon size="large" class="mr-2">mdi-email-check</v-icon>
            Acceptation de l'invitation
          </v-card-title>

          <v-card-text class="pa-8">
            <!-- Chargement -->
            <div v-if="isLoading" class="text-center">
              <v-progress-circular
                indeterminate
                color="primary"
                size="64"
                class="mb-4"
              />
              <p class="text-h6 mb-2">Acceptation de votre invitation...</p>
              <p class="text-body-2 text-medium-emphasis">
                Veuillez patienter pendant que nous créons votre compte
              </p>
            </div>

            <!-- Succès -->
            <div v-else-if="!error && invitationEmail" class="text-center">
              <v-icon size="80" color="success" class="mb-4">
                mdi-check-circle
              </v-icon>
              <p class="text-h5 mb-4">Invitation acceptée !</p>
              <p class="text-body-1 mb-2">
                Bienvenue <strong>{{ invitationEmail }}</strong>
              </p>
              <p class="text-body-2 text-medium-emphasis mb-4">
                Vous allez être redirigé vers votre tableau de bord...
              </p>
              <v-progress-linear
                indeterminate
                color="success"
                class="mt-4"
              />
            </div>

            <!-- Erreur -->
            <div v-else-if="error" class="text-center">
              <v-icon size="80" color="error" class="mb-4">
                mdi-alert-circle
              </v-icon>
              <p class="text-h5 mb-4">Erreur</p>
              <v-alert
                type="error"
                variant="tonal"
                class="mb-4"
              >
                {{ error }}
              </v-alert>
              <v-btn
                color="primary"
                variant="elevated"
                prepend-icon="mdi-home"
                @click="router.push('/')"
              >
                Retour à l'accueil
              </v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

