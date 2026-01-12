package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UserImportService {

    private final Logger log = LoggerFactory.getLogger(UserImportService.class);

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public UserImportService(UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserImportResult importUsersFromExcel(MultipartFile file) throws IOException {
        UserImportResult result = new UserImportResult();

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Bỏ qua header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    processUserRow(row, result);
                } catch (Exception e) {
                    result.addError(row.getRowNum() + 1, "Lỗi xử lý dòng: " + e.getMessage());
                    log.error("Lỗi xử lý dòng {}: {}", row.getRowNum() + 1, e.getMessage());
                }
            }
        }

        return result;
    }

    private void processUserRow(Row row, UserImportResult result) {
        // Đọc dữ liệu từ Excel - 4 cột: phone | name | product_purchased | date
        String phoneNumber = getCellValueAsString(row.getCell(0));
        String name = getCellValueAsString(row.getCell(1));
        String productPurchased = getCellValueAsString(row.getCell(2));
        Cell dateCell = row.getCell(3);

        // Validate số điện thoại (bắt buộc)
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            result.addError(row.getRowNum() + 1, "Cột phone (SĐT) không được để trống");
            return;
        }

        phoneNumber = phoneNumber.trim();

        // Parse date (không bắt buộc)
        Instant purchaseDate = null;
        if (dateCell != null) {
            try {
                if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                    purchaseDate = dateCell.getDateCellValue().toInstant();
                } else {
                    String dateStr = getCellValueAsString(dateCell);
                    if (dateStr != null && !dateStr.trim().isEmpty()) {
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        java.time.LocalDate localDate = java.time.LocalDate.parse(dateStr.trim(), formatter);
                        purchaseDate = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
                    }
                }
            } catch (Exception e) {
                result.addWarning(row.getRowNum() + 1, "Ngày tháng không hợp lệ, bỏ qua: " + e.getMessage());
            }
        }

        // Tìm user theo số điện thoại
        Optional<User> existingUserOpt = userRepository.findOneByPhone(phoneNumber);

        User user;
        boolean isUpdate = false;

        if (existingUserOpt.isPresent()) {
            // Cập nhật thông tin user hiện có
            user = existingUserOpt.get();
            isUpdate = true;
        } else {
            // Tạo user mới
            user = new User();
            user.setPassword(passwordEncoder.encode("123456")); // Mật khẩu mặc định
            user.setActivated(true);
            user.setLangKey("vi");
            user.setCreatedBy("system");
            user.setCreatedDate(Instant.now());

            // Tạo email mặc định từ số điện thoại
            String defaultEmail = phoneNumber + "@localhost";

            // Kiểm tra email đã tồn tại chưa, nếu có thêm timestamp
            Optional<User> existingEmail = userRepository.findOneByEmailIgnoreCase(defaultEmail);
            if (existingEmail.isPresent()) {
                defaultEmail = phoneNumber + "_" + System.currentTimeMillis() + "@localhost";
            }

            user.setEmail(defaultEmail);

            // Gán quyền USER mặc định
            authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(user::setAuthority);
        }

        // Cập nhật thông tin tên
        if (name != null && !name.trim().isEmpty()) {
            // Tách tên thành firstName và lastName
            String[] nameParts = name.trim().split("\\s+", 2);
            if (nameParts.length >= 2) {
                user.setFirstName(nameParts[0]);
                user.setLastName(nameParts[1]);
            } else {
                user.setFirstName(nameParts[0]);
                user.setLastName("");
            }
        } else if (!isUpdate) {
            // Nếu tạo mới mà không có tên, dùng mặc định
            user.setFirstName("Khách hàng");
            user.setLastName(phoneNumber);
        }

        user.setPhone(phoneNumber);
        user.setLastModifiedBy("system");
        user.setLastModifiedDate(Instant.now());

        userRepository.save(user);

        // Log thông tin sản phẩm và ngày (để tích hợp với Customer sau)
        if (productPurchased != null && !productPurchased.trim().isEmpty()) {
            log.info("Sản phẩm đã mua: {} - Ngày: {}", productPurchased.trim(), purchaseDate);
        }

        if (isUpdate) {
            result.incrementUpdated();
            log.info("Cập nhật user: {} - {}", phoneNumber, name);
        } else {
            result.incrementCreated();
            log.info("Tạo mới user: {} - {} (Email: {})", phoneNumber, name, user.getEmail());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Chuyển số thành string (ví dụ: số điện thoại)
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    public static class UserImportResult {

        private int created = 0;
        private int updated = 0;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void incrementCreated() {
            created++;
        }

        public void incrementUpdated() {
            updated++;
        }

        public void addError(int rowNumber, String message) {
            errors.add("Dòng " + rowNumber + ": " + message);
        }

        public void addWarning(int rowNumber, String message) {
            warnings.add("Dòng " + rowNumber + ": " + message);
        }

        public int getCreated() {
            return created;
        }

        public int getUpdated() {
            return updated;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public int getTotalProcessed() {
            return created + updated;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
