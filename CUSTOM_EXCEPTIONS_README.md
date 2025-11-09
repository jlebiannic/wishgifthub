# 🎯 Exceptions Personnalisées - Documentation

## 📋 Vue d'ensemble

L'application utilise des exceptions personnalisées pour gérer les différents cas d'erreur métier de manière cohérente et explicite.

## 🗂️ Liste des exceptions

### 1. ResourceNotFoundException

**Package :** `com.wishgifthub.exception`  
**Code HTTP :** `404 Not Found`  
**Code erreur :** `RESOURCE_NOT_FOUND`

**Utilisation :** Quand une ressource demandée n'existe pas en base de données.

**Constructeurs :**
```java
// Message personnalisé
throw new ResourceNotFoundException("Message personnalisé");

// Message généré automatiquement
throw new ResourceNotFoundException("Groupe", groupId);
// → "Groupe avec l'ID 'xxx' non trouvé(e)"
```

**Exemples d'utilisation :**
- Groupe non trouvé
- Utilisateur non trouvé
- Souhait non trouvé
- Invitation non trouvée

---

### 2. AccessDeniedException

**Package :** `com.wishgifthub.exception`  
**Code HTTP :** `403 Forbidden`  
**Code erreur :** `ACCESS_DENIED`

**Utilisation :** Quand un utilisateur tente d'accéder à une ressource pour laquelle il n'a pas les permissions.

**Constructeurs :**
```java
// Message personnalisé
throw new AccessDeniedException("Message personnalisé");

// Message par défaut
throw new AccessDeniedException();
// → "Accès refusé : vous n'avez pas les permissions nécessaires"
```

**Exemples d'utilisation :**
- Seul un admin peut créer un groupe
- Seul le propriétaire peut modifier/supprimer un groupe
- Seul le propriétaire d'un souhait peut le supprimer

---

### 3. BusinessRuleException

**Package :** `com.wishgifthub.exception`  
**Code HTTP :** `400 Bad Request`  
**Code erreur :** `BUSINESS_RULE_VIOLATION`

**Utilisation :** Quand une règle métier est violée.

**Constructeur :**
```java
throw new BusinessRuleException("Message de la règle violée");
```

**Exemples d'utilisation :**
- Vous ne pouvez pas réserver votre propre souhait
- Ce souhait est déjà réservé
- Le souhait n'appartient pas à ce groupe
- L'utilisateur n'appartient pas au groupe

---

### 4. InvalidInvitationException

**Package :** `com.wishgifthub.exception`  
**Code HTTP :** `400 Bad Request`  
**Code erreur :** `INVALID_INVITATION`

**Utilisation :** Quand une invitation n'est pas valide.

**Constructeur :**
```java
throw new InvalidInvitationException("Message d'erreur");
```

**Exemples d'utilisation :**
- Token d'invitation invalide
- Invitation déjà acceptée
- Invitation expirée (si implémenté)

---

### 5. DuplicateResourceException

**Package :** `com.wishgifthub.exception`  
**Code HTTP :** `409 Conflict`  
**Code erreur :** `DUPLICATE_RESOURCE`

**Utilisation :** Quand on tente de créer une ressource qui existe déjà.

**Constructeurs :**
```java
// Message personnalisé
throw new DuplicateResourceException("Message personnalisé");

// Message généré automatiquement
throw new DuplicateResourceException("Utilisateur", "email", "user@example.com");
// → "Utilisateur avec email 'user@example.com' existe déjà"
```

**Exemples d'utilisation :**
- Email déjà utilisé lors de l'inscription
- Nom de groupe en double (si unicité requise)

---

## 📊 Mapping des codes HTTP

| Exception | Code HTTP | Code erreur | Usage |
|-----------|-----------|-------------|-------|
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` | Ressource introuvable |
| `AccessDeniedException` | 403 | `ACCESS_DENIED` | Permissions insuffisantes |
| `BusinessRuleException` | 400 | `BUSINESS_RULE_VIOLATION` | Règle métier violée |
| `InvalidInvitationException` | 400 | `INVALID_INVITATION` | Invitation invalide |
| `DuplicateResourceException` | 409 | `DUPLICATE_RESOURCE` | Ressource dupliquée |
| `IllegalArgumentException` | 404 | `INVALID_ARGUMENT` | Argument invalide (legacy) |
| `SecurityException` | 403 | `SECURITY_ERROR` | Erreur de sécurité (legacy) |

## 🎨 Format de réponse d'erreur

Toutes les exceptions retournent une réponse JSON structurée :

```json
{
  "status": 404,
  "message": "Groupe avec l'ID 'xxx' non trouvé(e)",
  "code": "RESOURCE_NOT_FOUND",
  "timestamp": 1699520400000
}
```

**Champs :**
- `status` : Code HTTP
- `message` : Message d'erreur lisible par l'utilisateur
- `code` : Code d'erreur pour le traitement côté client
- `timestamp` : Timestamp de l'erreur (en millisecondes)

## 🔧 Utilisation dans les services

### Exemple : WishService

```java
public WishResponse reserveWish(UUID groupId, UUID wishId, UUID userId) {
    // Ressource non trouvée → 404
    Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

    // Règle métier violée → 400
    if (!wish.getGroup().getId().equals(groupId)) {
        throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
    }

    // Règle métier violée → 400
    if (wish.getUser().getId().equals(userId)) {
        throw new BusinessRuleException("Vous ne pouvez pas réserver votre propre souhait");
    }

    // Règle métier violée → 400
    if (wish.getReservedBy() != null) {
        throw new BusinessRuleException("Ce souhait est déjà réservé");
    }

    // ...
}
```

### Exemple : GroupService

```java
public void deleteGroup(UUID groupId, UUID adminId) {
    // Ressource non trouvée → 404
    Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));
    
    // Accès refusé → 403
    if (!group.getAdmin().getId().equals(adminId)) {
        throw new AccessDeniedException("Seul le propriétaire du groupe peut le supprimer");
    }
    
    groupRepository.delete(group);
}
```

## 🎯 Bonnes pratiques

### 1. Utiliser l'exception appropriée

✅ **Bon :**
```java
// Ressource non trouvée
throw new ResourceNotFoundException("Groupe", groupId);

// Règle métier
throw new BusinessRuleException("Vous ne pouvez pas réserver votre propre souhait");

// Permission refusée
throw new AccessDeniedException("Seul l'admin peut créer des groupes");
```

❌ **Mauvais :**
```java
// Trop générique
throw new RuntimeException("Erreur");

// Mauvais code HTTP
throw new IllegalArgumentException("Vous ne pouvez pas réserver votre propre souhait");
// → Devrait être BusinessRuleException (400) et non 404
```

### 2. Messages d'erreur clairs

✅ **Bon :**
```java
throw new BusinessRuleException("Vous ne pouvez pas réserver votre propre souhait");
```

❌ **Mauvais :**
```java
throw new BusinessRuleException("Erreur");
throw new BusinessRuleException("Invalid operation");
```

### 3. Utiliser les constructeurs adaptés

```java
// Pour les ressources
throw new ResourceNotFoundException("Groupe", groupId);
// Mieux que
throw new ResourceNotFoundException("Groupe avec l'ID " + groupId + " non trouvé");

// Pour les duplications
throw new DuplicateResourceException("Utilisateur", "email", email);
// Mieux que
throw new DuplicateResourceException("Email déjà utilisé");
```

## 🧪 Tests

### Tester les codes HTTP

```java
@Test
void shouldReturn404WhenGroupNotFound() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    
    // When & Then
    assertThrows(ResourceNotFoundException.class, () -> {
        groupService.deleteGroup(nonExistentId, adminId);
    });
}

@Test
void shouldReturn403WhenNotGroupOwner() {
    // Given
    UUID groupId = createGroup();
    UUID otherAdminId = createAnotherAdmin();
    
    // When & Then
    assertThrows(AccessDeniedException.class, () -> {
        groupService.deleteGroup(groupId, otherAdminId);
    });
}

@Test
void shouldReturn400WhenReservingOwnWish() {
    // Given
    UUID wishId = createWish(userId);
    
    // When & Then
    assertThrows(BusinessRuleException.class, () -> {
        wishService.reserveWish(groupId, wishId, userId);
    });
}
```

## 🔍 Débogage

### Logs automatiques

Le `GlobalExceptionHandler` log automatiquement toutes les exceptions :

```
WARN  - Ressource non trouvée : Groupe avec l'ID 'xxx' non trouvé(e)
WARN  - Règle métier violée : Vous ne pouvez pas réserver votre propre souhait
WARN  - Accès refusé : Seul le propriétaire du groupe peut le supprimer
ERROR - Erreur interne non gérée (avec stack trace complète)
```

### Vérifier le code d'erreur

Côté client, vous pouvez vérifier le `code` pour traiter spécifiquement certaines erreurs :

```javascript
try {
    await reserveWish(wishId);
} catch (error) {
    if (error.code === 'BUSINESS_RULE_VIOLATION') {
        if (error.message.includes('propre souhait')) {
            alert("Vous ne pouvez pas réserver votre propre souhait!");
        } else if (error.message.includes('déjà réservé')) {
            alert("Ce souhait a déjà été réservé par quelqu'un d'autre");
        }
    }
}
```

## 📚 Migration des anciennes exceptions

Les anciennes exceptions (`IllegalArgumentException`, `SecurityException`) sont toujours supportées pour compatibilité ascendante, mais il est recommandé de les remplacer progressivement :

| Ancien | Nouveau | Quand |
|--------|---------|-------|
| `IllegalArgumentException` | `ResourceNotFoundException` | Ressource introuvable |
| `IllegalArgumentException` | `BusinessRuleException` | Règle métier violée |
| `SecurityException` | `AccessDeniedException` | Permission refusée |

---

✅ **Les exceptions personnalisées sont maintenant opérationnelles dans toute l'application !**

