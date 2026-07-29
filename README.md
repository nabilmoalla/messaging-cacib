# messaging-cacib

Application de routage IBM MQ → PostgreSQL avec IHM de consultation.

## Stack

- **Backend** : Java 21, Spring Boot 4.0.x, Spring Data JPA
- **Base de données** : PostgreSQL 16
- **Broker** : IBM MQ 9.4 (image `icr.io/ibm-messaging/mq`, licence développeur)

## Structure du repo

```
messaging-cacib/
├── backend/            projet Maven Spring Boot (architecture hexagonale)
├── docker-compose.yml  IBM MQ + PostgreSQL
└── .env.example        modèle de configuration (copier en .env)
```

## Pré-requis

- Docker Desktop avec **≥ 4 Go RAM alloués** (l'image IBM MQ est lourde, ~2 Go,
  démarrage 30-60 s).
- Pour développer : **Java 21+** et **Maven 3.9+**.

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

## Feuille de route

Phase 0 — Bootstrap ✅
Phase 1 — Consommation MQ + persistance ✅
