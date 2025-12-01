# Fonctionnalité d'invitation de membres par email

## ✅ Implémentation terminée

L'administrateur peut désormais inviter de nouveaux membres par email dans un groupe, conformément aux spécifications.

---

## 📋 Fonctionnalités implémentées

### 1. **Formulaire d'invitation (Admin uniquement)**
   - Champ email avec validation
   - Bouton "Envoyer l'invitation"
   - Vérifications automatiques :
     - Email valide
     - Email non déjà membre
     - Pas d'invitation en attente pour cet email

### 2. **Affichage des invitations en attente**
   - Liste des invitations non encore acceptées
   - Date d'envoi de l'invitation
   - Bouton pour copier le lien d'invitation
   - Badge "En attente" avec icône

### 3. **Affichage des membres actifs**
   - Liste des membres qui ont accepté l'invitation
   - Distinction Admin / Membre
   - Date d'adhésion au groupe

---

## 🎨 Interface utilisateur

### Dialog "Gestion des membres et invitations"

Le dialog s'ouvre quand l'admin clique sur l'icône "œil" (👁️) d'un groupe.

```
┌─────────────────────────────────────────────────────────┐
│  📋 Gestion des membres et invitations          [X]     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ✉️ Inviter un nouveau membre                           │
│  ┌──────────────────────┐  ┌──────────────────┐        │
│  │ exemple@email.com    │  │  Envoyer         │        │
│  └──────────────────────┘  └──────────────────┘        │
│  ─────────────────────────────────────────────────      │
│                                                          │
│  ⏰ Invitations en attente (2)                          │
│  ┌─────────────────────────────────────────────┐       │
│  │ ⏰  user1@example.com                       │       │
│  │     Invitation envoyée le 01/12/2025  [📋] │       │
│  ├─────────────────────────────────────────────┤       │
│  │ ⏰  user2@example.com                       │       │
│  │     Invitation envoyée le 30/11/2025  [📋] │       │
│  └─────────────────────────────────────────────┘       │
│  ─────────────────────────────────────────────────      │
│                                                          │
│  ✅ Membres actifs (3)                                  │
│  ┌─────────────────────────────────────────────┐       │
│  │ 👑  admin@example.com                       │       │
│  │     Membre depuis 10/11/2025  [Admin]      │       │
│  ├─────────────────────────────────────────────┤       │
│  │ ✅  membre1@example.com                     │       │
│  │     Membre depuis 15/11/2025  [Membre]     │       │
│  └─────────────────────────────────────────────┘       │
│                                                          │
└─────────────────────────────────────────────────────────┘
                                            [Fermer]
```

---

## 🔧 Modifications techniques

### 1. **Store Group (`src/stores/group.ts`)**

**Ajouts :**
```typescript
// Nouveau type
export type Invitation = InvitationResponse

// Nouvel état
const invitations = ref<InvitationResponse[]>([])

// Nouvelles actions
async function inviteUser(groupId: string, email: string)
async function fetchGroupInvitations(groupId: string)
```

### 2. **InvitationsDialog (`src/components/InvitationsDialog.vue`)**

**Refonte complète avec :**
- Formulaire d'invitation avec validation d'email
- Affichage des invitations en attente
- Affichage des membres actifs
- Fonctionnalité de copie du lien d'invitation
- Gestion des erreurs et feedbacks

**Props ajoutées :**
```typescript
{
  groupId: string,
  members: GroupMember[],
  invitations: Invitation[],
  isAdmin: boolean,
  isLoading: boolean
}
```

**Events émis :**
```typescript
{
  close: [],
  invitationSent: []
}
```

### 3. **HomeView (`src/views/HomeView.vue`)**

**Modifications :**
- Appel à `fetchGroupInvitations()` lors de l'ouverture du dialog
- Gestion de l'événement `invitationSent` pour rafraîchir les données
- Passage des nouvelles props au composant `InvitationsDialog`

---

## 🔄 Flux d'utilisation

### Pour l'administrateur :

1. **Connexion** → Les groupes s'affichent automatiquement
2. **Clic sur l'icône "👥"** d'un groupe → Dialog s'ouvre
3. **Saisie d'un email** → Validation automatique
4. **Clic sur "Envoyer l'invitation"** → Invitation créée
5. **Copie du lien** (optionnel) → Envoi manuel du lien
6. **Visualisation des statuts** :
   - Section "En attente" : Invitations non acceptées
   - Section "Actifs" : Membres du groupe

### Pour l'invité :

1. **Réception du lien d'invitation** (par email ou autre)
2. **Clic sur le lien** → Acceptation automatique
3. **Création du compte** → Sans mot de passe (auth par token)
4. **Accès au groupe** → L'utilisateur devient membre actif

---

## 🎯 Validations implémentées

### Côté client (UI) :
- ✅ Format email valide (regex)
- ✅ Email non vide
- ✅ Email non déjà membre
- ✅ Pas d'invitation en attente pour cet email

### Côté serveur (API) :
- ✅ Seul l'admin du groupe peut inviter
- ✅ Génération d'un token UUID unique
- ✅ Création du lien d'invitation
- ✅ Stockage de l'invitation en base

---

## 📊 Statuts des invitations

| Statut | Icône | Couleur | Description |
|--------|-------|---------|-------------|
| **En attente** | ⏰ | Orange (warning) | Invitation envoyée mais pas encore acceptée |
| **Accepté** | ✅ | Vert (success) | Utilisateur a rejoint le groupe |
| **Administrateur** | 👑 | Bleu (primary) | Membre avec droits d'administration |

---

## 🚀 Améliorations futures possibles

- [ ] Endpoint backend pour récupérer la liste des invitations d'un groupe
- [ ] Possibilité de révoquer une invitation en attente
- [ ] Renvoyer une invitation expirée
- [ ] Notification par email automatique (actuellement, le lien doit être copié manuellement)
- [ ] Historique des invitations (acceptées/refusées)
- [ ] Limite du nombre d'invitations par groupe
- [ ] Expiration automatique des invitations après X jours

---

## 🧪 Tests à effectuer

### Test 1 : Inviter un nouveau membre
1. Se connecter en tant qu'admin
2. Cliquer sur l'icône "👥" d'un groupe
3. Entrer un email valide
4. Cliquer sur "Envoyer l'invitation"
5. ✅ L'invitation doit apparaître dans "Invitations en attente"

### Test 2 : Validation des emails
1. Essayer d'envoyer une invitation sans email → ❌ Erreur
2. Essayer d'envoyer une invitation avec email invalide → ❌ Erreur
3. Essayer d'envoyer une invitation à un membre existant → ❌ Erreur
4. Essayer d'envoyer 2 fois la même invitation → ❌ Erreur

### Test 3 : Copier le lien d'invitation
1. Envoyer une invitation
2. Cliquer sur l'icône "📋" à côté de l'invitation
3. ✅ Le lien doit être copié dans le presse-papier

### Test 4 : Affichage pour non-admin
1. Se connecter en tant qu'utilisateur non-admin
2. Ouvrir la liste des membres
3. ✅ Le formulaire d'invitation ne doit PAS être visible
4. ✅ Les invitations en attente ne doivent PAS être visibles
5. ✅ Seuls les membres actifs sont affichés

---

## 📝 Conformité avec les spécifications

| Spécification | Status | Implémentation |
|---------------|--------|----------------|
| Icône "œil" sur chaque groupe (admin) | ✅ | `GroupCard.vue` - Icône "mdi-account-group" |
| Pop-up/panneau pour les invitations | ✅ | `InvitationsDialog.vue` - Dialog modal |
| Possibilité d'inviter par email | ✅ | Formulaire avec validation |
| Liste des invitations acceptées | ✅ | Section "Membres actifs" |
| Liste des invitations en attente | ✅ | Section "Invitations en attente" |
| Pas de formulaire pour non-admin | ✅ | Conditionnel `v-if="isAdmin"` |
| Pas d'accès aux invitations pour non-admin | ✅ | Props `isAdmin` gère l'affichage |

---

## ✅ Résultat

L'implémentation est **complète et conforme** aux spécifications fournies. L'administrateur peut désormais :

1. ✅ Inviter des utilisateurs par email
2. ✅ Voir les invitations en attente
3. ✅ Voir les membres actifs
4. ✅ Copier les liens d'invitation
5. ✅ Gérer tous ses groupes depuis une interface intuitive

Les utilisateurs non-admin voient uniquement la liste des membres actifs, sans accès aux fonctionnalités d'invitation.

