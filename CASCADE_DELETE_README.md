# 🗑️ Suppression en Cascade des Groupes

## ✅ Configuration Terminée

La suppression en cascade a été configurée pour les groupes. Lorsqu'un groupe est supprimé, **toutes** les données associées sont automatiquement supprimées.

## 📋 Ce qui est supprimé en cascade

Quand vous supprimez un groupe, les entités suivantes sont **automatiquement supprimées** :

1. ✅ **Relations user_groups** - Tous les liens entre users et le groupe
2. ✅ **Invitations** - Toutes les invitations liées au groupe (acceptées ou non)
3. ✅ **Souhaits (wishes)** - Tous les souhaits créés dans le groupe

## 🔧 Configuration technique

### 1. Niveau JPA (Entité Group)

Dans l'entité `Group.java`, trois relations `OneToMany` avec cascade :

```java
@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
private List<UserGroup> userGroups = new ArrayList<>();

@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Invitation> invitations = new ArrayList<>();

@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Wish> wishes = new ArrayList<>();
```

**Explications :**
- `cascade = CascadeType.ALL` : Toutes les opérations (persist, merge, remove, refresh, detach) sont propagées
- `orphanRemoval = true` : Si une relation est retirée de la collection, l'entité orpheline est supprimée

### 2. Niveau Base de Données (PostgreSQL)

Dans le schéma SQL (`supabase_schema.sql`), les contraintes de clés étrangères avec `ON DELETE CASCADE` :

```sql
-- user_groups
group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE

-- invitations
group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE

-- wishes
group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE
```

## 🎯 Comportement

### Avant la suppression du groupe

```
Groupe "Noël 2025" (ID: xxx)
├── user_groups
│   ├── Admin (propriétaire)
│   ├── User 1
│   └── User 2
├── invitations
│   ├── Invitation pour user3@example.com (acceptée)
│   └── Invitation pour user4@example.com (en attente)
└── wishes
    ├── Souhait 1 (de User 1)
    ├── Souhait 2 (de User 2)
    └── Souhait 3 (de User 1)
```

### Après `DELETE /api/groups/xxx`

```
Groupe "Noël 2025" → ❌ SUPPRIMÉ
├── user_groups → ❌ TOUS SUPPRIMÉS
├── invitations → ❌ TOUTES SUPPRIMÉES
└── wishes → ❌ TOUS SUPPRIMÉS
```

**MAIS** : Les utilisateurs (User 1, User 2, Admin) ne sont **PAS** supprimés ! Ils peuvent toujours appartenir à d'autres groupes.

## 🧪 Tests de Non-Régression

Un scénario de test complet est disponible dans `api-tests/cascade-delete/` :

### Étapes du test

1. ✅ Créer un admin
2. ✅ Créer un groupe
3. ✅ Créer une invitation
4. ✅ Accepter l'invitation (création user_group)
5. ✅ Créer un souhait
6. ✅ Vérifier que tout existe
7. ✅ **SUPPRIMER le groupe**
8. ✅ Vérifier que les user_groups n'existent plus
9. ✅ Vérifier que les souhaits n'existent plus

### Exécution du test

```bash
# Via Bruno Desktop
# → Ouvrir le dossier "cascade-delete" et cliquer sur "Run Folder"

# Via Bruno CLI
cd api-tests
bru run cascade-delete --env local
```

## 📊 Exemple de suppression

### Via l'API REST

```http
DELETE /api/groups/{groupId}
Authorization: Bearer <admin-token>
```

**Réponse :**
- Status `204 No Content` si succès
- Les suppressions en cascade sont **automatiques et transparentes**

### Via le code Java

```java
@Transactional
public void deleteGroup(UUID groupId, UUID adminId) {
    Group group = groupRepository.findById(groupId).orElseThrow();
    if (!group.getAdmin().getId().equals(adminId)) {
        throw new SecurityException("Seul l'admin peut supprimer le groupe");
    }
    
    // Cette ligne déclenche automatiquement la suppression en cascade
    groupRepository.delete(group);
    
    // Pas besoin de supprimer manuellement :
    // - userGroupRepository.deleteByGroupId(groupId)  ❌ Inutile
    // - invitationRepository.deleteByGroupId(groupId)  ❌ Inutile
    // - wishRepository.deleteByGroupId(groupId)  ❌ Inutile
}
```

## 🔒 Sécurité

### Protection contre la suppression accidentelle

Seul l'**administrateur propriétaire** du groupe peut le supprimer :

```java
if (!group.getAdmin().getId().equals(adminId)) {
    throw new SecurityException("Seul l'admin peut supprimer le groupe");
}
```

### Protection Spring Security

L'annotation `@PreAuthorize` sur le contrôleur vérifie que l'utilisateur est admin :

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteGroup(@PathVariable("id") UUID id, @AuthenticationPrincipal User admin)
```

## 💡 Bonnes Pratiques

### 1. Toujours utiliser @Transactional

La suppression est transactionnelle, donc :
- ✅ Soit **tout** est supprimé (groupe + cascades)
- ✅ Soit **rien** n'est supprimé (en cas d'erreur)
- ❌ Pas de suppression partielle

### 2. Logs de suppression

Pour tracer les suppressions, vous pouvez ajouter des logs :

```java
@Transactional
public void deleteGroup(UUID groupId, UUID adminId) {
    Group group = groupRepository.findById(groupId).orElseThrow();
    if (!group.getAdmin().getId().equals(adminId)) {
        throw new SecurityException();
    }
    
    logger.info("Suppression du groupe {} par admin {}", groupId, adminId);
    logger.debug("Suppressions en cascade : {} user_groups, {} invitations, {} wishes", 
        group.getUserGroups().size(), 
        group.getInvitations().size(), 
        group.getWishes().size());
    
    groupRepository.delete(group);
    
    logger.info("Groupe {} supprimé avec succès", groupId);
}
```

### 3. Confirmation côté client

Côté frontend, demandez toujours une confirmation avant suppression :

```javascript
if (confirm(`Êtes-vous sûr de vouloir supprimer le groupe "${groupName}" ? 
Cela supprimera également :
- Tous les membres du groupe
- Toutes les invitations
- Tous les souhaits

Cette action est IRRÉVERSIBLE.`)) {
    await deleteGroup(groupId);
}
```

## 🐛 Dépannage

### Erreur : "constraint violation"

**Cause** : Une contrainte de clé étrangère n'a pas `ON DELETE CASCADE`

**Solution** : Vérifiez le schéma SQL et ajoutez `ON DELETE CASCADE` sur les FK concernées

### Erreur : "detached entity passed to persist"

**Cause** : Problème de gestion de la session Hibernate

**Solution** : Assurez-vous que la méthode est annotée avec `@Transactional`

### Les cascades JPA ne fonctionnent pas

**Vérifiez :**
1. Les annotations `@OneToMany` dans `Group.java`
2. Le `cascade = CascadeType.ALL`
3. Le `orphanRemoval = true`
4. La méthode est bien `@Transactional`

## 📚 Référence

### CascadeType.ALL vs ON DELETE CASCADE

| Niveau | Mécanisme | Déclencheur |
|--------|-----------|-------------|
| **JPA** | `CascadeType.ALL` | Déclenché par Java/Hibernate |
| **SQL** | `ON DELETE CASCADE` | Déclenché par PostgreSQL |

**Les deux sont configurés** pour une **double protection** :
- Si JPA fonctionne → Les cascades JPA suffisent
- Si problème JPA → Les cascades SQL prennent le relais
- **Résultat** : Suppression garantie dans tous les cas ✅

## ✅ Résumé

- ✅ Configuration JPA avec `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`
- ✅ Configuration SQL avec `ON DELETE CASCADE`
- ✅ Tests de non-régression disponibles
- ✅ Sécurité : Seul l'admin propriétaire peut supprimer
- ✅ Transactionnel : Atomicité garantie
- ✅ Automatique : Pas de code manuel nécessaire

---

🎉 **La suppression en cascade est opérationnelle et testée !**

