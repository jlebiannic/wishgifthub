<script setup lang="ts">
withDefaults(defineProps<{disabled?: boolean}>(), {disabled: false})
const provisional = defineModel<boolean>({default: false})
</script>

<template>
  <v-switch v-model="provisional" :disabled="disabled" color="primary"
            label="Souhait provisoire (non visible par les autres membres)"
            :hint="disabled
              ? 'Un souhait réservé ou ayant des participants ne peut pas être rendu provisoire.'
              : 'Visible uniquement par vous et, pour un enfant, par son parent gestionnaire. Décochez pour le publier.'"
            persistent-hint class="mb-4" />
</template>
