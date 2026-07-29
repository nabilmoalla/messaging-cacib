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

## Feuille de route

Phase 0 — Bootstrap ✅
