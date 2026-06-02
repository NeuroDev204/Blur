# -*- coding: utf-8 -*-
from docx import Document
from docx.shared import Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

doc = Document()

# ─── CÀI ĐẶT TRANG ────────────────────────────────────────────────────────────
section = doc.sections[0]
section.page_width    = Cm(21)
section.page_height   = Cm(29.7)
section.left_margin   = Cm(3)
section.right_margin  = Cm(2)
section.top_margin    = Cm(2.5)
section.bottom_margin = Cm(2.5)

# ─── HÀM TIỆN ÍCH ────────────────────────────────────────────────────────────
def _set_font(run, bold=False, italic=False, size=13, color=None, font="Times New Roman"):
    run.bold = bold
    run.italic = italic
    run.font.size = Pt(size)
    run.font.name = font
    rPr = run._r.get_or_add_rPr()
    rf = OxmlElement("w:rFonts")
    rf.set(qn("w:ascii"), font)
    rf.set(qn("w:hAnsi"), font)
    rPr.insert(0, rf)
    if color:
        run.font.color.rgb = RGBColor(*color)

def para(text="", align=WD_ALIGN_PARAGRAPH.JUSTIFY, bold=False, size=13,
         sb=0, sa=6, color=None, italic=False, indent=0):
    p = doc.add_paragraph()
    p.alignment = align
    p.paragraph_format.space_before = Pt(sb)
    p.paragraph_format.space_after  = Pt(sa)
    if indent:
        p.paragraph_format.left_indent = Cm(indent)
    if text:
        r = p.add_run(text)
        _set_font(r, bold=bold, size=size, color=color, italic=italic)
    return p

def heading(text, level=1):
    clr = {1:(0,70,127), 2:(0,112,192), 3:(31,73,125)}
    sz  = {1:16, 2:14, 3:13}
    sb  = {1:18, 2:12, 3:8}
    sa  = {1:10, 2:8,  3:6}
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(sb[level])
    p.paragraph_format.space_after  = Pt(sa[level])
    r = p.add_run(text)
    _set_font(r, bold=True, size=sz[level], color=clr[level])
    return p

def code(lines):
    for line in lines:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent  = Cm(1)
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after  = Pt(0)
        r = p.add_run(line)
        _set_font(r, size=10, font="Courier New", color=(31,73,125))

def table(headers, rows, widths=None):
    t = doc.add_table(rows=1+len(rows), cols=len(headers))
    t.style = "Table Grid"
    for i, h in enumerate(headers):
        c = t.rows[0].cells[i]
        c.text = ""
        r = c.paragraphs[0].add_run(h)
        _set_font(r, bold=True, size=11, color=(255,255,255))
        sh = OxmlElement("w:shd")
        sh.set(qn("w:fill"), "003366"); sh.set(qn("w:color"), "auto"); sh.set(qn("w:val"), "clear")
        c._tc.get_or_add_tcPr().append(sh)
    for ri, row in enumerate(rows):
        fill = "EBF3FF" if ri % 2 == 0 else "FFFFFF"
        for ci, val in enumerate(row):
            c = t.rows[ri+1].cells[ci]
            c.text = ""
            r = c.paragraphs[0].add_run(str(val))
            _set_font(r, size=11)
            sh = OxmlElement("w:shd")
            sh.set(qn("w:fill"), fill); sh.set(qn("w:color"), "auto"); sh.set(qn("w:val"), "clear")
            c._tc.get_or_add_tcPr().append(sh)
    if widths:
        for i, w in enumerate(widths):
            for row in t.rows:
                row.cells[i].width = Cm(w)
    return t

def bullet(text, level=1):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.left_indent  = Cm(level * 0.7)
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after  = Pt(2)
    r = p.add_run(text)
    _set_font(r, size=13)

def pb():
    doc.add_page_break()

# ══════════════════════════════════════════════════════════════════════════════
# TRANG BÌA
# ══════════════════════════════════════════════════════════════════════════════
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(40)
r = p.add_run("TRƯỜNG ĐẠI HỌC CÔNG NGHỆ THÔNG TIN")
_set_font(r, bold=True, size=14, color=(0,70,127))

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("KHOA CÔNG NGHỆ PHẦN MỀM")
_set_font(r, bold=True, size=13, color=(0,70,127))

doc.add_paragraph()
doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("BÁO CÁO ĐỒ ÁN MÔN HỌC")
_set_font(r, bold=True, size=18, color=(0,70,127))

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("THIẾT KẾ VÀ XÂY DỰNG ỨNG DỤNG MẠNG XÃ HỘI PHÂN TÁN")
_set_font(r, bold=True, size=15, color=(0,112,192))

doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("DỰ ÁN: BLUR SOCIAL NETWORK")
_set_font(r, bold=True, size=22, color=(0,70,127))

doc.add_paragraph()
doc.add_paragraph()
doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("TP. HỒ CHÍ MINH – NĂM 2025")
_set_font(r, bold=True, size=13, color=(100,100,100))

pb()

# ══════════════════════════════════════════════════════════════════════════════
# MỤC LỤC
# ══════════════════════════════════════════════════════════════════════════════
heading("MỤC LỤC", 1)
toc = [
    ("1.", "GIỚI THIỆU DỰ ÁN", False),
    ("1.1.", "Tổng quan", True),
    ("1.2.", "Mục tiêu và phạm vi", True),
    ("1.3.", "Kiến trúc tổng thể", True),
    ("1.4.", "Công nghệ sử dụng", True),
    ("2.", "CƠ SỞ LÝ THUYẾT", False),
    ("2.1.", "Kiến trúc Microservices", True),
    ("2.2.", "Event-Driven Architecture & Apache Kafka", True),
    ("2.3.", "Xử lý ngôn ngữ tự nhiên – PhoBERT", True),
    ("2.4.", "Hệ thống cache đa tầng", True),
    ("2.5.", "Xác thực và phân quyền với Keycloak", True),
    ("2.6.", "Resilience4j – Khả năng chịu lỗi", True),
    ("2.7.", "Design Patterns trong hệ thống phân tán", True),
    ("2.8.", "Cơ sở dữ liệu đồ thị Neo4j", True),
    ("3.", "LỌC BÌNH LUẬN BẰNG AI (PHÁT HIỆN NỘI DUNG ĐỘC HẠI)", False),
    ("4.", "KIẾN TRÚC KAFKA & EVENT-DRIVEN ARCHITECTURE", False),
    ("5.", "HỆ THỐNG CACHE ĐA TẦNG (MULTI-LEVEL CACHE)", False),
    ("6.", "XÁC THỰC VÀ PHÂN QUYỀN VỚI KEYCLOAK", False),
    ("7.", "KHẢ NĂNG CHỊU LỖI VỚI RESILIENCE4J", False),
    ("8.", "DESIGN PATTERNS ÁP DỤNG", False),
    ("9.", "HỆ THỐNG GỢI Ý FOLLOW (RECOMMENDATION SYSTEM)", False),
    ("10.", "KẾT QUẢ THỰC TẾ ĐẠT ĐƯỢC", False),
    ("11.", "KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN", False),
]
for num, title, is_sub in toc:
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(1)
    p.paragraph_format.space_after  = Pt(1)
    p.paragraph_format.left_indent  = Cm(0.8 if is_sub else 0)
    r = p.add_run(f"{num}    {title}")
    _set_font(r, bold=(not is_sub), size=12)

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 1 – GIỚI THIỆU
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 1: GIỚI THIỆU DỰ ÁN", 1)

heading("1.1. Tổng quan", 2)
para(
    "Blur Social Network là một nền tảng mạng xã hội thuần Việt được thiết kế và xây dựng theo kiến trúc "
    "microservices hiện đại, hướng đến việc cung cấp trải nghiệm tương tác phong phú với đầy đủ các tính năng "
    "như chia sẻ bài viết, bình luận, nhắn tin thời gian thực, gọi video/audio, cùng hệ thống kiểm duyệt nội "
    "dung tự động bằng trí tuệ nhân tạo. Dự án được xây dựng bằng Java 21 (Spring Boot 3.4) cho backend, "
    "React 18 + TypeScript cho frontend, và Python 3.11 (FastAPI) cho dịch vụ AI."
)
para(
    "Điểm nổi bật của hệ thống là sự kết hợp giữa nhiều công nghệ tiên tiến: Apache Kafka cho luồng sự kiện "
    "bất đồng bộ, Redis + Caffeine cho cache đa tầng, Keycloak cho xác thực OAuth2/OIDC, Resilience4j cho "
    "khả năng chịu lỗi, Neo4j cho đồ thị quan hệ xã hội, và mô hình PhoBERT v2 cho phát hiện ngôn ngữ độc hại "
    "tiếng Việt."
)

heading("1.2. Mục tiêu và phạm vi", 2)
para("Dự án hướng đến các mục tiêu chính sau:")
bullet("Xây dựng nền tảng mạng xã hội đầy đủ tính năng với khả năng mở rộng cao")
bullet("Áp dụng kiến trúc microservices để tách biệt các nghiệp vụ và triển khai độc lập")
bullet("Tự động kiểm duyệt bình luận tiếng Việt bằng AI (PhoBERT) theo pipeline bất đồng bộ")
bullet("Đảm bảo hiệu năng cao thông qua hệ thống cache đa tầng (Caffeine L1 + Redis L2)")
bullet("Bảo mật toàn diện với Keycloak OAuth2/OIDC và mã hóa thông tin cá nhân (PII)")
bullet("Gợi ý kết bạn thông minh dựa trên đồ thị xã hội Neo4j")
bullet("Đảm bảo độ sẵn sàng cao với Resilience4j (Circuit Breaker, Retry, Bulkhead)")
bullet("Giao tiếp thời gian thực qua Socket.IO, WebSocket/STOMP và WebRTC")

heading("1.3. Kiến trúc tổng thể", 2)
para(
    "Hệ thống gồm 6 microservice độc lập, giao tiếp qua API Gateway (cổng 8888) và Apache Kafka. "
    "Mỗi service có trách nhiệm rõ ràng và database riêng (Database per Service pattern):"
)
table(
    ["Service", "Cổng", "Ngôn ngữ", "Vai trò", "Database"],
    [
        ["API Gateway",     "8888", "Java/Spring", "Định tuyến, bảo mật, Circuit Breaker", "–"],
        ["User Service",    "8081", "Java/Spring", "Hồ sơ người dùng, đồ thị xã hội, gợi ý", "Neo4j"],
        ["Content Service", "8082", "Java/Spring", "Bài viết, bình luận, feed, CQRS, cache", "Neo4j + MongoDB"],
        ["Communication",   "8083", "Java/Spring", "Chat, thông báo, gọi video, AI chatbot", "MongoDB"],
        ["Model Service",   "8000", "Python/FastAPI", "Phát hiện nội dung độc hại (PhoBERT/ONNX)", "–"],
        ["Keycloak",        "8080", "Java", "Identity Provider, OAuth2/OIDC, JWT", "MySQL"],
    ],
    widths=[3.2, 1.5, 2.5, 5.5, 3.0]
)
doc.add_paragraph()

heading("1.4. Công nghệ sử dụng", 2)
table(
    ["Nhóm công nghệ", "Công nghệ / Phiên bản"],
    [
        ["Backend Framework", "Spring Boot 3.4.3, Spring Cloud 2024.0.0, Spring Security, WebFlux"],
        ["AI/ML", "Python 3.11, FastAPI, PyTorch, PhoBERT v2 (VinAI), ONNX Runtime, Google Gemini API"],
        ["Cơ sở dữ liệu", "Neo4j (đồ thị), MongoDB 6.0 (tài liệu), MySQL 8.0 (Keycloak), Redis (cache)"],
        ["Message Broker", "Apache Kafka 7.7.1 (KRaft – không cần ZooKeeper)"],
        ["Cache", "Caffeine (L1, in-process), Redis (L2, distributed), Redisson (distributed lock)"],
        ["Bảo mật", "Keycloak 26.1, OAuth2/OIDC, JWT RS256, AES-256 mã hoá PII"],
        ["Resilience", "Resilience4j 2.2.0 (CircuitBreaker, Retry, Bulkhead, TimeLimiter)"],
        ["Giao tiếp thực tế", "Socket.IO, STOMP/SockJS, WebRTC (gọi video/audio)"],
        ["Frontend", "React 18, TypeScript 5, Vite 5, Redux Toolkit, Chakra UI, Tailwind CSS"],
        ["DevOps", "Docker, Docker Compose, Nginx, Multi-stage builds"],
    ],
    widths=[4.5, 12]
)

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 2 – CƠ SỞ LÝ THUYẾT
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 2: CƠ SỞ LÝ THUYẾT", 1)

heading("2.1. Kiến trúc Microservices", 2)
para(
    "Kiến trúc microservices phân tách ứng dụng thành các dịch vụ nhỏ, độc lập, mỗi dịch vụ đảm nhận "
    "một nghiệp vụ cụ thể và có thể triển khai, mở rộng độc lập. So với kiến trúc monolithic, "
    "microservices mang lại khả năng mở rộng theo chiều ngang (horizontal scaling), khả năng triển "
    "khai liên tục (CI/CD), và tính chịu lỗi tốt hơn – khi một service gặp sự cố, phần còn lại vẫn "
    "hoạt động bình thường."
)
para(
    "Các nguyên tắc chính được áp dụng trong Blur: Database per Service (mỗi service có database riêng), "
    "API Gateway pattern (một điểm vào duy nhất), và Domain-Driven Design (DDD) cho ranh giới dịch vụ."
)

heading("2.2. Event-Driven Architecture & Apache Kafka", 2)
para(
    "Apache Kafka là một nền tảng luồng sự kiện phân tán (distributed event streaming platform) với khả "
    "năng xử lý hàng triệu sự kiện mỗi giây với độ trễ thấp. Kafka sử dụng mô hình publish-subscribe, "
    "trong đó Producer ghi message vào Topic, Consumer đọc theo offset. Kafka đảm bảo tính bền vững "
    "(durability) và khả năng tái xử lý (replay) thông qua lưu trữ log phân tán."
)
para(
    "Trong Blur, Kafka là xương sống của kiến trúc event-driven: truyền tải sự kiện kiểm duyệt bình luận "
    "bất đồng bộ, cập nhật feed theo mô hình CQRS, phân phối thông báo real-time, và điều phối Saga "
    "pattern cho giao dịch phân tán. Chế độ KRaft loại bỏ sự phụ thuộc vào ZooKeeper, đơn giản hoá "
    "vận hành và tăng hiệu năng."
)

heading("2.3. Xử lý ngôn ngữ tự nhiên – PhoBERT", 2)
para(
    "PhoBERT (Pho Bidirectional Encoder Representations from Transformers) là mô hình ngôn ngữ tiền "
    "huấn luyện lớn cho tiếng Việt, được phát triển bởi VinAI Research. Dựa trên kiến trúc BERT của "
    "Google, PhoBERT được huấn luyện trên tập ngữ liệu tiếng Việt 20 GB từ Wikipedia và báo điện tử. "
    "Phiên bản v2 cải thiện đáng kể khả năng hiểu ngữ cảnh, thành ngữ và từ lóng tiếng Việt – đặc biệt "
    "quan trọng cho bài toán phát hiện ngôn ngữ độc hại trên mạng xã hội."
)
para(
    "Mô hình được fine-tune trên tập dữ liệu tổng hợp từ YouTube, TikTok và ViHSD (Vietnamese Hate "
    "Speech Detection dataset), đạt F1-score = 75,95%, Accuracy = 93,87% và AUC-ROC = 95,42% trên tập "
    "kiểm tra thực tế. ONNX Runtime tối ưu hoá thời gian khởi động từ 60–90 giây xuống còn 5–15 giây."
)

heading("2.4. Hệ thống cache đa tầng", 2)
para(
    "Cache đa tầng (Multi-level Cache) kết hợp cache in-process (L1) và cache phân tán (L2) để tối ưu "
    "hoá cả tốc độ truy cập lẫn tính nhất quán dữ liệu. L1 (Caffeine) là cache bộ nhớ trong JVM với "
    "độ trễ nano-giây, trong khi L2 (Redis) là cache phân tán với độ trễ micro-giây nhưng chia sẻ "
    "giữa nhiều instance. Cơ chế invalidation qua Redis Pub/Sub đảm bảo tính nhất quán khi dữ liệu "
    "thay đổi trên bất kỳ node nào."
)
para(
    "Cache stampede (hiệu ứng đàn giải phóng – nhiều request đồng thời miss cache và cùng truy cập "
    "database) được ngăn chặn bằng Distributed Lock (Redisson) với double-check pattern. Atomic "
    "counter bằng Lua script đảm bảo đếm lượt thích, lượt xem chính xác trong môi trường concurrent."
)

heading("2.5. Xác thực và phân quyền với Keycloak", 2)
para(
    "Keycloak là một giải pháp Identity and Access Management (IAM) mã nguồn mở hỗ trợ đầy đủ chuẩn "
    "OAuth 2.0 và OpenID Connect (OIDC). Với vai trò là Identity Provider (IdP) trung tâm, Keycloak "
    "quản lý toàn bộ vòng đời người dùng: đăng ký, xác thực, phân quyền và liên kết với providers "
    "bên ngoài (Google). JWT được phát hành với thuật toán RS256, cho phép các service xác thực token "
    "cục bộ mà không cần gọi về Keycloak mỗi request."
)

heading("2.6. Resilience4j – Khả năng chịu lỗi", 2)
para(
    "Resilience4j là thư viện Java lightweight cung cấp các pattern chịu lỗi: Circuit Breaker ngăn "
    "cascade failure khi downstream service gặp sự cố; Retry tự động thử lại với exponential backoff; "
    "Bulkhead giới hạn concurrent calls để tránh resource exhaustion; TimeLimiter đặt timeout cho "
    "async calls. So với Hystrix (đã deprecated), Resilience4j có footprint nhỏ hơn và tích hợp tốt "
    "hơn với reactive programming."
)

heading("2.7. Design Patterns trong hệ thống phân tán", 2)
para(
    "Repository Pattern trừu tượng hoá lớp truy cập dữ liệu; CQRS (Command Query Responsibility "
    "Segregation) tách biệt luồng đọc/ghi để tối ưu từng hướng; Outbox Pattern đảm bảo atomicity "
    "giữa database write và Kafka publish; Saga Pattern điều phối giao dịch phân tán qua "
    "choreography; Observer Pattern qua Redis Pub/Sub cho cache invalidation; Strategy Pattern cho "
    "đa chiến lược gợi ý người dùng."
)

heading("2.8. Cơ sở dữ liệu đồ thị Neo4j", 2)
para(
    "Neo4j là cơ sở dữ liệu đồ thị (graph database) lưu trữ dữ liệu dưới dạng nodes và "
    "relationships, phù hợp tự nhiên cho bài toán mạng xã hội. Cypher query language cho phép "
    "traversal đồ thị hiệu quả với các truy vấn như 'bạn của bạn' (2-hop), tìm người quen chung, "
    "hay lan truyền thông tin. So với RDBMS, Neo4j xử lý deep-link queries nhanh hơn nhiều bậc "
    "nhờ index-free adjacency."
)

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 3 – LỌC BÌNH LUẬN BẰNG AI
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 3: LỌC BÌNH LUẬN BẰNG AI (PHÁT HIỆN NỘI DUNG ĐỘC HẠI)", 1)

heading("3.1. Kiến trúc hệ thống kiểm duyệt", 2)
para(
    "Hệ thống kiểm duyệt bình luận hoạt động hoàn toàn bất đồng bộ thông qua Apache Kafka, đảm bảo "
    "không ảnh hưởng đến luồng đăng bình luận của người dùng. Bình luận được lưu ngay lập tức với "
    "trạng thái PENDING và hiển thị cho người dùng trong khi hệ thống AI xử lý ngầm. Pipeline gồm "
    "5 bước chính:"
)
bullet("Người dùng đăng bình luận → Content Service lưu ngay với trạng thái PENDING")
bullet("Content Service publish message vào Kafka topic comment-moderation-request")
bullet("Model Service (Python/FastAPI) tiêu thụ message, tokenize và chạy inference PhoBERT")
bullet("Model Service publish kết quả vào Kafka topic comment-moderation-results")
bullet("Content Service cập nhật trạng thái bình luận, xoá cache và thông báo qua WebSocket")

heading("3.2. Định nghĩa nội dung độc hại (Toxic Content)", 2)
para(
    "Trước khi xây dựng bất kỳ mô hình nào, cần xác định rõ: nội dung nào được coi là 'độc hại' "
    "(toxic) trong bối cảnh mạng xã hội Blur? Hệ thống không tự định nghĩa tiêu chuẩn riêng mà "
    "dựa hoàn toàn vào định nghĩa từ ViHSD – tập dữ liệu được gán nhãn thủ công bởi chuyên gia ngôn "
    "ngữ học người Việt, được cộng đồng nghiên cứu NLP tiếng Việt công nhận rộng rãi."
)
para("ViHSD định nghĩa nội dung độc hại theo hai mức độ:", bold=True)
table(
    ["Loại", "Định nghĩa chi tiết", "Ví dụ thực tế từ ViHSD"],
    [
        ["OFFENSIVE\n(Xúc phạm)",
         "Ngôn từ thô tục, xúc phạm nhắm vào cá nhân hoặc nhóm người. "
         "Bao gồm: chửi thề, so sánh người với vật theo hướng miệt thị, "
         "nhận xét tiêu cực về ngoại hình/trí tuệ/hành vi theo cách xúc phạm.",
         "\"Lúp lúp như chó.\"\n\"Ý thức còn ít hơn cả số tiền trong túi t\""],
        ["HATE\n(Thù hận)",
         "Ngôn từ kích động thù hận, phân biệt đối xử dựa trên vùng miền, "
         "dân tộc, giới tính, tôn giáo. Kêu gọi bạo lực hoặc tẩy chay nhóm người.",
         "\"CÔN ĐỒ CỤC SÚC VÔ NHÂN TÍNH...\"\n\"mấy thằng bắc kì...\""],
    ],
    widths=[2.5, 8, 6]
)
doc.add_paragraph()
para(
    "Quy tắc nhị phân hoá (binaryization rule): "
    "label_id thuộc {1, 2} (OFFENSIVE hoặc HATE) → TOXIC = 1; "
    "label_id = 0 (CLEAN) → TOXIC = 0. "
    "Cả hai mức độ đều được nhóm chung thành TOXIC bởi vì với mục tiêu bảo vệ người dùng, "
    "cả xúc phạm cá nhân lẫn kích động thù hận đều gây tác hại tương đương và cần bị chặn lọc."
)
para("Phân loại cụ thể theo từng trường hợp điển hình:", bold=True)
table(
    ["Bình luận ví dụ", "Phân loại", "Lý do"],
    [
        ["\"Em được làm fan cứng luôn rồi nè\"",   "CLEAN",     "Ủng hộ, không xúc phạm"],
        ["\"Từ lý thuyết đến thực hành...\"",       "CLEAN",     "Chia sẻ kiến thức bình thường"],
        ["\"con chó dễ thương quá\"",               "CLEAN",     "Nói về con vật, ngữ cảnh tích cực"],
        ["\"Lúp lúp như chó.\"",                   "OFFENSIVE", "So sánh người với chó – miệt thị"],
        ["\"CÔN ĐỒ CỤC SÚC VÔ NHÂN TÍNH\"",       "HATE",      "Ngôn từ cực đoan, kêu gọi thù hận"],
        ["\"mấy thằng bắc kì...\"",                "HATE",      "Phân biệt đối xử theo vùng miền"],
    ],
    widths=[5.5, 2.5, 8.5]
)
doc.add_paragraph()
para("Thống kê phân bố nhãn trong toàn bộ dataset ViHSD (34.876 mẫu):")
table(
    ["Tập dữ liệu", "Tổng mẫu", "CLEAN", "OFFENSIVE + HATE (TOXIC)", "Tỷ lệ Toxic"],
    [
        ["train.csv",   "24.048", "19.886 (82,7%)", "4.162 (17,3%)",  "17,3%"],
        ["dev.csv",     "2.672",  "2.190 (82,0%)",  "482 (18,0%)",    "18,0%"],
        ["test.csv",    "6.680",  "5.548 (83,1%)",  "1.132 (16,9%)",  "16,9%"],
        ["test_df.csv", "1.476",  "600 (40,7%)",    "876 (59,3%)",    "59,3%"],
        ["Tổng cộng",   "34.876", "28.224 (80,9%)", "6.652 (19,1%)",  "19,1%"],
    ],
    widths=[2.5, 2, 4, 4.5, 2.5]
)
doc.add_paragraph()
para(
    "Nhận xét: Dataset ViHSD bị mất cân bằng – toxic chỉ chiếm ~19% trên hầu hết các tập. "
    "Nếu không xử lý, model sẽ thiên về dự đoán CLEAN để đạt accuracy cao (~80%) mà không thực "
    "sự học được pattern toxic. Giải pháp: balanced class weights và ViHSD ×5 boost (mục 3.7)."
)

heading("3.3. Tập dữ liệu ViHSD – Cấu trúc và tích hợp", 2)
para(
    "ViHSD (Vietnamese Hate Speech Detection) là dataset được gán nhãn thủ công, bao gồm bình "
    "luận thu thập từ mạng xã hội Việt Nam. Cấu trúc 4 file CSV:"
)
table(
    ["File", "Cột text", "Cột nhãn", "Mục đích gốc"],
    [
        ["train.csv",   "free_text", "label_id", "Tập huấn luyện ViHSD"],
        ["dev.csv",     "free_text", "label_id", "Tập validation ViHSD"],
        ["test.csv",    "free_text", "label_id", "Tập kiểm tra ViHSD"],
        ["test_df.csv", "cmt_col",   "labels",   "Test set bổ sung (cấu trúc khác cột)"],
    ],
    widths=[3, 3, 3, 7.5]
)
doc.add_paragraph()
para(
    "Ngoài ViHSD, hệ thống tích hợp dữ liệu scraper từ YouTube và TikTok. "
    "Dữ liệu scraper được gán nhãn tự động bằng script label_with_vihsd.py (mục 3.5). "
    "Độ ưu tiên file khi load: *_vihsd_labeled.json > *_relabeled.json > file gốc."
)

heading("3.4. Kiến trúc mô hình PhoBERT", 2)
para(
    "Mô hình nền là PhoBERT-base (VinAI, 2020) – phiên bản BERT dành riêng cho tiếng Việt, "
    "tiền huấn luyện trên 20 GB văn bản tiếng Việt. PhoBERT sử dụng kiến trúc RoBERTa "
    "(Robustly Optimized BERT Pretraining Approach), cải tiến so với BERT gốc với dynamic masking "
    "và loại bỏ NSP task. Toàn bộ cấu hình được lưu trong config.json của mô hình:"
)
table(
    ["Thành phần kiến trúc", "Thông số", "Ý nghĩa"],
    [
        ["Kiến trúc",            "RobertaForSequenceClassification", "Encoder RoBERTa + classification head 2 lớp"],
        ["Kích thước ẩn",        "hidden_size = 768",                "Mỗi token biểu diễn bằng vector 768 chiều"],
        ["Số transformer layer", "num_hidden_layers = 12",           "Mỗi lớp: Multi-head Self-Attention + FFN"],
        ["Attention heads",      "num_attention_heads = 12",         "Multi-head attention, 64 chiều/đầu (768/12)"],
        ["FFN trung gian",       "intermediate_size = 3072",         "Feed-forward: 768 → 3072 → 768 (hệ số 4×)"],
        ["Vocabulary",           "vocab_size = 64.001 token",        "BPE subword vocabulary tiếng Việt (VinAI)"],
        ["Max sequence length",  "max_position_embeddings = 258",    "Tối đa 256 token nội dung + 2 token đặc biệt"],
        ["Activation function",  "hidden_act = gelu",                "GELU – phi tuyến mượt hơn ReLU"],
        ["Dropout",              "0.1 (attention + hidden)",         "Regularization tránh overfitting khi fine-tune"],
        ["Tham số tổng",         "~135 triệu parameters",            "Kích thước điển hình BERT-base"],
    ],
    widths=[4, 4.5, 7.5]
)
doc.add_paragraph()
para(
    "Quy trình fine-tuning: PhoBERT-base pretrained weights được tải từ vinai/phobert-base. "
    "Classification head được thêm vào trên [CLS] token vector qua Linear(768 → 2) + Softmax "
    "để tạo phân phối xác suất [P_clean, P_toxic]. Toàn bộ 135M parameters được cập nhật "
    "trong quá trình fine-tuning (full fine-tuning, không freeze layer nào)."
)
code([
    "# Luồng dữ liệu qua mô hình",
    "Text input",
    "  → PhoBERT tokenizer (BPE, max_length=256)",
    "  → Token IDs + Attention Mask",
    "  → RoBERTa Encoder (12 transformer layers, hidden=768)",
    "  → [CLS] hidden state (vector 768 chiều)",
    "  → Linear(768 → 2)  [classification head]",
    "  → Softmax  →  [P_clean, P_toxic]",
    "  → P_toxic = 'raw toxic score' (0.0 – 1.0)",
])
doc.add_paragraph()

heading("3.5. Pipeline xây dựng tập dữ liệu huấn luyện", 2)
para(
    "Dữ liệu scraper (YouTube, TikTok) không có nhãn sẵn, cần gán nhãn tự động. "
    "Script label_with_vihsd.py giải quyết theo 2 bước:"
)
bullet("Bước 1 – Train mini labeler: Fine-tune PhoBERT trên toàn bộ ViHSD (train + dev + test + test_df). "
       "Labeler học định nghĩa toxic theo chuẩn human-annotated của ViHSD.")
bullet("Bước 2 – Gán nhãn JSON scraper: Dùng labeler để predict nhãn bình luận YouTube/TikTok. "
       "Chỉ giữ sample có confidence >= 0.75 (max(P_clean, P_toxic)), loại bỏ sample model không chắc. "
       "Kết quả lưu ra *_vihsd_labeled.json.")
code([
    "# label_with_vihsd.py – Gán nhãn dựa trên ViHSD labeler",
    "for text, probs in zip(texts, all_probs):",
    "    clean_prob, toxic_prob = probs[0], probs[1]",
    "    confidence_score = max(clean_prob, toxic_prob)",
    "",
    "    if confidence_score < 0.75:    # Ngưỡng confidence",
    "        stats['skipped'] += 1      # Loại bỏ – model không chắc chắn",
    "        continue",
    "",
    "    label = 1 if toxic_prob > clean_prob else 0  # Nhãn binary",
    "    labeled.append({'text': text, 'label': label,",
    "                    'toxic_score': round(toxic_prob, 4),",
    "                    'confidence': round(confidence_score, 4)})",
])
doc.add_paragraph()

heading("3.6. Chiến lược huấn luyện – Merge All & ViHSD Boost", 2)
para(
    "Script train_model_v2.py áp dụng chiến lược MERGE ALL → DEDUPLICATE → SPLIT → TRAIN ONCE. "
    "Chiến lược này khắc phục lỗi catastrophic forgetting của phiên bản cũ (train_model.py dùng "
    "sequential multi-stage training, dẫn đến F1 = 0.178). Quy trình gồm 5 bước:"
)
bullet("Bước 1 – Load: ViHSD CSV được load TRƯỚC JSON để ưu tiên trong dedup. "
       "Thứ tự ưu tiên: *_vihsd_labeled.json > *_relabeled.json > file gốc.")
bullet("Bước 2 – Dedup: Loại bỏ duplicate theo text.strip().lower(), nhãn ViHSD thắng khi trùng.")
bullet("Bước 3 – Stratified split: 90% train / 5% validation / 5% test, giữ tỷ lệ lớp ở mỗi tập.")
bullet("Bước 3.5 – ViHSD Boost: Nhân 5 lần các sample ViHSD trong train set (sau split, tránh dedup), "
       "chỉ trong train set, xáo trộn ngẫu nhiên (seed=42). "
       "Mục đích: tăng ảnh hưởng human-annotated data lên model.")
bullet("Bước 4 – Train once: CrossEntropyLoss + balanced class weights, 5 epochs, early stopping patience=2.")
code([
    "# train_model_v2.py – ViHSD Boost (sau split)",
    "def boost_vihsd_samples(train_split, vihsd_text_keys, repeat=5):",
    "    extra_texts, extra_labels = [], []",
    "    for text, label in zip(train_split['texts'], train_split['labels']):",
    "        if text.strip().lower() in vihsd_text_keys:   # Chỉ boost ViHSD",
    "            for _ in range(repeat - 1):               # -1 vì bản gốc đã có",
    "                extra_texts.append(text)",
    "                extra_labels.append(label)",
    "    combined = list(zip(all_texts + extra_texts, all_labels + extra_labels))",
    "    random.seed(42)",
    "    random.shuffle(combined)",
    "    return {'texts': ..., 'labels': ...}",
])
doc.add_paragraph()

heading("3.7. Cấu hình huấn luyện và hàm mất mát", 2)
table(
    ["Tham số", "Giá trị", "Lý do chọn"],
    [
        ["Model nền",        "vinai/phobert-base",              "PhoBERT chuyên tiếng Việt, 135M params"],
        ["Max token length", "256",                             "Đủ cho hầu hết bình luận; cân bằng tốc độ/accuracy"],
        ["Epochs",           "5 (early stop patience=2)",       "Dừng sớm nếu val F1 không cải thiện 2 epoch liên tiếp"],
        ["Batch size",       "16 (auto-scale theo VRAM)",       "Tự động tăng/giảm theo dung lượng GPU"],
        ["Learning rate",    "2e-5",                            "Chuẩn cho fine-tuning BERT, tránh quên kiến thức cũ"],
        ["Warmup ratio",     "0.1 (10% tổng số steps)",         "Tránh learning rate quá cao ở đầu huấn luyện"],
        ["Weight decay",     "0.01",                            "L2 regularization chống overfitting"],
        ["Max grad norm",    "1.0",                             "Gradient clipping tránh exploding gradient"],
        ["Loss function",    "CrossEntropyLoss + balanced weights", "Xử lý class imbalance: toxic ~19% < clean ~81%"],
        ["Metric chọn model","F1 (toxic class)",                "Ưu tiên recall – bắt toxic quan trọng hơn tránh FP"],
    ],
    widths=[3.5, 4.5, 8.5]
)
doc.add_paragraph()
para(
    "Balanced class weights: weight_clean = N / (2 × n_clean), weight_toxic = N / (2 × n_toxic). "
    "Khi toxic chiếm ít hơn, weight_toxic cao hơn, buộc model chú ý nhiều hơn vào ví dụ toxic."
)

heading("3.8. Tự động tìm ngưỡng tối ưu (Threshold Tuning)", 2)
para(
    "Sau khi train xong, thay vì dùng cố định ngưỡng 0.5, hệ thống quét khoảng [0.20, 0.81] "
    "bước 0.01 trên validation set để tìm ngưỡng tối đa hoá F1 của lớp TOXIC:"
)
code([
    "# train_model_v2.py – Auto-tune threshold",
    "def find_best_threshold(trainer, val_dataset):",
    "    val_probs = softmax(trainer.predict(val_dataset).predictions)[:, 1]",
    "    best_thresh, best_f1 = 0.5, 0.0",
    "    for t in np.arange(0.20, 0.81, 0.01):    # Quét 61 ngưỡng",
    "        pred = (val_probs >= t).astype(int)",
    "        _, _, f1, _ = precision_recall_fscore_support(",
    "            val_true, pred, average='binary', zero_division=0",
    "        )",
    "        if f1 > best_f1:",
    "            best_f1, best_thresh = f1, float(t)",
    "    return best_thresh  # Ngưỡng tốt nhất theo val F1",
])
doc.add_paragraph()
para(
    "Kết quả: Ngưỡng tối ưu tìm được là 0.26 (lưu trong evaluation_metrics.json). "
    "Thấp hơn ngưỡng mặc định 0.5 do dataset imbalanced – model có xu hướng cho P_toxic "
    "thấp hơn thực tế, cần ngưỡng thấp hơn để bắt đủ toxic content."
)

heading("3.9. Kết quả đánh giá mô hình", 2)
para(
    "Mô hình huấn luyện trong 3 epoch trước khi early stopping dừng (patience=2, epoch 1 tốt nhất). "
    "Kết quả theo từng epoch trên validation set:"
)
table(
    ["Epoch", "Train Loss", "Accuracy", "Precision", "Recall", "F1 (Toxic)", "Ghi chú"],
    [
        ["1", "0.4586", "93,80%", "76,10%", "74,86%", "75,48%", "BEST MODEL – step 10.297"],
        ["2", "0.9859", "93,11%", "–",      "–",      "71,43%", "F1 giảm 4,05% vs epoch 1"],
        ["3", "1.0945", "93,39%", "–",      "–",      "71,76%", "Vẫn thấp hơn epoch 1 → dừng"],
    ],
    widths=[1.5, 2.5, 2.5, 2.5, 2, 3, 6.5]
)
doc.add_paragraph()
para(
    "Nhận xét: Model hội tụ nhanh tại epoch 1. Từ epoch 2, loss tăng vọt (0.46 → 0.99) và F1 "
    "giảm rõ – dấu hiệu overfitting. Early stopping chọn đúng checkpoint tại step 10.297."
)
para("Kết quả cuối cùng trên test set (2.903 mẫu, ngưỡng = 0.26):", bold=True)
table(
    ["Chỉ số đánh giá", "Giá trị", "Diễn giải"],
    [
        ["Accuracy",         "93,87%", "2.724 / 2.903 mẫu dự đoán đúng"],
        ["Precision (toxic)","75,95%", "Trong số dự đoán là toxic: 75,95% là toxic thật"],
        ["Recall (toxic)",   "75,95%", "Trong số toxic thật: 75,95% được phát hiện"],
        ["F1-Score",         "75,95%", "Trung bình điều hoà của Precision và Recall"],
        ["AUC-ROC",          "95,42%", "Khả năng phân biệt toxic/clean trên mọi ngưỡng – xuất sắc"],
        ["Ngưỡng tối ưu",   "0.26",   "Tìm trên val set, tối đa hoá F1 toxic class"],
    ],
    widths=[3.5, 2, 11]
)
doc.add_paragraph()
para("Ma trận nhầm lẫn (Confusion Matrix) – Test set 2.903 mẫu:", bold=True)
table(
    ["", "Dự đoán: CLEAN", "Dự đoán: TOXIC"],
    [
        ["Thực tế: CLEAN (2.533 mẫu)", "TN = 2.444  (96,5%)", "FP = 89  (3,5%)"],
        ["Thực tế: TOXIC (370 mẫu)",   "FN = 89  (24,1%)",    "TP = 281  (75,9%)"],
    ],
    widths=[4.5, 4.5, 4.5]
)
doc.add_paragraph()
para("Phân tích confusion matrix:")
bullet("True Negative (TN) = 2.444: 96,5% bình luận sạch nhận diện đúng → false alarm rate rất thấp")
bullet("True Positive (TP) = 281: 75,9% bình luận toxic bị bắt → Recall = 75,95%")
bullet("False Positive (FP) = 89: chỉ 3,5% bình luận sạch bị nhầm là toxic → ít ảnh hưởng UX")
bullet("False Negative (FN) = 89: 24,1% toxic không bị phát hiện → hạn chế cần cải thiện ở phiên bản sau")
para(
    "FP = FN = 89 là kết quả trực tiếp của việc auto-tune ngưỡng tối đa hoá F1 "
    "với balanced metric → precision = recall = F1 = 75,95% là kết quả có chủ đích, không phải ngẫu nhiên. "
    "AUC-ROC = 95,42% xác nhận mô hình phân biệt toxic/clean xuất sắc trên mọi ngưỡng."
)

heading("3.10. Bộ lọc ngữ cảnh tiếng Việt (Context-aware Filter)", 2)
para(
    "Sau khi model cho ra raw P_toxic, predictor.py áp dụng bộ lọc ngữ cảnh để giảm false positive "
    "cho các từ nhập nhằng tiếng Việt. Ba nhóm regex pattern:"
)
table(
    ["Nhóm regex", "Các pattern tiêu biểu", "Mục đích"],
    [
        ["_AMBIGUOUS_WORDS",
         "chó, bò, gà, lợn, heo, mèo, ngựa, khỉ, chuột\n(có thể kèm 'con' phía trước)",
         "Nhận biết từ động vật có nghĩa kép (trung tính hoặc xúc phạm)"],
        ["_POSITIVE_CONTEXT",
         "dễ thương, xinh, đẹp, cute, cưng, yêu, thích, nuôi,\nbé, ăn, pet, adorable, tắm, chăm, vàng, đốm...",
         "Dấu hiệu ngữ cảnh tích cực: nói về thú cưng, yêu thương"],
        ["_STRONG_TOXIC",
         "đồ chó/bò/gà, thằng, con đĩ/điếm, mẹ m, đít,\nlồn, cặc, đéo, vãi lồn, ngu, óc chó, súc sinh...",
         "Từ độc hại rõ ràng – không điều chỉnh dù có từ nhập nhằng"],
    ],
    widths=[3.5, 7, 5.5]
)
doc.add_paragraph()
para("Điều kiện để giảm score 80% (adjusted = raw × 0.2):", bold=True)
bullet("has_ambiguous = True  (text chứa từ nhập nhằng)")
bullet("has_positive  = True  (text chứa ngữ cảnh tích cực)")
bullet("has_strong_toxic = False  (KHÔNG có từ độc hại mạnh)")
bullet("raw_score >= 0.3  (không cần điều chỉnh nếu score đã thấp)")
code([
    "# predictor.py – Context-aware score adjustment",
    "def _adjust_context_score(text: str, raw_score: float) -> float:",
    "    if raw_score < 0.3:",
    "        return raw_score           # Score thấp – không cần điều chỉnh",
    "    has_ambiguous    = _AMBIGUOUS_WORDS.search(text) is not None",
    "    has_positive     = _POSITIVE_CONTEXT.search(text) is not None",
    "    has_strong_toxic = _STRONG_TOXIC.search(text) is not None",
    "    if has_ambiguous and has_positive and not has_strong_toxic:",
    "        return raw_score * 0.2     # Ví dụ: 'con chó dễ thương' 0.71 → 0.14",
    "    return raw_score               # Ví dụ: 'thằng chó đó' 0.82 → 0.82",
])
doc.add_paragraph()
table(
    ["Ví dụ bình luận", "Raw score", "Điều chỉnh", "Kết quả cuối"],
    [
        ['"con chó dễ thương"',      "0.71", "× 0.2", "0.14 → APPROVED (con vật cưng)"],
        ['"nhà nuôi mèo dễ thương"', "0.55", "× 0.2", "0.11 → APPROVED (nuôi thú cưng)"],
        ['"thằng chó đó"',           "0.82", "Giữ nguyên", "0.82 → FLAGGED (có 'thằng' = strong toxic)"],
        ['"đồ bò"',                  "0.78", "Giữ nguyên", "0.78 → FLAGGED ('đồ bò' = strong toxic)"],
        ['"bán thịt lợn"',           "0.29", "Không xét",  "0.29 → APPROVED (raw < 0.3)"],
    ],
    widths=[4.5, 2.5, 2.8, 6.7]
)
doc.add_paragraph()

heading("3.11. Quy trình xác định toxic end-to-end", 2)
para(
    "Tổng hợp toàn bộ pipeline từ khi nhận text đến khi đưa ra quyết định kiểm duyệt:"
)
bullet("Bước 1 – Tokenize: PhoBERT tokenizer chuyển text thành token IDs (BPE), cắt/pad đến max_length=256")
bullet("Bước 2 – Encode: RoBERTa encoder xử lý qua 12 transformer layer → [CLS] vector (768 chiều)")
bullet("Bước 3 – Classify: Linear(768→2) + Softmax → [P_clean, P_toxic]; lấy P_toxic = raw_score")
bullet("Bước 4 – Context filter: _adjust_context_score() giảm 80% nếu từ nhập nhằng + ngữ cảnh tích cực + không có từ mạnh")
bullet("Bước 5 – Threshold: So sánh adjusted_score với ngưỡng (optimal=0.26 từ training / config.py dùng 0.5 trong production)")
bullet("Bước 6 – Action: Ánh xạ adjusted_score sang ToxicLevel và ModerationAction")
table(
    ["Adjusted Score", "ToxicLevel", "ModerationAction", "Hành động hệ thống", "Thông báo"],
    [
        ["< 0.4",     "LOW",    "APPROVED", "Hiển thị bình luận bình thường",          "Không"],
        ["0.4 – 0.5", "MEDIUM", "APPROVED", "Hiển thị nhưng log để review",            "Không"],
        ["0.5 – 0.7", "MEDIUM", "REJECTED", "Ẩn nội dung, thay thông báo hệ thống",   "WebSocket → tác giả"],
        ["≥ 0.7",     "HIGH",   "FLAGGED",  "Ẩn + đưa vào hàng đợi admin review",     "WebSocket + admin alert"],
    ],
    widths=[2.5, 2, 2.5, 5.5, 4]
)
doc.add_paragraph()
para(
    "Lưu ý về ngưỡng: Auto-tune tìm ngưỡng tối ưu = 0.26, nhưng production config.py sử dụng "
    "TOXIC_THRESHOLD = 0.5 cho ModerationAction.REJECTED. Điều này có nghĩa sản xuất yêu cầu "
    "P_toxic >= 0.5 mới REJECTED – cao hơn ngưỡng tối ưu 0.24 điểm. BORDERLINE_THRESHOLD = 0.3 "
    "trong config.py log các trường hợp borderline (0.26–0.5) để admin review thủ công."
)

heading("3.12. Tối ưu hoá inference – Dynamic Padding & ONNX", 2)
para(
    "Inference dùng dynamic padding (không pad cố định 256): pad đến độ dài thực tế của text, "
    "tăng tốc ~5× cho bình luận ngắn. Batch prediction dùng padding đến longest trong chunk "
    "(chunk_size=32) để tránh OOM GPU."
)
table(
    ["Tiêu chí", "PyTorch (Primary)", "ONNX Runtime (Optional)"],
    [
        ["Thời gian import",  "20–40 giây",  "~2 giây"],
        ["Nạp mô hình",       "15–30 giây",  "3–8 giây"],
        ["Cold start tổng",   "60–90 giây",  "5–15 giây"],
        ["Inference/request", "80–120 ms",   "20–50 ms"],
        ["RAM sử dụng",       "~2 GB",       "~800 MB"],
        ["Khuyến nghị",       "Development", "Production"],
    ],
    widths=[4, 5.5, 5.5]
)
doc.add_paragraph()
para(
    "Warmup sau khi load: chạy 3 câu thử ('test', 'xin chào', 'nội dung cần kiểm tra trung bình '). "
    "Mục đích: JIT compile CUDA kernels và pre-allocate GPU memory, tránh request đầu bị chậm 200–500 ms."
)

heading("3.13. Xử lý bất đồng bộ và đảm bảo tin cậy", 2)
para(
    "Kafka Consumer dùng enable.auto.commit = false (manual commit) để đảm bảo mỗi message "
    "chỉ xử lý đúng một lần (at-least-once semantics). max.poll.interval.ms = 300.000 ms (5 phút) "
    "tránh rebalance khi model đang inference. Startup guard đảm bảo Consumer chỉ lắng nghe "
    "sau khi model đã nạp xong hoàn toàn."
)
code([
    "# kafka_consumer.py – Manual commit, chỉ commit sau khi xử lý thành công",
    "consumer = AIOKafkaConsumer(",
    "    'comment-moderation-request',",
    "    bootstrap_servers='kafka:9093',",
    "    group_id='model-service',",
    "    enable_auto_commit=False,     # Manual commit",
    "    max_poll_interval_ms=300000,  # 5 phút cho inference dài",
    ")",
    "async for msg in consumer:",
    "    await moderation_service.moderate_comment(msg.value)",
    "    await consumer.commit()   # Commit chỉ sau khi xử lý thành công",
])


pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 4 – KAFKA
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 4: KIẾN TRÚC KAFKA & EVENT-DRIVEN ARCHITECTURE", 1)

heading("4.1. Cấu hình Kafka Cluster", 2)
para(
    "Kafka được triển khai theo chế độ KRaft (Kafka Raft), loại bỏ hoàn toàn sự phụ thuộc vào "
    "Apache ZooKeeper. Trong KRaft, metadata cluster được quản lý bởi chính các Kafka broker thông "
    "qua giao thức Raft consensus, đơn giản hoá vận hành và cải thiện độ ổn định. Single-broker "
    "node đảm nhận cả hai vai trò broker và controller, lắng nghe trên 3 listener: "
    "PLAINTEXT_HOST (9092 – client external), PLAINTEXT (9093 – inter-service), "
    "CONTROLLER (9094 – metadata Raft)."
)

heading("4.2. Danh sách Topics và luồng sự kiện", 2)
table(
    ["Topic", "Producer", "Consumer(s)", "Mục đích"],
    [
        ["comment-moderation-request", "Content Service\n(ModerationProducer)", "Model Service\n(AIOKafka Consumer)", "Gửi bình luận để kiểm duyệt AI"],
        ["comment-moderation-results", "Model Service\n(KafkaResultProducer)", "Content Service\n(ModerationResultConsumer)", "Trả kết quả kiểm duyệt (APPROVED / REJECTED / FLAGGED)"],
        ["post-events", "Content Service\n(OutboxPublisher)", "FeedProjectionService\nNotificationEventListener", "Sự kiện vòng đời bài viết (tạo / xoá / like)"],
        ["follow-events", "User Service", "NotificationEventListener\nFeed backfill", "Sự kiện follow / unfollow"],
        ["user-deletion-saga", "User Service", "Tất cả service\n(UserDeleteSagaConsumer)", "Điều phối xoá user phân tán (Saga)"],
    ],
    widths=[3.8, 3.2, 3.8, 4.7]
)
doc.add_paragraph()

heading("4.3. Outbox Pattern – Đảm bảo atomicity", 2)
para(
    "Vấn đề: Nếu Content Service lưu Post vào Neo4j thành công nhưng sau đó gặp lỗi trước khi "
    "publish Kafka message, sự kiện bị mất và hệ thống rơi vào trạng thái không nhất quán. "
    "Giải pháp: Outbox Pattern – ghi event vào bảng outbox trong cùng transaction database, "
    "sau đó một scheduler (polling mỗi 1 giây) đọc và publish lên Kafka:"
)
code([
    "// OutboxService.java – Lưu event cùng transaction Neo4j",
    "@Transactional",
    "public void saveEvent(String aggregateType, String aggregateId,",
    "                      String eventType, String topic, String payload) {",
    "    OutboxEvent event = OutboxEvent.builder()",
    "        .aggregateType(aggregateType).aggregateId(aggregateId)",
    "        .eventType(eventType).topic(topic).payload(payload)",
    "        .published(false).retryCount(0)",
    "        .createdAt(Instant.now()).build();",
    "    outboxRepository.save(event);  // Lưu cùng transaction",
    "}",
    "",
    "// OutboxScheduler.java – Polling và publish lên Kafka",
    "@Scheduled(fixedDelay = 1000)  // Mỗi 1 giây",
    "public void publishPendingEvents() {",
    "    List<OutboxEvent> events = outboxRepository.findByPublishedFalse();",
    "    events.forEach(event -> {",
    "        kafkaTemplate.send(event.getTopic(), event.getPayload());",
    "        event.setPublished(true);",
    "        outboxRepository.save(event);",
    "    });",
    "}",
])
doc.add_paragraph()

heading("4.4. CQRS Feed với Kafka Projection", 2)
para(
    "Feed người dùng được xây dựng theo CQRS: Write side ghi vào Neo4j (source of truth), "
    "FeedProjectionService lắng nghe Kafka topic post-events và materialization PostFeedItem "
    "vào MongoDB (denormalized read model). Query feed chỉ đọc từ MongoDB, không join Neo4j, "
    "đảm bảo độ trễ thấp. Eventual consistency ~1–2 giây được chấp nhận cho social feed."
)
code([
    "// FeedProjectionService.java – CQRS Read Model Projection",
    "@KafkaListener(topics = \"post-events\", groupId = \"content-feed-projection\")",
    "public void consume(String json) {",
    "    Map<String, Object> event = objectMapper.readValue(json, Map.class);",
    "    switch ((String) event.get(\"eventType\")) {",
    "        case \"POST_CREATED\" -> materializeNewPost(event);   // Thêm vào feed",
    "        case \"POST_DELETED\" -> removeFromFeed(event);       // Xoá khỏi feed",
    "        case \"POST_LIKED\"   -> updateLikeCount(event);      // Cập nhật số like",
    "    }",
    "}",
])
doc.add_paragraph()

heading("4.5. Saga Pattern – Xoá người dùng phân tán", 2)
para(
    "Khi xoá user, cần xoá dữ liệu trên nhiều service (User Service, Content Service, "
    "Communication Service). Thay vì two-phase commit (2PC – phức tạp và dễ deadlock), Blur "
    "dùng Choreography-based Saga: User Service publish user-deletion-saga event, mỗi service "
    "lắng nghe và tự xoá dữ liệu của mình rồi publish completion event. Không có coordinator "
    "trung tâm, không có distributed transaction."
)

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 5 – CACHE ĐA TẦNG
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 5: HỆ THỐNG CACHE ĐA TẦNG (MULTI-LEVEL CACHE)", 1)

heading("5.1. Kiến trúc tổng quan", 2)
para(
    "Content Service triển khai hệ thống cache 2 tầng kết hợp Caffeine (L1) và Redis (L2). "
    "Caffeine cực kỳ nhanh (~1–5 μs) nhưng không chia sẻ giữa các instance; Redis chậm hơn "
    "(~100–500 μs) nhưng là distributed cache, đảm bảo tính nhất quán khi scale ra nhiều instance. "
    "Khi cache L1 miss, hệ thống tự động backfill từ L2, không truy cập database – giảm tải "
    "database ~70–80% cho các query phổ biến."
)

heading("5.2. Lớp TwoLevelCache – Cơ chế đọc/ghi", 2)
para(
    "Class TwoLevelCache implements interface org.springframework.cache.Cache, override "
    "các method get(), put(), evict() theo logic 2 tầng. Cấu hình: L1 max 500 entries, "
    "TTL 30 giây; L2 (Redis) với TTL riêng theo từng tên cache."
)
code([
    "// TwoLevelCache.java – Logic đọc theo thứ tự L1 → L2 → Database",
    "@Override",
    "public ValueWrapper get(Object key) {",
    "    ValueWrapper l1 = l1Cache.get(key);      // Kiểm tra Caffeine trước",
    "    if (l1 != null) return l1;               // L1 HIT – trả về ngay (~1–5 μs)",
    "",
    "    ValueWrapper l2 = l2Cache.get(key);      // Kiểm tra Redis",
    "    if (l2 != null) {",
    "        l1Cache.put(key, l2.get());           // Backfill L1 từ L2",
    "        return l2;                             // L2 HIT (~100–500 μs)",
    "    }",
    "    return null;                               // MISS – sẽ đọc từ database",
    "}",
    "",
    "@Override",
    "public void put(Object key, Object value) {",
    "    l2Cache.put(key, value);                  // Ghi vào Redis",
    "    l1Cache.put(key, value);                  // Ghi vào Caffeine",
    "    publisher.publishInvalidation(name, key.toString()); // Notify các node khác",
    "}",
])
doc.add_paragraph()

heading("5.3. TwoLevelCacheManager và cấu hình TTL", 2)
para(
    "TwoLevelCacheManager implements CacheManager, sử dụng ConcurrentHashMap để quản lý "
    "các cache instance. Mỗi tên cache có TTL Redis riêng phù hợp với đặc điểm dữ liệu:"
)
table(
    ["Tên Cache", "L1 TTL", "L2 TTL (Redis)", "Max L1 Size", "Mô tả"],
    [
        ["posts",      "30 giây", "2 phút",  "500 entries", "Danh sách bài viết theo userId"],
        ["post",       "30 giây", "5 phút",  "500 entries", "Chi tiết một bài viết"],
        ["comments",   "30 giây", "3 phút",  "500 entries", "Danh sách bình luận theo postId"],
        ["savedPosts", "30 giây", "10 phút", "500 entries", "Bài viết đã lưu của user"],
        ["profiles",   "30 giây", "10 phút", "500 entries", "Hồ sơ người dùng"],
    ],
    widths=[3, 2, 3, 3, 5.5]
)
doc.add_paragraph()

heading("5.4. Cache Invalidation qua Redis Pub/Sub", 2)
para(
    "Khi một instance cập nhật hoặc xoá dữ liệu, L1 cache của các instance khác cần được "
    "đồng bộ ngay lập tức. CacheInvalidationPublisher gửi message qua Redis channel "
    "cache-invalidation:content-service. Tất cả instance subscribe channel này, nhận message "
    "và evict L1 cache tương ứng trong vòng < 100 ms:"
)
code([
    "// CacheInvalidationPublisher.java – Gửi invalidation message qua Redis",
    "public void publishInvalidation(String cacheName, String key) {",
    "    String msg = cacheName + \"::\" + (key != null ? key : \"*\");",
    "    redisTemplate.convertAndSend(\"cache-invalidation:content-service\", msg);",
    "}",
    "",
    "// CacheInvalidationSubscriber.java – Nhận và evict L1 cache",
    "@Override",
    "public void onMessage(Message message, byte[] pattern) {",
    "    String[] parts = deserialize(message.getBody()).split(\"::\");",
    "    Cache cache = cacheManager.getCache(parts[0]);",
    "    if (\"*\".equals(parts[1])) cache.clear();   // Xoá toàn bộ cache",
    "    else                        cache.evict(parts[1]); // Xoá key cụ thể",
    "}",
])
doc.add_paragraph()

heading("5.5. Distributed Lock – Chống Cache Stampede", 2)
para(
    "Cache stampede xảy ra khi nhiều request đồng thời miss cache và cùng truy cập database, "
    "gây quá tải đột ngột. DistributedCacheService dùng Redisson RLock để chỉ cho phép một "
    "request tính toán kết quả, các request khác chờ và đọc lại từ cache (double-check pattern). "
    "Timeout: 5 giây chờ khoá, 10 giây giữ khoá:"
)
code([
    "// DistributedCacheService.java – Double-check với Distributed Lock",
    "public <T> T getWithLock(String cacheKey, Duration ttl, Supplier<T> loader) {",
    "    RLock lock = redissonClient.getLock(\"lock:\" + cacheKey);",
    "    boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);",
    "    if (acquired) {",
    "        try {",
    "            T check = redisTemplate.opsForValue().get(cacheKey); // Double-check",
    "            if (check != null) return check;",
    "            T data = loader.get();      // Chỉ 1 thread thực thi query DB",
    "            redisTemplate.opsForValue().set(cacheKey, data, ttl);",
    "            return data;",
    "        } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }",
    "    }",
    "    return redisTemplate.opsForValue().get(cacheKey); // Đọc lại sau khi chờ",
    "}",
])
doc.add_paragraph()

heading("5.6. Atomic Counter với Lua Script", 2)
para(
    "Lượt thích (like count) và lượt xem được cập nhật bằng Lua script trong Redis để đảm bảo "
    "tính nguyên tử trong môi trường highly concurrent – tránh race condition khi nhiều "
    "request đồng thời tăng bộ đếm:"
)
code([
    "// Lua script – atomic increment đảm bảo không race condition",
    "String script = \"return redis.call('INCR', KEYS[1])\";",
    "Long newCount = redisTemplate.execute(",
    "    new DefaultRedisScript<>(script, Long.class),",
    "    Collections.singletonList(counterKey));",
])

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 6 – KEYCLOAK
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 6: XÁC THỰC VÀ PHÂN QUYỀN VỚI KEYCLOAK", 1)

heading("6.1. Tổng quan Realm và Cấu hình", 2)
para(
    "Keycloak 26.1 được triển khai với realm blur, backed bởi MySQL 8.0. Realm cấu hình "
    "Brute Force Protection (tự động khoá tài khoản sau nhiều lần đăng nhập sai), không cho phép "
    "đăng ký tự do, hỗ trợ đăng nhập bằng email. JWT Access Token có thời hạn 3.600 giây (1 giờ), "
    "SSO Session Idle Timeout 360.000 giây (~4 ngày), thuật toán ký RS256."
)

heading("6.2. OAuth2 Clients", 2)
table(
    ["Client", "Loại", "Flow", "Mục đích"],
    [
        ["blur-web-app", "Public (PKCE)", "Authorization Code + PKCE", "SPA React frontend – không lưu client secret phía client"],
        ["blur-api-gateway", "Confidential", "Authorization Code, Direct Access Grants", "API Gateway token exchange với Keycloak"],
        ["blur-service-account", "Service Account", "Client Credentials (M2M)", "Service-to-service calls, quản lý user qua Keycloak Admin API"],
    ],
    widths=[3.8, 3, 4, 5.7]
)
doc.add_paragraph()

heading("6.3. Protocol Mappers – Tuỳ chỉnh JWT Payload", 2)
para(
    "Protocol Mappers nhúng thêm thông tin tuỳ chỉnh vào JWT token, giúp các service downstream "
    "không cần gọi thêm API Keycloak để tra cứu thông tin người dùng:"
)
table(
    ["Mapper", "Claim trong JWT", "Giá trị"],
    [
        ["blur-user-id", "blur_user_id", "Blur internal user UUID – liên kết giữa Keycloak user và UserProfile trong Neo4j"],
        ["realm-roles",  "realm_roles",  "Danh sách roles: [\"USER\"] hoặc [\"USER\", \"ADMIN\"]"],
        ["username",     "preferred_username", "Username của người dùng"],
    ],
    widths=[3, 3.5, 9]
)
doc.add_paragraph()

heading("6.4. Luồng xác thực đầy đủ (Authorization Code + PKCE)", 2)
bullet("Bước 1: Người dùng nhấn Login → Frontend redirect đến Keycloak Authorization Endpoint kèm code_challenge (PKCE)")
bullet("Bước 2: Keycloak hiển thị trang đăng nhập, xác thực credentials")
bullet("Bước 3: Keycloak redirect về callback URL với authorization_code")
bullet("Bước 4: Frontend gửi code + code_verifier đến API Gateway /api/auth/login (token exchange)")
bullet("Bước 5: API Gateway đổi code lấy JWT access_token và refresh_token từ Keycloak Token Endpoint")
bullet("Bước 6: Frontend lưu token; Axios interceptor tự động gắn Authorization: Bearer vào mọi request")
bullet("Bước 7: API Gateway validate JWT qua JWKS endpoint (RS256 public key) – không gọi Keycloak mỗi request")
bullet("Bước 8: Service downstream đọc blur_user_id từ JWT để xác định người dùng")

heading("6.5. Cookie-to-Header Filter tại API Gateway", 2)
para(
    "Để hỗ trợ cả hai cách lưu token: cookie HttpOnly (bảo mật hơn với tấn công XSS) và "
    "Authorization header, API Gateway có CookieToHeaderWebFilter tự động chuyển token từ "
    "cookie access_token sang header Authorization: Bearer trước khi đến Spring Security filter chain."
)

heading("6.6. Mã hoá thông tin nhạy cảm (PII Encryption)", 2)
para(
    "Thông tin cá nhân nhạy cảm (số điện thoại, email, ngày sinh, địa chỉ) được mã hoá "
    "bằng AES-256 trước khi lưu vào Neo4j. emailIndex là blind index (SHA-256 hash của email) "
    "phục vụ cho việc lookup theo email mà không cần giải mã toàn bộ. Chỉ User Service có "
    "khoá giải mã; các service khác chỉ nhận DTO đã giải mã khi cần thiết."
)

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 7 – RESILIENCE4J
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 7: KHẢ NĂNG CHỊU LỖI VỚI RESILIENCE4J", 1)

heading("7.1. Tổng quan các pattern được áp dụng", 2)
para(
    "Hệ thống Blur áp dụng 4 pattern chịu lỗi từ Resilience4j 2.2.0, xếp lồng nhau theo thứ tự "
    "từ ngoài vào trong: Bulkhead → CircuitBreaker → Retry → TimeLimiter. Mỗi lớp bảo vệ một "
    "khía cạnh khác nhau của hệ thống phân tán, đảm bảo một service gặp sự cố không kéo theo "
    "toàn bộ hệ thống (cascade failure)."
)

heading("7.2. Circuit Breaker – Ngắt mạch tự động", 2)
para(
    "Circuit Breaker theo dõi tỷ lệ lỗi trong cửa sổ trượt (sliding window) 10 request gần "
    "nhất. Khi tỷ lệ lỗi vượt ngưỡng 50%, mạch chuyển sang trạng thái OPEN – từ chối ngay mọi "
    "request và trả về fallback, tránh đợi timeout gây blocking. Sau 30 giây, thử lại 3 request "
    "(HALF_OPEN); nếu thành công → CLOSED, nếu thất bại → OPEN lại:"
)
code([
    "// ResilientUserServiceClient.java – Xếp lồng 3 pattern",
    "@Bulkhead(name = \"userServiceBH\")",
    "@CircuitBreaker(name = \"userServiceCB\", fallbackMethod = \"getProfileFallback\")",
    "@Retry(name = \"userServiceRetry\")",
    "public ApiResponse<UserProfileResponse> getProfile(String userId) {",
    "    return profileClient.getProfile(userId);",
    "}",
    "",
    "// Fallback – trả về dữ liệu mặc định khi service không khả dụng",
    "private ApiResponse<UserProfileResponse> getProfileFallback(String userId, Throwable t) {",
    "    log.warn(\"User service không khả dụng, trả về fallback cho userId={}\", userId);",
    "    return ApiResponse.<UserProfileResponse>builder()",
    "        .statusCode(503).error(\"Dịch vụ tạm thời không khả dụng\").build();",
    "}",
])
doc.add_paragraph()

heading("7.3. Retry – Thử lại với Exponential Backoff", 2)
para(
    "Retry tự động thử lại 3 lần (1 lần gốc + 2 retry) với exponential backoff: 500 ms → 1 s → 2 s. "
    "Chỉ retry các lỗi mạng/tạm thời (IOException, FeignException.ServiceUnavailable), không retry "
    "lỗi logic (BadRequest 400, NotFound 404) để tránh tăng tải không cần thiết lên service đích."
)

heading("7.4. Bulkhead – Giới hạn đồng thời", 2)
para(
    "Bulkhead ngăn một service tiêu tốn toàn bộ thread pool của service khác. User Service được "
    "phép tối đa 25 concurrent calls từ Content Service; Communication Service tối đa 10 concurrent "
    "calls. Request vượt giới hạn bị reject sau 500 ms chờ trong queue, thay vì block indefinitely "
    "và gây resource exhaustion."
)

heading("7.5. TimeLimiter – Giới hạn thời gian chờ", 2)
para(
    "TimeLimiter đặt timeout cho async calls: User Service 5 giây, Communication Service 2 giây "
    "(thông báo ít critical hơn). Kết hợp với cancelRunningFuture = true để huỷ CompletableFuture "
    "khi hết timeout, giải phóng thread ngay lập tức."
)
table(
    ["Pattern", "Instance", "Cấu hình chính", "Bảo vệ trước"],
    [
        ["Circuit Breaker", "userServiceCB",         "50% failure rate, window 10, wait 30s", "Cascade failure khi User Service down"],
        ["Circuit Breaker", "contentServiceCB",      "50% failure rate, wait 30s",            "Cascade failure khi Content Service down"],
        ["Circuit Breaker", "communicationServiceCB","60% failure rate, wait 15s",            "Lỗi thông báo (non-critical, threshold cao hơn)"],
        ["Retry",           "userServiceRetry",      "3 attempts, exp backoff 0.5s→1s→2s",   "Lỗi mạng tạm thời"],
        ["Bulkhead",        "userServiceBH",         "25 concurrent calls, queue 0.5s",       "Thread pool exhaustion"],
        ["TimeLimiter",     "userServiceTL",         "5s timeout, cancel future",             "Slow response blocking threads"],
    ],
    widths=[3, 4, 5.5, 4]
)
doc.add_paragraph()

heading("7.6. API Gateway CircuitBreaker Filter", 2)
para(
    "API Gateway (Spring Cloud Gateway Reactive) tích hợp CircuitBreaker filter cho mỗi route. "
    "Khi downstream service down, gateway trả về fallback response từ FallbackController thay vì "
    "503 timeout trực tiếp, đảm bảo trải nghiệm người dùng nhất quán:"
)
code([
    "# application.yaml – Route với CircuitBreaker Filter",
    "filters:",
    "  - name: CircuitBreaker",
    "    args:",
    "      name: userServiceCB",
    "      fallbackUri: forward:/fallback/user-service",
])

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 8 – DESIGN PATTERNS
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 8: DESIGN PATTERNS ÁP DỤNG", 1)

heading("8.1. Tổng quan các pattern", 2)
table(
    ["Pattern", "Nhóm", "Áp dụng trong dự án", "Lợi ích"],
    [
        ["Repository",     "Structural",    "UserProfileRepository, PostRepository, CommentRepository (Spring Data Neo4j/MongoDB)", "Tách biệt logic nghiệp vụ khỏi truy cập dữ liệu"],
        ["CQRS",           "Architectural", "Write: Neo4j; Read: MongoDB PostFeedItem – FeedProjectionService", "Tối ưu đọc/ghi độc lập, cải thiện scalability"],
        ["Outbox",         "Integration",   "OutboxService + OutboxScheduler, node outbox_event trong Neo4j", "Đảm bảo atomicity giữa DB write và Kafka publish"],
        ["Saga",           "Integration",   "user-deletion-saga Kafka topic, UserDeleteSagaConsumer tại mỗi service", "Distributed transaction không cần 2PC"],
        ["Observer",       "Behavioral",    "Redis Pub/Sub (CacheInvalidationPublisher/Subscriber), Kafka listeners", "Loosely-coupled event notification"],
        ["Strategy",       "Behavioral",    "4 chiến lược gợi ý (mutual/nearby/similar/popular), model backend (PyTorch/ONNX)", "Dễ mở rộng thêm chiến lược mới"],
        ["Factory",        "Creational",    "_create_predictor() trong Model Service – chọn backend AI theo config", "Tạo object phù hợp dựa trên cấu hình môi trường"],
        ["Builder",        "Creational",    "Lombok @Builder trên mọi Entity và DTO (Post, Comment, ApiResponse…)", "Xây dựng object phức tạp rõ ràng, immutable"],
        ["Adapter",        "Structural",    "ReactiveJwtAuthenticationConverterAdapter, MapStruct Mappers", "Tương thích interface khác nhau"],
        ["Decorator",      "Structural",    "ResilientUserServiceClient bọc ProfileClient thêm Resilience4j", "Thêm chức năng mà không thay đổi core logic"],
        ["Database/Service","Architectural","Mỗi service có database riêng (Neo4j / MongoDB / MySQL)", "Loose coupling, độc lập triển khai và mở rộng"],
    ],
    widths=[2.8, 2.5, 6.5, 3.7]
)
doc.add_paragraph()

heading("8.2. Repository Pattern – Chi tiết", 2)
para(
    "Spring Data Neo4j và Spring Data MongoDB tự động implement Repository từ interface, "
    "hỗ trợ custom Cypher query với @Query annotation. Lớp service không biết đến chi tiết "
    "truy vấn, chỉ gọi phương thức tên rõ nghĩa:"
)
code([
    "// UserProfileRepository.java – Custom Cypher queries",
    "public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {",
    "    Optional<UserProfile> findByUsername(String username);",
    "    Optional<UserProfile> findByEmailIndex(String emailIndex); // Blind index",
    "",
    "    @Query(\"MATCH (me:user_profile {id:$userId})-[:follows]->(f:user_profile)\"",
    "         + \"-[:follows]->(rec:user_profile) WHERE rec <> me\"",
    "         + \" AND NOT (me)-[:follows]->(rec) RETURN rec SKIP $skip LIMIT $limit\")",
    "    List<UserProfile> findMutualRecommendations(",
    "        @Param(\"userId\") String userId,",
    "        @Param(\"skip\") int skip, @Param(\"limit\") int limit);",
    "}",
])
doc.add_paragraph()

heading("8.3. CQRS Pattern – Chi tiết", 2)
para(
    "Write Model (Command Side): PostService.createPost() → lưu vào Neo4j → publish post-events "
    "qua Outbox. Read Model (Query Side): FeedProjectionService consume post-events → upsert "
    "PostFeedItem vào MongoDB. FeedService.getMyFeed() chỉ đọc từ MongoDB, không join Neo4j. "
    "Lợi ích: feed query được index theo targetUserId + sort createdDate trên MongoDB riêng, "
    "không ảnh hưởng write performance Neo4j."
)

heading("8.4. Builder Pattern với Lombok", 2)
code([
    "// Post.java – Entity với @Builder (Lombok)",
    "@Node(\"post\") @Builder @NoArgsConstructor @AllArgsConstructor",
    "public class Post {",
    "    @Id private String id;",
    "    private String userId, content;",
    "    private List<String> mediaUrls;",
    "    private Instant createdAt, updatedAt;",
    "}",
    "",
    "// Sử dụng Builder – rõ ràng, immutable, không nhầm thứ tự tham số",
    "Post post = Post.builder()",
    "    .id(UUID.randomUUID().toString())",
    "    .userId(currentUserId)",
    "    .content(request.getContent())",
    "    .mediaUrls(request.getMediaUrls())",
    "    .createdAt(Instant.now())",
    "    .build();",
])

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 9 – FOLLOW RECOMMENDATION
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 9: HỆ THỐNG GỢI Ý FOLLOW (RECOMMENDATION SYSTEM)", 1)

heading("9.1. Tổng quan", 2)
para(
    "User Service cung cấp 4 chiến lược gợi ý người dùng nên follow, tất cả được implement bằng "
    "Cypher queries trên Neo4j graph database. Endpoint: GET /api/recommendations/{strategy} với "
    "pagination (page, size). Mọi chiến lược tự động loại trừ người đã follow, đã bị block, "
    "hoặc đã block mình – đảm bảo trải nghiệm gợi ý an toàn và phù hợp."
)

heading("9.2. Chiến lược 1: Gợi ý từ mạng lưới chung (Mutual)", 2)
para(
    "Tìm những người mà bạn bè của bạn đang follow nhưng bạn chưa follow (2-hop graph traversal). "
    "Kết quả được xếp hạng theo số lượng mutual connections nhiều nhất – những người có nhiều "
    "bạn chung hơn sẽ xuất hiện đầu tiên:"
)
code([
    "// Cypher Query – Mutual Recommendations (2-hop traversal)",
    "MATCH (me:user_profile {id:$userId})-[:follows]->(myFollowing:user_profile)",
    "MATCH (myFollowing)-[:follows]->(recommended:user_profile)",
    "WHERE recommended <> me",
    "  AND NOT (me)-[:follows]->(recommended)",
    "  AND NOT EXISTS((me)-[:BLOCKED]->(recommended))",
    "  AND NOT EXISTS((recommended)-[:BLOCKED]->(me))",
    "WITH recommended, COUNT(DISTINCT myFollowing) AS mutualCount",
    "ORDER BY mutualCount DESC  // Xếp hạng theo số bạn chung",
    "SKIP $skip LIMIT $limit",
    "RETURN recommended",
])
doc.add_paragraph()

heading("9.3. Chiến lược 2: Gợi ý theo địa phương (Nearby)", 2)
para(
    "Gợi ý người dùng cùng thành phố dựa trên thuộc tính city trong UserProfile. "
    "Phù hợp để kết nối người dùng trong cùng cộng đồng địa phương, hỗ trợ pagination. "
    "Đặc biệt hữu ích khi người dùng mới tham gia và chưa có mạng lưới kết nối."
)

heading("9.4. Chiến lược 3: Gợi ý theo sở thích tương đồng (Similar Taste)", 2)
para(
    "Dựa trên nguyên lý homophily: người follow cùng tài khoản có xu hướng chia sẻ sở thích. "
    "Tìm những người mà những người bạn follow cũng follow – một dạng collaborative filtering "
    "dựa trên đồ thị, không cần feature engineering phức tạp."
)

heading("9.5. Chiến lược 4: Gợi ý người nổi bật (Popular)", 2)
para(
    "Lọc người dùng có ít nhất minFollowers người theo dõi (mặc định 100), sắp xếp theo "
    "followersCount giảm dần. Phù hợp giới thiệu influencer hoặc KOL cho người dùng mới "
    "hoặc người dùng chưa có nhiều kết nối trong mạng lưới."
)

heading("9.6. Tích hợp sự kiện Follow vào Feed", 2)
para(
    "Khi A follow B, User Service publish follow-events Kafka topic. Content Service lắng nghe "
    "và backfill feed của A với các bài viết cũ của B (có giới hạn thời gian để tránh flood). "
    "Khi A unfollow B, các PostFeedItem của B được xoá khỏi feed của A trong MongoDB ngay lập tức."
)

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 10 – KẾT QUẢ THỰC TẾ
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 10: KẾT QUẢ THỰC TẾ ĐẠT ĐƯỢC", 1)

heading("10.1. Các tính năng hoàn thiện", 2)
para("Sau quá trình phát triển, hệ thống Blur đã hoàn thiện đầy đủ các tính năng chính:")
table(
    ["Tính năng", "Trạng thái", "Mô tả chi tiết"],
    [
        ["Đăng ký / Đăng nhập",    "✅ Hoàn thiện", "OAuth2/OIDC qua Keycloak, PKCE flow, Google SSO"],
        ["Quản lý hồ sơ",          "✅ Hoàn thiện", "Cập nhật thông tin, avatar, mã hoá PII (AES-256)"],
        ["Đăng bài viết",          "✅ Hoàn thiện", "Text, media (ảnh/video), lưu nháp, chỉnh sửa, xoá"],
        ["Feed cá nhân",           "✅ Hoàn thiện", "CQRS read model MongoDB, realtime cập nhật qua Kafka"],
        ["Bình luận & Reply",      "✅ Hoàn thiện", "Đa cấp, like bình luận, trạng thái kiểm duyệt hiển thị"],
        ["Kiểm duyệt AI",          "✅ Hoàn thiện", "PhoBERT async pipeline, 3 mức APPROVED/REJECTED/FLAGGED"],
        ["Follow / Unfollow",      "✅ Hoàn thiện", "Graph relationships Neo4j, cập nhật follower/following count"],
        ["Gợi ý người dùng",       "✅ Hoàn thiện", "4 chiến lược: mutual, nearby, similar taste, popular"],
        ["Nhắn tin thời gian thực","✅ Hoàn thiện", "Socket.IO, gửi/nhận, trạng thái đã đọc, đính kèm file"],
        ["Gọi video/audio",        "✅ Hoàn thiện", "WebRTC P2P, tín hiệu SDP qua Socket.IO"],
        ["Thông báo push",         "✅ Hoàn thiện", "STOMP/WebSocket, các sự kiện: like/comment/follow/call"],
        ["AI Chatbot",             "✅ Hoàn thiện", "Google Gemini API với ngữ cảnh cuộc trò chuyện"],
        ["Stories",                "✅ Hoàn thiện", "Story tạm thời, đếm lượt xem, tự động hết hạn"],
        ["Lưu bài viết",           "✅ Hoàn thiện", "Bookmark cá nhân, cache 10 phút"],
        ["Cache đa tầng",          "✅ Hoàn thiện", "Caffeine L1 + Redis L2, invalidation qua Pub/Sub"],
        ["Circuit Breaker",        "✅ Hoàn thiện", "Resilience4j, fallback response, monitoring Actuator"],
        ["Email thông báo",        "✅ Hoàn thiện", "Gửi email cho người dùng offline"],
    ],
    widths=[4, 2.5, 9]
)
doc.add_paragraph()

heading("10.2. Hiệu năng hệ thống đo được", 2)
table(
    ["Chỉ số", "Kết quả đo được", "Phương pháp / Ghi chú"],
    [
        ["API Response Time (cache HIT L1)",   "< 5 ms",       "Caffeine in-process, không qua network"],
        ["API Response Time (cache HIT L2)",   "< 50 ms",      "Redis network round trip"],
        ["API Response Time (cache MISS)",     "< 200 ms",     "Neo4j query + populate cache"],
        ["Thời gian kiểm duyệt bình luận",    "1–3 giây",     "Async Kafka pipeline, non-blocking UX"],
        ["Throughput AI moderation (PyTorch)", "5–10 req/s",   "Single instance, CPU inference"],
        ["Throughput AI moderation (ONNX)",    "20–40 req/s",  "ONNX tối ưu, ~4× nhanh hơn PyTorch"],
        ["Kafka message latency",              "< 50 ms",      "End-to-end producer → consumer"],
        ["Circuit Breaker phản hồi",           "< 10 ms",      "Immediate rejection khi trạng thái OPEN"],
        ["Cold start Model Service (PyTorch)", "60–90 giây",   "Import PyTorch + nạp PhoBERT model"],
        ["Cold start Model Service (ONNX)",    "5–15 giây",    "ONNX optimized loading (~6× nhanh hơn"],
        ["WebSocket latency (chat)",           "< 30 ms",      "Socket.IO, mạng cục bộ"],
        ["Cache invalidation propagation",     "< 100 ms",     "Redis Pub/Sub broadcast đến tất cả node"],
        ["Giảm tải database",                  "~70–80%",      "So sánh cache HIT rate trước/sau khi bật cache"],
    ],
    widths=[5.5, 3, 7]
)
doc.add_paragraph()

heading("10.3. Độ chính xác mô hình AI", 2)
table(
    ["Chỉ số", "Giá trị", "Tập dữ liệu / Ghi chú"],
    [
        ["Accuracy",            "93,87%",  "2.724 / 2.903 mẫu test dự đoán đúng"],
        ["F1-Score (toxic)",    "75,95%",  "Lớp TOXIC – trung bình điều hoà Precision/Recall"],
        ["Precision (toxic)",   "75,95%",  "75,95% dự đoán toxic là đúng (FP = 89 / 2.533 clean)"],
        ["Recall (toxic)",      "75,95%",  "75,95% toxic thật được phát hiện (FN = 89 / 370 toxic)"],
        ["AUC-ROC",             "95,42%",  "Phân biệt toxic/clean xuất sắc trên mọi ngưỡng"],
        ["False Positive Rate", "3,5%",    "89 / 2.533 bình luận sạch bị nhầm là toxic"],
        ["Ngưỡng tối ưu",      "0.26",    "Auto-tuned trên val set, tối đa hoá F1"],
    ],
    widths=[4, 3, 8.5]
)
doc.add_paragraph()
para(
    "Precision = Recall = F1 = 75,95% là kết quả của việc auto-tune ngưỡng tối đa hoá F1 "
    "với balanced metric. False Positive Rate chỉ 3,5% (89/2.533) – rất thấp, ít ảnh hưởng UX. "
    "AUC-ROC = 95,42% xác nhận mô hình có khả năng phân biệt toxic/clean xuất sắc. "
    "Context-aware filter giảm thêm false positive cho từ nhập nhằng tiếng Việt (ví dụ: 'con chó dễ thương')."
)

heading("10.4. Kiến trúc triển khai thực tế", 2)
para(
    "Toàn bộ hệ thống được container hoá bằng Docker với multi-stage builds để tối ưu image size. "
    "docker-compose.yml quản lý orchestration local development với đầy đủ health checks và "
    "dependency ordering (Keycloak depends_on MySQL với điều kiện service_healthy). "
    "Nginx đảm nhận reverse proxy và static file serving cho React frontend."
)
bullet("Tổng số container: 12 (api-gateway, user-service, content-service, communication-service, model-service, keycloak, neo4j, mongodb, redis, kafka, mysql, nginx)")
bullet("Java services: Multi-stage build với Amazon Corretto 21 (build stage: Maven, run stage: JRE only – image nhỏ hơn)")
bullet("Python service: Multi-stage build với deps stage riêng, runtime stage gọn nhẹ")
bullet("Health checks: Mọi service có endpoint kiểm tra sức khoẻ (/actuator/health cho Spring Boot, /api/v1/health cho FastAPI)")

heading("10.5. Bảo mật thực tế đạt được", 2)
bullet("JWT RS256 với khoá xoay định kỳ qua JWKS endpoint (không hardcode secret)")
bullet("Brute Force Protection: tự động khoá tài khoản sau nhiều lần đăng nhập sai")
bullet("AES-256 mã hoá PII (số điện thoại, ngày sinh, địa chỉ) – tuân thủ GDPR/PDPA")
bullet("Blind index (SHA-256) cho email lookup không cần giải mã toàn bộ dữ liệu")
bullet("CORS chỉ cho phép localhost:3000 và domain production – ngăn cross-origin attacks")
bullet("API Gateway là điểm vào duy nhất – các service không expose trực tiếp ra ngoài")
bullet("PKCE (Proof Key for Code Exchange) ngăn authorization code interception attacks")

pb()

# ══════════════════════════════════════════════════════════════════════════════
# CHƯƠNG 11 – KẾT LUẬN
# ══════════════════════════════════════════════════════════════════════════════
heading("CHƯƠNG 11: KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN", 1)

heading("11.1. Kết luận", 2)
para(
    "Dự án Blur Social Network đã thành công trong việc xây dựng một nền tảng mạng xã hội đầy đủ "
    "tính năng, ứng dụng nhiều công nghệ và kỹ thuật hiện đại trong lĩnh vực hệ thống phân tán. "
    "Các kết quả nổi bật đạt được:"
)
bullet("Kiến trúc microservices hoàn chỉnh với 6 service độc lập, Database per Service, API Gateway pattern")
bullet("Hệ thống kiểm duyệt AI hoạt động 100% bất đồng bộ, không chặn luồng người dùng; mô hình PhoBERT đạt F1 = 75,95%, AUC-ROC = 95,42%")
bullet("Cache đa tầng giảm tải database ~70–80% cho các query phổ biến, đảm bảo nhất quán qua Redis Pub/Sub")
bullet("Keycloak OAuth2/OIDC cung cấp bảo mật chuẩn enterprise với PII encryption AES-256")
bullet("Resilience4j đảm bảo hệ thống không bị cascade failure khi một service gặp sự cố")
bullet("Giao tiếp real-time đa kênh: Socket.IO chat, STOMP thông báo, WebRTC gọi video/audio")
bullet("Kafka event-driven architecture với Outbox Pattern đảm bảo exactly-once semantics")
bullet("4 chiến lược gợi ý follow thông minh dựa trên đồ thị xã hội Neo4j")

heading("11.2. Hướng phát triển", 2)
para("Các cải tiến tiếp theo có thể thực hiện trong các giai đoạn sau:")
bullet("Nâng cấp mô hình AI: Fine-tune thêm với dữ liệu domain-specific, thử nghiệm multimodal content moderation cho ảnh/video bằng ViT")
bullet("Kubernetes deployment: Chuyển từ docker-compose sang Kubernetes với Helm charts, Horizontal Pod Autoscaler")
bullet("Distributed Tracing: Tích hợp OpenTelemetry + Jaeger để theo dõi request trace qua nhiều service")
bullet("Service Mesh: Istio hoặc Linkerd để quản lý traffic, mTLS giữa services, observability")
bullet("Recommendation Engine nâng cao: Graph Neural Networks (GNN) trên Neo4j cho gợi ý chính xác hơn")
bullet("Content Delivery Network (CDN): Cloudflare/AWS CloudFront cho media assets, giảm latency toàn cầu")
bullet("Full-text Search: Elasticsearch cho tìm kiếm bài viết, người dùng với relevance ranking")
bullet("Rate Limiting nâng cao: Redis-based sliding window rate limiting tại API Gateway")

heading("11.3. Bài học kinh nghiệm", 2)
para("Trong quá trình phát triển, nhóm rút ra một số bài học quan trọng:")
bullet("Eventual Consistency không phải vấn đề, mà là sự đánh đổi có chủ đích: CQRS feed chấp nhận lag 1–2 giây nhưng đổi lại throughput cao hơn nhiều")
bullet("Outbox Pattern là giải pháp thiết yếu cho distributed systems – dual write không bao giờ đảm bảo atomicity")
bullet("AI moderation phải bất đồng bộ hoàn toàn – không bao giờ block user request chờ model inference")
bullet("Context-aware NLP quan trọng hơn raw accuracy: tiếng Việt có nhiều từ nhập nhằng cần xử lý ngữ cảnh đặc biệt")
bullet("Multi-level cache cần chiến lược invalidation rõ ràng – thiếu invalidation dẫn đến stale data nghiêm trọng")
bullet("PKCE và HttpOnly cookies nên là mặc định cho mọi ứng dụng web, không phải tuỳ chọn")

doc.save("d:/Blur/Blur_Project_Report.docx")
print("Bao cao da duoc tao thanh cong: d:/Blur/Blur_Project_Report.docx")
