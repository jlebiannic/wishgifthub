<script setup lang="ts">
const collective = defineModel<boolean>('collective', {default: false})
const maxParticipants = defineModel<number>('maxParticipants', {default: 2})
const validCapacity = (value: unknown) =>
  (Number.isInteger(Number(value)) && Number(value) >= 2 && Number(value) <= 2147483647)
  || 'Saisissez un nombre entier de participants, au minimum 2'

function step(delta: number) {
  const current = Number(maxParticipants.value)
  maxParticipants.value = Math.min(2147483647, Math.max(2, (Number.isFinite(current) ? current : 2) + delta))
}
</script>

<template>
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
