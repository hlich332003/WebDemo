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

    String USERS_BY_LOGIN_CACHE = "usersByLogin"; // kept for backward-compat name but login-related methods adjusted

    Optional<User> findOneByActivationKey(String activationKey);

    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByPhone(String phone);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "authorities")
    Page<User> findAll(Pageable pageable);

    // Provide explicit queries for lookup by 'login' identifier (which can be email or phone)
    @Query("select u from User u where lower(u.email) = lower(:identifier) or u.phone = :identifier")
    Optional<User> findOneByLogin(@Param("identifier") String identifier);

    @EntityGraph(attributePaths = "authorities")
    @Query("select u from User u where lower(u.email) = lower(:identifier) or u.phone = :identifier")
    Optional<User> findOneWithAuthoritiesByLogin(@Param("identifier") String identifier);
}
