<script setup lang="ts">
import {ref, watch} from 'vue'
import {isAxiosError} from 'axios'
import type {GroupResponse} from '@/generated/api/wish/data-contracts'
import {useGroupStore} from '@/stores/group'
import {useNotificationStore} from '@/stores/notification'
import {DEFAULT_COLLECTIVE_GIFT_THRESHOLD, parseEuroCents, thresholdRule} from '@/utils/price'

const props = defineProps<{modelValue: boolean, group: GroupResponse}>()
const emit = defineEmits<{'update:modelValue': [value: boolean]}>()
const groups = useGroupStore()
const notifications = useNotificationStore()
const threshold = ref('')
const saving = ref(false)

watch(() => [props.modelValue, props.group] as const, ([open]) => {
  if (open) threshold.value = String(props.group.collectiveGiftThreshold ?? DEFAULT_COLLECTIVE_GIFT_THRESHOLD)
}, {immediate: true})

async function save() {
  if (saving.value) return
  const cents = parseEuroCents(threshold.value)
  if (cents === null || thresholdRule(threshold.value) !== true) {
    notifications.showError(String(thresholdRule(threshold.value)))
    return
  }
  if (props.group.type !== 'noël') {
    notifications.showError('Le type de ce groupe ne permet pas de modifier ses paramètres')
    return
  }
  saving.value = true
  try {
    await groups.updateGroup(props.group.id, {
      name: props.group.name,
      type: props.group.type,
      collectiveGiftThreshold: cents / 100
    })
    notifications.showSuccess('Paramètres du groupe enregistrés')
    emit('update:modelValue', false)
  } catch (error: unknown) {
    notifications.showError(isAxiosError<{message?: string}>(error)
      ? error.response?.data?.message || 'Impossible d’enregistrer les paramètres du groupe'
      : 'Impossible d’enregistrer les paramètres du groupe')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <v-dialog :model-value="modelValue" persistent max-width="600">
    <v-card>
      <v-card-title class="pa-4 bg-primary">Paramètres du groupe</v-card-title>
      <v-card-text class="pa-4">
        <v-form :disabled="saving" @submit.prevent="save">
          <v-text-field v-model="threshold" label="Seuil de suggestion d’un cadeau commun (€)"
                        inputmode="decimal" :rules="[thresholdRule]" variant="outlined"
                        hint="Au-dessus de ce montant, un cadeau commun est proposé, jamais imposé."
                        persistent-hint />
        </v-form>
      </v-card-text>
      <v-card-actions class="pa-4">
        <v-spacer />
        <v-btn :disabled="saving" @click="emit('update:modelValue', false)">Annuler</v-btn>
        <v-btn color="primary" :loading="saving" :disabled="saving" @click="save">Enregistrer</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
