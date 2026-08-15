# 🤖 AI Agent Platform (Spring Boot & Spring AI & MySQL)

Hệ thống **AI Agent Platform** mở rộng, tự động hóa (Autonomous Agent) hỗ trợ thực hiện Code Review và các công việc AI kỹ thuật phần mềm, được xây dựng trên nền tảng **Spring Boot 3.x**, **Spring AI**, **Spring Security (JWT)** và **MySQL Database** (hỗ trợ cột JSON đa hình).

Khác biệt hoàn toàn so với các ứng dụng RAG (Retrieval-Augmented Generation) thông thường, hệ thống này cấp cho AI khả năng truy cập **Tool Calling**. Thay vì gửi toàn bộ source code qua API (dễ gây tràn RAM và Token limit), Agent được cấp đường dẫn dự án (`projectPath`) và **tự động sử dụng các File System Tools (List Directory, Read File, Grep Text)** để tự đọc, khám phá codebase và lập luận đưa ra báo cáo chi tiết.

---

## 🏗️ Kiến trúc & Cấu trúc Mã nguồn (Project Structure)

Dự án được thiết kế theo mô hình **Modular Layered Architecture (Phân tầng N-Tier)** với luồng xử lý bất đồng bộ độc lập (True Async Execution Worker).

### 📂 Cấu trúc Thư mục Chi tiết (`src/main/java/prj/anhzxje/aiagent/`)

```text
src/main/java/prj/anhzxje/aiagent/
├── agent/                  # CORE AI ENGINE (Spring AI Tool Calling & Reasoning)
│   └── CodeReviewAgent.java      # Reasoner chính, tích hợp File System Tools
├── config/                 # HỆ THỐNG CẤU HÌNH
│   ├── AgentConfig.java          # Cấu hình Spring AI, ObjectMapper & ThreadPool TaskExecutor
│   └── SecurityConfig.java       # Cấu hình Spring Security Stateless JWT Filter
├── controller/             # TẦNG REST API ENDPOINTS
│   ├── AuthController.java       # POST /api/auth/register, /api/auth/login
│   ├── ProjectController.java    # CRUD /api/projects (Quản lý dự án của tôi)
│   └── AgentTaskController.java  # POST, GET /api/tasks (Tạo & xem tiến độ AI Task)
├── dto/                    # DATA TRANSFER OBJECTS (Sub-packaged theo Domain)
│   ├── auth/                     # LoginRequest, RegisterRequest, AuthResponse
│   ├── project/                  # ProjectRequest, ProjectResponse
│   ├── task/                     # AgentTaskRequest, AgentTaskResponse
│   └── common/                   # ErrorResponse
├── entity/                 # DATABASE DOMAIN ENTITIES (JPA / Hibernate)
│   ├── User.java                 # Bảng users
│   ├── Project.java              # Bảng projects (N-1 với User)
│   └── AgentTask.java            # Bảng agent_tasks (N-1 User, N-1 Project, cột JSON input/output)
├── enums/                  # DOMAIN ENUMS
│   ├── Role.java                 # USER, ADMIN
│   ├── TaskType.java             # CODE_REVIEW, CODE_FIX, TEST_GENERATION...
│   ├── JobStatus.java            # PENDING, PROCESSING, COMPLETED, ERROR
│   ├── Category.java             # BUG, SECURITY, PERFORMANCE, CODE_QUALITY
│   └── Severity.java             # CRITICAL, HIGH, MEDIUM, LOW
├── exception/              # XỬ LÝ LỖI TOÀN CỤC (Global Exception Handling)
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── repository/             # TẦNG TRUY XUẤT DỮ LIỆU (Spring Data JPA)
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   └── AgentTaskRepository.java
├── security/               # XÁC THỰC BẢO MẬT & JWT ADAPTER
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
└── service/                # TẦNG NGHIỆP VỤ & WORKER BẤT ĐỒNG BỘ
    ├── ProjectService.java       # Nghiệp vụ quản lý dự án & kiểm tra ownership
    ├── AgentTaskService.java     # Quản lý lifecycle của AgentTask
    └── AgentTaskExecutor.java    # ThreadPool Async Worker thực thi Agent ngầm
```

---

## 🌟 Tính năng nổi bật

1. **Autonomous Agent Tool Calling**: Agent tự động gọi các công cụ `ListDirectoryTool`, `GlobTool`, `FileSystemTools`, `GrepTool` để đọc mã nguồn thực tế trên ổ đĩa Server/Local.
2. **Polymorphic Database Persistence (MySQL JSON)**: Sử dụng `@JdbcTypeCode(SqlTypes.JSON)` trên MySQL cho phép lưu trữ input/output dạng JSON linh hoạt cho bất kỳ loại AI Agent nào trong tương lai (`CODE_REVIEW`, `CODE_FIX`, `TEST_GEN`...).
3. **Stateless JWT Authentication**: Đăng ký, đăng nhập bảo mật với mã hóa BCrypt password và cấp phát JWT Token.
4. **Multi-tenant Ownership Control**: Đảm bảo an toàn dữ liệu, ngăn chặn User B truy cập hoặc can thiệp vào Project/Task của User A.
5. **True Async Worker Architecture**: API `POST /api/tasks` phản hồi tức thì dạng `PENDING` trong **< 50ms**, AI Agent tự động chạy ngầm trên ThreadPool độc lập (`AgentTaskExecutor`) và ghi kết quả `COMPLETED` vào MySQL.
6. **Robust Error Handling**: Trả về định dạng lỗi chuẩn JSON (HTTP 400, 401, 403, 404, 500) thống nhất qua `GlobalExceptionHandler`.

---

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ**: Java 21
- **Framework**: Spring Boot 3.4+ / 4.1
- **AI Framework**: Spring AI (`spring-ai-starter-model-google-genai`) & `spring-ai-agent-utils`
- **Security**: Spring Security & JJWT (Java JWT)
- **Database**: MySQL 8.x + Spring Data JPA (Hibernate 7.x)
- **Utilities**: Lombok, Jackson (ObjectMapper)

---

## 🚀 Hướng dẫn Cài đặt & Chạy Dự án

### 1. Yêu cầu Hệ thống
- Java 21+
- Maven 3.8+
- MySQL Server 8.0+
- Gemini API Key từ [Google AI Studio](https://aistudio.google.com/)

### 2. Khởi tạo Database (MySQL)
Mở MySQL Workbench hoặc Command Line và chạy lệnh:
```sql
CREATE DATABASE aiagent_db;
```

### 3. Cấu hình Biến môi trường
Cài đặt biến môi trường cho Gemini API Key trong hệ điều hành hoặc file cấu hình:
```bash
# Windows PowerShell
$env:GOOGLE_API_KEY="AIzaSyYourGeminiApiKeyHere"

# Linux / macOS
export GOOGLE_API_KEY="AIzaSyYourGeminiApiKeyHere"
```

Cập nhật thông số kết nối MySQL trong `src/main/resources/application.yml` (nếu cần):
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aiagent_db?createDatabaseIfNotExist=true
    username: root
    password: root_password
```

### 4. Build và Chạy Ứng dụng
```bash
# Biên dịch mã nguồn
./mvnw clean compile

# Chạy ứng dụng Spring Boot
./mvnw spring-boot:run
```
Ứng dụng sẽ khởi chạy mặc định tại cổng `http://localhost:8080`.

---

## 📖 Hướng dẫn sử dụng REST APIs

Toàn bộ các API chính ngoại trừ `/api/auth/**` đều yêu cầu kèm Header:
`Authorization: Bearer <JWT_TOKEN>`

### 1. Xác thực (Auth Flow)
- **POST** `/api/auth/register`: Đăng ký tài khoản.
  ```json
  { "username": "dev1", "email": "dev1@test.com", "password": "password123" }
  ```
- **POST** `/api/auth/login`: Đăng nhập lấy JWT Token.
  ```json
  { "username": "dev1", "password": "password123" }
  ```

### 2. Quản lý Dự án (Project API)
- **POST** `/api/projects`: Tạo dự án mới.
  ```json
  {
    "name": "My App Core",
    "description": "Dự án Spring Boot",
    "path": "D:/projects/my-app",
    "language": "Java"
  }
  ```
- **GET** `/api/projects`: Lấy danh sách dự án của tôi.
- **GET** `/api/projects/{id}`: Lấy thông tin dự án theo ID.

### 3. Quản lý AI Agent Task (Task API)
- **POST** `/api/tasks`: Khởi tạo AI Agent Task mới (Phản hồi tức thì `PENDING`).
  ```json
  {
    "projectId": 1,
    "type": "CODE_REVIEW",
    "input": "{\"projectPath\":\"D:/projects/my-app\",\"focus\":\"security\"}"
  }
  ```
- **GET** `/api/tasks/{id}`: Lấy chi tiết và kết quả AI Agent Task (chờ đến khi status `COMPLETED`).
- **GET** `/api/tasks`: Lấy danh sách tất cả Task của tôi.
