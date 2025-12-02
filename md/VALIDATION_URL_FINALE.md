# Validation des URLs avec @ValidUrl - Documentation Finale

## Date
2 décembre 2025

## Résumé

Implémentation d'une validation robuste des URLs (url et imageUrl) lors de la saisie d'un souhait, côté frontend ET backend, en utilisant l'annotation Bean Validation personnalisée `@ValidUrl`.

## Architecture de la solution

### Génération automatique via OpenAPI

L'annotation `@ValidUrl` est ajoutée automatiquement sur les champs générés grâce à l'extension OpenAPI `x-field-extra-annotation`.

```yaml
# wishgifthub-openapi/src/main/resources/openapi/schemas/requests.yml
url:
  type: string
  format: uri
  maxLength: 2048
  x-field-extra-annotation: "@com.wishgifthub.validation.ValidUrl"
```

**Résultat généré :**
```java
public class WishRequest {
    @com.wishgifthub.validation.ValidUrl
    private URI url = null;
    
    @com.wishgifthub.validation.ValidUrl
    private URI imageUrl = null;
}
```

### Composants backend

#### 1. Annotation @ValidUrl
**Fichier:** `src/main/java/com/wishgifthub/validation/ValidUrl.java`

```java
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {UrlValidator.class, UriValidator.class})
public @interface ValidUrl {
    String message() default "L'URL n'est pas valide (doit commencer par http:// ou https://)";
    // ...
}
```

#### 2. Validateurs

**UrlValidator** : Pour les String
**UriValidator** : Pour les java.net.URI

```java
public class UriValidator implements ConstraintValidator<ValidUrl, URI> {
    @Override
    public boolean isValid(URI value, ConstraintValidatorContext context) {
        if (value == null) return true;
        
        String scheme = value.getScheme();
        if (scheme == null) return false;
        
        String schemeLower = scheme.toLowerCase();
        if (!"http".equals(schemeLower) && !"https".equals(schemeLower)) {
            return false;
        }
        
        return value.getHost() != null && !value.getHost().isEmpty();
    }
}
```

#### 3. Validation automatique

**Contrôleur :**
```java
@RestController
@Validated  // Active Bean Validation
public class WishController implements SouhaitsApi {
    // La validation se fait automatiquement via @Valid dans l'interface
}
```

**Interface générée :**
```java
ResponseEntity<WishResponse> addWish(
    @PathVariable("groupId") UUID groupId,
    @Valid @RequestBody WishRequest wishRequest  // Déclenche la validation
);
```

#### 4. Service (validations complémentaires)

**WishService** vérifie uniquement la longueur des URLs :
```java
if (request.getUrl() != null && request.getUrl().toString().length() > 2048) {
    throw new BusinessRuleException("L'URL du produit ne peut pas dépasser 2048 caractères");
}
```

La validation du format (HTTP/HTTPS) est faite automatiquement par `@ValidUrl`.

### Frontend

**Fichier:** `src/components/AddWishDialog.vue`

Validation côté client avec les mêmes règles :

```typescript
function isValidUrl(urlString: string): boolean {
  if (!urlString) return true;
  try {
    const url = new URL(urlString);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

const rules = {
  url: [
    (v: string) => !v || v.length <= 2048 || 'L\'URL ne peut pas dépasser 2048 caractères',
    (v: string) => isValidUrl(v) || 'L\'URL n\'est pas valide (doit commencer par http:// ou https://)'
  ]
}
```

## Flux de validation complet

### 1. Saisie utilisateur (Frontend)
- L'utilisateur saisit une URL
- Validation en temps réel avec affichage d'erreurs
- Empêche la soumission si invalide

### 2. Requête HTTP
- Le formulaire frontend envoie la requête au backend

### 3. Contrôleur Spring (Backend)
- Spring MVC détecte `@Valid` sur le paramètre `wishRequest`
- Déclenche Bean Validation sur tous les champs

### 4. Bean Validation
- Valide `@NotNull`, `@Size` sur `giftName`
- Valide `@Size` sur `description`, `price`
- **Valide `@ValidUrl` sur `url` et `imageUrl`** ← Notre validation

### 5. Résultat
- **Si violation :** `MethodArgumentNotValidException` → HTTP 400 Bad Request
- **Si valide :** Exécution du service `createWish()`

### 6. Service métier
- Validations complémentaires (longueur des URLs)
- Persistance en base de données

## Règles de validation

Une URL est **valide** si :
- ✅ Elle utilise le protocole `http://` ou `https://`
- ✅ Elle contient un nom de domaine (host)
- ✅ Elle est bien formée selon la RFC URI
- ✅ Elle ne dépasse pas 2048 caractères
- ✅ OU elle est `null` ou vide (champs optionnels)

Une URL est **invalide** si :
- ❌ Protocole manquant : `example.com`
- ❌ Protocole non supporté : `ftp://example.com`
- ❌ Pas de host : `http://`
- ❌ Trop longue : > 2048 caractères
- ❌ Malformée : `http:/ /invalid`

## Avantages de cette approche

### 1. **Génération automatique**
- `x-field-extra-annotation` dans OpenAPI
- Pas besoin de modifier les classes générées
- Maintenu automatiquement lors de la régénération

### 2. **Standard Java**
- Utilise Bean Validation (JSR 380)
- Compatible avec tous les frameworks Java

### 3. **Validation déclarative**
- Annotation sur les champs
- Pas de code de validation manuel dans les services
- Séparation des préoccupations

### 4. **Réutilisable**
- `@ValidUrl` peut être utilisée partout dans l'application
- Sur des champs, paramètres, ou méthodes

### 5. **Double validation**
- Frontend : retour immédiat à l'utilisateur
- Backend : sécurité et cohérence des données

### 6. **Testable**
- Les validateurs peuvent être testés unitairement
- Indépendants de la logique métier

## Messages d'erreur

### Frontend
- "L'URL n'est pas valide (doit commencer par http:// ou https://)"
- "L'URL ne peut pas dépasser 2048 caractères"
- "L'image ne peut pas être chargée. Vérifiez que l'URL est correcte et accessible."

### Backend (via Bean Validation)
- "L'URL n'est pas valide (doit commencer par http:// ou https://)" (message par défaut de @ValidUrl)

### Backend (validations manuelles)
- "L'URL du produit ne peut pas dépasser 2048 caractères"
- "L'URL de l'image ne peut pas dépasser 2048 caractères"

## Fichiers modifiés/créés

### Backend
- ✅ `wishgifthub-openapi/src/main/resources/openapi/schemas/requests.yml` (modifié)
- ✅ `wishgifthub-api/src/main/java/com/wishgifthub/validation/ValidUrl.java` (créé)
- ✅ `wishgifthub-api/src/main/java/com/wishgifthub/validation/UrlValidator.java` (créé)
- ✅ `wishgifthub-api/src/main/java/com/wishgifthub/validation/UriValidator.java` (créé)
- ✅ `wishgifthub-api/src/main/java/com/wishgifthub/service/WishService.java` (modifié)
- ✅ `wishgifthub-api/src/main/java/com/wishgifthub/controller/WishController.java` (modifié)
- ❌ `UrlValidationService.java` (supprimé - plus nécessaire)

### Frontend
- ✅ `wishgifthub-ui/src/components/AddWishDialog.vue` (modifié)

## Compilation

```bash
# Backend
cd wishgifthub
mvn clean install -DskipTests -pl wishgifthub-openapi
mvn clean install -DskipTests -pl wishgifthub-api

# Frontend
cd wishgifthub-ui
npm run generate-api
```

Tous les modules compilent sans erreur. ✅

## Tests recommandés

### URLs valides
- `http://example.com` ✅
- `https://example.com/product` ✅
- `https://subdomain.example.com:8080/path?query=value` ✅
- `https://www.amazon.fr/product/B0DZDQ7SQK` ✅
- `` (vide) ✅
- `null` ✅

### URLs invalides
- `example.com` ❌ (pas de protocole)
- `ftp://example.com` ❌ (protocole non supporté)
- `http://` ❌ (pas de host)
- `javascript:alert('xss')` ❌ (protocole interdit)
- URL > 2048 caractères ❌

## Conclusion

La solution utilise pleinement les capacités de Bean Validation et du générateur OpenAPI pour créer une validation robuste, maintenable et conforme aux standards Java. L'utilisation de `x-field-extra-annotation` permet d'ajouter des annotations personnalisées sur les classes générées sans avoir à les modifier manuellement.

**Points clés :**
- ✅ Validation automatique via Bean Validation
- ✅ Génération automatique via OpenAPI
- ✅ Pas de code de validation manuel dans les services
- ✅ Réutilisable et testable
- ✅ Double validation (frontend + backend)

