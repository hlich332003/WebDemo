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

        if (userRepository.findOneByEmailIgnoreCase("admin@localhost").isEmpty()) {
            User adminUser = new User();
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setEmail("admin@localhost");
            adminUser.setPhone("0123456789");
            adminUser.setActivated(true);
            adminUser.setLangKey("en");
            authorityRepository.findById(AuthoritiesConstants.ADMIN).ifPresent(adminUser::setAuthority);
            userRepository.save(adminUser);
            log.info("Created default admin user.");
        }

        if (userRepository.findOneByEmailIgnoreCase("user@localhost").isEmpty()) {
            User regularUser = new User();
            regularUser.setPassword(passwordEncoder.encode("user"));
            regularUser.setFirstName("Regular");
            regularUser.setLastName("User");
            regularUser.setEmail("user@localhost");
            regularUser.setPhone("0987654321");
            regularUser.setActivated(true);
            regularUser.setLangKey("en");
            authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(regularUser::setAuthority);
            userRepository.save(regularUser);
            log.info("Created default regular user.");
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
