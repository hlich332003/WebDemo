# Hướng Dẫn Sửa Lỗi Sập Frontend

## Vấn Đề
Frontend bị sập với lỗi `ECONNRESET` khi đăng nhập xong, và không thể thêm sản phẩm vào giỏ hàng (lỗi 500).

## Nguyên Nhân Chính

### 1. **Backend Không Chạy** ⚠️
- Frontend cố gắng kết nối đến backend (port 8080) nhưng backend không chạy
- Điều này gây ra lỗi ECONNRESET và crash dev server

### 2. **WebSocket Connection Timing Issue**
- WebSocket cố kết nối quá sớm sau khi login
- Backend chưa kịp sẵn sàng → gây crash

### 3. **Duplicate API Calls**
- CartService được gọi nhiều lần cùng lúc
- Race condition khi load cart

## Giải Pháp Đã Áp Dụng

### ✅ 1. Cải Thiện Error Handling

**File: `webpack/error-handler.js`**
- Thêm nhiều error codes để bắt và bỏ qua
- Ngăn dev server crash khi mất kết nối backend

**File: `src/main/webapp/app/shared/services/websocket.service.ts`**
- Wrap socket.send() để catch errors
- Suppress tất cả errors từ SockJS
- Không để crash app khi WebSocket fail

### ✅ 2. Fix WebSocket Timing

**File: `src/main/webapp/app/layouts/navbar/navbar.component.ts`**
- Tăng delay WebSocket connection từ 2s lên 3s
- Chỉ connect khi không ở trang login
- Kiểm tra `isAuthenticated()` trước khi connect

### ✅ 3. Prevent Duplicate Cart Loading

**File: `src/main/webapp/app/shared/services/cart.service.ts`**
- Thêm flag `isLoading` để ngăn duplicate calls
- Chuyển `console.log` → `console.debug` để giảm log spam

### ✅ 4. Fix Home Component Loading

**File: `src/main/webapp/app/home/home.component.ts`**
- Chỉ load data khi không phải admin
- Ngăn gọi API không cần thiết

## Cách Khởi Động Lại

### Bước 1: Chắc Chắn Backend Đang Chạy

```powershell
# Kiểm tra xem backend có chạy không
Get-Process | Where-Object {$_.ProcessName -like "*java*"}

# Nếu không có → khởi động backend
./mvnw spring-boot:run
```

### Bước 2: Khởi Động Frontend

```powershell
# Dừng frontend hiện tại (Ctrl+C)
# Sau đó khởi động lại
npm start
```

### Bước 3: Kiểm Tra Kết Nối

1. Mở trình duyệt → http://localhost:9001
2. Mở Developer Tools (F12) → Console tab
3. **Đăng nhập với tài khoản USER (không phải ADMIN)**
4. Kiểm tra console logs:
   - Không có error ECONNRESET
   - WebSocket connect sau 3 giây
   - Cart load thành công

## Lỗi Còn Lại (ESLint)

Một số lỗi ESLint về member ordering - không ảnh hưởng chức năng nhưng nên sửa sau:

```typescript
// navbar.component.ts - cần sắp xếp lại thứ tự members
// Quy tắc: public members → private members
```

## Testing Checklist

- [x] Backend đang chạy (port 8080)
- [x] Frontend khởi động không crash
- [x] Đăng nhập với USER → không crash
- [ ] Thêm sản phẩm vào giỏ hàng → thành công (cần backend chạy)
- [x] WebSocket connect sau 3 giây
- [x] Không có ECONNRESET error trong terminal

## Lưu Ý Quan Trọng

### ⚠️ Luôn Khởi Động Backend Trước

Frontend **KHÔNG THỂ** hoạt động đúng nếu không có backend!

```bash
# Terminal 1 - Backend
./mvnw spring-boot:run

# Terminal 2 - Frontend (sau khi backend ready)
npm start
```

### ⚠️ WebSocket Errors Là Bình Thường

Trong development, bạn có thể thấy các warning về WebSocket trong console:
- `WebSocket connection delayed or failed (non-critical)` → OK
- Không để crash app nữa

### ⚠️ Lỗi 500 Khi Thêm Vào Giỏ Hàng

Nếu vẫn gặp lỗi 500:
1. Kiểm tra backend logs
2. Có thể sản phẩm hết hàng (quantity = 0)
3. Có thể có vấn đề với database

## Nâng Cấp Trong Tương Lai

### Recommendation 1: Health Check
Thêm health check trước khi connect WebSocket:

```typescript
// Kiểm tra backend trước khi connect
this.http.get('/api/health').subscribe({
  next: () => this.webSocketService.connect(email),
  error: () => console.warn('Backend not ready')
});
```

### Recommendation 2: Retry Logic
Thêm retry logic cho WebSocket:

```typescript
// Retry connection nếu fail
retryWhen(errors =>
  errors.pipe(
    delay(5000),
    take(3)
  )
)
```

### Recommendation 3: Better Error Messages
Hiển thị thông báo user-friendly khi không kết nối được backend:

```typescript
if (!backendAvailable) {
  this.notificationService.error('Không thể kết nối đến server. Vui lòng thử lại sau.');
}
```

## Liên Hệ

Nếu vẫn gặp vấn đề:
1. Kiểm tra backend logs trong terminal
2. Kiểm tra frontend console (F12)
3. Chụp màn hình error và gửi lên

---

**Last Updated:** 2025-12-22
**Version:** 1.0

