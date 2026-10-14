package com.company.efood.sys.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.sys.dto.PasswordPolicyDto;
import com.company.efood.sys.dto.RefreshTokenDto;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.entity.PasswordPolicy;
import com.company.efood.sys.model.AuthResponseModel;
import com.company.efood.sys.model.CustomUserDetails;
import com.company.efood.sys.model.LoginRequestModel;
import com.company.efood.sys.model.RegisterRequestModel;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.services.EmailOtpService;
import com.company.efood.sys.services.SmsOtpService;
import com.company.efood.sys.services.PasswordPolicyService;
import com.company.efood.sys.services.RegisterService;
import com.company.efood.sys.utils.AppUserType;
import com.company.efood.sys.utils.AuthTokenUtils;
import com.company.efood.sys.utils.Gender;
import com.company.efood.user.entity.Customer;
import com.company.efood.user.repository.CustomerRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import static com.company.efood.base.BaseConstants.AUTHENTICATION_ENDPOINT;
import static com.company.efood.base.BaseConstants.USER_TYPE_ID_CUSTOMER;

@Slf4j
@RestController
@RequestMapping(AUTHENTICATION_ENDPOINT)
public class AuthController {

    @Value("${app-jwt-expiration-milliseconds}")
    private long expiration;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthTokenUtils authTokenUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @Autowired
    private EmailOtpService emailOtpService;

    @Autowired
    private SmsOtpService smsOtpService;

    @Autowired
    private AppUserRepo appUserRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private com.company.efood.user.services.ReferralService referralService;

    @PostMapping("signin")
    public ResponseEntity<BaseResponse> signIn(@Valid @RequestBody LoginRequestModel requestModel) {
        System.out.println("🔐 [AUTH CONTROLLER SIGNIN] Attempting login for username: " + requestModel.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestModel.getUsername(),
                            requestModel.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Enforce phone OTP verification
            AppUser appUser = appUserRepo.findByUsername(requestModel.getUsername()).orElse(null);
            if (appUser != null && !Boolean.TRUE.equals(appUser.getPhoneVerified())
                    && appUser.getAppUserType() != AppUserType.SYSTEM_ADMIN
                    && appUser.getAppUserType() != AppUserType.DEVELOPER) {

                String otp = String.format("%06d", (int) (Math.random() * 1000000));
                appUser.setOtp(otp);
                appUser.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
                appUserRepo.save(appUser);

                String phoneNumber = (appUser.getPhone() != null && !appUser.getPhone().isBlank())
                        ? appUser.getPhone()
                        : requestModel.getUsername();

                boolean smsSent = smsOtpService.sendOtp(phoneNumber, otp);
                log.info("📱 [OTP ENFORCEMENT] User '{}' is not OTP verified. Sent new SMS OTP to '{}'", appUser.getUsername(), phoneNumber);

                BaseResponse unverifiedResponse = BaseResponse.builder()
                        .status(false)
                        .statusCode(HttpStatus.FORBIDDEN.value())
                        .message("Your phone number is not verified. A verification code has been sent to " + phoneNumber + ".")
                        .messageBn("আপনার মোবাইল নম্বরটি যাচাই করা হয়নি। " + phoneNumber + " নম্বরে ওটিপি পাঠানো হয়েছে।")
                        .data(Map.of(
                                "requiresOtpVerification", true,
                                "phone", phoneNumber,
                                "username", appUser.getUsername(),
                                "smsSent", smsSent,
                                "otp", smsSent ? "HIDDEN" : otp
                        ))
                        .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(unverifiedResponse);
            }

            String token = authTokenUtils.generateJWTToken(authentication);
            RefreshTokenDto refreshTokenDto = authTokenUtils.generateRefreshToken(requestModel.getUsername());

            System.out.println("✅ [AUTH CONTROLLER SIGNIN SUCCESS] User: " + requestModel.getUsername() + " | UserTypeId: " + userDetails.getUserTypeId() + " | AppUserType: " + userDetails.getAppUserType());

            BaseResponse response = BaseResponse.builder()
                    .status(true)
                    .statusCode(HttpStatus.OK.value())
                    .message("Login Successful")
                    .messageBn("")
                    .data(new AuthResponseModel(token, refreshTokenDto.getRefreshToken(), userDetails.getUserTypeId(),
                            userDetails.getAppUserType(), System.currentTimeMillis(), expiration))
                    .build();

            return ResponseEntity.ok(response);

        } catch (InternalAuthenticationServiceException e) {
            System.out.println("❌ [AUTH CONTROLLER SIGNIN ERROR] Internal auth error: " + e.getMessage());
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            System.out.println("❌ [AUTH CONTROLLER SIGNIN ERROR] Bad credentials or login failed: " + e.getMessage());
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Wrong username or password")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        System.out.println("🔄 [AUTH CONTROLLER REFRESH] Received refresh request with token: " + (refreshToken != null ? "PRESENT" : "NULL"));

        if (refreshToken == null || !authTokenUtils.verifyRefreshToken(refreshToken)) {
            System.out.println("❌ [AUTH CONTROLLER REFRESH ERROR] Invalid or expired refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        RefreshTokenDto refreshTokenDto = authTokenUtils.getRefreshToken(refreshToken);
        Long userId = refreshTokenDto.getAppUserId();

        if (userId == null) {
            System.out.println("❌ [AUTH CONTROLLER REFRESH ERROR] AppUserId is NULL in refresh token record!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has no associated user. Please log in again.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(
                authTokenUtils.getUsernameById(userId));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String newAccessToken = authTokenUtils.generateJWTToken(authentication);
        System.out.println("🎉 [AUTH CONTROLLER REFRESH SUCCESS] Successfully issued new accessToken for userId: " + userId);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", refreshToken
        ));
    }

    @PostMapping("signup")
    public ResponseEntity<BaseResponse> registerUser(@Valid @RequestBody RegisterRequestModel requestModel,
            HttpServletRequest request) {

        final String password = requestModel.getPassword();
        PasswordPolicyDto passwordPolicy;

        try {
            passwordPolicy = passwordPolicyService.getPublicPasswordPolicyById(1L);
        } catch (Exception e) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error retrieving password policy")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        if (requestModel.getUsername().isEmpty() || password.isEmpty() || requestModel.getPhone() == null || requestModel.getPhone().trim().isEmpty()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                    .message("Username, Password, and Phone Number cannot be empty")
                    .build();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }

        if (registerService.getUserByUsername(requestModel.getUsername()).isPresent()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message("User already exists")
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (password.length() < passwordPolicy.getMinLength()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Password must be at least " + passwordPolicy.getMinLength() + " characters long")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            AppUser newUser = generateAppUserEntity(requestModel, passwordPolicy);
            String otp = String.format("%06d", (int) (Math.random() * 1000000));
            newUser.setOtp(otp);
            newUser.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
            AppUser savedUser = registerService.addUser(newUser);
            populateCustomerProfile(savedUser, requestModel);

            // Trigger SMS OTP (dev mode will log to console & leave in DB)
            if (requestModel.getPhone() != null && !requestModel.getPhone().isBlank()) {
                smsOtpService.sendOtp(requestModel.getPhone().trim(), otp);
            }

            // ✅ SECURITY: Do NOT issue JWT token here. The user must verify their phone
            // number via OTP before they can sign in and receive an access token.
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("requiresOtpVerification", true);
            data.put("phone", requestModel.getPhone() != null ? requestModel.getPhone().trim() : "");
            data.put("username", requestModel.getUsername());

            BaseResponse response = BaseResponse.builder()
                    .status(true)
                    .statusCode(HttpStatus.ACCEPTED.value())
                    .message("Registration successful. A 6-digit verification code has been sent to " + requestModel.getPhone() + ". Please verify your phone to continue.")
                    .messageBn("")
                    .data(data)
                    .build();

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Registration failed: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/email-otp/request")
    public ResponseEntity<BaseResponse> requestEmailOtp(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Username is required")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        AppUser user = appUserRepo.findByUsername(username).orElse(null);
        if (user == null) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("User not found")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        user.setEmailVerified(false);
        user.setUpdateDate(LocalDateTime.now());
        appUserRepo.save(user);

        boolean mailSent = emailOtpService.sendOtpEmail(username, otp);
        BaseResponse response = BaseResponse.builder()
                .status(true)
                .statusCode(HttpStatus.OK.value())
                .message(mailSent ? "Email OTP sent successfully" : "Email OTP generated in development fallback mode")
                .data(mailSent
                        ? Map.of("expiresAt", user.getOtpExpiresAt().toString())
                        : Map.of("otp", otp, "expiresAt", user.getOtpExpiresAt().toString()))
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email-otp/verify")
    public ResponseEntity<BaseResponse> verifyEmailOtp(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String otp = body.get("otp");
        if (username == null || username.isBlank() || otp == null || otp.isBlank()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Username and OTP are required")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        AppUser user = appUserRepo.findByUsername(username).orElse(null);
        if (user == null) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("User not found")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("OTP expired")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!otp.equals(user.getOtp())) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("OTP did not match")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiresAt(null);
        user.setUpdateDate(LocalDateTime.now());
        appUserRepo.save(user);

        BaseResponse response = BaseResponse.builder()
                .status(true)
                .statusCode(HttpStatus.OK.value())
                .message("Email verified successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/phone-otp/request")
    public ResponseEntity<BaseResponse> requestPhoneOtp(@RequestBody Map<String, String> body) {
        String identifier = body.get("phone");
        if (identifier == null || identifier.isBlank()) {
            identifier = body.get("username");
        }
        if (identifier == null || identifier.isBlank()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Phone or username is required")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        final String target = identifier.trim();
        AppUser user = appUserRepo.findByPhone(target)
                .or(() -> appUserRepo.findByUsername(target))
                .orElse(null);

        if (user == null) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("User not found with phone/username: " + target)
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        user.setUpdateDate(LocalDateTime.now());
        appUserRepo.save(user);

        String phoneNumber = user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : target;
        boolean smsSent = smsOtpService.sendOtp(phoneNumber, otp);

        BaseResponse response = BaseResponse.builder()
                .status(true)
                .statusCode(HttpStatus.OK.value())
                .message(smsSent ? "OTP sent to your phone" : "OTP generated for testing. Check database OTP column or server logs.")
                .data(Map.of(
                        "phone", phoneNumber,
                        "smsSent", smsSent,
                        "otp", smsSent ? "HIDDEN" : otp, // Exposing in response when SMS is disabled for easy developer testing
                        "expiresAt", user.getOtpExpiresAt().toString()
                ))
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/phone-otp/verify")
    public ResponseEntity<BaseResponse> verifyPhoneOtp(@RequestBody Map<String, String> body) {
        String identifier = body.get("phone");
        if (identifier == null || identifier.isBlank()) {
            identifier = body.get("username");
        }
        String otp = body.get("otp");

        if (identifier == null || identifier.isBlank() || otp == null || otp.isBlank()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Phone/username and OTP are required")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        final String target = identifier.trim();
        AppUser user = appUserRepo.findByPhone(target)
                .or(() -> appUserRepo.findByUsername(target))
                .orElse(null);

        if (user == null) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("User not found")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("OTP expired. Please request a new code.")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!otp.trim().equals(user.getOtp())) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid OTP code")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        user.setPhoneVerified(true);
        user.setOtp(null);
        user.setOtpExpiresAt(null);
        user.setUpdateDate(LocalDateTime.now());
        appUserRepo.save(user);

        BaseResponse response = BaseResponse.builder()
                .status(true)
                .statusCode(HttpStatus.OK.value())
                .message("Phone number verified successfully")
                .data(Map.of(
                        "phoneVerified", true,
                        "username", user.getUsername()
                ))
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<BaseResponse> changePassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (username == null || username.isBlank() ||
            oldPassword == null || oldPassword.isBlank() ||
            newPassword == null || newPassword.isBlank()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Username, old password, and new password are required")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        AppUser user = appUserRepo.findByUsername(username).orElse(null);
        if (user == null) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("User account not found")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Incorrect current password")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        int minLength = (user.getPasswordPolicy() != null && user.getPasswordPolicy().getMinLength() != null)
                ? user.getPasswordPolicy().getMinLength() : 5;
        if (newPassword.length() < minLength) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("New password must be at least " + minLength + " characters long")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateDate(LocalDateTime.now());
        appUserRepo.save(user);

        BaseResponse response = BaseResponse.builder()
                .status(true)
                .statusCode(HttpStatus.OK.value())
                .message("Password changed successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<BaseResponse> forgotPasswordRequest(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Username/Email is required")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        AppUser user = appUserRepo.findByUsername(username).orElse(null);
        if (user == null) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("No account found with this username or email")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setUpdateDate(LocalDateTime.now());
        appUserRepo.save(user);

        boolean mailSent = emailOtpService.sendOtpEmail(username, otp);
        BaseResponse response = BaseResponse.builder()
                .status(true)
                .statusCode(HttpStatus.OK.value())
                .message(mailSent ? "Password reset OTP sent to your email" : "Password reset OTP generated (development mode)")
                .data(mailSent
                        ? Map.of("expiresAt", user.getOtpExpiresAt().toString())
                        : Map.of("otp", otp, "expiresAt", user.getOtpExpiresAt().toString()))
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<BaseResponse> forgotPasswordReset(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        if (username == null || username.isBlank() ||
            otp == null || otp.isBlank() ||
            newPassword == null || newPassword.isBlank()) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Username, OTP, and new password are required")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        AppUser user = appUserRepo.findByUsername(username).orElse(null);
        if (user == null) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("User not found")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("OTP has expired. Please request a new one.")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!otp.equals(user.getOtp())) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid OTP code.")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        int minLength = (user.getPasswordPolicy() != null && user.getPasswordPolicy().getMinLength() != null)
                ? user.getPasswordPolicy().getMinLength() : 5;
        if (newPassword.length() < minLength) {
            BaseResponse response = BaseResponse.builder()
                    .status(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("New password must be at least " + minLength + " characters long")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtp(null);
        user.setOtpExpiresAt(null);
        user.setUpdateDate(LocalDateTime.now());
        appUserRepo.save(user);

        BaseResponse response = BaseResponse.builder()
                .status(true)
                .statusCode(HttpStatus.OK.value())
                .message("Password has been reset successfully. You can now log in.")
                .build();
        return ResponseEntity.ok(response);
    }

    private AppUser generateAppUserEntity(RegisterRequestModel requestModel, PasswordPolicyDto passwordPolicy) {
        AppUser appUser = new AppUser();
        appUser.setDisplayName(requestModel.getDisplayName());
        appUser.setUsername(requestModel.getUsername());
        appUser.setPassword(passwordEncoder.encode(requestModel.getPassword()));
        appUser.setPasswordPolicy(generatePasswordPolicyEntity(passwordPolicy));
        appUser.setEntryUser(0L);
        appUser.setEntryDate(LocalDateTime.now());
        appUser.setAppUserType(AppUserType.CUSTOMER);
        appUser.setUserTypeId(USER_TYPE_ID_CUSTOMER);
        appUser.setPhone(requestModel.getPhone() != null ? requestModel.getPhone().trim() : null);
        appUser.setPhoneVerified(false);
        appUser.setEmailVerified(false);
        return appUser;
    }

    private PasswordPolicy generatePasswordPolicyEntity(PasswordPolicyDto passwordPolicy) {
        PasswordPolicy entity = new PasswordPolicy();
        BeanUtils.copyProperties(passwordPolicy, entity);
        return entity;
    }

    private void populateCustomerProfile(AppUser savedUser, RegisterRequestModel requestModel) {
        if (savedUser == null || !AppUserType.CUSTOMER.equals(savedUser.getAppUserType())) {
            return;
        }

        Customer customer = customerRepo.findByAppUserId(savedUser.getId())
                .orElseGet(() -> customerRepo.save(new Customer(savedUser)));

        if (requestModel.getDisplayName() != null && !requestModel.getDisplayName().isBlank()) {
            customer.setFullName(requestModel.getDisplayName().trim());
        }
        if (requestModel.getPhone() != null && !requestModel.getPhone().isBlank()) {
            customer.setPhone(requestModel.getPhone().trim());
        }
        if (requestModel.getUsername() != null && !requestModel.getUsername().isBlank()) {
            customer.setEmail(requestModel.getUsername().trim());
        }
        Gender gender = resolveGender(requestModel.getGender());
        if (gender != null) {
            customer.setGender(gender);
        }
        customer = customerRepo.save(customer);
        savedUser.setCustomer(customer);

        // Initialize unique referral code and link referrer if referralCode supplied
        referralService.initReferral(customer, requestModel.getReferralCode());
    }

    private Gender resolveGender(String genderValue) {
        if (genderValue == null || genderValue.isBlank()) {
            return null;
        }

        String normalized = genderValue.trim().toUpperCase();
        return switch (normalized) {
            case "MALE" -> Gender.MALE;
            case "FEMALE" -> Gender.FEMALE;
            case "OTHER" -> Gender.OTHER;
            default -> null;
        };
    }
}