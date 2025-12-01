<script setup lang="ts">
import {ref, watch} from 'vue'
import {useWishStore} from '@/stores/wish'

const props = defineProps<{
  groupId: string
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  wishAdded: []
}>()

const wishStore = useWishStore()

// Formulaire
const url = ref('')
const imageUrl = ref('')
const title = ref('')
const description = ref('')
const price = ref('')

const isLoadingMetadata = ref(false)
const isSaving = ref(false)
const errorMessage = ref('')

// Règles de validation
const rules = {
  title: [
    (v: string) => !!v || 'Le titre est requis',
    (v: string) => (v && v.length >= 1 && v.length <= 255) || 'Le titre doit contenir entre 1 et 255 caractères'
  ],
  description: [
    (v: string) => !v || v.length <= 1000 || 'La description ne peut pas dépasser 1000 caractères'
  ]
}

// Fonction pour extraire les métadonnées d'une URL (simulation)
async function fetchMetadataFromUrl() {
  if (!url.value) return

  isLoadingMetadata.value = true
  errorMessage.value = ''

  try {
    // TODO: Implémenter un service backend pour extraire les métadonnées
    // Pour l'instant, on simule juste avec un délai
    await new Promise(resolve => setTimeout(resolve, 500))

    // Simulation - à remplacer par un vrai appel API
    // const response = await fetch(`/api/metadata?url=${encodeURIComponent(url.value)}`)
    // const data = await response.json()

    // Pour l'instant, on peut juste utiliser l'URL comme image si c'est une image
    if (url.value.match(/\.(jpg|jpeg|png|gif|webp)$/i)) {
      imageUrl.value = url.value
    }

    // Les autres champs restent modifiables manuellement
  } catch (error) {
    console.error('Erreur lors de la récupération des métadonnées:', error)
  } finally {
    isLoadingMetadata.value = false
  }
}

// Watch l'URL pour déclencher la récupération des métadonnées
watch(url, () => {
  if (url.value) {
    fetchMetadataFromUrl()
  }
})

async function handleSubmit() {
  if (!title.value) {
    errorMessage.value = 'Le titre est requis'
    return
  }

  isSaving.value = true
  errorMessage.value = ''

  try {
    await wishStore.addWish(props.groupId, {
      giftName: title.value,
      description: description.value || null,
      url: url.value || null
    })

    // Réinitialiser le formulaire
    resetForm()

    // Fermer le dialog
    emit('update:modelValue', false)
    emit('wishAdded')
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || 'Erreur lors de l\'ajout du souhait'
  } finally {
    isSaving.value = false
  }
}

function resetForm() {
  url.value = ''
  imageUrl.value = ''
  title.value = ''
  description.value = ''
  price.value = ''
  errorMessage.value = ''
}

function handleClose() {
  resetForm()
  emit('update:modelValue', false)
}
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    max-width="600"
    persistent
  >
    <v-card>
      <v-card-title class="pa-4 bg-primary">
        <v-icon class="mr-2">mdi-gift-outline</v-icon>
        Ajouter un souhait
      </v-card-title>

      <v-card-text class="pa-4">
        <v-form @submit.prevent="handleSubmit">
          <!-- Champ URL -->
          <v-text-field
            v-model="url"
            label="URL du produit (optionnel)"
            placeholder="https://example.com/produit"
            prepend-inner-icon="mdi-link"
            variant="outlined"
            density="comfortable"
            class="mb-3"
            hint="Si renseignée, l'URL peut pré-remplir les autres champs"
            persistent-hint
            :loading="isLoadingMetadata"
          />

          <!-- Champ Image URL -->
          <v-text-field
            v-model="imageUrl"
            label="URL de l'image (optionnel)"
            placeholder="https://example.com/image.jpg"
            prepend-inner-icon="mdi-image"
            variant="outlined"
            density="comfortable"
            class="mb-3"
          />

          <!-- Prévisualisation de l'image -->
          <v-card v-if="imageUrl" variant="outlined" class="mb-3">
            <v-img
              :src="imageUrl"
              height="200"
              cover
              @error="imageUrl = ''"
            >
              <template v-slot:placeholder>
                <v-row
                  class="fill-height ma-0"
                  align="center"
                  justify="center"
                >
                  <v-progress-circular
                    indeterminate
                    color="grey-lighten-5"
                  />
                </v-row>
              </template>
            </v-img>
          </v-card>

          <!-- Champ Titre -->
          <v-text-field
            v-model="title"
            label="Titre *"
            placeholder="Ex: Livre de cuisine"
            prepend-inner-icon="mdi-text"
            variant="outlined"
            density="comfortable"
            class="mb-3"
            :rules="rules.title"
            required
          />

          <!-- Champ Description -->
          <v-textarea
            v-model="description"
            label="Description (optionnel)"
            placeholder="Décrivez votre souhait..."
            prepend-inner-icon="mdi-text-box"
            variant="outlined"
            rows="3"
            class="mb-3"
            :rules="rules.description"
          />

          <!-- Champ Prix -->
          <v-text-field
            v-model="price"
            label="Prix estimé (optionnel)"
            placeholder="Ex: 29.99 €"
            prepend-inner-icon="mdi-currency-eur"
            variant="outlined"
            density="comfortable"
            class="mb-3"
            hint="Information indicative uniquement"
            persistent-hint
          />

          <!-- Message d'erreur -->
          <v-alert
            v-if="errorMessage"
            type="error"
            variant="tonal"
            class="mb-3"
          >
            {{ errorMessage }}
          </v-alert>
        </v-form>
      </v-card-text>

      <v-divider />

      <v-card-actions class="pa-4">
        <v-spacer />
        <v-btn
          variant="text"
          @click="handleClose"
          :disabled="isSaving"
        >
          Annuler
        </v-btn>
        <v-btn
          color="primary"
          variant="elevated"
          @click="handleSubmit"
          :loading="isSaving"
          :disabled="!title"
        >
          Ajouter
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

