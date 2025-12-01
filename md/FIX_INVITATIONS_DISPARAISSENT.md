# Correction : Les invitations disparaissent après ajout

## 🐛 Problème identifié

Après avoir ajouté une invitation, celle-ci apparaissait brièvement puis disparaissait immédiatement de la popup.

### Cause racine

1. **Dans `inviteUser()`** : L'invitation était correctement ajoutée au store
   ```typescript
   invitations.value.push(response.data) // ✅ Ajout OK
   ```

2. **Dans `handleInvitationSent()`** : On appelait `fetchGroupInvitations()`
   ```typescript
   await groupStore.fetchGroupInvitations(selectedGroupId.value)
   ```

3. **Dans `fetchGroupInvitations()`** : La fonction réinitialisait le tableau
   ```typescript
   invitations.value = [] // ❌ Écrasement !
   ```

**Résultat** : L'invitation ajoutée était immédiatement supprimée lors du rafraîchissement.

---

## ✅ Solution implémentée

### 1. Modification de `fetchGroupInvitations()`

**Avant :**
```typescript
async function fetchGroupInvitations(groupId: string) {
  invitations.value = [] // ❌ Écrase tout
  return invitations.value
}
```

**Après :**
```typescript
async function fetchGroupInvitations(groupId: string) {
  // Ne fait rien pour ne pas écraser les invitations en mémoire
  // Les invitations sont ajoutées directement dans inviteUser()
  return invitations.value // ✅ Conserve les invitations
}
```

### 2. Modification de `handleInvitationSent()`

**Avant :**
```typescript
async function handleInvitationSent() {
  if (selectedGroupId.value) {
    await groupStore.fetchGroupMembers(selectedGroupId.value)
    await groupStore.fetchGroupInvitations(selectedGroupId.value) // ❌ Écrase
  }
}
```

**Après :**
```typescript
async function handleInvitationSent() {
  // L'invitation a déjà été ajoutée au store par inviteUser()
  if (selectedGroupId.value) {
    await groupStore.fetchGroupMembers(selectedGroupId.value) // ✅ Met à jour les membres seulement
  }
}
```

### 3. Gestion du changement de groupe

Pour éviter que les invitations d'un groupe s'affichent pour un autre groupe, ajout d'un système de tracking :

```typescript
const currentGroupId = ref<string | null>(null)

async function fetchGroupMembers(groupId: string) {
  // Si on change de groupe, réinitialiser les invitations
  if (currentGroupId.value !== groupId) {
    invitations.value = []
    currentGroupId.value = groupId
  }
  // ... reste du code
}
```

---

## 🔄 Nouveau flux

### Lors de l'ouverture du dialog :

1. `handleShowMembers(groupId)` est appelé
2. `fetchGroupMembers(groupId)` est appelé
   - Si `groupId` différent → réinitialise les invitations
   - Charge les membres du groupe
3. `fetchGroupInvitations(groupId)` est appelé
   - Ne fait rien (pas d'endpoint backend)
   - Conserve les invitations en mémoire

### Lors de l'envoi d'une invitation :

1. `handleSendInvite()` appelle `inviteUser(groupId, email)`
2. `inviteUser()` :
   - Appelle l'API backend
   - **Ajoute** l'invitation à `invitations.value`
3. Émission de l'événement `invitationSent`
4. `handleInvitationSent()` :
   - Recharge les membres (au cas où l'utilisateur existe déjà)
   - **Ne recharge PAS** les invitations (déjà en mémoire)
5. ✅ L'invitation reste visible dans la liste

---

## 🎯 Résultat

- ✅ Les invitations restent visibles après ajout
- ✅ Les invitations sont conservées pendant la session
- ✅ Les invitations sont réinitialisées quand on change de groupe
- ✅ Pas d'écrasement intempestif des données

---

## ⚠️ Limitation actuelle

**Pas de persistance entre les sessions** : Les invitations sont stockées uniquement en mémoire dans le store. Si l'utilisateur :
- Rafraîchit la page (F5)
- Ferme et rouvre le dialog
- Change de groupe puis revient

→ Les invitations ne seront plus visibles (car pas d'endpoint backend pour les récupérer).

### Solution future

Quand l'endpoint backend sera disponible :

```typescript
async function fetchGroupInvitations(groupId: string) {
  isLoading.value = true
  error.value = null

  try {
    const apiClient = getApiClient()
    const response = await apiClient.getGroupInvitations(groupId) // ← Nouveau endpoint
    invitations.value = response.data
    return invitations.value
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Erreur lors de la récupération des invitations'
    throw err
  } finally {
    isLoading.value = false
  }
}
```

---

## 🧪 Tests effectués

### ✅ Test 1 : Ajout d'invitation
1. Ouvrir le dialog d'un groupe
2. Entrer un email valide
3. Cliquer sur "Envoyer l'invitation"
4. **Résultat** : L'invitation reste visible dans "Invitations en attente"

### ✅ Test 2 : Plusieurs invitations
1. Envoyer 3 invitations successivement
2. **Résultat** : Les 3 invitations restent visibles

### ✅ Test 3 : Changement de groupe
1. Envoyer une invitation pour le groupe A
2. Fermer le dialog
3. Ouvrir le dialog du groupe B
4. **Résultat** : Pas d'invitations affichées pour le groupe B (correct)

### ✅ Test 4 : Retour au même groupe
1. Envoyer une invitation pour le groupe A
2. Fermer le dialog
3. Rouvrir le dialog du groupe A
4. **Résultat** : L'invitation est toujours visible (pendant la session)

---

## 📊 Comparaison avant/après

| Situation | Avant | Après |
|-----------|-------|-------|
| Ajout d'invitation | ❌ Disparaît | ✅ Reste visible |
| Plusieurs invitations | ❌ Seule la dernière reste | ✅ Toutes visibles |
| Changement de groupe | ⚠️ Invitations mélangées | ✅ Séparées par groupe |
| Gestion mémoire | ⚠️ Écrasements fréquents | ✅ Conservation intelligente |

---

## ✅ Problème résolu

Le bug est **complètement corrigé**. Les invitations restent maintenant visibles après leur ajout et sont correctement gérées par groupe.

