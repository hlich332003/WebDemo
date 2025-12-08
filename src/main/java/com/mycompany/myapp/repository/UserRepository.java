package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    Optional<User> findOneByActivationKey(String activationKey);

    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneByPhone(String phone);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByPhone(String phone);

    // Method để tìm user bằng email hoặc phone (cho đăng nhập)
    @EntityGraph(attributePaths = "authorities")
    default Optional<User> findOneWithAuthoritiesByEmailOrPhone(String emailOrPhone) {
        // Kiểm tra xem có phải email không (chứa @)
        if (emailOrPhone.contains("@")) {
            return findOneWithAuthoritiesByEmailIgnoreCase(emailOrPhone);
        } else {
            return findOneWithAuthoritiesByPhone(emailOrPhone);
        }
    }

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "authorities")
    Page<User> findAll(Pageable pageable);
}
