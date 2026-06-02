"""
Chapter 3 - Cơ sở lý thuyết (mở rộng đầy đủ)
Gọi hàm này sau khi đã định nghĩa doc, heading, body, bullet, add_table, divider
"""

def write_theory(doc, heading, body, bullet, add_table, divider, code_block):

    heading('CHƯƠNG 3: CƠ SỞ LÝ THUYẾT', level=1)

    # ──────────────────────────────────────────────────────────────────────────
    # 3.1 MICROSERVICES
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.1 Kiến trúc Microservices', level=2)

    heading('3.1.1 Định nghĩa', level=3)
    body(
        'Kiến trúc microservices (microservice architecture) là một phong cách kiến trúc phần mềm '
        'trong đó một ứng dụng lớn được xây dựng từ nhiều dịch vụ nhỏ (services) độc lập, '
        'mỗi service chạy trong tiến trình riêng biệt, giao tiếp với nhau qua các cơ chế '
        'nhẹ — thường là HTTP/REST API hoặc message queue. Khái niệm này được phổ biến hóa '
        'bởi Martin Fowler và James Lewis vào năm 2014.'
    )
    body(
        'Định nghĩa chính thức: "Microservices là cách tiếp cận phát triển ứng dụng đơn lẻ '
        'như một bộ các dịch vụ nhỏ, mỗi dịch vụ chạy trong tiến trình riêng và giao tiếp '
        'với nhau thông qua cơ chế nhẹ, thường là HTTP resource API. Các dịch vụ này được xây '
        'dựng xung quanh các khả năng nghiệp vụ và có thể được triển khai độc lập bằng các '
        'cơ chế triển khai hoàn toàn tự động." — Martin Fowler'
    )

    heading('3.1.2 So sánh với Monolithic Architecture', level=3)
    add_table(
        ['Tiêu chí', 'Monolithic', 'Microservices'],
        [
            ['Cấu trúc', 'Một đơn vị triển khai duy nhất', 'Nhiều service độc lập'],
            ['Triển khai', 'Deploy toàn bộ khi có thay đổi nhỏ', 'Deploy riêng từng service'],
            ['Scale', 'Scale toàn bộ ứng dụng', 'Scale từng service theo nhu cầu'],
            ['Ngôn ngữ', 'Một ngôn ngữ/framework duy nhất', 'Đa ngôn ngữ (polyglot)'],
            ['Database', 'Một database chung', 'Mỗi service có database riêng (DB per service)'],
            ['Lỗi', 'Một module lỗi → ảnh hưởng toàn bộ', 'Lỗi cục bộ nếu có fault tolerance'],
            ['Độ phức tạp', 'Đơn giản ban đầu, phức tạp khi scale', 'Phức tạp ngay từ đầu (network, ops)'],
            ['Team', 'Phù hợp team nhỏ', 'Phù hợp nhiều team độc lập (Conway\'s Law)'],
        ],
        [4, 5.5, 5.5]
    )

    heading('3.1.3 Các nguyên tắc thiết kế Microservices', level=3)
    body('Để microservices hoạt động hiệu quả, cần tuân theo các nguyên tắc sau:')
    bullet('Single Responsibility Principle (SRP): Mỗi service chỉ đảm nhận một domain nghiệp vụ cụ thể. Trong Blur: User Service quản lý identity & profile, Content Service quản lý nội dung, Communication Service quản lý giao tiếp.')
    bullet('Database per Service: Mỗi service sở hữu và kiểm soát database riêng. Không service nào được truy cập trực tiếp database của service khác. Giao tiếp qua API hoặc events.')
    bullet('API-first Design: Interface (REST API, message contracts) được thiết kế và thống nhất trước khi implement.')
    bullet('Failure Isolation: Lỗi ở một service không được lan tỏa sang service khác (bulkhead pattern).')
    bullet('Decentralized Governance: Mỗi team tự chọn công nghệ phù hợp nhất cho service của mình.')
    bullet('Infrastructure Automation: CI/CD, Docker, container orchestration cho phép triển khai nhanh và nhất quán.')

    heading('3.1.4 Thách thức của Microservices', level=3)
    body('Kiến trúc microservices đi kèm với nhiều thách thức cần giải quyết:')
    add_table(
        ['Thách thức', 'Mô tả', 'Giải pháp trong Blur'],
        [
            ['Distributed Transactions', 'Transaction trải dài nhiều service khó đảm bảo ACID', 'Saga Pattern (Choreography)'],
            ['Data Consistency', 'Eventual consistency thay vì strong consistency', 'Outbox Pattern + Kafka at-least-once'],
            ['Service Discovery', 'Service cần biết địa chỉ của service khác', 'Environment variables + Docker DNS'],
            ['Network Latency', 'Mọi call giữa service đều là network call', 'Caffeine L1 cache + OpenFeign với connection pool'],
            ['Cascading Failure', 'Service A chậm → Service B block → toàn bộ sập', 'Resilience4j Circuit Breaker + Fallback'],
            ['Distributed Tracing', 'Khó theo dõi một request đi qua nhiều service', 'Chưa implement (hướng phát triển: Zipkin)'],
            ['Security', 'Mỗi service cần xác thực riêng', 'JWT + Keycloak, mỗi service là OAuth2 Resource Server'],
        ],
        [4, 5, 6]
    )

    body(
        'Lưu ý quan trọng: Microservices không phải là giải pháp cho mọi bài toán. '
        'Với team nhỏ hoặc ứng dụng đơn giản, chi phí vận hành microservices (network overhead, '
        'distributed debugging, deployment complexity) có thể vượt lợi ích. '
        'Quy tắc thực tế: "Start with a monolith, migrate to microservices when you feel the pain."',
        italic=True
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.2 API GATEWAY
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.2 API Gateway & Spring Cloud Gateway', level=2)

    heading('3.2.1 Định nghĩa API Gateway', level=3)
    body(
        'API Gateway là một máy chủ đóng vai trò là điểm vào duy nhất (single entry point) '
        'cho tất cả client request. Nó đứng trước tất cả các microservices và là lớp đầu tiên '
        'tiếp nhận mọi traffic từ bên ngoài.'
    )
    body('Các chức năng cốt lõi của API Gateway:')
    bullet('Routing: Điều hướng request đến đúng service dựa trên URL path, HTTP method, headers.')
    bullet('Authentication & Authorization: Xác thực token trước khi request đến service.')
    bullet('Load Balancing: Phân phối tải đều giữa nhiều instance của cùng một service.')
    bullet('Rate Limiting: Giới hạn số lượng request trong khoảng thời gian nhất định.')
    bullet('SSL Termination: Xử lý HTTPS tại Gateway, traffic nội bộ có thể dùng HTTP.')
    bullet('Request/Response Transformation: Thêm/xóa headers, rewrite URL.')
    bullet('Caching: Cache response tại biên để giảm tải cho services.')
    bullet('Circuit Breaking: Ngắt mạch khi service downstream không khả dụng.')
    bullet('Logging & Monitoring: Thu thập metrics và access logs tập trung.')

    heading('3.2.2 Spring Cloud Gateway', level=3)
    body(
        'Spring Cloud Gateway là implementation của API Gateway pattern trong hệ sinh thái Spring, '
        'xây dựng trên Spring WebFlux (reactive, non-blocking I/O) và Project Reactor. '
        'Đây là thế hệ kế tiếp của Netflix Zuul với hiệu năng cao hơn.'
    )
    body('Ưu điểm so với Zuul 1.x:')
    bullet('Non-blocking I/O: Dùng Netty thay vì Servlet container, xử lý hàng nghìn kết nối concurrent với ít thread hơn.')
    bullet('Reactive pipeline: Request/response được xử lý dưới dạng Mono/Flux stream, phù hợp với WebSocket long-lived connections.')
    bullet('Filter Chain: Pre-filters và Post-filters dưới dạng GatewayFilter, dễ mở rộng.')
    bullet('Tích hợp native với Resilience4j cho reactive circuit breaker.')

    body('Cơ chế hoạt động — Route matching:')
    code_block(
        'Request đến :8888/api/post/123\n'
        '  → Predicate check: Path=/api/post/**  ✓\n'
        '  → Pre-filters: JWT validate, rewrite path /api/post/→/posts/\n'
        '  → Forward to: http://content-service:8082/posts/123\n'
        '  → Post-filters: Remove sensitive headers\n'
        '  → Response trả về client'
    )
    body(
        'Lưu ý: Spring Cloud Gateway yêu cầu project KHÔNG import spring-boot-starter-web '
        '(servlet-based) mà phải dùng spring-boot-starter-webflux. Hai dependency này '
        'xung đột với nhau vì một dùng Tomcat (blocking), một dùng Netty (non-blocking).',
        italic=True
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.3 KEYCLOAK / OAUTH2 / JWT
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.3 Xác thực & Phân quyền — Keycloak / OAuth 2.0 / JWT', level=2)

    heading('3.3.1 OAuth 2.0', level=3)
    body(
        'OAuth 2.0 là framework ủy quyền (authorization framework) chuẩn công nghiệp (RFC 6749) '
        'cho phép ứng dụng thứ ba truy cập tài nguyên thay mặt người dùng mà không cần biết '
        'mật khẩu của họ. OAuth 2.0 định nghĩa 4 grant types (luồng cấp phép):'
    )
    add_table(
        ['Grant Type', 'Mô tả', 'Dùng khi'],
        [
            ['Authorization Code', 'Redirect-based, trả về authorization code → đổi lấy token', 'Web app, SPA (dùng PKCE)'],
            ['Client Credentials', 'Service-to-service, không có user context', 'Backend APIs giao tiếp nhau'],
            ['Resource Owner Password', 'User gửi username/password trực tiếp cho app', 'Deprecated, legacy apps'],
            ['Device Code', 'Cho thiết bị không có browser (Smart TV, CLI)', 'IoT, CLI tools'],
        ],
        [4, 6, 5]
    )
    body(
        'Blur sử dụng Authorization Code Flow với PKCE (Proof Key for Code Exchange) '
        'cho luồng đăng nhập từ React SPA, và Client Credentials cho giao tiếp '
        'service-to-service nội bộ.'
    )

    heading('3.3.2 OpenID Connect (OIDC)', level=3)
    body(
        'OpenID Connect (OIDC) là lớp identity được xây dựng trên OAuth 2.0. Trong khi '
        'OAuth 2.0 chỉ xử lý authorization (quyền truy cập), OIDC bổ sung authentication '
        '(xác thực danh tính) bằng cách thêm ID Token (JWT chứa thông tin user).'
    )
    body('Các thành phần chính trong OIDC:')
    bullet('ID Token: JWT chứa thông tin user (sub, email, name, roles). Dùng để xác minh danh tính.')
    bullet('Access Token: Token để gọi API (có thể là JWT hoặc opaque token).')
    bullet('Refresh Token: Token để lấy Access Token mới khi hết hạn, không cần đăng nhập lại.')
    bullet('UserInfo Endpoint: Endpoint trả về thông tin user từ Access Token.')
    bullet('Discovery Endpoint (.well-known/openid-configuration): Chứa metadata về OIDC server (issuer, JWKS URI, token endpoint...).')

    heading('3.3.3 JWT (JSON Web Token)', level=3)
    body(
        'JWT (RFC 7519) là chuẩn định nghĩa cách truyền thông tin giữa các bên dưới dạng '
        'JSON object được ký số. JWT có cấu trúc 3 phần phân cách bởi dấu chấm (.):'
    )
    code_block('header.payload.signature')
    add_table(
        ['Phần', 'Nội dung', 'Ví dụ'],
        [
            ['Header', 'Algorithm và token type', '{"alg":"RS256","typ":"JWT"}'],
            ['Payload', 'Claims: sub, iat, exp, roles, email...', '{"sub":"user-uuid","exp":1234567890,"realm_access":{"roles":["USER"]}}'],
            ['Signature', 'HMACSHA256 hoặc RSA/ECDSA signature', 'RS256(base64(header)+"."+base64(payload), privateKey)'],
        ],
        [2.5, 5, 7.5]
    )
    body('Các loại Claims trong JWT:')
    bullet('Registered Claims (chuẩn): iss (issuer), sub (subject/userId), aud (audience), exp (expiration), iat (issued at), nbf (not before), jti (JWT ID).')
    bullet('Public Claims: email, name, preferred_username — được đăng ký tại IANA.')
    bullet('Private Claims: realm_access.roles, resource_access — tùy chỉnh bởi Keycloak/application.')
    body(
        'Lưu ý bảo mật JWT:',
        bold=True
    )
    bullet('KHÔNG lưu thông tin nhạy cảm (password, credit card) trong payload — payload chỉ được Base64-encoded, KHÔNG được mã hóa, bất kỳ ai cũng có thể decode.')
    bullet('Luôn validate signature trước khi dùng claims. Dùng thư viện đã được kiểm tra bảo mật (nimbus-jose-jwt, jjwt).')
    bullet('Kiểm tra exp claim để đảm bảo token chưa hết hạn.')
    bullet('Dùng RS256 (asymmetric) thay vì HS256 (symmetric) cho hệ thống phân tán — cho phép nhiều service verify với public key mà không cần share secret.')
    bullet('Access Token nên có TTL ngắn (5–15 phút), Refresh Token dài hơn (1–7 ngày).')

    heading('3.3.4 Keycloak', level=3)
    body(
        'Keycloak là Identity and Access Management (IAM) solution mã nguồn mở do Red Hat phát triển, '
        'hỗ trợ đầy đủ OAuth 2.0, OpenID Connect, SAML 2.0. Keycloak cung cấp:'
    )
    bullet('Realm: Đơn vị quản lý độc lập — mỗi realm có users, clients, roles, sessions riêng. Blur dùng realm "blur-realm".')
    bullet('Client: Ứng dụng đăng ký với Keycloak. Blur có: "blur-app" (React SPA, public client), "blur-gateway" (confidential), các service backend.')
    bullet('User Federation: Kết nối với LDAP/Active Directory.')
    bullet('Social Login: Google, GitHub, Facebook OAuth.')
    bullet('Admin REST API: Quản lý users, roles programmatically — UserService dùng Keycloak Admin Client để tạo user khi đăng ký.')
    bullet('Email Verification: Gửi email kích hoạt tài khoản tự động.')
    bullet('JWKS URI: Endpoint public key để các service verify JWT — Spring Security gọi endpoint này khi khởi động.')

    body('Luồng xác thực trong Blur:')
    code_block(
        'User đăng nhập\n'
        '  → Frontend gửi credentials đến /api/auth/login (User Service)\n'
        '  → User Service gọi Keycloak Token Endpoint\n'
        '  → Keycloak xác thực, trả về {access_token, refresh_token, id_token}\n'
        '  → User Service set HttpOnly cookie (secure, SameSite=Strict)\n'
        '  → Frontend sau đó gửi mọi request với cookie\n'
        '  → API Gateway đọc token từ cookie/Authorization header\n'
        '  → Spring Security xác thực JWT bằng Keycloak public key (JWKS)\n'
        '  → Forward request với userId (JWT sub) đến service tương ứng'
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.4 NEO4J
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.4 Graph Database — Neo4j & Cypher', level=2)

    heading('3.4.1 Định nghĩa Graph Database', level=3)
    body(
        'Graph Database là hệ quản trị cơ sở dữ liệu lưu trữ dữ liệu dưới dạng đồ thị '
        '(graph) với các thực thể chính là: node (nút), relationship (quan hệ/cạnh), và '
        'property (thuộc tính). Khác với relational database dùng bảng và JOIN, '
        'graph database duyệt quan hệ trực tiếp thông qua con trỏ (pointer traversal), '
        'không cần tính toán JOIN tốn kém.'
    )
    add_table(
        ['Khái niệm', 'Relational DB', 'Graph DB (Neo4j)'],
        [
            ['Đơn vị lưu trữ', 'Row trong Table', 'Node với Labels'],
            ['Quan hệ', 'Foreign Key + JOIN', 'Relationship có tên và direction'],
            ['Schema', 'Cứng (schema-full)', 'Linh hoạt (schema-optional)'],
            ['Truy vấn quan hệ sâu', 'O(n) với nhiều JOIN', 'O(log n) hoặc tốt hơn (index-free adjacency)'],
            ['Ngôn ngữ truy vấn', 'SQL', 'Cypher (Neo4j), Gremlin, SPARQL'],
        ],
        [4.5, 5, 5.5]
    )

    heading('3.4.2 Cypher Query Language', level=3)
    body(
        'Cypher là ngôn ngữ truy vấn khai báo (declarative) được thiết kế đặc biệt cho '
        'graph database, dùng ký pháp ASCII-art để biểu diễn pattern đồ thị.'
    )
    body('Cú pháp cơ bản:')
    code_block(
        '// Node: (alias:Label {property: value})\n'
        '// Relationship: -[:REL_TYPE {prop}]->\n\n'
        '// Tìm tất cả người A đang follow\n'
        'MATCH (a:UserProfile {userId: $userId})-[:FOLLOWS]->(b:UserProfile)\n'
        'RETURN b\n\n'
        '// Đếm mutual followers (bạn chung)\n'
        'MATCH (me:UserProfile {id: $myId})-[:FOLLOWS]->(mutual)<-[:FOLLOWS]-(other:UserProfile)\n'
        'WHERE NOT (me)-[:FOLLOWS]->(other) AND me <> other\n'
        'RETURN other, count(mutual) AS mutualCount\n'
        'ORDER BY mutualCount DESC'
    )

    heading('3.4.3 Tại sao Neo4j phù hợp cho mạng xã hội', level=3)
    body('Graph database phù hợp với social network vì:')
    bullet('Quan hệ follow/friend là dữ liệu đồ thị tự nhiên. Neo4j lưu trữ và duyệt quan hệ với hiệu năng O(1) per hop, không phụ thuộc vào tổng số node.')
    bullet('Bài toán "gợi ý kết bạn" yêu cầu duyệt đồ thị nhiều cấp (friend-of-friend) — rất tốn kém với SQL JOIN, rất tự nhiên với Cypher MATCH.')
    bullet('Feed projection: PostFeedItem được lưu như node có quan hệ với user, cho phép đọc feed của một user cực nhanh.')
    body(
        'Lưu ý hiệu năng: Neo4j sử dụng "index-free adjacency" — mỗi node lưu '
        'con trỏ trực tiếp đến các relationship của nó. Khi duyệt đồ thị, '
        'Neo4j không cần scan toàn bộ bảng như SQL, chỉ follow con trỏ. '
        'Điều này giúp deep traversal (nhiều hop) không bị degradation khi data tăng.',
        italic=True
    )

    heading('3.4.4 Spring Data Neo4j', level=3)
    body(
        'Spring Data Neo4j là abstraction layer cho phép tương tác với Neo4j trong '
        'Spring Boot, tương tự JPA cho relational database. Cung cấp:'
    )
    bullet('@Node annotation: Đánh dấu class là Neo4j node.')
    bullet('@Relationship annotation: Định nghĩa relationship giữa các node.')
    bullet('Repository pattern: Interface extend Neo4jRepository với @Query annotation cho custom Cypher.')
    bullet('Transaction management: @Transactional hoạt động với Neo4j session.')

    # ──────────────────────────────────────────────────────────────────────────
    # 3.5 KAFKA
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.5 Messaging — Apache Kafka', level=2)

    heading('3.5.1 Định nghĩa', level=3)
    body(
        'Apache Kafka là nền tảng event streaming phân tán (distributed event streaming platform) '
        'được phát triển ban đầu tại LinkedIn và open-source năm 2011. '
        'Kafka được thiết kế để xử lý hàng triệu sự kiện mỗi giây với độ trễ thấp, '
        'lưu trữ bền vững (durable), và khả năng replay event.'
    )

    heading('3.5.2 Các khái niệm cốt lõi', level=3)
    add_table(
        ['Khái niệm', 'Định nghĩa'],
        [
            ['Topic', 'Kênh phân loại message, tương tự "table" trong database. Mỗi topic có tên duy nhất (e.g., "post-events").'],
            ['Partition', 'Topic được chia thành nhiều partition để tăng throughput và song song hóa. Mỗi partition là ordered, immutable log.'],
            ['Producer', 'Client gửi (publish) message vào topic.'],
            ['Consumer', 'Client đọc (subscribe) message từ topic.'],
            ['Consumer Group', 'Nhóm consumers cùng đọc một topic. Mỗi partition chỉ được đọc bởi 1 consumer trong group (load balancing).'],
            ['Offset', 'Số thứ tự của message trong partition. Consumer tự quản lý offset đã đọc.'],
            ['Broker', 'Server Kafka. Cluster gồm nhiều broker để đảm bảo high availability.'],
            ['Replication Factor', 'Số bản sao của mỗi partition trên các broker khác nhau (fault tolerance).'],
            ['KRaft Mode', 'Kafka Raft — chế độ mới thay thế ZooKeeper bằng Raft consensus protocol tích hợp sẵn, đơn giản hóa vận hành.'],
        ],
        [3.5, 11.5]
    )

    heading('3.5.3 Đảm bảo delivery', level=3)
    body('Kafka cung cấp 3 mức đảm bảo delivery:')
    bullet('At-most-once: Message có thể bị mất, không bao giờ duplicate. (Producer không retry khi lỗi.)')
    bullet('At-least-once: Message không bị mất nhưng có thể duplicate. (Producer retry khi lỗi.) ← Blur dùng mức này.')
    bullet('Exactly-once: Đảm bảo message đến chính xác 1 lần. (Kafka Transactions — phức tạp, overhead cao.)')
    body(
        'Với at-least-once, Consumer cần implement idempotency — xử lý duplicate message '
        'không gây hậu quả phụ (e.g., check tồn tại trước khi insert, dùng upsert thay insert).',
        italic=True
    )

    heading('3.5.4 Kafka vs Traditional Message Queue', level=3)
    add_table(
        ['Tiêu chí', 'Kafka', 'RabbitMQ / ActiveMQ'],
        [
            ['Lưu trữ message', 'Lưu trữ lâu dài, có thể replay', 'Xóa sau khi consumer ACK'],
            ['Mô hình', 'Log-based (append-only)', 'Queue-based (pop và delete)'],
            ['Throughput', 'Rất cao (triệu msg/s)', 'Trung bình (vài chục nghìn msg/s)'],
            ['Consumer', 'Pull-based, consumer tự quản lý offset', 'Push-based, broker push message'],
            ['Use case', 'Event streaming, audit log, CQRS', 'Task queue, RPC, request/reply'],
            ['Ordering', 'Đảm bảo trong một partition', 'Không đảm bảo (trừ cấu hình đặc biệt)'],
        ],
        [4, 6, 5]
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.6 WEBSOCKET & WEBRTC
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.6 Giao tiếp thời gian thực — WebSocket & WebRTC', level=2)

    heading('3.6.1 WebSocket', level=3)
    body(
        'WebSocket (RFC 6455) là giao thức truyền thông hai chiều (full-duplex) trên một kết '
        'nối TCP duy nhất. Khác với HTTP request/response (half-duplex), WebSocket cho phép '
        'cả client và server chủ động gửi message bất kỳ lúc nào.'
    )
    body('Quá trình thiết lập kết nối WebSocket (Handshake):')
    code_block(
        'Client → Server: HTTP Upgrade request\n'
        '  GET /ws HTTP/1.1\n'
        '  Upgrade: websocket\n'
        '  Connection: Upgrade\n'
        '  Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\n\n'
        'Server → Client: 101 Switching Protocols\n'
        '  HTTP/1.1 101 Switching Protocols\n'
        '  Upgrade: websocket\n'
        '  Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=\n\n'
        '→ Kết nối TCP vẫn mở, dữ liệu truyền dưới dạng WebSocket frames'
    )

    body('So sánh các kỹ thuật real-time:')
    add_table(
        ['Kỹ thuật', 'Mô tả', 'Độ trễ', 'Overhead', 'Phù hợp'],
        [
            ['Short Polling', 'Client request định kỳ mỗi N giây', 'N giây', 'Cao (nhiều request)', 'Không thực sự real-time'],
            ['Long Polling', 'Server giữ request đến khi có data hoặc timeout', 'Thấp', 'Trung bình', 'Khi WS không khả dụng'],
            ['Server-Sent Events', 'Server push 1 chiều qua HTTP', 'Thấp', 'Thấp', 'Dashboard, feed updates'],
            ['WebSocket', 'Full-duplex, 2 chiều', 'Rất thấp (~ms)', 'Thấp sau handshake', 'Chat, game, collaboration'],
        ],
        [3.5, 5, 2, 2.5, 2]
    )

    heading('3.6.2 STOMP over WebSocket', level=3)
    body(
        'STOMP (Simple Text Oriented Messaging Protocol) là messaging protocol đơn giản '
        'chạy trên WebSocket. STOMP thêm ngữ nghĩa messaging (subscribe, send, ack) '
        'lên trên raw WebSocket frames.'
    )
    body('Blur dùng Spring\'s STOMP WebSocket cho notifications và call signaling:')
    bullet('/ws/comm/websocket — SockJS endpoint (fallback HTTP nếu WS bị block).')
    bullet('/app/... — prefix cho MessageMapping handlers (gửi đến server).')
    bullet('/user/{userId}/queue/... — user-specific destination (server push đến user).')
    bullet('/topic/... — broadcast destination (tất cả subscriber nhận).')

    heading('3.6.3 Socket.IO', level=3)
    body(
        'Socket.IO là thư viện JavaScript xây dựng trên WebSocket với các tính năng bổ sung: '
        'tự động reconnect, fallback về long-polling, room/namespace, binary data support. '
        'Blur dùng Socket.IO (Java server side: netty-socketio) cho tính năng chat thời gian thực.'
    )
    body(
        'Lưu ý: Socket.IO KHÔNG phải là pure WebSocket — nó có protocol riêng. '
        'Client Socket.IO không thể kết nối trực tiếp với pure WebSocket server và ngược lại.',
        italic=True
    )

    heading('3.6.4 WebRTC (Web Real-Time Communication)', level=3)
    body(
        'WebRTC là tập hợp các API và giao thức chuẩn (W3C + IETF) cho phép truyền '
        'audio, video, và data P2P (peer-to-peer) trực tiếp giữa các trình duyệt, '
        'không cần plugin hay server trung gian cho media stream.'
    )
    body('Kiến trúc WebRTC gồm 3 thành phần chính:')
    bullet('getUserMedia API: Truy cập camera và microphone của thiết bị.')
    bullet('RTCPeerConnection: Quản lý kết nối P2P, mã hóa (DTLS-SRTP), codec negotiation.')
    bullet('RTCDataChannel: Kênh truyền dữ liệu tùy ý P2P (text, binary).')

    body('Luồng thiết lập kết nối WebRTC (Signaling + ICE):')
    code_block(
        'Caller (A)                  Signaling Server              Callee (B)\n'
        '   |                             |                             |\n'
        '   |-- createOffer() ----------->|                             |\n'
        '   |   SDP Offer                 |-- relay offer ------------->|\n'
        '   |                             |                             |-- createAnswer()\n'
        '   |                             |<-- SDP Answer --------------|  \n'
        '   |<-- relay answer ------------|                             |\n'
        '   |-- setRemoteDescription()    |    setLocalDescription()    |\n'
        '   |                             |                             |\n'
        '   |-- ICE candidates ---------->|-- relay ICE --------------->|\n'
        '   |<-- ICE candidates ----------|<-- relay ICE ---------------|  \n'
        '   |                             |                             |\n'
        '   |============ P2P Media Stream (audio/video) ==============>|\n'
        '   |<============ P2P Media Stream (audio/video) ==============|'
    )

    body('Các giao thức nền của WebRTC:')
    bullet('SDP (Session Description Protocol): Mô tả media capabilities (codec, bitrate, resolution) của mỗi peer.')
    bullet('ICE (Interactive Connectivity Establishment): Thuật toán tìm đường kết nối tốt nhất giữa 2 peer qua NAT/firewall.')
    bullet('STUN (Session Traversal Utilities for NAT): Server giúp peer biết public IP/port của mình.')
    bullet('TURN (Traversal Using Relays around NAT): Server relay media khi P2P không thiết lập được (firewall nghiêm ngặt).')
    bullet('DTLS (Datagram TLS): Mã hóa cho UDP stream.')
    bullet('SRTP (Secure RTP): Mã hóa media stream.')

    body(
        'Lưu ý production: Trong môi trường thực tế (corporate firewall, symmetric NAT), '
        'STUN server đơn thuần có thể không đủ. Cần TURN server (Coturn) để đảm bảo '
        'kết nối trong mọi trường hợp. Blur hiện dùng Google STUN server cho demo.',
        italic=True
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.7 MULTI-LEVEL CACHE
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.7 Multi-Level Cache — Caffeine L1 + Redis L2', level=2)

    heading('3.7.1 Tại sao cần Cache?', level=3)
    body(
        'Cache là kỹ thuật lưu trữ tạm thời dữ liệu đã tính toán hoặc đọc từ nguồn chậm, '
        'để các lần truy cập sau được phục vụ nhanh hơn. Nguyên lý cơ bản: '
        '"Đừng tính/đọc lại thứ đã có."'
    )
    add_table(
        ['Tầng lưu trữ', 'Thời gian truy cập', 'Dung lượng'],
        [
            ['CPU Register', '< 1 ns', 'Bytes'],
            ['CPU L1 Cache', '~1 ns', 'KB'],
            ['CPU L2 Cache', '~4 ns', 'MB'],
            ['RAM (Caffeine)', '~10-100 ns', 'GB'],
            ['Redis (Network)', '~1-5 ms', 'GB-TB'],
            ['SSD Database', '~100 µs', 'TB'],
            ['HDD Database', '~10 ms', 'TB'],
        ],
        [5, 4, 6]
    )

    heading('3.7.2 Caffeine Cache (L1 — In-process)', level=3)
    body(
        'Caffeine là thư viện caching Java hiệu năng cao, thay thế cho Guava Cache. '
        'Caffeine sử dụng thuật toán W-TinyLFU (Window TinyLFU) — kết hợp LRU và LFU — '
        'để đạt hit rate cao hơn các thuật toán truyền thống trong workload thực tế.'
    )
    body('Đặc điểm Caffeine:')
    bullet('In-memory: Dữ liệu nằm trong heap của JVM process, truy cập ~10 nanoseconds.')
    bullet('Eviction policies: Size-based, Time-based (TTL/TTI), Reference-based (soft/weak).')
    bullet('W-TinyLFU algorithm: Dùng frequency sketch để ước tính tần suất truy cập, giữ lại hot items, evict cold items.')
    bullet('Async loading: Hỗ trợ CacheLoader async, tránh blocking thread khi cache miss.')
    bullet('Nhược điểm: Chỉ tồn tại trong 1 JVM instance. Khi có nhiều instance (horizontal scaling), cache không được chia sẻ → inconsistency.')

    heading('3.7.3 Redis Cache (L2 — Distributed)', level=3)
    body(
        'Redis (Remote Dictionary Server) là in-memory data store hỗ trợ nhiều cấu trúc dữ liệu '
        '(String, Hash, List, Set, ZSet, Stream...), thường được dùng làm distributed cache, '
        'session store, message broker, và leaderboard.'
    )
    body('Đặc điểm Redis trong vai trò L2 cache:')
    bullet('Shared across instances: Tất cả JVM instance đều đọc/ghi cùng một Redis → dữ liệu nhất quán.')
    bullet('Persistence: RDB snapshot và AOF (Append-Only File) tùy chọn — dữ liệu không mất khi restart.')
    bullet('TTL native: EXPIRE command cho phép set thời gian hết hạn tự động cho mỗi key.')
    bullet('Pub/Sub: Redis Publish/Subscribe cho phép broadcast message đến nhiều subscriber.')
    bullet('Nhược điểm: Mỗi lần đọc là 1 network round-trip (~1-5ms) — chậm hơn Caffeine 100,000 lần.')

    heading('3.7.4 TwoLevelCache Implementation', level=3)
    body(
        'Blur implement custom TwoLevelCache class implement Spring Cache interface, '
        'wrap cả Caffeine và Redis trong một đối tượng duy nhất:'
    )
    code_block(
        'get(key):\n'
        '  1. Kiểm tra Caffeine L1\n'
        '     → L1 HIT:  trả về ngay   (~10ns)\n'
        '  2. Kiểm tra Redis L2\n'
        '     → L2 HIT:  ghi vào L1, trả về   (~1-5ms)\n'
        '  3. Gọi database\n'
        '     → DB HIT:  ghi vào L1 + L2, trả về   (~100ms+)\n\n'
        'put(key, value):\n'
        '  → Ghi vào L1 (Caffeine) + L2 (Redis) đồng thời\n\n'
        'evict(key):\n'
        '  → Xóa khỏi L1 + L2\n'
        '  → Publish Redis Pub/Sub message → tất cả instance evict L1'
    )

    heading('3.7.5 Cache Invalidation Strategy', level=3)
    body(
        '"There are only two hard things in Computer Science: cache invalidation and naming things." '
        '— Phil Karlton',
        italic=True
    )
    body('Cache Invalidation là thách thức lớn nhất khi dùng cache. Blur áp dụng:')
    bullet('Write-through: Khi data thay đổi, cập nhật cả cache và database ngay lập tức. Đảm bảo cache luôn đồng bộ nhưng tăng latency write.')
    bullet('Cache-aside (Lazy loading): Read-path kiểm tra cache trước, nếu miss mới đọc DB và populate cache.')
    bullet('Redis Pub/Sub Invalidation: Khi instance A evict cache, publish message lên channel. Các instance B, C, D subscribe và evict L1 tương ứng.')
    body(
        'Lưu ý: Redis Pub/Sub không đảm bảo delivery (fire-and-forget). '
        'Nếu một instance offline khi message được publish, nó sẽ không nhận được khi comeback. '
        'Giải pháp: Dùng Redis Streams (có ack và history) hoặc chấp nhận eventual consistency '
        'với TTL ngắn.',
        italic=True
    )

    heading('3.7.6 Cache Warming', level=3)
    body(
        'Cache Warming (hay Cache Pre-loading) là kỹ thuật nạp sẵn dữ liệu hot vào cache '
        'khi service khởi động, trước khi nhận traffic thực sự. '
        'Tránh "cold start" — khi service vừa restart, mọi request đều cache miss và '
        'gây đột biến tải lên database.'
    )
    body(
        'Blur implement CacheWarmupRunner (implements ApplicationRunner) '
        'chạy sau khi Spring context khởi động xong, preload các cache phổ biến: '
        'popular posts, recent stories, trending profiles.'
    )

    heading('3.7.7 Distributed Lock với Redisson', level=3)
    body(
        'Cache Stampede (Thundering Herd) là vấn đề: khi một cache key hết hạn, '
        'nhiều request đồng thời cùng miss cache, tất cả đều gọi database, '
        'gây spike tải đột ngột.'
    )
    body('Giải pháp: Distributed Lock (Mutex-based loading)')
    code_block(
        'Khi cache miss:\n'
        '  1. Thread A acquire Redisson RLock (distributed lock)\n'
        '  2. Thread A đọc DB, populate cache, release lock\n'
        '  3. Thread B, C, D (đang đợi) acquire lock → thấy cache đã có → trả về\n'
        '  4. Chỉ 1 DB call dù có N concurrent request'
    )
    body(
        'Redisson RLock implement Red Lock algorithm (Redlock) — distributed lock '
        'đảm bảo safety (chỉ 1 holder tại 1 thời điểm) và liveness (lock được release '
        'kể cả khi holder crash, nhờ TTL tự động).',
        italic=True
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.8 CQRS
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.8 CQRS & Event-Driven Architecture', level=2)

    heading('3.8.1 Định nghĩa CQRS', level=3)
    body(
        'CQRS (Command Query Responsibility Segregation) là pattern phân tách hoàn toàn '
        'mô hình dữ liệu (và thường cả database) cho các thao tác ghi (Command) và '
        'đọc (Query). Được đề xuất bởi Greg Young dựa trên nguyên tắc CQS của Bertrand Meyer.'
    )
    body(
        'Nguyên tắc CQS (Command Query Separation): '
        '"Mọi method hoặc là Command (thay đổi state, không trả về giá trị) hoặc là Query '
        '(trả về giá trị, không thay đổi state) — không bao giờ là cả hai."'
    )

    heading('3.8.2 Tại sao cần CQRS cho Newsfeed?', level=3)
    body(
        'Bài toán: User A có 100,000 followers. Khi A đăng bài mới, làm sao 100,000 người '
        'đó thấy bài trong feed của mình? Hai cách tiếp cận phổ biến:'
    )
    add_table(
        ['Tiêu chí', 'Pull Model (không CQRS)', 'Push Model (CQRS)'],
        [
            ['Cách đọc feed', 'JOIN: posts của mọi người đang follow', 'Đọc pre-computed PostFeedItem'],
            ['Khi đăng bài', 'Chỉ lưu bài đăng', 'Phân phối vào feed của N followers'],
            ['Độ trễ đọc', 'Chậm (JOIN lớn)', 'Nhanh (đọc 1 bảng)'],
            ['Độ trễ ghi', 'Nhanh', 'Chậm hơn (phân phối cho N followers)'],
            ['Storage', 'Ít (1 bản ghi / bài đăng)', 'Nhiều (N bản ghi / bài đăng)'],
            ['Phù hợp', 'User ít follower', 'User nhiều follower (celebrity problem)'],
        ],
        [4, 5.5, 5.5]
    )
    body(
        'Blur dùng Push Model: khi đăng bài, Kafka event được phát ra và FeedProjectionService '
        'tạo PostFeedItem cho từng follower. Đọc feed chỉ là SELECT * WHERE targetUserId = ?.'
    )

    heading('3.8.3 Event-Driven Architecture', level=3)
    body(
        'Event-Driven Architecture (EDA) là paradigm thiết kế hệ thống xung quanh '
        'production, detection, consumption, và reaction của events. '
        'Services giao tiếp gián tiếp qua events thay vì gọi trực tiếp nhau.'
    )
    body('Lợi ích của EDA:')
    bullet('Loose coupling: Producer không biết và không phụ thuộc vào Consumer.')
    bullet('Scalability: Consumers có thể scale độc lập dựa trên lag.')
    bullet('Resilience: Nếu consumer down, events tích lũy trong Kafka và được xử lý khi consumer phục hồi.')
    bullet('Audit trail: Kafka log là lịch sử đầy đủ mọi sự kiện trong hệ thống.')
    body(
        'Nhược điểm: Eventual consistency — sau khi đăng bài, follower nhận được bài trong '
        'feed sau vài milliseconds (async), không phải ngay lập tức. '
        'Với hầu hết use case mạng xã hội, đây là acceptable trade-off.',
        italic=True
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.9 OUTBOX PATTERN
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.9 Outbox Pattern', level=2)

    heading('3.9.1 Vấn đề Dual Write', level=3)
    body(
        'Dual Write Problem: Trong hệ thống phân tán, khi một service cần '
        'thực hiện 2 thao tác atomic — lưu vào database VÀ gửi message lên Kafka — '
        'không có cơ chế đảm bảo cả hai thành công hoặc cả hai thất bại.'
    )
    code_block(
        'Vấn đề:\n'
        '  1. BEGIN TRANSACTION\n'
        '  2. INSERT INTO posts (...)  ← thành công\n'
        '  3. kafkaTemplate.send(...)  ← THẤT BẠI (Kafka down, network lỗi)\n'
        '  4. COMMIT\n'
        '  → Post được tạo nhưng event không được gửi\n'
        '  → Follower không bao giờ thấy bài trong feed  (DATA INCONSISTENCY!)'
    )

    heading('3.9.2 Giải pháp Outbox Pattern', level=3)
    body(
        'Outbox Pattern đảm bảo at-least-once event publishing bằng cách '
        'ghi event vào database (cùng transaction với business data) thay vì '
        'gửi trực tiếp lên Kafka. Một process riêng (Outbox Scheduler/Relay) '
        'đọc và forward events sang Kafka.'
    )
    code_block(
        'Giải pháp:\n'
        '  1. BEGIN TRANSACTION\n'
        '  2. INSERT INTO posts (...)        ← business operation\n'
        '  3. INSERT INTO outbox_events (...) ← cùng transaction!\n'
        '  4. COMMIT  ← cả 2 thành công hoặc cả 2 rollback\n\n'
        '  OutboxScheduler (chạy mỗi 1-5 giây):\n'
        '  1. SELECT * FROM outbox_events WHERE processed = false\n'
        '  2. kafkaTemplate.send(...)\n'
        '  3. UPDATE outbox_events SET processed = true\n'
        '  → Ngay cả khi Kafka tạm thời down, events được retry'
    )
    body(
        'Lưu ý: Outbox Pattern đảm bảo at-least-once (không mất event) nhưng không '
        'đảm bảo exactly-once (có thể duplicate nếu Kafka send thành công nhưng '
        'UPDATE outbox bị lỗi trước khi commit). Consumer cần implement idempotency.',
        italic=True
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.10 SAGA
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.10 Saga Pattern', level=2)

    heading('3.10.1 Vấn đề Distributed Transaction', level=3)
    body(
        'Trong hệ thống microservices với Database per Service, không thể dùng '
        'ACID transaction truyền thống (2PC — Two-Phase Commit) qua nhiều database vì: '
        '(1) Không phải mọi DB đều hỗ trợ XA transactions, '
        '(2) 2PC tạo distributed lock gây performance bottleneck, '
        '(3) Coordinator (transaction manager) là single point of failure.'
    )

    heading('3.10.2 Định nghĩa Saga', level=3)
    body(
        'Saga là một chuỗi local transactions, mỗi transaction thực hiện thay đổi trong '
        'một service và publish event/message để kích hoạt transaction tiếp theo. '
        'Nếu bất kỳ local transaction nào thất bại, Saga thực hiện compensating transactions '
        '(giao dịch bù trừ) để hoàn tác các thay đổi đã thực hiện.'
    )

    heading('3.10.3 Hai kiểu Saga', level=3)
    add_table(
        ['Tiêu chí', 'Choreography', 'Orchestration'],
        [
            ['Điều phối', 'Phân tán — mỗi service tự biết làm gì', 'Tập trung — Saga Orchestrator điều khiển luồng'],
            ['Giao tiếp', 'Events/messages qua Kafka', 'Command messages từ Orchestrator'],
            ['Coupling', 'Loose coupling', 'Services phụ thuộc vào Orchestrator'],
            ['Phù hợp', 'Workflow đơn giản, ít bước', 'Workflow phức tạp, nhiều bước'],
            ['Debug', 'Khó (luồng phân tán)', 'Dễ hơn (luồng tập trung)'],
            ['Blur dùng', '✓ (User Delete Saga)', '✗'],
        ],
        [4, 5.5, 5.5]
    )
    body('Blur implement Saga Choreography cho luồng xóa tài khoản:')
    code_block(
        'User Service                 Kafka                   Content/Comm Service\n'
        '     |                         |                            |\n'
        '     |-- DELETE user --------->|                            |\n'
        '     |-- publish USER_DELETED->|-- UserDeleteSagaConsumer-->|\n'
        '     |                         |   xóa posts, comments,     |\n'
        '     |                         |   conversations, messages  |\n'
        '     |                         |                            |\n'
        '     |   Nếu thất bại: service tự retry (Kafka at-least-once)\n'
        '     |   Không có compensating transaction ở bước này'
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.11 RESILIENCE4J
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.11 Resilience4j — Circuit Breaker & Fault Tolerance', level=2)

    heading('3.11.1 Định nghĩa Circuit Breaker', level=3)
    body(
        'Circuit Breaker (cầu dao tự động) là design pattern được Michael Nygard '
        'mô tả trong sách "Release It!" (2007), lấy cảm hứng từ cầu dao điện. '
        'Pattern này bao bọc (wrap) lời gọi đến service khác và theo dõi tỷ lệ lỗi. '
        'Khi tỷ lệ lỗi vượt ngưỡng, Circuit Breaker "ngắt" và ngừng gửi request đến '
        'service đó trong một khoảng thời gian.'
    )

    heading('3.11.2 Các trạng thái Circuit Breaker', level=3)
    code_block(
        '         ┌──────────────────────────────────────┐\n'
        '         │              CLOSED                  │\n'
        '         │  (hoạt động bình thường)             │\n'
        '         │  Đếm failures trong sliding window   │\n'
        '         └──────────────┬───────────────────────┘\n'
        '                        │ failure rate > threshold\n'
        '                        ▼\n'
        '         ┌──────────────────────────────────────┐\n'
        '         │               OPEN                   │\n'
        '         │  (ngắt mạch)                         │\n'
        '         │  Trả về fallback ngay lập tức         │\n'
        '         │  Không gửi request đến service       │\n'
        '         └──────────────┬───────────────────────┘\n'
        '                        │ sau waitDurationInOpenState\n'
        '                        ▼\n'
        '         ┌──────────────────────────────────────┐\n'
        '         │            HALF-OPEN                 │\n'
        '         │  (thăm dò phục hồi)                  │\n'
        '         │  Cho phép N request thử nghiệm       │\n'
        '         └──────────────┬───────────────────────┘\n'
        '              success   │   failure\n'
        '         ┌──────────────┤──────────────┐\n'
        '         ▼              │              ▼\n'
        '      CLOSED            │            OPEN'
    )

    heading('3.11.3 Các Pattern của Resilience4j', level=3)
    add_table(
        ['Pattern', 'Định nghĩa', 'Cấu hình trong Blur'],
        [
            ['Circuit Breaker',
             'Ngắt mạch khi failure rate > threshold. Tránh retry vô nghĩa vào service down.',
             'slidingWindowSize=10, failureRateThreshold=50%, waitDurationInOpenState=60s'],
            ['Retry',
             'Tự động thử lại với exponential backoff cho lỗi tạm thời (network glitch, 503).',
             'maxAttempts=3, waitDuration=500ms, exponential multiplier=2'],
            ['Bulkhead',
             'Giới hạn số lượng concurrent calls đến mỗi service. Tránh một service chậm làm cạn kiệt thread pool.',
             'maxConcurrentCalls=25, maxWaitDuration=100ms'],
            ['TimeLimiter',
             'Đặt timeout tối đa cho mỗi call. Tránh thread bị block vô thời hạn.',
             'timeoutDuration=3s cho Feign calls'],
            ['Rate Limiter',
             'Giới hạn số request trong khoảng thời gian nhất định.',
             'Chưa implement (hướng phát triển)'],
            ['Fallback',
             'Trả về response thay thế khi service không khả dụng: empty list, cached value, error message.',
             'ResilientUserServiceClient trả về null/empty, ResilientCommunicationClient trả về no-op'],
        ],
        [3, 7, 5]
    )

    heading('3.11.4 Cascading Failure và cách phòng tránh', level=3)
    body(
        'Cascading Failure (lỗi lan truyền) là hiện tượng: Service A chậm → '
        'Service B gọi A bị block → Thread pool của B cạn kiệt → '
        'Service C gọi B bị block → ... → Toàn bộ hệ thống sập.'
    )
    body(
        'Circuit Breaker phòng tránh bằng cách fail fast — khi A không ổn, '
        'B ngay lập tức trả về fallback thay vì đợi timeout A. '
        'Kết hợp với Bulkhead (giới hạn concurrent calls), '
        'lỗi ở một service được cô lập, không lan sang service khác.'
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.12 REDIS LUA ATOMIC COUNTER
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.12 Atomic Counter với Redis Lua Script', level=2)

    heading('3.12.1 Vấn đề Race Condition với Counter', level=3)
    body(
        'Race condition với counter: Khi nhiều requests đồng thời tăng counter '
        '(like count, comment count), cần đảm bảo không có lost update.'
    )
    code_block(
        'Vấn đề với read-modify-write:\n'
        '  Thread A: read count=5, count+1=6, write 6\n'
        '  Thread B: read count=5, count+1=6, write 6  ← lost update!\n'
        '  Kết quả: count=6 (đúng phải là 7)'
    )

    heading('3.12.2 Redis Lua Script', level=3)
    body(
        'Redis đảm bảo mọi lệnh được thực thi atomically — single-threaded event loop. '
        'Lua script chạy trên Redis server được đảm bảo thực thi atomically: '
        'không script nào khác được chạy trong khi script đang thực thi.'
    )
    code_block(
        '-- Lua script: atomic increment với kiểm tra giới hạn\n'
        'local current = redis.call("GET", KEYS[1])\n'
        'if current == false then\n'
        '    redis.call("SET", KEYS[1], ARGV[1])\n'
        '    return tonumber(ARGV[1])\n'
        'end\n'
        'local newVal = tonumber(current) + tonumber(ARGV[1])\n'
        'redis.call("SET", KEYS[1], newVal)\n'
        'return newVal\n\n'
        '-- Gọi từ Java:\n'
        '-- redisTemplate.execute(script, keys, delta)'
    )
    body('Ưu điểm so với Distributed Lock cho counter:')
    bullet('Không cần acquire/release lock — giảm latency và complexity.')
    bullet('Atomic bản thân Redis operation — không thể có race condition.')
    bullet('Throughput cao hơn — lock-free approach cho phép nhiều counter update đồng thời (mỗi key là độc lập).')
    bullet('Tự động giải quyết khi Redis node restart (nếu kết hợp với persistence).')

    # ──────────────────────────────────────────────────────────────────────────
    # 3.13 MONGODB
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.13 Document Database — MongoDB', level=2)

    heading('3.13.1 Định nghĩa', level=3)
    body(
        'MongoDB là hệ quản trị cơ sở dữ liệu NoSQL hướng tài liệu (document-oriented), '
        'lưu trữ dữ liệu dưới dạng BSON (Binary JSON). Mỗi document là một JSON object '
        'không cần tuân theo schema cố định — các document trong cùng collection có thể '
        'có cấu trúc khác nhau.'
    )
    add_table(
        ['Khái niệm MongoDB', 'Tương đương SQL'],
        [
            ['Database', 'Database'],
            ['Collection', 'Table'],
            ['Document', 'Row/Record'],
            ['Field', 'Column'],
            ['_id', 'Primary Key (auto ObjectId hoặc custom)'],
            ['Index', 'Index'],
            ['Embedded Document', 'Denormalized JOIN (related data in same document)'],
            ['$lookup aggregation', 'LEFT OUTER JOIN'],
        ],
        [7, 8]
    )

    heading('3.13.2 Tại sao MongoDB phù hợp cho Chat & Notifications?', level=3)
    body('Blur dùng MongoDB cho Communication Service vì:')
    bullet('Schema linh hoạt: Tin nhắn có thể có attachments (ảnh, video, file) hoặc không. Thay đổi cấu trúc message không cần migration.')
    bullet('Embedded documents: ParticipantInfo (userId, avatar, name) được nhúng trực tiếp vào ChatMessage — đọc 1 document thay vì JOIN bảng.')
    bullet('Horizontal scaling: MongoDB Sharding phân tán data across nhiều server dựa trên shard key.')
    bullet('TTL Index: MongoDB hỗ trợ TTL index tự động xóa document sau thời gian nhất định — phù hợp cho notifications cũ.')
    bullet('Array operators: $push, $pull, $addToSet cho phép thao tác trực tiếp trên mảng (e.g., readBy array trong ChatMessage).')

    heading('3.13.3 Lưu ý khi dùng MongoDB', level=3)
    bullet('Không có ACID transaction mặc định (có từ MongoDB 4.0, nhưng chậm hơn). Trong Blur: ChatMessage service dùng @Transactional nhưng multi-document transaction có overhead.')
    bullet('Denormalization (nhúng data): Tăng read performance nhưng cần update nhiều chỗ khi data thay đổi (e.g., user đổi avatar → tất cả ChatMessage của user đó không tự cập nhật).')
    bullet('Schema validation: MongoDB hỗ trợ JSON Schema validation nhưng không enforce nghiêm ngặt như SQL NOT NULL constraint.')

    # ──────────────────────────────────────────────────────────────────────────
    # 3.14 PHOBERT & NLP (đề cập nhẹ, đang phát triển)
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.14 PhoBERT & NLP cho Tiếng Việt (Đang phát triển)', level=2)

    heading('3.14.1 BERT và Transformer Architecture', level=3)
    body(
        'BERT (Bidirectional Encoder Representations from Transformers) là mô hình ngôn ngữ '
        'được Google giới thiệu năm 2018, dựa trên kiến trúc Transformer (Vaswani et al., 2017). '
        'BERT được pre-train trên lượng text khổng lồ với 2 tasks: '
        'Masked Language Model (MLM) và Next Sentence Prediction (NSP).'
    )
    body('Đặc điểm chính của BERT:')
    bullet('Bidirectional: Đọc văn bản theo cả hai hướng (trái-phải và phải-trái) đồng thời, hiểu context đầy đủ hơn so với mô hình một chiều như GPT.')
    bullet('Pre-training + Fine-tuning: Pre-train một lần trên corpus lớn, sau đó fine-tune cho task cụ thể với dataset nhỏ hơn nhiều.')
    bullet('Contextual embeddings: Từ "bank" trong "river bank" và "bank account" có vector representation khác nhau.')

    heading('3.14.2 PhoBERT', level=3)
    body(
        'PhoBERT là mô hình BERT được phát triển bởi VinAI Research (2020), '
        'pre-trained đặc biệt cho tiếng Việt trên corpus 20GB văn bản tiếng Việt. '
        'PhoBERT v2 (sử dụng trong Blur) được train với word-level tokenization '
        'thay vì syllable-level, cho kết quả tốt hơn trên nhiều downstream tasks.'
    )
    body('Thông số PhoBERT-base:')
    bullet('12 Transformer layers, 768 hidden dimensions, 12 attention heads.')
    bullet('110M parameters.')
    bullet('Accuracy ~85-90% cho sentiment analysis trên tiếng Việt.')
    bullet('Model size: ~400MB (PyTorch format).')

    heading('3.14.3 ONNX Runtime', level=3)
    body(
        'ONNX (Open Neural Network Exchange) là định dạng mở cho ML models, '
        'cho phép chuyển đổi model giữa các framework (PyTorch → ONNX → TensorFlow/CoreML...). '
        'ONNX Runtime là inference engine tối ưu cho ONNX models.'
    )
    body('Lợi ích khi dùng ONNX Runtime thay vì PyTorch trực tiếp:')
    bullet('5-10x faster cold start: ONNX Runtime load model nhanh hơn đáng kể.')
    bullet('Graph optimization: Fuse operations, eliminate redundant computations.')
    bullet('Quantization support: INT8 quantization giảm model size và tăng inference speed với ít mất mát accuracy.')
    bullet('Cross-platform: Chạy trên CPU, GPU, mobile mà không cần thay đổi code.')
    body(
        'Lưu ý: Phần kiểm duyệt bình luận (Model Service) đang trong giai đoạn phát triển. '
        'Pipeline Kafka (comment-moderation, moderation-results topics) đã được thiết kế, '
        'nhưng tích hợp thực tế vào luồng bình luận chưa được kích hoạt.',
        italic=True
    )

    # ──────────────────────────────────────────────────────────────────────────
    # 3.15 DOCKER & CONTAINERIZATION
    # ──────────────────────────────────────────────────────────────────────────
    heading('3.15 Containerization — Docker & Docker Compose', level=2)

    heading('3.15.1 Docker', level=3)
    body(
        'Docker là nền tảng containerization cho phép đóng gói ứng dụng cùng với '
        'tất cả dependencies (libraries, runtime, config) vào một container — '
        'đơn vị phần mềm tiêu chuẩn, chạy nhất quán trên mọi môi trường.'
    )
    add_table(
        ['Tiêu chí', 'Virtual Machine', 'Docker Container'],
        [
            ['Isolation', 'Full OS isolation', 'Process-level isolation (namespace, cgroup)'],
            ['Boot time', 'Phút', 'Giây (thậm chí milliseconds)'],
            ['Size', 'GBs (full OS)', 'MBs (chỉ app layer)'],
            ['Resource overhead', 'Cao (full OS)', 'Thấp (share kernel)'],
            ['Portability', 'Kém (OS-specific image)', 'Cao (run anywhere Docker is)'],
            ['Security isolation', 'Rất tốt', 'Tốt (nhưng kém VM)'],
        ],
        [4, 5, 6]
    )

    heading('3.15.2 Multi-Stage Build', level=3)
    body(
        'Multi-stage Docker build cho phép dùng nhiều FROM instructions trong một Dockerfile, '
        'mỗi stage có thể copy artifacts từ stage trước. Kết quả: image production nhỏ gọn, '
        'không chứa build tools.'
    )
    code_block(
        '# Stage 1: Build\n'
        'FROM maven:3.9-amazoncorretto-21 AS builder\n'
        'WORKDIR /app\n'
        'COPY pom.xml .\n'
        'RUN mvn dependency:go-offline  # cache dependencies\n'
        'COPY src ./src\n'
        'RUN mvn package -DskipTests\n\n'
        '# Stage 2: Runtime (chỉ JRE, không có Maven/JDK)\n'
        'FROM amazoncorretto:21-alpine\n'
        'WORKDIR /app\n'
        'COPY --from=builder /app/target/*.jar app.jar\n'
        'ENTRYPOINT ["java", "-jar", "app.jar"]\n'
        '# Image nhỏ hơn ~3-4x so với single-stage'
    )

    heading('3.15.3 Docker Compose', level=3)
    body(
        'Docker Compose là công cụ định nghĩa và chạy multi-container Docker applications. '
        'Toàn bộ hệ thống Blur (11 services + infrastructure) được định nghĩa trong '
        'một file docker-compose.yml, khởi động bằng 1 lệnh: docker compose up -d.'
    )
    body('Docker Compose xử lý:')
    bullet('Service dependencies: depends_on đảm bảo thứ tự khởi động (Kafka trước khi services).')
    bullet('Networking: Tất cả services trong cùng Docker network, giao tiếp qua service name.')
    bullet('Volume mounting: Persist data (Neo4j, MongoDB, Redis data directories).')
    bullet('Environment variables: Inject config như Keycloak URL, DB credentials.')
    bullet('Health checks: Kiểm tra service ready trước khi dependent service khởi động.')

    body(
        'Lưu ý: Docker Compose phù hợp cho development và small-scale production. '
        'Với production quy mô lớn, cần Kubernetes (K8s) để có auto-scaling, '
        'self-healing, rolling updates, và resource management tốt hơn.',
        italic=True
    )
