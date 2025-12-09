# Informations de Version et Build - Backend

## Description

L'API expose les informations de version et de build via Spring Boot Actuator et un endpoint personnalisé.

## Endpoints disponibles

### 1. Endpoint personnalisé `/api/version`

**URL:** `GET /api/version`

**Accès:** Public (pas d'authentification requise)

**Réponse exemple:**
```json
{
  "version": "1.0-SNAPSHOT",
  "group": "com.code",
  "artifact": "wishgifthub-api",
  "name": "wishgifthub-api",
  "buildTimestamp": "2025-12-09T16:30:45.123Z",
  "buildDate": "2025-12-09 17:30:45",
  "git": {
    "branch": "main",
    "commitId": "a1b2c3d",
    "commitIdFull": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0",
    "commitTime": "2025-12-09T16:25:30Z",
    "commitMessage": "Add version endpoint"
  }
}
```

### 2. Actuator Info `/actuator/info`

**URL:** `GET /actuator/info`

**Accès:** Public

**Réponse exemple:**
```json
{
  "app": {
    "name": "WishGiftHub API",
    "description": "API REST pour la gestion des listes de souhaits",
    "encoding": "UTF-8",
    "java": {
      "version": "21"
    }
  },
  "build": {
    "artifact": "wishgifthub-api",
    "name": "wishgifthub-api",
    "time": "2025-12-09T16:30:45.123Z",
    "version": "1.0-SNAPSHOT",
    "group": "com.code"
  },
  "git": {
    "branch": "main",
    "commit": {
      "id": {
        "abbrev": "a1b2c3d",
        "describe": "v1.0.0-1-ga1b2c3d"
      },
      "message": {
        "short": "Add version endpoint"
      },
      "time": "2025-12-09T16:25:30Z"
    },
    "build": {
      "time": "2025-12-09T16:30:45Z"
    }
  },
  "java": {
    "version": "21",
    "vendor": {
      "name": "Oracle Corporation"
    },
    "runtime": {
      "name": "Java(TM) SE Runtime Environment",
      "version": "21.0.1+12-LTS-29"
    },
    "jvm": {
      "name": "Java HotSpot(TM) 64-Bit Server VM",
      "vendor": "Oracle Corporation",
      "version": "21.0.1+12-LTS-29"
    }
  },
  "os": {
    "name": "Windows 11",
    "version": "10.0",
    "arch": "amd64"
  }
}
```

### 3. Actuator Health `/actuator/health`

**URL:** `GET /actuator/health`

**Accès:** Public (détails visibles si authentifié)

**Réponse exemple:**
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
        "path": "/app",
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

## Configuration

### pom.xml

Le plugin Spring Boot génère automatiquement le fichier `build-info.properties` :

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>build-info</id>
            <goals>
                <goal>build-info</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Le plugin Git génère le fichier `git.properties` :

```xml
<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
    <version>9.0.1</version>
    ...
</plugin>
```

### application.properties

```properties
# Actuator - Healthcheck et informations de build
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
management.health.db.enabled=true

# Informations de build exposées via /actuator/info
management.info.build.enabled=true
management.info.env.enabled=true
management.info.git.enabled=true
management.info.git.mode=full
management.info.java.enabled=true
management.info.os.enabled=true

# Métadonnées personnalisées de l'application
info.app.name=WishGiftHub API
info.app.description=API REST pour la gestion des listes de souhaits
```

## Build

Pour générer les informations de build et Git :

```bash
mvn clean package
```

Les fichiers suivants seront générés dans `target/classes/` :
- `META-INF/build-info.properties` - Informations de build
- `git.properties` - Informations Git

## Utilisation dans le code

### Injecter BuildProperties

```java
@Autowired
private Optional<BuildProperties> buildProperties;

public void printVersion() {
    buildProperties.ifPresent(build -> {
        System.out.println("Version: " + build.getVersion());
        System.out.println("Build Time: " + build.getTime());
    });
}
```

### Injecter GitProperties

```java
@Autowired
private Optional<GitProperties> gitProperties;

public void printGitInfo() {
    gitProperties.ifPresent(git -> {
        System.out.println("Branch: " + git.getBranch());
        System.out.println("Commit: " + git.getShortCommitId());
    });
}
```

## Différences avec le frontend

| Aspect | Backend | Frontend |
|--------|---------|----------|
| **Technologie** | Spring Boot Actuator + Maven plugins | Script Node.js personnalisé |
| **Fichiers générés** | `build-info.properties`, `git.properties` | `version.json`, `version.ts` |
| **Moment de génération** | À chaque build Maven | À chaque build/dev npm |
| **Format timestamp** | ISO-8601 (UTC) | ISO-8601 (UTC) + format lisible |
| **Informations Git** | ✅ Automatique | ❌ Non disponible |
| **Endpoints** | `/api/version`, `/actuator/info` | `/version.json` |
| **Sécurité** | Public (configurable) | Public (fichier statique) |

## Notes

- Les informations Git sont uniquement disponibles si l'application est buildée dans un dépôt Git
- Le timestamp reflète l'heure exacte du build Maven
- Les informations sont incluses dans le JAR final
- En développement (run via IDE), les informations peuvent ne pas être disponibles

