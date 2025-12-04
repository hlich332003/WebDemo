//package com.mycompany.myapp.config;
//
//import com.mycompany.myapp.domain.Authority;
//import com.mycompany.myapp.domain.Category;
//import com.mycompany.myapp.domain.Product;
//import com.mycompany.myapp.domain.User;
//import com.mycompany.myapp.repository.AuthorityRepository;
//import com.mycompany.myapp.repository.CategoryRepository;
//import com.mycompany.myapp.repository.ProductRepository;
//import com.mycompany.myapp.repository.UserRepository;
//import com.mycompany.myapp.security.AuthoritiesConstants;
//import java.util.HashSet;
//import java.util.Optional;
//import java.util.Set;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//@Profile("dev") // Chỉ chạy trong profile dev
//public class DatabaseSeeder implements CommandLineRunner {
//
//    private final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
//
//    private final UserRepository userRepository;
//    private final AuthorityRepository authorityRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final CategoryRepository categoryRepository;
//    private final ProductRepository productRepository;
//    private final DatabaseSeedService databaseSeedService;
//
//    public DatabaseSeeder(
//        UserRepository userRepository,
//        AuthorityRepository authorityRepository,
//        PasswordEncoder passwordEncoder,
//        CategoryRepository categoryRepository,
//        ProductRepository productRepository,
//        DatabaseSeedService databaseSeedService
//    ) {
//        this.userRepository = userRepository;
//        this.authorityRepository = authorityRepository;
//        this.passwordEncoder = passwordEncoder;
//        this.categoryRepository = categoryRepository;
//        this.productRepository = productRepository;
//        this.databaseSeedService = databaseSeedService;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        // Delegate to transactional seed service to avoid lazy initialization / session issues
//        databaseSeedService.seedInitialData();
//    }
//}
