# 🔐 Vérification des droits administrateur

## Résumé

Pour garantir que seul un utilisateur avec `isAdmin = true` peut créer un groupe, nous avons mis en place **3 niveaux de sécurité** :

## ✅ 1. Vérification au niveau du Service

**Fichier** : `GroupService.java`

```java
@Transactional
public GroupResponse createGroup(GroupRequest request, UUID adminId) {
    User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
    
    // ✅ Vérification explicite que l'utilisateur est admin
    if (!admin.isAdmin()) {
        throw new SecurityException("Seuls les administrateurs peuvent créer des groupes");
    }
    
    // ... reste du code
}
```

**Avantages** :
- ✅ Protection au niveau métier
- ✅ Impossible de contourner même en appelant directement le service
- ✅ Message d'erreur clair

## ✅ 2. Annotation @IsAdmin au niveau du Contrôleur

**Fichier** : `GroupController.java`

```java
@IsAdmin  // ← Vérifie automatiquement les droits admin
@PostMapping
public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupRequest request, @AuthenticationPrincipal User admin) {
    return ResponseEntity.ok(groupService.createGroup(request, admin.getId()));
}
```

**Fonctionnement** :
- L'annotation `@IsAdmin` est interceptée par un Aspect (AOP)
- Avant l'exécution de la méthode, vérifie que `user.isAdmin() == true`
- Lance une `SecurityException` si l'utilisateur n'est pas admin

**Fichier** : `AdminCheckAspect.java`

```java
@Aspect
@Component
public class AdminCheckAspect {
    @Before("@annotation(IsAdmin)")
    public void checkAdminRole() {
        // Récupère l'utilisateur authentifié
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        // Vérifie isAdmin
        if (!user.isAdmin()) {
            throw new SecurityException("Accès refusé : droits administrateur requis");
        }
    }
}
```

**Avantages** :
- ✅ Vérification déclarative et réutilisable
- ✅ Protège avant même l'entrée dans le contrôleur
- ✅ Facile à appliquer sur plusieurs endpoints

## ✅ 3. Token JWT contient l'information isAdmin

**Fichier** : `JwtService.java`

Le token JWT généré contient le claim `isAdmin` :

```java
public String generateToken(User user) {
    return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("isAdmin", user.isAdmin())  // ← Stocké dans le token
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}
```

**Avantage** :
- ✅ Le statut admin est vérifié au moment de la génération du token
- ✅ Évite une requête en base à chaque appel

## 🎯 Ordre de vérification

Lorsqu'un utilisateur appelle `POST /api/groups` :

```
1. 🔐 JwtAuthFilter extrait le token et charge le User
2. 🛡️ AdminCheckAspect vérifie @IsAdmin avant le contrôleur
   → Si user.isAdmin() == false → SecurityException ❌
3. 🎮 GroupController.createGroup() est exécuté
4. 💼 GroupService.createGroup() vérifie à nouveau isAdmin
   → Double sécurité au niveau métier ✅
5. ✅ Le groupe est créé
```

## 🧪 Tests

### ✅ Cas nominal : Admin crée un groupe

```bash
POST /api/groups
Authorization: Bearer {admin_token}
Body: { "name": "Noël 2025", "type": "noël" }

→ 200 OK ✅
```

### ❌ Cas d'erreur : User simple tente de créer un groupe

```bash
POST /api/groups
Authorization: Bearer {user_token}  # user.isAdmin = false
Body: { "name": "Noël 2025", "type": "noël" }

→ 403 Forbidden ❌
→ "Accès refusé : droits administrateur requis"
```

### ❌ Cas d'erreur : Aucun token

```bash
POST /api/groups
Body: { "name": "Noël 2025", "type": "noël" }

→ 401 Unauthorized ❌
→ "Authentification requise"
```

## 📋 Endpoints protégés par @IsAdmin

| Endpoint | Méthode | Protection |
|----------|---------|------------|
| `/api/groups` | POST | ✅ @IsAdmin + Service |
| `/api/groups` | GET | ✅ @IsAdmin |
| `/api/groups/{id}` | PUT | ✅ @IsAdmin + Service |
| `/api/groups/{id}` | DELETE | ✅ @IsAdmin + Service |
| `/api/groups/{id}/invite` | POST | ✅ @IsAdmin (via InvitationController) |

## 🔧 Configuration requise

### 1. Dépendance Maven (déjà ajoutée)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 2. Activation AOP (automatique avec Spring Boot)

Spring Boot active automatiquement AOP si la dépendance est présente.

## 🎓 Comment ça marche en détail ?

### Flux complet d'une requête

```
Client envoie: POST /api/groups avec Bearer token
         ↓
[1] JwtAuthFilter intercepte la requête
    → Extrait le JWT token
    → Décode le token et récupère userId
    → Charge le User depuis la BDD
    → Met le User dans SecurityContext
         ↓
[2] AdminCheckAspect (@Before advice)
    → Récupère le User depuis SecurityContext
    → Vérifie user.isAdmin()
    → ❌ Si false → lance SecurityException
    → ✅ Si true → continue
         ↓
[3] GroupController.createGroup()
    → @AuthenticationPrincipal injecte le User
    → Appelle groupService.createGroup()
         ↓
[4] GroupService.createGroup()
    → Charge le User à nouveau (sécurité)
    → ❌ Vérifie encore user.isAdmin()
    → ✅ Crée le groupe
    → Ajoute l'admin dans user_groups
    → Retourne GroupResponse
         ↓
[5] Réponse HTTP 200 OK avec le groupe créé
```

## 💡 Bonnes pratiques appliquées

✅ **Defense in depth** : Plusieurs niveaux de sécurité  
✅ **Fail-secure** : Par défaut, l'accès est refusé  
✅ **Least privilege** : Seuls les admins ont accès  
✅ **Separation of concerns** : Sécurité séparée de la logique métier  
✅ **Reusability** : Annotation réutilisable sur d'autres endpoints  

## 🚀 Pour ajouter la protection admin à un nouvel endpoint

```java
@IsAdmin  // ← Ajoutez simplement cette annotation
@PostMapping("/nouvel-endpoint")
public ResponseEntity<?> maMethode(@AuthenticationPrincipal User admin) {
    // Le code ici ne s'exécutera QUE si user.isAdmin() == true
}
```

## 📝 Notes importantes

1. **Token JWT valide** : L'utilisateur DOIT d'abord s'authentifier via `/api/auth/register` ou `/api/auth/login`
2. **isAdmin dans la BDD** : Le champ `is_admin` DOIT être à `true` dans la table `users`
3. **Cache** : Le statut admin est vérifié à chaque requête (pas de cache)
4. **Exception handling** : Les `SecurityException` sont automatiquement converties en HTTP 403 par Spring Security

## 🐛 Debugging

Si un admin ne peut pas créer de groupe :

1. Vérifiez le token JWT : `https://jwt.io`
2. Vérifiez la BDD : `SELECT id, email, is_admin FROM users WHERE email = '...'`
3. Vérifiez les logs : `SecurityException` avec le message d'erreur
4. Testez avec Bruno : `auth/register.bru` puis `groups/create-group.bru`

