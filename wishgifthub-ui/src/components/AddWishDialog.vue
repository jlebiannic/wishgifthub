<script setup lang="ts">
import {ref, watch} from 'vue'
import {useWishStore} from '@/stores/wish'
import {getApiClient} from '@/api/client'

const props = defineProps<{
  groupId: string
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  wishAdded: []
}>()

const wishStore = useWishStore()

// Référence au formulaire
const form = ref<any>(null)

// Formulaire
const url = ref('')
const imageUrl = ref('')
const title = ref('')
const description = ref('')
const price = ref('')

const isLoadingMetadata = ref(false)
const isSaving = ref(false)
const errorMessage = ref('')
const imageLoadError = ref(false)

// Fonction de validation d'URL
function isValidUrl(urlString: string): boolean {
  if (!urlString) return true // URL vide est valide (optionnel)
  try {
    const url = new URL(urlString)
    // Vérifier que le protocole est http ou https
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

// Fonction de validation d'URL d'image
function isValidImageUrl(urlString: string): boolean {
  if (!urlString) return true // URL vide est valide (optionnel)

  // Vérifier d'abord que c'est une URL valide
  if (!isValidUrl(urlString)) return false

  // Extensions d'images valides
  const validExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp', '.ico']

  try {
    const url = new URL(urlString)
    const path = url.pathname.toLowerCase()

    // Vérifier que le chemin se termine par une extension d'image valide
    return validExtensions.some(ext => path.endsWith(ext))
  } catch {
    return false
  }
}

// Règles de validation
const rules = {
  title: [
    (v: string) => !!v || 'Le titre est requis',
    (v: string) => (v && v.length >= 1 && v.length <= 255) || 'Le titre doit contenir entre 1 et 255 caractères'
  ],
  description: [
    (v: string) => !v || v.length <= 10000 || 'La description ne peut pas dépasser 10 000 caractères'
  ],
  url: [
    (v: string) => !v || v.length <= 2048 || 'L\'URL ne peut pas dépasser 2048 caractères',
    (v: string) => isValidUrl(v) || 'L\'URL n\'est pas valide (doit commencer par http:// ou https://)'
  ],
  imageUrl: [
    (v: string) => !v || v.length <= 2048 || 'L\'URL de l\'image ne peut pas dépasser 2048 caractères',
    (v: string) => isValidImageUrl(v) || 'L\'URL doit pointer vers une image valide (extensions acceptées : jpg, jpeg, png, gif, webp, svg, bmp, ico)'
  ],
  price: [
    (v: string) => !v || v.length <= 100 || 'Le prix ne peut pas dépasser 100 caractères'
  ]
}

// Fonction pour extraire les métadonnées d'une URL
async function fetchMetadataFromUrl() {
  if (!url.value) return

  isLoadingMetadata.value = true
  errorMessage.value = ''

  try {
    // Utiliser le client API généré avec authentification automatique
    const apiClient = getApiClient()
    const response = await apiClient.extractMetadata({ url: url.value })

    const metadata = response.data

    // Pré-remplir les champs avec les métadonnées extraites
    // Ne pas écraser si l'utilisateur a déjà saisi quelque chose
    if (metadata.title && !title.value) {
      title.value = metadata.title
    }

    if (metadata.description && !description.value) {
      description.value = metadata.description
    }

    if (metadata.image && !imageUrl.value) {
      imageUrl.value = metadata.image
    }

    if (metadata.price && !price.value) {
      price.value = metadata.price
    }

    // Afficher un message si une erreur est présente mais continuer
    if (metadata.error) {
      console.warn('Avertissement lors de l\'extraction:', metadata.error)
    }
  } catch (error: any) {
    console.error('Erreur lors de la récupération des métadonnées:', error)
    // Ne pas bloquer l'utilisateur, il peut saisir manuellement
  } finally {
    isLoadingMetadata.value = false
  }
}

// Watch l'URL pour déclencher la récupération des métadonnées avec debounce
let debounceTimer: number | null = null
watch(url, () => {
  // Annuler le timer précédent
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }

  // Attendre 1 seconde après la dernière frappe avant de déclencher l'extraction
  if (url.value) {
    debounceTimer = setTimeout(() => {
      fetchMetadataFromUrl()
    }, 1000)
  }
})

async function handleSubmit() {
  // Valider le formulaire
  if (form.value) {
    const { valid } = await form.value.validate()
    if (!valid) {
      errorMessage.value = 'Veuillez corriger les erreurs dans le formulaire'
      return
    }
  }

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
      url: url.value || null,
      imageUrl: imageUrl.value || null,
      price: price.value || null
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
  imageLoadError.value = false
  if (form.value) {
    form.value.resetValidation()
  }
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
        <v-form ref="form" @submit.prevent="handleSubmit">
          <!-- Champ URL -->
          <v-text-field
            v-model="url"
            label="URL du produit (optionnel)"
            placeholder="https://example.com/produit"
            prepend-inner-icon="mdi-link"
            variant="outlined"
            density="comfortable"
            class="mb-3"
            :hint="`Si renseignée, l'URL peut pré-remplir les autres champs (${url.length}/2048)`"
            persistent-hint
            :loading="isLoadingMetadata"
            :rules="rules.url"
            counter="2048"
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
            :rules="rules.imageUrl"
            counter="2048"
            @update:model-value="imageLoadError = false"
          />

          <!-- Message d'erreur si l'image ne peut pas être chargée -->
          <v-alert
            v-if="imageLoadError"
            type="warning"
            variant="tonal"
            class="mb-3"
            density="compact"
          >
            L'image ne peut pas être chargée. Vérifiez que l'URL est correcte et accessible.
          </v-alert>

          <!-- Prévisualisation de l'image -->
          <v-card v-if="imageUrl && !imageLoadError" variant="outlined" class="mb-3">
            <v-img
              :src="imageUrl"
              height="200"
              cover
              @error="imageLoadError = true"
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
            counter="10000"
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
            :rules="rules.price"
            counter="100"
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

