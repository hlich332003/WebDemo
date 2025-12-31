# 📥 HƯỚNG DẪN IMPORT DỮ LIỆU - WEBDEMO

## 📋 MỤC LỤC

1. [Import Products (Sản phẩm)](#1-import-products-sản-phẩm)
2. [Import Users (Người dùng có tài khoản)](#2-import-users-người-dùng-có-tài-khoản)
3. [Import Customers (Khách hàng mua offline)](#3-import-customers-khách-hàng-mua-offline)

---

## 1. IMPORT PRODUCTS (Sản phẩm)

### **📋 Format Excel:**

```
| Cột A: ID | Cột B: Tên SP | Cột C: Mô tả | Cột D: Giá | Cột E: Số lượng | Cột F: URL ảnh | Cột G: - | Cột H: Danh mục |
|-----------|---------------|--------------|------------|-----------------|----------------|----------|-----------------|
|           | iPhone 15     | Điện thoại   | 29000000   | 50              | http://...     | -        | Điện thoại      |
| 123       | Samsung S24   | Điện thoại   | 25000000   | 30              |                | -        | Điện thoại      |
```

### **Quy tắc:**

- **Cột A (ID):**
  - Để trống = Tạo mới sản phẩm
  - Có giá trị = Cập nhật sản phẩm có ID này
- **Cột B (Tên):** Bắt buộc
- **Cột C (Mô tả):** Tùy chọn (mặc định: "Chưa có mô tả")
- **Cột D (Giá):** Bắt buộc, phải là số
- **Cột E (Số lượng):** Tùy chọn (mặc định: 0)
- **Cột F (URL ảnh):** Tùy chọn
- **Cột G:** Bỏ trống
- **Cột H (Danh mục):** Bắt buộc, phải tồn tại trong DB

### **Hỗ trợ ảnh nhúng trong Excel:**

- ✅ Có thể copy/paste ảnh trực tiếp vào Excel
- ✅ Hệ thống tự động extract và lưu vào database
- ✅ Ưu tiên: Ảnh nhúng > URL ảnh > Placeholder

### **API:**

```bash
POST /api/admin/import/products
Authorization: Bearer ADMIN_TOKEN
Content-Type: multipart/form-data
Body: file=products.xlsx
```

### **Files liên quan:**

- `ProductRepository.java`
- `FileImportService.importProducts()`
- `FileImportResource.importProducts()`

---

## 2. IMPORT USERS (Người dùng có tài khoản)

### **📋 Format Excel:**

```
| Cột A: SĐT   | Cột B: Họ      | Cột C: Tên   | Cột D: Email          |
|--------------|----------------|--------------|------------------------|
| 0987654321   | Nguyễn Văn     | An           | ngan@gmail.com        |
| 0123456789   | Trần Thị       | Bình         | binhtt@gmail.com      |
```

### **Quy tắc:**

- **Cột A (SĐT):** Tùy chọn
- **Cột B (Họ):** Tùy chọn
- **Cột C (Tên):** Tùy chọn
- **Cột D (Email):** Tùy chọn
- **⚠️ Phải có ít nhất SĐT hoặc Email**

### **Logic:**

1. Tìm user theo SĐT → Nếu không có → Tìm theo Email
2. Nếu tìm thấy → **CẬP NHẬT**
3. Nếu không → **TẠO MỚI**
   - Password mặc định: `123456`
   - Activated: `true`
   - Role: `ROLE_USER`

### **API:**

```bash
POST /api/admin/import/users
Authorization: Bearer ADMIN_TOKEN
Content-Type: multipart/form-data
Body: file=users.xlsx
```

### **Files liên quan:**

- `UserRepository.java`
- `FileImportService.importUsers()`
- `FileImportResource.importUsers()`

---

## 3. IMPORT CUSTOMERS (Khách hàng mua offline)

### **📋 Format Excel:**

```
| Cột A: SĐT   | Cột B: Tên khách hàng | Cột C: Sản phẩm đã mua     | Cột D: Ngày mua   |
|--------------|-----------------------|----------------------------|-------------------|
| 0987654321   | Nguyễn Văn An         | Giày Nike, Áo thun         | 31/12/2025        |
| 0123456789   | Trần Thị Bình         | Quần jean                  | 30/12/2025        |
```

### **Quy tắc - TẤT CẢ 4 CỘT ĐỀU BẮT BUỘC:**

- **Cột A (SĐT):** ✅ BẮT BUỘC - 10 số
- **Cột B (Tên):** ✅ BẮT BUỘC
- **Cột C (Sản phẩm):** ✅ BẮT BUỘC - Danh sách phân cách bởi dấu phẩy
- **Cột D (Ngày mua):** ✅ BẮT BUỘC - Format: `dd/MM/yyyy` (VD: 31/12/2025)

### **Validation:**

```
❌ Thiếu SĐT → Error: "Cột A (SĐT) không được để trống"
❌ Thiếu Tên → Error: "Cột B (Tên) không được để trống"
❌ Thiếu Sản phẩm → Error: "Cột C (Sản phẩm đã mua) không được để trống"
❌ Thiếu Ngày → Error: "Cột D (Ngày mua hàng) không được để trống"
❌ Sai format ngày → Error: "Vui lòng dùng format: dd/MM/yyyy"
```

### **Logic:**

1. Tìm customer theo SĐT
2. Nếu không có → **TẠO MỚI**
3. Nếu có → **CẬP NHẬT**
   - Cập nhật tên
   - **Append** sản phẩm mới vào danh sách cũ
   - Cập nhật ngày mua gần nhất

### **Ví dụ Append:**

```
Lần 1: 0987654321 | Nguyễn Văn An | Giày Nike | 30/12/2025
→ products_purchased = "Giày Nike"

Lần 2: 0987654321 | Nguyễn Văn An | Áo thun | 31/12/2025
→ products_purchased = "Giày Nike, Áo thun" ✅
→ last_purchase_date = 31/12/2025 ✅
```

### **🔗 Tự động link với User:**

Khi khách hàng đăng ký tài khoản với SĐT đã có trong `customer`, hệ thống tự động link:

```java
// UserService.registerUser()
linkCustomerToUser(newUser);
// → Tìm customer theo phone → Link customer.user_id = user.id
```

### **Database:**

```sql
CREATE TABLE customer (
    id BIGINT PRIMARY KEY,
    phone NVARCHAR(10) NOT NULL UNIQUE,
    full_name NVARCHAR(100) NOT NULL,
    products_purchased NVARCHAR(MAX) NOT NULL,
    created_date DATETIMEOFFSET NOT NULL,
    last_purchase_date DATETIMEOFFSET NOT NULL,
    notes NVARCHAR(MAX),
    user_id BIGINT NULL,
    FOREIGN KEY (user_id) REFERENCES jhi_user(id)
);
```

### **API:**

```bash
POST /api/admin/import/customers
Authorization: Bearer ADMIN_TOKEN
Content-Type: multipart/form-data
Body: file=customers.xlsx
```

### **Files liên quan:**

- `Customer.java` (Entity)
- `CustomerRepository.java`
- `FileImportService.importCustomers()`
- `UserService.linkCustomerToUser()` (Auto-link)
- `FileImportResource.importCustomers()`

---

## 📊 SO SÁNH: USERS vs CUSTOMERS

| Đặc điểm          | **Users** (jhi_user) | **Customers** (customer)            |
| ----------------- | -------------------- | ----------------------------------- |
| **Mục đích**      | Tài khoản đăng nhập  | Khách mua offline (chưa có account) |
| **Email**         | Tùy chọn             | ❌ Không có                         |
| **Phone**         | Tùy chọn             | ✅ Bắt buộc (unique)                |
| **Password**      | ✅ Có                | ❌ Không có                         |
| **Login**         | ✅ Có thể            | ❌ Không thể                        |
| **Sản phẩm mua**  | ❌ Không lưu         | ✅ Lưu lịch sử                      |
| **Link với nhau** | Tự động (theo phone) | Tự động (khi đăng ký)               |

---

## 🧪 TESTING

### **Test 1: Import Products**

```bash
curl -X POST http://localhost:8080/api/admin/import/products \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@products.xlsx"
```

### **Test 2: Import Users**

```bash
curl -X POST http://localhost:8080/api/admin/import/users \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@users.xlsx"
```

### **Test 3: Import Customers**

```bash
curl -X POST http://localhost:8080/api/admin/import/customers \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@customers.xlsx"
```

---

## ✅ CHECKLIST TRƯỚC KHI IMPORT

### **Products:**

- [ ] Đã tạo tất cả categories cần thiết
- [ ] File Excel đúng format (8 cột)
- [ ] Cột Giá là số, không có ký tự đặc biệt
- [ ] Tên danh mục trong Excel khớp với DB

### **Users:**

- [ ] File Excel đúng format (4 cột)
- [ ] Có ít nhất SĐT hoặc Email
- [ ] Email đúng format (nếu có)
- [ ] SĐT đúng 10 số (nếu có)

### **Customers:**

- [ ] File Excel đúng format (4 cột)
- [ ] **TẤT CẢ 4 CỘT ĐỀU ĐÃ ĐIỀN**
- [ ] SĐT đúng 10 số
- [ ] Ngày mua đúng format: dd/MM/yyyy

---

## 🐛 TROUBLESHOOTING

### **Lỗi: "Không tìm thấy danh mục"**

→ Tạo danh mục trước trong Admin → Categories

### **Lỗi: "Email đã tồn tại"**

→ User này đã có trong hệ thống, sẽ cập nhật thông tin

### **Lỗi: "SĐT đã tồn tại"**

→ Customer này đã có, sẽ append sản phẩm mới

### **Lỗi: "Cột X không được để trống"**

→ Kiểm tra lại file Excel, điền đầy đủ các cột bắt buộc

### **Lỗi: "Sai định dạng ngày"**

→ Dùng format: dd/MM/yyyy (VD: 31/12/2025)

---

## 📄 FILES LIÊN QUAN

### **Backend:**

- `FileImportService.java` - Service xử lý import
- `FileImportResource.java` - API endpoints
- `Customer.java` - Entity khách hàng offline
- `UserService.java` - Auto-link customer với user

### **Database:**

- `jhipster_db.sql` - Cấu trúc database
- Bảng `customer` - Line 8-30
- Bảng `jhi_user` - Line 238-269
- Bảng `jhi_product` - Line 181-209

---

**✅ HOÀN TẤT - Tất cả tính năng import đã sẵn sàng!** 🎉
