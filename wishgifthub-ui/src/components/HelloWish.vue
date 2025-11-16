<script setup lang="ts">
import {ref} from 'vue'

const message = ref('Hello Wish !')
</script>

<template>
  <v-container class="fill-height">
    <v-row align="center" justify="center">
      <v-col cols="12" md="8" lg="6">
        <v-card class="mx-auto elevation-12" rounded="xl">
          <v-card-text class="text-center pa-12">
            <v-avatar size="120" class="mb-6" color="primary">
              <v-icon size="64" color="white">mdi-gift</v-icon>
            </v-avatar>

            <h1 class="text-h2 font-weight-bold mb-4 text-gradient">
              {{ message }}
            </h1>

            <p class="text-h6 text-medium-emphasis mb-8">
              Bienvenue sur votre plateforme de gestion de cadeaux 🎁
            </p>

            <v-divider class="mb-6"></v-divider>

            <v-chip-group class="justify-center mb-6">
              <v-chip color="primary" variant="flat">
                <v-icon start>mdi-vuejs</v-icon>
                Vue 3
              </v-chip>
              <v-chip color="secondary" variant="flat">
                <v-icon start>mdi-language-typescript</v-icon>
                TypeScript
              </v-chip>
              <v-chip color="accent" variant="flat">
                <v-icon start>mdi-material-design</v-icon>
                Vuetify 3
              </v-chip>
            </v-chip-group>

            <v-btn
              color="primary"
              size="large"
              variant="elevated"
              prepend-icon="mdi-rocket-launch"
            >
              Commencer
            </v-btn>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.text-gradient {
  background: linear-gradient(45deg, #6366F1, #EC4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
</style>

