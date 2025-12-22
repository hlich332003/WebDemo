package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.CategoryRepository;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    public FileImportService(
        ProductRepository productRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthorityRepository authorityRepository,
        CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.categoryRepository = categoryRepository;
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

            // --- Optimization: Pre-load categories ---
            Map<String, Category> categoryCacheByName = new HashMap<>();
            Map<String, Category> categoryCacheBySlug = new HashMap<>();
            categoryRepository.findAll().forEach(cat -> {
                if (cat.getName() != null) {
                    categoryCacheByName.put(cat.getName().trim(), cat);
                }
                if (cat.getSlug() != null) {
                    categoryCacheBySlug.put(cat.getSlug().trim(), cat);
                }
            });

            Category defaultCategory = categoryCacheBySlug.get("chua-phan-loai");
            if (defaultCategory == null) {
                defaultCategory = categoryRepository.findBySlug("chua-phan-loai").orElseGet(() -> {
                    Category newCat = new Category();
                    newCat.setName("Chưa phân loại");
                    newCat.setSlug("chua-phan-loai");
                    return categoryRepository.save(newCat);
                });
                categoryCacheBySlug.put("chua-phan-loai", defaultCategory);
                if(defaultCategory.getName() != null) {
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
                    product.setImageUrl(
                        (imageUrl == null || imageUrl.trim().isEmpty())
                            ? "https://via.placeholder.com/300x300?text=No+Image"
                            : imageUrl.trim()
                    );

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
                                "Không tìm thấy danh mục '" +
                                categoryInput +
                                "'. Vui lòng kiểm tra lại tên danh mục hoặc tạo danh mục mới."
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
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy quyền USER mặc định", "fileImport", "userAuthorityNotFound"));

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                User user = new User();
                Cell idCell = currentRow.getCell(0);
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                    user.setId((long) idCell.getNumericCellValue());
                }

                try {
                    String password = getCellValue(currentRow.getCell(2));
                    if (password == null || password.isBlank()) {
                        throw new IllegalArgumentException("Mật khẩu không được để trống");
                    }
                    user.setPassword(passwordEncoder.encode(password));

                    user.setFirstName(getCellValue(currentRow.getCell(3)));
                    user.setLastName(getCellValue(currentRow.getCell(4)));

                    String email = getCellValue(currentRow.getCell(5));
                    if (email == null || email.isBlank()) {
                        throw new IllegalArgumentException("Email không được để trống");
                    }
                    user.setEmail(email.toLowerCase());

                    user.setPhone(getCellValue(currentRow.getCell(6)));
                    user.setActivated(true);
                    user.setAuthority(userAuthority);
                } catch (Exception e) {
                    throw new BadRequestAlertException(
                        "Lỗi đọc dữ liệu người dùng tại dòng " + (rowNumber + 1) + ": " + e.getMessage(),
                        "fileImport",
                        "dataReadError"
                    );
                }

                if (user.getId() != null) {
                    userRepository
                        .findById(user.getId())
                        .ifPresent(existingUser -> {
                            user.setCreatedBy(existingUser.getCreatedBy());
                            user.setCreatedDate(existingUser.getCreatedDate());
                        });
                } else {
                    // Sync with UserService: remove non-activated user with same email or phone
                    final String userEmail = user.getEmail();
                    if (userEmail != null && !userEmail.isBlank()) {
                        userRepository
                            .findOneByEmailIgnoreCase(userEmail)
                            .ifPresent(existingUser -> {
                                if (existingUser.isActivated()) {
                                    throw new BadRequestAlertException(
                                        "Email '" + userEmail + "' đã tồn tại và đã được kích hoạt.",
                                        "fileImport",
                                        "emailExists"
                                    );
                                }
                                userRepository.delete(existingUser);
                                userRepository.flush();
                            });
                    }

                    final String userPhone = user.getPhone();
                    if (userPhone != null && !userPhone.isBlank()) {
                        userRepository
                            .findOneByPhone(userPhone)
                            .ifPresent(existingUser -> {
                                if (existingUser.isActivated()) {
                                    throw new BadRequestAlertException(
                                        "Số điện thoại '" + userPhone + "' đã tồn tại và đã được kích hoạt.",
                                        "fileImport",
                                        "phoneExists"
                                    );
                                }
                                userRepository.delete(existingUser);
                                userRepository.flush();
                            });
                    }
                }
                usersToSave.add(user);
                rowNumber++;
            }
            userRepository.saveAll(usersToSave);
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
}
