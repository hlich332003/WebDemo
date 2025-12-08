package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.MailService;
import com.mycompany.myapp.service.RefreshTokenService;
import com.mycompany.myapp.service.TokenBlacklistService;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.PasswordChangeDTO;
import com.mycompany.myapp.web.rest.errors.*;
import com.mycompany.myapp.web.rest.vm.KeyAndPasswordVM;
import com.mycompany.myapp.web.rest.vm.ManagedUserVM;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping(value = "/api", produces = "application/json; charset=UTF-8")
public class AccountResource {

    private static class AccountResourceException extends RuntimeException {

        private AccountResourceException(String message) {
            super(message);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    private final TokenBlacklistService tokenBlacklistService;

    private final RefreshTokenService refreshTokenService;

    private final JwtDecoder jwtDecoder;

    public AccountResource(
        UserRepository userRepository,
        UserService userService,
        MailService mailService,
        TokenBlacklistService tokenBlacklistService,
        RefreshTokenService refreshTokenService,
        JwtDecoder jwtDecoder
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        // Send creation/welcome email instead of an activation email (accounts are auto-activated)
        mailService.sendCreationEmail(user);
    }

    /**
     * {@code GET  /activate} : activate the registered user.
     *
     * @param key the activation key.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be activated.
     */
    @GetMapping("/activate")
    public void activateAccount(@RequestParam(value = "key") String key) {
        // Activation endpoint is kept for compatibility but activation is disabled
        // Accounts are auto-activated at registration. Log and return.
        LOG.info("Activation endpoint called with key='{}'. Activation is disabled; accounts are auto-activated.", key);
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/account")
    public AdminUserDTO getAccount() {
        return userService
            .getUserWithAuthorities()
            .map(AdminUserDTO::new)
            .orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user login wasn't found.
     */
    @PostMapping(value = "/account", consumes = "application/json; charset=UTF-8")
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        String userEmail = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.orElseThrow().getEmail().equalsIgnoreCase(userEmail))) {
            throw new EmailAlreadyUsedException();
        }
        Optional<User> user = userRepository.findOneByEmailIgnoreCase(userEmail);
        if (!user.isPresent()) {
            throw new AccountResourceException("User could not be found");
        }
        userService.updateUser(
            userDTO.getFirstName(),
            userDTO.getLastName(),
            userDTO.getEmail(),
            userDTO.getLangKey(),
            userDTO.getImageUrl()
        );
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        Optional<User> user = userService.requestPasswordReset(mail);
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.orElseThrow());
        } else {
            // Pretend the request has been successful to prevent checking which emails really exist
            // but log that an invalid attempt has been made
            LOG.warn("Password reset requested for non existing mail");
        }
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the password could not be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return (
            StringUtils.isEmpty(password) ||
            password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH ||
            password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH
        );
    }

    /**
     * {@code POST /account/logout} : Logout user và vô hiệu hóa token.
     * Token sẽ được thêm vào blacklist trong Redis.
     *
     * @param authHeader Authorization header chứa JWT token
     */
    @PostMapping("/account/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Parse JWT để lấy expiry date
                Jwt jwt = jwtDecoder.decode(token);
                Instant expiryDate = jwt.getExpiresAt();

                // Thêm token vào blacklist
                if (expiryDate != null) {
                    tokenBlacklistService.blacklistToken(token, expiryDate);
                    LOG.debug("Token blacklisted successfully");
                }

                // Xóa refresh token của user (nếu có)
                SecurityUtils.getCurrentUserLogin()
                    .flatMap(userRepository::findOneByEmailIgnoreCase)
                    .ifPresent(user -> {
                        refreshTokenService.deleteByUserId(user.getId());
                        LOG.debug("Refresh token deleted for user: {}", user.getEmail());
                    });
            } catch (Exception e) {
                LOG.error("Error during logout: {}", e.getMessage());
                // Vẫn trả về 204 để client xóa token ở localStorage
            }
        }
    }
}
