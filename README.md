# 🤖 AI Agent Code Review System (Spring AI)

Một hệ thống **AI Agent tự động hóa (Autonomous Agent)** hỗ trợ thực hiện Code Review cho các dự án phần mềm, được xây dựng trên nền tảng **Spring Boot** và **Spring AI**, kết hợp sức mạnh của **Google Gemini**.

Khác biệt hoàn toàn so với các ứng dụng RAG (Retrieval-Augmented Generation) thông thường, hệ thống này cấp cho AI khả năng truy cập Tool Calling. Thay vì nhận toàn bộ source code thông qua Request (dễ gây tràn RAM và Token), Agent sẽ được cung cấp đường dẫn dự án (`projectPath`) và **tự động sử dụng các File System Tools (List, Read, Grep)** để khám phá kiến trúc, đọc các file cần thiết và đưa ra báo cáo Code Review chuyên sâu.

---

## 🌟 Tính năng nổi bật

- **Autonomous Tool Calling**: Agent sử dụng thư viện `spring-ai-agent-utils` để tự động List Directory, Read File và Grep Text trong toàn bộ codebase.
- **Asynchronous Job Pattern**: Quá trình phân tích code tốn rất nhiều thời gian. Hệ thống xử lý qua luồng Background (Async) với HTTP 202 Accepted và Polling để tránh Timeout (504 Gateway Timeout).
- **In-memory Job Tracking**: Hỗ trợ xem tiến độ công việc (PENDING, PROCESSING, COMPLETED, ERROR) với tính năng dọn dẹp bộ nhớ định kỳ chống rò rỉ RAM (Memory Leak).
- **Token Protection & Security**: Prompt được cấu hình cực kỳ chặt chẽ, buộc Agent phải **bỏ qua** các thư mục rác (như `node_modules`, `target`, `.git`) và các file binary lớn để tiết kiệm chi phí Token API và chống quá tải LLM.
- **Resilient JSON Parsing**: Xử lý triệt để tình trạng LLM bọc kết quả JSON trong thẻ markdown (````json ... ````) giúp Backend luôn trả về JSON sạch sẽ.
- **Hạ ngưỡng Safety Threshold**: Tùy chỉnh `GoogleGenAiSafetySetting` giúp Agent không bị chặn lầm khi phát hiện hoặc phân tích các lỗ hổng bảo mật nhạy cảm.

---

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ**: Java 21
- **Framework**: Spring Boot 3.x
- **AI Framework**: Spring AI (`spring-ai-starter-model-google-genai`)
- **Agent Tools**: `spring-ai-agent-utils`
- **Validation**: Hibernate Validator (Spring Boot Validation)
- **Utilities**: Lombok, Jackson (ObjectMapper)

---

## 🚀 Hướng dẫn cài đặt & Chạy dự án

### 1. Yêu cầu hệ thống
- Java 21+
- Maven 3.8+
- API Key của Google Gemini (Google AI Studio)

### 2. Cấu hình
Bạn cần thiết lập biến môi trường cho API Key của Gemini. Mở file `application.properties` hoặc thiết lập biến môi trường OS:
```properties
spring.ai.google.genai.api-key=YOUR_GEMINI_API_KEY
```

### 3. Build và khởi chạy
Mở terminal tại thư mục gốc của dự án và chạy lệnh:
```bash
# Build dự án
./mvnw clean compile

# Chạy ứng dụng Spring Boot
./mvnw spring-boot:run
```
Ứng dụng sẽ khởi chạy mặc định tại cổng `8080`.

---

## 📖 Hướng dẫn sử dụng API (Postman)

Hệ thống sử dụng cơ chế Asynchronous (Polling). Để thực hiện một code review, bạn cần làm theo 2 bước:

### Bước 1: Khởi tạo Job (Submit Review)
Gửi yêu cầu phân tích một dự án nằm trên ổ cứng của Server/Local.
- **Endpoint**: `POST /api/review`
- **Body**:
  ```json
  {
      "projectPath": "D:/projects/my-spring-app",
      "focus": "security" // (Tùy chọn: security, performance, code_quality, bug)
  }
  ```
- **Response** (HTTP 202 Accepted): Trả về `jobId`.
  ```json
  {
      "jobId": "8f8b9e3d-74d6-4444-93cd-567a22ab1a92",
      "message": "Job đã được đưa vào hàng đợi xử lý."
  }
  ```

### Bước 2: Theo dõi tiến độ & Lấy kết quả
Sử dụng `jobId` nhận được ở Bước 1 để kiểm tra (Polling cách nhau mỗi 10-15s).
- **Endpoint**: `GET /api/review/{jobId}`
- **Response** (Job đang chạy):
  ```json
  {
      "jobId": "8f8b9e3d-74d6-4444-93cd-567a22ab1a92",
      "status": "PROCESSING",
      ...
  }
  ```
- **Response** (Job hoàn thành):
  ```json
  {
      "jobId": "8f8b9e3d-74d6-4444-93cd-567a22ab1a92",
      "status": "COMPLETED",
      "result": {
          "totalIssues": 2,
          "issues": [
              {
                  "severity": "HIGH",
                  "category": "SECURITY",
                  "file": "SecurityConfig.java",
                  "description": "Lộ lọt thông tin nhạy cảm...",
                  "recommendation": "Sử dụng Environment Variables."
              }
          ],
          "suggestions": ["Cấu hình thêm CSRF token."]
      },
      ...
  }
  ```

---

## 📂 Cấu trúc thư mục lõi

```text
src/main/java/prj/anhzxje/aiagent/
├── agent/            # Logic giao tiếp với Google Gemini (Tools, Prompt)
├── config/           # Cấu hình Spring (Async ThreadPool, Scheduling)
├── controller/       # HTTP REST APIs
├── dto/              # Data Transfer Objects (Request/Response)
├── enums/            # Các giá trị Enum (JobStatus, Severity, Category)
├── model/            # Cấu trúc đối tượng nghiệp vụ (ReviewJob)
└── service/          # Business logic, quản lý Jobs bộ nhớ RAM
```

---

## 🔮 Hướng phát triển tương lai (Roadmap)
- [ ] Chuyển đổi lưu trữ `ReviewJob` từ RAM xuống cơ sở dữ liệu (PostgreSQL / MySQL) bằng Spring Data JPA.
- [ ] Tích hợp bảo mật API bằng Spring Security và JWT Authentication.
- [ ] Mở rộng khả năng đọc mã nguồn trực tiếp từ GitHub/GitLab thông qua Personal Access Token thay vì bắt buộc phải clone source về Local.
- [ ] Bổ sung giao diện Frontend (React/Vue) để hiển thị báo cáo dưới dạng Dashboard trực quan.
