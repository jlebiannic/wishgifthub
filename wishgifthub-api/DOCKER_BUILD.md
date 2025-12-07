# 🐳 Guide de Build Docker - WishGiftHub Backend

Ce guide explique comment construire (builder) l'application avec Docker.

---

## 📋 Prérequis

- **Docker** installé (version 20.10+)
- **Docker Compose** installé (version 2.0+)

Vérifier l'installation :
```bash
docker --version
docker-compose --version
```

---

## 🚀 Build avec Docker Compose (RECOMMANDÉ)

C'est la méthode la plus simple qui build automatiquement l'image et démarre les services.

### 1. Préparer les secrets

**Première fois uniquement** :
```bash
# Windows
generate-secrets.bat

# Linux/Mac
chmod +x generate-secrets.sh
./generate-secrets.sh
```

### 2. Builder et démarrer

```bash
# Build + démarrage en une seule commande
docker-compose up --build -d
```

**Options :**
- `--build` : Force le rebuild de l'image
- `-d` : Mode détaché (arrière-plan)

### 3. Vérifier

```bash
# Voir les containers en cours
docker-compose ps

# Voir les logs
docker-compose logs -f

# Tester le healthcheck
curl http://localhost:8080/actuator/health
```

---

## 🔨 Build manuel avec docker build

Pour construire l'image sans Docker Compose :

### 1. Builder l'image

```bash
# Se placer dans le répertoire wishgifthub-api
cd wishgifthub-api

# Builder l'image
docker build -t wishgifthub-backend:latest .
```

**Options utiles :**
```bash
# Avec un tag spécifique
docker build -t wishgifthub-backend:1.0.0 .

# Sans utiliser le cache
docker build --no-cache -t wishgifthub-backend:latest .

# Afficher le build détaillé
docker build --progress=plain -t wishgifthub-backend:latest .
```

### 2. Lister les images

```bash
docker images wishgifthub-backend

# Résultat attendu :
# REPOSITORY              TAG       IMAGE ID       CREATED         SIZE
# wishgifthub-backend     latest    abc123def456   2 minutes ago   ~300MB
```

### 3. Inspecter l'image

```bash
# Voir l'historique des layers
docker history wishgifthub-backend:latest

# Voir les détails de l'image
docker inspect wishgifthub-backend:latest
```

---

## 🏗️ Processus de Build Multi-Stage

Le Dockerfile utilise un **build multi-stage** pour optimiser la taille de l'image finale :

```
┌─────────────────────────────────────────────────────────────┐
│ Stage 1: Build OpenAPI (maven:3.9-eclipse-temurin-21-alpine) │
│ - Copie les fichiers pom.xml                                 │
│ - Télécharge les dépendances Maven                           │
│ - Copie les sources OpenAPI                                  │
│ - Génère le fichier openapi.yaml                             │
│ Résultat : openapi.yaml généré                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│ Stage 2: Build Backend (maven:3.9-eclipse-temurin-21-alpine)│
│ - Copie les fichiers pom.xml                                 │
│ - Télécharge les dépendances Maven                           │
│ - Copie les sources OpenAPI (depuis Stage 1)                 │
│ - Copie les sources de l'API                                 │
│ - Exécute mvn clean package -DskipTests                      │
│ Résultat : wishgifthub-api.jar                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│ Stage 3: Runtime (eclipse-temurin:21-jre-alpine)            │
│ - Installe wget pour le healthcheck                          │
│ - Crée un utilisateur non-root "spring"                      │
│ - Copie le JAR depuis Stage 2                                │
│ - Configure les variables d'environnement                    │
│ - Définit l'ENTRYPOINT pour lancer Java                      │
│ Résultat : Image finale (~300MB)                             │
└─────────────────────────────────────────────────────────────┘
```

**Avantages du multi-stage :**
- ✅ Image finale légère (~300MB vs ~1GB avec Maven/JDK)
- ✅ Pas d'outils de build dans l'image de production
- ✅ Sécurité accrue (JRE uniquement, pas de JDK)
- ✅ Build rapide grâce au cache Docker

---

## ⏱️ Temps de Build

| Build | Première fois | Builds suivants |
|-------|---------------|-----------------|
| **Sans cache** | 10-15 minutes | 10-15 minutes |
| **Avec cache** | 10-15 minutes | 2-3 minutes |

**Optimisation du cache :**
- Les dépendances Maven sont mises en cache
- Seuls les fichiers modifiés sont reconstruits
- Le cache est invalidé si pom.xml change

---

## 🔄 Rebuild de l'image

### Quand rebuilder ?

Vous devez rebuilder l'image quand :
- ✅ Le code Java a changé
- ✅ Le fichier `pom.xml` a changé (nouvelles dépendances)
- ✅ Le `Dockerfile` a changé
- ✅ Les fichiers de migration Flyway ont changé
- ❌ PAS besoin si seulement `.env` a changé (c'est une variable d'exécution)

### Commandes de rebuild

```bash
# Avec Docker Compose (SIMPLE)
docker-compose up --build

# Forcer le rebuild sans cache
docker-compose build --no-cache

# Builder uniquement sans démarrer
docker-compose build

# Avec docker build (MANUEL)
docker build --no-cache -t wishgifthub-backend:latest .
```

---

## 🐞 Debugging du Build

### Voir les logs de build détaillés

```bash
docker-compose build --progress=plain
```

### Build échoue : "mvn command not found"

➡️ Problème de Dockerfile ou image de base

```bash
# Vérifier l'image de base
docker pull maven:3.9-eclipse-temurin-21-alpine
```

### Build échoue : "Could not resolve dependencies"

➡️ Problème de connexion Maven Central

```bash
# Vérifier la connexion réseau
docker run --rm maven:3.9-eclipse-temurin-21-alpine mvn --version

# Builder avec les logs Maven
docker build --build-arg MAVEN_OPTS="-X" -t wishgifthub-backend:latest .
```

### Build très lent

➡️ Optimiser le cache Docker

```bash
# Nettoyer le cache Docker
docker builder prune -f

# Voir l'utilisation du cache
docker system df

# Utiliser BuildKit (plus rapide)
export DOCKER_BUILDKIT=1
docker build -t wishgifthub-backend:latest .
```

---

## 📦 Gérer les images

### Lister les images

```bash
# Toutes les images
docker images

# Images wishgifthub uniquement
docker images wishgifthub-backend
```

### Supprimer les images

```bash
# Supprimer une image spécifique
docker rmi wishgifthub-backend:latest

# Supprimer les images non utilisées
docker image prune -a

# Forcer la suppression
docker rmi -f wishgifthub-backend:latest
```

### Tagger les images

```bash
# Ajouter un tag de version
docker tag wishgifthub-backend:latest wishgifthub-backend:1.0.0

# Tag pour un registry
docker tag wishgifthub-backend:latest registry.example.com/wishgifthub-backend:1.0.0
```

---

## 🚢 Push vers un Registry Docker

### Docker Hub

```bash
# Login
docker login

# Tag avec votre username
docker tag wishgifthub-backend:latest votre-username/wishgifthub-backend:latest

# Push
docker push votre-username/wishgifthub-backend:latest
```

### Registry privé

```bash
# Login
docker login registry.example.com

# Tag
docker tag wishgifthub-backend:latest registry.example.com/wishgifthub-backend:latest

# Push
docker push registry.example.com/wishgifthub-backend:latest
```

---

## 🔍 Inspecter l'image buildée

### Voir les layers

```bash
docker history wishgifthub-backend:latest
```

### Voir le contenu de l'image

```bash
# Lancer un shell dans l'image
docker run --rm -it wishgifthub-backend:latest /bin/sh

# Ou si l'image est déjà en cours d'exécution
docker exec -it wishgifthub-backend /bin/sh
```

### Vérifier la taille

```bash
docker images wishgifthub-backend:latest --format "{{.Size}}"
```

---

## 📊 Optimisation de la taille de l'image

### Taille actuelle

```bash
docker images wishgifthub-backend:latest
# Résultat attendu : ~300MB
```

### Techniques d'optimisation utilisées

1. **Multi-stage build** ✅
   - Seulement le JRE dans l'image finale
   - Pas de Maven, pas de sources

2. **Image Alpine** ✅
   - Base légère (~5MB)
   - vs Ubuntu (~70MB)

3. **Une seule couche JAR** ✅
   - JAR copié en une seule fois
   - Pas de fichiers temporaires

4. **Utilisateur non-root** ✅
   - Sécurité sans surcoût

### Comparer les tailles

```bash
# Voir toutes les images
docker images

# Comparer avec une image non-optimisée
# Non-optimisé (avec JDK + Maven) : ~1GB
# Optimisé (JRE uniquement) : ~300MB
# Gain : ~70%
```

---

## 🎯 Exemples complets

### Workflow de développement complet

```bash
# 1. Modifier le code Java
vim src/main/java/com/wishgifthub/controller/WishController.java

# 2. Rebuilder et redémarrer
docker-compose up --build -d

# 3. Voir les logs
docker-compose logs -f backend

# 4. Tester
curl http://localhost:8080/actuator/health
```

### Workflow de release

```bash
# 1. Builder avec un tag de version
docker build -t wishgifthub-backend:1.2.3 .

# 2. Tagger aussi en latest
docker tag wishgifthub-backend:1.2.3 wishgifthub-backend:latest

# 3. Push vers le registry
docker push wishgifthub-backend:1.2.3
docker push wishgifthub-backend:latest

# 4. Déployer en production
docker pull wishgifthub-backend:1.2.3
docker-compose -f docker-compose.prod.yml up -d
```

---

## 📋 Checklist de build

- [ ] Docker et Docker Compose installés
- [ ] Fichier `.env` créé avec les secrets
- [ ] Dans le bon répertoire (`wishgifthub-api`)
- [ ] Connexion internet OK (pour Maven Central)
- [ ] Espace disque suffisant (~2GB)
- [ ] `docker-compose up --build -d` exécuté
- [ ] Container en cours : `docker-compose ps`
- [ ] Healthcheck OK : `curl http://localhost:8080/actuator/health`

---

## 🆘 Aide rapide

```bash
# Build
docker-compose up --build -d

# Rebuild sans cache
docker-compose build --no-cache

# Voir les images
docker images wishgifthub-backend

# Voir les logs de build
docker-compose build --progress=plain

# Nettoyer
docker-compose down
docker rmi wishgifthub-backend:latest

# Tout supprimer (images + volumes)
docker-compose down -v
docker rmi wishgifthub-backend:latest
docker builder prune -a
```

---

**Votre image Docker est maintenant prête ! 🐳**

