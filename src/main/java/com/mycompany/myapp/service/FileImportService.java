package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.CategoryRepository;
import com.mycompany.myapp.repository.CustomerRepository;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class FileImportService {

    private final Logger log = LoggerFactory.getLogger(FileImportService.class);

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;

    public FileImportService(
        ProductRepository productRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthorityRepository authorityRepository,
        CategoryRepository categoryRepository,
        CustomerRepository customerRepository
    ) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.categoryRepository = categoryRepository;
        this.customerRepository = customerRepository;
    }

    // Existing implementation methods
    public void importProducts(MultipartFile file) throws Exception {
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new BadRequestAlertException("Chỉ chấp nhận file Excel định dạng .xlsx", "fileImport", "invalidFileFormat");
        }
        try (InputStream inputStream = file.getInputStream()) {
            processProductExcel(inputStream);
        } catch (Exception e) {
            log.error("Lỗi khi import sản phẩm từ file: {}", e.getMessage());
            throw e;
        }
    }

    public void importProductsFromUrl(String urlString) throws Exception {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
                processProductExcel(inputStream);
            }
        } catch (Exception e) {
            log.error("Lỗi khi import sản phẩm từ URL: {}", e.getMessage());
            throw e;
        }
    }

    private void processProductExcel(InputStream inputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            List<Product> productsToSave = new ArrayList<>();
            int rowNumber = 0;

            // --- Get embedded images from Excel ---
            Map<String, byte[]> imageMap = extractImagesFromExcel(workbook, sheet);
            log.debug("Found {} embedded images in Excel", imageMap.size());

            // --- Optimization: Pre-load categories ---
            Map<String, Category> categoryCacheByName = new HashMap<>();
            Map<String, Category> categoryCacheBySlug = new HashMap<>();
            categoryRepository
                .findAll()
                .forEach(cat -> {
                    if (cat.getName() != null) {
                        categoryCacheByName.put(cat.getName().trim(), cat);
                    }
                    if (cat.getSlug() != null) {
                        categoryCacheBySlug.put(cat.getSlug().trim(), cat);
                    }
                });

            Category defaultCategory = categoryCacheBySlug.get("chua-phan-loai");
            if (defaultCategory == null) {
                defaultCategory = categoryRepository
                    .findBySlug("chua-phan-loai")
                    .orElseGet(() -> {
                        Category newCat = new Category();
                        newCat.setName("Chưa phân loại");
                        newCat.setSlug("chua-phan-loai");
                        return categoryRepository.save(newCat);
                    });
                categoryCacheBySlug.put("chua-phan-loai", defaultCategory);
                if (defaultCategory.getName() != null) {
                    categoryCacheByName.put(defaultCategory.getName().trim(), defaultCategory);
                }
            }
            // --- End Optimization ---

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Product product = new Product();
                Cell idCell = currentRow.getCell(0);
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                    product.setId((long) idCell.getNumericCellValue());
                }

                try {
                    String name = getCellValue(currentRow.getCell(1));
                    if (name == null || name.trim().isEmpty()) {
                        throw new IllegalArgumentException("Tên sản phẩm không được để trống");
                    }
                    product.setName(name.trim());

                    String description = getCellValue(currentRow.getCell(2));
                    product.setDescription((description == null || description.trim().isEmpty()) ? "Chưa có mô tả" : description.trim());

                    Cell priceCell = currentRow.getCell(3);
                    if (priceCell == null || priceCell.getCellType() != CellType.NUMERIC) {
                        throw new IllegalArgumentException("Giá sản phẩm không được để trống và phải là số");
                    }
                    product.setPrice(BigDecimal.valueOf(priceCell.getNumericCellValue()));

                    Cell quantityCell = currentRow.getCell(4);
                    if (quantityCell != null && quantityCell.getCellType() == CellType.NUMERIC) {
                        product.setQuantity((int) quantityCell.getNumericCellValue());
                    } else {
                        product.setQuantity(0);
                    }

                    String imageUrl = getCellValue(currentRow.getCell(5));

                    // Check if there's an embedded image for this row
                    String imageKey = sheet.getSheetName() + "_" + rowNumber;
                    byte[] imageData = imageMap.get(imageKey);

                    if (imageData != null && imageData.length > 0) {
                        // Image found in Excel - save as binary
                        product.setImageData(imageData);
                        // Detect content type based on image signature
                        String contentType = detectImageContentType(imageData);
                        product.setImageContentType(contentType);
                        // Generate data URL for preview
                        String base64 = java.util.Base64.getEncoder().encodeToString(imageData);
                        product.setImageUrl("data:" + contentType + ";base64," + base64);
                        log.debug("Loaded embedded image for row {}, size: {} bytes", rowNumber, imageData.length);
                    } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        // No embedded image - use URL from cell
                        product.setImageUrl(imageUrl.trim());
                    } else {
                        // No image at all - use placeholder
                        product.setImageUrl("https://via.placeholder.com/300x300?text=No+Image");
                    }

                    // --- Use category cache ---
                    String categoryInput = getCellValue(currentRow.getCell(7));
                    Category category;

                    if (categoryInput == null || categoryInput.trim().isEmpty()) {
                        category = defaultCategory;
                    } else {
                        String trimmedInput = categoryInput.trim();
                        category = categoryCacheByName.get(trimmedInput);
                        if (category == null) {
                            category = categoryCacheBySlug.get(trimmedInput);
                        }

                        if (category == null) {
                            throw new IllegalArgumentException(
                                "Không tìm thấy danh mục '" + categoryInput + "'. Vui lòng kiểm tra lại tên danh mục hoặc tạo danh mục mới."
                            );
                        }
                    }
                    product.setCategory(category);
                    // --- End category cache usage ---

                } catch (BadRequestAlertException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BadRequestAlertException(
                        "Lỗi tại dòng " + (rowNumber + 1) + ": " + e.getMessage(),
                        "fileImport",
                        "dataReadError"
                    );
                }

                if (product.getId() != null) {
                    Optional<Product> existingProductOpt = productRepository.findById(product.getId());
                    if (existingProductOpt.isEmpty()) {
                        throw new IllegalArgumentException(
                            "Không tìm thấy sản phẩm với ID=" + product.getId() + ". Vui lòng kiểm tra lại hoặc để trống cột ID để tạo mới."
                        );
                    }

                    Product existingProduct = existingProductOpt.get();

                    if (!existingProduct.getName().equals(product.getName())) {
                        log.warn(
                            "Cảnh báo: Sản phẩm ID={} đang đổi tên từ '{}' thành '{}'",
                            product.getId(),
                            existingProduct.getName(),
                            product.getName()
                        );
                    }

                    product.setCreatedBy(existingProduct.getCreatedBy());
                    product.setCreatedDate(existingProduct.getCreatedDate());
                } else {
                    Optional<Product> duplicateProduct = productRepository.findFirstByName(product.getName());
                    if (duplicateProduct.isPresent()) {
                        throw new IllegalArgumentException(
                            "Sản phẩm '" +
                            product.getName() +
                            "' đã tồn tại (ID=" +
                            duplicateProduct.get().getId() +
                            "). " +
                            "Nếu muốn cập nhật, vui lòng điền ID=" +
                            duplicateProduct.get().getId() +
                            " vào cột A."
                        );
                    }
                }
                productsToSave.add(product);
                rowNumber++;
            }
            productRepository.saveAll(productsToSave);
        }
    }

    public void importUsers(MultipartFile file) throws Exception {
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new BadRequestAlertException("Chỉ chấp nhận file Excel định dạng .xlsx", "fileImport", "invalidFileFormat");
        }
        try (InputStream inputStream = file.getInputStream()) {
            processUserExcel(inputStream);
        } catch (Exception e) {
            log.error("Lỗi khi import người dùng từ file: {}", e.getMessage());
            throw e;
        }
    }

    public void importUsersFromUrl(String urlString) throws Exception {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
                processUserExcel(inputStream);
            }
        } catch (Exception e) {
            log.error("Lỗi khi import người dùng từ URL: {}", e.getMessage());
            throw e;
        }
    }

    private void processUserExcel(InputStream inputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            List<User> usersToSave = new ArrayList<>();
            int rowNumber = 0;

            Authority userAuthority = authorityRepository
                .findById(AuthoritiesConstants.USER)
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy quyền USER mặc định", "fileImport", "userAuthorityNotFound")
                );

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                try {
                    // Đọc dữ liệu từ các cột (theo thứ tự: Phone, FirstName, LastName, Email)
                    String phoneNumber = getCellValue(currentRow.getCell(0));
                    String firstName = getCellValue(currentRow.getCell(1));
                    String lastName = getCellValue(currentRow.getCell(2));
                    String email = getCellValue(currentRow.getCell(3));

                    // Validate: Phải có ít nhất SĐT hoặc Email
                    if ((phoneNumber == null || phoneNumber.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
                        throw new IllegalArgumentException("Phải có ít nhất số điện thoại hoặc email");
                    }

                    // Tìm user theo SĐT hoặc Email
                    User user = null;
                    boolean isUpdate = false;

                    // Ưu tiên tìm theo số điện thoại
                    if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                        Optional<User> existingByPhone = userRepository.findOneByPhone(phoneNumber.trim());
                        if (existingByPhone.isPresent()) {
                            user = existingByPhone.get();
                            isUpdate = true;
                        }
                    }

                    // Nếu không tìm thấy theo SĐT, tìm theo email
                    if (user == null && email != null && !email.trim().isEmpty()) {
                        Optional<User> existingByEmail = userRepository.findOneByEmailIgnoreCase(email.trim());
                        if (existingByEmail.isPresent()) {
                            user = existingByEmail.get();
                            isUpdate = true;
                        }
                    }

                    // Nếu không tìm thấy, tạo mới
                    if (user == null) {
                        user = new User();
                        user.setPassword(passwordEncoder.encode("123456"));
                        user.setActivated(true);
                        user.setLangKey("vi");
                        user.setAuthority(userAuthority);
                        log.info("Creating new user: {}", phoneNumber);
                    }

                    // Cập nhật thông tin
                    if (firstName != null && !firstName.trim().isEmpty()) {
                        user.setFirstName(firstName.trim());
                    }

                    if (lastName != null && !lastName.trim().isEmpty()) {
                        user.setLastName(lastName.trim());
                    }

                    if (email != null && !email.trim().isEmpty()) {
                        String emailLower = email.trim().toLowerCase();
                        // Kiểm tra email đã tồn tại chưa (nếu không phải của user hiện tại)
                        Optional<User> userWithEmail = userRepository.findOneByEmailIgnoreCase(emailLower);
                        if (userWithEmail.isEmpty() || (user.getId() != null && userWithEmail.get().getId().equals(user.getId()))) {
                            user.setEmail(emailLower);
                        } else {
                            log.warn("Dòng {}: Email {} đã tồn tại, bỏ qua", rowNumber + 1, emailLower);
                        }
                    }

                    if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                        String phoneTrim = phoneNumber.trim();
                        // Kiểm tra SĐT đã tồn tại chưa (nếu không phải của user hiện tại)
                        Optional<User> userWithPhone = userRepository.findOneByPhone(phoneTrim);
                        if (userWithPhone.isEmpty() || (user.getId() != null && userWithPhone.get().getId().equals(user.getId()))) {
                            user.setPhone(phoneTrim);
                        } else {
                            log.warn("Dòng {}: Số điện thoại {} đã tồn tại, bỏ qua", rowNumber + 1, phoneTrim);
                        }
                    }

                    usersToSave.add(user);
                    log.info("Processed user at row {}: {} - {}", rowNumber + 1, isUpdate ? "UPDATE" : "CREATE", user.getEmail());
                } catch (Exception e) {
                    log.error("Error at row {}: {}", rowNumber + 1, e.getMessage());
                    throw new BadRequestAlertException(
                        "Lỗi đọc dữ liệu người dùng tại dòng " + (rowNumber + 1) + ": " + e.getMessage(),
                        "fileImport",
                        "dataReadError"
                    );
                }
                rowNumber++;
            }

            userRepository.saveAll(usersToSave);
        }
    }

    /**
     * Import khách hàng mua offline
     * Format: SĐT | Tên | Sản phẩm đã mua
     */
    public void importCustomers(MultipartFile file) throws Exception {
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new BadRequestAlertException("Chỉ chấp nhận file Excel định dạng .xlsx", "fileImport", "invalidFileFormat");
        }
        try (InputStream inputStream = file.getInputStream()) {
            processCustomerExcel(inputStream);
        } catch (Exception e) {
            log.error("Lỗi khi import khách hàng từ file: {}", e.getMessage());
            throw e;
        }
    }

    private void processCustomerExcel(InputStream inputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            List<com.mycompany.myapp.domain.Customer> customersToSave = new ArrayList<>();
            int rowNumber = 0;

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) {
                    rowNumber++;
                    continue; // Skip header
                }

                try {
                    // Đọc: SĐT | Tên | Sản phẩm đã mua | Ngày mua hàng (TẤT CẢ BẮT BUỘC)
                    String phone = getCellValue(currentRow.getCell(0));
                    String fullName = getCellValue(currentRow.getCell(1));
                    String productsPurchased = getCellValue(currentRow.getCell(2));
                    Cell purchaseDateCell = currentRow.getCell(3);

                    // Validate: TẤT CẢ 4 CỘT ĐỀU BẮT BUỘC
                    if (phone == null || phone.trim().isEmpty()) {
                        throw new IllegalArgumentException("Cột A (SĐT) không được để trống");
                    }

                    if (fullName == null || fullName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Cột B (Tên) không được để trống");
                    }

                    if (productsPurchased == null || productsPurchased.trim().isEmpty()) {
                        throw new IllegalArgumentException("Cột C (Sản phẩm đã mua) không được để trống");
                    }

                    if (purchaseDateCell == null) {
                        throw new IllegalArgumentException("Cột D (Ngày mua hàng) không được để trống");
                    }

                    // Parse ngày mua hàng (BẮT BUỘC)
                    java.time.Instant purchaseDate = null;
                    if (purchaseDateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(purchaseDateCell)) {
                        // Excel date format
                        purchaseDate = purchaseDateCell.getDateCellValue().toInstant();
                    } else {
                        String dateStr = getCellValue(purchaseDateCell);
                        if (dateStr == null || dateStr.trim().isEmpty()) {
                            throw new IllegalArgumentException("Cột D (Ngày mua hàng) không được để trống");
                        }
                        try {
                            // Parse dd/MM/yyyy
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            java.time.LocalDate localDate = java.time.LocalDate.parse(dateStr.trim(), formatter);
                            purchaseDate = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
                        } catch (Exception e) {
                            throw new IllegalArgumentException(
                                "Cột D (Ngày mua hàng) sai định dạng. Vui lòng dùng format: dd/MM/yyyy (VD: 31/12/2025)"
                            );
                        }
                    }

                    // Tìm customer theo SĐT
                    com.mycompany.myapp.domain.Customer customer = customerRepository.findOneByPhone(phone.trim()).orElse(null);

                    if (customer == null) {
                        // Tạo mới
                        customer = new com.mycompany.myapp.domain.Customer();
                        customer.setPhone(phone.trim());
                        customer.setFullName(fullName.trim());
                        customer.setProductsPurchased(productsPurchased.trim());
                        customer.setLastPurchaseDate(purchaseDate);
                        log.info("Tạo mới khách hàng: {} - Ngày mua: {}", phone, purchaseDate);
                    } else {
                        // Cập nhật
                        customer.setFullName(fullName.trim());

                        // Append sản phẩm mới vào danh sách cũ
                        String existingProducts = customer.getProductsPurchased();
                        if (existingProducts != null && !existingProducts.isEmpty()) {
                            customer.setProductsPurchased(existingProducts + ", " + productsPurchased.trim());
                        } else {
                            customer.setProductsPurchased(productsPurchased.trim());
                        }

                        // Cập nhật ngày mua gần nhất
                        customer.setLastPurchaseDate(purchaseDate);
                        log.info("Cập nhật khách hàng: {} - Ngày mua: {}", phone, purchaseDate);
                    }

                    customersToSave.add(customer);
                } catch (Exception e) {
                    log.error("Lỗi tại dòng {}: {}", rowNumber + 1, e.getMessage());
                    throw new BadRequestAlertException(
                        "Lỗi đọc dữ liệu khách hàng tại dòng " + (rowNumber + 1) + ": " + e.getMessage(),
                        "fileImport",
                        "dataReadError"
                    );
                }
                rowNumber++;
            }

            customerRepository.saveAll(customersToSave);
            log.info("Import thành công {} khách hàng", customersToSave.size());
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else {
            return null;
        }
    }

    /**
     * Extract embedded images from Excel file
     * Returns a map of "sheetName_rowIndex" -> imageData
     */
    private Map<String, byte[]> extractImagesFromExcel(Workbook workbook, Sheet sheet) {
        Map<String, byte[]> imageMap = new HashMap<>();

        try {
            if (workbook instanceof XSSFWorkbook) {
                XSSFWorkbook xssfWorkbook = (XSSFWorkbook) workbook;
                XSSFSheet xssfSheet = (XSSFSheet) sheet;

                // Get drawing patriarch (contains all shapes including images)
                XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
                if (drawing != null) {
                    List<XSSFShape> shapes = drawing.getShapes();
                    for (XSSFShape shape : shapes) {
                        if (shape instanceof XSSFPicture) {
                            XSSFPicture picture = (XSSFPicture) shape;
                            XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getAnchor();

                            // Get row index where image is located
                            int row1 = anchor.getRow1();

                            // Get image data
                            XSSFPictureData pictureData = picture.getPictureData();
                            byte[] imageData = pictureData.getData();

                            // Store with key: sheetName_rowIndex
                            String key = sheet.getSheetName() + "_" + row1;
                            imageMap.put(key, imageData);

                            log.debug("Found image at row {}, size: {} bytes, type: {}", row1, imageData.length, pictureData.getMimeType());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract images from Excel: {}", e.getMessage());
        }

        return imageMap;
    }

    /**
     * Detect image content type from byte array signature
     */
    private String detectImageContentType(byte[] imageData) {
        if (imageData == null || imageData.length < 4) {
            return "image/jpeg"; // Default
        }

        // Check PNG signature: 89 50 4E 47
        if (imageData[0] == (byte) 0x89 && imageData[1] == (byte) 0x50 && imageData[2] == (byte) 0x4E && imageData[3] == (byte) 0x47) {
            return "image/png";
        }

        // Check JPEG signature: FF D8 FF
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8 && imageData[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        // Check GIF signature: 47 49 46 38
        if (imageData[0] == (byte) 0x47 && imageData[1] == (byte) 0x49 && imageData[2] == (byte) 0x46 && imageData[3] == (byte) 0x38) {
            return "image/gif";
        }

        // Check BMP signature: 42 4D
        if (imageData[0] == (byte) 0x42 && imageData[1] == (byte) 0x4D) {
            return "image/bmp";
        }

        // Check WebP signature: 52 49 46 46 ... 57 45 42 50
        if (
            imageData.length >= 12 &&
            imageData[0] == (byte) 0x52 &&
            imageData[1] == (byte) 0x49 &&
            imageData[2] == (byte) 0x46 &&
            imageData[3] == (byte) 0x46 &&
            imageData[8] == (byte) 0x57 &&
            imageData[9] == (byte) 0x45 &&
            imageData[10] == (byte) 0x42 &&
            imageData[11] == (byte) 0x50
        ) {
            return "image/webp";
        }

        return "image/jpeg"; // Default fallback
    }
}
