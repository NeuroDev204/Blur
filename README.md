# Blur — Social Networking Platform

Nền tảng mạng xã hội full-stack theo kiến trúc microservices: realtime chat, video/audio call, feed CQRS, AI chat, và kiểm duyệt bình luận tiếng Việt bằng PhoBERT.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript 5, Vite 5, Redux Toolkit, Chakra UI, Tailwind CSS, React Router v7, Axios, Formik + Yup, Framer Motion, Socket.IO Client, STOMP.js + SockJS, WebRTC, Swiper, Lucide React, Emoji Picker, react-grid-layout, react-hot-toast, jwt-decode |
| Backend | Java 21, Spring Boot 3.4.3, Spring Cloud 2024.0.0, Spring Security OAuth2 Resource Server, OpenFeign, Resilience4j, MapStruct, Lombok, Maven |
| Auth (IDP) | **Keycloak 26.1** (JWT issuer, realm import, MySQL-backed) |
| API Gateway | Spring Cloud Gateway (reactive) + Resilience4j CircuitBreaker |
| Graph DB | Neo4j (social graph, profiles, posts, relationships) |
| Document DB | MongoDB 6.0 (chat, notifications, AI conversations) |
| Relational DB | MySQL 8.0 (Keycloak persistence) |
| Cache | Caffeine (L1) + Redis (L2), Redisson (distributed lock), Redis Pub/Sub, Redis Lua scripts |
| Messaging | Apache Kafka 7.7.1 (KRaft mode, no ZooKeeper) |
| Real-time | WebSocket — Socket.IO (chat) + STOMP/SockJS (notifications), WebRTC (video/audio) |
| ML / AI | Python 3.11, FastAPI, PyTorch, **PhoBERT v2** (merged-dataset training), **ONNX Runtime** (5–10× faster cold start), aiokafka, Google Gemini API |
| Data Collection | yt-dlp, YouTube Data API, TikTokApi, Playwright, pandas |
| Infrastructure | Docker (multi-stage), Docker Compose, Nginx, Amazon Corretto 21 |

---

## Distributed System Patterns

| Pattern | Description |
|---------|-------------|
| Multi-Level Cache | Caffeine (L1) + Redis (L2), per-entity TTL |
| Distributed Lock | Redisson RLock + double-check (chống cache stampede) |
| Cache Invalidation | Redis Pub/Sub → evict L1 cross-instance |
| Cache Warming | Hot data preload khi startup |
| Outbox Pattern | Reliable event publishing (scheduler + cleanup) |
| Saga | Multi-service distributed transaction qua Kafka |
| CQRS | Event-driven feed projection (read/write tách biệt) |
| Circuit Breaker | Resilience4j (Gateway + inter-service Feign) |
| Atomic Counters | Redis Lua cho lock-free counter |
| Async ML Pipeline | Kafka → FastAPI → Kafka (non-blocking moderation) |

---

## Architecture

```
┌─────────────────────────────────────────┐
│        Frontend (React / Nginx)         │
└──────────────────┬──────────────────────┘
                   │
       ┌───────────▼───────────┐       ┌──────────────────┐
       │   API Gateway (8888)  │──────▶│  Keycloak (8080) │
       │  Spring Cloud Gateway │  JWT  │   MySQL-backed   │
       └───────────┬───────────┘       └──────────────────┘
                   │
      ┌────────────┼────────────┬──────────────┐
      ▼            ▼            ▼              ▼
 ┌─────────┐ ┌──────────┐ ┌──────────┐  ┌──────────┐
 │  User   │ │ Content  │ │  Comm.   │  │  Model   │
 │ Service │ │ Service  │ │ Service  │  │ Service  │
 │  8081   │ │   8082   │ │   8083   │  │  8000    │
 └────┬────┘ └────┬─────┘ └────┬─────┘  └────┬─────┘
      │           │             │             │
      └───────────┼─────────────┼─────────────┘
                  │             │
     ┌──────┬─────┼──────┬──────┼──────┬──────────┐
     ▼      ▼     ▼      ▼      ▼      ▼          ▼
  ┌─────┐ ┌────┐ ┌───┐ ┌─────┐ ┌────┐ ┌─────┐ ┌────────┐
  │Neo4j│ │Mongo│ │SQL│ │Redis│ │Kafka│ │Gemini│ │PhoBERT│
  └─────┘ └────┘ └───┘ └─────┘ └────┘ └─────┘ └────────┘
```

---

## Services

| Service | Port | Description |
|---------|------|-------------|
| **keycloak** | 8080 | Identity Provider, JWT issuer, realm `blur-realm` |
| **api-gateway** | 8888 | Reactive routing, CORS, JWT resource-server validation, CircuitBreaker |
| **user-service** | 8081 | Profile, social graph (Neo4j Cypher), friend recommendations, Keycloak admin client |
| **content-service** | 8082 | Posts, comments, stories, CQRS feed, multi-level cache, Outbox, Saga |
| **communication-service** | 8083 | Socket.IO chat, STOMP notifications, WebRTC signaling, Gemini AI chat, email fallback |
| **model-service** | 8000 | FastAPI, PhoBERT v2 / ONNX, Kafka consumer+producer, async moderation pipeline, Prometheus metrics |

---

## Project Structure

```
Blur/
├── Backend/
│   ├── user-service/              # Profile + social graph (Neo4j)
│   ├── content-service/           # Posts, stories, CQRS feed, Outbox
│   ├── communication-service/     # Chat, notifications, calls, AI
│   ├── api-gateway/               # Reactive gateway + JWT
│   ├── model-service/             # FastAPI + PhoBERT/ONNX + Kafka
│   │   ├── app/                   # predictor, predictor_onnx, kafka, services
│   │   ├── training/              # train_model_v2.py (merged-dataset)
│   │   └── scraper/               # yt-dlp, TikTok, Playwright
│   ├── keycloak/blur-realm.json   # Realm config (import on startup)
│   ├── docker/                    # Infra scripts
│   └── docker-compose.yml         # Local dev infra
├── frontend/                      # React + Vite SPA
├── blur-deploy/                   # Full-stack compose (deploy)
└── production/                    # Production compose + Nginx
```

---

## Getting Started

### Prerequisites
Docker & Docker Compose · Java 21 · Node.js 18+ · Python 3.11

### Full Stack (Docker)
```bash
cd blur-deploy && docker compose up -d
```

### Local Development
```bash
# 1. Infrastructure (Neo4j, Mongo, MySQL, Redis, Kafka, Keycloak)
docker compose -f Backend/docker-compose.yml up -d

# 2. Backend services
cd Backend/user-service          && ./mvnw spring-boot:run
cd Backend/content-service       && ./mvnw spring-boot:run
cd Backend/communication-service && ./mvnw spring-boot:run
cd Backend/api-gateway           && ./mvnw spring-boot:run

# 3. Model service (Python)
cd Backend/model-service
python -m venv venv && source venv/Scripts/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000

# 4. Frontend
cd frontend && npm install && npm run dev
```

### Keycloak
- Admin Console: <http://localhost:8080> · user `admin` / pass `admin`
- Realm `blur-realm` tự import từ `Backend/keycloak/blur-realm.json`

---

## API Overview

Tất cả request đi qua Gateway `http://localhost:8888/api`.

| Prefix | Service | Description |
|--------|---------|-------------|
| `/auth/**` | user-service | Login / token (proxy Keycloak) |
| `/users/**` | user-service | Account |
| `/profile/**` | user-service | Profile CRUD + recommendations |
| `/post/**` | content-service | Posts, likes, shares |
| `/post/comment/**` | content-service | Comments |
| `/stories/**` | content-service | Stories |
| `/chat/**` | communication-service | Messaging |
| `/notification/**` | communication-service | Notifications |
| `/ai/**` | communication-service | Gemini AI chat |

---

## Key Features

- **Authentication** — Keycloak 26.1 (OIDC/JWT) + Spring OAuth2 Resource Server
- **Social Graph** — Neo4j Cypher: follow/recommend (mutual, taste, city, popular)
- **Realtime Chat** — Socket.IO + STOMP notifications
- **Video/Audio Calls** — WebRTC peer-to-peer
- **AI Chat** — Google Gemini với context-aware replies
- **Toxic Detection** — PhoBERT v2 (Vietnamese), ONNX backend tùy chọn, async Kafka pipeline
- **Feed** — CQRS event-driven projections
- **Multi-Level Cache** — Caffeine + Redis + Redisson lock + Pub/Sub invalidation + warming
- **Distributed Transactions** — Outbox + Saga
- **Resilience** — Resilience4j CircuitBreaker trên Gateway & Feign clients
- **Media Upload** — Cloudinary

---

## Author

**NeuroDev204**
