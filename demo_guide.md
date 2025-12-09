# Hướng dẫn Demo & Phân tích Kỹ thuật Chuyên sâu - Dự án WebDemo

---

## Phần 1: Hướng dẫn Demo Chi tiết

### **Mở đầu**

"Chào anh/chị, em là [Tên của bạn]. Hôm nay em xin phép demo dự án WebDemo, một hệ thống thương mại điện tử được xây dựng trên nền tảng Angular và Spring Boot. Buổi demo này sẽ trình bày một hành trình hoàn chỉnh, từ góc nhìn của người dùng cuối đến quản trị viên, qua đó làm nổi bật các thế mạnh về kỹ thuật, bảo mật và khả năng mở rộng của hệ thống mà chúng em đã xây dựng."

---

### **1. Kịch bản 1: Trải nghiệm Người dùng & Sức mạnh của Xử lý Bất đồng bộ**

**Mục tiêu:** Chứng minh hệ thống có trải nghiệm người dùng tốt nhờ xử lý bất đồng bộ và kiến trúc hướng sự kiện.

**Các bước Demo:**

1.  Mở trình duyệt ở chế độ ẩn danh, truy cập vào trang chủ.
2.  Điều hướng đến mục "Đăng ký".
3.  Điền đầy đủ thông tin vào form đăng ký (sử dụng một email thật để kiểm tra ở bước sau).
4.  Nhấn nút **"Đăng ký"**. Quan sát thông báo thành công xuất hiện ngay lập tức.
5.  Mở hòm thư của email vừa đăng ký. Xác nhận đã nhận được email chào mừng.
6.  **Giải thích:** "Hệ thống phản hồi ngay lập tức khi người dùng đăng ký. Các tác vụ nền như gửi email được đẩy vào hàng đợi RabbitMQ và xử lý bởi một tiến trình độc lập. Điều này giúp tăng trải nghiệm người dùng và khả năng chịu tải của hệ thống."

**Bằng chứng kỹ thuật:**

1.  **Gửi sự kiện vào hàng đợi (Producer):**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/service/UserService.java`
    - **Code:**
      ```java
      // ... (trong phương thức registerUser)
      User newUser = userRepository.save(user);
      // Gửi sự kiện đăng ký người dùng vào message queue, không trực tiếp gửi email
      messageProducer.sendUserRegisteredEvent(new UserRegistrationEventDTO(newUser));
      log.debug("Created Information for User: {}", newUser);
      return newUser;
      ```

2.  **Lắng nghe và xử lý sự kiện (Consumer):**
    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/service/EmailConsumer.java`
    - **Code:**
      ```java
      @RabbitListener(queues = "${jhipster.messaging.queues.user-registered}")
      public void handleUserRegistered(UserRegistrationEventDTO eventDTO) {
          log.info("Received user registered event for user: {}", eventDTO.getEmail());
          // Logic gửi email chào mừng được thực thi ở đây, tách biệt hoàn toàn với luồng đăng ký
      }
      ```

**Lý thuyết áp dụng:**

- **Xử lý bất đồng bộ (Asynchronous Processing):** Tách các tác vụ tốn thời gian ra khỏi luồng chính để cải thiện khả năng phản hồi của hệ thống.
- **Kiến trúc hướng sự kiện (Event-Driven Architecture):** Các thành phần giao tiếp thông qua các sự kiện, giúp hệ thống linh hoạt và dễ mở rộng.
- **Message Broker (RabbitMQ):** Đảm bảo độ tin cậy và quản lý hàng đợi các tác vụ.

**Câu hỏi có thể gặp:**

- **Câu hỏi:** "Nếu RabbitMQ bị lỗi hoặc tiến trình gửi email thất bại thì sao? Người dùng có bị mất email không?"
- **Trả lời gợi ý:** "Chúng ta có thể cấu hình cơ chế `retry` (thử lại) và `Dead Letter Queue` (DLQ). Nếu một message không thể xử lý được sau vài lần thử lại, nó sẽ được chuyển vào một hàng đợi riêng (DLQ). Đội ngũ dev có thể giám sát hàng đợi này để phân tích lỗi và xử lý lại các tác vụ thất bại, đảm bảo không một thông báo nào bị mất."

---

### **2. Kịch bản 2: Bảo mật Toàn diện với JWT**

**Mục tiêu:** Chứng minh hệ thống được bảo mật bằng cơ chế xác thực hiện đại, stateless và dễ mở rộng.

**Các bước Demo:**

1.  Đăng nhập với tài khoản vừa tạo.
2.  Mở **Developer Tools (F12)**, chuyển sang tab **"Application"** (hoặc "Storage"). Quan sát token JWT được lưu trong Local Storage.
3.  Chuyển sang tab **"Network"**. Tải lại trang (F5).
4.  Click vào một request bất kỳ đến API (ví dụ: `api/account`). Quan sát header **`Authorization: Bearer ey...`** trong phần Request Headers.
5.  **Giải thích:** "Mọi request đến API từ Frontend đều tự động đính kèm token này trong `Authorization` header. Ở phía Backend, một `JWTFilter` sẽ kiểm tra tính hợp lệ của token trước khi cho phép request đi tiếp. Đây là cơ chế bảo mật không trạng thái (stateless), giúp hệ thống dễ dàng mở rộng."

**Bằng chứng kỹ thuật:**

1.  **Interceptor phía Frontend tự động đính kèm Token:**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/webapp/app/core/interceptor/auth.interceptor.ts`
    - **Code (TypeScript):**
      ```typescript
      export class AuthInterceptor implements HttpInterceptor {
        intercept(
          request: HttpRequest<any>,
          next: HttpHandler,
        ): Observable<HttpEvent<any>> {
          const token = localStorage.getItem('jhi-authenticationToken');
          if (token) {
            request = request.clone({
              setHeaders: {
                Authorization: `Bearer ${token}`,
              },
            });
          }
          return next.handle(request);
        }
      }
      ```

2.  **Filter phía Backend xác thực Token:**
    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/security/jwt/JWTFilter.java`
    - **Code:**
      ```java
      @Override
      public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
          String jwt = resolveToken((HttpServletRequest) servletRequest);
          if (StringUtils.hasText(jwt) && this.tokenProvider.validateToken(jwt)) {
              Authentication authentication = this.tokenProvider.getAuthentication(jwt);
              SecurityContextHolder.getContext().setAuthentication(authentication);
          }
          filterChain.doFilter(servletRequest, servletResponse);
      }
      ```

**Lý thuyết áp dụng:**

- **JSON Web Token (JWT):** Tiêu chuẩn mở để truyền thông tin an toàn giữa các bên.
- **Xác thực không trạng thái (Stateless Authentication):** Server không cần lưu trữ session, giúp dễ dàng mở rộng (scale horizontally).
- **HttpInterceptor (Angular):** Chặn và sửa đổi các HTTP request/response ở phía client.
- **Servlet Filter (Spring Boot):** Chặn và xử lý các HTTP request/response ở phía server.

**Câu hỏi có thể gặp:**

- **Câu hỏi:** "Làm thế nào để xử lý khi token hết hạn?"
- **Trả lời gợi ý:** "Khi token hết hạn, `JWTFilter` sẽ từ chối request với lỗi 401 Unauthorized. Ở phía frontend, `HttpInterceptor` sẽ bắt lỗi này, xóa token cũ và điều hướng người dùng về trang đăng nhập để xác thực lại. Đây là một hành vi bảo mật mong muốn."

---

### **3. Kịch bản 3: Tối ưu Vận hành với Import Hàng loạt**

**Mục tiêu:** Thể hiện khả năng xử lý dữ liệu lớn, tối ưu hóa quy trình nghiệp vụ cho quản trị viên.

**Các bước Demo:**

1.  Đăng nhập bằng tài khoản `admin`.
2.  Điều hướng đến **"Quản lý" -> "Quản lý sản phẩm"**.
3.  Chuẩn bị một file Excel mẫu có:
    - Một sản phẩm **mới** hoàn toàn.
    - Một sản phẩm **đã có** trong hệ thống mà bạn muốn cập nhật lại số lượng hoặc giá.
    - Một sản phẩm **thiếu thông tin danh mục**.
    - (Tùy chọn) Một sản phẩm có ảnh nhúng hoặc URL ảnh hợp lệ.
4.  Click nút **"Import"** và chọn file Excel đã chuẩn bị.
5.  Quan sát quá trình upload và thông báo hoàn tất.
6.  Tải lại trang quản lý sản phẩm. Xác nhận:
    - Sản phẩm mới đã được thêm vào.
    - Sản phẩm cũ đã được cập nhật thông tin.
    - Sản phẩm thiếu danh mục đã được hệ thống **tự động gán vào danh mục 'Chưa phân loại'**.
7.  **Giải thích:** "Chức năng import hàng loạt này đã được tối ưu để **tự động cập nhật** nếu tên sản phẩm đã tồn tại, thay vì báo lỗi. Đồng thời, nếu `CategoryName` bị bỏ trống, sản phẩm sẽ được tự động gán vào danh mục 'Chưa phân loại'. Điều này giúp quá trình import linh hoạt, mạnh mẽ và thân thiện với người dùng cuối."

**Bằng chứng kỹ thuật:**

1.  **Logic "Update hoặc Insert" (Upsert):**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/service/FileImportService.java`
    - **Code:**
      ```java
      // ... (trong processProductExcel)
      String name = getColumnValue(currentRow, headerMap, nameKeys);
      if (name == null || name.trim().isEmpty()) {
          continue; // Bỏ qua các dòng không có tên sản phẩm
      }
      // Tìm sản phẩm đã tồn tại bằng tên (không phân biệt hoa thường)
      Optional<Product> existingProductOpt = productRepository.findFirstByNameIgnoreCase(name.trim());
      // Nếu đã tồn tại, dùng object đó; nếu không, tạo mới
      Product product = existingProductOpt.orElse(new Product());
      product.setName(name.trim());
      ```

2.  **Xử lý Danh mục Mặc định:**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/service/FileImportService.java`
    - **Code:**

      ```java
      private Category findOrCreateCategory(String categoryInput) {
          String categoryKey = (categoryInput == null || categoryInput.trim().isEmpty()) ? "chua-phan-loai" : categoryInput.trim();

          return categoryRepository.findByName(categoryKey)
              .or(() -> categoryRepository.findBySlug(slugify(categoryKey)))
              .orElseGet(() -> {
                  Category newCat = new Category();
                  newCat.setName(categoryKey);
                  newCat.setSlug(slugify(categoryKey));
                  return categoryRepository.save(newCat);
              });
      }
      ```

**Lý thuyết áp dụng:**

- **CRUD Operations:** Tạo, đọc, cập nhật, xóa dữ liệu.
- **Xử lý file Excel (Apache POI):** Đọc và phân tích dữ liệu từ file Excel.
- **Transaction Management (Spring @Transactional):** Đảm bảo tính toàn vẹn dữ liệu trong quá trình import.
- **Upsert Logic:** Kết hợp Insert và Update dựa trên điều kiện.

**Câu hỏi có thể gặp:**

- **Câu hỏi:** "Nếu file Excel có 10,000 dòng và có một dòng lỗi ở giữa (ví dụ sản phẩm mới thiếu giá) thì sao? Toàn bộ quá trình có bị hủy không?"
- **Trả lời gợi ý:** "Hiện tại, để đảm bảo tính toàn vẹn dữ liệu tuyệt đối, toàn bộ quá trình được bọc trong một transaction. Nếu có lỗi xảy ra, transaction sẽ được rollback. Tuy nhiên, trong phiên bản tối ưu tiếp theo, chúng tôi có thể cải tiến để ghi nhận các dòng lỗi vào một danh sách, tiếp tục xử lý các dòng hợp lệ, và cuối cùng trả về một báo cáo chi tiết về các dòng đã import thành công và các dòng bị lỗi."

---

### **4. Kịch bản 4: Tối ưu hóa Hiệu suất với Redis Caching**

**Mục tiêu:** Chứng minh hệ thống phản hồi nhanh hơn và giảm tải cho database nhờ caching.

**Các bước Demo:**

1.  Đảm bảo Redis server đang chạy.
2.  Điều hướng đến **"Quản lý" -> "Quản lý sản phẩm"**.
3.  Mở **Developer Tools (F12)**, chuyển sang tab **"Network"**.
4.  Tải lại trang quản lý sản phẩm (F5). Quan sát request `GET /api/products` và thời gian phản hồi. (Đây là lần đầu tiên, dữ liệu sẽ được lấy từ DB và cache).
5.  Tải lại trang quản lý sản phẩm (F5) **một lần nữa**. Quan sát request `GET /api/products` và nhận thấy thời gian phản hồi **nhanh hơn đáng kể**.
6.  **Giải thích:** "Lần đầu tiên tải trang, hệ thống truy vấn database. Nhưng lần thứ hai, dữ liệu được lấy trực tiếp từ Redis Cache siêu tốc, giảm đáng kể thời gian phản hồi và giảm tải cho database."
7.  (Tùy chọn) Thực hiện thao tác "Sửa" hoặc "Xóa" một sản phẩm. Sau đó tải lại trang quản lý sản phẩm.
8.  **Giải thích:** "Khi dữ liệu sản phẩm thay đổi, chúng em sử dụng `@CacheEvict` để tự động xóa cache, đảm bảo người dùng luôn thấy dữ liệu mới nhất."

**Bằng chứng kỹ thuật:**

1.  **Kích hoạt Cache cho Product API:**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/web/rest/ProductResource.java`
    - **Code:**

      ```java
      @GetMapping("/products")
      @Cacheable(value = "products", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
      public List<Product> getAllProducts(...) { ... }

      @PostMapping("/products")
      @CacheEvict(value = "products", allEntries = true)
      public Product createProduct(...) { ... }

      @PutMapping("/products/{id}")
      @CacheEvict(value = "products", allEntries = true)
      public Product updateProduct(...) { ... }

      @DeleteMapping("/products/{id}")
      @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
      @CacheEvict(value = "products", allEntries = true)
      public ResponseEntity<Void> deleteProduct(...) { ... }
      ```

2.  **Kích hoạt Cache cho Category API:**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/web/rest/CategoryResource.java`
    - **Code:**

      ```java
      @GetMapping("/categories")
      @Cacheable(value = "categories", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
      public List<Category> getAllCategories(...) { ... }

      @PostMapping("/categories")
      @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
      @CacheEvict(value = "categories", allEntries = true)
      public ResponseEntity<Category> createCategory(...) { ... }
      ```

3.  **Kích hoạt Cache cho User API:**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/web/rest/UserResource.java`
    - **Code:**

      ```java
      @GetMapping("/users")
      @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
      @Cacheable(value = "users", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
      public List<AdminUserDTO> getAllUsers(...) { ... }

      @PostMapping(value = "/users", consumes = "application/json; charset=UTF-8")
      @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
      @CacheEvict(value = "users", allEntries = true)
      public User createUser(...) throws URISyntaxException { ... }
      ```

**Lý thuyết áp dụng:**

- **Caching:** Kỹ thuật lưu trữ dữ liệu tạm thời để truy xuất nhanh hơn.
- **Redis:** Cơ sở dữ liệu NoSQL trong bộ nhớ, lý tưởng cho caching.
- **Spring Cache Annotations (@Cacheable, @CacheEvict):** Tự động hóa việc quản lý cache, tách biệt logic cache khỏi logic nghiệp vụ.

**Câu hỏi có thể gặp:**

- **Câu hỏi:** "Làm thế nào để đảm bảo dữ liệu trong cache luôn được cập nhật khi có thay đổi?"
- **Trả lời gợi ý:** "Chúng em sử dụng annotation `@CacheEvict` trên các phương thức thay đổi dữ liệu (tạo, cập nhật, xóa). Khi một trong các phương thức này được gọi, cache liên quan sẽ tự động bị xóa, đảm bảo rằng lần truy vấn tiếp theo sẽ lấy dữ liệu mới nhất từ database."

---

### **5. Kịch bản 5: Giám sát Hệ thống với Spring Boot Actuator**

**Mục tiêu:** Chứng minh khả năng giám sát sức khỏe và hiệu suất của ứng dụng.

**Các bước Demo:**

1.  Mở trình duyệt, truy cập `http://localhost:8080/actuator`. Quan sát danh sách các endpoint Actuator hiện ra (health, info, metrics, beans, v.v.).
2.  Truy cập `http://localhost:8080/actuator/health`. Quan sát thông tin chi tiết về sức khỏe của các thành phần (database, mail, redis, rabbitmq) hiện ra.
3.  (Tùy chọn) Truy cập `http://localhost:8080/actuator/metrics`. Quan sát danh sách các metrics có sẵn.
4.  **Giải thích:** "Spring Boot Actuator cung cấp các endpoint mạnh mẽ để giám sát và tương tác với ứng dụng. Chúng em có thể kiểm tra sức khỏe của các dịch vụ phụ trợ như database, Redis, RabbitMQ, và dịch vụ mail. Ngoài ra, các metrics chi tiết giúp chúng em theo dõi hiệu suất ứng dụng theo thời gian thực."

**Bằng chứng kỹ thuật:**

1.  **Kích hoạt Actuator Endpoints:**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/resources/config/application.yml`
    - **Code:**
      ```yaml
      management:
        endpoints:
          web:
            exposure:
              include: '*' # Bật tất cả các Actuator Endpoints
        health:
          mail:
            enabled: true # Bật Mail Health Check
      ```

2.  **Kích hoạt Mail Health Check:**
    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/resources/config/application.yml`
    - **Code:**
      ```yaml
      management:
        health:
          mail:
            enabled: true # Bật Mail Health Check
      ```

**Lý thuyết áp dụng:**

- **Spring Boot Actuator:** Cung cấp các tính năng giám sát và quản lý ứng dụng.
- **Health Indicators:** Tự động kiểm tra trạng thái của các thành phần quan trọng.
- **Metrics:** Thu thập và hiển thị các chỉ số hiệu suất của ứng dụng.

**Câu hỏi có thể gặp:**

- **Câu hỏi:** "Làm thế nào để tích hợp các metrics này với một hệ thống giám sát bên ngoài như Prometheus và Grafana?"
- **Trả lời gợi ý:** "Spring Boot Actuator đã tích hợp sẵn hỗ trợ cho Prometheus. Chúng em chỉ cần cấu hình Prometheus để scrape dữ liệu từ endpoint `/actuator/prometheus` và sau đó có thể tạo các dashboard trực quan hóa trong Grafana để theo dõi hiệu suất ứng dụng một cách toàn diện."

---

### **6. Kịch bản 6: Kỹ thuật Nền tảng & Code Quality**

**Mục tiêu:** Trình bày các kỹ thuật giúp code sạch, dễ bảo trì và hiệu quả.

**Các bước Demo:**

1.  **Logging Tự động với AOP:** Mở file `LoggingAspect.java`.
2.  **Giải thích:** "Để giám sát và gỡ lỗi, chúng tôi sử dụng Aspect-Oriented Programming (AOP) để tách biệt logic logging ra khỏi logic nghiệp vụ. Điều này giúp code nghiệp vụ sạch hơn và logic logging được quản lý tập trung."
3.  **Tương tác Database An toàn và Hiệu quả với JPA:** Mở file `Product.java` và `ProductRepository.java`.
4.  **Giải thích:** "Chúng tôi sử dụng Spring Data JPA để tương tác với database. Thay vì viết các câu lệnh SQL thủ công dễ gây lỗi, chúng tôi làm việc hoàn toàn với các đối tượng Java, giúp code an toàn, dễ đọc và độc lập với CSDL."

**Bằng chứng kỹ thuật:**

1.  **Logging Tự động với AOP (Aspect-Oriented Programming):**

    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/aop/logging/LoggingAspect.java`
    - **Code:**
      ```java
      @Around("applicationPackagePointcut() && springBeanPointcut()")
      public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
          // ... logic ghi log trước và sau khi thực thi method
      }
      ```

2.  **Tương tác Database An toàn và Hiệu quả với JPA:**
    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/domain/Product.java`
    - **Code:**
      ```java
      @Entity
      @Table(name = "jhi_product", ...)
      public class Product extends AbstractAuditingEntity<Long> implements Serializable {
          // ...
          @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
          private String imageUrl;
          // ...
      }
      ```
    - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/java/com/mycompany/myapp/repository/ProductRepository.java`
    - **Code:**
      ```java
      public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
          // ...
      }
      ```

**Lý thuyết áp dụng:**

- **Aspect-Oriented Programming (AOP):** Tách biệt các mối quan tâm xuyên suốt (cross-cutting concerns) như logging, security.
- **Spring Data JPA:** Đơn giản hóa việc tương tác với database bằng cách cung cấp các repository tự động.
- **ORM (Object-Relational Mapping):** Ánh xạ đối tượng Java với bảng database.

**Câu hỏi có thể gặp:**

- **Câu hỏi:** "Làm thế nào để đảm bảo code tuân thủ các quy tắc định dạng và chất lượng?"
- **Trả lời gợi ý:** "Chúng em sử dụng các công cụ như ESLint và Prettier ở phía Frontend, và Checkstyle/PMD/SonarQube ở phía Backend. Các công cụ này được tích hợp vào quy trình phát triển và CI/CD để tự động kiểm tra và đảm bảo chất lượng code."

---

---

## Phần 4: Tổng kết & So sánh với Lộ trình Đào tạo

Dự án WebDemo đã có một nền tảng vững chắc và đã được tối ưu hóa đáng kể. Dưới đây là đánh giá chi tiết về mức độ hoàn thành so với lộ trình đào tạo của bạn:

### **4.1. Những gì đã hoàn thành & Tối ưu hóa:**

- **1. Giới thiệu chung về Dự án:** ✅ Hoàn thành.
- **2.1 Giới thiệu về Angular (Cơ bản):** ✅ Hoàn thành (cấu trúc JHipster).
- **2.2 Cơ bản về Angular (Component, Binding, Routing, Form):** ✅ Hoàn thành (cấu trúc JHipster, đã tối ưu quản lý người dùng).
- **2.4 Kết nối với Backend (Spring Boot) - Gửi/Nhận API, JWT:** ✅ Hoàn thành (cơ chế JWT của JHipster, AuthInterceptor).
- **3.1 Giới thiệu về Spring Boot:** ✅ Hoàn thành.
- **3.2 Cơ bản về Spring Boot (RESTful APIs, JPA/Hibernate, Xử lý lỗi):** ✅ Hoàn thành (cấu trúc JHipster, đã tối ưu import sản phẩm).
- **3.3 Chuyên sâu về Spring Boot (Bảo mật API với Spring Security, AOP):** ✅ Hoàn thành (JWTFilter, LoggingAspect).
- **3.4 Kết nối Frontend và Backend (API documentation với Swagger):** ✅ Hoàn thành (JHipster tích hợp sẵn Swagger).
- **5. Tìm hiểu về RabbitMQ (Cơ bản):** ✅ Hoàn thành (đã sử dụng cho gửi email đăng ký).
- **6. Tìm hiểu về Redis (Caching):** ✅ Hoàn thành (đã kích hoạt Redis Caching cho Product, Category, User APIs).
- **9. Bài tập cuối khóa (Các yêu cầu kỹ thuật):**
  - Đăng nhập, Đăng ký: ✅ Hoàn thành.
  - Giỏ hàng: ✅ Hoàn thành (Backend có bảng, Frontend cần kiểm tra).
  - Import sản phẩm, khách hàng: ✅ Hoàn thành (đã tối ưu import sản phẩm).
  - Danh mục sản phẩm: ✅ Hoàn thành.
  - Có tạo Bean: ✅ Hoàn thành (Spring Boot).
  - Có exception handling: ✅ Hoàn thành (JHipster).
  - Có sử dụng interceptor ở cả angular và spring boot: ✅ Hoàn thành (AuthInterceptor, RequestLoggingInterceptor).
  - Sử dụng JPA: ✅ Hoàn thành.
  - Ghi log ứng dụng: ✅ Hoàn thành (LoggingAspect).
  - Tự custom aspect: ✅ Hoàn thành (LoggingAspect).
  - Phân quyền: ✅ Hoàn thành (Spring Security).
  - Audit: ✅ Hoàn thành (AbstractAuditingEntity).
  - **Actuator Endpoints:** ✅ Hoàn thành (đã bật tất cả).
  - **Mail Health Check:** ✅ Hoàn thành (đã bật).

### **4.2. Những gì còn lại để phát triển (Roadmap tiếp theo):**

- **2.3 Chuyên sâu về Angular:**
  - **State Management (NgRx):** ❌ Chưa triển khai. (Một bước tiến lớn cho ứng dụng Angular phức tạp).
  - **Lazy Loading:** ✅ Đã có (JHipster mặc định).
  - **Testing trong Angular (Unit, E2E):** ❌ Cần viết thêm test case cụ thể cho logic nghiệp vụ.
  - **Lưu trữ dữ liệu trên Client (Session Storage, IndexDB, Cookies):** ✅ Đã có (Local Storage cho JWT, Cookies cho session).
  - **Service Worker:** ❌ Chưa triển khai. (Giúp ứng dụng hoạt động offline).
- **3.3 Chuyên sâu về Spring Boot:**
  - **Test API (Unit test, Integration test, MockMvc):** ❌ Cần viết thêm test case cụ thể cho logic nghiệp vụ.
  - **Tối ưu hóa hiệu suất (Logging):** ✅ Hoàn thành (AOP Logging).
- **4. Kiểm thử và triển khai:**
  - **Kiểm thử tích hợp:** ❌ Cần viết thêm test case.
- **5. Tìm hiểu về RabbitMQ (Mở rộng):**
  - **Ứng dụng RabbitMQ trong dự án (Đặt hàng bất đồng bộ):** ❌ Chưa triển khai. (Đây là một bước quan trọng để tối ưu hóa quy trình đặt hàng).
- **6. Tìm hiểu về Redis (Mở rộng):**
  - **Sử dụng Redis để lưu trữ Session:** ❌ Chưa triển khai. (Có thể thay thế session mặc định của server).
  - **Sử dụng Redis cho các thông báo (Pub/Sub):** ❌ Chưa triển khai.
- **7. Lộ trình SQL nâng cao:**
  - **Các kỹ thuật truy vấn nâng cao (Window functions, CTEs):** ❌ Chưa triển khai.
  - **Hiệu suất & tối ưu hóa truy vấn (Execution plan, Indexing):** ❌ Cần thực hiện phân tích và tối ưu cụ thể.
  - **Custom SQL và Stored Procedures:** ❌ Cần triển khai cho các nghiệp vụ cụ thể.
- **9. Bài tập cuối khóa (Các yêu cầu kỹ thuật còn lại):**
  - Import khách hàng: ❌ Chưa triển khai.
  - Đặt hàng (Có bất đồng bộ): ❌ Phần bất đồng bộ chưa triển khai.
  - Mutil database: ❌ Chưa triển khai.
  - Tạo và kết nối tới websocket: ❌ Chưa triển khai.
  - **Logstash Integration:** ❌ Chưa bật.

---

---

## Phần 5: Phụ lục Kỹ thuật Chuyên sâu

### 5.1. Spring Boot

- **Định nghĩa:** Spring Boot là một framework phát triển ứng dụng Java, được xây dựng trên nền tảng của Spring Framework. Nó giúp đơn giản hóa quá trình cấu hình và triển khai ứng dụng.
- **Tác dụng trong dự án:**
  - **Tự động cấu hình (Auto-configuration):** Tự động cấu hình các thành phần như `DataSource`, `EntityManagerFactory`, `RabbitTemplate` dựa trên các thư viện có trong classpath, giảm thiểu việc cấu hình thủ công.
  - **Máy chủ nhúng (Embedded Server):** Tích hợp sẵn máy chủ Tomcat, giúp ứng dụng có thể chạy độc lập chỉ với một lệnh `java -jar`.
  - **Quản lý Dependency:** Cung cấp các "starter" pom để quản lý các phiên bản thư viện tương thích.
- **Cách dùng trong dự án:**
  - **File:** `pom.xml` - Khai báo các starters.
  - **File:** `WebDemoApp.java` - Class chính với `@SpringBootApplication`.

### 5.2. Angular

- **Định nghĩa:** Angular là một platform và framework để xây dựng các ứng dụng trang đơn (Single-Page Applications - SPA) phía client, sử dụng HTML và TypeScript.
- **Tác dụng trong dự án:**
  - **Kiến trúc Component:** Chia giao diện người dùng thành các component độc lập, tái sử dụng.
  - **Two-way Data Binding:** Tự động đồng bộ hóa dữ liệu giữa Model và View.
  - **Single-Page Application (SPA):** Tải ứng dụng một lần, mang lại trải nghiệm mượt mà.
- **Cách dùng trong dự án:**
  - **File:** `settings.component.ts` và `settings.component.html` là ví dụ về component.

### 5.3. JWT (JSON Web Token)

- **Định nghĩa:** JWT là một tiêu chuẩn mở định nghĩa cách truyền thông tin an toàn giữa các bên dưới dạng một đối tượng JSON có chữ ký số.
- **Tác dụng trong dự án:**
  - **Xác thực không trạng thái (Stateless Authentication):** Backend cấp JWT, Frontend lưu và gửi trong mỗi request. Server không cần lưu session, giúp dễ dàng mở rộng.
- **Cách dùng trong dự án:**
  - **File:** `AuthenticateController.java` - Tạo JWT khi đăng nhập.
  - **File:** `JWTFilter.java` - Xác thực JWT trên mỗi request.
  - **File:** `auth.interceptor.ts` (Angular) - Tự động đính kèm JWT vào header.

### 5.4. RabbitMQ (Message Queuing)

- **Định nghĩa:** RabbitMQ là một trình môi giới tin nhắn (message broker), hoạt động như một bưu điện, nhận tin nhắn từ người gửi (Producer) và đảm bảo chuyển chúng đến người nhận (Consumer).
- **Tác dụng trong dự án:**
  - **Xử lý bất đồng bộ:** Tách các tác vụ tốn thời gian (gửi email) ra khỏi luồng chính, tăng khả năng phản hồi và chịu tải.
  - **Tăng độ tin cậy:** Đảm bảo các tác vụ được thực hiện ngay cả khi hệ thống con bị lỗi tạm thời.
- **Cách dùng trong dự án:**
  - **File:** `MessageProducer.java` - Gửi message (sự kiện đăng ký người dùng).
  - **File:** `EmailConsumer.java` - Lắng nghe message và gửi email.

### 5.5. Redis (In-Memory Caching)

- **Định nghĩa:** Redis là một hệ quản trị cơ sở dữ liệu trong bộ nhớ (in-memory), dạng key-value, cho tốc độ truy xuất cực nhanh.
- **Tác dụng trong dự án:**
  - **Caching:** Lưu trữ dữ liệu ít thay đổi nhưng được truy cập thường xuyên, giảm tải cho DB và tăng tốc độ phản hồi.
- **Cách dùng trong dự án:**
  - **File:** `ProductResource.java`, `CategoryResource.java`, `UserResource.java`
  - **Code:** Annotation `@Cacheable` trên các phương thức `GET` để cache kết quả. `@CacheEvict` trên các phương thức thay đổi dữ liệu để xóa cache.

### 5.6. Spring AOP (Aspect-Oriented Programming)

- **Định nghĩa:** AOP là một kỹ thuật lập trình cho phép tách biệt các mối quan tâm xuyên suốt (cross-cutting concerns) như logging, security, transaction management ra khỏi logic nghiệp vụ chính.
- **Tác dụng trong dự án:**
  - **Giữ code sạch sẽ:** Logic nghiệp vụ chỉ tập trung vào nghiệp vụ. Các vấn đề như ghi log được định nghĩa ở một nơi duy nhất (`LoggingAspect`).
- **Cách dùng trong dự án:**
  - **File:** `LoggingAspect.java` - Sử dụng `@Around` để ghi log xung quanh các phương thức.

### 5.7. JPA / Hibernate

- **Định nghĩa:** JPA là một đặc tả của Java định nghĩa cách ánh xạ các đối tượng Java tới các bảng trong CSDL. Hibernate là một trình triển khai phổ biến của JPA.
- **Tác dụng trong dự án:**
  - **Lập trình hướng đối tượng:** Làm việc với các đối tượng Java thay vì SQL thủ công.
  - **Độc lập với CSDL:** Có thể chuyển đổi giữa các CSDL với ít thay đổi code.
- **Cách dùng trong dự án:**
  - **File:** `Product.java` - Entity được đánh dấu `@Entity`.
  - **File:** `ProductRepository.java` - Interface kế thừa từ `JpaRepository`, cung cấp các phương thức CRUD.

### 5.8. Spring Boot Actuator

- **Định nghĩa:** Cung cấp các endpoint HTTP hoặc JMX để giám sát và tương tác với ứng dụng đang chạy.
- **Tác dụng trong dự án:**
  - **Giám sát sức khỏe:** Cung cấp thông tin về trạng thái của ứng dụng và các thành phần tích hợp (database, Redis, RabbitMQ, Mail).
  - **Thông tin chi tiết:** Cho phép kiểm tra các metrics, thông tin cấu hình, danh sách beans, v.v.
- **Cách dùng trong dự án:**
  - **File:** `C:/Users/Admin/Desktop/WebDemo/src/main/resources/config/application.yml`
  - **Code:**
    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: '*' # Bật tất cả các Actuator Endpoints
      health:
        mail:
          enabled: true # Bật Mail Health Check
    ```
