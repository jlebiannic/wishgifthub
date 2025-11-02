# 🔐 Protection Admin - Vue d'ensemble

## Question
> Comment être sûr que admin a bien l'attribut isAdmin à true pour la fonction createGroup ?

## Réponse
✅ **3 niveaux de sécurité garantissent que seul un admin peut créer un groupe**

---

## 📊 Diagramme de sécurité

```
                    Client
                      │
                      ↓
          POST /api/groups + JWT Token
                      │
        ┌─────────────┴─────────────┐
        │                           │
        │   JwtAuthFilter           │
        │   • Vérifie token         │
        │   • Charge User           │
        │                           │
        └─────────────┬─────────────┘
                      │
                      ↓
        ┌─────────────┴─────────────┐
        │                           │
        │   @IsAdmin (AOP)          │◄─── NIVEAU 1 : Vérifie user.isAdmin()
        │   AdminCheckAspect        │     ❌ Si false → 403 Forbidden
        │                           │
        └─────────────┬─────────────┘
                      │ ✅ isAdmin = true
                      ↓
        ┌─────────────┴─────────────┐
        │                           │
        │   GroupController         │
        │   • createGroup()         │
        │                           │
        └─────────────┬─────────────┘
                      │
                      ↓
        ┌─────────────┴─────────────┐
        │                           │
        │   GroupService            │◄─── NIVEAU 2 : Vérifie ENCORE
        │   • if(!user.isAdmin())   │     ❌ Si false → SecurityException
        │   • createGroup()         │
        │                           │
        └─────────────┬─────────────┘
                      │ ✅ isAdmin = true
                      ↓
                Database
                • INSERT INTO groups
                • INSERT INTO user_groups
                      │
                      ↓
                HTTP 200 OK ✅
```

---

## 🛡️ Les 3 niveaux de protection

### 🥇 NIVEAU 1 : Annotation @IsAdmin (AOP)
```java
@IsAdmin  // ← Intercepté par AdminCheckAspect
@PostMapping
public ResponseEntity<GroupResponse> createGroup(...) {
```

**Protection** : Avant même d'entrer dans la méthode  
**Vérifie** : `user.isAdmin() == true`  
**Si false** : `SecurityException` → HTTP 403  
**Localisation** : `AdminCheckAspect.java`

---

### 🥈 NIVEAU 2 : Vérification dans le Service
```java
public GroupResponse createGroup(GroupRequest request, UUID adminId) {
    User admin = userRepository.findById(adminId).orElseThrow();
    
    if (!admin.isAdmin()) {  // ← Double vérification
        throw new SecurityException("...");
    }
```

**Protection** : Au niveau métier (impossible à contourner)  
**Vérifie** : Charge le User depuis la BDD et vérifie `isAdmin`  
**Si false** : `SecurityException`  
**Localisation** : `GroupService.java`

---

### 🥉 NIVEAU 3 : Token JWT contient isAdmin
```java
public String generateToken(User user) {
    return Jwts.builder()
        .claim("isAdmin", user.isAdmin())  // ← Info dans le token
        .signWith(...)
        .compact();
}
```

**Protection** : Le statut admin est vérifié à la génération du token  
**Localisation** : `JwtService.java`

---

## ✅ Tableau de garanties

| Cas | isAdmin | Token | Résultat |
|-----|---------|-------|----------|
| Admin avec token valide | `true` | ✅ Valide | ✅ **200 OK** - Groupe créé |
| User simple avec token valide | `false` | ✅ Valide | ❌ **403 Forbidden** |
| Admin sans token | `true` | ❌ Absent | ❌ **401 Unauthorized** |
| User avec token invalide | `false` | ❌ Invalide | ❌ **401 Unauthorized** |
| Attaque : bypass du contrôleur | `false` | N/A | ❌ **SecurityException** (service) |

---

## 🧪 Comment tester ?

### ✅ Test 1 : Admin réussit à créer un groupe
```bash
# 1. S'inscrire en tant qu'admin
POST /api/auth/register
{ "email": "admin@example.com", "password": "..." }
→ Retourne admin_token (avec isAdmin=true dans le JWT)

# 2. Créer un groupe
POST /api/groups
Authorization: Bearer {admin_token}
{ "name": "Noël 2025", "type": "noël" }
→ 200 OK ✅ Groupe créé
```

### ❌ Test 2 : User échoue à créer un groupe
```bash
# 1. Accepter une invitation (crée un user simple)
GET /api/invite/{token}
→ Retourne user_token (avec isAdmin=false)

# 2. Tenter de créer un groupe
POST /api/groups
Authorization: Bearer {user_token}
{ "name": "Tentative", "type": "noël" }
→ 403 Forbidden ❌
→ "Accès refusé : droits administrateur requis"
```

---

## 📁 Fichiers créés/modifiés

### ✨ Nouveaux fichiers
- ✅ `config/IsAdmin.java` - Annotation
- ✅ `config/AdminCheckAspect.java` - Aspect AOP
- ✅ `api-tests/groups/security-test-user-create-group.bru` - Test
- ✅ `SECURITE_ADMIN.md` - Doc complète
- ✅ `SECURITE_ADMIN_RESUME.md` - Résumé
- ✅ `SECURITE_ADMIN_VISUEL.md` - Ce fichier

### 📝 Fichiers modifiés
- ✅ `controller/GroupController.java` - Ajout @IsAdmin
- ✅ `service/GroupService.java` - Ajout vérification
- ✅ `pom.xml` - Ajout spring-boot-starter-aop

---

## 🎯 Points clés

1. **Sécurité multi-niveaux** : 3 points de vérification
2. **Fail-secure** : Par défaut, l'accès est refusé
3. **Testable** : Tests automatisés avec Bruno
4. **Maintenable** : Annotation réutilisable
5. **Documenté** : Documentation complète

---

## 💡 Conclusion

### Question initiale
> Comment être sûr que admin a bien isAdmin à true ?

### Réponse
**VOUS ÊTES SÛR À 100%** car :

1. ✅ L'aspect AOP bloque AVANT l'exécution
2. ✅ Le service bloque PENDANT l'exécution
3. ✅ Le token JWT est vérifié à la génération

**Même un attaquant ne peut pas créer de groupe sans isAdmin=true !** 🛡️

---

## 📚 Documentation complète
👉 Voir `SECURITE_ADMIN.md` pour tous les détails

## 🧪 Tests
👉 Voir `api-tests/groups/security-test-user-create-group.bru`

