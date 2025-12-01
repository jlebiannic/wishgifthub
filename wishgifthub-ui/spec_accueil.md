### **Spécification IHM / UI – Parcours Admin & Utilisateur**

#### **1. Page d’accueil**

*   **Objectif :** Point d’entrée unique pour tous les utilisateurs.
*   **Éléments :**
    *   **Titre clair** : « Bienvenue sur \[Nom de l’application] »
    *   **Section Connexion :**
        *   **Champ identifiant + mot de passe**
        *   **Bouton “Se connecter”**
        *   **Précision visible** : *« La connexion est réservée aux administrateurs »*
    *   **Lien d’information** : « En savoir plus sur les rôles » (optionnel pour UX)

***

#### **2. État après connexion (Admin)**

*   **Affichage conditionnel :**
    *   Si **admin connecté** :
        *   **Bouton “Créer un groupe”** (visible uniquement pour admin)
        *   **Liste des groupes** auxquels l’admin appartient
            *   Chaque groupe affiché sous forme de **carte ou ligne** avec :
                *   Nom du groupe
                *   **Icône “œil”** à droite → *Action : Afficher les invitations (acceptées / en attente / refusées)*
          *   **Section Invitations** (pop-up ou panneau latéral) :
                *   Possibilité d'inviter un user par son adresse email 
                *   Liste des membres invités avec statut :
                    *   Accepté
                    *   En attente

***

#### **3. État pour un utilisateur non admin**

*   **Pas de bouton “Créer un groupe”**
*   **Liste des groupes** auxquels il appartient (même présentation que pour admin, mais sans icône “œil”)
*   **Pas d’accès aux statuts des invitations**
*   **Lorqu'un user entre l'url avec le token de l'invitation alors ce token est pris et l'api invitation acceptée est appelée** Il n'y alors pas de mire de connexion et seulement l'affichage classique pour un user standard non admin 

***

#### **4. Principes UX**

*   **Interface intuitive :**
    *   Boutons bien identifiés, labels explicites
    *   Icônes avec info-bulle (*ex. : “Voir les invitations”*)
*   **Navigation simple :**
    *   Page d’accueil → Connexion → Tableau de bord
*   **Feedback utilisateur :**
    *   Messages clairs en cas d’erreur (ex. : “Accès réservé aux administrateurs”)
*   **Responsive design** pour desktop et mobile

