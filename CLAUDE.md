# CLAUDE.md — CACIB TBS Messaging

> À placer à la racine du repo Git avant de lancer `claude` en CLI. Claude Code charge
> automatiquement ce fichier comme contexte de projet à chaque session. La section
> **Feuille de route** en bas contient les prompts à donner phase par phase.

## 1. Contexte métier

Le département paiement de la banque reçoit des messages émis par les applications
Back Office via une file **IBM MQ Series**. Ces messages transitent ensuite par une
application de routage vers d'autres destinations.

**Objectif de l'exercice** : construire l'application de routage, à savoir une
application web qui :

1. Lit et stocke les messages déposés sur une file IBM MQ dans une base de données
   relationnelle.
2. Expose des API REST permettant de consulter ces messages via une IHM.

**Contrainte principale** : performance et résilience, compte tenu d'une volumétrie
de messages importante.

**Hors périmètre** : authentification et autorisations.

## 2. Environnement technique imposé

- Java 21+
- Spring Boot 4+
- Angular 22
- Base de données relationnelle (libre — PostgreSQL recommandé)
- Projet Maven
- Documentation permettant de tester l'application
- Docker (si possible)
- Code poussé sur GitHub

> ⚠️ **Versions récentes** : Spring Boot 4 (GA nov. 2025) et Angular 22 sont récents.
> Avant Phase 0, vérifier la disponibilité de versions compatibles pour :
> `ibm-mq-jms-spring-boot-starter` (fallback : `com.ibm.mq.allclient` + config
> manuelle du `ConnectionFactory` si besoin), `springdoc-openapi`, `Angular Material`.
> En cas de blocage, `Spring Boot 3.4+ / Java 21` reste une base valide et
> équivalente en pratique pour l'exercice.

## 3. Architecture cible

```
                 ┌───────────────┐
 Back Office ───▶│  IBM MQ Queue │
                 └───────┬───────┘
                         │ JMS listener (concurrent consumers)
                         ▼
              ┌─────────────────────┐
              │   Spring Boot app   │
              │ ─────────────────── │
              │  Consumer (in)      │──▶ persistance idempotente
              │  REST API (out)     │◀── lecture paginée / filtrée
              └─────────┬───────────┘
                         │ JDBC
                         ▼
                 ┌───────────────┐
                 │  PostgreSQL   │
                 └───────────────┘
                         ▲
                         │ HTTP/JSON
                 ┌───────────────┐
                 │  Angular UI   │
                 └───────────────┘
```

**Architecture interne recommandée : hexagonale / ports & adapters**, dans un seul
module Spring Boot (le multi-module Maven n'apporte pas de valeur pour un exercice
de cette taille et coûte du temps de setup — à ne considérer qu'en bonus si le temps
le permet).

**Organisation du repo** : mono-repo à deux répertoires racine.

```
messaging-cacib/
├── backend/           # projet Maven Spring Boot
├── frontend/          # application Angular 22
├── docker-compose.yml
└── README.md
```

**Structure des packages backend** :

```
com.cacib.messaging
├── domain
│   ├── model          # Message, MessageStatus (POJO, aucune dépendance framework)
│   └── port
│       ├── in         # driving ports (use cases) : IngestMessageUseCase, QueryMessagesUseCase
│       └── out        # driven ports : MessageRepositoryPort
├── application
│   └── service        # implémentation des use cases : MessageIngestionService, MessageQueryService
├── infrastructure
│   ├── mq             # config connexion IBM MQ + listener JMS (adapter "in")
│   ├── persistence    # entités JPA, Spring Data repo, adapter du port repository (adapter "out")
│   └── web            # controllers REST, DTO, mapper, gestion d'erreurs (adapter "in")
└── config             # beans Spring, OpenAPI, configuration MQ/DB
```

Le domaine ne dépend d'aucun détail d'infrastructure : le listener MQ et le
controller REST sont deux adapters interchangeables autour du même cœur métier.
Cela permet de tester `application.service` sans MQ ni base réelle.

## 4. Stack technique détaillée

**Backend**
- Spring Boot 4 (Web, Data JPA, Validation, Actuator)
- Spring JMS + client IBM MQ (`com.ibm.mq.allclient` / `ibm-mq-jms-spring-boot-starter`
  — vérifier compatibilité SB4, sinon config manuelle du `ConnectionFactory`)
- PostgreSQL + Flyway pour les migrations de schéma
- MapStruct pour le mapping entité ↔ DTO. **Si Lombok retenu**, l'ordre des
  annotation processors dans le `pom.xml` est critique (lombok-mapstruct-binding).
  Alternative plus simple : Java 21 records + MapStruct seul, pas de Lombok.
- springdoc-openapi pour la doc API (Swagger UI)
- Resilience4j (retry/circuit breaker) si des appels sortants supplémentaires existent
- Lombok (optionnel, à utiliser avec parcimonie)

**Frontend**
- Angular 22, standalone components, signals pour l'état
- Angular Material pour l'IHM (table paginée, filtres, détail message) — vérifier
  compatibilité version 22
- Client HTTP typé (généré depuis le contrat OpenAPI ou écrit à la main)

**Tests**
- JUnit 5 + Mockito pour les tests unitaires
- Testcontainers (PostgreSQL + IBM MQ) pour les tests d'intégration du listener et
  du repository
- MockMvc / WebTestClient pour les tests de contrat REST
- Vitest ou Jest pour les tests Angular (Karma étant déprécié, Angular CLI 22
  privilégie Vitest par défaut)

**Infra locale**
- `docker-compose.yml` : IBM MQ (`icr.io/ibm-messaging/mq`), PostgreSQL, backend,
  frontend
  - **IBM MQ** : variable `LICENSE=accept` obligatoire, `MQ_QMGR_NAME`,
    `MQ_APP_PASSWORD`. Image lourde (~2 Go, démarrage 30-60s).
  - **Ressources Docker Desktop** : allouer ≥ 4 Go RAM (sous Windows, ajuster
    dans les settings Docker).
  - **Secrets** : credentials MQ et DB via `.env` + `env_file` dans docker-compose,
    jamais en dur dans `application.yml`.
- **CORS** : configurer le backend pour autoriser l'origine du front en dev
  (`localhost:4200`).
- **Tests lents** : les tests Testcontainers IBM MQ sont longs (démarrage image).
  Les isoler via `@Tag("slow")` ou un profil Maven dédié pour ne pas ralentir le
  build rapide.
- GitHub Actions : build + tests backend et frontend à chaque push

## 5. Modèle de données (proposition)

Table `message` :

| Colonne | Type | Notes |
|---|---|---|
| id | UUID / bigserial | PK |
| mq_message_id | varchar, **unique** | `JMSMessageID`, clé de dédoublonnage |
| correlation_id | varchar, indexé | `JMSCorrelationID` |
| source_queue | varchar, indexé | nom de la file d'origine |
| source_application | varchar, indexé | si disponible dans les headers |
| status | varchar + CHECK constraint (RECEIVED, PROCESSED, ERROR) | indexé — préférer VARCHAR + CHECK à un enum PG natif pour simplifier les migrations Flyway |
| headers | jsonb | headers JMS bruts |
| payload | text | corps du message |
| received_at | timestamp, indexé | date de dépôt en base |
| processed_at | timestamp, nullable | |

L'unicité sur `mq_message_id` garantit l'idempotence en cas de redélivrance MQ.

## 6. API REST (proposition)

- `GET /api/v1/messages` — liste paginée (offset) et filtrable :
    `?page=0&size=50&status=&sourceQueue=&from=&to=`
- `GET /api/v1/messages/{id}` — détail d'un message
- `GET /api/v1/messages/stats` — compteurs par statut / par file, pour un dashboard
- Réponses au format JSON standard, erreurs via un format uniforme
  (`@RestControllerAdvice`, code, message, timestamp)
- Documentation exposée via Swagger UI (`/swagger-ui.html`)

## 7. Performance et résilience — points d'attention

- **Consumers JMS concurrents** configurables (`concurrency=5-10`) pour absorber la
  volumétrie
- **Idempotence** : contrainte unique + `INSERT ... ON CONFLICT DO NOTHING` (ou
  équivalent JPA) pour ignorer les redélivrances sans erreur
- **Gestion des erreurs MQ** : retry borné puis routage vers une backout queue / DLQ
  IBM MQ plutôt que de bloquer le consumer. Nommer explicitement :
  - File applicative : `DEV.QUEUE.1` (config par défaut de l'image dev IBM MQ)
  - Backout queue : `DEV.QUEUE.BACKOUT`
  - Seuil de rejeu (`BOTHRESH`) : `3` (au-delà, message routé vers backout)
- **Session transactionnelle** JMS pour garantir qu'un message n'est acquitté qu'une
  fois persisté
- **Pool de connexions DB** (HikariCP) dimensionné et pool de connexions MQ
- **Indexation** des colonnes utilisées dans les filtres API (`status`,
  `received_at`, `source_queue`) — pagination offset, l'indexation suffit au
  volume visé par l'exercice
- **Observabilité** : Spring Actuator (health, metrics), logs structurés avec un id
  de corrélation, métriques Micrometer exposées pour supervision

## 8. Bonnes pratiques de développement

- Séparation stricte domaine / infrastructure (voir architecture §3)
- DTO en frontière d'API, jamais d'entité JPA exposée directement
- Validation des entrées (`jakarta.validation`) et gestion d'erreurs centralisée
- Tests à chaque couche : unitaires sur le domaine/service, intégration sur
  listener + repository (Testcontainers), contrat sur les controllers
- Migrations de schéma versionnées (Flyway), jamais de `ddl-auto=update` en usage
  réel
- Commits atomiques et messages clairs (Conventional Commits recommandé :
  `feat:`, `fix:`, `test:`, `docs:`)
- README avec instructions de lancement (`docker-compose up`), jeu de données de
  démo, et exemples d'appels API (curl ou collection Postman) pour répondre à
  l'exigence « documentation pour tester l'application »

## 9. Feuille de route et prompts Claude Code

À dérouler phase par phase, en validant chaque étape avant de passer à la suivante.

**Phase 0 — Bootstrap**
> Crée la structure mono-repo `backend/` + `frontend/` décrite au §3. Avant de
> figer les versions, vérifie la disponibilité et la compatibilité de Spring
> Boot 4, `ibm-mq-jms-spring-boot-starter`, `springdoc-openapi` et Angular 22 /
> Angular Material 22 (fallback : SB 3.4+ si SB4 pose problème). Initialise
> ensuite le projet Maven Spring Boot / Java 21 dans `backend/` avec la structure
> de packages (domain avec port/in et port/out / application / infrastructure /
> config). Ajoute un docker-compose.yml à la racine avec IBM MQ (image
> icr.io/ibm-messaging/mq, `LICENSE=accept`, credentials via `.env`) et
> PostgreSQL. Crée un README avec les instructions de lancement et les
> pré-requis (Docker Desktop ≥ 4 Go RAM sous Windows).

**Phase 1 — Consommation MQ + persistance**
> Implémente le listener JMS qui consomme la file configurée, mappe chaque message
> IBM MQ vers le modèle de domaine Message (voir §5 de CLAUDE.md), et le persiste
> de façon idempotente via mq_message_id. Ajoute la gestion des erreurs avec retry
> borné et routage vers une backout queue après échec. Écris des tests
> d'intégration avec Testcontainers (IBM MQ + PostgreSQL).

**Phase 2 — API REST**
> Expose les endpoints REST définis au §6 de CLAUDE.md, en couches
> controller/service/repository via les ports du domaine, avec DTO + MapStruct,
> validation des paramètres, gestion centralisée des erreurs, et documentation
> OpenAPI (springdoc). Ajoute des tests MockMvc couvrant les cas nominaux et
> d'erreur.

**Phase 3 — IHM Angular**
> Crée une application Angular 22 en standalone components avec Angular Material :
> une vue liste paginée/filtrable des messages consommant l'API REST, une vue
> détail, et un indicateur de statut. Utilise les signals pour l'état et un
> service HTTP typé aligné sur le contrat OpenAPI.

**Phase 4 — Performance et résilience**
> Ajoute le tuning décrit au §7 : consumers concurrents configurables, pool
> HikariCP dimensionné, index DB sur les colonnes de filtre, health checks
> Actuator, métriques Micrometer. Documente les choix dans le README.

**Phase 5 — Qualité, documentation et CI**
> Ajoute un pipeline GitHub Actions (build + tests backend et frontend), un
> Dockerfile multi-stage pour le backend et le frontend, complète le README avec
> les instructions de test (collection Postman ou exemples curl, jeu de données
> de démo), et vérifie la couverture de tests avant de pousser sur GitHub.
