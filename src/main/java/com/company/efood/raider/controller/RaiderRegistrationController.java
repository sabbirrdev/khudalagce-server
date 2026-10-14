package com.company.efood.raider.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.notification.service.FcmNotificationService;
import com.company.efood.raider.dto.RaiderDto;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.repository.RaiderRepo;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.company.efood.base.BaseConstants.*;

@Slf4j
@RestController
@RequestMapping(RAIDER_PUBLIC_END_POINT)
public class RaiderRegistrationController {

    @Value("${app-jwt-expiration-milliseconds}")
    private long expiration;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthTokenUtils authTokenUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @Autowired
    private RaiderRepo raiderRepo;

    @Autowired
    private BaseUtils baseUtils;

    @Autowired
    private FcmNotificationService fcmNotificationService;

    @Autowired
    private com.company.efood.sys.services.SmsOtpService smsOtpService;

    @GetMapping("available")
    public BaseResponse getAvailableRiders(@RequestParam(value = "vehicleType", required = false) String vehicleType) {
        try {
            List<Raider> riders = (vehicleType == null || vehicleType.isBlank())
                    ? raiderRepo.findByIsAvailableTrueOrderByIdDesc()
                    : raiderRepo.findByIsAvailableTrueAndVehicleTypeIgnoreCaseOrderByIdDesc(vehicleType.trim());

            List<RaiderDto> riderDtos = riders.stream().map(this::mapToRaiderDto).collect(Collectors.toList());
            return baseUtils.generateSuccessResponse(riderDtos, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping("signup")
    public BaseResponse registerUser(@Valid @RequestBody RegisterRequestModel requestModel, HttpServletRequest request) {
        final String pass = requestModel.getPassword();
        PasswordPolicyDto pw;

        try {
            pw = passwordPolicyService.getPublicPasswordPolicyById(1L);
        } catch (Exception e) {
            return BaseResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .status(false)
                    .message("Error retrieving password policy")
                    .build();
        }

        if (requestModel.getUsername() == null || requestModel.getUsername().trim().isEmpty()
            || requestModel.getPassword() == null || requestModel.getPassword().trim().isEmpty()
            || requestModel.getPhone() == null || requestModel.getPhone().trim().isEmpty()) {
            return BaseResponse.builder()
                    .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                    .status(false)
                    .message("Username, Password, and Contact Phone Number cannot be empty")
                    .build();
        }

        if (registerService.getUserByUsername(requestModel.getUsername()).isPresent()) {
            return BaseResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .status(false)
                    .message("User already exists")
                    .build();
        }

        if (requestModel.getPassword().length() < pw.getMinLength()) {
            return BaseResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .status(false)
                    .message("Password must be at least " + pw.getMinLength() + " characters long")
                    .build();
        }

        try {
            AppUser riderUser = generateRiderEntity(requestModel, pw);
            String otp = String.format("%06d", (int) (Math.random() * 1000000));
            riderUser.setOtp(otp);
            riderUser.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
            AppUser savedUser = registerService.addUser(riderUser);
            populateRaiderProfile(savedUser, requestModel);

            // Send SMS OTP (dev mode will log and keep in DB)
            if (requestModel.getPhone() != null && !requestModel.getPhone().isBlank()) {
                smsOtpService.sendOtp(requestModel.getPhone().trim(), otp);
            }

            // Notify Admin Portal
            try {
                fcmNotificationService.notifyAdminNewRegistration("Rider", requestModel.getDisplayName(), requestModel.getPhone(), savedUser.getId());
            } catch (Exception e) {
                log.warn("Could not notify admin of new rider registration: {}", e.getMessage());
            }

            // ✅ SECURITY: Do NOT issue JWT token here. The rider must verify phone via OTP first.
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

        } catch (InternalAuthenticationServiceException e) {
            log.error("Internal auth error during rider signup", e);
            return BaseResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .status(false)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Rider registration failed", e);
            return BaseResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .status(false)
                    .message("Invalid username or password")
                    .build();
        }
    }

    private AppUser generateRiderEntity(RegisterRequestModel requestModel, PasswordPolicyDto pw) {
        AppUser appUser = new AppUser();
        appUser.setDisplayName(requestModel.getDisplayName());
        appUser.setUsername(requestModel.getUsername());
        appUser.setPassword(passwordEncoder.encode(requestModel.getPassword()));
        appUser.setPasswordPolicy(generatePasswordPolicyEntity(pw));
        appUser.setEntryUser(0L);
        appUser.setEntryDate(LocalDateTime.now());
        appUser.setAppUserType(AppUserType.RAIDER);
        appUser.setUserTypeId(USER_TYPE_ID_RAIDER);
        appUser.setPhone(requestModel.getPhone() != null ? requestModel.getPhone().trim() : null);
        appUser.setPhoneVerified(false);
        return appUser;
    }

    private PasswordPolicy generatePasswordPolicyEntity(PasswordPolicyDto pw) {
        PasswordPolicy entity = new PasswordPolicy();
        BeanUtils.copyProperties(pw, entity);
        return entity;
    }

    private void populateRaiderProfile(AppUser savedUser, RegisterRequestModel requestModel) {
        if (savedUser == null || !AppUserType.RAIDER.equals(savedUser.getAppUserType())) {
            return;
        }

        raiderRepo.findByAppUserId(savedUser.getId()).ifPresent(raider -> {
            if (requestModel.getDisplayName() != null && !requestModel.getDisplayName().isBlank()) {
                raider.setFullName(requestModel.getDisplayName().trim());
            }
            if (requestModel.getPhone() != null && !requestModel.getPhone().isBlank()) {
                raider.setPhone(requestModel.getPhone().trim());
            }
            if (requestModel.getVehicleType() != null) {
                raider.setVehicleType(requestModel.getVehicleType().trim());
            }
            if (requestModel.getVehicleNumber() != null) {
                raider.setVehicleNumber(requestModel.getVehicleNumber().trim());
            }
            if (requestModel.getComplianceDocuments() != null && !requestModel.getComplianceDocuments().isEmpty()) {
                if (raider.getComplianceDocuments() == null) {
                    raider.setComplianceDocuments(new java.util.HashMap<>());
                }
                raider.getComplianceDocuments().putAll(requestModel.getComplianceDocuments());
            }
            // Admin Approval Gate: Newly registered riders are locked by default
            raider.setIsApproved(false);
            raider.setActive(false);
            raiderRepo.save(raider);
        });
    }

    private RaiderDto mapToRaiderDto(Raider raider) {
        RaiderDto dto = new RaiderDto();
        BeanUtils.copyProperties(raider, dto);
        return dto;
    }
}