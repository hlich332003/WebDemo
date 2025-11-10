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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev") // Chỉ chạy trong profile dev
public class DatabaseSeeder implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DatabaseSeeder(
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

    @Override
    public void run(String... args) throws Exception {
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

        // Create admin user if not exists
        if (userRepository.findOneByLogin("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setLogin("admin");
            adminUser.setPassword(passwordEncoder.encode("admin")); // Mã hóa mật khẩu
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setEmail("admin@localhost");
            adminUser.setActivated(true);
            adminUser.setLangKey("en");
            Set<Authority> authorities = new HashSet<>();
            authorityRepository.findById(AuthoritiesConstants.ADMIN).ifPresent(authorities::add);
            authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
            adminUser.setAuthorities(authorities);
            userRepository.save(adminUser);
            log.info("Created default admin user.");
        }

        // Create a regular user if not exists
        if (userRepository.findOneByLogin("user").isEmpty()) {
            User regularUser = new User();
            regularUser.setLogin("user");
            regularUser.setPassword(passwordEncoder.encode("user")); // Mã hóa mật khẩu
            regularUser.setFirstName("Regular");
            regularUser.setLastName("User");
            regularUser.setEmail("user@localhost");
            regularUser.setActivated(true);
            regularUser.setLangKey("en");
            Set<Authority> authorities = new HashSet<>();
            authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
            regularUser.setAuthorities(authorities);
            userRepository.save(regularUser);
            log.info("Created default regular user.");
        }

        // Create sample categories if not exists
        if (categoryRepository.findByName("Electronics").isEmpty()) {
            Category electronics = new Category();
            electronics.setName("Electronics");
            electronics.setSlug("electronics");
            electronics.setIsFeatured(true); // Đặt là nổi bật
            categoryRepository.save(electronics);
            log.info("Created category: Electronics");
        }

        if (categoryRepository.findByName("Books").isEmpty()) {
            Category books = new Category();
            books.setName("Books");
            books.setSlug("books");
            books.setIsFeatured(false); // Không nổi bật
            categoryRepository.save(books);
            log.info("Created category: Books");
        }

        // Create sample products if not exists
        Optional<Category> electronicsCategory = categoryRepository.findByName("Electronics");
        if (electronicsCategory.isPresent() && productRepository.findByName("Laptop").isEmpty()) {
            Product laptop = new Product();
            laptop.setName("Laptop");
            laptop.setDescription("Powerful laptop for work and gaming.");
            laptop.setPrice(1200.00);
            laptop.setQuantity(50);
            laptop.setImageUrl("https://example.com/laptop.jpg");
            laptop.setIsFeatured(true); // Đặt là nổi bật
            laptop.setCategory(electronicsCategory.get());
            productRepository.save(laptop);
            log.info("Created product: Laptop");
        }

        Optional<Category> booksCategory = categoryRepository.findByName("Books");
        if (booksCategory.isPresent() && productRepository.findByName("The Great Gatsby").isEmpty()) {
            Product book = new Product();
            book.setName("The Great Gatsby");
            book.setDescription("A classic novel by F. Scott Fitzgerald.");
            book.setPrice(15.99);
            book.setQuantity(100);
            book.setImageUrl("https://example.com/gatsby.jpg");
            book.setIsFeatured(false); // Không nổi bật
            book.setCategory(booksCategory.get());
            productRepository.save(book);
            log.info("Created product: The Great Gatsby");
        }

        log.info("Initial data seeding complete.");
    }
}
