# Hướng dẫn chuyển Identity -> Profile + Neo4j

> Lưu ý: Tài liệu này chỉ cung cấp hướng dẫn và full code mẫu; repo hiện tại chưa bị chỉnh sửa.

## Mục tiêu
- Bỏ Identity riêng biệt trong user-service, dùng Profile làm nguồn định danh duy nhất.
- Profile/Post/Comment/Story dùng Neo4j.
- Chat tiếp tục dùng MongoDB.
- Loại bỏ mọi IdentityClient và dữ liệu lấy từ Identity trong các service khác.

## Thay đổi chính
- User-service chỉ còn 1 nguồn dữ liệu (Neo4j). MySQL/JPA đã được loại bỏ.
- Auth/Users vẫn tồn tại nhưng thao tác trực tiếp trên UserProfile (Neo4j).
- Content-service chuyển toàn bộ entity/repository sang Neo4j.
- API Gateway dùng `/api/auth/**` và `/api/users/**` thay cho `/api/identity/**`.
- Communication-service bỏ IdentityClient, chỉ dùng ProfileClient.

## Cấu hình môi trường (tối thiểu)
- Neo4j: `NEO4J_URI`, `NEO4J_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`
- JWT: `SIGNER_KEY`, `VALID_DURATION`, `REFRESHABLE_DURATION`
- MongoDB: chỉ dùng cho chat/notification

## Chạy local (gợi ý)
1. Dựng hạ tầng:
   - `docker compose -f Backend/docker-compose.yml up -d`
2. Chạy user-service:
   - Windows: `cd Backend/user-service` rồi `mvnw.cmd spring-boot:run`
   - Linux/macOS: `cd Backend/user-service` rồi `./mvnw spring-boot:run`
3. Chạy content-service:
   - Windows: `cd Backend/content-service` rồi `mvnw.cmd spring-boot:run`
   - Linux/macOS: `cd Backend/content-service` rồi `./mvnw spring-boot:run`
4. Chạy communication-service:
   - Windows: `cd Backend/communication-service` rồi `mvnw.cmd spring-boot:run`
   - Linux/macOS: `cd Backend/communication-service` rồi `./mvnw spring-boot:run`
5. Chạy api-gateway:
   - Windows: `cd Backend/api-gateway` rồi `mvnw.cmd spring-boot:run`
   - Linux/macOS: `cd Backend/api-gateway` rồi `./mvnw spring-boot:run`

## Test nhanh
1. Health check:
   - `GET http://localhost:8081/actuator/health`
   - `GET http://localhost:8082/actuator/health`
   - `GET http://localhost:8083/actuator/health`
   - `GET http://localhost:8888/actuator/health`
2. Auth:
   - `POST http://localhost:8888/api/auth/token`
   - `POST http://localhost:8888/api/auth/introspect`
3. Profile:
   - `GET http://localhost:8888/api/profile/users/myInfo`
4. Post/Comment/Story:
   - `POST http://localhost:8888/api/post/posts/create`
   - `POST http://localhost:8888/api/post/comment/{postId}`
   - `POST http://localhost:8888/api/stories/create`

## Code đầy đủ (đã chia file)
- `Backend/docs/huong-dan-profile-identity-neo4j.user-service.md`
- `Backend/docs/huong-dan-profile-identity-neo4j.content-service.md`
- `Backend/docs/huong-dan-profile-identity-neo4j.api-gateway.md`
- `Backend/docs/huong-dan-profile-identity-neo4j.communication-service.md`
- `Backend/docs/huong-dan-profile-identity-neo4j.docker.md`
