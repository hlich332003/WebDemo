# WebDemo - Hệ thống Quản lý Bán hàng E-commerce

## 📋 Giới thiệu Dự án

Dự án WebDemo là một hệ thống E-commerce toàn diện được xây dựng bằng **Angular** (Frontend) và **Spring Boot** (Backend), tích hợp đầy đủ các tính năng hiện đại như:

- 🔐 Xác thực & phân quyền với JWT
- 🔔 Thông báo real-time với WebSocket
- 🛒 Quản lý giỏ hàng & đơn hàng
- 📦 Xử lý bất đồng bộ với RabbitMQ
- ⚡ Cache với Redis
- 📊 Multi-database (jhipster_db + analytics_db)
- 🎨 Giao diện responsive Bootstrap

---

## 🏗️ Kiến trúc Hệ thống

### Frontend

- **Framework**: Angular 19.2.x
- **UI Library**: Bootstrap 5, Font Awesome
- **State Management**: RxJS
- **Real-time**: WebSocket (SockJS + STOMP)
- **HTTP Client**: Angular HttpClient với Interceptor

### Backend

- **Framework**: Spring Boot 3.x
- **Security**: Spring Security + JWT
- **Database**: SQL Server (2 databases)
  - `jhipster_db`: Dữ liệu nghiệp vụ chính
  - `analytics_db`: Logs, Analytics, Notifications
- **ORM**: JPA/Hibernate với Custom Queries & Stored Procedures
- **Messaging**: RabbitMQ (async processing)
- **Cache**: Redis
- **WebSocket**: STOMP over SockJS
- **Monitoring**: Actuator, Prometheus, Grafana

---

## 🗄️ Cấu trúc Database

### Database 1: `jhipster_db` (Dữ liệu nghiệp vụ chính)

#### 1. **jhi_user** - Bảng người dùng

```sql
- id (bigint, PK, Identity)
- login (nvarchar(50), Unique, Not Null)
- password_hash (nvarchar(60), Not Null)
- first_name (nvarchar(50))
- last_name (nvarchar(50))
- email (nvarchar(191), Unique)
- image_url (nvarchar(256))
- activated (bit, Not Null)
- lang_key (nvarchar(10))
- activation_key (nvarchar(20))
- reset_key (nvarchar(20))
- created_by (nvarchar(50), Not Null)
- created_date (datetime)
- reset_date (datetime)
- last_modified_by (nvarchar(50))
- last_modified_date (datetime)
```

#### 2. **jhi_authority** - Bảng quyền

```sql
- name (nvarchar(50), PK)
```

#### 3. **jhi_user_authority** - Bảng quan hệ User-Authority

```sql
- user_id (bigint, FK -> jhi_user)
- authority_name (nvarchar(50), FK -> jhi_authority)
- PK: (user_id, authority_name)
```

#### 4. **category** - Danh mục sản phẩm

```sql
- id (bigint, PK, Identity)
- name (nvarchar(100), Not Null)
- description (nvarchar(500))
- image_url (nvarchar(500))
- parent_id (bigint, FK -> category)
- created_at (datetime2, Not Null)
- updated_at (datetime2)
- is_active (bit, Default: 1)
```

#### 5. **product** - Sản phẩm

```sql
- id (bigint, PK, Identity)
- name (nvarchar(255), Not Null)
- description (nvarchar(max))
- price (decimal(10,2), Not Null)
- stock_quantity (int, Not Null)
- image_url (nvarchar(500))
- category_id (bigint, FK -> category)
- created_at (datetime2, Not Null)
- updated_at (datetime2)
- is_active (bit, Default: 1)
- sku (nvarchar(100))
- discount_price (decimal(10,2))
```

#### 6. **cart** - Giỏ hàng

```sql
- id (bigint, PK, Identity)
- user_id (nvarchar(255), Not Null, Unique)
- created_at (datetime2, Not Null)
- updated_at (datetime2)
```

#### 7. **cart_item** - Chi tiết giỏ hàng

```sql
- id (bigint, PK, Identity)
- cart_id (bigint, FK -> cart)
- product_id (bigint, FK -> product)
- quantity (int, Not Null)
- price (decimal(10,2), Not Null)
- created_at (datetime2, Not Null)
```

#### 8. **order** - Đơn hàng

```sql
- id (bigint, PK, Identity)
- user_id (nvarchar(255), Not Null)
- status (nvarchar(50), Not Null)
- total_amount (decimal(10,2), Not Null)
- shipping_address (nvarchar(500), Not Null)
- payment_method (nvarchar(50))
- created_at (datetime2, Not Null)
- updated_at (datetime2)
- notes (nvarchar(max))
- tracking_number (nvarchar(100))
```

#### 9. **order_item** - Chi tiết đơn hàng

```sql
- id (bigint, PK, Identity)
- order_id (bigint, FK -> order)
- product_id (bigint, FK -> product)
- quantity (int, Not Null)
- price (decimal(10,2), Not Null)
- product_name (nvarchar(255))
```

#### 10. **payment** - Thanh toán

```sql
- id (bigint, PK, Identity)
- order_id (bigint, FK -> order)
- amount (decimal(10,2), Not Null)
- payment_method (nvarchar(50), Not Null)
- status (nvarchar(50), Not Null)
- transaction_id (nvarchar(255))
- created_at (datetime2, Not Null)
```

#### 11. **review** - Đánh giá sản phẩm

```sql
- id (bigint, PK, Identity)
- product_id (bigint, FK -> product)
- user_id (nvarchar(255), Not Null)
- rating (int, Not Null)
- comment (nvarchar(max))
- created_at (datetime2, Not Null)
```

#### 12. **wishlist_item** - Sản phẩm yêu thích

```sql
- id (bigint, PK, Identity)
- user_id (nvarchar(255), Not Null)
- product_id (bigint, FK -> product)
- created_at (datetime2, Not Null)
```

#### 13. **refresh_token** - Token làm mới

```sql
- id (bigint, PK, Identity)
- user_id (nvarchar(255), Not Null)
- token (nvarchar(500), Not Null, Unique)
- expiry_date (datetime2, Not Null)
- created_at (datetime2, Not Null)
```

---

### Database 2: `analytics_db` (Logs & Analytics)

#### 1. **notification** - Thông báo (Real-time WebSocket)

```sql
- id (bigint, PK, Identity)
- message (nvarchar(500), Not Null)
- type (nvarchar(20), Not Null)
  * ORDER_CREATED: Đơn hàng mới (→ admin)
  * ORDER_CONFIRMED: Đơn hàng xác nhận (→ user)
  * ORDER_SHIPPED: Đơn hàng đang giao (→ user)
  * ORDER_DELIVERED: Đơn hàng đã giao (→ user)
  * PROMOTION: Khuyến mãi (→ all users)
  * SYSTEM: Thông báo hệ thống
- created_at (datetime2, Not Null)
- user_id (nvarchar(255), Not Null)
- is_read (bit, Not Null, Default: 0)
- link (nvarchar(500))
- action_type (nvarchar(50))
- related_id (bigint)
```

#### 2. **analytics_log** - Log phân tích hành vi

```sql
- id (bigint, PK, Identity)
- user_id (nvarchar(255))
- action (nvarchar(100), Not Null)
- entity_type (nvarchar(50))
- entity_id (bigint)
- ip_address (nvarchar(50))
- user_agent (nvarchar(500))
- created_at (datetime2, Not Null)
- metadata (nvarchar(max))
```

---

## 🔔 Hệ thống Thông báo Real-time (WebSocket)

### Luồng hoạt động

#### 1. **Kết nối WebSocket**

```typescript
// Angular: websocket.service.ts
connect() {
  const token = localStorage.getItem('authenticationToken');
  const socket = new SockJS('/ws');
  this.stompClient = Stomp.over(socket);

  this.stompClient.connect(
    { 'X-Authorization': `Bearer ${token}` },
    () => {
      // Subscribe notifications
      this.stompClient.subscribe('/user/queue/notifications', (message) => {
        this.handleNotification(JSON.parse(message.body));
      });
    }
  );
}
```

#### 2. **Backend gửi thông báo**

```java
// NotificationService.java
public void sendToUser(String userId, NotificationDTO notification) {
  notification.setCreatedAt(Instant.now());

  // Lưu vào DB
  Notification entity = notificationMapper.toEntity(notification);
  notificationRepository.save(entity);

  // Gửi qua WebSocket
  Map<String, Object> payload = createWebSocketPayload(notification);
  messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
}

```

#### 3. **Các loại thông báo**

**Admin nhận:**

- `ORDER_CREATED`: Khi có đơn hàng mới được tạo
- `ORDER_CANCELLED`: Khi khách hàng hủy đơn
- `LOW_STOCK`: Cảnh báo sản phẩm sắp hết hàng
- `NEW_REVIEW`: Đánh giá sản phẩm mới

**User nhận:**

- `ORDER_CONFIRMED`: Đơn hàng được xác nhận
- `ORDER_SHIPPED`: Đơn hàng đang vận chuyển
- `ORDER_DELIVERED`: Đơn hàng đã giao thành công
- `PROMOTION`: Khuyến mãi, giảm giá
- `WISHLIST_PRICE_DROP`: Sản phẩm yêu thích giảm giá

### Component hiển thị (notification-bell)

```
src/main/webapp/app/layouts/navbar/navbar.component.html
- Badge hiển thị số thông báo chưa đọc
- Dropdown list thông báo
- Click để đánh dấu đã đọc
- Link đến trang chi tiết
```

---

## 🚀 Cài đặt & Chạy Dự án

### Yêu cầu hệ thống

- Java 21+
- Node.js 20+
- SQL Server 2019+
- Maven 3.9+
- Redis (optional)
- RabbitMQ (optional)

### Bước 1: Clone & Cấu hình Database

```bash
# Clone project
git clone <repository-url>
cd WebDemo

# Tạo 2 databases trong SQL Server
CREATE DATABASE jhipster_db;
CREATE DATABASE analytics_db;

# Cập nhật connection string trong:
src/main/resources/config/application-dev.yml
```

### Bước 2: Cài đặt Dependencies

```powershell
# Backend
mvn clean install -DskipTests

# Frontend
npm install
```

### Bước 3: Chạy Backend

```powershell
# Development mode
mvn spring-boot:run

# Hoặc
./mvnw spring-boot:run
```

### Bước 4: Chạy Frontend

```powershell
npm start
# Application: http://localhost:9000
```

### Bước 5: Test WebSocket

1. Đăng nhập với user có quyền ADMIN
2. Kiểm tra chuông thông báo trên navbar
3. Tạo đơn hàng mới → Admin sẽ nhận thông báo real-time
4. Đăng nhập user khác → Nhận thông báo đơn hàng thành công

---

## 📁 Cấu trúc File Quan trọng

### Backend (Spring Boot)

#### Configuration

```
src/main/java/com/mycompany/myapp/config/
├── WebSocketConfig.java              # Cấu hình WebSocket STOMP
├── WebSocketSecurityConfig.java      # Security cho WebSocket
├── SecurityConfiguration.java         # JWT Security
├── DatabaseConfiguration.java         # Multi-database config
├── CacheConfiguration.java            # Redis cache
└── RabbitMQConfig.java               # RabbitMQ message queue
```

#### Domain (Entity)

```
src/main/java/com/mycompany/myapp/domain/
├── User.java                          # Entity người dùng
├── Authority.java                     # Entity quyền
├── Product.java                       # Entity sản phẩm
├── Category.java                      # Entity danh mục
├── Order.java                         # Entity đơn hàng
├── OrderItem.java                     # Chi tiết đơn hàng
├── Cart.java                          # Giỏ hàng
├── CartItem.java                      # Chi tiết giỏ hàng
├── Notification.java                  # Thông báo (analytics_db)
├── AnalyticsLog.java                  # Log phân tích (analytics_db)
├── Payment.java                       # Thanh toán
├── Review.java                        # Đánh giá
└── WishlistItem.java                  # Sản phẩm yêu thích
```

#### Service

```
src/main/java/com/mycompany/myapp/service/
├── NotificationService.java           # Quản lý thông báo WebSocket
├── OrderService.java                  # Logic đơn hàng
├── ProductService.java                # Logic sản phẩm
├── CartService.java                   # Logic giỏ hàng
├── UserService.java                   # Logic người dùng
└── messaging/
    ├── MessageProducerService.java    # Gửi message RabbitMQ
    └── MessageConsumerService.java    # Nhận message RabbitMQ
```

#### Controller (REST API)

```
src/main/java/com/mycompany/myapp/web/rest/
├── AccountResource.java               # API user profile
├── UserJWTController.java             # API đăng nhập JWT
├── ProductResource.java               # API sản phẩm
├── OrderResource.java                 # API đơn hàng
├── CartResource.java                  # API giỏ hàng
├── NotificationResource.java          # API thông báo
└── CategoryResource.java              # API danh mục
```

#### Repository

```
src/main/java/com/mycompany/myapp/repository/
├── UserRepository.java                # JPA Repository User
├── ProductRepository.java             # JPA + Custom Query
├── OrderRepository.java               # JPA + Stored Procedure
├── NotificationRepository.java        # Analytics DB
└── AnalyticsLogRepository.java        # Analytics DB
```

### Frontend (Angular)

#### Core Modules

```
src/main/webapp/app/
├── core/
│   ├── auth/                         # Authentication services
│   ├── interceptor/                  # HTTP Interceptors
│   │   ├── auth.interceptor.ts       # Thêm JWT token vào header
│   │   └── error-handler.interceptor.ts
│   └── util/                         # Utilities
│
├── shared/
│   ├── websocket/
│   │   └── websocket.service.ts      # WebSocket service
│   ├── notification/
│   │   └── notification.service.ts   # Notification service
│   └── language/                     # i18n
│
├── entities/
│   ├── product/                      # Module sản phẩm
│   ├── order/                        # Module đơn hàng
│   ├── cart/                         # Module giỏ hàng
│   ├── category/                     # Module danh mục
│   └── notification/                 # Module thông báo
│
├── layouts/
│   ├── navbar/
│   │   ├── navbar.component.ts       # Navbar với notification bell
│   │   └── navbar.component.html
│   ├── footer/
│   └── main/
│
└── admin/
    ├── user-management/              # Quản lý user
    ├── metrics/                      # Monitoring
    └── logs/                         # System logs
```

#### Routing

```
src/main/webapp/app/app.routes.ts     # Main routing
```

---

## 🎯 Các Tính năng Chính

### 1. Xác thực & Phân quyền

- ✅ Đăng ký tài khoản mới
- ✅ Đăng nhập với JWT
- ✅ Refresh token tự động
- ✅ Phân quyền ROLE_USER, ROLE_ADMIN
- ✅ Quên mật khẩu & reset password
- ✅ Kích hoạt tài khoản qua email

### 2. Quản lý Sản phẩm

- ✅ CRUD sản phẩm (Admin)
- ✅ Phân trang, tìm kiếm, lọc
- ✅ Upload ảnh sản phẩm
- ✅ Quản lý danh mục cây (parent-child)
- ✅ Quản lý tồn kho
- ✅ Giá khuyến mãi

### 3. Giỏ hàng & Đơn hàng

- ✅ Thêm/xóa/sửa giỏ hàng
- ✅ Đặt hàng với xác nhận
- ✅ Xử lý đơn hàng bất đồng bộ (RabbitMQ)
- ✅ Theo dõi trạng thái đơn hàng
- ✅ Lịch sử đơn hàng
- ✅ Hủy đơn hàng

### 4. Thông báo Real-time (WebSocket)

- ✅ Thông báo đơn hàng mới cho Admin
- ✅ Thông báo xác nhận đơn cho User
- ✅ Thông báo khuyến mãi
- ✅ Badge đếm số thông báo chưa đọc
- ✅ Dropdown hiển thị danh sách thông báo
- ✅ Đánh dấu đã đọc/chưa đọc

### 5. Đánh giá & Yêu thích

- ✅ Đánh giá sản phẩm (rating + comment)
- ✅ Danh sách sản phẩm yêu thích
- ✅ Thông báo khi giá giảm

### 6. Analytics & Logs

- ✅ Ghi log hành vi người dùng
- ✅ Theo dõi sản phẩm xem nhiều
- ✅ Dashboard thống kê (Admin)
- ✅ Export báo cáo

### 7. Performance Optimization

- ✅ Redis caching
- ✅ Lazy loading modules
- ✅ Image optimization
- ✅ Database indexing
- ✅ Connection pooling

---

## 🧪 Testing

### Backend Testing

```powershell
# Unit tests
mvn test

# Integration tests
mvn verify

# Test coverage
mvn clean test jacoco:report
```

### Frontend Testing

```powershell
# Unit tests (Jest)
npm test

# E2E tests
npm run e2e

# Test coverage
npm run test:coverage
```

---

## 📚 Lộ trình Đào tạo Người mới

### **1. Giới thiệu Dự án (1 ngày)**

- ✅ Mục tiêu: Hệ thống E-commerce với Angular + Spring Boot
- ✅ Đối tượng: Admin quản lý, User mua hàng
- ✅ Kiến trúc: Frontend (Angular) ↔ REST API ↔ Backend (Spring Boot) ↔ Database (SQL Server)
- ✅ Công nghệ: JWT, WebSocket, RabbitMQ, Redis

---

### **2. Frontend - Angular (2 tuần)**

#### **Tuần 1: Cơ bản**

- Cài đặt môi trường: Node.js, Angular CLI, VS Code
- TypeScript cơ bản: types, interfaces, classes
- Angular architecture: Modules, Components, Services, Directives
- Data binding: Property, Event, Two-way binding
- Routing & Navigation
- Forms: Reactive Forms, Template-driven Forms
- HTTP Client: Gọi REST API, error handling

#### **Tuần 2: Nâng cao**

- State Management: RxJS Observables, BehaviorSubject
- Lazy Loading: Tăng hiệu suất
- Interceptors: Thêm JWT token, xử lý lỗi
- WebSocket: SockJS + STOMP
- Testing: Jasmine, Karma
- Storage: LocalStorage, SessionStorage, IndexedDB, Cookies
- Service Worker: PWA basics

**Bài tập:** Xây dựng trang sản phẩm với giỏ hàng, kết nối API

---

### **3. Backend - Spring Boot (2 tuần)**

#### **Tuần 1: Cơ bản**

- Khởi tạo project: Spring Initializr, JHipster
- RESTful API: Controller, Service, Repository
- HTTP Methods: GET, POST, PUT, DELETE, PATCH
- JPA/Hibernate: Entity, CRUD operations
- Exception Handling: @ControllerAdvice, @ExceptionHandler
- Response chung: ResponseEntity, DTO pattern
- Database: Connection pooling, transactions

#### **Tuần 2: Nâng cao**

- Spring Security: JWT authentication, authorization
- Multi-database: jhipster_db + analytics_db
- Custom Queries: @Query, Native SQL
- Stored Procedures: @Procedure
- Validation: @Valid, custom validators
- Testing: JUnit, Mockito, MockMvc
- Swagger: API documentation
- Logging: SLF4J, Logback
- Cache: Redis integration
- Async: @Async, RabbitMQ

**Bài tập:** Tạo API đơn hàng với xác thực JWT, ghi log, cache

---

### **4. WebSocket & Real-time (3 ngày)**

- Cấu hình STOMP over SockJS
- Security cho WebSocket
- Gửi thông báo từ Backend → Frontend
- Hiển thị notification bell
- Subscribe/Unsubscribe topics
- Xử lý reconnection

**Bài tập:** Implement hệ thống thông báo real-time

---

### **5. RabbitMQ - Message Queue (3 ngày)**

- Khái niệm: Producer, Consumer, Queue, Exchange, Binding, Routing Key
- Cài đặt RabbitMQ: Docker, Management UI
- Tích hợp Spring Boot: RabbitTemplate, @RabbitListener
- Xử lý bất đồng bộ: Gửi email, xử lý đơn hàng
- Dead Letter Queue: Xử lý lỗi
- Retry mechanism

**Bài tập:** Xử lý đơn hàng bất đồng bộ qua RabbitMQ

---

### **6. Redis - Cache & Session (2 ngày)**

- Redis là gì: Key-value store, use cases
- Cài đặt Redis: Docker, Redis CLI
- Tích hợp Spring Boot: RedisTemplate, @Cacheable
- Cache strategy: Cache-Aside, Write-Through
- Session management: Spring Session Redis
- Pub/Sub: Real-time messaging

**Bài tập:** Cache kết quả tìm kiếm sản phẩm

---

### **7. SQL Server Nâng cao (1 tuần)**

- Window Functions: ROW_NUMBER, RANK, LAG/LEAD, SUM() OVER
- CTE: Common Table Expressions, Recursive CTEs
- Indexing: Clustered, Non-clustered, Composite
- Query optimization: Execution plan, EXPLAIN
- Partitioning: Range, List partitioning
- Stored Procedures: Input/Output parameters
- Triggers: AFTER, INSTEAD OF

**Bài tập:** Viết stored procedure báo cáo doanh thu theo tháng

---

### **8. Kỹ năng Mềm (Liên tục)**

- ✅ Giao tiếp: Standup meeting, code review
- ✅ Làm việc nhóm: Git workflow, Pull Request
- ✅ Giải quyết vấn đề: Debug, tìm root cause
- ✅ Tiếp thu feedback: Cải thiện code quality
- ✅ Quản lý thời gian: Kanban board, task estimation

---

### **9. Bài tập Tổng hợp (6 tuần)**

#### **Yêu cầu: Xây dựng hệ thống E-commerce hoàn chỉnh**

**Chức năng:**

- ✅ Đăng ký / Đăng nhập (JWT)
- ✅ Quản lý danh mục sản phẩm (CRUD)
- ✅ Quản lý sản phẩm (CRUD, import Excel)
- ✅ Quản lý khách hàng (CRUD)
- ✅ Giỏ hàng (thêm/xóa/sửa)
- ✅ Đặt hàng (bất đồng bộ với RabbitMQ)
- ✅ Thông báo real-time (WebSocket)
- ✅ Phân quyền (Admin/User)
- ✅ Audit log (created_by, created_date, modified_by, modified_date)

**Yêu cầu kỹ thuật:**

- ✅ Backend: Spring Boot với Bean configuration
- ✅ Exception Handling: Custom exceptions, global handler
- ✅ Interceptor: Logging, JWT validation (Angular + Spring)
- ✅ Database: JPA, Custom SQL, Stored Procedures
- ✅ Multi-database: jhipster_db + analytics_db
- ✅ WebSocket: Notification system
- ✅ RabbitMQ: Async order processing
- ✅ Redis: Cache product list
- ✅ Logging: Request/Response logs
- ✅ Custom Aspect: @Logging annotation
- ✅ Testing: Unit tests, Integration tests

**Điều kiện lên Thử việc:**

- ✅ Hoàn thành dự án trong 6 tuần
- ✅ Kanban: 50% công việc trên + 50% dự án thực tế
- ✅ Code review pass
- ✅ Done 40 task mức medium

---

## 🔧 Troubleshooting

### WebSocket không kết nối

```typescript
// Kiểm tra token
console.log(localStorage.getItem('authenticationToken'));

// Kiểm tra STOMP connection
stompClient.debug = str => console.log(str);
```

### Database connection failed

```yaml
# Check application-dev.yml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=jhipster_db;encrypt=false
    username: sa
    password: your_password
```

### RabbitMQ connection refused

```bash
# Start RabbitMQ with Docker
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
# Access: http://localhost:15672 (guest/guest)
```

---

## 📖 Tài liệu Tham khảo

- [Angular Documentation](https://angular.io/docs)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security JWT](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [WebSocket with STOMP](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [Redis Documentation](https://redis.io/documentation)
- [JHipster](https://www.jhipster.tech/)

---

## 👥 Team

- **Backend Developer**: Spring Boot, SQL Server, WebSocket
- **Frontend Developer**: Angular, TypeScript, Bootstrap
- **DevOps**: Docker, CI/CD, Monitoring

---

## 📝 License

This project is licensed under the MIT License.

---

## 📞 Contact

- **Email**: support@webdemo.com
- **Slack**: #webdemo-support

---

**Cập nhật lần cuối**: 22/12/2025
