<script setup lang="ts">
import {ref, watch} from 'vue'
import type {AxiosError} from 'axios'
import {useChildrenStore} from '@/stores/children'
import {useAuthStore} from '@/stores/auth'
import {useConfirmStore} from '@/stores/confirm'
import {useNotificationStore} from '@/stores/notification'
import type {UserResponse} from '@/generated/api/wish/data-contracts'
import AvatarSelector from './AvatarSelector.vue'
import UserAvatar from './UserAvatar.vue'

const props = defineProps<{modelValue: boolean, groupId: string, refreshing?: boolean}>()
const emit = defineEmits<{'update:modelValue': [value: boolean], changed: []}>()
const childrenStore = useChildrenStore()
const authStore = useAuthStore()
const confirmStore = useConfirmStore()
const notifications = useNotificationStore()
const editingId = ref<string | null>(null)
const pseudo = ref('')
const avatarId = ref<string | null>(null)
const avatarKey = ref(0)
const busy = ref(false)
const form = ref<{validate: () => Promise<{valid: boolean}>, resetValidation: () => void} | null>(null)
const pseudoRules = [
  (value: string) => !!value?.trim() || 'Le pseudo est requis',
  (value: string) => value.length <= 100 || 'Le pseudo ne peut pas dépasser 100 caractères'
]

function edit(child?: UserResponse) {
  editingId.value = child?.id ?? null
  pseudo.value = child?.pseudo ?? ''
  avatarId.value = child?.avatarId ?? null
  avatarKey.value++
  form.value?.resetValidation()
}

watch(() => [props.modelValue, props.groupId], () => edit())

async function save() {
  if (busy.value || props.refreshing || !pseudo.value.trim() || pseudo.value.length > 100) return
  busy.value = true
  try {
    if (form.value && !(await form.value.validate()).valid) return
    await childrenStore.saveChild(props.groupId, editingId.value, {
      pseudo: pseudo.value.trim(), avatarId: avatarId.value
    })
    notifications.showSuccess(editingId.value ? 'Profil enfant modifié' : 'Enfant ajouté au groupe')
    edit()
    emit('changed')
  } catch (error) {
    const apiError = error as AxiosError<{message: string}>
    notifications.showError(apiError.response?.data?.message || 'Impossible d’enregistrer le profil enfant')
  } finally {
    busy.value = false
  }
}

async function remove(child: UserResponse) {
  if (busy.value || props.refreshing) return
  busy.value = true
  try {
    const confirmed = await confirmStore.confirm({
      title: 'Supprimer le profil enfant',
      message: `Supprimer ${authStore.getMemberDisplayName(child)} définitivement ? Son profil, tous ses souhaits et ses appartenances à tous les groupes seront supprimés, pas seulement dans ce groupe. Cette action est irréversible.`,
      confirmText: 'Supprimer définitivement',
      confirmColor: 'error',
      icon: 'mdi-delete-alert',
      iconColor: 'error'
    })
    if (!confirmed) return
    await childrenStore.deleteChild(props.groupId, child.id)
    if (editingId.value === child.id) edit()
    notifications.showSuccess('Profil enfant supprimé')
    emit('changed')
  } catch (error) {
    const apiError = error as AxiosError<{message: string}>
    notifications.showError(apiError.response?.data?.message || 'Impossible de supprimer le profil enfant')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <v-dialog :model-value="modelValue" max-width="650" persistent>
    <v-card>
      <v-card-title>Mes enfants dans ce groupe</v-card-title>
      <v-card-text>
        <p class="mb-4">Créez autant de profils enfants que nécessaire, sans compte ni changement de connexion.</p>
        <div v-for="child in childrenStore.children" :key="child.id" class="d-flex align-center ga-2 mb-3">
          <UserAvatar :avatar-id="child.avatarId" :size="40" />
          <span class="flex-grow-1">{{ authStore.getMemberDisplayName(child) }}</span>
          <v-btn icon="mdi-pencil" variant="text" :aria-label="`Modifier ${child.pseudo}`"
                 :disabled="busy || refreshing" @click="edit(child)" />
          <v-btn icon="mdi-delete" variant="text" color="error" :aria-label="`Supprimer ${child.pseudo}`"
                 :disabled="busy || refreshing" @click="remove(child)" />
        </div>
        <v-divider class="my-4" />
        <h3 class="mb-3">{{ editingId ? 'Modifier le profil enfant' : 'Ajouter un enfant' }}</h3>
        <v-form ref="form" @submit.prevent="save">
          <fieldset :disabled="busy || refreshing" class="child-fields">
            <v-text-field v-model="pseudo" label="Pseudo de l’enfant" :rules="pseudoRules"
                          maxlength="100" counter="100" :disabled="busy || refreshing" required />
            <AvatarSelector :key="avatarKey" v-model="avatarId" />
          </fieldset>
          <div class="d-flex ga-2 mt-4">
            <v-btn type="submit" color="primary" :loading="busy"
                   :disabled="busy || refreshing || !pseudo.trim() || pseudo.length > 100">
              {{ editingId ? 'Enregistrer' : 'Créer le profil enfant' }}
            </v-btn>
            <v-btn v-if="editingId" :disabled="busy || refreshing" @click="edit()">Nouvel enfant</v-btn>
          </div>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn :disabled="busy || refreshing" @click="emit('update:modelValue', false)">Fermer</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.child-fields {
  border: 0;
  min-width: 0;
}
</style>
