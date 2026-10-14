package com.company.efood.seller.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.notification.service.FcmNotificationService;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.sys.dto.PasswordPolicyDto;
import com.company.efood.sys.dto.RefreshTokenDto;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.entity.PasswordPolicy;
import com.company.efood.sys.model.AuthResponseModel;
import com.company.efood.sys.model.CustomUserDetails;
import com.company.efood.sys.model.RegisterRequestModel;
import com.company.efood.sys.services.PasswordPolicyService;
import com.company.efood.sys.services.RegisterService;
import com.company.efood.sys.utils.AppUserType;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.company.efood.base.BaseConstants.*;

@Slf4j
@RestController
@RequestMapping(SELLER_PUBLIC_END_POINT)
@RequiredArgsConstructor
public class SellerRegistrationController {

    @Value("${app-jwt-expiration-milliseconds}")
    private long expiration;
    private final AuthenticationManager authenticationManager;
    private final AuthTokenUtils authTokenUtils;
    private final PasswordEncoder passwordEncoder;
    private final RegisterService registerService;
    private final PasswordPolicyService passwordPolicyService;
    private final SellerRepo sellerRepo;
    private final FcmNotificationService fcmNotificationService;
    private final com.company.efood.sys.repository.ShopCategoryRepo shopCategoryRepo;
    private final com.company.efood.sys.services.SmsOtpService smsOtpService;

    @PostMapping("signup")
    public BaseResponse registerUser(@Valid @RequestBody RegisterRequestModel requestModel, HttpServletRequest request) {
        if (!isValidRequest(requestModel)) {
            return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Username, Password, and Contact Phone Number cannot be empty!");
        }

        PasswordPolicyDto passwordPolicy = getPasswordPolicy();
        if (passwordPolicy == null) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving password policy");
        }

        if (isUserExists(requestModel.getUsername())) {
            return createErrorResponse(HttpStatus.CONFLICT, "User already exists");
        }

        if (!isValidPassword(requestModel.getPassword(), passwordPolicy.getMinLength())) {
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                String.format("Password must be at least %d characters long", passwordPolicy.getMinLength()));
        }

        try {
            return processRegistration(requestModel, passwordPolicy);
        } catch (InternalAuthenticationServiceException e) {
            log.error("Authentication service error during seller signup", e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Seller registration failure", e);
            return createErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    private boolean isValidRequest(RegisterRequestModel requestModel) {
        return Objects.nonNull(requestModel.getUsername()) && !requestModel.getUsername().trim().isEmpty()
            && Objects.nonNull(requestModel.getPassword()) && !requestModel.getPassword().trim().isEmpty()
            && Objects.nonNull(requestModel.getPhone()) && !requestModel.getPhone().trim().isEmpty();
    }

    private PasswordPolicyDto getPasswordPolicy() {
        try {
            return passwordPolicyService.getPublicPasswordPolicyById(1L);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isUserExists(String username) {
        return registerService.getUserByUsername(username).isPresent();
    }

    private boolean isValidPassword(String password, int minLength) {
        return password.length() >= minLength;
    }

    private BaseResponse processRegistration(RegisterRequestModel requestModel, PasswordPolicyDto passwordPolicy) {
        AppUser user = generateSellerEntity(requestModel, passwordPolicy);
        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        AppUser savedUser = registerService.addUser(user);

        // Populate Seller profile
        Seller seller = sellerRepo.findByAppUserId(savedUser.getId())
                .orElseGet(() -> new Seller(savedUser));
        if (requestModel.getDisplayName() != null && !requestModel.getDisplayName().isBlank()) {
            seller.setFullName(requestModel.getDisplayName().trim());
        }
        if (requestModel.getPhone() != null && !requestModel.getPhone().isBlank()) {
            seller.setPhone(requestModel.getPhone().trim());
        }

        // Link selected ShopCategory
        if (requestModel.getShopCategoryId() != null) {
            shopCategoryRepo.findById(requestModel.getShopCategoryId())
                    .ifPresent(seller::setShopCategory);
        } else if (requestModel.getShopCategory() != null && !requestModel.getShopCategory().isBlank()) {
            shopCategoryRepo.findByCode(requestModel.getShopCategory().trim().toUpperCase())
                    .ifPresent(seller::setShopCategory);
        }

        sellerRepo.save(seller);

        // Send SMS OTP (dev mode will log and keep in DB)
        if (requestModel.getPhone() != null && !requestModel.getPhone().isBlank()) {
            smsOtpService.sendOtp(requestModel.getPhone().trim(), otp);
        }

        // Notify Admin Portal
        try {
            fcmNotificationService.notifyAdminNewRegistration("Seller", requestModel.getDisplayName(), requestModel.getPhone(), savedUser.getId());
        } catch (Exception e) {
            log.warn("Could not notify admin of new seller registration: {}", e.getMessage());
        }

        // ✅ SECURITY: Do NOT issue JWT token here. The seller must verify phone via OTP first.
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("requiresOtpVerification", true);
        data.put("phone", requestModel.getPhone() != null ? requestModel.getPhone().trim() : "");
        data.put("username", requestModel.getUsername());

        return BaseResponse.builder()
                .statusCode(HttpStatus.ACCEPTED.value())
                .status(true)
                .message("Registration successful. A 6-digit verification code has been sent to " + requestModel.getPhone() + ". Please verify your phone to continue.")
                .data(data)
                .build();
    }

    private BaseResponse createErrorResponse(HttpStatus status, String message) {
        return BaseResponse.builder()
                .statusCode(status.value())
                .status(false)
                .message(message)
                .build();
    }

    private AppUser generateSellerEntity(RegisterRequestModel requestModel, PasswordPolicyDto pw) {
        AppUser appUser = new AppUser();
        appUser.setDisplayName(requestModel.getDisplayName());
        appUser.setUsername(requestModel.getUsername().trim());
        appUser.setPassword(passwordEncoder.encode(requestModel.getPassword()));
        appUser.setPasswordPolicy(generatePasswordPolicyEntity(pw));
        appUser.setEntryUser(0L);
        appUser.setEntryDate(LocalDateTime.now());
        appUser.setAppUserType(AppUserType.SELLER);
        appUser.setUserTypeId(USER_TYPE_ID_SELLER);
        appUser.setPhone(requestModel.getPhone() != null ? requestModel.getPhone().trim() : null);
        appUser.setPhoneVerified(false);
        return appUser;
    }

    private PasswordPolicy generatePasswordPolicyEntity(PasswordPolicyDto pw) {
        PasswordPolicy entity = new PasswordPolicy();
        BeanUtils.copyProperties(pw, entity);
        return entity;
    }
}