package com.mycompany.myapp.config;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.CategoryRepository;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseSeedService {

    private final Logger log = LoggerFactory.getLogger(DatabaseSeedService.class);

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DatabaseSeedService(
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        PasswordEncoder passwordEncoder,
        CategoryRepository categoryRepository,
        ProductRepository productRepository
    ) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void seedInitialData() {
        log.info("Seeding initial data...");

        // Create default authorities if they don't exist
        if (authorityRepository.findById(AuthoritiesConstants.ADMIN).isEmpty()) {
            Authority adminAuthority = new Authority();
            adminAuthority.setName(AuthoritiesConstants.ADMIN);
            authorityRepository.save(adminAuthority);
        }
        if (authorityRepository.findById(AuthoritiesConstants.USER).isEmpty()) {
            Authority userAuthority = new Authority();
            userAuthority.setName(AuthoritiesConstants.USER);
            authorityRepository.save(userAuthority);
        }

        // Admin identity we want to ensure exists (do not delete or overwrite existing data)
        final String adminEmail = "admin@localhost";
        final String adminPhone = "0000000001";

        Optional<User> existingAdminOpt = userRepository.findOneByEmailIgnoreCase(adminEmail);
        if (existingAdminOpt.isEmpty()) {
            existingAdminOpt = userRepository.findOneByPhone(adminPhone);
        }

        if (existingAdminOpt.isEmpty()) {
            User adminUser = new User();
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setEmail(adminEmail);
            adminUser.setPhone(adminPhone);
            adminUser.setActivated(true);
            adminUser.setLangKey("en");
            authorityRepository.findById(AuthoritiesConstants.ADMIN).ifPresent(adminUser::setAuthority);
            userRepository.save(adminUser);
            log.info("Created default admin user: {} / {}", adminEmail, adminPhone);
        } else {
            User adminUser = existingAdminOpt.get();
            boolean changed = false;

            if (adminUser.getFirstName() == null || adminUser.getFirstName().isEmpty()) {
                adminUser.setFirstName("Admin");
                changed = true;
            }
            if (adminUser.getLastName() == null || adminUser.getLastName().isEmpty()) {
                adminUser.setLastName("User");
                changed = true;
            }

            if (adminUser.getPhone() == null || adminUser.getPhone().isEmpty()) {
                adminUser.setPhone(adminPhone);
                changed = true;
            }

            if (adminUser.getEmail() == null || adminUser.getEmail().isEmpty()) {
                adminUser.setEmail(adminEmail);
                changed = true;
            }

            if (!adminUser.isActivated()) {
                adminUser.setActivated(true);
                changed = true;
            }
            if (adminUser.getLangKey() == null || adminUser.getLangKey().isEmpty()) {
                adminUser.setLangKey("en");
                changed = true;
            }

            if (adminUser.getPassword() == null || adminUser.getPassword().isEmpty()) {
                adminUser.setPassword(passwordEncoder.encode("admin"));
                changed = true;
            }

            if (adminUser.getAuthority() == null || !adminUser.getAuthority().getName().equals(AuthoritiesConstants.ADMIN)) {
                authorityRepository.findById(AuthoritiesConstants.ADMIN).ifPresent(adminUser::setAuthority);
                changed = true;
            }

            if (changed) {
                userRepository.save(adminUser);
                log.info("Updated existing admin user (added missing fields/authority): {}", adminUser.getEmail());
            } else {
                log.info("Admin user already exists and is up-to-date: {}", adminUser.getEmail());
            }
        }

        Optional<User> existingUserOpt = userRepository.findOneByEmailIgnoreCase("user@localhost");
        if (existingUserOpt.isEmpty()) {
            User regularUser = new User();
            regularUser.setPassword(passwordEncoder.encode("user"));
            regularUser.setFirstName("Regular");
            regularUser.setLastName("User");
            regularUser.setEmail("user@localhost");
            regularUser.setActivated(true);
            regularUser.setLangKey("en");
            authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(regularUser::setAuthority);
            userRepository.save(regularUser);
            log.info("Created default regular user.");
        } else {
            User regularUser = existingUserOpt.get();
            if (regularUser.getAuthority() == null || !regularUser.getAuthority().getName().equals(AuthoritiesConstants.USER)) {
                authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(regularUser::setAuthority);
                userRepository.save(regularUser);
                log.info("Added USER authority to existing user: {}", regularUser.getEmail());
            }
        }

        if (categoryRepository.findByName("Electronics").isEmpty()) {
            Category electronics = new Category();
            electronics.setName("Electronics");
            electronics.setSlug("electronics");
            categoryRepository.save(electronics);
            log.info("Created category: Electronics");
        }

        if (categoryRepository.findByName("Books").isEmpty()) {
            Category books = new Category();
            books.setName("Books");
            books.setSlug("books");
            categoryRepository.save(books);
            log.info("Created category: Books");
        }

        Optional<Category> electronicsCategory = categoryRepository.findByName("Electronics");
        if (electronicsCategory.isPresent() && productRepository.findFirstByName("Laptop").isEmpty()) {
            Product laptop = new Product();
            laptop.setName("Laptop");
            laptop.setDescription("Powerful laptop for work and gaming.");
            laptop.setPrice(1200.00);
            laptop.setQuantity(50);
            laptop.setImageUrl("https://example.com/laptop.jpg");
            laptop.setCategory(electronicsCategory.get());
            productRepository.save(laptop);
            log.info("Created product: Laptop");
        }

        Optional<Category> booksCategory = categoryRepository.findByName("Books");
        if (booksCategory.isPresent() && productRepository.findFirstByName("The Great Gatsby").isEmpty()) {
            Product book = new Product();
            book.setName("The Great Gatsby");
            book.setDescription("A classic novel by F. Scott Fitzgerald.");
            book.setPrice(15.99);
            book.setQuantity(100);
            book.setImageUrl("https://example.com/gatsby.jpg");
            book.setCategory(booksCategory.get());
            productRepository.save(book);
            log.info("Created product: The Great Gatsby");
        }

        log.info("Initial data seeding complete.");
    }
}
