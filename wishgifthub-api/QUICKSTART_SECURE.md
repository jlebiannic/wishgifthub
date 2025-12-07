# 🚀 Démarrage Rapide Sécurisé - Backend WishGiftHub

## Configuration des secrets AVANT le premier démarrage

### ⚠️ IMPORTANT
Les secrets (mot de passe BDD, JWT secret) ne sont plus inclus dans le code source pour des raisons de sécurité. Vous devez les générer avant le premier démarrage.

---

## Option 1 : Script automatique (RECOMMANDÉ)

### Windows
```cmd
cd wishgifthub-api
generate-secrets.bat
```

### Linux/Mac
```bash
cd wishgifthub-api
chmod +x generate-secrets.sh
./generate-secrets.sh
```

Le script va :
1. ✅ Créer le fichier `.env` depuis `.env.example`
2. ✅ Générer un mot de passe PostgreSQL sécurisé
3. ✅ Générer un JWT secret (256 bits)
4. ✅ Sauvegarder automatiquement dans `.env`

---

## Option 2 : Manuel

### Étape 1 : Copier le template

```bash
cp .env.example .env
```

### Étape 2 : Générer les secrets

**Linux/Mac :**
```bash
# Mot de passe PostgreSQL
openssl rand -base64 32

# JWT Secret (64 caractères hex)
openssl rand -hex 32
```

**Windows PowerShell :**
```powershell
# Mot de passe PostgreSQL
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))

# JWT Secret
-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) })
```

### Étape 3 : Éditer le fichier .env

Ouvrir `.env` et remplacer :
```env
POSTGRES_PASSWORD=VotreMotDePasseGenere
WISHGIFTHUB_JWT_SECRET=VotreJWTSecretGenere
```

---

## Démarrage de l'application

Une fois les secrets configurés :

```bash
# Builder et démarrer avec Docker Compose
docker-compose up --build -d

# Vérifier les logs
docker-compose logs -f

# Tester le healthcheck
curl http://localhost:8080/actuator/health
```

**Explication de `docker-compose up --build` :**
- `docker-compose up` : Démarre les services
- `--build` : Force le rebuild de l'image Docker avant de démarrer
- `-d` : Mode détaché (arrière-plan)

**Pour plus de détails sur le build Docker, voir [DOCKER_BUILD.md](DOCKER_BUILD.md)**

---

## 🔒 Sécurité

### ✅ Bonnes pratiques appliquées

- ✅ Secrets générés aléatoirement (pas de valeurs par défaut)
- ✅ Fichier `.env` dans `.gitignore` (ne sera jamais commité)
- ✅ JWT secret de 256 bits (64 caractères hex)
- ✅ Mot de passe PostgreSQL fort (32 caractères)
- ✅ Fichier `.env.example` sans valeurs sensibles

### ⚠️ À NE PAS FAIRE

- ❌ Committer le fichier `.env` dans Git
- ❌ Partager les secrets par email/chat
- ❌ Utiliser des secrets faibles (ex: "password", "123456")
- ❌ Réutiliser les mêmes secrets entre environnements

### 🛡️ Recommandations supplémentaires

**Pour la production :**

1. **Rotation des secrets** : Changer tous les 90 jours
   ```bash
   # Générer de nouveaux secrets
   ./generate-secrets.sh
   
   # Redémarrer avec les nouveaux secrets
   docker-compose up -d --force-recreate
   ```

2. **Permissions du fichier .env**
   ```bash
   chmod 600 .env  # Lecture/écriture pour le propriétaire uniquement
   ```

3. **Utiliser Docker Secrets** (Docker Swarm/Kubernetes)
   ```bash
   # Créer les secrets
   echo "VotreMotDePasse" | docker secret create db_password -
   echo "VotreJWTSecret" | docker secret create jwt_secret -
   ```

4. **Vault (HashiCorp)** pour les environnements critiques

---

## 🧪 Vérification

```bash
# Vérifier que .env existe et n'est pas vide
cat .env | grep POSTGRES_PASSWORD
cat .env | grep WISHGIFTHUB_JWT_SECRET

# Vérifier que .env n'est PAS dans Git
git status --ignored | grep .env

# Vérifier la longueur du JWT secret (doit être 64)
cat .env | grep WISHGIFTHUB_JWT_SECRET | cut -d'=' -f2 | wc -c
# Résultat attendu : 65 (64 + retour à la ligne)
```

---

## 📋 Checklist de premier démarrage

- [ ] Script `generate-secrets.sh` ou `generate-secrets.bat` exécuté
- [ ] Fichier `.env` créé
- [ ] `POSTGRES_PASSWORD` généré (32+ caractères)
- [ ] `WISHGIFTHUB_JWT_SECRET` généré (64 caractères hex)
- [ ] Fichier `.env` vérifié et **non** dans Git
- [ ] `docker-compose up` réussi
- [ ] Healthcheck OK : `curl http://localhost:8080/actuator/health`

---

## ❓ Problèmes courants

### Erreur : "WISHGIFTHUB_JWT_SECRET is required"

➡️ Le fichier `.env` n'existe pas ou ne contient pas le JWT secret
```bash
./generate-secrets.sh
```

### Erreur : "password authentication failed"

➡️ Le mot de passe PostgreSQL dans `.env` ne correspond pas
```bash
# Régénérer les secrets
./generate-secrets.sh

# Recréer les containers
docker-compose down -v
docker-compose up
```

### Erreur : "JWT secret too short"

➡️ Le JWT secret doit faire 64 caractères (256 bits)
```bash
# Vérifier la longueur
cat .env | grep WISHGIFTHUB_JWT_SECRET

# Régénérer si nécessaire
openssl rand -hex 32
```

---

## 📚 Documentation complémentaire

- **SECRETS_SECURITY.md** : Guide complet de sécurisation des secrets
- **ACTUATOR_HEALTH.md** : Configuration du healthcheck
- **.env.example** : Template de configuration

---

**Bon démarrage sécurisé ! 🔒**

