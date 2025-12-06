<script setup lang="ts">
import {computed, ref} from 'vue'
import type {GroupMember, Invitation} from '@/stores/group'
import {useGroupStore} from '@/stores/group'
import {useAuthStore} from '@/stores/auth'
import {useNotificationStore} from '@/stores/notification'
import AvatarSelector from './AvatarSelector.vue'
import UserAvatar from './UserAvatar.vue'

const props = defineProps<{
  groupId: string
  members: GroupMember[]
  invitations: Invitation[]
  isAdmin: boolean
  isLoading: boolean
}>()

const emit = defineEmits<{
  close: []
  invitationSent: []
}>()

const groupStore = useGroupStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

// Formulaire d'invitation
const inviteEmail = ref('')
const inviteEmailError = ref('')
const isSendingInvite = ref(false)
const selectedAvatarId = ref<string | null>(null)
const invitePseudo = ref('')

// Membres acceptés (ceux qui ont un compte)
const acceptedMembers = computed(() => props.members)

// Invitations en attente (non acceptées)
const pendingInvitations = computed(() =>
  props.invitations.filter(inv => !inv.accepted)
)

// Validation de l'email
function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

// Envoi d'une invitation
async function handleSendInvite() {
  inviteEmailError.value = ''

  if (!inviteEmail.value) {
    inviteEmailError.value = 'Veuillez entrer une adresse email'
    return
  }

  if (!validateEmail(inviteEmail.value)) {
    inviteEmailError.value = 'Adresse email invalide'
    return
  }

  // Vérifier si l'email est déjà membre
  const alreadyMember = props.members.some(m => m.email === inviteEmail.value)
  if (alreadyMember) {
    inviteEmailError.value = 'Cet utilisateur est déjà membre du groupe'
    return
  }

  // Vérifier si une invitation est déjà en attente
  const alreadyInvited = props.invitations.some(
    inv => inv.email === inviteEmail.value && !inv.accepted
  )
  if (alreadyInvited) {
    inviteEmailError.value = 'Une invitation est déjà en attente pour cet email'
    return
  }

  isSendingInvite.value = true

  try {
    const invitation = await groupStore.inviteUser(props.groupId, inviteEmail.value, selectedAvatarId.value, invitePseudo.value || null)

    if (invitation) {
      inviteEmail.value = ''
      selectedAvatarId.value = null
      invitePseudo.value = ''
      emit('invitationSent')
    }
  } catch (err: any) {
    inviteEmailError.value = err.response?.data?.message || 'Erreur lors de l\'envoi de l\'invitation'
  } finally {
    isSendingInvite.value = false
  }
}

/**
 * Génère le lien d'invitation complet avec le domaine actuel
 */
function getInvitationLink(token: string): string {
  const origin = window.location.origin
  return `${origin}/invite/${token}`
}

function copyInvitationLink(link: string) {
  navigator.clipboard.writeText(link)
  notificationStore.success('Lien d\'invitation copié dans le presse-papier')
}
</script>

<template>
  <v-card max-width="800">
    <v-card-title class="d-flex align-center justify-space-between pa-4">
      <div class="text-h6">
        <v-icon class="mr-2">mdi-account-multiple</v-icon>
        Gestion des membres et invitations
      </div>
    </v-card-title>

    <v-divider />

    <v-card-text class="pa-4" style="max-height: 600px; overflow-y: auto">
      <!-- Formulaire d'invitation (Admin uniquement) -->
      <div v-if="isAdmin" class="mb-6">
        <div class="text-subtitle-1 font-weight-bold mb-3">
          <v-icon class="mr-2" color="primary">mdi-email-plus</v-icon>
          Inviter un nouveau membre
        </div>

        <v-form @submit.prevent="handleSendInvite">
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="inviteEmail"
                label="Adresse email"
                placeholder="exemple@email.com"
                prepend-inner-icon="mdi-email"
                :error-messages="inviteEmailError"
                :disabled="isSendingInvite"
                variant="outlined"
                density="comfortable"
                type="email"
              />
            </v-col>
            <v-col cols="12">
              <v-text-field
                v-model="invitePseudo"
                label="Pseudo (optionnel)"
                placeholder="Jean"
                prepend-inner-icon="mdi-account"
                :disabled="isSendingInvite"
                variant="outlined"
                density="comfortable"
                hint="Le nom/pseudo à afficher pour ce membre"
                persistent-hint
              />
            </v-col>
            <v-col cols="12">
              <div class="text-caption text-medium-emphasis mb-2">Avatar du membre</div>
              <AvatarSelector v-model="selectedAvatarId" />
            </v-col>
            <v-col cols="12">
              <v-btn
                type="submit"
                color="primary"
                :loading="isSendingInvite"
                :disabled="isSendingInvite"
                block
                size="large"
                prepend-icon="mdi-send"
              >
                Envoyer l'invitation
              </v-btn>
            </v-col>
          </v-row>
        </v-form>

        <v-divider class="my-4" />
      </div>

      <!-- Invitations en attente -->
      <div v-if="isAdmin && pendingInvitations.length > 0" class="mb-6">
        <div class="text-subtitle-1 font-weight-bold mb-3">
          <v-icon class="mr-2" color="warning">mdi-clock-outline</v-icon>
          Invitations en attente ({{ pendingInvitations.length }})
        </div>

        <v-list lines="two" class="bg-grey-lighten-5 rounded">
          <v-list-item
            v-for="invitation in pendingInvitations"
            :key="invitation.id"
            class="mb-2 bg-white rounded"
          >
            <template v-slot:prepend>
              <UserAvatar :avatar-id="invitation.avatarId" :size="40" />
            </template>

            <v-list-item-title>
              {{ invitation.pseudo || invitation.email }}
            </v-list-item-title>

            <v-list-item-subtitle>
              <div v-if="invitation.pseudo">{{ invitation.email }}</div>
              <div>Invitation envoyée le {{ new Date(invitation.createdAt).toLocaleDateString('fr-FR') }}</div>
              <div class="text-caption text-primary mt-1">
                <v-icon size="x-small" class="mr-1">mdi-link</v-icon>
                {{ getInvitationLink(invitation.token) }}
              </div>
            </v-list-item-subtitle>

            <template v-slot:append>
              <v-tooltip text="Copier le lien d'invitation" location="left">
                <template v-slot:activator="{ props: tooltipProps }">
                  <v-btn
                    v-if="invitation.token"
                    icon
                    variant="text"
                    size="small"
                    v-bind="tooltipProps"
                    @click="copyInvitationLink(getInvitationLink(invitation.token))"
                  >
                    <v-icon>mdi-content-copy</v-icon>
                  </v-btn>
                </template>
              </v-tooltip>
            </template>
          </v-list-item>
        </v-list>

        <v-divider class="my-4" />
      </div>

      <!-- Membres acceptés -->
      <div>
        <div class="text-subtitle-1 font-weight-bold mb-3">
          <v-icon class="mr-2" color="success">mdi-check-circle</v-icon>
          Membres actifs ({{ acceptedMembers.length }})
        </div>

        <v-progress-linear
          v-if="isLoading"
          indeterminate
          color="primary"
          class="mb-4"
        />

        <div
          v-else-if="acceptedMembers.length === 0"
          class="text-center py-8"
        >
          <v-icon size="64" color="grey-lighten-1">mdi-account-outline</v-icon>
          <div class="text-h6 text-medium-emphasis mt-4">Aucun membre actif</div>
        </div>

        <v-list v-else lines="two">
          <v-list-item
            v-for="member in acceptedMembers"
            :key="member.id"
            class="mb-2 rounded border"
          >
            <template v-slot:prepend>
              <UserAvatar :avatar-id="member.avatarId" :size="40" />
            </template>

            <v-list-item-title>
              {{ authStore.getMemberDisplayName(member) }}
            </v-list-item-title>

            <v-list-item-subtitle>
              <div v-if="member.pseudo">{{ member.email }}</div>
              <div>Membre depuis {{ new Date(member.createdAt).toLocaleDateString('fr-FR') }}</div>
            </v-list-item-subtitle>

            <template v-slot:append>
              <v-chip
                v-if="member.isAdmin"
                color="primary"
                size="small"
                variant="flat"
              >
                Administrateur
              </v-chip>
              <v-chip
                v-else
                color="success"
                size="small"
                variant="flat"
              >
                Membre
              </v-chip>
            </template>
          </v-list-item>
        </v-list>
      </div>
    </v-card-text>

    <v-divider />

    <v-card-actions class="pa-4">
      <v-spacer />
      <v-btn color="primary" variant="text" @click="$emit('close')">
        Fermer
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<style scoped>
:deep(.v-list-item__prepend) {
  margin-right: 16px !important;
}

:deep(.v-list-item) {
  padding: 12px 16px !important;
}
</style>

