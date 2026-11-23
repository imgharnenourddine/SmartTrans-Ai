# Guide de démonstration — SmartTrans-AI

Démonstration en direct à partir des rôles, des pages et des règles réellement présents dans l’application Jakarta EE et React. Aucun rôle supplémentaire n’a été créé.

Les comptes sont insérés par la migration Flyway `backend/src/main/resources/db/migration/V4__seed_demo_accounts.sql`. Elle est idempotente : un e-mail déjà présent n’est pas modifié. Le mot de passe n’est stocké qu’en empreinte BCrypt, produite par le même `BCryptPasswordEncoder` que `Passwords.java`. Les clients, véhicules, chauffeurs et missions proviennent de `V3__seed_demo_data.sql`.

Le formulaire de connexion est prérempli avec `admin@smarttrans.ai` / `Password1!`. Ce couple appartient au bootstrap Spring de développement, qui ne s’exécute que si la table `users` est vide. Après la V4, ces identifiants ne sont pas ceux de la démonstration : les remplacer à l’écran.

## Comptes

Mot de passe commun, facile à saisir en direct : `Demo2026!`

| Rôle | Profil | E-mail | Mot de passe | Données associées |
| --- | --- | --- | --- | --- |
| `ADMIN` | Amina El Fassi | `admin@demo.smarttrans.ai` | `Demo2026!` | Accès complet, y compris Utilisateurs et Rôles |
| `TRANSPORT_MANAGER` | Hassan Berrada | `responsable@demo.smarttrans.ai` | `Demo2026!` | Missions DB-01 à DB-07, journal d’activités |
| `DRIVER` | Fatima Zahra | `chauffeur@demo.smarttrans.ai` | `Demo2026!` | Fiche chauffeur `LIC-DB-702`, mission `DB-02` |

Il n’existe pas de rôle Client. Les sociétés Atlas Freight, Maghreb Logistics, etc. sont des fiches client, pas des comptes.

## Permissions vérifiées dans le code

| Fonction | Administrateur | Responsable transport | Chauffeur |
| --- | --- | --- | --- |
| Tableau de bord `/dashboard` | Tous les indicateurs | Tous les indicateurs | Uniquement ses missions et sa fiche |
| Chauffeurs, véhicules, clients | Liste, création, modification | Liste, création, modification | Pas de menu. Une fiche ouverte par URL reste lisible si l’API l’autorise |
| Missions | Liste, création, modification, suppression, statuts | Identique | Uniquement ses missions. Démarrer et terminer. Pas d’annulation, pas de modification, pas de suppression |
| Assistant `/assistant` | Oui | Oui | Refus (page et API) |
| Activités `/activities` | Oui | Oui | Refus (page et API) |
| Utilisateurs `/users` et rôles `/roles` | Oui | Refus | Refus |
| Profil `/profile` | Refus, réservé au rôle `DRIVER` | Refus | Oui. Le changement de disponibilité est masqué si le statut est `ON_MISSION` ou `SUSPENDED` |

Côté API, les mêmes portes sont dans `access.requireAny` de chaque ressource JAX-RS. Une page refusée renvoie vers `/dashboard`.

Règles métier utiles pendant la démo :

- Une mission naît au statut `PLANIFIED`. Le client est obligatoire. Le chauffeur et le véhicule peuvent être vides jusqu’au démarrage.
- Démarrer exige un chauffeur disponible, un permis non expiré, un véhicule disponible, une assurance et une visite technique non expirées. Le chauffeur passe `ON_MISSION` et le véhicule `IN_USE`.
- Terminer ou annuler une mission en cours libère le chauffeur et le véhicule.
- Le chauffeur ne peut pas annuler.
- La réaffectation n’est possible que tant que la mission est planifiée, et pas vers un chauffeur déjà en mission ou un véhicule déjà en service.
- La suppression n’est possible que pour une mission planifiée ou annulée, et seulement par l’administrateur ou le responsable.

## Données déjà présentes

| Référence | Statut | Client | Chauffeur | Véhicule | Trajet |
| --- | --- | --- | --- | --- | --- |
| DB-01 | Planifiée | Atlas Freight | Karim Benali | DB-701-A | Casablanca → Marrakech |
| DB-02 | Planifiée | Maghreb Logistics | Fatima Zahra | DB-702-A | Rabat → Fès |
| DB-03 | Planifiée | Sud Transport | Youssef Amrani | DB-703-A | Agadir → Tanger |
| DB-04 | En cours | Nord Cargo | Nadia El Idrissi | DB-704-A | Casablanca → Oujda |
| DB-05 | En cours | Oriental Express | Omar Tazi | DB-705-A | Fès → Nador |
| DB-06 | Annulée | Sahara Fret | Sara Bennani | DB-706-A | Laâyoune → Agadir |
| DB-07 | Terminée | Fes Distribution | Mehdi Kabbaj | DB-707-A | Fès → Casablanca |

Alertes visibles sur le tableau de bord gestionnaire, fenêtre de 30 jours :

- permis de Karim Benali, échéance à 15 jours ;
- assurance de DB-701-A, échéance à 12 jours ;
- assurance de la remorque DB-707-A, échéance à 8 jours.

DB-02 est le fil conducteur du chauffeur : Fatima est disponible, son permis est valable, le fourgon DB-702-A est disponible et ses documents ne sont pas expirés. Le bouton **Démarrer** est donc réellement utilisable.

## Ordre recommandé

1. Administrateur, environ 3 minutes : montrer que les comptes et les rôles existent, puis se déconnecter.
2. Responsable transport, environ 8 minutes : tableau de bord, consultation de DB-01, création d’une mission, assistant, journal.
3. Chauffeur, environ 4 minutes : ses missions, démarrage de DB-02, profil bloqué en mission.
4. Retour bref sur le responsable, environ 2 minutes : DB-02 est en cours, Fatima n’est plus disponible.

Se déconnecter entre chaque compte. Le menu **Déconnexion** est en bas de la barre latérale.

## 1. Administrateur — Amina El Fassi

- E-mail : `admin@demo.smarttrans.ai`
- Mot de passe : `Demo2026!`
- Page d’arrivée : `/dashboard`

### Actions

1. Sur `/login`, remplacer l’e-mail et le mot de passe préremplis, puis valider **Connexion**.
2. Rester sur le tableau de bord. Montrer les indicateurs de missions, chauffeurs, véhicules et clients, puis les alertes de permis et de documents.
3. Ouvrir **Utilisateurs**. La liste contient Amina El Fassi, Hassan Berrada et Fatima Zahra, avec leur rôle.
4. Ouvrir une fiche, par exemple Hassan Berrada, pour montrer le rôle `TRANSPORT_MANAGER` sans le modifier.
5. Ouvrir **Rôles** via l’URL `/roles` (le menu n’affiche que Utilisateurs). Les trois rôles sont Administrateur, Responsable transport et Chauffeur.
6. Montrer que le menu gestionnaire est aussi présent : Chauffeurs, Véhicules, Clients, Missions, Assistant, Activités.
7. Se déconnecter.

### Résultat attendu

Le tableau de bord s’affiche avec les sept missions. La liste des utilisateurs montre les trois comptes de démonstration. Un responsable ou un chauffeur qui tenterait `/users` ou `/roles` serait renvoyé au tableau de bord.

### À dire au jury

L’administrateur est le seul acteur qui gère les comptes. Le contrôle est le même dans React (`ProtectedRoute`) et dans l’API (`UserResource` et `RoleResource` exigent `ADMIN`). Les autres menus montrent que ce rôle couvre aussi l’exploitation, sans qu’un second compte soit nécessaire pour la suite.

Ne pas créer d’utilisateur pendant la démo : le formulaire fonctionne, mais un compte supplémentaire n’apporte rien au scénario.

## 2. Responsable transport — Hassan Berrada

- E-mail : `responsable@demo.smarttrans.ai`
- Mot de passe : `Demo2026!`
- Page d’arrivée : `/dashboard`

### Actions

1. Se connecter. Le menu **Utilisateurs** est absent.
2. Sur le tableau de bord, citer les cartes de missions planifiées, en cours et terminées, puis les alertes : permis de Karim Benali, assurance de DB-701-A et de DB-707-A.
3. Ouvrir **Missions**. Montrer les statuts Planifiée, En cours, Annulée et Terminée. Ouvrir **DB-01** : client Atlas Freight, chauffeur Karim Benali, véhicule DB-701-A, trajet Casablanca → Marrakech, marchandise Textile, 4 200 kg.
4. Revenir à la liste, puis **Nouvelle mission**. Saisir exactement :
   - Référence : `DB-DEMO`
   - Client : Atlas Freight
   - Départ : `Casablanca`
   - Destination : `Tanger`
   - Date : un créneau de la semaine en cours
   - Marchandise : `Colis express`
   - Poids : `900`
   - Chauffeur : Sara Bennani
   - Véhicule : DB-706-A
5. Valider **Créer la mission**. La fiche s’ouvre au statut Planifiée, avec les boutons Modifier, Démarrer, Annuler et Supprimer.
6. Ouvrir **Assistant**. Cliquer la suggestion `Quel chauffeur et véhicule puis-je affecter ?`.
7. Ouvrir **Activités**. Le journal contient les créations de DB-01, DB-02 et DB-04, puis la création de DB-DEMO faite à l’instant. Ouvrir la ligne DB-DEMO.
8. Se déconnecter.

### Résultat attendu

DB-DEMO apparaît dans la liste, affectée à Sara Bennani et au véhicule DB-706-A, tous deux disponibles. L’assistant répond avec des citations issues de la base (chauffeurs, véhicules, missions). Le mode affiché est `LLM` si `MISTRAL_API_KEY` est configurée, sinon `GROUNDED` : la réponse reste construite à partir des données, sans appel au modèle. Le journal affiche l’action Création sur la mission DB-DEMO.

### À dire au jury

Le responsable planifie sans être administrateur. La mission peut être enregistrée avant le départ, avec un client obligatoire et des ressources contrôlées : un chauffeur déjà en mission ou un véhicule déjà en service serait refusé. L’assistant ne répond pas de mémoire : il interroge les missions, les disponibilités et les échéances, puis cite ces enregistrements. Le journal d’activités trace la création sans action manuelle.

Ne pas démarrer DB-01 ni DB-DEMO ici. Le démarrage est réservé au chauffeur, sur DB-02.

## 3. Chauffeur — Fatima Zahra

- E-mail : `chauffeur@demo.smarttrans.ai`
- Mot de passe : `Demo2026!`
- Page d’arrivée : `/dashboard`

Le compte est lié à la fiche `LIC-DB-702`. Sans ce lien, l’API ne rattache aucune mission au chauffeur.

### Actions

1. Se connecter. Le menu contient Dashboard, Mes missions et Mon profil. Pas de Chauffeurs, Véhicules, Clients, Assistant, Activités ni Utilisateurs.
2. Sur le tableau de bord, montrer que seules les missions de Fatima apparaissent. Avant le démarrage, c’est DB-02, Rabat → Fès, statut Planifiée.
3. Ouvrir **Mes missions**, puis **DB-02**. Vérifier Maghreb Logistics, Fatima Zahra, DB-702-A, pièces détachées, 1 800 kg.
4. Cliquer **Démarrer**.
5. Ouvrir **Mon profil**. Le statut est En mission. Le contrôle de disponibilité n’est pas proposé.
6. Se déconnecter.

### Résultat attendu

DB-02 passe à En cours et la date de démarrage se remplit. Il n’y a pas de bouton Annuler, Modifier ni Supprimer. Le profil indique que Fatima est en mission. Une tentative d’URL `/assistant` ou `/users` ramène au tableau de bord.

### À dire au jury

Le chauffeur ne voit pas le parc. La liste et le détail sont filtrés sur la fiche liée à son compte. Démarrer est une transition contrôlée : permis, disponibilité, assurance et visite technique sont vérifiés, puis le chauffeur et le véhicule sont réservés. L’annulation reste une décision du responsable.

## 4. Retour responsable — effet du démarrage

Se reconnecter avec `responsable@demo.smarttrans.ai`.

1. Ouvrir **Missions** et constater que DB-02 est En cours.
2. Ouvrir **Chauffeurs** : Fatima Zahra est En mission. Nadia El Idrissi et Omar Tazi l’étaient déjà à cause de DB-04 et DB-05. Sara Bennani reste disponible, affectée à DB-DEMO.
3. Ouvrir **Véhicules** : DB-702-A est En service.
4. Ouvrir **Activités** : un changement de statut de DB-02 s’est ajouté au journal.

### À dire au jury

Une action du chauffeur met à jour l’exploitation vue par le responsable, sans double saisie. Les ressources occupées ne peuvent pas être réaffectées à une autre mission planifiée.

## Limites à connaître avant de monter sur scène

- Les notifications existent en base, sans écran. Ne pas les annoncer.
- Le module documents a été retiré (migration V2). Ne pas ouvrir ce sujet.
- Il n’y a pas d’espace client.
- La connexion met à jour `last_login_at`. Elle n’écrit pas de ligne « Connexion » dans le journal, même si ce type d’action existe dans le modèle.
- Sans clé Mistral, l’assistant reste utilisable en mode `GROUNDED`. Avec une clé invalide ou un délai dépassé, le service retombe aussi sur cette réponse. Le prévoir si la salle n’a pas d’accès sortant.
- La V4 ne réinitialise pas les statuts. Si DB-02 a déjà été démarrée pendant une répétition, Fatima n’a plus le bouton Démarrer : la fiche propose **Terminer**. Pour rejouer le démarrage sur une base déjà utilisée, il faut une base réinitialisée, ou terminer DB-02 au préalable puis s’appuyer sur une autre mission planifiée dont le chauffeur possède un compte. Aujourd’hui, seul le compte de Fatima est un chauffeur connecté.
- DB-DEMO reste en base après la soutenance. Sa référence est unique : une seconde création le même jour renvoie un conflit 409.
- Ne pas appliquer cette migration sur une base de production réelle si ces comptes de démonstration n’y ont pas leur place. Elle n’altère pas les comptes déjà créés, mais elle ajoute trois utilisateurs et relie Fatima Zahra à `chauffeur@demo.smarttrans.ai` lorsque sa fiche n’a pas encore d’utilisateur.

## Vérification technique

L’empreinte stockée a été contrôlée avec `BCryptPasswordEncoder` de Spring Security Crypto 6.4.5, la classe utilisée par l’API : `Demo2026!` correspond au hash, `Password1!` ne correspond pas. Le script n’insère un utilisateur que si l’e-mail est absent, n’ajoute un rôle que s’il manque, et ne remplace pas un `user_id` de chauffeur déjà occupé par un autre compte.

Au moment de cette préparation, ni PostgreSQL ni l’API n’étaient démarrés. La connexion HTTP et le contrôle d’accès en situation réelle n’ont donc pas été rejoués. Les pages et les refus décrits ci-dessus viennent des routes React et des `requireAny` JAX-RS. Au premier démarrage, Flyway applique la V4 avant l’ouverture de l’application.
