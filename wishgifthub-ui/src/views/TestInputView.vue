<script setup lang="ts">
import {ref} from 'vue'

const testInput = ref('')

function onInput() {
  console.log('Input changed:', testInput.value)
}
</script>

<template>
  <v-container class="pa-8">
    <v-card max-width="600" class="mx-auto">
      <v-card-title>Test de saisie - Debug</v-card-title>
      <v-card-text>
        <h3 class="mb-4">Test 1 : Input HTML standard</h3>
        <input
          v-model="testInput"
          type="text"
          placeholder="Taper ici (input standard)"
          style="width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px;"
          @input="onInput"
        />
        <p class="mt-2">Valeur: {{ testInput }}</p>

        <v-divider class="my-6" />

        <h3 class="mb-4">Test 2 : v-text-field Vuetify</h3>
        <v-text-field
          v-model="testInput"
          label="Taper ici (Vuetify)"
          variant="outlined"
          @update:model-value="onInput"
        />
        <p>Valeur: {{ testInput }}</p>

        <v-divider class="my-6" />

        <h3 class="mb-4">Test 3 : v-text-field avec tous les props</h3>
        <v-text-field
          v-model="testInput"
          label="Identifiant de test"
          prepend-inner-icon="mdi-account"
          variant="outlined"
          density="comfortable"
          clearable
        />
        <p>Valeur: {{ testInput }}</p>

        <v-divider class="my-6" />

        <v-alert type="info" variant="tonal">
          <strong>Instructions:</strong><br>
          1. Essaie de cliquer et taper dans chaque champ<br>
          2. Vérifie que la valeur s'affiche en dessous<br>
          3. Vérifie la console navigateur (F12) pour les logs
        </v-alert>
      </v-card-text>
    </v-card>
  </v-container>
</template>

