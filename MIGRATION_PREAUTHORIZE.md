# 🔐 Migration vers @PreAuthorize - Spring Security natif

## ✅ Pourquoi @PreAuthorize est mieux que @IsAdmin custom

### Avantages de @PreAuthorize

| Critère | @IsAdmin (custom) | @PreAuthorize (Spring Security) |
|---------|-------------------|----------------------------------|
| **Standard** | ❌ Custom | ✅ Standard Spring Security |
| **Maintenance** | ❌ Code à maintenir | ✅ Maintenu par Spring |
| **Documentation** | ❌ Peu de docs | ✅ Très bien documenté |
| **Fonctionnalités** | ❌ Basique | ✅ Expressions SpEL puissantes |
| **Intégration** | ⚠️ Nécessite AOP custom | ✅ Intégré natif |
| **Testabilité** | ⚠️ Nécessite mocks | ✅ Support natif dans les tests |
| **Performance** | ⚠️ Aspect custom | ✅ Optimisé par Spring |

---

## 🔄 Ce qui a été changé

### 1️⃣ Activation de @EnableMethodSecurity

**Fichier** : `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // ← Active @PreAuthorize
public class SecurityConfig {
    // ...
}
```

### 2️⃣ Ajout des Authorities dans JwtAuthFilter

**Fichier** : `JwtAuthFilter.java`

```java
// Avant
UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        user, null, null);  // ← Pas d'authorities

// Après
List<SimpleGrantedAuthority> authorities = user.isAdmin() 
    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
    : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        user, null, authorities);  // ← Avec authorities
```

### 3️⃣ Remplacement de @IsAdmin par @PreAuthorize

**Fichier** : `GroupController.java`

```java
// Avant
@IsAdmin
@PostMapping
public ResponseEntity<GroupResponse> createGroup(...) {

// Après
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<GroupResponse> createGroup(...) {
```

---

## 🎯 Fonctionnement de @PreAuthorize

### Expression SpEL de base

```java
@PreAuthorize("hasRole('ADMIN')")         // Vérifie si l'user a ROLE_ADMIN
@PreAuthorize("hasRole('USER')")          // Vérifie si l'user a ROLE_USER
@PreAuthorize("hasAnyRole('ADMIN','USER')") // Vérifie l'un OU l'autre
@PreAuthorize("isAuthenticated()")        // Vérifie juste l'authentification
```

### Expressions avancées

```java
// Accès à l'objet User authentifié
@PreAuthorize("principal.id == #userId")

// Expressions complexes
@PreAuthorize("hasRole('ADMIN') and principal.email == 'admin@example.com'")

// Vérifier un paramètre de méthode
@PreAuthorize("#groupId == authentication.principal.activeGroupId")
```

---

## 🛡️ Les 3 niveaux de sécurité (version @PreAuthorize)

### 🥇 NIVEAU 1 : @PreAuthorize au niveau du contrôleur

```java
@PreAuthorize("hasRole('ADMIN')")  // ← Vérifie AVANT la méthode
@PostMapping
public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupRequest request, @AuthenticationPrincipal User admin) {
    return ResponseEntity.ok(groupService.createGroup(request, admin.getId()));
}
```

**Protection** : Évalue l'expression SpEL AVANT l'exécution  
**Vérifie** : Que l'utilisateur a bien `ROLE_ADMIN` dans ses authorities  
**Si false** : `AccessDeniedException` → HTTP 403  
**Géré par** : Spring Security (natif)

---

### 🥈 NIVEAU 2 : Vérification dans le Service (inchangée)

```java
@Transactional
public GroupResponse createGroup(GroupRequest request, UUID adminId) {
    User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
    
    // Double sécurité métier
    if (!admin.isAdmin()) {
        throw new SecurityException("Seuls les administrateurs peuvent créer des groupes");
    }
    // ... création du groupe
}
```

**Protection** : Au niveau métier (défense en profondeur)  
**Toujours utile** : Même avec @PreAuthorize

---

### 🥉 NIVEAU 3 : Authorities basées sur isAdmin

```java
List<SimpleGrantedAuthority> authorities = user.isAdmin() 
    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
    : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
```

**Protection** : Les authorities sont définies à l'authentification  
**Utilisées par** : @PreAuthorize pour évaluer `hasRole('ADMIN')`

---

## 📊 Flux de sécurité avec @PreAuthorize

```
                    POST /api/groups
                    Bearer {token}
                          ↓
┌─────────────────────────────────────────────┐
│ 1. JwtAuthFilter                            │
│    ✓ Extrait userId du token               │
│    ✓ Charge User depuis BDD                │
│    ✓ Ajoute ROLE_ADMIN ou ROLE_USER        │
│    ✓ Met User + authorities dans Security  │
└─────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────┐
│ 2. @PreAuthorize("hasRole('ADMIN')")       │
│    ✓ Évalue l'expression SpEL              │
│    ✓ Vérifie authorities contient ROLE_... │
│    ❌ Si false → AccessDeniedException 403  │
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
│    ✓ Double vérification isAdmin()         │
│    ✅ Si true → Crée le groupe              │
└─────────────────────────────────────────────┘
                          ↓
                    HTTP 200 OK ✅
```

---

## 🧪 Tests

### Test 1 : Admin peut créer un groupe ✅

```bash
POST /api/auth/register
{ "email": "admin@example.com", "password": "..." }
→ User créé avec isAdmin=true
→ Token JWT retourné

POST /api/groups
Authorization: Bearer {admin_token}
{ "name": "Noël 2025", "type": "noël" }
→ 200 OK ✅
```

**Pourquoi ça marche ?**
1. JwtAuthFilter ajoute `ROLE_ADMIN` aux authorities
2. @PreAuthorize("hasRole('ADMIN')") → true ✅
3. Service vérifie isAdmin() → true ✅

### Test 2 : User ne peut PAS créer de groupe ❌

```bash
GET /api/invite/{token}
→ User créé avec isAdmin=false
→ Token JWT retourné

POST /api/groups
Authorization: Bearer {user_token}
{ "name": "Tentative", "type": "noël" }
→ 403 Forbidden ❌
```

**Pourquoi ça échoue ?**
1. JwtAuthFilter ajoute `ROLE_USER` aux authorities
2. @PreAuthorize("hasRole('ADMIN')") → false ❌
3. AccessDeniedException → HTTP 403

---

## 🎓 Expressions @PreAuthorize utiles

### Vérifications de rôles

```java
// User a le rôle ADMIN
@PreAuthorize("hasRole('ADMIN')")

// User a le rôle USER
@PreAuthorize("hasRole('USER')")

// User a au moins un des rôles
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")

// User a tous les rôles
@PreAuthorize("hasRole('ADMIN') and hasRole('MODERATOR')")
```

### Accès au principal (User authentifié)

```java
// User peut accéder à ses propres données
@PreAuthorize("principal.id == #userId")

// Admin OU propriétaire peut accéder
@PreAuthorize("hasRole('ADMIN') or principal.id == #userId")

// Vérifier une propriété du User
@PreAuthorize("principal.isAdmin()")  // Équivalent à hasRole('ADMIN')
@PreAuthorize("principal.email == 'admin@example.com'")
```

### Avec paramètres de méthode

```java
@PreAuthorize("#groupId != null")
public void deleteGroup(@PathVariable UUID groupId) { }

// Vérifier propriété d'un objet
@PreAuthorize("#wish.userId == principal.id")
public void updateWish(@RequestBody Wish wish) { }
```

---

## 📦 Dépendances requises

### pom.xml (déjà présentes)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Note** : Plus besoin de `spring-boot-starter-aop` car @PreAuthorize est natif dans Spring Security.

---

## 🔧 Configuration complète

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // ← Crucial !
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/invite/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## ✅ Avantages de cette approche

### 1. Standard Spring Security ✅
- Utilisé par des millions de projets
- Très bien testé et documenté
- Mises à jour de sécurité régulières

### 2. Expressions SpEL puissantes ✅
```java
@PreAuthorize("hasRole('ADMIN') and #groupId == principal.activeGroupId")
```

### 3. Support natif des tests ✅
```java
@Test
@WithMockUser(roles = "ADMIN")
public void testCreateGroup_asAdmin() {
    // Test avec un user admin
}

@Test
@WithMockUser(roles = "USER")
public void testCreateGroup_asUser_shouldFail() {
    // Test avec un user simple (doit échouer)
}
```

### 4. Intégration avec Spring Method Security ✅
- @PostAuthorize : Vérification APRÈS l'exécution
- @PreFilter : Filtre les paramètres AVANT
- @PostFilter : Filtre le résultat APRÈS

---

## 🆚 Comparaison finale

### Avec @IsAdmin (custom)
```java
@IsAdmin  // Custom annotation
@PostMapping
public ResponseEntity<GroupResponse> createGroup(...) {
```

**Nécessite** :
- ✅ IsAdmin.java (annotation custom)
- ✅ AdminCheckAspect.java (aspect AOP custom)
- ✅ spring-boot-starter-aop (dépendance)

### Avec @PreAuthorize (Spring Security)
```java
@PreAuthorize("hasRole('ADMIN')")  // Standard Spring Security
@PostMapping
public ResponseEntity<GroupResponse> createGroup(...) {
```

**Nécessite** :
- ✅ @EnableMethodSecurity dans SecurityConfig
- ✅ Authorities dans JwtAuthFilter
- ❌ **Pas de code custom**
- ❌ **Pas de dépendance AOP supplémentaire**

---

## 🎯 Conclusion

### Migration effectuée ✅

1. ✅ Ajout de `@EnableMethodSecurity` dans SecurityConfig
2. ✅ Ajout des authorities (`ROLE_ADMIN` / `ROLE_USER`) dans JwtAuthFilter
3. ✅ Remplacement de `@IsAdmin` par `@PreAuthorize("hasRole('ADMIN')")`
4. ✅ Peut supprimer IsAdmin.java et AdminCheckAspect.java (optionnel)
5. ✅ Peut retirer spring-boot-starter-aop du pom.xml (optionnel)

### Avantages obtenus ✅

- ✅ **Code plus standard** et maintenable
- ✅ **Meilleure intégration** avec Spring Security
- ✅ **Expressions SpEL puissantes** pour des règles complexes
- ✅ **Support natif des tests** avec @WithMockUser
- ✅ **Moins de code custom** à maintenir

### Sécurité maintenue ✅

- ✅ Même niveau de sécurité (3 couches)
- ✅ @PreAuthorize vérifie AVANT la méthode
- ✅ Service vérifie PENDANT l'exécution
- ✅ Authorities basées sur isAdmin

---

**La migration vers @PreAuthorize est une amélioration significative !** 🎉

