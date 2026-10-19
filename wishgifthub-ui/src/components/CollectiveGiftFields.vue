<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {DEFAULT_COLLECTIVE_GIFT_THRESHOLD, parseEuroCents} from '@/utils/price'

const props = withDefaults(defineProps<{
  price?: string
  threshold?: number
  active?: boolean
}>(), {price: '', threshold: DEFAULT_COLLECTIVE_GIFT_THRESHOLD, active: true})

const collective = defineModel<boolean>('collective', {default: false})
const maxParticipants = defineModel<number>('maxParticipants', {default: 2})
const dismissed = ref(false)
const priceCents = computed(() => parseEuroCents(props.price))
const suggestCollective = computed(() => props.active && !collective.value && !dismissed.value
  && priceCents.value !== null && priceCents.value > Math.round(props.threshold * 100))
const formattedThreshold = computed(() => new Intl.NumberFormat('fr-FR', {
  style: 'currency', currency: 'EUR'
}).format(props.threshold))

watch(() => [priceCents.value, props.threshold, props.active], () => {
  dismissed.value = false
})

function acceptSuggestion() {
  collective.value = true
  dismissed.value = true
}
const validCapacity = (value: unknown) =>
  (Number.isInteger(Number(value)) && Number(value) >= 2 && Number(value) <= 2147483647)
  || 'Saisissez un nombre entier de participants, au minimum 2'

function step(delta: number) {
  const current = Number(maxParticipants.value)
  maxParticipants.value = Math.min(2147483647, Math.max(2, (Number.isFinite(current) ? current : 2) + delta))
}
</script>

<template>
  <v-alert v-if="suggestCollective" type="info" variant="tonal" class="mb-4">
    <p>Ce souhait dépasse {{ formattedThreshold }}. Souhaitez-vous en faire un cadeau commun,
      réservé par plusieurs membres du groupe ?</p>
    <div class="d-flex flex-wrap ga-2 mt-3">
      <v-btn size="small" color="primary" @click="acceptSuggestion">Oui, cadeau commun</v-btn>
      <v-btn size="small" variant="text" @click="dismissed = true">Non, garder individuel</v-btn>
    </div>
  </v-alert>
  <v-switch
    v-model="collective"
    label="Autoriser un cadeau commun"
    color="primary"
    hint="Permettre à plusieurs personnes de se regrouper pour offrir ce cadeau."
    persistent-hint
    class="mb-4"
  />
  <v-text-field
    v-if="collective"
    v-model.number="maxParticipants"
    label="Nombre maximum de participants"
    type="number"
    inputmode="numeric"
    min="2"
    max="2147483647"
    step="1"
    :rules="[validCapacity]"
    variant="outlined"
    class="mb-3"
  >
    <template #prepend>
      <v-btn icon="mdi-minus" aria-label="Diminuer le nombre de participants"
             :disabled="Number(maxParticipants) <= 2" @click="step(-1)" />
    </template>
    <template #append>
      <v-btn icon="mdi-plus" aria-label="Augmenter le nombre de participants"
             :disabled="Number(maxParticipants) >= 2147483647" @click="step(1)" />
    </template>
  </v-text-field>
</template>
