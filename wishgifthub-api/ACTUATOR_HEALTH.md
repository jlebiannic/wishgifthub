# Configuration Actuator Health - WishGiftHub Backend

## ✅ Configuration activée

Spring Boot Actuator est maintenant configuré pour le healthcheck Docker.

## 📋 Ce qui a été configuré

### 1. Dépendance Maven (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. Configuration (application.properties)

```properties
# Actuator - Healthcheck pour Docker
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
management.health.db.enabled=true
```

**Explications :**
- `management.endpoints.web.exposure.include=health,info` : Expose les endpoints /actuator/health et /actuator/info
- `management.endpoint.health.show-details=when-authorized` : Affiche les détails uniquement si authentifié
- `management.health.db.enabled=true` : Vérifie la connexion à la base de données

### 3. Sécurité (SecurityConfig.java)

```java
.requestMatchers(
    "/api/auth/**",
    "/api/invite/**",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/actuator/health/**"  // ⬅️ Endpoint accessible sans authentification
).permitAll()
```

## 🧪 Tester le healthcheck

### En local (sans Docker)

```bash
# Démarrer l'application
mvn spring-boot:run

# Tester le healthcheck
curl http://localhost:8080/actuator/health
```

**Réponse attendue :**
```json
{
  "status": "UP"
}
```

### Avec Docker

```bash
# Construire et démarrer
cd wishgifthub-api
docker-compose up --build -d

# Vérifier l'état de santé
docker ps
# Doit afficher: Up X minutes (healthy)

# Tester manuellement
curl http://localhost:8080/actuator/health
```

### Détails du healthcheck (avec authentification)

```bash
# Se connecter et obtenir un token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"password"}' \
  | jq -r '.token')

# Voir les détails du healthcheck
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/actuator/health
```

**Réponse détaillée :**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

## 🐳 Healthcheck Docker

Le Dockerfile utilise ce endpoint pour vérifier la santé :

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

### États possibles

- **starting** 🟡 : Pendant les 60 premières secondes
- **healthy** 🟢 : Le healthcheck réussit
- **unhealthy** 🔴 : Le healthcheck échoue 3 fois de suite

### Vérifier l'état

```bash
# État actuel
docker ps

# Historique des healthchecks
docker inspect wishgifthub-backend --format='{{json .State.Health}}' | jq

# Logs du healthcheck
docker inspect wishgifthub-backend | grep -A 20 Health
```

## 📊 Endpoints Actuator disponibles

| Endpoint | Description | Authentification |
|----------|-------------|------------------|
| `/actuator/health` | État de santé global | ❌ Non requise |
| `/actuator/info` | Informations sur l'application | ❌ Non requise |

## 🔧 Personnalisation

### Ajouter d'autres endpoints

Modifier `application.properties` :

```properties
# Exposer plus d'endpoints (ATTENTION : sécurité !)
management.endpoints.web.exposure.include=health,info,metrics,env

# Ou tous les endpoints (DEV uniquement)
management.endpoints.web.exposure.include=*
```

### Personnaliser le healthcheck

Créer un indicateur de santé personnalisé :

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Votre logique métier
        boolean isHealthy = checkCustomCondition();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("custom", "Everything is fine")
                .build();
        } else {
            return Health.down()
                .withDetail("custom", "Something went wrong")
                .build();
        }
    }
    
    private boolean checkCustomCondition() {
        // Votre vérification
        return true;
    }
}
```

## 🚨 Troubleshooting

### Erreur 404 sur /actuator/health

```bash
# Vérifier que Actuator est bien dans les dépendances
mvn dependency:tree | grep actuator

# Vérifier les logs au démarrage
# Doit afficher : "Exposing 2 endpoint(s) beneath base path '/actuator'"
```

### Healthcheck échoue dans Docker

```bash
# Vérifier que wget est installé
docker exec wishgifthub-backend which wget

# Tester manuellement dans le container
docker exec wishgifthub-backend wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health

# Vérifier les logs Spring Boot
docker logs wishgifthub-backend
```

### Base de données marquée DOWN

```bash
# Vérifier la connexion DB
docker exec wishgifthub-backend nc -zv postgres 5432

# Voir les logs Flyway
docker logs wishgifthub-backend | grep Flyway
```

## ✅ Checklist de validation

- [ ] Dépendance `spring-boot-starter-actuator` dans pom.xml
- [ ] Configuration Actuator dans application.properties
- [ ] Endpoint /actuator/health autorisé dans SecurityConfig
- [ ] `curl http://localhost:8080/actuator/health` retourne `{"status":"UP"}`
- [ ] `docker ps` affiche (healthy) après 60 secondes
- [ ] Healthcheck visible avec `docker inspect`

---

**Actuator Health est maintenant actif ! ✅**

