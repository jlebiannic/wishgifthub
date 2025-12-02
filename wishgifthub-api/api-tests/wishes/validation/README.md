# Tests de Validation des URLs

Ce dossier contient les tests Bruno pour vérifier que la validation des URLs fonctionne correctement via l'annotation `@ValidUrl`.

## Vue d'ensemble

La validation des URLs est effectuée à deux niveaux :
1. **Bean Validation** : Via l'annotation `@ValidUrl` sur les champs `url` et `imageUrl` du `WishRequest`
2. **Service** : Validation de la longueur maximale (2048 caractères) dans `WishService`

## Tests de validation

### Tests de succès (URLs valides)

| Test | Description | URL testée |
|------|-------------|------------|
| `01-valid-url-http.bru` | URL HTTP valide | `http://example.com/product` |
| `02-valid-url-https.bru` | URL HTTPS valide | `https://www.amazon.fr/product/B0DZDQ7SQK` |
| `03-valid-url-empty.bru` | Champs optionnels vides | `null` / non fourni |

### Tests d'échec (URLs invalides)

| Test | Description | URL testée | Statut attendu |
|------|-------------|------------|----------------|
| `04-invalid-url-no-protocol.bru` | URL sans protocole | `example.com/product` | 400 |
| `05-invalid-url-ftp-protocol.bru` | Protocole FTP non supporté | `ftp://ftp.example.com/file.zip` | 400 |
| `06-invalid-url-malformed.bru` | URL malformée sans host | `http://` | 400 |
| `07-invalid-url-javascript.bru` | Protocole JavaScript (XSS) | `javascript:alert('XSS')` | 400 |
| `08-invalid-imageurl-no-protocol.bru` | Image URL sans protocole | `cdn.example.com/image.jpg` | 400 |
| `09-invalid-url-too-long.bru` | URL dépassant 2048 caractères | URL > 2048 chars | 400 |

## Règles de validation

Une URL est **valide** si :
- ✅ Elle utilise le protocole `http://` ou `https://`
- ✅ Elle contient un nom de domaine (host)
- ✅ Elle est bien formée selon la RFC URI
- ✅ Elle ne dépasse pas 2048 caractères
- ✅ OU elle est `null` / non fournie (champs optionnels)

Une URL est **invalide** si :
- ❌ Le protocole est manquant
- ❌ Le protocole n'est pas HTTP ou HTTPS
- ❌ Elle n'a pas de host
- ❌ Elle dépasse 2048 caractères
- ❌ Elle est malformée

## Exécution des tests

### Prérequis

1. Variables d'environnement configurées :
   - `base_url` : URL de base de l'API (ex: `http://localhost:8080`)
   - `user_token` : Token JWT d'un utilisateur
   - `group_id` : ID d'un groupe auquel l'utilisateur appartient

2. Backend démarré avec la validation activée

### Exécuter tous les tests

```bash
# Depuis le dossier api-tests
bruno run wishes/validation
```

### Exécuter un test spécifique

```bash
bruno run wishes/validation/04-invalid-url-no-protocol.bru
```

## Résultats attendus

### Tests de succès
- **Statut HTTP** : 200 OK
- **Réponse** : Objet `WishResponse` avec l'URL enregistrée
- **Comportement** : Le souhait est créé puis supprimé automatiquement (nettoyage)

### Tests d'échec
- **Statut HTTP** : 400 Bad Request
- **Réponse** : Objet d'erreur contenant un message explicite
- **Message** : Doit mentionner "URL", "valide", "valid", "http", etc.

## Implémentation technique

### Backend

**Annotation ValidUrl**
```java
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {UrlValidator.class, UriValidator.class})
public @interface ValidUrl {
    String message() default "L'URL n'est pas valide (doit commencer par http:// ou https://)";
}
```

**Générée automatiquement dans WishRequest**
```java
@com.wishgifthub.validation.ValidUrl
private URI url = null;

@com.wishgifthub.validation.ValidUrl
private URI imageUrl = null;
```

**Validation automatique**
```java
@RestController
@Validated
public class WishController implements SouhaitsApi {
    public ResponseEntity<WishResponse> addWish(
        @PathVariable("groupId") UUID groupId,
        @Valid @RequestBody WishRequest wishRequest  // Déclenche Bean Validation
    ) {
        // La validation se fait automatiquement avant d'arriver ici
    }
}
```

## Sécurité

Ces tests vérifient également la sécurité :
- ❌ Empêche les URLs JavaScript (XSS)
- ❌ Empêche les protocoles non sécurisés (ftp, file, etc.)
- ❌ Empêche les URLs trop longues (DoS)

## Maintenance

Si vous ajoutez de nouveaux cas de validation :
1. Créez un nouveau fichier `.bru` avec un numéro de séquence
2. Suivez le même format que les tests existants
3. Mettez à jour ce README

## Troubleshooting

### Les tests d'échec retournent 200 au lieu de 400
- Vérifier que `@Validated` est présent sur le contrôleur
- Vérifier que `@Valid` est présent dans l'interface générée
- Vérifier que les validateurs `UrlValidator` et `UriValidator` sont bien chargés

### Les tests de succès échouent
- Vérifier que les URLs sont bien formées
- Vérifier que le backend accepte HTTP et HTTPS
- Vérifier la configuration du `Validator` Bean

### Nettoyage échoue
- Les souhaits créés lors des tests de succès sont automatiquement supprimés
- Si le nettoyage échoue, supprimer manuellement via l'API ou la base de données

