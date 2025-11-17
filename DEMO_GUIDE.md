# 🎯 TÀI LIỆU PHÂN TÍCH KỸ THUẬT CHUYÊN SÂU - DỰ ÁN WEBDEMO

## 📋 TỔNG QUAN DỰ ÁN

### Thông tin cơ bản

- **Tên dự án:** WebDemo - Hệ thống E-commerce
- **Công nghệ:** Angular 19 + Spring Boot 3.4.5 + SQL Server + Redis + RabbitMQ
- **Mục tiêu:** Xây dựng hệ thống bán hàng online hoàn chỉnh, áp dụng các công nghệ và best practices hiện đại.

### Kiến trúc Dự án

Dự án được xây dựng theo kiến trúc 3 lớp (3-Tier Architecture), phân tách rõ ràng các mối quan tâm, giúp hệ thống dễ phát triển, bảo trì và mở rộng.

1.  **Presentation Layer (Frontend - Angular):** Chịu trách nhiệm toàn bộ về giao diện và trải nghiệm người dùng (UI/UX).
2.  **Business & Data Access Layer (Backend - Spring Boot):** Là "bộ não" của hệ thống, xử lý toàn bộ logic nghiệp vụ, xác thực, phân quyền và giao tiếp với các nguồn dữ liệu.
3.  **Data Layer (Hạ tầng dữ liệu):** Bao gồm SQL Server (lưu trữ chính), Redis (cache), và RabbitMQ (xử lý bất đồng bộ).

---

## 📚 PHÂN TÍCH KỸ THUẬT & ÁP DỤNG THỰC TẾ

### 1. Frontend: Angular

#### 🔹 Quản lý trạng thái (State Management)

- **1. Nó là gì?**

  - Là các kỹ thuật và công cụ để quản lý, chia sẻ và đồng bộ hóa dữ liệu (trạng thái) giữa các component khác nhau trong một ứng dụng trang đơn (SPA). Trạng thái có thể là dữ liệu từ server, trạng thái UI (ví dụ: loading, error), hoặc dữ liệu người dùng nhập.

- **2. Dùng làm gì?**

  - Để tạo ra một luồng dữ liệu một chiều, dễ dự đoán, giúp việc debug và bảo trì ứng dụng trở nên đơn giản hơn, đặc biệt khi ứng dụng phát triển lớn và phức tạp. Nó giải quyết vấn đề "prop drilling" (truyền dữ liệu qua nhiều tầng component không liên quan).

- **3. Vì sao dùng cái này trong dự án?**

  - **Trạng thái giỏ hàng** là một ví dụ điển hình của **trạng thái toàn cục (global state)**. Nó cần được chia sẻ và đồng bộ giữa nhiều component không liên quan trực tiếp đến nhau (ví dụ: nút "Thêm vào giỏ" ở trang chi tiết sản phẩm, icon giỏ hàng ở header, và trang giỏ hàng). Nếu không có một cơ chế quản lý state tập trung, việc đồng bộ này sẽ rất phức tạp và dễ gây lỗi.
  - Các trang quản lý phức tạp có nhiều trạng thái giao diện (UI state) như `isLoading`, `isEditing`, `filters`... cần một cách quản lý hiệu quả để cập nhật giao diện một cách tối ưu.

- **4. Áp dụng vào đâu trong dự án? (Dự án đã áp dụng như thế nào)**

  - **RxJS `BehaviorSubject` (cho Global State):**
    1.  `CartService.ts` sử dụng một `BehaviorSubject` (ví dụ: `private _cartItems = new BehaviorSubject<CartItem[]>([]);`) để lưu trữ trạng thái hiện tại của giỏ hàng.
    2.  Các component như `ProductDetailComponent` (khi thêm sản phẩm vào giỏ) hoặc `CartComponent` (hiển thị giỏ hàng) tương tác với service này để cập nhật hoặc lấy dữ liệu giỏ hàng.
    3.  Bất kỳ component nào cũng có thể `subscribe` vào `cartService.cartItems$` (một `Observable` công khai từ `BehaviorSubject`) để nhận cập nhật và tự động render lại giao diện khi giỏ hàng thay đổi.
  - **Angular `Signals` (cho Local State):**
    1.  Trong các component của khu vực **Admin** (ví dụ: `OrderManagementComponent`), các trạng thái giao diện cục bộ như `isLoading = signal(false);` hoặc `orders = signal<Order[]>([]);` được quản lý bằng `signal()`.
    2.  Khi trạng thái thay đổi (ví dụ: `this.isLoading.set(true);`), Angular biết chính xác phần nào của template cần được cập nhật.

- **5. Làm vậy có tác dụng gì?**
  - **Luồng dữ liệu rõ ràng và dễ bảo trì:** Giúp code dễ hiểu và dễ bảo trì. Khi có lỗi liên quan đến dữ liệu, có thể dễ dàng truy vết nguồn gốc của sự thay đổi.
  - **Đồng bộ hóa trạng thái hiệu quả:** Đảm bảo dữ liệu giỏ hàng luôn nhất quán trên toàn ứng dụng.
  - **Hiệu suất tối ưu:** `Signals` giúp cải thiện hiệu suất render của ứng dụng bằng cách cho phép Angular thực hiện các cập nhật DOM một cách chính xác và có chọn lọc, tránh việc kiểm tra thay đổi không cần thiết trên toàn bộ cây component.

### 🎨 Angular State Management – Chuyên sâu

- **NgRx/Redux Pattern: Dành cho state phức tạp**

  - **Vấn đề:** `BehaviorSubject` tốt cho state nhỏ, nhưng khi ứng dụng lớn, việc quản lý nhiều state phụ thuộc lẫn nhau trở nên khó khăn và khó debug.
  - **Giải pháp (NgRx):** Áp dụng kiến trúc Redux, cung cấp một luồng dữ liệu một chiều nghiêm ngặt: **Component -> Dispatch Action -> Effect (gọi API) -> Action Success/Failure -> Reducer (cập nhật State) -> Selector (lấy State) -> Component**.
  - **Lợi ích:**
    - **Single Source of Truth:** Toàn bộ state nằm trong một Store duy nhất, dễ quản lý.
    - **Dễ dự đoán:** State là bất biến và chỉ được thay đổi bởi các pure function (reducer).
    - **Dễ debug:** Với **NgRx DevTools**, bạn có thể "du hành thời gian", xem lại từng action và sự thay đổi của state, giúp tìm lỗi cực nhanh.

- **Memoization & Selectors: Tối ưu hóa việc truy xuất state**

  - **Vấn đề:** Việc tính toán các dữ liệu dẫn xuất từ state (ví dụ: tổng giá trị giỏ hàng) có thể tốn kém và không cần thiết nếu thực hiện liên tục.
  - **Giải pháp (Memoization):** Là kỹ thuật cache lại kết quả của một hàm. Các selector của NgRx (`createSelector`) được tích hợp sẵn memoization. Một selector sẽ **chỉ tính toán lại** khi các phần state đầu vào của nó thực sự thay đổi. Nếu không, nó sẽ trả về kết quả đã cache.
  - **Lợi ích:** Tránh các phép tính toán và re-render không cần thiết, tối ưu hóa hiệu suất đáng kể.

- **Hybrid Signals + RxJS: Tận dụng điểm mạnh của cả hai**
  - **Bối cảnh:** Angular 16+ giới thiệu Signals như một cơ chế quản lý state hiệu quả hơn cho UI.
  - **Chiến lược "Hybrid":**
    - **Dùng RxJS cho "Control Flow":** Sử dụng service dựa trên RxJS để quản lý các luồng dữ liệu bất đồng bộ phức tạp từ backend (gọi API, debounce search, retry...).
    - **Dùng Signals cho "State" và "View Binding":** Trong component, dùng hàm `toSignal` để chuyển đổi `Observable` từ service thành `Signal`. Sau đó, dùng `Signal` và `computed` signal để quản lý trạng thái của component và binding vào template.
  - **Lợi ích:** Kết hợp sự mạnh mẽ trong xử lý bất đồng bộ của RxJS và khả năng cập nhật giao diện chi tiết, hiệu quả của Signals. Đây là "best practice" cho các ứng dụng Angular hiện đại.

#### 🔹 Tối ưu hiệu suất: Lazy Loading

- **1. Nó là gì?**

  - Là một chiến lược trong thiết kế ứng dụng, trong đó việc khởi tạo một đối tượng hoặc tải một tài nguyên được trì hoãn cho đến khi nó thực sự cần thiết. Trong Angular, nó là một kỹ thuật của Router để tải các `NgModule` hoặc `Route[]` một cách linh động.

- **2. Dùng làm gì?**

  - Dùng để chia nhỏ ứng dụng thành các "khúc" (chunks) JavaScript nhỏ hơn. Thay vì tải toàn bộ ứng dụng trong lần đầu tiên, chỉ những phần cần thiết ban đầu được tải.

- **3. Vì sao dùng cái này trong dự án?**

  - Khu vực **Admin** của dự án chứa nhiều component, service và thư viện (ví dụ: các thư viện biểu đồ, thư viện import/export Excel), làm cho kích thước của nó khá lớn.
  - Tuy nhiên, phần lớn người dùng truy cập trang web là khách hàng thông thường và không bao giờ truy cập vào khu vực này.
  - Việc bắt tất cả người dùng phải tải về code của phần Admin là một sự lãng phí băng thông và làm tăng thời gian tải trang một cách không cần thiết, ảnh hưởng tiêu cực đến trải nghiệm người dùng.

- **4. Áp dụng vào đâu trong dự án? (Dự án đã áp dụng như thế nào)**

  - Trong file `app.routes.ts`, các route dành cho khu vực quản trị được nhóm lại và cấu hình để lazy-load thông qua thuộc tính `loadChildren`:

  ```typescript
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.routes'),
    canActivate: [adminGuard] // Guard để bảo vệ route
  }
  ```

  - Khi ứng dụng được build, Webpack sẽ tự động tách code của module `admin` thành một file JavaScript riêng (chunk).
  - File này chỉ được trình duyệt yêu cầu và tải về từ server khi người dùng (đã được xác thực và có quyền `ROLE_ADMIN`) điều hướng đến một URL bắt đầu bằng `/admin`.

- **5. Làm vậy có tác dụng gì?**
  - **Cải thiện thời gian tải trang lần đầu (FCP/LCP):** Kích thước gói JavaScript ban đầu (initial bundle) mà người dùng thông thường phải tải về giảm đi đáng kể (có thể từ vài MB xuống vài trăm KB), giúp trải nghiệm tải trang của họ nhanh hơn.
  - **Tối ưu tài nguyên:** Giảm lượng dữ liệu cần tải và lượng JavaScript cần phân tích cú pháp (parse) khi khởi động ứng dụng, đặc biệt hữu ích cho người dùng có kết nối mạng chậm hoặc thiết bị cấu hình thấp.

### 🚀 Angular Lazy Loading – Chuyên sâu

- **Preloading Strategy: Tải trước module để tăng tốc điều hướng**

  - **Vấn đề:** Lazy loading có thể gây ra độ trễ nhỏ khi người dùng lần đầu tiên điều hướng đến một module.
  - **Giải pháp (Preloading):** Tải trước các lazy-loaded module ở chế độ nền **sau khi** ứng dụng ban đầu đã tải xong và trình duyệt đang "rảnh rỗi".
  - **Cách làm:** Angular Router cung cấp sẵn chiến lược `PreloadAllModules`. Khi được kích hoạt, sau khi initial bundle được tải, router sẽ âm thầm tải các chunk của các module khác.
  - **Kết quả:** Khi người dùng click vào link, module đã có sẵn trong cache, việc điều hướng diễn ra gần như tức thì, mang lại trải nghiệm tốt nhất của cả hai thế giới.

- **Bundle Analyzer: Phân tích và tìm ra "kẻ tội đồ" làm phình to bundle**

  - **Vấn đề:** Khó biết được thư viện hay component nào đang chiếm nhiều dung lượng nhất trong bundle cuối cùng.
  - **Giải pháp (`webpack-bundle-analyzer`):** Một công cụ trực quan hóa, tạo ra một biểu đồ treemap cho thấy kích thước của từng thành phần trong bundle.
  - **Lợi ích:** Giúp bạn dễ dàng phát hiện các vấn đề như: import nhầm cả một thư viện lớn thay vì chỉ một hàm, một thư viện nặng bị đưa vào initial bundle thay vì được lazy-load. Đây là công cụ không thể thiếu để tối ưu kích thước ứng dụng.

- **Image Optimization: Tối ưu tài sản nặng nhất**
  - **Vấn đề:** Hình ảnh thường là nguyên nhân chính làm chậm trang web.
  - **Giải pháp (kết hợp nhiều kỹ thuật):**
    - **Lazy Loading ảnh:** Dùng thuộc tính `loading="lazy"` trên thẻ `<img>` để trình duyệt chỉ tải ảnh khi người dùng cuộn đến.
    - **Responsive Images (`srcset`):** Cung cấp nhiều phiên bản kích thước của cùng một ảnh để trình duyệt tự chọn phiên bản phù hợp nhất với màn hình thiết bị, tránh tải ảnh 2MB cho một thumbnail 100px.
    - **Modern Image Formats (WebP, AVIF):** Sử dụng các định dạng ảnh thế hệ mới có khả năng nén tốt hơn JPEG/PNG. Dùng thẻ `<picture>` để cung cấp các định dạng này với fallback cho trình duyệt cũ.

### 2. Backend: Spring Boot

#### 🔹 Bảo mật: Spring Security & JWT (JSON Web Token)

- **1. Nó là gì?**

  - **Spring Security:** Là một framework cực kỳ mạnh mẽ và tùy biến cao, cung cấp các giải pháp toàn diện về **xác thực (Authentication)** và **phân quyền (Authorization)** cho các ứng dụng Java. Nó hoạt động dựa trên một chuỗi các bộ lọc (Filter Chain) mà mọi yêu cầu HTTP phải đi qua.
  - **JWT (JSON Web Token):** Là một tiêu chuẩn mở (RFC 7519) định nghĩa một cách nhỏ gọn và khép kín (self-contained) để truyền thông tin giữa các bên dưới dạng một đối tượng JSON. Token này bao gồm 3 phần: Header, Payload (chứa các claims như user ID, roles), và Signature (để xác minh tính toàn vẹn).

- **2. Dùng làm gì?**

  - **Spring Security:** Dùng để bảo vệ các endpoint của ứng dụng, đảm bảo rằng chỉ những người dùng hợp lệ mới có thể truy cập và chỉ được phép thực hiện những hành động mà họ có quyền. Nó cung cấp các cơ chế như mã hóa mật khẩu, quản lý người dùng, và tích hợp với các giao thức xác thực khác.
  - **JWT:** Dùng để tạo ra các "thẻ truy cập" (access tokens) sau khi người dùng xác thực thành công. Các thẻ này sau đó được gửi kèm trong mỗi yêu cầu để chứng minh danh tính và quyền hạn của người dùng mà không cần server phải tra cứu lại trong database hoặc duy trì trạng thái phiên.

- **3. Vì sao dùng cái này trong dự án?**

  - Dự án xây dựng API RESTful, yêu cầu một cơ chế bảo mật **không trạng thái (stateless)**. Việc sử dụng session-cookie truyền thống sẽ buộc server phải lưu trữ thông tin session, gây khó khăn khi mở rộng hệ thống theo chiều ngang (horizontal scaling) vì tất cả các instance của backend phải chia sẻ cùng một kho session.
  - JWT giải quyết vấn đề này một cách hoàn hảo. Mỗi token đã chứa tất cả thông tin cần thiết (user ID, roles). Bất kỳ instance nào của backend cũng có thể xác thực token này mà không cần một kho lưu trữ session chung, giúp hệ thống trở nên linh hoạt và dễ mở rộng.

- **4. Áp dụng vào đâu trong dự án? (Dự án đã áp dụng như thế nào)**

  - **Mã hóa mật khẩu:** Mật khẩu người dùng được mã hóa bằng **BCrypt** trước khi lưu vào database. Bean `PasswordEncoder` trong `SecurityConfiguration.java` định nghĩa việc sử dụng BCrypt.

    ```java
    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

    ```

  - **Luồng xác thực (Login):**
    1.  Người dùng nhập `username` và `password` tại trang **Login** (Frontend).
    2.  Request `POST /api/authenticate` được gửi đến `AuthenticateController.java` (Backend).
    3.  `AuthenticateController` gọi `AuthenticationManager` của Spring Security để xác thực thông tin.
    4.  Nếu xác thực thành công, `TokenProvider` (một service tùy chỉnh) sẽ tạo ra một JWT. JWT này chứa `username` và danh sách các quyền (`authorities` như `ROLE_USER`, `ROLE_ADMIN`).
    5.  JWT được trả về cho client Angular. Client sẽ lưu token này vào **LocalStorage** (key: `jhi-authenticationToken`).
  - **Luồng ủy quyền (Truy cập tài nguyên):**
    1.  Với mọi request tiếp theo đến các API được bảo vệ, `AuthInterceptor.ts` (Frontend) sẽ tự động đọc JWT từ LocalStorage và đính kèm vào header `Authorization: Bearer <token>`.
    2.  Phía backend, `SecurityConfiguration.java` định nghĩa `SecurityFilterChain` mà mọi request phải đi qua. Cấu hình `.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))` kích hoạt bộ lọc sẽ trích xuất, giải mã và xác thực chữ ký của token.
    3.  Nếu token hợp lệ, thông tin người dùng (`Authentication` object) sẽ được tạo và đặt vào `SecurityContextHolder`.
  - **Luồng phân quyền:**
    1.  Các API trong khu vực **Admin** được bảo vệ bởi annotation `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` ngay trên phương thức của Controller.
    2.  Spring Security, nhờ có `@EnableMethodSecurity`, sẽ kiểm tra `SecurityContextHolder` để xem người dùng hiện tại có quyền `ROLE_ADMIN` hay không. Nếu không, request sẽ bị từ chối với lỗi `403 Forbidden`.
  - **Cấu hình Stateless:** Trong `SecurityConfiguration.java`, `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))` được cấu hình để đảm bảo Spring Security không tạo hoặc sử dụng `HttpSession`, củng cố tính stateless của API.

- **5. Làm vậy có tác dụng gì?**
  - **Bảo mật chặt chẽ và linh hoạt:** Xây dựng được một hệ thống bảo mật toàn diện, phân quyền rõ ràng giữa vai trò người dùng thông thường (`ROLE_USER`) và quản trị viên (`ROLE_ADMIN`).
  - **Khả năng mở rộng cao:** Backend trở nên hoàn toàn stateless, sẵn sàng cho việc triển khai trên nhiều server (load balancing) mà không gặp vấn đề về đồng bộ session, giúp hệ thống dễ dàng mở rộng để đáp ứng lượng truy cập lớn.
  - **Tách biệt Frontend và Backend:** Cơ chế token giúp việc phát triển độc lập giữa hai đội trở nên dễ dàng hơn.

#### 🔹 Phân tích chuyên sâu: Cấu hình SecurityFilterChain

File `SecurityConfiguration.java` là trung tâm điều khiển bảo mật cho toàn bộ ứng dụng backend. Nó sử dụng Spring Security 6+ để định nghĩa ai có thể truy cập vào cái gì, và truy cập như thế nào.

- **Annotation quan trọng:**

  - `@Configuration`: Đánh dấu lớp này là một nguồn chứa các định nghĩa bean cho Spring.
  - `@EnableMethodSecurity(securedEnabled = true)`: Kích hoạt **bảo mật ở cấp độ phương thức**, cho phép sử dụng các annotation như `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` trực tiếp trên các phương thức trong Controller hoặc Service để kiểm soát quyền truy cập một cách chi tiết.

- **`filterChain(HttpSecurity http, ...)` Bean (Trái tim của cấu hình):**

  - Đây là nơi xây dựng "chuỗi bộ lọc bảo mật" (`SecurityFilterChain`). Mọi request HTTP gửi đến ứng dụng đều phải đi qua chuỗi này.
  - **Phân tích các mắt xích trong chuỗi:**
    - `.csrf(csrf -> csrf.disable())`: **Tắt tính năng chống tấn công CSRF**. Điều này an toàn vì dự án sử dụng JWT để xác thực (gửi qua header `Authorization`), không phụ thuộc vào session cookie.
    - `.addFilterAfter(new SpaWebFilter(), ...)`: Thêm một bộ lọc tùy chỉnh để giải quyết vấn đề routing của ứng dụng trang đơn (SPA), đảm bảo khi người dùng refresh trang ở một URL sâu (ví dụ: `/products/123`), request được chuyển hướng đúng về `index.html` để Angular xử lý.
    - `.headers(...)`: Cấu hình các HTTP Header liên quan đến bảo mật để tăng cường bảo vệ phía trình duyệt, chống lại các tấn công như Clickjacking và XSS.
    - `.authorizeHttpRequests(...)`: **Phần quan trọng nhất - Định nghĩa các quy tắc phân quyền.** Các quy tắc được đọc từ trên xuống dưới và quy tắc khớp đầu tiên sẽ được áp dụng.
      - `requestMatchers(...).permitAll()`: Cho phép tất cả mọi người truy cập vào các tài nguyên tĩnh, các API công khai (đăng nhập, đăng ký, xem sản phẩm...).
      - `requestMatchers(...).hasAuthority(AuthoritiesConstants.ADMIN)`: Yêu cầu người dùng phải được xác thực và có quyền `ROLE_ADMIN` mới được truy cập các API quản trị.
      - `requestMatchers("/api/**").authenticated()`: **Quy tắc "bắt tất cả" (catch-all).** Bất kỳ request nào đến `/api/**` mà không khớp các quy tắc trên đều yêu cầu người dùng phải đăng nhập.
    - `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`: **Cấu hình cực kỳ quan trọng cho kiến trúc JWT.** Yêu cầu Spring Security **không tạo hoặc sử dụng `HttpSession`**, giúp backend trở nên stateless và dễ dàng mở rộng.
    - `.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))`: **Kích hoạt việc xác thực bằng JWT.** Cấu hình ứng dụng như một "Resource Server", tự động trích xuất, giải mã và xác thực JWT từ header `Authorization` của mỗi request.

- **Tóm tắt luồng hoạt động của một Request:**
  1.  Một request `GET /api/admin/users` đến server với header `Authorization: Bearer <token>`.
  2.  Request đi qua `SecurityFilterChain`.
  3.  `.authorizeHttpRequests` được kiểm tra, request khớp với quy tắc `hasAuthority('ROLE_ADMIN')`.
  4.  Bộ lọc của `.oauth2ResourceServer` được kích hoạt. Nó trích xuất, giải mã và xác thực token.
  5.  Nếu token hợp lệ và chứa quyền `ROLE_ADMIN`, nó tạo đối tượng `Authentication` và đặt vào `SecurityContextHolder`. Request được đi tiếp.
  6.  Nếu token không hợp lệ, trả về lỗi `401 Unauthorized`. Nếu không đủ quyền, trả về lỗi `403 Forbidden`.

#### 🔹 Xử lý lỗi tập trung: Global Exception Handling với `@ControllerAdvice`

File `ExceptionTranslator.java` là một ví dụ điển hình và rất mạnh mẽ về Global Exception Handling, được JHipster tạo ra theo chuẩn **RFC 7807 - Problem Details for HTTP APIs**, giúp định dạng lỗi trả về được chuẩn hóa.

- **Tổng quan và Mục tiêu:**

  - `@ControllerAdvice`: Đánh dấu `ExceptionTranslator.java` là một "trung tâm xử lý lỗi" cho toàn bộ ứng dụng.
  - `extends ResponseEntityExceptionHandler`: Kế thừa các phương thức xử lý cho các exception phổ biến của Spring MVC.
  - **Mục tiêu**: Bắt tất cả các `Throwable` (lỗi) ném ra từ tầng web, chuyển đổi chúng thành một cấu trúc JSON chuẩn hóa (`ProblemDetailWithCause`) và trả về cho client.

- **Phương thức xử lý chính (`@ExceptionHandler`):**

  - Dự án sử dụng một phương thức `handleAnyException` duy nhất, được đánh dấu `@ExceptionHandler`, để bắt tất cả các lỗi không được xử lý cụ thể. Điều này giúp tập trung logic "dịch" lỗi vào một chỗ.

  ```java
  @ExceptionHandler
  public ResponseEntity<Object> handleAnyException(Throwable ex, NativeWebRequest request) {
    ProblemDetailWithCause pdCause = wrapAndCustomizeProblem(ex, request);
    return handleExceptionInternal((Exception) ex, pdCause, buildHeaders(ex), HttpStatusCode.valueOf(pdCause.getStatus()), request);
  }

  ```

- **Quá trình "Dịch" một Exception (ví dụ: Lỗi validation):**

  1.  Client gửi request tạo sản phẩm với `name` để trống.
  2.  Annotation `@Valid` trong Controller phát hiện lỗi và ném ra `MethodArgumentNotValidException`.
  3.  `ExceptionTranslator` bắt được exception này.
  4.  Bên trong, nó gọi đến phương thức `customizeProblem` để xây dựng response lỗi.
  5.  Nó xác định `HttpStatus` là `400 Bad Request`.
  6.  Nó trích xuất thông tin chi tiết về lỗi bằng phương thức `getFieldErrors`: trường nào (`name`) của đối tượng nào (`product`) đã vi phạm ràng buộc nào (`NotBlank`).
  7.  Nó tạo ra một response JSON chuẩn hóa chứa tất cả thông tin trên, bao gồm một mảng `fieldErrors` để frontend có thể sử dụng.

- **Ví dụ kết quả trả về cho lỗi validation:**

  ```json
  {
    "type": "https://www.jhipster.tech/problem/constraint-violation",
    "title": "Method argument not valid",
    "status": 400,
    "detail": "Invalid request content.",
    "path": "/api/products",
    "message": "error.validation",
    "fieldErrors": [
      {
        "objectName": "product",
        "field": "name",
        "message": "NotBlank"
      }
    ]
  }
  ```

- **Lợi ích:**
  - **Code Controller sạch sẽ:** Controller chỉ cần tập trung vào logic "happy path".
  - **Trải nghiệm Frontend tốt hơn:** Frontend nhận được response lỗi có cấu trúc rõ ràng, giúp hiển thị thông báo lỗi chính xác cho người dùng.
  - **Dễ bảo trì:** Khi có lỗi mới, bạn chỉ cần cập nhật logic trong `ExceptionTranslator` thay vì phải sửa ở nhiều Controller.

#### 🔹 Kiểm toán thực thể (Entity Auditing) với Spring Data JPA

- **1. Nó là gì?**

  - **Entity Auditing** là một kỹ thuật cho phép tự động ghi lại thông tin về lịch sử thay đổi của một bản ghi (thực thể) trong database. Các thông tin này thường bao gồm: người tạo, ngày tạo, người sửa đổi cuối cùng, và ngày sửa đổi cuối cùng.

- **2. Dùng làm gì?**

  - Để theo dõi và trả lời các câu hỏi quan trọng về dữ liệu: Ai đã tạo ra bản ghi này? Khi nào? Ai là người cuối cùng chỉnh sửa nó? Vào lúc nào?
  - Nó là một yêu cầu phổ biến trong các hệ thống doanh nghiệp để đảm bảo tính minh bạch, truy xuất nguồn gốc và phục vụ cho việc kiểm toán (audit).

- **3. Vì sao dùng cái này trong dự án?**

  - **Tự động hóa:** Thay vì phải tự tay gán giá trị cho các trường `createdBy`, `createdDate`... trong mỗi phương thức `save()` của service, Spring Data JPA sẽ làm việc này một cách tự động và trong suốt.
  - **Đảm bảo tính toàn vẹn:** Giảm thiểu rủi ro lỗi do con người (quên không gán giá trị), đảm bảo rằng thông tin kiểm toán luôn chính xác.
  - **Code sạch sẽ:** Giữ cho logic nghiệp vụ trong các lớp Service không bị "nhiễu" bởi các đoạn code kỹ thuật lặp đi lặp lại.

- **4. Áp dụng vào đâu trong dự án? (Dự án đã áp dụng như thế nào)**

  - **Bật tính năng Auditing:** Trong một class `@Configuration` nào đó (thường là class Application chính), có annotation `@EnableJpaAuditing`.
  - **Tạo `AbstractAuditingEntity`:** Dự án có một lớp cha `AbstractAuditingEntity<T>` chứa các trường kiểm toán. Các annotation `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate` của Spring Data JPA được sử dụng để đánh dấu các trường này.

    ```java
    public abstract class AbstractAuditingEntity<T> implements Serializable {

      // ...
      @CreatedBy
      @Column(name = "created_by", nullable = false, length = 50, updatable = false)
      private String createdBy;

      @CreatedDate
      @Column(name = "created_date", updatable = false)
      private Instant createdDate = Instant.now();

      @LastModifiedBy
      @Column(name = "last_modified_by", length = 50)
      private String lastModifiedBy;

      @LastModifiedDate
      @Column(name = "last_modified_date")
      private Instant lastModifiedDate = Instant.now();
      // ...
    }

    ```

  - **Kế thừa:** Tất cả các Entity quan trọng cần được kiểm toán (ví dụ: `Product`, `Order`...) đều kế thừa từ `AbstractAuditingEntity`.

    ```java
    @Entity
    @Table(name = "product")
    public class Product extends AbstractAuditingEntity<Long> {
      // ... các trường của Product
    }

    ```

  - **Cung cấp người dùng hiện tại:** Spring Security được cấu hình để cung cấp thông tin về người dùng đang đăng nhập, giúp Spring Data JPA biết phải điền giá trị nào vào các trường `@CreatedBy` và `@LastModifiedBy`.

- **5. Làm vậy có tác dụng gì?**
  - **Tăng khả năng truy vết:** Khi có sự cố hoặc sai sót về dữ liệu, có thể dễ dàng truy ngược lại lịch sử để biết ai đã gây ra thay đổi và vào lúc nào.
  - **Đáp ứng yêu cầu nghiệp vụ:** Nhiều hệ thống (tài chính, y tế...) có yêu cầu bắt buộc về việc lưu lại dấu vết thay đổi dữ liệu.
  - **Tự động và không xâm lấn:** Lập trình viên chỉ cần cho Entity kế thừa từ lớp cha là xong, toàn bộ logic còn lại được framework xử lý, giúp tăng năng suất và giảm lỗi.

#### 🔹 Tối ưu hiệu suất: Caching với Redis

- **1. Nó là gì?**

  - **Caching:** Là một kỹ thuật lưu trữ tạm thời các kết quả của những thao tác tốn kém (như truy vấn database, gọi API bên ngoài) vào một bộ nhớ có tốc độ truy cập cao (như RAM).
  - **Redis:** Là một hệ quản trị cơ sở dữ liệu key-value mã nguồn mở, hiệu suất cực cao, lưu trữ dữ liệu trong bộ nhớ (in-memory). Nó thường được sử dụng làm database, cache, và message broker.

- **2. Dùng làm gì?**

  - Redis được dùng làm một **bộ đệm phân tán (distributed cache)**. "Phân tán" có nghĩa là cache này có thể được chia sẻ bởi nhiều instance của ứng dụng backend, đảm bảo tính nhất quán. Nó lưu trữ các kết quả truy vấn database thường xuyên được sử dụng để giảm thời gian phản hồi của API.

- **3. Vì sao dùng cái này trong dự án?**

  - Các trang như **Danh sách sản phẩm** và **Trang chủ** có tần suất đọc rất cao. Nếu mỗi lượt truy cập đều phải truy vấn vào SQL Server, database sẽ nhanh chóng trở thành điểm nghẽn (bottleneck) của hệ thống, làm giảm hiệu năng và khả năng chịu tải.
  - Việc áp dụng cache là giải pháp bắt buộc để giải quyết vấn đề này, giúp giảm tải cho database và tăng tốc độ phản hồi của API, mang lại trải nghiệm người dùng tốt hơn.

- **4. Áp dụng vào đâu trong dự án? (Dự án đã áp dụng như thế nào)**

  - Dự án sử dụng **Spring Cache Abstraction** để tích hợp Redis một cách trong suốt thông qua các annotation.
  - **Cache Read (`@Cacheable`):**

    - Phương thức `findAll` trong `ProductService.java` (dùng cho **Trang chủ** và **Trang danh sách sản phẩm**) được đánh dấu `@Cacheable("products")`.
    - Khi API `GET /api/products` được gọi lần đầu, Spring Cache sẽ kiểm tra xem có key tương ứng trong Redis không.
    - Nếu không có, nó sẽ thực thi phương thức `findAll` (truy vấn SQL Server), sau đó lưu kết quả vào Redis với một key được tạo tự động.
    - Những lần gọi tiếp theo với cùng tham số, dữ liệu sẽ được trả về trực tiếp từ Redis mà không cần truy vấn database.

    ```java
    // Trong ProductService.java
    @Cacheable(cacheNames = "products")
    public Page<ProductDTO> findAll(Pageable pageable) {
      log.debug("Request to get all Products");
      return productRepository.findAll(pageable).map(productMapper::toDto);
    }

    ```

  - **Cache Invalidation (`@CacheEvict`):**

    - Để đảm bảo tính nhất quán của dữ liệu, cache phải được xóa khi dữ liệu gốc thay đổi.
    - Các phương thức `save`, `update`, `delete` trong `ProductService.java` được đánh dấu `@CacheEvict(value = "products", allEntries = true)`.
    - Khi một sản phẩm được thêm, sửa hoặc xóa, annotation này sẽ ra lệnh cho Spring Cache xóa tất cả các mục trong cache có tên "products", buộc lần đọc tiếp theo phải truy vấn lại từ SQL Server.

    ```java
    // Trong ProductService.java
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO save(ProductDTO productDTO) {
      // ...
    }

    ```

- **5. Làm vậy có tác dụng gì?**
  - **Giảm độ trễ API một cách đột phá:** Thời gian phản hồi của API lấy danh sách sản phẩm giảm từ **~150ms** (khi phải truy vấn SQL Server) xuống chỉ còn **~2-5ms** (khi dữ liệu đã có sẵn trong Redis).
  - **Bảo vệ Database và tăng khả năng chịu tải:** Giảm đáng kể số lượng truy vấn đọc đến SQL Server, giúp database hoạt động ổn định và dành tài nguyên cho các tác vụ ghi quan trọng hơn.

### ⚡ Redis Cache – Chuyên sâu

- **TTL (Time-to-Live): Tránh dữ liệu cũ (Stale Data)**

  - **Nó là gì?** Là thời gian tồn tại của một entry trong cache trước khi nó tự động bị xóa.
  - **Tại sao cần?** Để tránh tình trạng dữ liệu trong cache khác với dữ liệu trong DB (ví dụ: khi DB được cập nhật bởi một tiến trình khác). TTL đảm bảo rằng dữ liệu sẽ được làm mới sau một khoảng thời gian nhất định, là một cơ chế "dọn dẹp" tự động an toàn.
  - **Cách cấu hình:** Cấu hình TTL cho từng cache name trong `application.yml` để có chiến lược cache linh hoạt (ví dụ: cache sản phẩm 10 phút, cache danh mục 1 giờ).

- **Cache Stampede Problem (Hiệu ứng "bầy đàn"): Ngăn chặn quá tải DB**

  - **Vấn đề:** Khi một key cache rất phổ biến hết hạn, hàng nghìn request có thể đồng thời "thấy" cache miss và cùng lúc "dội" vào database để lấy dữ liệu, gây quá tải đột ngột.
  - **Giải pháp:** Sử dụng `@Cacheable(value = "products", sync = true)`. Khi `sync=true`, chỉ có **thread đầu tiên** được phép thực thi phương thức để làm mới cache. Các thread khác đến cùng lúc sẽ bị block và chờ đợi, sau đó nhận kết quả từ cache mới được tạo. Điều này ngăn chặn hiệu quả việc "dội" DB.

- **Cluster & Replication: Đảm bảo tính sẵn sàng cao và khả năng mở rộng**

  - **Vấn đề:** Một instance Redis duy nhất là một **điểm lỗi đơn (Single Point of Failure)** và bị giới hạn về bộ nhớ.
  - **Giải pháp:**
    - **Replication (Master-Slave):** Sao chép dữ liệu sang các node slave để dự phòng. Nếu master sập, một slave có thể được thăng cấp (thường dùng **Redis Sentinel** để tự động hóa).
    - **Cluster:** Phân mảnh (sharding) dữ liệu trên nhiều node khi bộ nhớ cache quá lớn. Điều này cho phép **mở rộng theo chiều ngang (horizontal scaling)**.
  - **Lợi ích:** Tăng độ tin cậy, khả năng chịu lỗi và dung lượng lưu trữ của hệ thống cache.

- **Monitoring: Tối ưu hóa chiến lược Cache**
  - **Tại sao cần?** "Bạn không thể tối ưu cái bạn không đo lường được". Giám sát cache giúp trả lời các câu hỏi: Tỷ lệ **hit/miss ratio** có tốt không? Có cần tăng/giảm TTL không? Cache có đang dùng quá nhiều bộ nhớ không?
  - **Công cụ:**
    - **RedisInsight:** Công cụ GUI trực quan để xem dữ liệu và các chỉ số cơ bản.
    - **Prometheus & Grafana:** Bộ đôi tiêu chuẩn để giám sát chuyên nghiệp. Sử dụng **Redis Exporter** để thu thập metric, **Prometheus** để lưu trữ và **Grafana** để vẽ biểu đồ theo dõi hit/miss ratio, mức sử dụng bộ nhớ, độ trễ lệnh... theo thời gian.

#### 🔹 Xử lý bất đồng bộ: RabbitMQ

- **1. Nó là gì?**

  - **Xử lý bất đồng bộ:** Là một mô hình thiết kế phần mềm, trong đó các tác vụ được tách ra khỏi luồng xử lý chính của một yêu cầu, cho phép chúng được thực thi độc lập và không theo thứ tự.
  - **RabbitMQ:** Là một **Message Broker** (trung gian môi giới tin nhắn) mã nguồn mở, một trong những triển khai phổ biến nhất của giao thức AMQP (Advanced Message Queuing Protocol). Nó hoạt động như một "bưu điện" trung gian, nhận tin nhắn từ bên gửi (Producer) và đảm bảo chuyển đến đúng bên nhận (Consumer) thông qua các hàng đợi (queues).

- **2. Dùng làm gì?**

  - Dùng để tách các tác vụ tốn thời gian (I/O-bound) hoặc không yêu cầu phải hoàn thành ngay lập tức (như gửi email, xử lý ảnh, tạo báo cáo) ra khỏi luồng xử lý chính của API.

- **3. Vì sao dùng cái này trong dự án?**

  - Việc gửi email qua SMTP là một tác vụ mạng, có thể chậm hoặc thất bại do nhiều yếu tố bên ngoài (ví dụ: server SMTP quá tải, lỗi mạng). Nếu thực hiện nó đồng bộ trong API đặt hàng, người dùng sẽ phải chờ đợi lâu và nếu việc gửi email lỗi, toàn bộ giao dịch đặt hàng có thể bị ảnh hưởng hoặc người dùng không nhận được thông báo quan trọng.
  - Việc sử dụng RabbitMQ giúp **tăng tốc độ phản hồi API** và **tăng độ tin cậy** của hệ thống bằng cách tách biệt hoàn toàn tác vụ gửi email.

- **4. Áp dụng vào đâu trong dự án? (Dự án đã áp dụng như thế nào)**

  - **Producer (Bên gửi):** Trong `OrderService.java`, sau khi lưu đơn hàng thành công, nó gọi `MessageProducer` để gửi thông tin đơn hàng vào RabbitMQ.

    ```java
    // Trong OrderService.java
    Order savedOrder = orderRepository.save(order);
    messageProducer.sendOrderConfirmationEmailMessage(savedOrder);
    ```

    ```java
    // Trong MessageProducer.java
    public void sendOrderConfirmationEmailMessage(Order order) {
      log.debug("Sending message for order confirmation email: {}", order.getId());
      rabbitTemplate.convertAndSend(RabbitMQConfig.APP_EXCHANGE, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY, order);
    }

    ```

  - **Consumer (Bên nhận):** Lớp `EmailConsumer.java` (hoặc `EmailService.java`) có một phương thức "lắng nghe" hàng đợi. Khi có message mới, nó sẽ được tự động gọi để thực thi.

    ```java
    // Trong EmailConsumer.java
    @RabbitListener(queues = RabbitMQConfig.ORDER_EMAIL_QUEUE)
    public void receiveOrderEmailMessage(Order order) {
      log.info("Received message to send order confirmation email for order: {}", order.getId());
      mailService.sendOrderConfirmationEmail(order);
    }

    ```

- **5. Làm vậy có tác dụng gì?**
  - **Cải thiện trải nghiệm người dùng:** Người dùng nhận được phản hồi "Đặt hàng thành công" ngay lập tức mà không phải chờ đợi quá trình gửi email hoàn tất.
  - **Tăng độ tin cậy (Resilience):** Nếu dịch vụ email tạm thời bị lỗi, message vẫn nằm an toàn trong RabbitMQ. Hệ thống có thể được cấu hình để tự động thử lại (retry) việc gửi email sau một khoảng thời gian.
  - **Giảm tải cho Backend:** Tách biệt tác vụ gửi email giúp luồng xử lý chính của API không bị chặn, cho phép backend xử lý nhiều yêu cầu hơn.

### 📬 RabbitMQ – Chuyên sâu

- **Exchange Types: Lựa chọn chiến lược routing**

  - Exchange là "bưu điện", quyết định message sẽ đi đâu. Việc chọn đúng loại exchange là rất quan trọng.
    - **Direct:** Routing key khớp chính xác. Dùng khi muốn gửi message đến một consumer cụ thể.
    - **Fanout:** "Phát thanh", gửi đến tất cả queue đã binding. Dùng để broadcast thông báo.
    - **Topic:** Linh hoạt nhất, routing dựa trên pattern (ví dụ `order.*.new`, `#.payment`). **Đây là lựa chọn tốt nhất cho các hệ thống phức tạp** vì nó cho phép các consumer khác nhau lắng nghe các nhóm sự kiện khác nhau một cách linh hoạt. Ví dụ, service `Notification` có thể lắng nghe `order.created`, trong khi service `Analytics` lắng nghe `order.#` (tất cả sự kiện liên quan đến order).

- **Dead Letter Queue (DLQ): Xử lý các message lỗi**

  - **Vấn đề:** Điều gì xảy ra khi một message không thể được xử lý (do lỗi code, dữ liệu sai...)? Nếu để nó quay lại queue, nó sẽ gây ra vòng lặp xử lý lỗi vô hạn.
  - **Giải pháp (DLQ):** Là một queue đặc biệt để chứa các message "chết". Khi một message bị consumer từ chối (reject) một số lần nhất định, RabbitMQ sẽ tự động chuyển nó vào DLQ.
  - **Lợi ích:** Giúp cô lập các message lỗi để dev phân tích, sửa lỗi và có thể xử lý lại thủ công sau đó, đồng thời giữ cho queue chính không bị tắc nghẽn.

- **Retry & Backoff: Tăng khả năng chịu lỗi cho các sự cố tạm thời**

  - **Vấn đề:** Một consumer có thể bị lỗi do các vấn đề tạm thời (mất kết nối mạng, API bên thứ 3 quá tải). Việc đưa message vào DLQ ngay lập tức là không tối ưu.
  - **Giải pháp:** Cấu hình cơ chế thử lại (retry). Một kỹ thuật phổ biến là sử dụng DLQ và TTL để tạo ra các hàng đợi retry. Khi xử lý lỗi, message được gửi đến một hàng đợi `retry-5s` (sẽ tự động được chuyển về queue chính sau 5s). Nếu vẫn lỗi, nó được gửi đến hàng đợi `retry-30s`,...
  - **Exponential Backoff:** Khoảng thời gian giữa các lần thử lại nên tăng dần (5s, 30s, 5 phút...) để tránh "dội" vào một hệ thống đang gặp sự cố.

- **Idempotency: Đảm bảo xử lý message nhiều lần vẫn đúng**
  - **Định nghĩa:** Một thao tác được gọi là idempotent nếu việc thực thi nó nhiều lần cho cùng một kết quả như thực thi một lần.
  - **Tại sao quan trọng?** RabbitMQ đảm bảo cơ chế **"at-least-once delivery"** (giao ít nhất một lần), nghĩa là một message có thể được giao **nhiều hơn một lần** trong trường hợp có lỗi mạng hoặc consumer crash.
  - **Vấn đề:** Nếu consumer không idempotent, việc xử lý lại message sẽ gây ra lỗi (ví dụ: trừ tiền 2 lần, tạo 2 đơn hàng giống hệt nhau).
  - **Giải pháp:** Thiết kế consumer có tính idempotent. Cách phổ biến là gắn một ID duy nhất cho mỗi message/sự kiện. Trước khi xử lý, consumer kiểm tra xem ID này đã được xử lý trước đó hay chưa (bằng cách lưu các ID đã xử lý vào DB hoặc Redis).

#### 🔹 Lập trình hướng khía cạnh (AOP) & Ghi log

- **1. Nó là gì?**

  - AOP (Aspect-Oriented Programming): Là một kỹ thuật lập trình cho phép tách các **mối quan tâm xuyên suốt (cross-cutting concerns)** như logging, transaction management, security ra khỏi logic nghiệp vụ chính.
  - Ghi log: Là quá trình ghi lại các sự kiện xảy ra trong một ứng dụng (ví dụ: lỗi, cảnh báo, thông tin gỡ lỗi) vào một file hoặc console.

- **2. Dùng làm gì?**

  - Để tự động hóa các tác vụ lặp đi lặp lại mà không làm "rối" code nghiệp vụ, giúp code sạch hơn, dễ bảo trì và tuân thủ nguyên tắc Single Responsibility.

- **3. Vì sao dùng cái này trong dự án?**

  - Việc ghi log để theo dõi và gỡ lỗi là cực kỳ quan trọng trong mọi ứng dụng. Tuy nhiên, việc chèn code `log.info(...)` vào đầu và cuối mỗi phương thức service/controller sẽ làm code trở nên dài dòng, khó đọc, và khó bảo trì (nếu muốn thay đổi định dạng log, phải sửa ở nhiều nơi).
  - AOP là giải pháp hoàn hảo để tự động hóa việc ghi log một cách tập trung và không xâm lấn vào code nghiệp vụ.

- **4. Áp dụng vào đâu trong dự án? (Dự án đã áp dụng như thế nào)**

  - File `LoggingAspect.java` được đánh dấu `@Aspect` để chỉ định nó là một Aspect.
  - Một `@Pointcut` được định nghĩa để "bắt" tất cả các phương thức trong các package `com.mycompany.myapp.service` và `com.mycompany.myapp.web.rest`.

    ```java
    @Pointcut(
      "within(@org.springframework.stereotype.Repository *)" +
      " || within(@org.springframework.stereotype.Service *)" +
      " || within(@org.springframework.web.bind.annotation.RestController *)"
    )
    public void applicationPackagePointcut() {
      // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    ```

  - Các "lời khuyên" (Advices) được sử dụng:
    - `@Before("applicationPackagePointcut()")`: Ghi log tên class, tên phương thức và các tham số đầu vào **trước khi** phương thức được thực thi.
    - `@AfterReturning(pointcut = "applicationPackagePointcut()", returning = "result")`: Ghi log kết quả trả về và tổng thời gian thực thi của phương thức **sau khi** nó hoàn thành thành công.

- **5. Làm vậy có tác dụng gì?**
  - **Tự động hóa hoàn toàn và không xâm lấn:** Lập trình viên không cần viết code log thủ công trong từng phương thức nghiệp vụ, giữ cho code nghiệp vụ sạch sẽ và dễ đọc.
  - **Hỗ trợ gỡ lỗi và giám sát hiệu quả:** Khi có lỗi hoặc cần phân tích, có thể dễ dàng theo dõi toàn bộ luồng thực thi của một yêu cầu qua các lớp Service và Controller, cùng với dữ liệu vào/ra và thời gian xử lý tại mỗi bước, giúp nhanh chóng xác định nguyên nhân gốc rễ.

### 🧩 AOP & Logging – Chuyên sâu

- **Correlation ID: Truy vết request xuyên suốt hệ thống**

  - **Vấn đề:** Trong kiến trúc microservices, một request có thể đi qua nhiều service. Việc xâu chuỗi các dòng log từ các service khác nhau để theo dõi một request duy nhất là cực kỳ khó khăn.
  - **Giải pháp:** Gắn một **Correlation ID** (hoặc Trace ID) duy nhất cho mỗi request ngay tại cổng vào (API Gateway hoặc Filter). ID này sau đó được truyền đi qua các header HTTP hoặc message header.
  - **Cách thực hiện:** Sử dụng **MDC (Mapped Diagnostic Context)** của SLF4J để tự động thêm Correlation ID vào mọi dòng log được ghi bởi thread xử lý request đó. Khi xem log trên một hệ thống tập trung, bạn chỉ cần lọc theo ID này là có thể thấy toàn bộ hành trình của request.

- **Centralized Logging: Tập trung hóa log để dễ dàng tìm kiếm và phân tích**

  - **Vấn đề:** Khi ứng dụng chạy trên nhiều server, log file nằm rải rác khắp nơi. Việc SSH vào từng máy để xem log là bất khả thi.
  - **Giải pháp:** Đẩy tất cả log từ mọi instance về một nơi duy nhất.
  - **Công cụ (ELK Stack):**
    - **Elasticsearch:** Lưu trữ và đánh index tất cả các dòng log.
    - **Logstash (hoặc Filebeat/Fluentd):** "Shipper" thu thập log từ các file, parse chúng (thường sang định dạng JSON) và gửi đến Elasticsearch.
    - **Kibana:** Giao diện web mạnh mẽ để tìm kiếm, lọc, và trực quan hóa log (ví dụ: "hiển thị tất cả log `ERROR` có `correlationId=xyz` trong 5 phút qua").

- **Performance Monitoring: Tách biệt metric khỏi log**
  - **Vấn đề:** Ghi log thời gian thực thi của mọi phương thức tạo ra quá nhiều "nhiễu" và không phải là cách hiệu quả để giám sát hiệu năng tổng thể.
  - **Giải pháp:** Sử dụng AOP để gửi dữ liệu dưới dạng **metric** thay vì log.
  - **Cách thực hiện:** Dùng thư viện **Micrometer** (tích hợp sẵn trong Spring Boot Actuator) và một AOP Aspect. Aspect này sẽ đo thời gian thực thi của các phương thức quan trọng và ghi nhận nó dưới dạng một `Timer` metric. Các metric này sau đó được phơi bày qua endpoint `/actuator/prometheus` và được các công cụ như **Prometheus** thu thập, **Grafana** trực quan hóa. Điều này cho phép bạn theo dõi P95 latency, request rate... một cách chuyên nghiệp.

#### 🗄️ Database & JPA – Chuyên sâu

- **Optimistic vs Pessimistic Locking: Xử lý xung đột dữ liệu**

  - **Vấn đề (Lost Update):** Hai người dùng cùng sửa một bản ghi. Người sửa xong sau sẽ ghi đè lên dữ liệu của người sửa xong trước, làm mất mát dữ liệu.
  - **Giải pháp 1: Optimistic Locking (Khóa lạc quan):**
    - **Tư tưởng:** "Xung đột hiếm khi xảy ra". Cứ để sửa, nhưng trước khi lưu, hãy kiểm tra xem có ai khác đã sửa bản ghi trong lúc mình đang làm việc không.
    - **Cách làm:** Thêm một cột `@Version` vào entity. Khi `UPDATE`, JPA sẽ tự động thêm điều kiện `AND version = <version_lúc_đọc>`. Nếu có người khác đã cập nhật (làm `version` thay đổi), câu `UPDATE` sẽ thất bại và JPA ném ra `ObjectOptimisticLockingFailureException`, buộc người dùng phải làm lại.
    - **Khi nào dùng:** Tốt cho các ứng dụng có tỷ lệ đọc cao, ghi thấp.
  - **Giải pháp 2: Pessimistic Locking (Khóa bi quan):**
    - **Tư tưởng:** "Xung đột dễ xảy ra". Khóa bản ghi lại ngay khi đọc để không ai khác có thể động vào.
    - **Cách làm:** Sử dụng `LockModeType.PESSIMISTIC_WRITE` khi truy vấn. Database sẽ khóa bản ghi đó lại cho đến khi transaction kết thúc. Các transaction khác muốn truy cập sẽ phải chờ.
    - **Khi nào dùng:** Tốt cho các hệ thống có tỷ lệ xung đột cao, yêu cầu tính toàn vẹn nghiêm ngặt (ví dụ: tài chính, đặt vé). Nhược điểm là có thể làm giảm hiệu năng và gây deadlock.

- **Batch Processing: Tăng tốc độ xử lý dữ liệu hàng loạt**

  - **Vấn đề:** Khi import 10,000 dòng từ Excel, việc gọi `repository.save()` 10,000 lần sẽ tạo ra 10,000 lượt giao tiếp mạng với DB, cực kỳ chậm.
  - **Giải pháp:** Nhóm nhiều câu lệnh `INSERT`/`UPDATE` thành một "lô" (batch) và gửi đến DB trong một lần.
  - **Cách cấu hình:** Bật chế độ batch trong `application.yml` (`spring.jpa.properties.hibernate.jdbc.batch_size=50`) và sử dụng `repository.saveAll(entities)`. Hibernate sẽ tự động nhóm các câu lệnh lại.
  - **Lợi ích:** Tăng tốc độ import/update dữ liệu hàng loạt lên nhiều lần.

- **Stored Procedure Best Practices: Trả về DTO thay vì Entity**
  - **Vấn đề:** Nếu một Stored Procedure (SP) trả về `SELECT *` và bạn map kết quả vào một Entity phức tạp, bạn có thể bị **over-fetching** (lấy thừa dữ liệu) và gây ra **N+1 Select Problem** nếu entity đó có các quan hệ EAGER.
  - **Giải pháp:**
    1.  Sửa SP để chỉ `SELECT` những cột cần thiết.
    2.  Tạo một **DTO (Data Transfer Object)** hoặc một **Interface-based Projection** chỉ chứa các trường đó.
    3.  Trong Repository, map kết quả của SP vào DTO/Interface thay vì Entity.
  - **Lợi ích:** Tăng hiệu suất đáng kể bằng cách giảm lượng dữ liệu truyền qua mạng và tránh các truy vấn không cần thiết.

---

## 🗺️ CHI TIẾT TÍNH NĂNG THEO TRANG

### 🏠 TRANG CHỦ (Home)

**URL:** `/`

- **Công nghệ áp dụng:** ✅ **Redis Cache** (cache sản phẩm nổi bật để tăng tốc độ tải), ✅ **JPA Query** (`findByFeaturedTrue()` để lấy sản phẩm có thuộc tính `featured` là `true`), ✅ **Lazy Loading** (tải ảnh khi cuộn để tối ưu hiệu suất).
- **File liên quan:** `home.component.ts`, `ProductResource.java`.

---

### 🛍️ DANH SÁCH SẢN PHẨM (Product List)

**URL:** `/products`

- **Công nghệ áp dụng:** ✅ **Redis Cache** (cache dữ liệu sản phẩm theo từng trang để giảm tải DB), ✅ **Pagination** (Spring Data Pageable để xử lý phân trang hiệu quả ở backend), ✅ **Custom Query** (sử dụng `@Query` trong `ProductRepository` để lọc sản phẩm theo `slug` của danh mục), ✅ **Debounce Search** (RxJS `debounceTime` ở frontend để tránh gửi quá nhiều request tìm kiếm khi người dùng gõ).
- **File liên quan:** `product.component.ts`, `ProductRepository.java`.

---

### 📦 CHI TIẾT SẢN PHẨM (Product Detail)

**URL:** `/products/:id`

- **Công nghệ áp dụng:** ✅ **Redis Cache** (cache thông tin chi tiết của từng sản phẩm để tăng tốc độ truy xuất), ✅ **LocalStorage** (lưu thông tin giỏ hàng tạm thời của người dùng).
- **File liên quan:** `product-detail.component.ts`, `ProductResource.java`.

---

### 🛒 GIỎ HÀNG (Cart)

**URL:** `/cart`

- **Công nghệ áp dụng:** ✅ **LocalStorage** (lưu dữ liệu giỏ hàng để duy trì giữa các phiên), ✅ **RxJS BehaviorSubject** (`CartService` để quản lý trạng thái giỏ hàng một cách reactive và đồng bộ trên toàn ứng dụng).
- **File liên quan:** `cart.component.ts`, `cart.service.ts`.

---

### 💳 CHECKOUT (Thanh toán)

**URL:** `/checkout`

- **Công nghệ áp dụng:** ✅ **Reactive Forms** (xây dựng form thông tin giao hàng với validation mạnh mẽ, dễ kiểm thử), ✅ **@Transactional** (đảm bảo toàn vẹn dữ liệu khi tạo đơn hàng và cập nhật tồn kho), ✅ **RabbitMQ** (gửi sự kiện đặt hàng thành công để gửi email xác nhận một cách bất đồng bộ, không làm chậm phản hồi API).
- **File liên quan:** `checkout.component.ts`, `OrderService.java`, `MessageProducer.java`.

---

### 🔐 ĐĂNG NHẬP/ĐĂNG KÝ (Login/Register)

**URL:** `/login`, `/register`

- **Công nghệ áp dụng:** ✅ **JWT** (tạo token xác thực sau khi đăng nhập), ✅ **BCrypt** (mã hóa mật khẩu an toàn), ✅ **LocalStorage** (lưu JWT token), ✅ **RabbitMQ** (gửi email xác nhận đăng ký bất đồng bộ).
- **File liên quan:** `login.component.ts`, `AccountResource.java`, `UserService.java`.

---

### 👨‍💼 ADMIN DASHBOARD

**URL:** `/admin`

- **Công nghệ áp dụng:** ✅ **Stored Procedure** (gọi `sp_get_dashboard_stats` từ SQL Server để lấy dữ liệu thống kê tổng hợp hiệu quả), ✅ **@PreAuthorize** (chỉ cho phép người dùng có `ROLE_ADMIN` truy cập).
- **File liên quan:** `admin-dashboard.component.ts`, `DashboardService.java`.

---

### 📦 QUẢN LÝ SẢN PHẨM (Admin Products)

**URL:** `/admin/products`

- **Công nghệ áp dụng:** ✅ **Apache POI** (thư viện Java để Import/Export dữ liệu sản phẩm từ/ra file Excel), ✅ **Multipart File** (xử lý upload ảnh sản phẩm), ✅ **@CacheEvict** (xóa cache sản phẩm khi có thay đổi để đảm bảo dữ liệu nhất quán).
- **File liên quan:** `product-management.component.ts`, `FileImportService.java`.

---

## 🎯 ĐỐI CHIẾU YÊU CẦU BÀI TẬP CUỐI KHÓA

Phần này kiểm tra việc hoàn thành các yêu cầu kỹ thuật của bài tập cuối khóa thông qua các tính năng đã được triển khai trong dự án.

| Yêu cầu Kỹ thuật              | Trạng thái        | Minh chứng trong Dự án                                                                                                                                                                                                  |
| :---------------------------- | :---------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Có tạo Bean**               | ✅ **Hoàn thành** | Toàn bộ dự án Spring Boot được xây dựng dựa trên các Spring Beans (`@Bean`, `@Component`, `@Service`, `@Repository`). Ví dụ điển hình là `SecurityFilterChain` trong `SecurityConfiguration.java`.                      |
| **Có Exception Handling**     | ✅ **Hoàn thành** | Sử dụng `@ControllerAdvice` và các class Exception tùy chỉnh (`BadRequestAlertException`) để xử lý lỗi tập trung, trả về response chung.                                                                                |
| **Interceptor ở Angular**     | ✅ **Hoàn thành** | `AuthInterceptor.ts` tự động thêm JWT token vào header của các request.                                                                                                                                                 |
| **Interceptor ở Spring Boot** | ✅ **Hoàn thành** | `SecurityFilterChain` hoạt động như một chuỗi các interceptor (filter) để xử lý mọi request đến.                                                                                                                        |
| **Sử dụng JPA**               | ✅ **Hoàn thành** | Toàn bộ tầng Repository sử dụng `JpaRepository` để thao tác với database.                                                                                                                                               |
| **Sử dụng Custom SQL**        | ✅ **Hoàn thành** | Sử dụng annotation `@Query` trong các Repository để viết các truy vấn JPQL phức tạp.                                                                                                                                    |
| **Sử dụng Stored Procedures** | ✅ **Hoàn thành** | Sử dụng annotation `@Procedure` để gọi Stored Procedure từ SQL Server cho trang Dashboard.                                                                                                                              |
| **Ghi log ứng dụng**          | ✅ **Hoàn thành** | Sử dụng SLF4J và cấu hình Logback. Đặc biệt, đã tự custom Aspect (`LoggingAspect.java`) để ghi log tự động cho các lời gọi service, giúp theo dõi chi tiết mà không làm "rối" code nghiệp vụ.                           |
| **Tự custom Aspect**          | ✅ **Hoàn thành** | `LoggingAspect.java` được tạo bằng Spring AOP (`@Aspect`, `@Before`, `@AfterReturning`) để bao bọc các phương thức trong tầng Service, thể hiện rõ kỹ năng tách rời các cross-cutting concerns.                         |
| **Phân quyền**                | ✅ **Hoàn thành** | Hệ thống phân quyền `ROLE_ADMIN` và `ROLE_USER` được định nghĩa chặt chẽ trong `SecurityConfiguration.java` và áp dụng qua `@PreAuthorize`.                                                                             |
| **Audit (Kiểm toán)**         | ✅ **Hoàn thành** | Sử dụng `AbstractAuditingEntity` với các trường `createdBy`, `createdDate`, `lastModifiedBy`, `lastModifiedDate`. Spring Data JPA tự động điền các giá trị này.                                                         |
| **Xử lý bất đồng bộ**         | ✅ **Hoàn thành** | **(Điểm nhấn)** Sử dụng RabbitMQ và Spring AMQP. Khi đặt hàng, `OrderService` gửi message tới RabbitMQ. `EmailService` lắng nghe và xử lý việc gửi email độc lập, giúp API `/api/orders` phản hồi ngay lập tức.         |
| **Sử dụng Cache**             | ✅ **Hoàn thành** | **(Điểm nhấn)** Sử dụng Redis và Spring Cache. Annotation `@Cacheable("products")` trong `ProductService` tự động cache kết quả truy vấn. `@CacheEvict` tự động xóa cache khi dữ liệu thay đổi, đảm bảo tính nhất quán. |
