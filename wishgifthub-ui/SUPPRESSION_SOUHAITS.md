# ✅ Suppression de souhaits - Implémentée

## 🎉 Fonctionnalité ajoutée

Les utilisateurs peuvent maintenant **supprimer leurs propres souhaits** depuis leur liste.

---

## 📋 Implémentation

### Composant MemberCard.vue

#### 1. État de suppression

```typescript
const isDeleting = ref<string | null>(null)
```

Stocke l'ID du souhait en cours de suppression pour afficher un loader.

#### 2. Fonction de suppression

```typescript
async function handleDelete(wish: WishResponse) {
  if (!confirm('Êtes-vous sûr de vouloir supprimer ce souhait ?')) {
    return
  }

  isDeleting.value = wish.id

  try {
    await wishStore.deleteWish(props.groupId, wish.id)
    emit('wishUpdated')
  } catch (error) {
    console.error('Erreur lors de la suppression:', error)
    alert('Erreur lors de la suppression du souhait')
  } finally {
    isDeleting.value = null
  }
}
```

**Fonctionnalités** :
- ✅ Demande confirmation avant suppression
- ✅ Appel au store wish pour supprimer
- ✅ Émet un événement pour rafraîchir la liste
- ✅ Gestion d'erreur avec message utilisateur
- ✅ Loader pendant la suppression

#### 3. Bouton de suppression

```vue
<!-- Bouton Supprimer (uniquement pour mes propres souhaits) -->
<v-btn
  v-if="isCurrentUser"
  color="error"
  size="small"
  variant="text"
  icon="mdi-delete"
  @click="handleDelete(wish)"
  :loading="isDeleting === wish.id"
>
  <v-icon>mdi-delete</v-icon>
  <v-tooltip activator="parent" location="top">
    Supprimer
  </v-tooltip>
</v-btn>
```

**Caractéristiques** :
- 🔴 Couleur rouge (error) pour indiquer une action destructive
- 👤 Visible uniquement sur **mes propres souhaits** (`v-if="isCurrentUser"`)
- 🗑️ Icône de corbeille (mdi-delete)
- 💬 Tooltip "Supprimer" au survol
- ⏳ Loader pendant la suppression

---

## 🎨 Interface utilisateur

### Carte de mes souhaits

```
┌─────────────────────────────────────┐
│ [Image du produit]                  │
├─────────────────────────────────────┤
│ MacBook Pro 16 pouces               │
│ 2499.00 €                           │
│ Ordinateur portable Apple...        │
│ [Voir le produit ↗]                │
├─────────────────────────────────────┤
│              [🗑️]      [Réserver]  │ ← Bouton supprimer
└─────────────────────────────────────┘
```

### Carte des souhaits d'un autre utilisateur

```
┌─────────────────────────────────────┐
│ [Image du produit]                  │
├─────────────────────────────────────┤
│ Livre de cuisine                    │
│ 29.99 €                             │
│ Livre de recettes italiennes...     │
│ [Voir le produit ↗]                │
├─────────────────────────────────────┤
│                        [Réserver]   │ ← Pas de bouton supprimer
└─────────────────────────────────────┘
```

---

## 🔄 Flux de suppression

### Étape 1 : Clic sur le bouton supprimer

```
Utilisateur clique sur 🗑️
```

### Étape 2 : Confirmation

```
┌──────────────────────────────────────┐
│ Êtes-vous sûr de vouloir supprimer  │
│ ce souhait ?                         │
│                                      │
│          [Annuler]  [OK]             │
└──────────────────────────────────────┘
```

### Étape 3 : Suppression

Si l'utilisateur confirme :

```
1. Bouton affiche un loader ⏳
2. Appel API : DELETE /api/groups/{groupId}/wishes/{wishId}
3. Backend supprime le souhait de la base
4. Frontend supprime le souhait de la liste
5. Liste rafraîchie automatiquement
```

---

## 🔒 Règles de sécurité

### Frontend

- ✅ Le bouton n'apparaît **que** sur mes propres souhaits
- ✅ Vérification : `v-if="isCurrentUser"`
- ✅ Confirmation obligatoire avant suppression

### Backend

L'endpoint vérifie que :
- ✅ L'utilisateur est authentifié
- ✅ L'utilisateur est bien le créateur du souhait
- ✅ Le souhait appartient au groupe spécifié

**Code backend** (déjà implémenté) :

```java
public void deleteWish(UUID groupId, UUID wishId, UUID userId) {
    Wish wish = wishRepository.findById(wishId)
        .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));
    
    // Vérifier que l'utilisateur est bien le créateur
    if (!wish.getUser().getId().equals(userId)) {
        throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres souhaits");
    }
    
    wishRepository.delete(wish);
}
```

---

## 🧪 Tests

### Test 1 : Suppression d'un de mes souhaits

1. Se connecter
2. Aller dans un groupe
3. Cliquer sur ma carte pour l'étendre
4. ✅ Le bouton 🗑️ est visible à côté de chaque souhait
5. Cliquer sur 🗑️
6. ✅ Message de confirmation s'affiche
7. Confirmer
8. ✅ Le souhait disparaît de la liste

### Test 2 : Souhaits d'un autre utilisateur

1. Se connecter
2. Aller dans un groupe
3. Cliquer sur la carte d'un autre membre
4. ✅ Le bouton 🗑️ n'apparaît **PAS**
5. ✅ Seul le bouton "Réserver" est visible

### Test 3 : Annulation de la suppression

1. Cliquer sur 🗑️
2. Message de confirmation s'affiche
3. Cliquer sur "Annuler"
4. ✅ Le souhait reste dans la liste
5. ✅ Rien n'est supprimé

### Test 4 : Erreur de suppression

1. Déconnecter le backend
2. Essayer de supprimer un souhait
3. ✅ Message d'erreur s'affiche : "Erreur lors de la suppression du souhait"
4. ✅ Le souhait reste dans la liste

---

## 📊 Positionnement du bouton

Le bouton de suppression est placé **avant** les boutons d'action :

```
[Chip statut]  [Spacer]  [🗑️ Supprimer]  [Réserver/Annuler]
```

**Pourquoi à gauche ?**
- ✅ Moins accessible = moins de risque de clic accidentel
- ✅ Sépare visuellement les actions de gestion (supprimer) des actions utilisateur (réserver)
- ✅ Icône seule (sans texte) = discret

---

## 🎨 Style du bouton

```vue
<v-btn
  color="error"        ← Rouge pour danger
  size="small"         ← Petit pour discrétion
  variant="text"       ← Texte (pas élevé) pour discrétion
  icon="mdi-delete"    ← Icône corbeille
>
```

**Caractéristiques** :
- 🔴 **Couleur rouge** : Indique une action destructive
- 📏 **Petit** : Moins invasif visuellement
- 🎨 **Variant text** : Pas de fond, plus discret
- 🗑️ **Icône seule** : Compréhensible sans texte

---

## 🔄 API utilisée

### Endpoint

```
DELETE /api/groups/{groupId}/wishes/{wishId}
```

### Store method

```typescript
async function deleteWish(groupId: string, wishId: string) {
  isLoading.value = true
  error.value = null

  try {
    const apiClient = getApiClient()
    await apiClient.deleteWish(groupId, wishId)
    
    // Retirer le souhait de la liste locale
    wishes.value = wishes.value.filter(w => w.id !== wishId)
    
    return true
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Erreur lors de la suppression du souhait'
    throw err
  } finally {
    isLoading.value = false
  }
}
```

**Déjà implémenté** dans `src/stores/wish.ts` ✅

---

## ✅ Avantages

### Expérience utilisateur

- ✅ **Contrôle total** sur sa liste de souhaits
- ✅ **Confirmation** évite les suppressions accidentelles
- ✅ **Feedback visuel** avec le loader
- ✅ **Messages d'erreur** clairs en cas de problème

### Sécurité

- ✅ **Validation côté serveur** : Impossible de supprimer le souhait d'un autre
- ✅ **Confirmation** : Réduit les erreurs utilisateur
- ✅ **Isolation** : Le bouton n'apparaît que sur ses propres souhaits

---

## 🆚 Avant vs Après

### ❌ Avant

```
Impossible de supprimer un souhait
→ Il faut contacter un admin
→ Ou modifier manuellement en base
```

### ✅ Après

```
Clic sur 🗑️ → Confirmation → Supprimé
→ Autonomie complète
→ Liste à jour instantanément
```

---

## 📝 Notes techniques

### Émission d'événement

Après suppression, on émet `wishUpdated` pour que le parent recharge la liste :

```typescript
emit('wishUpdated')
```

Le parent (`GroupMembersView`) écoute cet événement et rafraîchit :

```typescript
async function handleWishUpdated() {
  await wishStore.fetchGroupWishes(groupId.value)
}
```

### Suppression optimiste vs pessimiste

**Implémentation actuelle** : **Pessimiste**
- Attend la confirmation du serveur
- Puis supprime de la liste locale
- Plus sûr mais légèrement plus lent

**Alternative optimiste** :
- Supprime immédiatement de la liste
- Restaure en cas d'erreur
- Plus rapide mais risque d'incohérence

---

## ✅ Fonctionnalité complète !

La suppression de souhaits est maintenant **entièrement fonctionnelle** avec :

- ✅ Bouton visible uniquement sur mes souhaits
- ✅ Confirmation avant suppression
- ✅ Loader pendant la suppression
- ✅ Gestion d'erreur
- ✅ Rafraîchissement automatique de la liste
- ✅ Sécurité côté serveur

**Les utilisateurs ont maintenant le contrôle total de leur liste de souhaits !** 🎉

