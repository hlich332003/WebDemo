package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.PasswordChangeDTO;
import com.mycompany.myapp.web.rest.vm.KeyAndPasswordVM;
import com.mycompany.myapp.web.rest.vm.ManagedUserVM;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AccountResource} REST controller.
 */
@AutoConfigureMockMvc
@IntegrationTest
class AccountResourceIT {

    static final String TEST_USER_EMAIL = "test@localhost";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc restAccountMockMvc;

    private Long numberOfUsers;

    @BeforeEach
    void countUsers() {
        numberOfUsers = userRepository.count();
    }

    @AfterEach
    void cleanupAndCheck() {
        assertThat(userRepository.count()).isEqualTo(numberOfUsers);
        numberOfUsers = null;
    }

    @Test
    @WithUnauthenticatedMockUser
    void testNonAuthenticatedUser() throws Exception {
        restAccountMockMvc.perform(get("/api/authenticate")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(TEST_USER_EMAIL)
    void testAuthenticatedUser() throws Exception {
        restAccountMockMvc.perform(get("/api/authenticate").with(request -> request)).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(TEST_USER_EMAIL)
    void testGetExistingAccount() throws Exception {
        Set<String> authorities = new HashSet<>();
        authorities.add(AuthoritiesConstants.ADMIN);

        AdminUserDTO user = new AdminUserDTO();
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail(TEST_USER_EMAIL);
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setAuthorities(authorities);
        userService.createUser(user);

        restAccountMockMvc
            .perform(get("/api/account").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.email").value(TEST_USER_EMAIL))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
            .andExpect(jsonPath("$.langKey").value("en"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));

        userService.deleteUser(TEST_USER_EMAIL);
    }

    @Test
    void testGetUnknownAccount() throws Exception {
        restAccountMockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void testRegisterValid() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM();
        validUser.setPassword("password");
        validUser.setFirstName("Alice");
        validUser.setLastName("Test");
        validUser.setEmail("test-register-valid@example.com");
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        validUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
        assertThat(userRepository.findOneByEmailIgnoreCase("test-register-valid@example.com")).isEmpty();

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(validUser)))
            .andExpect(status().isCreated());

        assertThat(userRepository.findOneByEmailIgnoreCase("test-register-valid@example.com")).isPresent();

        userService.deleteUser("test-register-valid@example.com");
    }

    static Stream<ManagedUserVM> invalidUsers() {
        return Stream.of(
            createInvalidUser("password", "Bob", "Green", "invalid", true), // <-- invalid email
            createInvalidUser("123", "Bob", "Green", "bob@example.com", true), // password with only 3 digits
            createInvalidUser(null, "Bob", "Green", "bob@example.com", true) // invalid null password
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUsers")
    @Transactional
    void testRegisterInvalidUsers(ManagedUserVM invalidUser) throws Exception {
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmailIgnoreCase("bob@example.com");
        assertThat(user).isEmpty();
    }

    private static ManagedUserVM createInvalidUser(String password, String firstName, String lastName, String email, boolean activated) {
        ManagedUserVM invalidUser = new ManagedUserVM();
        invalidUser.setPassword(password);
        invalidUser.setFirstName(firstName);
        invalidUser.setLastName(lastName);
        invalidUser.setEmail(email);
        invalidUser.setActivated(activated);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
        return invalidUser;
    }

    @Test
    @Transactional
    void testRegisterDuplicateEmail() throws Exception {
        // First user
        ManagedUserVM firstUser = new ManagedUserVM();
        firstUser.setPassword("password");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Test");
        firstUser.setEmail("test-register-duplicate-email@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Register first user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(firstUser)))
            .andExpect(status().isCreated());

        Optional<User> testUser1 = userRepository.findOneByEmailIgnoreCase("test-register-duplicate-email@example.com");
        assertThat(testUser1).isPresent();

        // Duplicate email, different login
        ManagedUserVM secondUser = new ManagedUserVM();
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail(firstUser.getEmail());
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
        secondUser.setAuthorities(firstUser.getAuthorities());

        // Register second (non activated) user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(secondUser)))
            .andExpect(status().isCreated());

        Optional<User> testUser3 = userRepository.findOneByEmailIgnoreCase("test-register-duplicate-email@example.com");
        assertThat(testUser3).isPresent();

        // Duplicate email - with uppercase email address
        ManagedUserVM userWithUpperCaseEmail = new ManagedUserVM();
        userWithUpperCaseEmail.setPassword(firstUser.getPassword());
        userWithUpperCaseEmail.setFirstName(firstUser.getFirstName());
        userWithUpperCaseEmail.setLastName(firstUser.getLastName());
        userWithUpperCaseEmail.setEmail("TEST-register-duplicate-email@example.com");
        userWithUpperCaseEmail.setImageUrl(firstUser.getImageUrl());
        userWithUpperCaseEmail.setLangKey(firstUser.getLangKey());
        userWithUpperCaseEmail.setAuthorities(firstUser.getAuthorities());

        // Register third (not activated) user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userWithUpperCaseEmail)))
            .andExpect(status().isCreated());

        Optional<User> testUser4 = userRepository.findOneByEmailIgnoreCase("test-register-duplicate-email@example.com");
        assertThat(testUser4).isPresent();
        assertThat(testUser4.orElseThrow().getEmail()).isEqualTo("test-register-duplicate-email@example.com");

        testUser4.orElseThrow().setActivated(true);
        userService.updateUser((new AdminUserDTO(testUser4.orElseThrow())));

        // Register 4th (already activated) user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(secondUser)))
            .andExpect(status().is4xxClientError());

        userService.deleteUser("test-register-duplicate-email@example.com");
    }

    @Test
    @Transactional
    void testRegisterAdminIsIgnored() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM();
        validUser.setPassword("password");
        validUser.setFirstName("Bad");
        validUser.setLastName("Guy");
        validUser.setEmail("badguy@example.com");
        validUser.setActivated(true);
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        validUser.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> userDup = userRepository.findOneWithAuthoritiesByEmailIgnoreCase("badguy@example.com");
        assertThat(userDup).isPresent();
        assertThat(userDup.orElseThrow().getAuthority()).isNotNull();
        assertThat(userDup.orElseThrow().getAuthority().getName()).isEqualTo(AuthoritiesConstants.USER);

        userService.deleteUser("badguy@example.com");
    }

    @Test
    @Transactional
    void testActivateAccount() throws Exception {
        final String activationKey = "some activation key";
        User user = new User();
        user.setEmail("activate-account@example.com");
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(false);
        user.setActivationKey(activationKey);
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());

        userRepository.saveAndFlush(user);

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", activationKey)).andExpect(status().isOk());

        user = userRepository.findOneByEmailIgnoreCase(user.getEmail()).orElse(null);
        assertThat(user.isActivated()).isTrue();

        userService.deleteUser("activate-account@example.com");
    }

    @Test
    @Transactional
    void testActivateAccountWithWrongKey() throws Exception {
        restAccountMockMvc.perform(get("/api/activate?key=wrongActivationKey")).andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional
    @WithMockUser("save-account@example.com")
    void testSaveAccount() throws Exception {
        User user = new User();
        user.setEmail("save-account@example.com");
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-account@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneWithAuthoritiesByEmailIgnoreCase(user.getEmail()).orElse(null);
        assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
        assertThat(updatedUser.isActivated()).isTrue();
        assertThat(updatedUser.getAuthority().getName()).isEqualTo(AuthoritiesConstants.USER);

        userService.deleteUser("save-account@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("save-invalid-email@example.com")
    void testSaveInvalidEmail() throws Exception {
        User user = new User();
        user.setEmail("save-invalid-email@example.com");
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());

        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("invalid email");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByEmailIgnoreCase("invalid email")).isNotPresent();

        userService.deleteUser("save-invalid-email@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("save-existing-email@example.com")
    void testSaveExistingEmail() throws Exception {
        User user = new User();
        user.setEmail("save-existing-email@example.com");
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        User anotherUser = new User();
        anotherUser.setEmail("save-existing-email2@example.com");
        anotherUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        anotherUser.setActivated(true);
        anotherUser.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());

        userRepository.saveAndFlush(anotherUser);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email2@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("save-existing-email@example.com").orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email@example.com");

        userService.deleteUser("save-existing-email@example.com");
        userService.deleteUser("save-existing-email2@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("save-existing-email-and-login@example.com")
    void testSaveExistingEmailAndLogin() throws Exception {
        User user = new User();
        user.setEmail("save-existing-email-and-login@example.com");
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email-and-login@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("save-existing-email-and-login@example.com").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-and-login@example.com");

        userService.deleteUser("save-existing-email-and-login@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("change-password-wrong-existing-password@example.com")
    void testChangePasswordWrongExistingPassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.insecure().nextAlphanumeric(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-wrong-existing-password@example.com");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(new PasswordChangeDTO("1" + currentPassword, "new password")))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-wrong-existing-password@example.com").orElse(null);
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isFalse();
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.getPassword())).isTrue();

        userService.deleteUser("change-password-wrong-existing-password@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("change-password@example.com")
    void testChangePassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.insecure().nextAlphanumeric(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password@example.com");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(new PasswordChangeDTO(currentPassword, "new password")))
            )
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password@example.com").orElse(null);
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isTrue();

        userService.deleteUser("change-password@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("change-password-too-small@example.com")
    void testChangePasswordTooSmall() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.insecure().nextAlphanumeric(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-too-small@example.com");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        String newPassword = RandomStringUtils.insecure().next(ManagedUserVM.PASSWORD_MIN_LENGTH - 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-too-small@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());

        userService.deleteUser("change-password-too-small@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("change-password-too-long@example.com")
    void testChangePasswordTooLong() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.insecure().nextAlphanumeric(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-too-long@example.com");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        String newPassword = RandomStringUtils.insecure().next(ManagedUserVM.PASSWORD_MAX_LENGTH + 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-too-long@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());

        userService.deleteUser("change-password-too-long@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("change-password-empty@example.com")
    void testChangePasswordEmpty() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.insecure().nextAlphanumeric(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-empty@example.com");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(new PasswordChangeDTO(currentPassword, "")))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-empty@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());

        userService.deleteUser("change-password-empty@example.com");
    }

    @Test
    @Transactional
    void testRequestPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setEmail("password-reset@example.com");
        user.setLangKey("en");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset@example.com"))
            .andExpect(status().isOk());

        userService.deleteUser("password-reset@example.com");
    }

    @Test
    @Transactional
    void testRequestPasswordResetUpperCaseEmail() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setEmail("password-reset-upper-case@example.com");
        user.setLangKey("en");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset-upper-case@EXAMPLE.COM"))
            .andExpect(status().isOk());

        userService.deleteUser("password-reset-upper-case@example.com");
    }

    @Test
    void testRequestPasswordResetWrongEmail() throws Exception {
        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset-wrong-email@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void testFinishPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setEmail("finish-password-reset@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("new password");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(keyAndPassword))
            )
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByEmailIgnoreCase(user.getEmail()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();

        userService.deleteUser("finish-password-reset@example.com");
    }

    @Test
    @Transactional
    void testFinishPasswordResetTooSmall() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setEmail("finish-password-reset-too-small@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key too small");
        user.setAuthority(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("foo");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(keyAndPassword))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase(user.getEmail()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isFalse();

        userService.deleteUser("finish-password-reset-too-small@example.com");
    }

    @Test
    @Transactional
    void testFinishPasswordResetWrongKey() throws Exception {
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("wrong reset key");
        keyAndPassword.setNewPassword("new password");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(keyAndPassword))
            )
            .andExpect(status().isInternalServerError());
    }
}
