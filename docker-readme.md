# Commandes Docker

## Réseau

Créer le réseau docker

<code>docker network create wishgifthub-net</code>

## back-end

### Build image

A la racine du projet

<code>docker build -t wishgifthub-backend:latest .</code>

Taguer pour push sur docker hub

<code>docker tag wishgifthub-backend:latest jlebiannic/wishgifthub-backend:latest</code>

Login docker:
<code>docker login</code>

Push de l'image sur docker hub

<code>docker push jlebiannic/wishgifthub-backend:latest</code>

### Exécution sur un serveur

Ajout du user debian au group docker

<code>sudo usermod -aG docker $USER</code>

Login docker:
<code>docker login</code>

Pull image docker

<code>docker pull jlebiannic/wishgifthub-backend:latest</code>

Run

<code>docker run -d --env-file /home/debian/config/wishgifthub/.env --network wishgifthub-net -p 8080:8080 --name wishgifthub-backend jlebiannic/wishgifthub-backend:latest
</code>

## Front

### Build image

Dans le répertoire wishgifthub-ui

<code>docker build -t wishgifthub-frontend:latest .</code>

Taguer pour push sur docker hub

<code>docker tag wishgifthub-frontend:latest jlebiannic/wishgifthub-frontend:latest</code>

Login docker:
<code>docker login</code>

Push de l'image sur docker hub

<code>docker push jlebiannic/wishgifthub-frontend:latest</code>

### Exécution sur un serveur

Ajout du user debian au group docker

<code>sudo usermod -aG docker $USER</code>

Login docker:
<code>docker login</code>

Pull image docker

<code>docker pull jlebiannic/wishgifthub-frontend:latest</code>

Run

<code>docker run -d --network wishgifthub-net -p 80:80 --name wishgifthub-frontend jlebiannic/wishgifthub-frontend:latest
</code>
