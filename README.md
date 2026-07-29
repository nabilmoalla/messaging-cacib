# messaging-cacib

Application de routage IBM MQ → PostgreSQL avec IHM de consultation.

## Stack

- **Backend** : Java 21, Spring Boot 4.0.x, Spring Data JPA
- **Frontend** : Angular 22, standalone components, signals, Angular Material 22
- **Base de données** : PostgreSQL 16
- **Broker** : IBM MQ 9.4 (image `icr.io/ibm-messaging/mq`, licence développeur)

## Structure du repo

```
messaging-cacib/
├── backend/            projet Maven Spring Boot (architecture hexagonale)
├── frontend/           application Angular 22 (liste/détail des messages)
├── docker-compose.yml  IBM MQ + PostgreSQL
└── .env.example        modèle de configuration (copier en .env)
```

## Pré-requis

- Docker Desktop avec **≥ 4 Go RAM alloués** (l'image IBM MQ est lourde, ~2 Go,
  démarrage 30-60 s).
- Pour développer : **Java 21+**, **Maven 3.9+**, **Node.js 20+** et
  **Angular CLI 22**.

## Démarrage

```powershell
Copy-Item .env.example .env    # ajuster les mots de passe si besoin
docker compose up -d
docker compose ps               # postgres et ibmmq doivent finir "healthy"
```

## Développement

### Backend

```powershell
cd backend
mvn spring-boot:run
```

Le backend écoute par défaut sur http://localhost:8080.

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
jamais bloqué.

### Tester manuellement l'ingestion

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

Documentation interactive : Swagger UI sur http://localhost:8080/swagger-ui.html.

## Frontend (Angular)

```powershell
cd frontend
npm install
npm start              # ng serve, http://localhost:4200
```

L'IHM appelle le backend sur `http://localhost:8080` (voir
`src/environments/environment.ts`). Le backend doit tourner et autoriser
l'origine `http://localhost:4200` en CORS — déjà configuré par défaut
(`app.cors.allowed-origins` dans `application.yml`, surchargeable via
`APP_CORS_ALLOWED_ORIGINS`).

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

Phase 0 — Bootstrap ✅
Phase 1 — Consommation MQ + persistance ✅
Phase 2 — API REST ✅
Phase 3 — IHM Angular ✅
Phase 4 — Performance et résilience ✅
