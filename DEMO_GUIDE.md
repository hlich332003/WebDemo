# 🎬 HƯỚNG DẪN DEMO DỰ ÁN WEBDEMO

**Dự án:** Angular + Spring Boot E-commerce  
**Ngày:** 24/11/2025  
**Thời gian demo:** 15-20 phút

---

## 📋 CHUẨN BỊ TRƯỚC KHI DEMO

### 1. Khởi động Services

```powershell
# Mở Docker Desktop
# Khởi động Redis + RabbitMQ

# Terminal 1: Backend
cd C:\Users\Admin\Desktop\WebDemo
./mvnw

# Terminal 2: Frontend
npm start

# Chờ đến khi thấy:
# Backend: "Started WebDemoApp in X seconds"
# Frontend: "Compiled successfully"
```

### 2. Kiểm tra Services

✅ **Backend:** http://localhost:8080  
✅ **Frontend:** http://localhost:9000  
✅ **RabbitMQ UI:** http://localhost:15672 (guest/guest)  
✅ **SQL Server:** Kết nối bằng SSMS

### 3. Chuẩn bị dữ liệu test

```sql
-- Kiểm tra có sản phẩm
SELECT TOP 10 * FROM product ORDER BY id;

-- Nếu cần, thêm sản phẩm test
INSERT INTO product (name, price, quantity, image_url, description, category_id)
VALUES
  ('Laptop Gaming', 25000000, 10, 'https://via.placeholder.com/300', 'Laptop chơi game', 1),
  ('Mouse Wireless', 500000, 2, 'https://via.placeholder.com/300', 'Chuột không dây', 2);
```

---

## 🎯 KỊCH BẢN DEMO (15 PHÚT)

### PHẦN 1: GIỚI THIỆU DỰ ÁN (2 phút)

**Nói:**

> "Dự án WebDemo là ứng dụng e-commerce fullstack với Angular frontend và Spring Boot backend.
>
> Công nghệ chính:
>
> - **Frontend:** Angular 19 với TypeScript
> - **Backend:** Spring Boot 3.4, Spring Security
> - **Database:** SQL Server
> - **Message Queue:** RabbitMQ
> - **Cache:** Redis
> - **Authentication:** JWT"

**Mở browser:**

- Trang chủ: http://localhost:9000
- Giới thiệu giao diện: Header, danh mục, sản phẩm

---

### PHẦN 2: DEMO AUTHENTICATION & AUTHORIZATION (3 phút)

#### 2.1 Đăng ký tài khoản mới

**Thao tác:**

1. Click "Đăng ký"
2. Nhập:
   - Username: `demo123`
   - Email: `demo@example.com`
   - Password: `Pass@123`
3. Submit

**Mở F12 → Network tab:**

```
POST /api/register
Request: { "login": "demo123", "email": "demo@example.com", "password": "Pass@123" }
Response: 201 Created
```

**Nói:**

> "Backend validate dữ liệu (email format, password strength) với Global Exception Handler."

---

#### 2.2 Đăng nhập & JWT Token

**Thao tác:**

1. Đăng nhập với tài khoản vừa tạo
2. Mở F12 → Application → Session Storage
3. Tìm key `authenticationToken`

**Giải thích:**

> "Token JWT được lưu trong sessionStorage. Khi đóng tab sẽ mất, phải login lại.
>
> Token có 3 phần: Header.Payload.Signature
>
> Mỗi request sau đó sẽ gửi kèm token trong Authorization header."

**Mở Network → Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

#### 2.3 Phân quyền Admin

**Thao tác:**

1. Đăng nhập với admin (username: `admin`, pass: `admin`)
2. Navbar xuất hiện menu "Quản trị"
3. Click vào → Thấy các trang: Quản lý sản phẩm, Đơn hàng, Người dùng

**Giải thích:**

> "Spring Security kiểm tra role trong JWT token:
>
> - ROLE_USER → Chỉ xem sản phẩm, đặt hàng
> - ROLE_ADMIN → Quản lý toàn bộ hệ thống"

**Code minh họa (JWTFilter.java):**

```java
// Backend check authority
.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
```

---

### PHẦN 3: DEMO VALIDATION & GIỚI HẠN SỐ LƯỢNG (4 phút)

#### 3.1 Setup sản phẩm test

**Vào SQL Server:**

```sql
-- Set sản phẩm chỉ còn 2 cái
UPDATE product
SET quantity = 2
WHERE id = 1;
```

**Refresh trang chủ:**

> Thấy hiển thị: "⚠️ Chỉ còn 2 sản phẩm"

---

#### 3.2 Test thêm vào giỏ hàng

**Thao tác:**

1. Click "Thêm vào giỏ" → ✅ Thành công (giỏ có 1)
2. Click lần 2 → ✅ Thành công (giỏ có 2)
3. Click lần 3 → ❌ Hiển thị: "⚠️ Không thể thêm sản phẩm vào giỏ hàng!"

**Giải thích:**

> "Frontend validate trong `cart.service.ts`:
>
> - Check quantity còn trong kho
> - Check số lượng đã có trong giỏ
> - Không cho vượt quá"

**Mở F12 → Console:**

```
CartService: Cannot add 1. Already have 2 in cart (max 2 available)
```

---

#### 3.3 Test tăng số lượng trong giỏ

**Thao tác:**

1. Vào giỏ hàng
2. Thử click nút "+" (tăng số lượng)
3. Hiển thị: "⚠️ Đã đạt giới hạn số lượng!"

**Giải thích:**

> "Mọi thao tác đều được validate:
>
> - Thêm vào giỏ
> - Tăng/giảm số lượng
> - Nhập số lượng thủ công"

---

### PHẦN 4: DEMO RABBITMQ - GỬI EMAIL BẤT ĐỒNG BỘ (3 phút)

#### 4.1 Đặt hàng

**Thao tác:**

1. Trong giỏ hàng, click "Thanh toán"
2. Điền thông tin:
   - Tên: Test User
   - Email: test@example.com
   - Địa chỉ: 123 Test Street
3. Click "Đặt hàng"
4. API trả về ngay (< 1 giây)

**Giải thích:**

> "Backend không chờ email gửi xong mới trả response.
>
> Thay vào đó, gửi message vào RabbitMQ queue."

---

#### 4.2 Kiểm tra RabbitMQ

**Mở RabbitMQ UI:** http://localhost:15672

**Thao tác:**

1. Login: guest/guest
2. Vào tab "Queues and Streams"
3. Tìm queue: `order.email.queue`
4. Thấy:
   - Messages: Tăng lên (nếu email chưa gửi)
   - Consumers: 1 (EmailConsumer đang lắng nghe)

**Click vào queue → Get messages:**

```json
{
  "to": "test@example.com",
  "subject": "Order Confirmation",
  "body": "Your order #12345 has been placed successfully"
}
```

**Giải thích:**

> "Consumer (EmailConsumer.java) nhận message và gửi email thực tế.
>
> Nếu fail → Retry tự động hoặc vào Dead Letter Queue."

---

#### 4.3 Check backend logs

**Terminal backend:**

```
2025-11-24 10:30:15 INFO  OrderService - Order created: #12345
2025-11-24 10:30:15 INFO  OrderService - Sent email message to queue
2025-11-24 10:30:16 INFO  EmailConsumer - Received email message: test@example.com
2025-11-24 10:30:17 INFO  EmailConsumer - Email sent successfully
```

---

### PHẦN 5: DEMO REDIS CACHING (2 phút)

#### 5.1 Test cache

**Thao tác:**

1. Vào trang "Tất cả sản phẩm"
2. Mở backend logs → Thấy: `Loading products from database...`
3. Reload trang (F5)
4. Logs **KHÔNG** hiển thị `Loading products from database...`

**Giải thích:**

> "Lần 1: Query database → Lưu vào Redis (TTL = 30 giây)  
> Lần 2-N: Lấy từ Redis (nhanh hơn 10-100 lần)"

---

#### 5.2 Test TTL (Time To Live)

**Chờ 30 giây → Reload lại:**

- Logs hiển thị lại: `Loading products from database...`

**Giải thích:**

> "Sau 30 giây, Redis tự động xóa cache → Lần tiếp query DB lại.
>
> Tại sao 30 giây?
>
> - Đủ nhanh để user không thấy dữ liệu cũ sau khi đặt hàng
> - Giảm load cho database"

---

### PHẦN 6: DEMO TÍNH NĂNG TÌM KIẾM (2 phút)

#### 6.1 Test debounce search

**Thao tác:**

1. Vào "Tất cả sản phẩm"
2. Gõ nhanh vào ô search: "l-a-p-t-o-p"
3. Mở F12 → Network

**Quan sát:**

- Chỉ thấy 1 request sau khi gõ xong
- Request gửi sau 500ms

**Giải thích:**

> "Debounce: Chờ user gõ xong 500ms mới gọi API.
>
> Lợi ích:
>
> - Giảm số API calls (6 lần → 1 lần)
> - UX mượt mà hơn
> - Server nhẹ hơn"

---

#### 6.2 Test clear search

**Thao tác:**

1. Sau khi search → Click nút "✕" (clear)
2. Tất cả sản phẩm hiển thị lại

---

### PHẦN 7: DEMO GLOBAL EXCEPTION HANDLING (2 phút)

#### 7.1 Test validation error

**Thao tác:**

1. Đăng ký với email không hợp lệ: `test123` (không có @)
2. Mở F12 → Network → Response:

```json
{
  "message": "Validation failed: email: must be a valid email",
  "status": 400,
  "error": "Bad Request",
  "timestamp": "2025-11-24T10:30:00",
  "path": "/api/register"
}
```

**Giải thích:**

> "GlobalExceptionHandler bắt tất cả exception và trả về response thống nhất."

---

#### 7.2 Test business logic error

**Thao tác:**

1. Set sản phẩm quantity = 0
2. Thử đặt hàng
3. Response:

```json
{
  "message": "Insufficient stock for Laptop Gaming. Available: 0",
  "status": 400,
  "error": "Business Error",
  "timestamp": "2025-11-24T10:30:00",
  "path": "/api/orders"
}
```

---

## 🎓 CÂU HỎI DEMO (Dự đoán & Trả lời)

### Q1: "JWT token lưu ở đâu? Nếu user đóng tab thì sao?"

**A:**

> "Token lưu trong `sessionStorage`. Nếu đóng tab → Mất token → Phải login lại.
>
> Nếu muốn lưu lâu dài hơn → Dùng `localStorage` (tồn tại đến khi xóa thủ công).
>
> Trade-off:
>
> - sessionStorage: Bảo mật hơn
> - localStorage: UX tốt hơn (không phải login lại)"

---

### Q2: "Nếu 2 user cùng mua sản phẩm cuối cùng thì sao?"

**A:**

> "Backend validate khi tạo order:
>
> ```java
> if (product.getQuantity() < orderQty) {
>     throw new BusinessException('Insufficient stock');
> }
> ```
>
> User A checkout trước → Thành công  
> User B checkout sau → Fail với message 'Insufficient stock'"

---

### Q3: "RabbitMQ có retry không? Nếu email fail thì sao?"

**A:**

> "Có 2 cơ chế:
>
> 1. **Auto Retry:** RabbitMQ retry 3 lần (configurable)
> 2. **Dead Letter Queue (DLQ):** Sau 3 lần fail → Message vào DLQ để admin xử lý thủ công
>
> Hiện tại project đang dùng retry mặc định."

---

### Q4: "Redis cache bị xóa khi restart app?"

**A:**

> "Có! Redis là in-memory database.
>
> Nếu muốn persist:
>
> - Enable RDB snapshot (save to disk mỗi X phút)
> - Hoặc AOF (Append Only File) - log mọi write operation
>
> Trade-off:
>
> - Không persist: Nhanh nhất
> - Có persist: An toàn hơn nhưng chậm hơn"

---

### Q5: "Debounce 500ms có quá lâu không?"

**A:**

> "500ms là sweet spot:
>
> - Dưới 300ms: Vẫn gọi API quá nhiều
> - Trên 1000ms: User cảm giác lag
>
> Có thể tune tùy use case:
>
> - Search gợi ý: 300ms
> - Filter phức tạp: 700ms"

---

## 📊 CHECKLIST DEMO

### Trước khi demo:

- [ ] Docker Desktop đang chạy
- [ ] Redis container running
- [ ] RabbitMQ container running
- [ ] Backend đã start (./mvnw)
- [ ] Frontend đã start (npm start)
- [ ] Database có dữ liệu test
- [ ] Đã test đăng nhập admin/user
- [ ] Mở sẵn RabbitMQ UI (http://localhost:15672)
- [ ] Mở sẵn SSMS (SQL Server)

### Trong khi demo:

- [ ] Giới thiệu dự án (2 phút)
- [ ] Demo Authentication & JWT (3 phút)
- [ ] Demo Validation số lượng (4 phút)
- [ ] Demo RabbitMQ async (3 phút)
- [ ] Demo Redis caching (2 phút)
- [ ] Demo Search với debounce (2 phút)
- [ ] Demo Exception Handling (2 phút)
- [ ] Q&A (5 phút)

### Sau demo:

- [ ] Dừng backend (Ctrl+C)
- [ ] Dừng frontend (Ctrl+C)
- [ ] Commit code lên Git (nếu có thay đổi)

---

## 🔥 TIPS DEMO THÀNH CÔNG

### 1. Chuẩn bị kỹ

- Test toàn bộ flow 1-2 lần trước khi demo thật
- Có data backup nếu cần reset

### 2. Tự tin

- Nói chậm, rõ ràng
- Giải thích "WHY" (tại sao dùng công nghệ này), không chỉ "WHAT"

### 3. Xử lý sự cố

- Nếu lỗi → Bình tĩnh check logs
- Nếu không fix được ngay → Skip sang phần khác, quay lại sau

### 4. Tương tác

- Hỏi audience: "Các bạn có thắc mắc gì không?"
- Khuyến khích đặt câu hỏi

### 5. Highlight điểm mạnh

- ✅ Security: JWT + Validation 2 layers
- ✅ Performance: Redis cache + Debounce
- ✅ Scalability: RabbitMQ + Microservice ready
- ✅ Code quality: Clean architecture + Exception handling

---

## 📝 TÓM TẮT

### Đã demo được:

✅ **Authentication:** JWT token, phân quyền  
✅ **Validation:** Giới hạn số lượng 2 layers (FE + BE)  
✅ **RabbitMQ:** Gửi email bất đồng bộ  
✅ **Redis:** Cache với TTL  
✅ **Debounce:** Tối ưu search  
✅ **Exception Handling:** Response thống nhất

### Thời gian:

- **Demo:** 15 phút
- **Q&A:** 5 phút
- **Total:** 20 phút

---

**CHÚC BẠN DEMO THÀNH CÔNG! 🎉**

---

_Ngày: 24/11/2025_  
_Version: 1.0.0_  
_Author: GitHub Copilot_
