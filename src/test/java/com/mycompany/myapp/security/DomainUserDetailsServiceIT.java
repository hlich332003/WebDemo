package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.service.UserService;
import java.util.Locale;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integrations tests for {@link DomainUserDetailsService}.
 */
@Transactional
@IntegrationTest
class DomainUserDetailsServiceIT {

    private static final String USER_ONE_EMAIL = "test-user-one@localhost";
    private static final String USER_ONE_PHONE = "0123456789";
    private static final String USER_TWO_EMAIL = "test-user-two@localhost";
    private static final String USER_TWO_PHONE = "0987654321";
    private static final String USER_THREE_EMAIL = "test-user-three@localhost";
    private static final String USER_THREE_PHONE = "0123123123";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("userDetailsService")
    private UserDetailsService domainUserDetailsService;

    public User getUserOne() {
        User userOne = new User();
        userOne.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        userOne.setActivated(true);
        userOne.setEmail(USER_ONE_EMAIL);
        userOne.setPhone(USER_ONE_PHONE);
        userOne.setFirstName("userOne");
        userOne.setLastName("doe");
        userOne.setLangKey("en");
        return userOne;
    }

    public User getUserTwo() {
        User userTwo = new User();
        userTwo.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        userTwo.setActivated(true);
        userTwo.setEmail(USER_TWO_EMAIL);
        userTwo.setPhone(USER_TWO_PHONE);
        userTwo.setFirstName("userTwo");
        userTwo.setLastName("doe");
        userTwo.setLangKey("en");
        return userTwo;
    }

    public User getUserThree() {
        User userThree = new User();
        userThree.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        userThree.setActivated(false);
        userThree.setEmail(USER_THREE_EMAIL);
        userThree.setPhone(USER_THREE_PHONE);
        userThree.setFirstName("userThree");
        userThree.setLastName("doe");
        userThree.setLangKey("en");
        return userThree;
    }

    @BeforeEach
    void init() {
        userRepository.save(getUserOne());
        userRepository.save(getUserTwo());
        userRepository.save(getUserThree());
    }

    @AfterEach
    void cleanup() {
        userService.deleteUser(USER_ONE_EMAIL);
        userService.deleteUser(USER_TWO_EMAIL);
        userService.deleteUser(USER_THREE_EMAIL);
    }

    @Test
    void assertThatUserCanBeFoundByEmail() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_ONE_EMAIL);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_EMAIL);
    }

    @Test
    void assertThatUserCanBeFoundByEmailIgnoreCase() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_ONE_EMAIL.toUpperCase(Locale.ENGLISH));
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_EMAIL);
    }

    @Test
    void assertThatUserCanBeFoundByPhone() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_TWO_PHONE);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_TWO_EMAIL); // Should return email as username
    }

    @Test
    void assertThatEmailIsPrioritizedOverLogin() {
        // This test case is no longer relevant as login is removed.
        // We now prioritize email, then phone.
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_ONE_EMAIL);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_EMAIL);
    }

    @Test
    void assertThatUserNotActivatedExceptionIsThrownForNotActivatedUsers() {
        assertThatExceptionOfType(UserNotActivatedException.class).isThrownBy(() ->
            domainUserDetailsService.loadUserByUsername(USER_THREE_EMAIL)
        );
    }
}
