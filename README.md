# messaging-cacib

Application de routage IBM MQ → PostgreSQL avec IHM de consultation.

## Stack

- **Backend** : Java 21, Spring Boot 4.0.x (Jackson 3 par défaut), Spring JMS +
  IBM MQ starter 4.1.0, Spring Data JPA, Flyway (`spring-boot-starter-flyway`),
  springdoc-openapi 3.0.x, MapStruct
- **Frontend** : Angular 22, standalone components, signals, Angular Material 22
- **Base de données** : PostgreSQL 16
- **Broker** : IBM MQ 9.4 (image `icr.io/ibm-messaging/mq`, licence développeur)
- **Tests** : JUnit 5, Testcontainers, MockMvc

## Structure du repo

```
messaging-cacib/
├── backend/            projet Maven Spring Boot (architecture hexagonale)
├── frontend/           application Angular 22 (liste/détail des messages)
├── mq-config/          image IBM MQ dérivée (provisioning MQSC de la backout queue)
├── docker-compose.yml  toute la stack : backend, frontend, IBM MQ, PostgreSQL
└── .env.example        modèle de configuration (copier en .env)
```

## Pré-requis

- **Pour juste lancer la démo** : Docker Desktop avec **≥ 4 Go RAM alloués**
  (l'image IBM MQ est lourde, ~2 Go, démarrage 30-60 s). Rien d'autre — Java,
  Maven et Node ne sont pas nécessaires, tout tourne en containers.
- **Pour développer** : en plus de Docker, **Java 21+**, **Maven 3.9+**,
  **Node.js 20+** (testé avec Node 24) et **Angular CLI 22**.

## Démarrage rapide — toute la stack en Docker

C'est le chemin le plus simple pour essayer l'application : backend, frontend,
PostgreSQL et IBM MQ tournent chacun dans leur container, tous démarrés par
une seule commande.

```powershell
Copy-Item .env.example .env    # ajuster les mots de passe si besoin
docker compose up -d --build
docker compose ps               # les 4 services doivent finir "healthy"
```

Le premier démarrage prend quelques minutes (build des images backend/frontend
+ téléchargement de l'image IBM MQ, ~2 Go). Les démarrages suivants sont
beaucoup plus rapides (couches Docker déjà en cache).

Une fois les 4 services `healthy` :

- **Application** : http://localhost:4200 — l'IHM Angular, servie par nginx,
  qui fait aussi office de reverse-proxy vers le backend sur `/api/*` (même
  origine que la page : pas de CORS à gérer dans ce mode).
- **Backend direct** : http://localhost:8080 (Swagger UI, Actuator, etc. —
  voir la liste plus bas)
- **IBM MQ Console** : https://localhost:9443/ibmmq/console/
  (login : `admin` / mot de passe défini dans `.env`)
- **PostgreSQL** : `localhost:5432`, DB `messaging`

Pour tout arrêter :

```powershell
docker compose down          # conserve les volumes
docker compose down -v       # supprime aussi les données PG + MQ
```

### Comment c'est dockerisé

- `backend/Dockerfile` : build multi-stage (`maven:3.9-eclipse-temurin-21` →
  `eclipse-temurin:21-jre-alpine`). Le `pom.xml` est copié et les dépendances
  résolues (`mvn dependency:go-offline`) *avant* de copier `src/` — les
  dépendances changent rarement, le code source à chaque commit ; ça garde
  la couche Docker des dépendances en cache d'un build à l'autre. Les tests ne
  sont pas rejoués pendant le build (`-DskipTests`) : ils sont déjà couverts
  par `mvn test`/`mvn verify -Pintegration-tests`, et les rejouer ici
  demanderait l'accès à Docker-in-Docker pour Testcontainers.
- `frontend/Dockerfile` : build multi-stage (`node:24-alpine` → `nginx:alpine`).
  `frontend/nginx.conf` sert les fichiers statiques et proxifie `/api/*` vers
  le service `backend` (résolu par le DNS interne de Docker Compose).
- Dans `docker-compose.yml`, le service `backend` utilise les noms de service
  (`postgres`, `ibmmq`) comme hostnames au lieu de `localhost`, et attend
  (`depends_on: condition: service_healthy`) que Postgres et MQ soient prêts
  avant de démarrer.

## Développement en local (sans rebuild Docker à chaque changement)

Pour itérer sur le code sans reconstruire les images à chaque modification,
backend et frontend peuvent tourner en local pendant que Postgres et MQ
restent en containers :

### 1. Lancer IBM MQ + PostgreSQL uniquement

```powershell
docker compose up -d postgres ibmmq
docker compose ps      # vérifier que ibmmq et postgres sont "healthy"
```

### 2. Compiler et lancer le backend

```powershell
cd backend
mvn spring-boot:run
```

Le backend écoute par défaut sur http://localhost:8080.

Endpoints utiles :
- **Health** : http://localhost:8080/actuator/health
- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** : http://localhost:8080/v3/api-docs
- **Metrics Prometheus** : http://localhost:8080/actuator/prometheus

### 3. Lancer le frontend

Voir [§ Frontend (Angular)](#frontend-angular) plus bas — `npm start` pointe
par défaut sur ce backend local (`http://localhost:8080`).

## Développement

### Tests

```powershell
cd backend
mvn install                          # ou mvn test / mvn verify : jamais de Docker par défaut
mvn verify -Pintegration-tests       # inclut les tests Testcontainers (PostgreSQL + IBM MQ)
```

Tous les tests basés sur Testcontainers (`MessageRepositoryAdapterIT`,
`MessageMqListenerIT`) sont marqués `@Tag("slow")`, nommés en `*IT`, et le
plugin Failsafe qui les exécute n'est déclaré que dans le profil Maven
`integration-tests` — il n'est pas bindé au cycle de vie par défaut. Résultat :
`mvn install` (qui traverse aussi les phases `integration-test`/`verify` du
cycle de vie standard) ne sollicite jamais Docker ; il faut explicitement
`-Pintegration-tests` pour les lancer.

> **Note Docker Engine 29.x / Docker Desktop récent** : Testcontainers 1.20.x
> (basé sur une ancienne version de l'API Docker) ne détecte pas correctement
> Docker Engine 29+ (`Could not find a valid Docker environment`). Le projet
> utilise donc Testcontainers **2.0.5+**, qui corrige cette négociation de
> version d'API. Si tu vois cette erreur avec un fork/clone plus ancien de ce
> repo, vérifie que `testcontainers.version` dans `pom.xml` est bien en 2.x —
> et que les artifacts ont changé de nom (`org.testcontainers:junit-jupiter` →
> `org.testcontainers:testcontainers-junit-jupiter`,
> `org.testcontainers:postgresql` → `org.testcontainers:testcontainers-postgresql`).

### Migrations de schéma

Les migrations Flyway vivent dans `backend/src/main/resources/db/migration/`.
Convention : `V{numero}__description.sql` (ex : `V1__create_message_table.sql`).
Elles sont exécutées automatiquement au démarrage de l'application.

### Backout queue IBM MQ

`mq-config/` contient une image dérivée de `icr.io/ibm-messaging/mq` qui
provisionne au *build* (pas au runtime, pour ne pas écraser le contenu de
`/etc/mqm` fourni par l'image) :
- la file `DEV.QUEUE.BACKOUT`
- `BOTHRESH(3)` et `BOQNAME(DEV.QUEUE.BACKOUT)` sur `DEV.QUEUE.1`

Un message qui fait échouer le listener (ex. base indisponible) n'est pas
acquitté (session JMS transactionnelle) : IBM MQ le represente jusqu'à 3 fois,
puis le route automatiquement vers `DEV.QUEUE.BACKOUT` — le consumer n'est
jamais bloqué. Vérifier la config après `docker compose up -d --build` :

```powershell
docker exec messaging-ibmmq bash -c "echo 'DISPLAY QLOCAL(DEV.QUEUE.1) BOTHRESH BOQNAME' | runmqsc QM1"
```

### Tester manuellement l'ingestion (sans Testcontainers)

```powershell
cd backend
mvn spring-boot:run
```

Dans un autre terminal, déposer un message avec l'utilitaire d'exemple IBM MQ :

```powershell
echo '<payment><amount>100</amount></payment>' | docker exec -i messaging-ibmmq /opt/mqm/samp/bin/amqsput DEV.QUEUE.1 QM1
```

Le message doit apparaître en base en quelques secondes :

```powershell
docker exec messaging-postgres psql -U messaging -d messaging -c "SELECT mq_message_id, source_queue, status, payload FROM message;"
```

### API REST

Toutes les routes sont sous `/api/v1/messages`. Filtres communs : `status`
(`RECEIVED`/`PROCESSED`/`ERROR`), `sourceQueue`, `from`/`to` (ISO-8601).

**Liste — pagination offset** :

```powershell
curl "http://localhost:8080/api/v1/messages?page=0&size=20"
curl "http://localhost:8080/api/v1/messages?status=PROCESSED&sourceQueue=DEV.QUEUE.1"
```

**Détail d'un message** :

```powershell
curl "http://localhost:8080/api/v1/messages/<id>"
```

**Statistiques** (compteurs par statut / par file source) :

```powershell
curl "http://localhost:8080/api/v1/messages/stats"
```

Les erreurs (404, 400 de validation) suivent un format uniforme :
`{ "timestamp", "status", "error", "message", "path" }`.

## Frontend (Angular)

```powershell
cd frontend
npm install
npm start              # ng serve, http://localhost:4200
```

L'IHM appelle le backend sur `http://localhost:8080` (voir
`src/environments/environment.ts`). Le backend doit tourner (§ Démarrage
rapide) et autoriser l'origine `http://localhost:4200` en CORS — déjà
configuré par défaut (`app.cors.allowed-origins` dans `application.yml`,
surchargeable via `APP_CORS_ALLOWED_ORIGINS`).

Vues :
- `/messages` — liste paginée (offset) et filtrable (statut, file source,
  intervalle de dates), indicateur de statut coloré
- `/messages/:id` — détail complet (headers JMS, payload, dates)

```powershell
npm test                          # Vitest
npm run build -- --configuration production
```

## Performance et résilience

### ID de corrélation

Chaque requête HTTP et chaque message MQ consommé porte un ID de corrélation dans
le MDC (`correlationId`), affiché entre crochets sur chaque ligne de log
(`logging.pattern.correlation` dans `application.yml`) :

- **HTTP** (`CorrelationIdFilter`) : réutilise le header `X-Correlation-Id`
  entrant s'il existe, sinon en génère un et le renvoie dans la réponse — un
  client (ou un futur reverse-proxy/gateway) peut donc imposer son propre ID de
  bout en bout.
- **MQ** (`MessageMqListener`) : réutilise directement le `JMSMessageID` du
  message — déjà unique, pas besoin d'en générer un autre.

Dans les deux cas, le MDC est nettoyé dans un `finally`. C'est indispensable :
les threads du serveur HTTP (Tomcat) comme ceux du conteneur JMS
(`DefaultMessageListenerContainer`) sont **réutilisés** entre requêtes/messages
— sans ce nettoyage, l'ID d'un message resterait collé aux logs du message
suivant traité par le même thread.

```powershell
curl -i http://localhost:8080/api/v1/messages   # -> header X-Correlation-Id en réponse
```

### Métriques (Micrometer / Prometheus)

`spring-boot-starter-actuator` fournit Micrometer *core*, mais pas l'exporteur
Prometheus : sans `io.micrometer:micrometer-registry-prometheus`,
`/actuator/prometheus` répond 500 malgré l'endpoint déclaré dans
`management.endpoints.web.exposure.include`. La dépendance a été ajoutée.

Au-delà des métriques JVM par défaut, `MessageIngestionService` expose un
compteur métier `messages.ingested{outcome="persisted"|"duplicate"}` — un taux
de doublons anormalement élevé est un signal direct de problème de
redélivrance côté MQ.

```powershell
curl http://localhost:8080/actuator/prometheus | grep messages_ingested
```

### Health checks

`management.endpoint.health.show-details` était sur `when-authorized`, un
réglage pensé pour les apps avec authentification (masquer le détail des
composants aux appelants anonymes). Cet exercice n'a explicitement **aucune
authentification** (CLAUDE.md §1) : personne n'est jamais "authorized", donc ce
réglage masquait silencieusement **tout le temps** l'état de la base et de MQ
— `/actuator/health` renvoyait juste `{"status":"UP"}` même si un composant
était en panne. Passé à `always`.

> En déploiement réel, on isolerait plutôt ces endpoints sur un port de
> gestion interne (`management.server.port`) séparé du port applicatif public,
> plutôt que de tout exposer en `always` sur le même port — non implémenté ici
> pour rester dans le périmètre de l'exercice.

```powershell
curl http://localhost:8080/actuator/health   # -> détail db / jms / diskSpace / ssl
```

### Dimensionnement des pools

- **HikariCP** (`spring.datasource.hikari`) : `maximum-pool-size=20` — la
  concurrence JMS max (`APP_MQ_CONCURRENCY=5-10`) occupe au plus 10 connexions
  pendant les pics d'ingestion, ce qui laisse ~10 connexions de marge pour les
  lectures REST concurrentes. `connection-timeout` (10 s) évite qu'un thread
  bloque indéfiniment si le pool est saturé ; `leak-detection-threshold`
  (30 s) logue un warning si une connexion reste empruntée anormalement
  longtemps sans être rendue — utile pour repérer une fuite de connexion.
- **Pool MQ** (`ibm.mq.pool.max-connections=10`) : aligné sur le même plafond
  de concurrence JMS (10), puisque `pooled-jms` alloue une connexion par
  thread consommateur actif.

## Feuille de route

État actuel : **Phase 5 — Qualité, documentation et dockerisation** ✅
(Phase 0 — Bootstrap ✅, Phase 1 — Consommation MQ + persistance ✅,
Phase 2 — API REST ✅, Phase 3 — IHM Angular ✅, Phase 4 — Performance et
résilience ✅)

Le périmètre de la Phase 5 a été volontairement réduit à la dockerisation
backend + frontend (voir sections précédentes) ; la mise en place d'une CI
GitHub Actions n'a pas été retenue pour cet exercice.
