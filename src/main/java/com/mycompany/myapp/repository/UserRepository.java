package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @EntityGraph(attributePaths = "authority")
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "authority")
    Optional<User> findOneWithAuthoritiesByPhone(String phone);

    @EntityGraph(attributePaths = "authority")
    default Optional<User> findOneWithAuthoritiesByEmailOrPhone(String emailOrPhone) {
        if (emailOrPhone.contains("@")) {
            return findOneWithAuthoritiesByEmailIgnoreCase(emailOrPhone);
        } else {
            return findOneWithAuthoritiesByPhone(emailOrPhone);
        }
    }

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "authority")
    Page<User> findAll(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.authority a WHERE a.name = :authorityName")
    Long countByAuthority_Name(@Param("authorityName") String authorityName);

    @EntityGraph(attributePaths = "authority")
    @Query("SELECT u FROM User u JOIN u.authority a WHERE a.name = :authorityName")
    Page<User> findAllByAuthority_Name(@Param("authorityName") String authorityName, Pageable pageable);

    @EntityGraph(attributePaths = "authority")
    @Query("SELECT u FROM User u JOIN u.authority a WHERE a.name = :authorityName")
    List<User> findAllByAuthority(@Param("authorityName") String authorityName);
}
