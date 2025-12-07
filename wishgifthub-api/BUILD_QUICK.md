# 🐳 Build Docker - Guide Rapide

## Commandes essentielles

### 1. Préparer les secrets (première fois)

```bash
# Windows
generate-secrets.bat

# Linux/Mac
./generate-secrets.sh
```

### 2. Builder et démarrer

```bash
docker-compose up --build -d
```

**Ce que fait cette commande :**
1. 📦 Build l'image Docker (si pas en cache ou si `--build` est utilisé)
2. 🚀 Démarre les containers (PostgreSQL + Backend)
3. 🏥 Active les healthchecks
4. ✅ L'application est accessible sur http://localhost:8080

### 3. Vérifier

```bash
# Voir les containers
docker-compose ps

# Voir les logs
docker-compose logs -f

# Tester l'API
curl http://localhost:8080/actuator/health
```

---

## 📊 Processus de Build

```
┌─────────────────────────────────────┐
│ Stage 1: Build OpenAPI              │
│ → Génère openapi.yaml               │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│ Stage 2: Build Backend               │
│ → Compile le code Java              │
│ → Crée le fichier JAR                │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│ Stage 3: Runtime                     │
│ → Image finale (~300MB)              │
│ → JRE 21 + JAR + wget                │
└─────────────────────────────────────┘
```

**Temps de build :**
- Première fois : 10-15 minutes
- Builds suivants (avec cache) : 2-3 minutes

---

## 🔄 Quand rebuilder ?

✅ **Rebuilder quand :**
- Le code Java a changé
- Le `pom.xml` a changé
- Le `Dockerfile` a changé
- Les migrations Flyway ont changé

❌ **PAS besoin de rebuilder si :**
- Seulement le fichier `.env` a changé
- Seulement la configuration a changé

---

## 🛠️ Commandes utiles

```bash
# Builder sans démarrer
docker-compose build

# Forcer le rebuild (sans cache)
docker-compose build --no-cache

# Redémarrer sans rebuilder
docker-compose restart

# Arrêter
docker-compose down

# Tout supprimer (avec volumes)
docker-compose down -v
```

---

## 📚 Documentation complète

Pour plus de détails : **[DOCKER_BUILD.md](DOCKER_BUILD.md)**

Contenu :
- 🏗️ Processus de build multi-stage détaillé
- 🐞 Debugging du build
- 📦 Gestion des images
- 🚢 Push vers un registry
- 🔍 Inspection de l'image
- 📊 Optimisation de la taille
- 🎯 Exemples complets

---

**Bon build ! 🐳**

