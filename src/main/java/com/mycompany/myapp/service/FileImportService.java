package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.CategoryRepository; // Import CategoryRepository
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.io.BufferedInputStream;
import java.io.InputStream;
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
    private final CategoryRepository categoryRepository; // Inject CategoryRepository

    public FileImportService(
        ProductRepository productRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthorityRepository authorityRepository,
        CategoryRepository categoryRepository // Thêm vào constructor
    ) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.categoryRepository = categoryRepository;
    }

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

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) { // Skip header row
                    rowNumber++;
                    continue;
                }

                Product product = new Product();
                Cell idCell = currentRow.getCell(0);
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                    product.setId((long) idCell.getNumericCellValue());
                }

                try {
                    product.setName(getCellValue(currentRow.getCell(1)));
                    product.setDescription(getCellValue(currentRow.getCell(2)));
                    product.setPrice(getNumericCellValue(currentRow.getCell(3)));
                    product.setQuantity((int) getNumericCellValue(currentRow.getCell(4)));
                    product.setImageUrl(getCellValue(currentRow.getCell(5)));
                    product.setIsFeatured(getBooleanCellValue(currentRow.getCell(6))); // Đọc isFeatured

                    String categoryName = getCellValue(currentRow.getCell(7)); // Đọc tên danh mục từ cột 7
                    if (categoryName != null && !categoryName.isEmpty()) {
                        categoryRepository.findByName(categoryName).ifPresent(product::setCategory);
                    } else {
                        throw new BadRequestAlertException(
                            "Danh mục không được để trống tại dòng " + (rowNumber + 1),
                            "fileImport",
                            "categoryRequired"
                        );
                    }
                } catch (Exception e) {
                    throw new BadRequestAlertException(
                        "Lỗi đọc dữ liệu sản phẩm tại dòng " + (rowNumber + 1) + ": " + e.getMessage(),
                        "fileImport",
                        "dataReadError"
                    );
                }

                if (product.getId() != null) {
                    productRepository
                        .findById(product.getId())
                        .ifPresent(existingProduct -> {
                            product.setCreatedBy(existingProduct.getCreatedBy());
                            product.setCreatedDate(existingProduct.getCreatedDate());
                        });
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

            Optional<Authority> userAuthority = authorityRepository.findById(AuthoritiesConstants.USER);
            if (userAuthority.isEmpty()) {
                throw new BadRequestAlertException("Không tìm thấy quyền USER mặc định", "fileImport", "userAuthorityNotFound");
            }
            Set<Authority> defaultAuthorities = Collections.singleton(userAuthority.get());

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) { // Skip header row
                    rowNumber++;
                    continue;
                }

                User user = new User();
                Cell idCell = currentRow.getCell(0);
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                    user.setId((long) idCell.getNumericCellValue());
                }

                try {
                    user.setLogin(getCellValue(currentRow.getCell(1)));
                    user.setPassword(passwordEncoder.encode(getCellValue(currentRow.getCell(2))));
                    user.setFirstName(getCellValue(currentRow.getCell(3)));
                    user.setLastName(getCellValue(currentRow.getCell(4)));
                    user.setEmail(getCellValue(currentRow.getCell(5)));
                    user.setPhone(getCellValue(currentRow.getCell(6)));
                    user.setActivated(true);
                    user.setAuthorities(defaultAuthorities);
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
                    if (userRepository.findOneByLogin(user.getLogin()).isPresent()) {
                        throw new BadRequestAlertException("Login đã tồn tại: " + user.getLogin(), "fileImport", "loginExists");
                    }
                    if (userRepository.findOneByEmailIgnoreCase(user.getEmail()).isPresent()) {
                        throw new BadRequestAlertException("Email đã tồn tại: " + user.getEmail(), "fileImport", "emailExists");
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

    private double getNumericCellValue(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            throw new IllegalArgumentException("Giá trị không phải là số.");
        }
        return cell.getNumericCellValue();
    }

    private Boolean getBooleanCellValue(Cell cell) {
        if (cell == null) {
            return false; // Mặc định là false nếu ô trống
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            return Boolean.parseBoolean(cell.getStringCellValue());
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue() == 1; // Coi 1 là true, 0 là false
        }
        return false;
    }
}
