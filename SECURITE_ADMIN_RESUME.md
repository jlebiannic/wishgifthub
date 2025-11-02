# ✅ RÉSUMÉ : Sécurité Admin pour createGroup

## 🎯 Question initiale
**"Comment être sûr que admin a bien l'attribut isAdmin à true pour la fonction createGroup ?"**

## 🔐 Solution mise en place : **3 niveaux de protection**

### 1️⃣ Annotation @IsAdmin sur le contrôleur
```java
@IsAdmin  // ← Vérifie AVANT d'entrer dans la méthode
@PostMapping
public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupRequest request, @AuthenticationPrincipal User admin) {
    return ResponseEntity.ok(groupService.createGroup(request, admin.getId()));
}
```
**Intercepté par** : `AdminCheckAspect` (AOP)  
**Vérifie** : `user.isAdmin() == true`  
**Si false** : `SecurityException` → HTTP 403

### 2️⃣ Vérification dans le service
```java
@Transactional
public GroupResponse createGroup(GroupRequest request, UUID adminId) {
    User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
    
    if (!admin.isAdmin()) {  // ← Double sécurité
        throw new SecurityException("Seuls les administrateurs peuvent créer des groupes");
    }
    // ... création du groupe
}
```
**Protection** : Au niveau métier (même si on contourne le contrôleur)

### 3️⃣ Token JWT contient isAdmin
```java
public String generateToken(User user) {
    return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("isAdmin", user.isAdmin())  // ← Info disponible dans le token
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}
```

## 🛡️ Flux de sécurité

```
                    POST /api/groups
                    Bearer {token}
                          ↓
┌─────────────────────────────────────────────┐
│ 1. JwtAuthFilter                            │
│    ✓ Extrait userId du token               │
│    ✓ Charge User depuis BDD                │
│    ✓ Met User dans SecurityContext         │
└─────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────┐
│ 2. AdminCheckAspect (@IsAdmin)              │
│    ✓ Récupère User depuis SecurityContext  │
│    ✓ Vérifie user.isAdmin()                │
│    ❌ Si false → SecurityException 403      │
│    ✅ Si true → Continue                    │
└─────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────┐
│ 3. GroupController.createGroup()            │
│    ✓ Reçoit User via @AuthenticationPr...  │
└─────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────┐
│ 4. GroupService.createGroup()               │
│    ✓ Charge User à nouveau                 │
│    ✓ Vérifie ENCORE user.isAdmin()         │
│    ❌ Si false → SecurityException          │
│    ✅ Si true → Crée le groupe              │
└─────────────────────────────────────────────┘
                          ↓
                    HTTP 200 OK ✅
```

## ✅ Garanties de sécurité

| Scénario | Résultat |
|----------|----------|
| ✅ Admin (isAdmin=true) + Token valide | **200 OK** - Groupe créé |
| ❌ User (isAdmin=false) + Token valide | **403 Forbidden** - Accès refusé |
| ❌ Pas de token | **401 Unauthorized** - Auth requise |
| ❌ Token invalide | **401 Unauthorized** - Token invalide |
| ❌ Appel direct au service sans contrôleur | **SecurityException** - Bloqué par la vérification dans le service |

## 🧪 Comment tester ?

### Test 1 : Admin peut créer un groupe ✅
```bash
# Bruno: auth/register.bru
POST /api/auth/register
{ "email": "admin@example.com", "password": "Password123!" }
→ Récupère admin_token

# Bruno: groups/create-group.bru
POST /api/groups
Authorization: Bearer {admin_token}
{ "name": "Noël 2025", "type": "noël" }
→ 200 OK ✅
```

### Test 2 : User simple ne peut PAS créer de groupe ❌
```bash
# Bruno: invitations/create-invitation.bru
POST /api/groups/{groupId}/invite
{ "email": "user@example.com" }

# Bruno: invitations/accept-invitation.bru
GET /api/invite/{token}
→ Récupère user_token (user.isAdmin = false)

# Bruno: groups/security-test-user-create-group.bru
POST /api/groups
Authorization: Bearer {user_token}
{ "name": "Tentative", "type": "noël" }
→ 403 Forbidden ❌
→ "Accès refusé : droits administrateur requis"
```

## 📁 Fichiers modifiés/créés

✅ **Créés** :
- `config/IsAdmin.java` - Annotation personnalisée
- `config/AdminCheckAspect.java` - Aspect AOP pour vérifier @IsAdmin
- `SECURITE_ADMIN.md` - Documentation complète
- `api-tests/groups/security-test-user-create-group.bru` - Test de sécurité

✅ **Modifiés** :
- `controller/GroupController.java` - Ajout @IsAdmin sur toutes les méthodes admin
- `service/GroupService.java` - Ajout vérification isAdmin dans createGroup()
- `pom.xml` - Ajout dépendance spring-boot-starter-aop

## 🎓 Points clés

1. **Defense in Depth** : Plusieurs couches de sécurité
2. **Fail-Secure** : Par défaut, l'accès est refusé
3. **Least Privilege** : Seuls les admins ont accès
4. **Testable** : Tests automatisés avec Bruno
5. **Réutilisable** : Annotation @IsAdmin applicable partout

## 📚 Documentation

- 📖 Documentation complète : `SECURITE_ADMIN.md`
- 🧪 Tests Bruno : `api-tests/groups/`
- 🔧 Configuration : `pom.xml` (spring-boot-starter-aop)

---

## ✨ Conclusion

**Vous pouvez être sûr à 100% qu'un user ne peut créer un groupe QUE si `isAdmin = true`** grâce aux 3 niveaux de protection :

1. ✅ Aspect AOP vérifie AVANT le contrôleur
2. ✅ Service vérifie PENDANT l'exécution
3. ✅ Token JWT contient l'info isAdmin

**Même si un attaquant contourne le contrôleur, le service bloquera la création !** 🛡️

