<script setup lang="ts">
import {useRouter} from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<template>
  <v-container fluid class="fill-height py-8 px-6">
    <v-row justify="center" align="center">
      <v-col cols="12" md="8" lg="6">
        <v-card class="mx-auto" elevation="8">
          <v-card-title class="text-h5 text-center pa-6 bg-warning">
            <v-icon size="x-large" class="mr-2">mdi-clock-alert-outline</v-icon>
            Session Expirée
          </v-card-title>

          <v-card-text class="pa-8">
            <v-alert type="warning" variant="tonal" class="mb-6">
              <div class="text-h6 mb-2">Votre session a expiré</div>
              <div class="text-body-1">
                Pour des raisons de sécurité, votre lien d'accès a une durée de validité limitée.
              </div>
            </v-alert>

            <div class="mb-6">
              <h3 class="text-h6 mb-3">
                <v-icon color="primary" class="mr-2">mdi-email-outline</v-icon>
                Comment accéder à nouveau à l'application ?
              </h3>
              <p class="text-body-1 ml-8">
                Veuillez utiliser le lien d'invitation que vous avez reçu par email.
                Ce lien vous permettra de vous reconnecter automatiquement.
              </p>
            </div>

            <v-divider class="my-6" />

            <div>
              <h3 class="text-h6 mb-3">
                <v-icon color="info" class="mr-2">mdi-help-circle-outline</v-icon>
                Vous n'avez plus le lien ?
              </h3>
              <p class="text-body-1 ml-8">
                Contactez la personne qui vous a invité pour recevoir une nouvelle invitation.
              </p>
            </div>

            <v-alert type="info" variant="tonal" class="mt-6">
              <div class="text-body-2">
                Astuce : Ajoutez le lien d'invitation à vos favoris pour y accéder facilement.
              </div>
            </v-alert>
          </v-card-text>

          <v-card-actions class="px-6 pb-6">
            <v-spacer />
            <v-btn
              color="primary"
              variant="outlined"
              prepend-icon="mdi-home"
              @click="goHome"
            >
              Retour à l'accueil
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

