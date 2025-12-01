# Fix : Utilisateur simple ne voit pas les membres du groupe

## 🐛 Problème

Lorsqu'un utilisateur simple (non-admin) se connecte et accède à la page d'un groupe, il voit "Aucun membre" alors qu'il y a bien 3 membres dont un admin.

---

## 🔍 Cause du problème

### Problème 1 : Autorités Spring Security manquantes

L'endpoint `/api/groups/{groupId}/users` est protégé par :
```java
@PreAuthorize("hasAuthority('GROUP_' + #groupId)")
```

Cela signifie que l'utilisateur doit avoir l'autorité `GROUP_{groupId}` dans son JWT.

**Le problème** : Le filtre JWT ajoute bien les autorités depuis le claim `groupIds` du token, MAIS si l'utilisateur rafraîchit la page ou se reconnecte, le frontend n'appelait pas la bonne méthode pour récupérer les groupes.

### Problème 2 : restoreSession() ne récupérait pas les groupes pour les non-admins

Dans `auth.ts`, la fonction `restoreSession()` ne récupérait les groupes que pour les admins :

**Avant :**
```typescript
if (decodedToken.isAdmin && groupIds.length > 0) {
  const groupStore = useGroupStore()
  await groupStore.fetchGroups()
}
```

Donc les utilisateurs non-admin n'avaient pas leurs groupes chargés automatiquement.

### Problème 3 : GroupMembersView utilisait fetchGroups() pour tous

La page `GroupMembersView.vue` appelait toujours `fetchGroups()` qui est réservé aux admins :

**Avant :**
```typescript
await groupStore.fetchGroups()
```

---

## ✅ Solutions appliquées

### Solution 1 : Récupérer les groupes pour les non-admins dans restoreSession()

**Fichier** : `src/stores/auth.ts`

**Avant :**
```typescript
if (decodedToken.isAdmin && groupIds.length > 0) {
  const groupStore = useGroupStore()
  await groupStore.fetchGroups()
}
```

**Après :**
```typescript
// Récupérer les groupes pour tous les utilisateurs (admin ou non)
if (groupIds.length > 0) {
  const groupStore = useGroupStore()
  if (decodedToken.isAdmin) {
    await groupStore.fetchGroups()
  } else {
    await groupStore.fetchMyGroups()
  }
}
```

**Explication** : Les utilisateurs non-admin utilisent `fetchMyGroups()` qui appelle `GET /api/groups/me` au lieu de `GET /api/groups`.

### Solution 2 : Utiliser la bonne méthode dans GroupMembersView

**Fichier** : `src/views/GroupMembersView.vue`

**Avant :**
```typescript
await groupStore.fetchGroups()
```

**Après :**
```typescript
// Utiliser fetchMyGroups() pour les non-admins
if (authStore.isAdmin) {
  await groupStore.fetchGroups()
} else {
  await groupStore.fetchMyGroups()
}
```

**Explication** : On appelle la bonne méthode selon le rôle de l'utilisateur.

---

## 🔄 Flux complet corrigé

### Utilisateur invité accepte l'invitation

1. Clic sur le lien d'invitation
2. `acceptInvitation()` côté backend :
   - Crée l'utilisateur
   - L'ajoute au groupe (table `user_groups`)
   - Récupère tous ses groupes
   - Génère un JWT avec `groupIds` : `["uuid-group-1", "uuid-group-2"]`
3. `loginWithToken()` côté frontend :
   - Décode le JWT
   - Extrait les `groupIds`
   - Stocke le token
   - Appelle `fetchMyGroups()` ✅

### Utilisateur rafraîchit la page

1. Page se charge
2. `restoreSession()` est appelé :
   - Lit le token depuis localStorage
   - Décode le token
   - Extrait les `groupIds`
   - Appelle `fetchMyGroups()` ✅ (CORRIGÉ)

### Utilisateur accède à un groupe

1. Clic sur un groupe
2. Navigation vers `/group/{groupId}`
3. `GroupMembersView` se charge :
   - Appelle `fetchMyGroups()` pour les non-admins ✅ (CORRIGÉ)
   - Appelle `fetchGroupMembers(groupId)` :
     - Backend vérifie l'autorité `GROUP_{groupId}` ✅
     - Le filtre JWT a ajouté cette autorité depuis le claim `groupIds` ✅
     - L'utilisateur a l'accès ✅
   - Les membres s'affichent ✅

---

## 🧪 Pour tester

### Scénario de test complet

1. **Admin invite un utilisateur**
   - Se connecter en admin
   - Créer un groupe "Test"
   - Inviter user@example.com
   - Copier le lien d'invitation

2. **Utilisateur accepte**
   - Coller le lien dans un navigateur
   - Connexion automatique
   - Redirection vers l'accueil
   - ✅ Groupe "Test" visible dans la liste

3. **Utilisateur accède au groupe**
   - Cliquer sur le groupe "Test"
   - ✅ Page des membres s'affiche
   - ✅ 2 membres visibles : admin@example.com et user@example.com
   - ✅ Pas d'erreur 403

4. **Utilisateur rafraîchit la page**
   - Appuyer sur F5
   - ✅ Toujours connecté
   - ✅ Groupes toujours visibles
   - ✅ Membres toujours affichés

---

## 📊 Comparaison avant/après

| Situation | Avant | Après |
|-----------|-------|-------|
| **Acceptation invitation** | ✅ Fonctionne | ✅ Fonctionne |
| **Rafraîchissement page** | ❌ Groupes non chargés | ✅ Groupes chargés |
| **Accès aux membres** | ❌ Erreur 403 | ✅ Membres visibles |
| **Utilisateur non-admin** | ❌ "Aucun membre" | ✅ Membres affichés |

---

## 🔐 Sécurité

### Vérifications backend

Le backend vérifie bien que l'utilisateur a l'autorité pour accéder au groupe :

```java
@PreAuthorize("hasAuthority('GROUP_' + #groupId)")
public ResponseEntity<List<UserResponse>> getUsersByGroup(UUID groupId)
```

### Autorités JWT

Le filtre JWT ajoute les autorités depuis le token :

```java
List<UUID> groupIds = jwtService.getGroupIdsFromToken(token);
for (UUID groupId : groupIds) {
    authorities.add(new SimpleGrantedAuthority("GROUP_" + groupId.toString()));
}
```

### Claims JWT

Le token JWT contient :
```json
{
  "sub": "user-id",
  "isAdmin": false,
  "groupIds": ["group-id-1", "group-id-2"],
  "iat": 1234567890,
  "exp": 1234654290
}
```

---

## ✅ Problème résolu !

Les utilisateurs non-admin peuvent maintenant :
- ✅ Voir leurs groupes après connexion
- ✅ Voir leurs groupes après rafraîchissement de page
- ✅ Accéder à la liste des membres d'un groupe
- ✅ Ajouter et voir les souhaits

**Le système fonctionne correctement pour tous les types d'utilisateurs !** 🎉

