# Blur 🌫️

A **full-stack social media platform** with real-time chat, voice/video calling, posts, stories, and notifications - built with modern microservices architecture.

![Status](https://img.shields.io/badge/Status-Production%20Ready-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## ✨ Features

| Category | Features |
|----------|----------|
| **Authentication** | OAuth2, JWT, Email verification, Password reset, Role-based access |
| **Real-time Chat** | 1-to-1 messaging, Typing indicators, Read receipts, Message reactions, File attachments |
| **Voice/Video Calls** | WebRTC P2P calls, Call history, Ringtone notifications |
| **Posts & Feed** | Create/edit/delete posts, Likes, Comments, Replies, Media uploads |
| **Stories** | 24h ephemeral stories, Like stories, Auto-progression |
| **Social** | Follow/Unfollow users, User profiles, Follower/Following lists |
| **Notifications** | Real-time alerts via WebSocket & Kafka for likes, comments, follows |
| **UI/UX** | Dark mode, Responsive design, Toast notifications, PWA support |

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| **Java 17+** | Core language |
| **Spring Boot 3.x** | Application framework |
| **Spring Cloud Gateway** | API Gateway & routing |
| **Spring Security + JWT** | Authentication & authorization |
| **Socket.IO (Netty)** | Real-time WebSocket communication |
| **Apache Kafka** | Event-driven messaging between services |
| **MongoDB** | Document database (Chat, Posts, Stories, Notifications) |
| **MySQL** | Relational database (Identity) |
| **Neo4j** | Graph database (Profiles & relationships) |
| **Redis** | Caching & session storage |
| **Cloudinary** | Media file storage |

### Frontend
| Technology | Purpose |
|------------|---------|
| **React 18** | UI framework |
| **TypeScript** | Type-safe JavaScript |
| **Vite** | Fast build tool |
| **Redux Toolkit** | State management |
| **React Router v7** | Routing |
| **Tailwind CSS** | Styling |
| **Chakra UI** | Component library |
| **Socket.IO Client** | Real-time communication |
| **WebRTC** | Peer-to-peer voice/video calls |
| **Axios** | HTTP client |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| **Docker & Docker Compose** | Containerization |
| **Nginx** | Reverse proxy & static serving |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         React Frontend (3000)                       │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       API Gateway (8888)                            │
└─────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────┐       ┌───────────────────┐       ┌───────────────┐
│   Identity    │       │   Chat Service    │       │    Profile    │
│   Service     │       │   8083 + 8099     │       │   Service     │
│    (8080)     │       │   (Socket.IO)     │       │    (8081)     │
│    MySQL      │       │     MongoDB       │       │     Neo4j     │
└───────────────┘       └───────────────────┘       └───────────────┘
        │                           │                           │
        │               ┌───────────┴───────────┐               │
        │               ▼                       ▼               │
        │   ┌───────────────────┐   ┌───────────────────┐       │
        │   │   Post Service    │   │  Story Service    │       │
        │   │     (8084)        │   │     (8086)        │       │
        │   │     MongoDB       │   │     MongoDB       │       │
        │   └───────────────────┘   └───────────────────┘       │
        │                                                       │
        └──────────────────┬────────────────────────────────────┘
                           ▼
              ┌─────────────────────┐
              │ Notification Service│◄───── Kafka Events
              │       (8082)        │
              │       MongoDB       │
              └─────────────────────┘

  ┌─────────┐  ┌─────────┐  ┌─────────┐
  │  Redis  │  │  Kafka  │  │ AI Svc  │
  │ (Cache) │  │ (Queue) │  │ (8087)  │
  └─────────┘  └─────────┘  └─────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose

### 1. Clone & Setup

```bash
git clone https://github.com/vansy204/blur.git
cd Blur
```

### 2. Start Infrastructure

```bash
cd Backend
docker-compose up -d   # MongoDB, Redis, Kafka
```

### 3. Run Backend Services

```bash
# Run each service (in separate terminals or use IDE)
cd Backend/IdentityService && mvn spring-boot:run
cd Backend/chat-service && mvn spring-boot:run
cd Backend/profile-service && mvn spring-boot:run
cd Backend/post-service && mvn spring-boot:run
cd Backend/story-service && mvn spring-boot:run
cd Backend/notification-service && mvn spring-boot:run
cd Backend/api-gateway && mvn spring-boot:run
```

### 4. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:3000**

---

## 📁 Project Structure

```
Blur/
├── Backend/
│   ├── api-gateway/          # Spring Cloud Gateway (8888)
│   ├── IdentityService/      # Auth & users (8080)
│   ├── profile-service/      # User profiles (8081)
│   ├── notification-service/ # Real-time alerts (8082)
│   ├── chat-service/         # Messaging & calls (8083/8099)
│   ├── post-service/         # Posts & comments (8084)
│   ├── story-service/        # Stories (8086)
│   ├── ai-service/           # AI features (8087)
│   ├── blur-common-lib/      # Shared library
│   └── docker/               # Docker configs
├── frontend/                 # React + Vite app
├── production/               # Production deployment
└── blur-deploy/              # Deployment scripts
```

---

## 🔧 Environment Variables

**Frontend** (`frontend/.env`):
```env
VITE_API_BASE=http://localhost:8888
VITE_SOCKET_URL=http://localhost:8099
```

**Backend**: Configure in each service's `application.yaml`

---

## 👨‍💻 Author

**Văn Sỹ (vansy204)**
- GitHub: [@vansy204](https://github.com/vansy204)

---

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.
