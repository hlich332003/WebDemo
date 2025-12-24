//package com.mycompany.myapp.config;
//
//import com.mycompany.myapp.domain.Authority;
//import com.mycompany.myapp.domain.User;
//import com.mycompany.myapp.repository.AuthorityRepository;
//import com.mycompany.myapp.repository.UserRepository;
//import com.mycompany.myapp.security.AuthoritiesConstants;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//@Component
//@Profile("dev") // Chỉ chạy trong profile dev
//@Transactional
//public class DatabaseSeeder implements CommandLineRunner {
//
//    private final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
//
//    private final UserRepository userRepository;
//    private final AuthorityRepository authorityRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    public DatabaseSeeder(
//        UserRepository userRepository,
//        AuthorityRepository authorityRepository,
//        PasswordEncoder passwordEncoder
//    ) {
//        this.userRepository = userRepository;
//        this.authorityRepository = authorityRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        log.info("Bắt đầu khởi tạo dữ liệu mẫu...");
//
//        // 1. Tạo các quyền (Authorities)
//        Authority adminAuthority = createAuthority(AuthoritiesConstants.ADMIN);
//        Authority userAuthority = createAuthority(AuthoritiesConstants.USER);
//
//        // 2. Tạo người dùng (Users)
//        createUser("admin@localhost", "admin", "Admin", "User", "0123456789", "en", adminAuthority);
//        createUser("user@localhost", "user", "Regular", "User", "0987654321", "en", userAuthority);
//
//        log.info("Hoàn tất khởi tạo dữ liệu người dùng và quyền.");
//    }
//
//    private Authority createAuthority(String authorityName) {
//        return authorityRepository.findById(authorityName).orElseGet(() -> {
//            Authority newAuthority = new Authority();
//            newAuthority.setName(authorityName);
//            authorityRepository.save(newAuthority);
//            log.info("Đã tạo quyền: {}", authorityName);
//            return newAuthority;
//        });
//    }
//
//    private void createUser(String email, String password, String firstName, String lastName, String phone, String langKey, Authority authority) {
//        if (userRepository.findOneByEmailIgnoreCase(email).isEmpty()) {
//            User newUser = new User();
//            newUser.setPassword(passwordEncoder.encode(password));
//            newUser.setFirstName(firstName);
//            newUser.setLastName(lastName);
//            newUser.setEmail(email);
//            newUser.setPhone(phone);
//            newUser.setActivated(true);
//            newUser.setLangKey(langKey);
//            newUser.setAuthority(authority);
//            userRepository.save(newUser);
//            log.info("Đã tạo người dùng: {}", email);
//        }
//    }
//}
