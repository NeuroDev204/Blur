# Blur — Social Networking Platform

Blur is a modern, full-stack social networking platform built with a microservices architecture. It supports real-time messaging, content creation, social relationships, AI-powered features, and automated toxic comment detection.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript, Vite, Chakra UI, Tailwind CSS, Redux |
| Backend | Java 21, Spring Boot 3.x, Spring Cloud Gateway, Maven |
| Graph DB | Neo4j (users, profiles, posts, comments, stories, relationships) |
| Document DB | MongoDB (chat, notifications) |
| Cache | Redis |
| Messaging | Apache Kafka |
| Real-time | WebSocket (Socket.IO + STOMP) |
| ML/AI | Python, PyTorch, PhoBERT (Vietnamese NLP), Google Gemini API |
| Containers | Docker, Docker Compose |

---

## Architecture

```
┌─────────────────────────────────────────┐
│           Frontend (React / Nginx)       │
│                Port 80                  │
└──────────────────┬──────────────────────┘
                   │
       ┌───────────▼───────────┐
       │   API Gateway (8888)  │
       │  Spring Cloud Gateway │
       └───────────┬───────────┘
                   │
      ┌────────────┼────────────┬──────────┐
      ▼            ▼            ▼          ▼
 ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐
 │  User   │ │ Content  │ │  Comm.   │ │ Model  │
 │ Service │ │ Service  │ │ Service  │ │Service │
 │  8081   │ │  8082    │ │  8083    │ │(Python)│
 └────┬────┘ └────┬─────┘ └────┬─────┘ └────────┘
      │           │             │
      └───────────┼─────────────┘
                  │
     ┌────────────┼────────────┬──────────┐
     ▼            ▼            ▼          ▼
  ┌─────┐    ┌─────────┐  ┌────────┐ ┌────────┐
  │Neo4j│    │ MongoDB │  │ Redis  │ │ Kafka  │
  └─────┘    └─────────┘  └────────┘ └────────┘
```

---

## Services

### user-service (port 8081)
Handles user authentication, profile management, and identity. Supports Google OAuth2 login. Stores all user and relationship data in Neo4j.

### content-service (port 8082)
Manages posts, comments, stories, likes, and shares. Uses Neo4j for social graph queries and Kafka for async event publishing.

### communication-service (port 8083)
Provides real-time chat (Socket.IO), push notifications (STOMP), email fallback, and AI-powered chat responses via the Gemini API. Backed by MongoDB.

### api-gateway (port 8888)
Central entry point for all client traffic. Handles routing, CORS, and authentication orchestration via Spring Cloud Gateway.

### model-service (Python)
ML pipeline for Vietnamese toxic comment detection using PhoBERT. Scrapes YouTube and TikTok for training data, trains and serves a toxicity classification model.

---

## Project Structure

```
Blur/
├── Backend/
│   ├── user-service/           # Auth, profiles, OAuth2
│   ├── content-service/        # Posts, comments, stories
│   ├── communication-service/  # Chat, notifications, AI
│   ├── api-gateway/            # Request routing
│   ├── model-service/          # Python ML pipeline
│   ├── docker/                 # Infrastructure setup
│   ├── docs/                   # Architecture guides
│   └── docker-compose.yml      # Local dev infrastructure
├── frontend/                   # React TypeScript SPA
├── blur-deploy/                # Production deployment
└── production/                 # Production Docker Compose
```

---

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local service development)
- Node.js 18+ (for frontend development)

### Option 1 — Full Stack (Docker)

```bash
cd blur-deploy
docker compose up -d
```

- Frontend: http://localhost
- Neo4j Browser: http://localhost:7474 (user: `neo4j`, password: `12345678`)

### Option 2 — Local Development

**Start infrastructure:**
```bash
docker compose -f Backend/docker-compose.yml up -d
```

**Run each service:**
```bash
cd Backend/user-service && ./mvnw spring-boot:run
cd Backend/content-service && ./mvnw spring-boot:run
cd Backend/communication-service && ./mvnw spring-boot:run
cd Backend/api-gateway && ./mvnw spring-boot:run
```

**Run the frontend:**
```bash
cd frontend
npm install
npm run dev
```

---

## API Overview

All requests go through the API Gateway at `http://localhost:8888/api`.

| Prefix | Service | Description |
|--------|---------|-------------|
| `/auth/**` | user-service | Login, token generation/introspection |
| `/users/**` | user-service | Account management |
| `/profile/**` | user-service | User profile CRUD |
| `/post/**` | content-service | Posts, likes, shares |
| `/post/comment/**` | content-service | Comments |
| `/stories/**` | content-service | Stories |
| `/chat/**` | communication-service | Real-time messaging |
| `/notification/**` | communication-service | Notifications |

---

## Key Features

- **Authentication** — JWT-based auth with Google OAuth2 support
- **Social Graph** — Follow/unfollow, friend recommendations via Neo4j graph queries
- **Content** — Create and interact with posts, comments, and stories
- **Real-time Chat** — Socket.IO messaging with STOMP notifications
- **AI Chat Assistance** — Gemini-powered smart replies in conversations
- **Content Moderation** — Automated Vietnamese toxic comment detection (PhoBERT)
- **Feed** — CQRS-based personalized feed with Redis caching

---

## Authors

**NeuroDev204**
