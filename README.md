# Blur — Social Networking Platform

Blur is a modern, full-stack social networking platform built with a microservices architecture. It supports real-time messaging, content creation, social relationships, AI-powered features, and automated toxic comment detection.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript 5, Vite 5, Redux Toolkit, Chakra UI, Tailwind CSS, React Router v7, Axios, Formik, Framer Motion, WebRTC, Cloudinary |
| Backend | Java 21, Spring Boot 3.4.3, Spring Cloud 2024.0.0, Spring Security, OpenFeign, MapStruct, Lombok, Maven |
| API Gateway | Spring Cloud Gateway |
| Graph DB | Neo4j (social graph, profiles, posts, relationships) |
| Document DB | MongoDB 6.0 (chat, notifications, AI conversations) |
| Cache | Caffeine (L1 in-memory) + Redis (L2 distributed), Redisson (distributed lock), Redis Lua Scripts |
| Messaging | Apache Kafka 7.7.1 (KRaft mode) |
| Real-time | WebSocket (Socket.IO + STOMP), WebRTC (video/audio calls) |
| ML/AI | Python 3.11, FastAPI, PyTorch, PhoBERT (Vietnamese NLP), Google Gemini API |
| Data Scraping | yt-dlp, TikTok API, Selenium, Playwright |
| Infrastructure | Docker (multi-stage), Docker Compose, Nginx, Amazon Corretto 21 |

---

## Distributed System Patterns

| Pattern | Description |
|---------|-------------|
| Multi-Level Cache | Caffeine (L1) + Redis (L2) with per-entity TTL |
| Distributed Lock | Redisson RLock, double-check pattern (cache stampede prevention) |
| Cache Invalidation | Redis Pub/Sub → L1 eviction across instances |
| Cache Warming | Hot data preloaded on startup |
| Outbox Pattern | Reliable event publishing with scheduled outbox + cleanup |
| Saga Pattern | Multi-service distributed transaction orchestration via Kafka |
| CQRS | Event-driven feed projection (separate read/write models) |
| Atomic Counters | Redis Lua scripts for lock-free operations |

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

| Service | Port | Description |
|---------|------|-------------|
| **user-service** | 8081 | Auth, profiles, OAuth2, friend recommendations (Neo4j Cypher) |
| **content-service** | 8082 | Posts, comments, stories, CQRS feed, multi-level cache, Outbox pattern |
| **communication-service** | 8083 | Chat (Socket.IO), notifications (STOMP), WebRTC calls, Gemini AI chat, email fallback |
| **api-gateway** | 8888 | Routing, CORS, authentication orchestration |
| **model-service** | 8000 | PhoBERT toxic detection, async Kafka pipeline, data scraping (YouTube, TikTok) |

---

## Project Structure

```
Blur/
├── Backend/
│   ├── user-service/           # Auth, profiles, OAuth2, recommendations
│   ├── content-service/        # Posts, comments, stories, CQRS feed
│   ├── communication-service/  # Chat, notifications, calls, AI
│   ├── api-gateway/            # Request routing
│   ├── model-service/          # Python FastAPI ML pipeline
│   ├── docker/                 # Infrastructure setup, deploy scripts
│   └── docker-compose.yml      # Local dev infrastructure
├── frontend/                   # React TypeScript SPA
├── blur-deploy/                # Production deployment
└── production/                 # Production Docker Compose
```

---

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21, Node.js 18+, Python 3.11 (for local dev)

### Full Stack (Docker)

```bash
cd blur-deploy && docker compose up -d
```

### Local Development

```bash
# Infrastructure
docker compose -f Backend/docker-compose.yml up -d

# Backend services
cd Backend/user-service && ./mvnw spring-boot:run
cd Backend/content-service && ./mvnw spring-boot:run
cd Backend/communication-service && ./mvnw spring-boot:run
cd Backend/api-gateway && ./mvnw spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

---

## API Overview

All requests go through `http://localhost:8888/api`.

| Prefix | Service | Description |
|--------|---------|-------------|
| `/auth/**` | user-service | Login, token |
| `/users/**` | user-service | Account management |
| `/profile/**` | user-service | Profile CRUD |
| `/post/**` | content-service | Posts, likes, shares |
| `/post/comment/**` | content-service | Comments |
| `/stories/**` | content-service | Stories |
| `/chat/**` | communication-service | Messaging |
| `/notification/**` | communication-service | Notifications |

---

## Key Features

- **Authentication** — JWT + Google OAuth2 + Spring Security
- **Social Graph** — Neo4j-based follow/recommend (mutual, taste, city, popular)
- **Real-time Chat** — Socket.IO + STOMP notifications
- **Video/Audio Calls** — WebRTC peer-to-peer
- **AI Chat** — Gemini-powered smart replies
- **Content Moderation** — PhoBERT Vietnamese toxic detection via Kafka pipeline
- **Feed** — CQRS event-driven projections
- **Multi-Level Cache** — Caffeine + Redis + distributed lock + Pub/Sub invalidation + warming
- **Distributed Transactions** — Outbox + Saga patterns
- **Media Upload** — Cloudinary integration

---

## Authors

**NeuroDev204**
