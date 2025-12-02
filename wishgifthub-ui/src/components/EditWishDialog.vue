<script setup lang="ts">
import {ref, watch} from 'vue'
import {useWishStore} from '@/stores/wish'
import {useNotificationStore} from '@/stores/notification'
import {getApiClient} from '@/api/client'
import type {WishResponse} from '@/generated/api/wish/data-contracts'

const props = defineProps<{
  groupId: string
  wish: WishResponse | null
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  wishUpdated: []
}>()

const wishStore = useWishStore()
const notificationStore = useNotificationStore()

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
const imageLoadError = ref(false)

// Charger les données du souhait quand il change
watch(() => props.wish, (newWish) => {
  if (newWish) {
    title.value = newWish.giftName || ''
    description.value = newWish.description || ''
    url.value = newWish.url?.toString() || ''
    imageUrl.value = newWish.imageUrl?.toString() || ''
    price.value = newWish.price || ''
  }
}, { immediate: true })

// Fonction de validation d'URL
function isValidUrl(urlString: string): boolean {
  if (!urlString) return true
  try {
    const url = new URL(urlString)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

// Fonction de validation d'URL d'image
function isValidImageUrl(urlString: string): boolean {
  if (!urlString) return true
  if (!isValidUrl(urlString)) return false

  const validExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp', '.ico']

  try {
    const url = new URL(urlString)
    const path = url.pathname.toLowerCase()
    return validExtensions.some(ext => path.endsWith(ext))
  } catch {
    return false
  }
}

// Fonction pour nettoyer une URL
function cleanUrl(urlString: string): string {
  if (!urlString || !urlString.trim()) return urlString

  try {
    const url = new URL(urlString)

    const trackingParams = [
      'utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content',
      'gclid', 'gclsrc', 'dclid',
      'gad_source', 'gad_campaignid',
      'gbraid', 'wbraid',
      'fbclid', 'fb_action_ids', 'fb_action_types', 'fb_source', 'fb_ref',
      'ref', 'ref_', 'pf_rd_r', 'pf_rd_p', 'pf_rd_m', 'pf_rd_s', 'pf_rd_t', 'pf_rd_i',
      'pd_rd_r', 'pd_rd_w', 'pd_rd_wg',
      '_encoding', 'psc', 'refRID', 'qid', 'sr',
      'loopcd',
      'mc_cid', 'mc_eid', '_hsenc', '_hsmi', 'mkt_tok',
      'msclkid', 'igshid'
    ]

    const cleanedParams = new URLSearchParams()
    url.searchParams.forEach((value, key) => {
      if (!trackingParams.includes(key) && !key.startsWith('utm_')) {
        cleanedParams.append(key, value)
      }
    })

    url.search = cleanedParams.toString()
    return url.toString()
  } catch {
    return urlString
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

  try {
    const apiClient = getApiClient()
    const response = await apiClient.extractMetadata({ url: url.value })
    const metadata = response.data

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

    if (metadata.error) {
      console.warn('Avertissement lors de l\'extraction:', metadata.error)
    }
  } catch (error: any) {
    console.error('Erreur lors de la récupération des métadonnées:', error)
  } finally {
    isLoadingMetadata.value = false
  }
}

// Watch l'URL avec debounce
let debounceTimer: number | null = null
watch(url, () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  if (url.value) {
    debounceTimer = setTimeout(() => {
      fetchMetadataFromUrl()
    }, 1000)
  }
})

async function handleSubmit() {
  if (!props.wish) return

  if (form.value) {
    const { valid } = await form.value.validate()
    if (!valid) {
      notificationStore.showError('Veuillez corriger les erreurs dans le formulaire')
      return
    }
  }

  if (!title.value) {
    notificationStore.showError('Le titre est requis')
    return
  }

  isSaving.value = true

  try {
    const cleanedUrl = url.value ? cleanUrl(url.value) : null
    const cleanedImageUrl = imageUrl.value ? cleanUrl(imageUrl.value) : null

    await wishStore.updateWish(props.groupId, props.wish.id!, {
      giftName: title.value,
      description: description.value || null,
      url: cleanedUrl,
      imageUrl: cleanedImageUrl,
      price: price.value || null
    })

    notificationStore.showSuccess('Souhait modifié avec succès !')
    emit('update:modelValue', false)
    emit('wishUpdated')
  } catch (error: any) {
    notificationStore.showError(error.response?.data?.message || 'Erreur lors de la modification du souhait')
  } finally {
    isSaving.value = false
  }
}

function handleClose() {
  emit('update:modelValue', false)
}

function handleUrlBlur() {
  if (url.value) {
    const cleaned = cleanUrl(url.value)
    if (cleaned !== url.value) {
      url.value = cleaned
    }
  }
}

function handleImageUrlBlur() {
  if (imageUrl.value) {
    const cleaned = cleanUrl(imageUrl.value)
    if (cleaned !== imageUrl.value) {
      imageUrl.value = cleaned
    }
  }
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
        <v-icon class="mr-2">mdi-pencil</v-icon>
        Modifier le souhait
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
            @blur="handleUrlBlur"
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
            @blur="handleImageUrlBlur"
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
                <v-row class="fill-height ma-0" align="center" justify="center">
                  <v-progress-circular indeterminate color="grey-lighten-5" />
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
            :rules="rules.price"
            counter="100"
          />
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
        >
          Enregistrer
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

