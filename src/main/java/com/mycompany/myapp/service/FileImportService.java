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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.text.Normalizer;

@Service
@Transactional
public class FileImportService {

    private final Logger log = LoggerFactory.getLogger(FileImportService.class);

    // Precompile commonly used regex patterns to avoid repeated compilation and to make pattern errors obvious
    private static final Pattern HYPERLINK_PATTERN;
    static {
        Pattern tmp;
        try {
            // Match HYPERLINK("url","display") or HYPERLINK('url','display')
            tmp = Pattern.compile("(?i)HYPERLINK\\s*\\(\\s*(['\"])\\s*(.*?)\\s*\\1");
        } catch (Exception e) {
            // fallback safe pattern: capture contents inside parentheses
            tmp = Pattern.compile("(?i)HYPERLINK\\s*\\((.*)\\)");
            LoggerFactory.getLogger(FileImportService.class).warn("Failed to compile advanced HYPERLINK pattern, using fallback: {}", e.getMessage());
        }
        HYPERLINK_PATTERN = tmp;
    }

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
        String originalFilename = file == null ? null : file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
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
            if (sheet == null) {
                throw new BadRequestAlertException("File Excel không có sheet nào", "fileImport", "nosheet");
            }

            // Đọc header row (dòng 0) và tạo map tên cột -> index
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestAlertException("File Excel thiếu hàng header (dòng 1)", "fileImport", "noheader");
            }

            Map<String, Integer> headerMap = new HashMap<>();
            short lastCell = headerRow.getLastCellNum();
            for (int c = 0; c < lastCell; c++) {
                Cell hcell = headerRow.getCell(c);
                String raw = getCellValue(hcell);
                if (raw == null) continue;
                // normalize header keys to a canonical ascii lowercase form used throughout the method
                String key = normalizeHeaderKey(raw);
                if (!key.isEmpty()) {
                    headerMap.put(key, c);
                }
            }

            // Các key thường dùng (normalize them the same way as headers)
            Set<String> nameKeys = Set.of(normalizeHeaderKey("name"), normalizeHeaderKey("productname"), normalizeHeaderKey("tensp"), normalizeHeaderKey("ten"));
            Set<String> descKeys = Set.of(normalizeHeaderKey("description"), normalizeHeaderKey("mota"), normalizeHeaderKey("desc"));
            Set<String> priceKeys = Set.of(normalizeHeaderKey("price"), normalizeHeaderKey("gia"), normalizeHeaderKey("cost"));
            Set<String> qtyKeys = Set.of(normalizeHeaderKey("quantity"), normalizeHeaderKey("qty"), normalizeHeaderKey("soluong"), normalizeHeaderKey("stock"));
            Set<String> imageKeys = Set.of(normalizeHeaderKey("imageurl"), normalizeHeaderKey("image"), normalizeHeaderKey("imageurladdress"), normalizeHeaderKey("image_url"), normalizeHeaderKey("anh"));
            Set<String> categoryKeys = Set.of(normalizeHeaderKey("category"), normalizeHeaderKey("categoryname"), normalizeHeaderKey("danhmuc"));
            // note: we intentionally ignore any "id" column from Excel; DB will generate IDs

            List<Product> productsToSave = new ArrayList<>();

            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row currentRow = sheet.getRow(r);
                if (currentRow == null) continue; // skip blank rows

                Product product = new Product();

                try {
                    // --- ID (nếu có) ---
                    // NOTE: IDs from the Excel file are ignored on import. The database will generate IDs automatically.
                    // If the sheet includes an "id" column it will be read but not applied to the entity to avoid accidental
                    // attempts to overwrite DB-generated identifiers.

                    // --- Name (bắt buộc) ---
                    String name = null;
                    for (String k : nameKeys) {
                        if (headerMap.containsKey(k)) {
                            name = getCellValue(currentRow.getCell(headerMap.get(k)));
                            break;
                        }
                    }
                    if (name == null || name.trim().isEmpty()) {
                        throw new IllegalArgumentException("Tên sản phẩm (cột 'Name') là bắt buộc");
                    }
                    product.setName(name.trim());

                    // --- Description (tùy chọn) ---
                    String description = null;
                    for (String k : descKeys) {
                        if (headerMap.containsKey(k)) {
                            description = getCellValue(currentRow.getCell(headerMap.get(k)));
                            break;
                        }
                    }
                    product.setDescription((description == null || description.trim().isEmpty()) ? "Chưa có mô tả" : description.trim());

                    // --- Price (bắt buộc) ---
                    Double price = null;
                    for (String k : priceKeys) {
                        if (headerMap.containsKey(k)) {
                            Cell priceCell = currentRow.getCell(headerMap.get(k));
                            if (priceCell != null && priceCell.getCellType() == CellType.NUMERIC) {
                                price = priceCell.getNumericCellValue();
                            } else {
                                String pStr = getCellValue(priceCell);
                                if (pStr != null && !pStr.isEmpty()) {
                                    // try robust parsing that accepts commas, currency symbols, etc.
                                    try {
                                        price = parseFlexibleDouble(pStr);
                                    } catch (Exception ex) {
                                        // fallback
                                        try {
                                            price = Double.parseDouble(pStr);
                                        } catch (Exception ex2) {
                                            // will be handled below
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                    if (price == null) {
                        throw new IllegalArgumentException("Giá sản phẩm (cột 'Price') là bắt buộc và phải là số");
                    }
                    product.setPrice(price);

                    // --- Quantity (tùy chọn, default 0) ---
                    Integer quantity = null;
                    for (String k : qtyKeys) {
                        if (headerMap.containsKey(k)) {
                            Cell qCell = currentRow.getCell(headerMap.get(k));
                            if (qCell != null && qCell.getCellType() == CellType.NUMERIC) {
                                quantity = (int) qCell.getNumericCellValue();
                            } else {
                                String qStr = getCellValue(qCell);
                                if (qStr != null && !qStr.isEmpty()) {
                                    try {
                                        quantity = Integer.parseInt(qStr);
                                    } catch (NumberFormatException ex) {
                                        // ignore
                                    }
                                }
                            }
                            break;
                        }
                    }
                    product.setQuantity(quantity != null ? quantity : 0);

                    // --- Image URL (tùy chọn) ---
                    String imageUrl = null;
                    Integer imageCol = null;
                    for (String k : imageKeys) {
                        if (headerMap.containsKey(k)) {
                            imageCol = headerMap.get(k);
                            imageUrl = getCellValue(currentRow.getCell(imageCol));
                            break;
                        }
                    }
                     // Nếu không có cell text, thử lấy ảnh nhúng trong sheet tại ô tương ứng
                     if (imageUrl == null || imageUrl.trim().isEmpty()) {
                         // nếu có imageCol, thử lấy ảnh nhúng tại cột đó
                         if (imageCol != null) {
                             try {
                                 String picData = getImageFromSheet(sheet, currentRow.getRowNum(), imageCol);
                                 if (picData != null) imageUrl = picData;
                             } catch (Exception ex) {
                                 log.debug("Không lấy được ảnh nhúng tại dòng {} col {}: {}", currentRow.getRowNum(), imageCol, ex.getMessage());
                             }
                         }
                         // nếu vẫn null, thử quét một vài cột (0..10) để tìm ảnh nhúng cùng hàng
                         if (imageUrl == null || imageUrl.trim().isEmpty()) {
                             for (int c = 0; c <= Math.min(10, lastCell); c++) {
                                 try {
                                     String picData = getImageFromSheet(sheet, currentRow.getRowNum(), c);
                                     if (picData != null) {
                                         imageUrl = picData;
                                         break;
                                     }
                                 } catch (Exception ex) {
                                     // ignore
                                 }
                             }
                         }
                     }

                     product.setImageUrl((imageUrl == null || imageUrl.trim().isEmpty()) ? "https://via.placeholder.com/300x300?text=No+Image" : imageUrl.trim());

                    // --- Category (tùy chọn) ---
                    String categoryInput = null;
                    for (String k : categoryKeys) {
                        if (headerMap.containsKey(k)) {
                            categoryInput = getCellValue(currentRow.getCell(headerMap.get(k)));
                            break;
                        }
                    }
                    // Make an effectively-final copy so it can be referenced from lambdas below
                    String categoryKey = categoryInput == null ? null : categoryInput.trim();
                    Category category;
                    if (categoryKey == null || categoryKey.isEmpty()) {
                        // replace inline lambda with method reference to avoid capturing local variables
                        category = categoryRepository
                            .findBySlug("chua-phan-loai")
                            .orElseGet(this::createDefaultCategory);
                    } else {
                        // Avoid capturing non-final local variables in lambdas by doing explicit lookups
                        Optional<Category> catOpt = categoryRepository.findByName(categoryKey);
                        if (catOpt.isEmpty()) {
                            catOpt = categoryRepository.findBySlug(categoryKey);
                        }
                        if (catOpt.isEmpty()) {
                            // auto-create category instead of failing
                            Category newCat = new Category();
                            newCat.setName(categoryKey);
                            String generatedSlug = slugify(categoryKey);
                            newCat.setSlug(generatedSlug == null ? UUID.randomUUID().toString() : generatedSlug);
                            category = categoryRepository.save(newCat);
                        } else {
                            category = catOpt.get();
                        }
                    }
                     product.setCategory(category);

                } catch (BadRequestAlertException e) {
                    throw e;
                } catch (Exception e) {
                    // include row information in the message to make it clearer on the frontend what failed
                    throw new BadRequestAlertException(
                        "Lỗi tại dòng " + (r + 1) + ": " + e.getMessage(),
                        "fileImport",
                        "dataReadError"
                    );
                }

                // Validation: don't use ID column from Excel — always create new product unless you implement an update flow.
                // Check duplicate by name only (case-insensitive).
                // Robust duplicate detection: compare normalized names (remove diacritics, punctuation and case)
                String candidateName = product.getName() == null ? "" : product.getName().trim();
                String normalizedCandidate = normalizeHeaderKey(candidateName);
                Optional<Product> duplicateProduct = Optional.empty();
                // First try quick DB lookup by case-insensitive match
                try {
                    duplicateProduct = productRepository.findFirstByNameIgnoreCase(candidateName);
                } catch (Throwable t) {
                    // ignore and fall through to slower check
                }
                // If not found, fall back to normalized comparison across existing products (covers diacritics/whitespace differences)
                if (duplicateProduct.isEmpty()) {
                    try {
                        for (Product existing : productRepository.findAll()) {
                            String existingNorm = normalizeHeaderKey(existing.getName());
                            if (existingNorm.equals(normalizedCandidate) && existing.getId() != null) {
                                duplicateProduct = Optional.of(existing);
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        // If anything goes wrong with the slower scan, treat as no duplicate and continue (import may still fail on DB constraints later)
                        log.debug("Could not perform normalized duplicate scan: {}", t.getMessage());
                    }
                }

                if (duplicateProduct.isPresent()) {
                    throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' đã tồn tại (ID=" + duplicateProduct.get().getId() + "). Nếu muốn cập nhật, hiện import không hỗ trợ cập nhật theo tên; hãy xóa bản ghi trùng hoặc implement update flow."
                    );
                }

                productsToSave.add(product);
            }

            productRepository.saveAll(productsToSave);
        }
    }

    public void importUsers(MultipartFile file) throws Exception {
        String originalFilename = file == null ? null : file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
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
                // Do NOT set user.id from Excel - IDs are DB-generated. Drop any ID column silently.

                try {
                    // Previous column mapping used 'login' at column 1; switch to use email as identifier (column 1 assumed to be email now)
                    String emailOrLogin = getCellValue(currentRow.getCell(1));
                    String password = getCellValue(currentRow.getCell(2));
                    user.setPassword(passwordEncoder.encode(password != null ? password : "password"));
                    user.setFirstName(getCellValue(currentRow.getCell(3)));
                    user.setLastName(getCellValue(currentRow.getCell(4)));
                    user.setEmail(emailOrLogin != null ? emailOrLogin.toLowerCase() : null);
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

                // Only check uniqueness by email (do not rely on Excel-provided IDs)
                if (userRepository.findOneByEmailIgnoreCase(user.getEmail()).isPresent()) {
                    throw new BadRequestAlertException("Email đã tồn tại: " + user.getEmail(), "fileImport", "emailExists");
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

        // If cell has a hyperlink object (cell-level), prefer the hyperlink address (useful when Excel stores URL as a link)
        if (cell.getHyperlink() != null && cell.getHyperlink().getAddress() != null) {
            return cell.getHyperlink().getAddress();
        }

        CellType type = cell.getCellType();

        // Handle formulas by checking for HYPERLINK(...) in the formula first, then evaluate
        if (type == CellType.FORMULA) {
            try {
                // try to extract URL from HYPERLINK("url","display") formula
                String formula = cell.getCellFormula();
                if (formula != null) {
                    Matcher m = HYPERLINK_PATTERN.matcher(formula);
                    if (m.find()) {
                        String url = m.group(2);
                        if (url != null && !url.isEmpty()) {
                            return url;
                        }
                    }
                }

                // fallback to hyperlink object if available on formula cell
                if (cell.getHyperlink() != null && cell.getHyperlink().getAddress() != null) {
                    return cell.getHyperlink().getAddress();
                }

                // fallback: evaluate the formula and return evaluated string/number/boolean
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                CellValue evaluated = evaluator.evaluate(cell);
                if (evaluated == null) {
                    return null;
                }
                switch (evaluated.getCellType()) {
                    case STRING:
                        return evaluated.getStringValue();
                    case NUMERIC:
                        double dv = evaluated.getNumberValue();
                        if (dv == Math.floor(dv)) {
                            return String.valueOf((long) dv);
                        }
                        return String.valueOf(dv);
                    case BOOLEAN:
                        return String.valueOf(evaluated.getBooleanValue());
                    default:
                        return null;
                }
            } catch (Exception e) {
                log.debug("Không thể evaluate formula cell: {}", e.getMessage());
                return null;
            }
        }

        // Non-formula handling
        switch (type) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double dv = cell.getNumericCellValue();
                if (dv == Math.floor(dv)) {
                    return String.valueOf((long) dv);
                }
                return String.valueOf(dv);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    private Double parseFlexibleDouble(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        // remove common currency symbols and spaces
        // accept regular whitespace (\s). Avoid raw Unicode escape sequences (e.g. U+00A0) in source
        s = s.replaceAll("(?i)[\\s]*(vnd|vnđ|đ|d|usd|€|eur|\\$)", "");
        s = s.replaceAll("\\s+", "");

        // If contains both '.' and ',', decide decimal separator by last occurrence
        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) {
                // comma likely decimal separator, remove dots (thousands)
                s = s.replaceAll("\\.", "");
                s = s.replace(',', '.');
            } else {
                // dot likely decimal separator, remove commas
                s = s.replaceAll(",", "");
            }
        } else if (lastComma >= 0) {
            // only comma present -> assume comma is decimal if there are <=2 decimals after it, else maybe thousands
            if (s.length() - lastComma - 1 <= 2) {
                s = s.replace(',', '.');
            } else {
                s = s.replaceAll(",", "");
            }
        }

        // Remove any non numeric (allow leading - and decimal dot)
        s = s.replaceAll("[^0-9.\\-]", "");
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            log.debug("parseFlexibleDouble failed for '{}': {}", raw, ex.getMessage());
            return null;
        }
    }

    // helper to create slug from category name
    private String slugify(String input) {
        if (input == null) return null;
        String nowhitespace = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = nowhitespace.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? null : slug;
    }

    /**
     * Thử lấy ảnh nhúng (embedded picture) gắn vào ô (row, col) nếu có.
     * Trả về data URI (data:<mime>;base64,<data>) nếu tìm thấy ảnh, hoặc null nếu không.
     */
    private String getImageFromSheet(Sheet sheet, int rowIndex, int colIndex) {
        if (!(sheet instanceof XSSFSheet xssfSheet)) {
            return null;
        }
        XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
        if (drawing == null) {
            return null;
        }
        List<XSSFShape> shapes = drawing.getShapes();
        for (XSSFShape shape : shapes) {
            if (shape instanceof XSSFPicture pic) {
                ClientAnchor anchor = pic.getClientAnchor();
                if (anchor instanceof XSSFClientAnchor xa) {
                    int r = xa.getRow1();
                    int c = xa.getCol1();
                    if (r == rowIndex && c == colIndex) {
                        XSSFPictureData picData = pic.getPictureData();
                        if (picData == null) {
                            continue;
                        }
                        byte[] data = picData.getData();
                        String mime = picData.getMimeType();
                        if (mime == null) {
                            // guess by format
                            switch (picData.getPictureType()) {
                                case Workbook.PICTURE_TYPE_PNG -> mime = "image/png";
                                case Workbook.PICTURE_TYPE_JPEG -> mime = "image/jpeg";
                                default -> mime = "application/octet-stream";
                            }
                        }
                        String base64 = Base64.getEncoder().encodeToString(data);
                        return "data:" + mime + ";base64," + base64;
                    }
                }
            }
        }
        return null;
    }


    // Thêm phương thức tiện ích để tạo category mặc định, tránh lambda capture
    private Category createDefaultCategory() {
        Category defaultCategory = new Category();
        defaultCategory.setName("Chưa phân loại");
        defaultCategory.setSlug("chua-phan-loai");
        return categoryRepository.save(defaultCategory);
    }

    // normalize header keys to a canonical ascii lowercase form used throughout the method
    private String normalizeHeaderKey(String raw) {
        if (raw == null) {
            return "";
        }
        // 1. Normalize unicode (remove diacritics)
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        // 2. Convert to ASCII-friendly lower-case and trim
        normalized = normalized.trim().toLowerCase();
        // 3. Replace all non-alphanumeric characters with a single hyphen
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        // 4. Trim leading/trailing hyphens
        normalized = normalized.replaceAll("(^-|-$)", "");
        return normalized;
    }
}

