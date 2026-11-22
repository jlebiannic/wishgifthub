# Journal d'installation du Raspberry Pi

Ce fichier consigne, à partir du 8 novembre 2026, les interventions sur le Pi : commandes, motifs, résultats et suites éventuelles. Ne jamais y copier de clé privée, mot de passe, secret JWT ni contenu du fichier `.env`.

## Contexte

- Machine : `sniper`, accessible par SSH avec `pi@192.168.0.50`.
- Clé locale : `rssh/id_ed25519_raspi` (hors dépôt).
- Matériel constaté : Raspberry Pi 2 Model B Rev 1.1, ARMv7 32 bits (`armv7l`, architecture Debian `armhf`).
- Système : Raspbian GNU/Linux 13 (trixie).
- Mémoire constatée lors du diagnostic : 920 Mio ; disque racine 29 Gio, environ 24 Gio disponibles.
- Applications existantes : FlightSniper et Gabrielle Coaching, servies par le Nginx installé sur l'hôte, port 80.
- Cible future : `http://192.168.0.50/wishgifthub/`, backend Spring Boot Java 21, base PostgreSQL Supabase. Les paramètres et secrets seront dans un `.env` côté serveur.
- Le déploiement applicatif et la configuration Nginx restent à réaliser ; ils ne font pas partie de l'installation Java.

## Connexion depuis Windows

Le client utilisé est `C:/Windows/System32/OpenSSH/ssh.exe`. La clé du dossier `rssh` a des permissions trop ouvertes pour OpenSSH. Chaque connexion utilise une copie temporaire dont l'héritage des droits est supprimé et dont la lecture est accordée au SID Windows courant avec `icacls`. Cette copie est supprimée dans un bloc `finally` ; la clé originale reste inchangée.

Les scripts distants sont encodés en Base64 UTF-8 pour éviter les problèmes de guillemets entre PowerShell et Bash, puis exécutés avec `echo <script-base64> | base64 -d | bash`. Le Base64 n'est pas un chiffrement ; aucun secret n'est placé dans ces scripts.

Gabarit PowerShell utilisé depuis la racine du projet (remplacer le contenu de `$taskScript` par le bloc Bash de l'intervention) :

```powershell
$taskKey = Join-Path $env:TEMP ('raspi-install-' + [guid]::NewGuid().ToString() + '.key')
Copy-Item -LiteralPath rssh/id_ed25519_raspi -Destination $taskKey
try {
    $taskSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $taskKey /inheritance:r /grant:r "*$($taskSid):(R)" | Out-Null
    $taskScript = @'
set -eu
hostname
'@
    $taskEncoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($taskScript))
    & C:/Windows/System32/OpenSSH/ssh.exe -n -i $taskKey `
        -o BatchMode=yes -o ConnectTimeout=10 pi@192.168.0.50 `
        "echo $taskEncoded | base64 -d | bash"
    if ($LASTEXITCODE -ne 0) { throw "La commande SSH a échoué : $LASTEXITCODE" }
} finally {
    Remove-Item -LiteralPath $taskKey
}
```

## 2026-11-08 — Installation Java 21

### Diagnostic préalable effectué

```bash
set -eu
hostname
uname -m
dpkg --print-architecture
command -v java || true
apt-cache policy openjdk-21-jre-headless
sudo -n true
```

Résultats : hôte `sniper`, `armv7l`, `armhf`, aucune commande Java disponible. Le dépôt Raspbian propose `openjdk-21-jre-headless`, candidat initial `21.0.10+7-1~deb13u1+rpi1`. Les droits sudo sans interaction sont disponibles.

### Choix

Installer le runtime Java 21 sans interface graphique depuis le dépôt Raspbian existant, plutôt qu'ajouter une distribution ou un dépôt tiers. Un JRE suffit pour exécuter le JAR Spring Boot ; Maven et le JDK de compilation restent sur le poste de développement ou la CI. La découverte du paquet ARM32 natif remplace la proposition initiale d'utiliser Liberica.

### Commandes d'installation exécutées

```bash
set -eu
sudo -n apt-get update
sudo -n apt-get install -y --no-install-recommends openjdk-21-jre-headless
java -version
readlink -f /usr/bin/java
dpkg-query -W openjdk-21-jre-headless
systemctl is-active nginx flightsniper gabrielle-coaching
```

`apt-get update` actualise les index des dépôts. L'installation ajoute Java et ses dépendances nécessaires sans mise à niveau générale du système. Aucun redémarrage ni changement des applications existantes n'est prévu.

### Résultats vérifiés

Installation réussie. Après actualisation des index, le paquet disponible était plus récent que le candidat observé initialement :

```text
openjdk version "21.0.12.1" 2026-08-18
OpenJDK Runtime Environment (build 21.0.12.1+1-1-deb13u1rpi1-Raspbian)
OpenJDK Client VM (build 21.0.12.1+1-1-deb13u1rpi1-Raspbian, mixed mode, emulated-client, sharing)
```

- Paquet installé : `openjdk-21-jre-headless:armhf`, version `21.0.12.1+1-1~deb13u1+rpi1`.
- Exécutable : `/usr/lib/jvm/java-21-openjdk-armhf/bin/java`, disponible via `/usr/bin/java` grâce à `update-alternatives`.
- Répertoire Java pour un futur service : `/usr/lib/jvm/java-21-openjdk-armhf`. Aucune variable `JAVA_HOME` globale n'a été ajoutée.
- Dépendances ajoutées : `ca-certificates-java` (`20240118`), `java-common` (`0.76`), `liblcms2-2:armhf` (`2.16-2+deb13u2`). Le magasin de certificats Java a été initialisé.
- Bilan APT : 4 nouveaux paquets, 0 paquet mis à niveau, 0 supprimé ; 41,3 Mo téléchargés, environ 195 Mo d'espace disque supplémentaire annoncé.
- `systemctl is-active nginx flightsniper gabrielle-coaching` : les trois services sont `active` après installation.
- Aucun redémarrage demandé ou effectué ; aucun service WishGiftHub créé, aucun JAR déployé, aucune connexion à Supabase effectuée.
- Les messages `debconf` concernant l'absence de terminal ont été suivis d'un passage automatique au mode non interactif et n'ont pas empêché l'installation. Le message local `GetConsoleMode ... failed with 6` du client SSH n'a pas empêché l'exécution distante.
- Après réception de tous les résultats, des processus SSH Windows sont restés ouverts. Ils ont été identifiés avec `Get-CimInstance Win32_Process -Filter "Name = 'ssh.exe'"` et fermés avec `Stop-Process` uniquement lorsque leur ligne de commande contenait notre clé temporaire `raspi-install-<UUID>.key` ou `raspi-inspect-<UUID>.key` et la destination `pi@192.168.0.50`. Cela concerne aussi les connexions des diagnostics précédents. Les blocs `finally` PowerShell assurent ensuite la suppression des copies temporaires. L'installation APT et les vérifications Java étaient déjà terminées avant cette fermeture locale.

### Maintenance ultérieure (non exécutée)

Pour mettre Java à jour via les mêmes dépôts :

```bash
sudo apt-get update
sudo apt-get install --only-upgrade openjdk-21-jre-headless
java -version
```

Si WishGiftHub utilise alors Java, redémarrer son service après mise à jour pour qu'il charge le nouveau runtime. Chaque intervention future devra être ajoutée à ce journal avec ses résultats.

## 2026-11-08 — Déploiement WishGiftHub sous /wishgifthub/

### Périmètre et configuration

Déploiement demandé du frontend et du backend sur le Pi. L'utilisateur a demandé un `.env` avec des données fictives qu'il complétera ensuite. Aucune connexion Supabase ni migration ne doit être tentée avec ces valeurs. Le backend est installé comme service activé au démarrage, mais son `ExecCondition` refuse de lancer Java tant qu'une variable requise est vide ou contient `CHANGE_ME`. Cela est une attente de configuration, pas une panne de compilation.

Diagnostic distant exécuté :

```bash
sudo -n find /etc/wishgifthub /opt/wishgifthub /home/pi /home/debian/config -maxdepth 4 -name .env -type f 2>/dev/null || true
sudo -n cat /etc/nginx/sites-enabled/flightsniper
sudo -n ss -ltnp
java -version
```

Aucun `.env` trouvé dans ces emplacements. Le port 8080 est libre. Le serveur Nginx par défaut reste celui de FlightSniper ; on lui ajoute uniquement l'inclusion d'un snippet WishGiftHub. Les fichiers de déploiement reproductibles sont dans `deploy/pi/` : service, modèle `.env`, contrôle de configuration, snippet Nginx et script `install.sh`.

### Préparation locale

Java 21 disponible localement : `D:/Dev/tools/openjdk-21.0.2`. Le Java par défaut du poste est Java 11, donc `JAVA_HOME` est défini uniquement pour les commandes Maven de cette intervention.

```powershell
$env:JAVA_HOME = 'D:/Dev/tools/openjdk-21.0.2'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
mvn install -pl wishgifthub-openapi -am -DskipTests -B
mvn package -pl wishgifthub-api -am -B -q
npm install --prefix rssh/build-tools --no-save --package-lock=false node@22
```

L'installation OpenAPI a réussi. Node 22 est préparé dans `rssh/build-tools` (ignoré par Git), sans modifier Node global. `npm ci --no-audit --no-fund` est lancé dans `wishgifthub-ui`. Le premier `npm ci` utilise le npm global (Node 20.9) et affiche des avertissements de version ; la compilation et les tests utilisent explicitement le Node 22 local.

Modifications du frontend : base API fondée sur `import.meta.env.BASE_URL`, chemins de `version.json` et invitations adaptés au préfixe, copie des invitations avec repli sur les navigateurs HTTP dépourvus de Clipboard API. Le routeur utilise déjà `BASE_URL`. Le préfixe `/wishgifthub/` est passé à Vite lors du build, sans changer le développement à la racine.

### Installation distante prévue

L'archive contient `frontend/wishgifthub/`, `backend/app.jar` et `deploy/pi/`. Depuis son répertoire d'extraction, `sudo bash deploy/pi/install.sh` :

1. Sauvegarde le serveur Nginx et les fichiers de service préexistants dans `/var/backups/wishgifthub/<horodatage>/`.
2. Crée le compte système `wishgifthub` sans connexion interactive.
3. Copie les fichiers dans `/opt/wishgifthub/releases/<horodatage>/` et positionne les liens `frontend` et `backend`.
4. Crée `/etc/wishgifthub/.env` en mode `600` avec des valeurs fictives uniquement si ce fichier n'existe pas déjà ; il ne remplace jamais un fichier existant.
5. Installe le service et le contrôle de configuration. Java écoutera sur `127.0.0.1:8080`, avec `-Xms64m -Xmx256m -XX:+UseSerialGC`.
6. Installe `/etc/nginx/snippets/wishgifthub.conf` et son inclusion dans le serveur existant ; teste `nginx -t` avant rechargement. En cas de validation échouée, restaure le fichier Nginx sauvegardé.
7. Exécute `systemctl daemon-reload` puis `systemctl enable --now wishgifthub` et vérifie l'état des applications existantes.

Le chemin `/wishgifthub` redirige vers `/wishgifthub/`. Les routes Vue retombent sur `index.html`, les ressources inexistantes sous `assets/` renvoient 404. `/wishgifthub/api/` est transmis à `/api/` sur le backend ; si celui-ci est arrêté, Nginx retourne une réponse JSON HTTP 503 explicite.

### Compilations et tests effectués

- Backend : `mvn package -pl wishgifthub-api -am -B -q` réussi, 46 tests répartis en 12 suites, aucune erreur ni échec. Le journal local de compilation est dans `rssh/backend-build.log` (ignoré par Git).
- JAR : `wishgifthub-api/target/wishgifthub-api-1.0-SNAPSHOT.jar`, 61 397 728 octets, SHA-256 `368df35a0362b2e12e04ef9e705a4d3462fba3f0a06da7e26296759066fc3b09`.
- Frontend : Node local `22.23.4`, `npm ci` terminé. Le lancement sous le bac à sable a rencontré `spawn EPERM` ; la compilation a ensuite été exécutée avec les droits nécessaires à la création des processus.
- Le script `npm run build -- --base=/wishgifthub/` ne transmettait pas effectivement l'argument à Vite dans le script parallèle existant. Un script explicite `npm run build:pi` a été ajouté et utilisé pour le build final : `vite build --base=/wishgifthub/`, avec contrôle TypeScript réussi. Le HTML compilé référence bien `/wishgifthub/assets/...` et `/wishgifthub/favicon.svg`.
- Vitest ne reconnaît pas l'option CLI `--base` : le préfixe est maintenant fixé dans sa configuration de test. Les 11 tests ciblés du client API et des invitations passent sous `/wishgifthub/`, y compris le repli de copie en HTTP.

Commandes finales frontend depuis `wishgifthub-ui` :

```powershell
$env:Path = "D:/Dev/wishgifthub/rssh/build-tools/node_modules/node/bin;$env:Path"
& D:/Dev/wishgifthub/rssh/build-tools/node_modules/node/bin/node.exe 'C:/Program Files/nodejs/node_modules/npm/bin/npm-cli.js' run build:pi
& D:/Dev/wishgifthub/rssh/build-tools/node_modules/node/bin/node.exe node_modules/vitest/vitest.mjs run src/api/client.spec.ts src/components/__tests__/InvitationsDialog.spec.ts
```

### Transport et archive

Le client SSH Windows a de nouveau bloqué ; un essai de redirection de ses sorties dans un script local `rssh/run-pi.ps1` a d'abord rencontré un verrou de fichier, puis expiré sans modification distante. Le transport final utilise `deploy/pi/pi_remote.py`, via Paramiko installé localement :

```powershell
python -m pip install --target rssh/python-tools paramiko
python deploy/pi/pi_remote.py --script rssh/preflight.sh --timeout 60
```

Le script Python vérifie la clé d'hôte à partir de `~/.ssh/known_hosts`, refuse les hôtes inconnus, charge la clé privée sans la copier et ferme les connexions. Il exécute les scripts Bash encodés en Base64 et permet les transferts SFTP. Paramiko installé : `5.1.0`. Le diagnostic final a confirmé l'absence d'installation WishGiftHub préalable et les trois services existants actifs.

Archive préparée dans `rssh/wishgifthub-release.tgz` avec `C:/Windows/System32/tar.exe` (le `tar.exe` MinGW du PATH interprète incorrectement les chemins Windows avec lettre de lecteur). Arborescence source : `rssh/package/{frontend/wishgifthub,backend,deploy/pi}`. Le frontend est copié depuis `wishgifthub-ui/dist/`, le JAR est renommé `backend/app.jar`, les fichiers de déploiement sont copiés depuis `deploy/pi/`.

```powershell
& C:/Windows/System32/tar.exe -czf rssh/wishgifthub-release.tgz -C rssh/package frontend backend deploy
python deploy/pi/pi_remote.py --upload rssh/wishgifthub-release.tgz --remote /home/pi/wishgifthub-release.tgz --timeout 180
```

Archive finale : 57 844 515 octets, SHA-256 `ab7cc053d8ab60de27f633a3a393642c7763e3d2c79414743a243d09224ca897`.

Un premier contrôle SHA-256 distant a été lancé alors que le transfert n'était pas encore terminé ; il a refusé l'archive incomplète et arrêté le script avant toute extraction ou installation. Le contrôle doit être relancé seulement après la fin du transfert.

### Installation effectuée

Le transfert terminé, le script distant suivant a été exécuté via `python deploy/pi/pi_remote.py --script rssh/deploy.sh --timeout 180` :

```bash
set -euo pipefail
echo 'ab7cc053d8ab60de27f633a3a393642c7763e3d2c79414743a243d09224ca897  /home/pi/wishgifthub-release.tgz' | sha256sum -c -
mkdir -p /home/pi/wishgifthub-stage-20261108
tar -xzf /home/pi/wishgifthub-release.tgz -C /home/pi/wishgifthub-stage-20261108
cd /home/pi/wishgifthub-stage-20261108
sudo -n bash deploy/pi/install.sh | tee installation.log
```

Résultats : contrôle d'intégrité réussi, installation terminée le 8 novembre 2026 vers 18 h 06 (Europe/Paris).

| Élément | Emplacement / état |
| --- | --- |
| Version installée | `/opt/wishgifthub/releases/20261108-180558/` |
| Frontend actif | `/opt/wishgifthub/frontend` → `releases/20261108-180558/frontend` |
| Backend installé | `/opt/wishgifthub/backend` → `releases/20261108-180558/backend` |
| JAR | `/opt/wishgifthub/backend/app.jar` ; empreinte identique au JAR local |
| Configuration sensible | `/etc/wishgifthub/.env`, propriétaire `root:root`, permissions `600` ; répertoire parent `700` |
| Service | `/etc/systemd/system/wishgifthub.service`, activé au démarrage (`enabled`) |
| Contrôle des paramètres | `/usr/local/libexec/wishgifthub-check-env` |
| Routage | `/etc/nginx/snippets/wishgifthub.conf`, inclus dans le serveur par défaut existant |
| Sauvegarde initiale | `/var/backups/wishgifthub/20261108-180558/` |
| Sources de l'installation | `/home/pi/wishgifthub-stage-20261108/`, avec `installation.log` |

Le service est actuellement **inactif volontairement**, car `ExecCondition` détecte `CHANGE_ME` dans `SPRING_DATASOURCE_URL`. Le journal systemd indique `Skipped due to 'exec-condition'`. Java n'a pas lancé l'application et aucune migration Supabase n'a été exécutée.

### Vérifications effectuées

Le script de contrôle est conservé dans `deploy/pi/verify.sh`. Sa première exécution était depuis `rssh/verify.sh` ; la copie suivie par Git détecte désormais les noms des ressources dans le HTML au lieu de figer leurs empreintes de build.

```powershell
python deploy/pi/pi_remote.py --script deploy/pi/verify.sh --timeout 90
```

Contrôles HTTP réussis :

- `/wishgifthub` : 301 vers `/wishgifthub/`.
- `/wishgifthub/`, route profonde de groupe, route d'invitation, `version.json`, favicon, JavaScript et CSS : 200.
- Ressource inexistante `/wishgifthub/assets/missing.js` : 404.
- `/wishgifthub/api/version` : 503 JSON, attendu tant que le `.env` est fictif.
- `/flightsnipper/` : 200 ; racine `/` : 404 comme avant.
- Accès `http://192.168.0.50/wishgifthub/` : 200.
- Gabrielle sur le port 8765 et via le nom `gabrielle.coaching` dans Nginx : 200.

Autres commandes de contrôle, contenues dans le script :

```bash
sudo -n nginx -t
systemctl is-active nginx flightsniper gabrielle-coaching
systemctl is-enabled wishgifthub
systemctl show wishgifthub -p ActiveState -p SubState -p ExecCondition
sudo -n journalctl -u wishgifthub -n 6 --no-pager
sudo -n stat -c '%a %U:%G %n' /etc/wishgifthub /etc/wishgifthub/.env
sudo -n -u wishgifthub test ! -r /etc/wishgifthub/.env
sha256sum /opt/wishgifthub/backend/app.jar
readlink -f /opt/wishgifthub/frontend
readlink -f /opt/wishgifthub/backend
```

Nginx valide et les trois services existants sont toujours actifs. Le `.env` est lu par systemd en root ; le compte applicatif ne peut pas lire ce fichier directement. Aucune vérification fonctionnelle avec Supabase n'est possible avant fourniture des vrais paramètres. Les contrôles frontend sont des tests automatisés et HTTP ; aucune session de navigateur interactive n'a été effectuée.

L'ancien processus SSH Windows de diagnostic a été fermé après vérification (même filtre sur le nom de clé temporaire et la destination que lors de l'installation Java). Les connexions finales Paramiko se ferment normalement.

### À faire par l'utilisateur : compléter le .env et démarrer le backend

Sur le Pi :

```bash
sudo nano /etc/wishgifthub/.env
```

Remplacer toutes les valeurs contenant `CHANGE_ME` par les vrais paramètres. Le format est celui de `systemd EnvironmentFile` : `NOM=valeur`, sans `export` ; entourer les valeurs de guillemets si elles contiennent des espaces. Ne pas utiliser de substitutions comme `${AUTRE_VARIABLE}`. Il faut les identifiants PostgreSQL Supabase, pas les clés API `anon` ou `service_role`. Le secret JWT doit être aléatoire et faire au moins 32 octets pour HMAC-SHA256. Ne jamais publier ce fichier ni copier ses valeurs dans ce journal.

L'hôte et le port sont ceux indiqués par Supabase dans **Connect**. Pour un backend sur un réseau IPv4 uniquement, Supabase propose le pooler en mode **session** ; la connexion directe nécessite IPv6 ou l'option IPv4. Source : [documentation Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres). Conserver le paramètre TLS `sslmode=require` dans l'URL JDBC. L'URL des invitations est déjà configurée sur `http://192.168.0.50/wishgifthub/invite/`.

Le premier vrai démarrage lance Flyway : vérifier que l'on cible la bonne base et que son schéma correspond aux migrations du projet. Les migrations applicables seront exécutées ; Hibernate est configuré en validation (`ddl-auto=validate`).

Après modification du `.env` (aucune recompilation ni `daemon-reload` nécessaire) :

```bash
sudo systemctl restart wishgifthub
sudo systemctl status wishgifthub --no-pager
sudo journalctl -u wishgifthub -n 100 --no-pager
curl -f http://127.0.0.1:8080/actuator/health
curl -f http://192.168.0.50/wishgifthub/api/version
```

Sur ce Pi, le démarrage Spring Boot peut prendre un certain temps. Les commandes `curl` doivent réussir après la fin du démarrage. Si la configuration Supabase est erronée, corriger le `.env` puis redémarrer ; après plusieurs échecs rapprochés, utiliser `sudo systemctl reset-failed wishgifthub` avant de réessayer. Les journaux restent sur le Pi et peuvent contenir des informations de connexion : ne pas les partager sans les expurger.

Pour le script de vérification après activation du backend, définir `WISHGIFTHUB_EXPECT_API=200` dans le script distant avant d'exécuter le contrôle ; sa valeur par défaut est `503`, correspondant à cette installation avec configuration fictive.

### Retour arrière et entretien

La configuration Nginx précédente est conservée dans `/var/backups/wishgifthub/20261108-180558/nginx-default.conf`. Pour retirer le routage WishGiftHub et arrêter son service (commandes non exécutées) :

```bash
sudo systemctl disable --now wishgifthub
sudo cp -a /var/backups/wishgifthub/20261108-180558/nginx-default.conf "$(readlink -f /etc/nginx/sites-enabled/flightsniper)"
sudo nginx -t && sudo systemctl reload nginx
```

Cette restauration complète du serveur ne convient que si aucune autre modification Nginx n'est intervenue depuis ; sinon retirer uniquement l'inclusion WishGiftHub. Les fichiers et le `.env` sont conservés. Pour une prochaine livraison, compiler avec `npm run build:pi`, reconstruire le JAR, créer une nouvelle archive, vérifier son empreinte puis exécuter le script d'installation. Celui-ci conserve le `.env` existant. Ne pas utiliser automatiquement les anciennes empreintes de cette intervention pour une nouvelle livraison.

**État final : frontend accessible, backend et service installés, démarrage backend en attente du `.env` réel.**

Une copie de ce journal est également installée sur le Pi dans `/opt/wishgifthub/INSTALLATION_PI.md` : transfert SFTP vers `/home/pi/INSTALLATION_PI.md`, puis `sudo install -m 644 /home/pi/INSTALLATION_PI.md /opt/wishgifthub/INSTALLATION_PI.md`. Le fichier du dépôt reste la source à mettre à jour lors des prochaines interventions.

## 2026-11-08 — Configuration Supabase partielle

L'utilisateur a fourni les paramètres de connexion et JWT, en précisant qu'il complétera lui-même le mot de passe. Mise à jour de `/etc/wishgifthub/.env` pour les quatre variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `WISHGIFTHUB_JWT_SECRET` et `WISHGIFTHUB_JWT_EXPIRATION_MS` ; aucune de leurs valeurs sensibles n'est consignée ici. Expiration JWT : 604800000 ms, soit 7 jours. Le formatage Markdown fourni autour de l'URL a été retiré et `?sslmode=require` conservé dans l'URL JDBC.

Commande locale :

```powershell
python deploy/pi/update_env.py
```

Le script reçoit les quatre paramètres JSON par saisie masquée, les transmet sur l'entrée standard d'un processus Python distant exécuté avec `sudo -n`, et remplace atomiquement le `.env` en préservant les autres lignes. Les valeurs ne figurent ni dans les arguments de processus, ni dans un script enregistré, ni dans les sorties. Le fichier reste `root:root`, mode `600`. Le helper ne permet pas de modifier le mot de passe : la ligne `SPRING_DATASOURCE_PASSWORD` est conservée à compléter sur le Pi. Le secret JWT reste exclusivement dans le `.env` distant.

Aucun démarrage du backend ni connexion à Supabase ne sont effectués pendant cette intervention. Après saisie du vrai mot de passe avec `sudo nano /etc/wishgifthub/.env`, exécuter `sudo systemctl restart wishgifthub` puis vérifier son état comme décrit plus haut.

Contrôle distant effectué sans afficher les valeurs :

```bash
sudo -n grep -q '^SPRING_DATASOURCE_PASSWORD=CHANGE_ME$' /etc/wishgifthub/.env
sudo -n stat -c '%a %U:%G %n' /etc/wishgifthub/.env
systemctl show wishgifthub -p ActiveState -p SubState
```

Résultats : mot de passe toujours `CHANGE_ME`, permissions `600 root:root`, backend `inactive/dead` comme prévu. Journal local retransféré via `pi_remote.py --upload INSTALLATION_PI.md --remote /home/pi/INSTALLATION_PI.md`, puis copié avec `sudo -n install -m 644 /home/pi/INSTALLATION_PI.md /opt/wishgifthub/INSTALLATION_PI.md`.

## 2026-11-08 — Démarrage après saisie du mot de passe : approbation requise

L'utilisateur confirme avoir complété le mot de passe. Un script local `rssh/start-backend.sh` a été préparé pour exécuter `sudo -n systemctl restart wishgifthub`, attendre la réponse de `/actuator/health` puis vérifier `/wishgifthub/api/version`. Le script n'a pas été exécuté sur le Pi : le contrôle automatique d'approbation a rejeté la commande SSH avant son lancement, car le démarrage Spring Boot peut appliquer les migrations Flyway sur la base Supabase et modifier son schéma. Une autorisation explicite de ces migrations est demandée avant une nouvelle tentative. Aucun paramètre ni réglage Flyway n'a été changé pour contourner ce refus.

## 2026-11-08 — Démarrage autorisé avec migrations Flyway

L'utilisateur a explicitement autorisé le démarrage et les éventuelles migrations Supabase. Commande locale exécutée :

```powershell
python deploy/pi/pi_remote.py --script rssh/start-backend.sh --timeout 240
```

Le script exécute `sudo -n systemctl restart wishgifthub`, vérifie l'état systemd, puis attend `/actuator/health` sur `127.0.0.1:8080` (jusqu'à 60 tentatives espacées de 3 secondes) avant de tester `/wishgifthub/api/version` via Nginx. Début du démarrage à 18 h 31 (Europe/Paris). État initial : `active/running`, PID 4174.

Diagnostic local : `python deploy/pi/pi_remote.py --script rssh/backend-diagnostics.sh --timeout 45`. Ce script lit l'état du service et sélectionne les messages de démarrage, connexion, migrations et erreurs de `journalctl -u wishgifthub -n 150 --no-pager`. Les valeurs du `.env` sont retirées des messages avant affichage ; le fichier de configuration lui-même n'est pas affiché.

### Résultats vérifiés

- Service `wishgifthub` : `active/running`, activé au démarrage ; contrôle `ExecCondition` réussi (code 0), aucun redémarrage automatique pendant le démarrage observé.
- Connexion PostgreSQL Supabase établie par HikariCP.
- Flyway : validation réussie de 10 entrées de migration ; schéma `public` déjà en version **9**, message `Schema "public" is up to date. No migration necessary.` Aucune migration supplémentaire n'a été appliquée lors de ce démarrage.
- `/actuator/health` sur `127.0.0.1:8080` : HTTP 200, `{"status":"UP"}` (le contrôle de santé base de données est activé).
- `/wishgifthub/api/version` via Nginx : HTTP 200, version applicative `1.0-SNAPSHOT`.
- Le démarrage complet a pris plusieurs minutes sur le Pi 2 ; les futurs redémarrages doivent laisser le temps au service de terminer son initialisation.
- `MemoryCurrent` n'est pas renseigné par systemd sur ce système ; cette propriété ne permet donc pas de conclure à une consommation mémoire nulle.

Vérification du déploiement actif :

```powershell
python deploy/pi/pi_remote.py --script rssh/verify-active.sh --timeout 60
```

Ce script est construit à partir de `deploy/pi/verify.sh`, précédé de `export WISHGIFTHUB_EXPECT_API=200`. La lecture brute de `journalctl` est retirée : les journaux ont été examinés séparément avec masquage des valeurs sensibles. Les contrôles HTTP du frontend, des routes profondes, des ressources statiques et de l'API sont réussis. FlightSniper et Gabrielle répondent toujours en HTTP 200 ; leurs services et Nginx restent actifs. Le `.env` conserve les permissions `600 root:root`, dans un répertoire `700`.

Flyway a émis un avertissement de compatibilité : PostgreSQL 17.6 est plus récent que les versions testées avec le Flyway embarqué (jusqu'à PostgreSQL 16). Cet avertissement n'a pas empêché validation, connexion et démarrage ; prévoir la mise à jour des dépendances lors d'une prochaine maintenance, sans changement de version pendant cette installation.

Le journal est synchronisé vers le Pi avec `python deploy/pi/pi_remote.py --upload INSTALLATION_PI.md --remote /home/pi/INSTALLATION_PI.md --timeout 60`, puis installé par le script `rssh/install-journal.sh` (`sudo -n install -m 644 /home/pi/INSTALLATION_PI.md /opt/wishgifthub/INSTALLATION_PI.md`).

**État final : frontend et backend opérationnels sur `http://192.168.0.50/wishgifthub/`, connexion Supabase vérifiée.** Aucun compte de test ni donnée métier n'a été créé pour ces vérifications.

## 2026-11-22 — Déploiement des invitations WhatsApp

- Frontend compilé avec `npm run build:pi` et publié dans `/opt/wishgifthub/releases/whatsapp-20261122-142655-6b8d1e6e/frontend`. Le lien symbolique `/opt/wishgifthub/frontend` a été basculé après vérification de l’archive SHA-256. Le frontend précédent reste disponible dans la release `20261108-180558`.
- Le service WishGiftHub a été redémarré avec son JAR et son fichier d’environnement existants. Flyway a validé le schéma et n’a appliqué aucune migration. Le service est actif, le contrôle de santé répond `UP` et la version de l’API est accessible via Nginx.
- La page, le bundle contenant la fonctionnalité WhatsApp et le modèle `invitation-whatsapp.txt` répondent en HTTP 200 depuis le réseau local.
