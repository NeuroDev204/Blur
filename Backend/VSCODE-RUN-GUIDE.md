# 🚀 Hướng Dẫn Chạy Microservices với VS Code

## 📋 Tổng Quan Hệ Thống

| Service | Port | Main Class | Database |
|---------|------|-----------|----------|
| `api-gateway` | 8080 | `com.blur.apigateway.ApiGatewayApplication` | - |
| `IdentityService` | 8081 | `org.identityservice.IdentityServiceApplication` | MySQL + Redis |
| `profile-service` | 8082 | `com.blur.profileservice.ProfileServiceApplication` | Neo4j + Redis |
| `post-service` | 8083 | `com.postservice.PostServiceApplication` | MongoDB + Redis |
| `chat-service` | 8084 | `com.blur.chatservice.ChatServiceApplication` | MongoDB + Redis |
| `notification-service` | 8085 | `com.blur.notificationservice.NotificationServiceApplication` | MongoDB + Redis |
| `story-service` | 8086 | `com.example.storyservice.StoryServiceApplication` | MongoDB + Redis |

---

## 🔧 Yêu Cầu Cài Đặt

### 1. VS Code Extensions (Bắt buộc)

Cài đặt các extensions sau trong VS Code:

```
- Extension Pack for Java (vscjava.vscode-java-pack)
- Spring Boot Extension Pack (vmware.vscode-boot-dev-pack)
- Debugger for Java (vscjava.vscode-java-debug)
```

**Cách cài đặt nhanh:** Nhấn `Ctrl+Shift+X`, tìm và cài "Extension Pack for Java"

### 2. Java JDK 21

Đảm bảo đã cài JDK 21:
```bash
java --version
# Output: openjdk 21.x.x
```

### 3. Maven

```bash
mvn --version
# Output: Apache Maven 3.x.x
```

---

## 📁 Cấu Trúc File .env

### File `.env.shared` (Biến môi trường chung)

Đặt tại `/Backend/.env.shared` - chứa các biến dùng chung cho tất cả service:

```env
# Database Common
MYSQL_HOST=localhost
MYSQL_PORT=3306
MONGODB_HOST=localhost
MONGODB_PORT=27017
REDIS_HOST=localhost
REDIS_PORT=6379
NEO4J_HOST=localhost
NEO4J_PORT=7687

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT
JWT_SECRET=your-super-secret-key-here
JWT_ISSUER=blur-identity-service
JWT_EXPIRATION=86400000

# Service URLs (cho Feign Client)
IDENTITY_SERVICE_URL=http://localhost:8081
PROFILE_SERVICE_URL=http://localhost:8082
POST_SERVICE_URL=http://localhost:8083
CHAT_SERVICE_URL=http://localhost:8084
NOTIFICATION_SERVICE_URL=http://localhost:8085
STORY_SERVICE_URL=http://localhost:8086
```

### File `.env` riêng của từng service

Mỗi service có file `.env` riêng trong thư mục của mình, chứa biến đặc thù:

**Ví dụ: `IdentityService/.env`**
```env
# Identity Service specific
SERVER_PORT=8081
MYSQL_DATABASE=identity_db
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password
```

**Ví dụ: `post-service/.env`**
```env
# Post Service specific
SERVER_PORT=8083
MONGODB_DATABASE=post_db
```

---

## 🏃 Cách Chạy Microservices

### Cách 1: Chạy Tất Cả Services (Recommended)

1. Mở thư mục `Backend` trong VS Code
2. Nhấn `F5` hoặc `Ctrl+Shift+D`
3. Chọn **"🚀 Run All Microservices"** từ dropdown
4. Nhấn ▶️ Run

### Cách 2: Chạy Từng Service

1. Nhấn `Ctrl+Shift+D` để mở Debug panel
2. Chọn service muốn chạy từ dropdown (vd: "IdentityService")
3. Nhấn ▶️ Run

### Cách 3: Chạy Nhóm Services Core

Chọn **"🔧 Run Core Services (Identity + Gateway + Profile)"** để chạy 3 service quan trọng nhất.

---

## 🔄 Cách Spring-Dotenv Load Biến Môi Trường

Thư viện `spring-dotenv` (đã được thêm vào tất cả services) sẽ tự động load biến môi trường theo thứ tự ưu tiên:

1. **System Environment Variables** (cao nhất)
2. **File `.env` trong thư mục service** 
3. **File `.env.shared` (nếu được cấu hình)**

### Cách sử dụng trong `application.yaml`:

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:mydb}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:}
    
  data:
    mongodb:
      uri: mongodb://${MONGODB_HOST:localhost}:${MONGODB_PORT:27017}/${MONGODB_DATABASE:mydb}
      
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

---

## 🐳 Khởi Động Infrastructure (Docker)

Trước khi chạy services, cần khởi động các dependency:

```bash
cd Backend/docker

# Khởi động tất cả infrastructure
docker-compose up -d

# Kiểm tra trạng thái
docker-compose ps
```

**Services trong docker-compose:**
- MySQL
- MongoDB  
- Redis
- Neo4j
- Kafka + Zookeeper

---

## ⚠️ Troubleshooting

### Lỗi: "The debug type is not recognized"

**Giải pháp:** Cài extension "Debugger for Java" trong VS Code

### Lỗi: Service không tìm thấy biến môi trường

**Kiểm tra:**
1. File `.env` có ở đúng thư mục service không
2. Đã reload VS Code chưa 
3. Thử restart Java Language Server: `Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace"

### Lỗi: Port đã bị sử dụng

**Giải pháp:**
```bash
# Tìm process đang dùng port
lsof -i :8081

# Kill process
kill -9 <PID>
```

### Lỗi: Maven build failed

**Giải pháp:**
```bash
cd Backend/<service-name>
mvn clean install -DskipTests
```

---

## 📊 Thứ Tự Khởi Động Khuyến Nghị

1. **Infrastructure** (Docker containers)
2. **IdentityService** (Xác thực)
3. **api-gateway** (Routing)
4. **profile-service** (Profile người dùng)
5. **Các services còn lại** (Theo nhu cầu)

---

## 🔍 Debug Tips

- Sử dụng breakpoints trong VS Code
- Xem console output trong tab TERMINAL
- Check logs tại: `<service>/target/logs/`
- Sử dụng Postman/cURL để test API endpoints

---

## 📞 Liên Hệ

Nếu gặp vấn đề, hãy:
1. Check logs chi tiết
2. Verify tất cả biến môi trường đã được set
3. Đảm bảo infrastructure đang chạy
