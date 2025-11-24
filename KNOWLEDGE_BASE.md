# 📚 KIẾN THỨC DỰ ÁN - WebDemo

**Dự án:** Angular + Spring Boot E-commerce  
**Ngày cập nhật:** 24/11/2025  
**Mục đích:** Tổng hợp kiến thức đã áp dụng trong dự án

---

# MỤC LỤC

1. [JWT Authentication](#1-jwt-authentication)
2. [Global Exception Handling](#2-global-exception-handling)
3. [RabbitMQ Message Queue](#3-rabbitmq-message-queue)
4. [Redis Caching](#4-redis-caching)
5. [Validation & Security](#5-validation--security)
6. [Frontend Optimization](#6-frontend-optimization)
7. [Database & JPA](#7-database--jpa)

---

## 1. JWT AUTHENTICATION

### 📖 Khái niệm

**JWT (JSON Web Token)** là chuẩn mở (RFC 7519) để truyền thông tin an toàn giữa các bên dưới dạng JSON object.

**Cấu trúc JWT:**

```
Header.Payload.Signature
eyJhbGc...  .  eyJzdWI...  .  SflKxwRJ...
```

### 🔧 Áp dụng trong dự án

#### Backend - Spring Boot

**File:** `SecurityConfiguration.java`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(authz ->
      authz
        .requestMatchers("/api/authenticate")
        .permitAll()
        .requestMatchers("/api/register")
        .permitAll()
        .requestMatchers("/api/admin/**")
        .hasAuthority("ROLE_ADMIN")
        .anyRequest()
        .authenticated()
    )
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

  return http.build();
}

```

**File:** `JWTFilter.java` (Interceptor)

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
  throws ServletException, IOException {
  // 1. Lấy token từ header
  String jwt = resolveToken(request);

  // 2. Validate token
  if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
    // 3. Extract username từ token
    String username = tokenProvider.getUsernameFromToken(jwt);

    // 4. Load user details
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    // 5. Set authentication vào SecurityContext
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
      userDetails,
      null,
      userDetails.getAuthorities()
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  filterChain.doFilter(request, response);
}

```

**File:** `TokenProvider.java`

```java
public String createToken(Authentication authentication) {
  String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));

  long now = System.currentTimeMillis();
  Date validity = new Date(now + this.tokenValidityInMilliseconds);

  return Jwts.builder()
    .setSubject(authentication.getName())
    .claim("auth", authorities)
    .signWith(key, SignatureAlgorithm.HS512)
    .setExpiration(validity)
    .compact();
}

public boolean validateToken(String token) {
  try {
    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    return true;
  } catch (JwtException | IllegalArgumentException e) {
    return false;
  }
}

```

#### Frontend - Angular

**File:** `auth-jwt.service.ts`

```typescript
login(credentials: Login): Observable<void> {
  return this.http.post<{ id_token: string }>(
    this.applicationConfigService.getEndpointFor('api/authenticate'),
    credentials
  ).pipe(
    map(response => {
      const jwt = response.id_token;
      if (jwt) {
        this.storeAuthenticationToken(jwt);
      }
    })
  );
}

private storeAuthenticationToken(jwt: string): void {
  // Lưu vào sessionStorage (mất khi đóng tab)
  sessionStorage.setItem('authenticationToken', jwt);

  // Hoặc localStorage (tồn tại lâu dài)
  // localStorage.setItem('authenticationToken', jwt);
}

getToken(): string | null {
  return sessionStorage.getItem('authenticationToken');
}
```

**File:** `auth.interceptor.ts` (Interceptor)

```typescript
intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
  // 1. Lấy token từ storage
  const token = this.authService.getToken();

  // 2. Clone request và thêm Authorization header
  if (token) {
    request = request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // 3. Xử lý response (refresh token nếu cần)
  return next.handle(request).pipe(
    catchError(error => {
      if (error.status === 401) {
        // Token hết hạn → logout
        this.authService.logout();
        this.router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
}
```

### 🔒 Bảo mật Token

**1. Đăng xuất:**

```typescript
logout(): void {
  // Xóa token khỏi storage
  sessionStorage.removeItem('authenticationToken');
  localStorage.removeItem('authenticationToken');

  // Clear SecurityContext (backend sẽ tự xóa khi nhận request không có token)
  this.router.navigate(['/login']);
}
```

**2. Token hết hạn:**

- Access token: 15 phút (short-lived)
- Refresh token: 30 ngày (long-lived)

**3. Tắt máy/Đóng tab:**

- `sessionStorage` → Mất token, phải login lại
- `localStorage` → Giữ token, vẫn đăng nhập

---

## 2. GLOBAL EXCEPTION HANDLING

### 📖 Khái niệm

**Global Exception Handler** xử lý tất cả exception trong ứng dụng tại một nơi duy nhất, trả về response thống nhất.

### 🔧 Áp dụng trong dự án

**File:** `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Response thống nhất cho mọi API error
   */
  public static class ErrorResponse {

    private String message;
    private int status;
    private String error;
    private LocalDateTime timestamp;
    private String path;
    // Constructor, getters, setters...
  }

  /**
   * 1. Xử lý Validation errors (@Valid)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
    String errors = ex
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .map(error -> error.getField() + ": " + error.getDefaultMessage())
      .collect(Collectors.joining(", "));

    ErrorResponse response = new ErrorResponse(
      "Validation failed: " + errors,
      HttpStatus.BAD_REQUEST.value(),
      "Bad Request",
      LocalDateTime.now(),
      request.getDescription(false).replace("uri=", "")
    );

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * 2. Xử lý Resource Not Found
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex, WebRequest request) {
    ErrorResponse response = new ErrorResponse(
      ex.getMessage(),
      HttpStatus.NOT_FOUND.value(),
      "Not Found",
      LocalDateTime.now(),
      request.getDescription(false).replace("uri=", "")
    );

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  /**
   * 3. Xử lý Business Logic errors
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, WebRequest request) {
    log.error("Business error: {}", ex.getMessage());

    ErrorResponse response = new ErrorResponse(
      ex.getMessage(),
      HttpStatus.BAD_REQUEST.value(),
      "Business Error",
      LocalDateTime.now(),
      request.getDescription(false).replace("uri=", "")
    );

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * 4. Xử lý tất cả exception khác
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
    log.error("Unexpected error: ", ex);

    ErrorResponse response = new ErrorResponse(
      "Internal server error",
      HttpStatus.INTERNAL_SERVER_ERROR.value(),
      "Internal Server Error",
      LocalDateTime.now(),
      request.getDescription(false).replace("uri=", "")
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}

```

**File:** `OrderService.java` (Sử dụng)

```java
public OrderDTO createOrder(OrderDTO orderDTO) {
  // Validate quantity
  for (OrderItemDTO item : orderDTO.getItems()) {
    Product product = productRepository
      .findById(item.getProductId())
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + item.getProductId()));

    if (product.getQuantity() < item.getQuantity()) {
      throw new BusinessException("Insufficient stock for product: " + product.getName());
    }

    // Giảm quantity
    product.setQuantity(product.getQuantity() - item.getQuantity());
    productRepository.save(product);
  }
  // Lưu order...
}

```

### 📊 Response Format

**Success:**

```json
{
  "data": { "id": 1, "name": "Product A" },
  "message": "Success"
}
```

**Error:**

```json
{
  "message": "Insufficient stock for product: Laptop",
  "status": 400,
  "error": "Business Error",
  "timestamp": "2025-11-24T10:30:00",
  "path": "/api/orders"
}
```

---

## 3. RABBITMQ MESSAGE QUEUE

### 📖 Khái niệm

**RabbitMQ** là message broker, giúp gửi/nhận message bất đồng bộ giữa các service.

**Các thành phần:**

- **Producer:** Gửi message
- **Exchange:** Nhận message từ producer, route đến queue
- **Queue:** Lưu trữ message
- **Consumer:** Nhận và xử lý message

### 🔧 Áp dụng trong dự án

#### Cấu hình

**File:** `RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {

  public static final String QUEUE_NAME = "order.email.queue";
  public static final String EXCHANGE_NAME = "order.exchange";
  public static final String ROUTING_KEY = "order.email";

  @Bean
  public Queue emailQueue() {
    return new Queue(QUEUE_NAME, true); // durable = true
  }

  @Bean
  public TopicExchange exchange() {
    return new TopicExchange(EXCHANGE_NAME);
  }

  @Bean
  public Binding binding(Queue emailQueue, TopicExchange exchange) {
    return BindingBuilder.bind(emailQueue).to(exchange).with(ROUTING_KEY);
  }
}

```

#### Producer (Gửi message)

**File:** `OrderService.java`

```java
@Service
public class OrderService {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  public OrderDTO createOrder(OrderDTO orderDTO) {
    // 1. Lưu order vào database
    Order order = orderRepository.save(newOrder);

    // 2. Gửi message để gửi email BẤT ĐỒNG BỘ
    EmailMessage emailMsg = new EmailMessage(
      order.getUser().getEmail(),
      "Order Confirmation",
      "Your order #" + order.getId() + " has been placed"
    );

    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, emailMsg);

    log.info("Sent email message to queue for order: {}", order.getId());

    // 3. Trả về ngay (không chờ email gửi xong)
    return orderMapper.toDto(order);
  }
}

```

#### Consumer (Nhận message)

**File:** `EmailConsumer.java`

```java
@Component
public class EmailConsumer {

  private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

  @Autowired
  private EmailService emailService;

  @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
  public void handleEmailMessage(EmailMessage message) {
    log.info("Received email message: {}", message);

    try {
      // Gửi email thực tế
      emailService.sendEmail(message.getTo(), message.getSubject(), message.getBody());
      log.info("Email sent successfully to: {}", message.getTo());
    } catch (Exception e) {
      log.error("Failed to send email: ", e);
      // Message sẽ được retry tự động hoặc đi vào DLQ
    }
  }
}

```

### 🎯 Lợi ích

1. **Bất đồng bộ:** API trả về ngay, không chờ email
2. **Decoupling:** OrderService không phụ thuộc EmailService
3. **Retry:** Auto retry khi gửi email fail
4. **Scalability:** Có thể chạy nhiều consumer song song

---

## 4. REDIS CACHING

### 📖 Khái niệm

**Redis** là in-memory database, dùng để cache dữ liệu truy xuất nhanh.

**TTL (Time To Live):** Thời gian tồn tại của data trong cache.

### 🔧 Áp dụng trong dự án

#### Cấu hình

**File:** `application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10 phút
```

**File:** `RedisConfig.java`

```java
@Configuration
@EnableCaching
public class RedisConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(10)) // TTL = 10 phút
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

    return RedisCacheManager.builder(factory).cacheDefaults(config).build();
  }
}

```

#### Sử dụng Cache

**File:** `ProductService.java`

```java
@Service
public class ProductService {

  /**
   * Cache kết quả findAll() trong 10 phút
   */
  @Cacheable(value = "products", key = "'all'")
  public List<ProductDTO> findAll() {
    log.info("Loading products from database..."); // Chỉ log khi cache miss
    return productRepository.findAll().stream().map(productMapper::toDto).toList();
  }

  /**
   * Cache theo ID
   */
  @Cacheable(value = "products", key = "#id")
  public ProductDTO findOne(Long id) {
    return productRepository.findById(id).map(productMapper::toDto).orElse(null);
  }

  /**
   * Xóa cache khi update
   */
  @CachePut(value = "products", key = "#productDTO.id")
  public ProductDTO update(ProductDTO productDTO) {
    Product product = productRepository.save(productMapper.toEntity(productDTO));
    return productMapper.toDto(product);
  }

  /**
   * Xóa toàn bộ cache
   */
  @CacheEvict(value = "products", allEntries = true)
  public void clearCache() {
    log.info("Cleared all product cache");
  }
}

```

### ⏰ TTL trong Redis

**Cách hoạt động:**

1. Lần 1 gọi API → Query DB, lưu vào Redis với TTL = 10 phút
2. Lần 2-N (trong 10 phút) → Lấy từ Redis (NHANH)
3. Sau 10 phút → Redis tự động xóa, lần tiếp query DB lại

**Check TTL:**

```bash
redis-cli
> TTL products::all
(integer) 598  # Còn 598 giây
```

---

## 5. VALIDATION & SECURITY

### 📖 Validation số lượng sản phẩm

**Vấn đề:** User có thể thêm 100 sản phẩm vào giỏ khi chỉ còn 2 trong kho.

### 🔧 Áp dụng trong dự án

#### Frontend Validation

**File:** `cart.service.ts`

```typescript
addToCart(product: IProduct, quantity: number = 1): boolean {
  // ✅ Kiểm tra hết hàng
  const availableStock = product.quantity ?? 0;
  if (availableStock <= 0) {
    return false;
  }

  // ✅ Kiểm tra số lượng trong giỏ
  const currentInCart = this.getQuantityInCart(product.id);
  const newTotal = currentInCart + quantity;

  // ✅ Không cho vượt quá tồn kho
  if (newTotal > availableStock) {
    console.warn(`Cannot add ${quantity}. Only ${availableStock - currentInCart} available`);
    return false;
  }

  // Thêm vào giỏ
  this.cartItems.push({ product, quantity });
  return true;
}
```

**File:** `cart.component.ts`

```typescript
increaseQuantity(productId: number, currentQty: number): void {
  const item = this.cart.find(i => i.product.id === productId);
  const maxQty = item?.product.quantity ?? 0;

  // ✅ Không cho tăng quá tồn kho
  if (currentQty >= maxQty) {
    this.notify.error('⚠️ Đã đạt giới hạn số lượng!');
    return;
  }

  this.cartService.updateQuantity(productId, currentQty + 1);
}
```

#### Backend Validation

**File:** `OrderService.java`

```java
public OrderDTO createOrder(OrderDTO orderDTO) {
  for (OrderItemDTO item : orderDTO.getItems()) {
    Product product = productRepository.findById(item.getProductId()).orElseThrow(() -> new EntityNotFoundException("Product not found"));

    // ✅ Validate tồn kho
    if (product.getQuantity() < item.getQuantity()) {
      throw new BusinessException("Insufficient stock for " + product.getName() + ". Available: " + product.getQuantity());
    }

    // Giảm quantity
    product.setQuantity(product.getQuantity() - item.getQuantity());
    productRepository.save(product);
  }
  // Lưu order...
}

```

### 🔒 Luồng bảo mật

```
Client                  CartService            OrderService (Backend)
  |                         |                           |
  |--addToCart(product)--->|                           |
  |                         |--validate quantity------->| ✅
  |<---return success-------|                           |
  |                         |                           |
  |--checkout()------------>|                           |
  |                         |--createOrder()----------->|
  |                         |                           |--validate lại
  |                         |                           |--giảm quantity
  |                         |<---OrderDTO---------------|
  |<---Success--------------|                           |
```

---

## 6. FRONTEND OPTIMIZATION

### 📖 Debounce Search

**Vấn đề:** User gõ "laptop" → Gọi API 6 lần (l, la, lap, lapt, lapto, laptop)

**Giải pháp:** Chờ user gõ xong 500ms mới gọi API 1 lần.

### 🔧 Áp dụng trong dự án

**File:** `product-list.component.ts`

```typescript
export class ProductListComponent implements OnInit, OnDestroy {
  searchTerm = '';
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Setup debounce
    this.searchSubject
      .pipe(
        debounceTime(500), // Chờ 500ms
        distinctUntilChanged(), // Chỉ trigger khi giá trị thay đổi
        takeUntil(this.destroy$),
      )
      .subscribe(term => {
        this.searchTerm = term;
        this.loadProducts(); // Gọi API
      });
  }

  // Được gọi mỗi khi user gõ
  onSearchChange(term: string): void {
    this.searchSubject.next(term); // Không gọi API ngay
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

**HTML:**

```html
<input [(ngModel)]="searchTerm" (ngModelChange)="onSearchChange($event)" placeholder="Tìm kiếm..." />
```

### 📊 So sánh

| Không debounce            | Có debounce              |
| ------------------------- | ------------------------ |
| Gõ "laptop" = 6 API calls | Gõ "laptop" = 1 API call |
| UX: Lag, chậm             | UX: Mượt mà              |
| Server: Quá tải           | Server: Nhẹ nhàng        |

---

## 7. DATABASE & JPA

### 📖 @SqlResultSetMapping

**Mục đích:** Map kết quả native SQL query vào DTO.

### 🔧 Áp dụng trong dự án

**File:** `Order.java`

```java
@Entity
@Table(name = "orders")
@SqlResultSetMapping(
  name = "OrderSearchDTOMapping",
  classes = @ConstructorResult(
    targetClass = OrderSearchDTO.class,
    columns = {
      @ColumnResult(name = "id", type = Long.class),
      @ColumnResult(name = "orderNumber", type = String.class),
      @ColumnResult(name = "totalAmount", type = BigDecimal.class),
      @ColumnResult(name = "status", type = String.class),
      @ColumnResult(name = "createdDate", type = LocalDateTime.class),
    }
  )
)
public class Order {
  // Entity fields...
}

```

**File:** `OrderRepositoryCustomImpl.java`

```java
@Repository
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<OrderSearchDTO> searchOrders(String keyword) {
    String sql =
      """
          SELECT
              o.id,
              o.order_number as orderNumber,
              o.total_amount as totalAmount,
              o.status,
              o.created_date as createdDate
          FROM orders o
          WHERE o.order_number LIKE :keyword
             OR o.status LIKE :keyword
          ORDER BY o.created_date DESC
      """;

    Query query = entityManager.createNativeQuery(sql, "OrderSearchDTOMapping");
    query.setParameter("keyword", "%" + keyword + "%");

    return query.getResultList();
  }
}

```

**File:** `OrderSearchDTO.java`

```java
public class OrderSearchDTO {

  private Long id;
  private String orderNumber;
  private BigDecimal totalAmount;
  private String status;
  private LocalDateTime createdDate;

  // Constructor khớp với @SqlResultSetMapping
  public OrderSearchDTO(Long id, String orderNumber, BigDecimal totalAmount, String status, LocalDateTime createdDate) {
    this.id = id;
    this.orderNumber = orderNumber;
    this.totalAmount = totalAmount;
    this.status = status;
    this.createdDate = createdDate;
  }
  // Getters, setters...
}

```

### 🎯 Khi nào dùng?

- ✅ Native SQL phức tạp (JOIN nhiều bảng, aggregate functions)
- ✅ Cần performance tốt hơn JPA query
- ✅ Trả về DTO thay vì Entity (tránh lazy loading issues)

---

## 📝 TÓM TẮT

### Công nghệ đã áp dụng:

| Công nghệ               | Mục đích             | File chính                                              |
| ----------------------- | -------------------- | ------------------------------------------------------- |
| **JWT**                 | Authentication       | JWTFilter.java, TokenProvider.java, auth-jwt.service.ts |
| **Global Exception**    | Error handling       | GlobalExceptionHandler.java                             |
| **RabbitMQ**            | Async messaging      | RabbitMQConfig.java, EmailConsumer.java                 |
| **Redis**               | Caching              | RedisConfig.java, @Cacheable                            |
| **Validation**          | Security             | cart.service.ts, OrderService.java                      |
| **Debounce**            | Optimize search      | product-list.component.ts                               |
| **SqlResultSetMapping** | Native query mapping | Order.java, OrderRepositoryCustomImpl.java              |

### Interceptors:

| Type         | File                | Mục đích                     |
| ------------ | ------------------- | ---------------------------- |
| **Backend**  | JWTFilter.java      | Validate JWT token           |
| **Frontend** | auth.interceptor.ts | Thêm Bearer token vào header |

### Filters:

| File           | Mục đích                              |
| -------------- | ------------------------------------- |
| JWTFilter.java | Filter mọi HTTP request, validate JWT |

---

**END OF KNOWLEDGE BASE**

_Ngày cập nhật: 24/11/2025_  
_Version: 1.0.0_
