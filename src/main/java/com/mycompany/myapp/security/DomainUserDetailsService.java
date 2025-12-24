package com.mycompany.myapp.security;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import java.util.*;
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

    public DomainUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);

        // Sử dụng phương thức tìm kiếm linh hoạt cho cả Email và SĐT
        return userRepository
            .findOneWithAuthoritiesByEmailOrPhone(login)
            .map(user -> createSpringSecurityUser(login, user))
            .orElseThrow(() -> new UsernameNotFoundException("User with email or phone " + login + " was not found in the database"));
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        // Authority trong User entity là quan hệ ManyToOne (User -> Authority)
        // Nhưng Spring Security cần List<GrantedAuthority>
        List<SimpleGrantedAuthority> grantedAuthorities = Collections.singletonList(
            new SimpleGrantedAuthority(user.getAuthority().getName())
        );

        // Luôn sử dụng Email làm username cho Spring Security Principal
        // Điều này đảm bảo tính nhất quán cho WebSocket và các dịch vụ khác
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), grantedAuthorities);
    }

    /**
     * Inner class to hold User details with ID
     */
    public static class UserWithId extends org.springframework.security.core.userdetails.User {

        private final Long id;

        public UserWithId(String username, String password, Collection<? extends GrantedAuthority> authorities, Long id) {
            super(username, password, authorities);
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
