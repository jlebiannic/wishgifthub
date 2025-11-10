# 🔒 Scénario de Tests de Sécurité - Non Régression des Droits

## 📋 Description

Ce scénario de tests automatisés valide tous les contrôles de sécurité et de permissions de l'application WishGiftHub. Il vérifie que les règles d'accès sont correctement appliquées à tous les niveaux.

## 🎯 Objectif

Garantir que :
- ✅ Seuls les admins peuvent créer/modifier/supprimer des groupes
- ✅ Seul le propriétaire d'un groupe peut le gérer
- ✅ Les users ne peuvent pas réserver leurs propres souhaits
- ✅ Chacun ne peut gérer que ses propres ressources
- ✅ Les authorities Spring Security fonctionnent correctement

## 📊 Couverture des tests (26 étapes)

### Phase 1 : Setup (Étapes 1-4)
1. ✅ Créer un admin
2. ✅ Admin crée un groupe
3. ✅ Admin crée une invitation
4. ✅ User accepte l'invitation

### Phase 2 : Tests Groupe - Permissions Admin (Étapes 5-7)
5. ❌ **SÉCURITÉ**: User ne peut PAS créer de groupe
6. ❌ **SÉCURITÉ**: User ne peut PAS modifier le groupe
7. ❌ **SÉCURITÉ**: User ne peut PAS supprimer le groupe

### Phase 3 : Tests Souhaits - Création (Étapes 8-9)
8. ✅ User crée un souhait
9. ✅ Admin crée un souhait

### Phase 4 : Tests Souhaits - Réservations (Étapes 10-13)
10. ❌ **SÉCURITÉ**: User ne peut PAS réserver son propre souhait
11. ✅ User peut réserver le souhait de l'admin
12. ❌ **SÉCURITÉ**: Admin ne peut PAS annuler réservation du user
13. ✅ User peut annuler sa propre réservation

### Phase 5 : Tests Souhaits - Suppressions (Étapes 14-15)
14. ❌ **SÉCURITÉ**: Admin ne peut PAS supprimer souhait du user
15. ✅ User peut supprimer son propre souhait

### Phase 6 : Tests Inter-Admins (Étapes 16-20)
16. ✅ Créer un second admin
17. ❌ **SÉCURITÉ**: Admin2 ne peut PAS modifier groupe d'Admin1
18. ❌ **SÉCURITÉ**: Admin2 ne peut PAS supprimer groupe d'Admin1
19. ❌ **SÉCURITÉ**: Admin2 ne peut PAS inviter dans groupe d'Admin1
20. ❌ **SÉCURITÉ**: Admin2 ne peut PAS voir les membres du groupe d'Admin1

### Phase 7 : Tests Isolation des Groupes (Étapes 21-25)
21. ✅ Admin2 crée son propre groupe
22. ❌ **SÉCURITÉ**: User ne peut PAS voir membres du groupe Admin2
23. ✅ Admin2 crée un souhait dans son groupe
24. ❌ **SÉCURITÉ**: User ne peut PAS réserver souhait dans groupe Admin2
25. ❌ **SÉCURITÉ**: User ne peut PAS voir souhaits dans groupe Admin2

### Phase 8 : Nettoyage (Étape 26)
26. ✅ Admin1 peut supprimer son propre groupe

## 🚀 Exécution

### Via Bruno Desktop (Recommandé)

1. Ouvrir Bruno Desktop
2. Ouvrir la collection `api-tests`
3. Sélectionner l'environnement `local`
4. Naviguer vers le dossier `security`
5. Cliquer sur **"Run Folder"** pour exécuter tous les tests dans l'ordre

### Via Bruno CLI

```bash
cd api-tests
bru run security --env local
```

### Exécuter un test spécifique

```bash
# Test : User ne peut pas créer de groupe
bru run security/5-test-user-cannot-create-group.bru --env local
```

## 📊 Codes HTTP attendus

| Type de test | Code HTTP | Signification |
|--------------|-----------|---------------|
| ✅ Opération autorisée | **200** ou **204** | Succès |
| ❌ Permission refusée | **403** | Accès refusé |
| ❌ Règle métier violée | **400** | Requête invalide |
| ❌ Ressource introuvable | **404** | Non trouvé |

## 🔍 Détails des contrôles de sécurité

### 1. Rôle ADMIN requis

**Endpoints protégés :**
- `POST /api/groups` - Créer un groupe
- `PUT /api/groups/{id}` - Modifier un groupe
- `DELETE /api/groups/{id}` - Supprimer un groupe
- `POST /api/groups/{id}/invite` - Créer une invitation

**Annotation :**
```java
@PreAuthorize("hasRole('ADMIN')")
```

**Test :** Étape 5 - User tente de créer un groupe → **403**

---

### 2. Propriété du groupe

**Règle :** Seul le propriétaire (créateur) du groupe peut le modifier/supprimer.

**Vérification dans le service :**
```java
if (!group.getAdmin().getId().equals(adminId)) {
    throw new AccessDeniedException("Seul le propriétaire...");
}
```

**Tests :**
- Étape 6 : User tente de modifier → **403**
- Étape 7 : User tente de supprimer → **403**
- Étape 17 : Admin2 tente de modifier groupe d'Admin1 → **403**
- Étape 18 : Admin2 tente de supprimer groupe d'Admin1 → **403**

---

### 3. Appartenance au groupe

**Règle :** Un utilisateur doit appartenir au groupe pour y créer/consulter des souhaits.

**Annotation :**
```java
@PreAuthorize("hasAuthority('GROUP_' + #groupId)")
```

**Mécanisme :** Le JWT contient la liste des IDs de groupes comme authorities.

**Tests :**
- Étape 8 : User (dans le groupe) crée un souhait → **200**
- Si un user hors groupe tentait → **403**

---

### 4. Règle métier : Réservation

**Règle :** Un utilisateur ne peut pas réserver son propre souhait.

**Vérification dans le service :**
```java
if (wish.getUser().getId().equals(userId)) {
    throw new BusinessRuleException("Vous ne pouvez pas réserver votre propre souhait");
}
```

**Test :** Étape 10 - User tente de réserver son souhait → **400**

---

### 5. Règle métier : Annulation réservation

**Règle :** Seul celui qui a réservé peut annuler la réservation.

**Vérification dans le service :**
```java
if (!wish.getReservedBy().getId().equals(userId)) {
    throw new BusinessRuleException("Vous n'avez pas réservé ce souhait");
}
```

**Test :** Étape 12 - Admin tente d'annuler réservation du user → **400**

---

### 6. Propriété du souhait

**Règle :** Seul le créateur du souhait peut le supprimer.

**Vérification dans le service :**
```java
if (!wish.getUser().getId().equals(userId)) {
    throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres souhaits");
}
```

**Test :** Étape 14 - Admin tente de supprimer souhait du user → **403**

---

### 7. Invitation : Admin du groupe

**Règle :** Seul l'admin propriétaire du groupe peut créer des invitations.

**Vérification dans le service :**
```java
Group group = groupRepository.findByIdAndAdminId(groupId, adminId)
    .orElseThrow(() -> new AccessDeniedException("..."));
```

**Test :** Étape 19 - Admin2 tente d'inviter dans groupe d'Admin1 → **403**

---

### 8. Isolation des groupes

**Règle :** Un utilisateur ne peut accéder qu'aux ressources des groupes auxquels il appartient.

**Mécanisme :** Les annotations `@PreAuthorize("hasAuthority('GROUP_' + #groupId)")` vérifient que l'utilisateur a l'authority correspondant au groupe.

**Vérifications :**
- Voir les membres d'un groupe
- Réserver un souhait dans un groupe
- Voir les souhaits d'un groupe

**Tests :**
- Étape 22 : User tente de voir membres du groupe Admin2 → **403**
- Étape 24 : User tente de réserver dans groupe Admin2 → **403**
- Étape 25 : User tente de voir souhaits du groupe Admin2 → **403**

---

## ✅ Résultats attendus

Si tous les tests passent, vous verrez dans les logs :

```
✅ SÉCURITÉ - Étape 1: Admin créé
✅ SÉCURITÉ - Étape 2: Groupe créé par admin
✅ SÉCURITÉ - Étape 3: Invitation créée
✅ SÉCURITÉ - Étape 4: User a accepté l'invitation
✅ SÉCURITÉ - Étape 5: User ne peut PAS créer de groupe ✓
✅ SÉCURITÉ - Étape 6: User ne peut PAS modifier le groupe ✓
✅ SÉCURITÉ - Étape 7: User ne peut PAS supprimer le groupe ✓
✅ SÉCURITÉ - Étape 8: User a créé un souhait
✅ SÉCURITÉ - Étape 9: Admin a créé un souhait
✅ SÉCURITÉ - Étape 10: User ne peut PAS réserver son propre souhait ✓
✅ SÉCURITÉ - Étape 11: User peut réserver le souhait de l'admin ✓
✅ SÉCURITÉ - Étape 12: Admin ne peut PAS annuler la réservation du user ✓
✅ SÉCURITÉ - Étape 13: User peut annuler sa propre réservation ✓
✅ SÉCURITÉ - Étape 14: Admin ne peut PAS supprimer souhait d'un autre ✓
✅ SÉCURITÉ - Étape 15: User peut supprimer son propre souhait ✓
✅ SÉCURITÉ - Étape 16: Second admin créé
✅ SÉCURITÉ - Étape 17: Admin2 ne peut PAS modifier groupe d'Admin1 ✓
✅ SÉCURITÉ - Étape 18: Admin2 ne peut PAS supprimer groupe d'Admin1 ✓
✅ SÉCURITÉ - Étape 19: Admin2 ne peut PAS inviter dans groupe d'Admin1 ✓
✅ SÉCURITÉ - Étape 21: Admin2 crée son propre groupe
✅ SÉCURITÉ - Étape 22: User ne peut PAS voir membres d'un groupe auquel il n'appartient pas ✓
✅ SÉCURITÉ - Étape 23: Admin2 a créé un souhait dans son groupe
✅ SÉCURITÉ - Étape 24: User ne peut PAS réserver souhait dans groupe auquel il n'appartient pas ✓
✅ SÉCURITÉ - Étape 25: User ne peut PAS voir souhaits d'un user dans groupe auquel il n'appartient pas ✓
✅ SÉCURITÉ - Étape 26: Admin1 peut supprimer son propre groupe ✓
========================================
✅✅✅ TEST DE SÉCURITÉ TERMINÉ AVEC SUCCÈS! ✅✅✅
========================================
```

## 🐛 Dépannage

### Un test de sécurité échoue (retourne 200 au lieu de 403)

**Cause :** Les contrôles de sécurité ne fonctionnent pas.

**Actions :**
1. Vérifier que `@EnableMethodSecurity` est présent dans `SecurityConfig`
2. Vérifier les annotations `@PreAuthorize` sur les contrôleurs
3. Vérifier que le JWT contient les bonnes authorities
4. Vérifier les logs de l'application Spring Boot

### Un test légitime échoue (retourne 403 au lieu de 200)

**Cause :** L'utilisateur n'a pas les bonnes permissions.

**Actions :**
1. Vérifier que le JWT est bien mis à jour (après création de groupe, acceptation invitation)
2. Vérifier que la variable d'environnement contient le bon token
3. Vérifier que le groupe existe et que l'utilisateur en fait partie

### Erreur 401 Unauthorized

**Cause :** Token JWT manquant ou invalide.

**Actions :**
1. Relancer le scénario depuis l'étape 1
2. Vérifier que les variables `security_admin_token` et `security_user_token` sont définies

## 📚 Variables d'environnement utilisées

| Variable | Description | Définie à l'étape |
|----------|-------------|-------------------|
| `security_admin_token` | JWT de l'admin 1 | Étape 1, mise à jour étape 2 |
| `security_admin_id` | ID de l'admin 1 | Étape 1 |
| `security_group_id` | ID du groupe créé | Étape 2 |
| `security_invitation_token` | Token d'invitation | Étape 3 |
| `security_user_token` | JWT du user invité | Étape 4 |
| `security_user_wish_id` | ID du souhait du user | Étape 8 |
| `security_admin_wish_id` | ID du souhait de l'admin | Étape 9 |
| `security_admin2_token` | JWT de l'admin 2 | Étape 16, mise à jour étape 21 |
| `security_admin2_group_id` | ID du groupe de l'admin 2 | Étape 21 |
| `security_admin2_wish_id` | ID du souhait de l'admin 2 | Étape 23 |

## 💡 Bonnes pratiques

1. **Exécuter régulièrement** - Après chaque modification des règles de sécurité
2. **CI/CD** - Intégrer dans votre pipeline de déploiement
3. **Ajout de tests** - Ajouter un test pour chaque nouvelle règle de sécurité
4. **Logs** - Consulter les logs Spring Boot pour comprendre les échecs

## 🔗 Liens connexes

- **Scénario de non-régression complet** : `scenarios/`
- **Test de cascade** : `cascade-delete/`
- **Documentation exceptions** : `CUSTOM_EXCEPTIONS_README.md`
- **Documentation authorities** : `modification_authorities_groups.md`

---

✅ **Le scénario de tests de sécurité est prêt à être exécuté !**

