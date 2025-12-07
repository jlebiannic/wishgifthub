# Guide de Sécurisation des Secrets - WishGiftHub

## 🔒 Gestion Sécurisée des Secrets

Ce guide explique comment gérer de manière sécurisée le mot de passe de la base de données et le secret JWT.

---

## ⚠️ PROBLÈME ACTUEL

Dans le Dockerfile et docker-compose.yml, les secrets sont en dur :

```dockerfile
ENV SPRING_DATASOURCE_PASSWORD=password
ENV WISHGIFTHUB_JWT_SECRET=5a7f8c3d9e2b1a6f4c8e7d3a9b2c5e1f8a3d7c2b6e4a9d1f5c8b3e7a2d6c9f4b
```

❌ **C'est DANGEREUX** :
- Les secrets sont visibles dans l'image Docker
- Ils peuvent être extraits avec `docker history`
- Ils sont dans le code source (risque de commit dans Git)

---

## ✅ SOLUTIONS SÉCURISÉES

### Solution 1 : Variables d'environnement avec fichier .env (SIMPLE)

**Avantages** : Simple, fonctionne partout
**Inconvénients** : Le fichier .env doit être protégé

#### Étape 1 : Supprimer les valeurs par défaut du Dockerfile

Les secrets ne doivent PAS être dans le Dockerfile. Ils seront fournis à l'exécution.

#### Étape 2 : Créer un fichier .env sécurisé

```bash
# Générer un mot de passe fort
openssl rand -base64 32

# Générer un JWT secret
openssl rand -hex 32
```

Créer `.env` (ne JAMAIS committer ce fichier) :

```env
POSTGRES_PASSWORD=VotreMotDePasseSecurise123!
WISHGIFTHUB_JWT_SECRET=a1b2c3d4e5f6...  # 64 caractères hex
```

#### Étape 3 : Sécuriser le fichier .env

```bash
# Permissions restrictives (Linux/Mac)
chmod 600 .env

# Vérifier qu'il est dans .gitignore
echo ".env" >> .gitignore
```

#### Étape 4 : Utiliser avec Docker Compose

```yaml
services:
  backend:
    env_file:
      - .env  # Charge les variables depuis .env
```

---

### Solution 2 : Docker Secrets (RECOMMANDÉ pour production)

**Avantages** : Chiffré, géré par Docker Swarm/Kubernetes
**Inconvénients** : Nécessite Docker Swarm ou Kubernetes

#### Configuration avec Docker Swarm

```bash
# Créer les secrets
echo "VotreMotDePasseSecurise" | docker secret create db_password -
echo "VotreJWTSecret" | docker secret create jwt_secret -

# Lister les secrets
docker secret ls
```

#### docker-compose.yml pour Swarm

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    secrets:
      - db_password
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    deploy:
      placement:
        constraints: [node.role == manager]

  backend:
    image: wishgifthub-backend:latest
    secrets:
      - db_password
      - jwt_secret
    environment:
      SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/db_password
      WISHGIFTHUB_JWT_SECRET_FILE: /run/secrets/jwt_secret
    deploy:
      replicas: 2

secrets:
  db_password:
    external: true
  jwt_secret:
    external: true
```

#### Modifier le code Spring Boot pour lire les secrets

```java
@Configuration
public class SecretConfiguration {
    
    @Bean
    public String databasePassword(
        @Value("${SPRING_DATASOURCE_PASSWORD_FILE:}") String passwordFile,
        @Value("${SPRING_DATASOURCE_PASSWORD:}") String passwordEnv
    ) {
        if (passwordFile != null && !passwordFile.isEmpty()) {
            return readSecret(passwordFile);
        }
        return passwordEnv;
    }
    
    @Bean
    public String jwtSecret(
        @Value("${WISHGIFTHUB_JWT_SECRET_FILE:}") String secretFile,
        @Value("${WISHGIFTHUB_JWT_SECRET:}") String secretEnv
    ) {
        if (secretFile != null && !secretFile.isEmpty()) {
            return readSecret(secretFile);
        }
        return secretEnv;
    }
    
    private String readSecret(String filePath) {
        try {
            return Files.readString(Paths.get(filePath)).trim();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read secret from " + filePath, e);
        }
    }
}
```

---

### Solution 3 : Vault (HashiCorp Vault / AWS Secrets Manager)

**Avantages** : Très sécurisé, rotation automatique, audit
**Inconvénients** : Complexe, nécessite infrastructure

#### Exemple avec HashiCorp Vault

```bash
# Démarrer Vault
docker run -d --name=vault -p 8200:8200 vault

# Stocker les secrets
vault kv put secret/wishgifthub \
  db_password=VotreMotDePasse \
  jwt_secret=VotreJWTSecret

# Récupérer un secret
vault kv get -field=db_password secret/wishgifthub
```

#### Spring Boot avec Vault

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      scheme: https
      authentication: TOKEN
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        profile-separator: '/'
```

---

### Solution 4 : Kubernetes Secrets (pour Kubernetes)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: wishgifthub-secrets
type: Opaque
data:
  db-password: VGVzdDEyMyE=  # base64 encodé
  jwt-secret: YTFiMmMzZDQ=     # base64 encodé
```

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wishgifthub-backend
spec:
  template:
    spec:
      containers:
      - name: backend
        env:
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: wishgifthub-secrets
              key: db-password
        - name: WISHGIFTHUB_JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: wishgifthub-secrets
              key: jwt-secret
```

---

## 🎯 RECOMMANDATIONS PAR ENVIRONNEMENT

### Développement Local
✅ Fichier `.env` avec `.gitignore`

```bash
cp .env.example .env
# Éditer .env avec vos valeurs locales
docker-compose up
```

### Staging / Pré-production
✅ Variables d'environnement injectées par CI/CD

```bash
# GitLab CI / GitHub Actions
docker run -e POSTGRES_PASSWORD=$DB_PASS -e WISHGIFTHUB_JWT_SECRET=$JWT_SECRET ...
```

### Production
✅ Docker Secrets (Docker Swarm) ou Kubernetes Secrets

```bash
# Docker Swarm
docker stack deploy -c docker-compose.prod.yml wishgifthub

# Kubernetes
kubectl apply -f secrets.yaml
kubectl apply -f deployment.yaml
```

---

## 🛡️ BONNES PRATIQUES

### 1. Ne JAMAIS committer les secrets

```bash
# .gitignore
.env
.env.local
.env.*.local
secrets/
*.key
*.pem
```

### 2. Rotation régulière des secrets

```bash
# Tous les 90 jours, générer de nouveaux secrets
openssl rand -hex 32 > new_jwt_secret.txt

# Mettre à jour graduellement (zero-downtime)
```

### 3. Permissions strictes

```bash
# Fichiers de secrets : lecture seule pour le propriétaire
chmod 400 secrets/*

# Répertoires : accès restreint
chmod 700 secrets/
```

### 4. Audit et logs

```bash
# Surveiller l'accès aux secrets
docker events --filter 'type=secret'

# Logs d'audit Vault
vault audit enable file file_path=/var/log/vault_audit.log
```

### 5. Chiffrement au repos

Les secrets doivent être chiffrés :
- Docker Swarm : chiffrement automatique
- Kubernetes : chiffrement etcd
- Fichiers : `ansible-vault`, `git-crypt`, `sops`

---

## 📋 CHECKLIST DE SÉCURITÉ

- [ ] Secrets supprimés du Dockerfile
- [ ] Secrets supprimés du docker-compose.yml (ou utilisation de ${VAR})
- [ ] .env dans .gitignore
- [ ] .env.example créé (sans valeurs réelles)
- [ ] Permissions 600 ou 400 sur les fichiers de secrets
- [ ] Rotation des secrets planifiée
- [ ] Accès aux secrets audité
- [ ] Secrets chiffrés au repos
- [ ] Pas de secrets dans les logs
- [ ] Pas de secrets dans les images Docker

---

## 🚀 MIGRATION RAPIDE (Recommandé)

Je vais maintenant modifier vos fichiers pour implémenter la solution la plus simple et sécurisée.


