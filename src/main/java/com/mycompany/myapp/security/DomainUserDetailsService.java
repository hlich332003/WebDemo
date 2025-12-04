package com.mycompany.myapp.security;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final UserRepository userRepository;

    // Sửa lỗi "illegal escape character" bằng cách thay đổi regex
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

    public DomainUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);

        if (new EmailValidator().isValid(login, null)) {
            return userRepository
                .findOneWithAuthoritiesByEmailIgnoreCase(login)
                .map(user -> createSpringSecurityUser(login, user))
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + login + " was not found in the database"));
        }

        if (PHONE_PATTERN.matcher(login).matches()) {
            return userRepository
                .findOneWithAuthoritiesByPhone(login)
                .map(user -> createSpringSecurityUser(login, user))
                .orElseThrow(() -> new UsernameNotFoundException("User with phone " + login + " was not found in the database"));
        }

        // Fallback: the repository supports a combined lookup (email OR phone) via findOneWithAuthoritiesByLogin
        // This covers cases where the identifier isn't clearly an email or pure phone number
        return userRepository
            .findOneWithAuthoritiesByLogin(login)
            .map(user -> createSpringSecurityUser(login, user))
            .orElseThrow(() -> new UsernameNotFoundException("User " + login + " was not found in the database"));
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String username, User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + username + " was not activated");
        }
        List<GrantedAuthority> grantedAuthorities = user
            .getAuthorities()
            .stream()
            .map(authority -> new SimpleGrantedAuthority(authority.getName()))
            .collect(Collectors.toList());
        String principal = username; // use the provided login identifier (email or phone)
        return new UserWithId(principal, user.getPassword(), grantedAuthorities, user.getId());
    }

    public static class UserWithId extends org.springframework.security.core.userdetails.User {

        private static final long serialVersionUID = 1L;

        private final Long id;

        public UserWithId(String username, String password, Collection<? extends GrantedAuthority> authorities, Long id) {
            super(username, password, authorities);
            this.id = id;
        }

        public Long getId() {
            return this.id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            UserWithId that = (UserWithId) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), id);
        }

        @Override
        public String toString() {
            return "UserWithId{" + "id=" + id + ", " + super.toString() + "}";
        }
    }
}
