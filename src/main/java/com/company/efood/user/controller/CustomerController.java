package com.company.efood.user.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.repository.AddressRepo;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.utils.AuthTokenUtils;
import com.company.efood.sys.utils.Gender;
import com.company.efood.user.entity.Customer;
import com.company.efood.user.repository.CustomerRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.company.efood.base.BaseConstants.PRIVET_ENDPOINT;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;

@Slf4j
@RestController
@RequestMapping({PRIVET_ENDPOINT + "customer", "/customer", "/api/private/customer"})
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepo customerRepo;
    private final AppUserRepo appUserRepo;
    private final AddressRepo addressRepo;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    /**
     * GET /api/private/customer/profile
     * Returns the full customer profile from the CUSTOMER entity.
     */
    @GetMapping({"/profile", "/me"})
    public ResponseEntity<BaseResponse> getMyProfile(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            if (userId == null) {
                userId = CurrentUserContext.getReferenceId();
            }

            Customer customer = customerRepo.findByAppUserId(userId).orElse(null);
            if (customer == null && userId != null) {
                // Try finding by AppUser
                AppUser user = appUserRepo.findById(userId).orElse(null);
                if (user != null) {
                    customer = new Customer(user);
                    customer.setFullName(user.getDisplayName());
                    customer.setEmail(user.getUsername());
                    customer = customerRepo.save(customer);
                }
            }

            if (customer == null) {
                return ResponseEntity.badRequest().body(
                        BaseResponse.builder()
                                .status(false)
                                .statusCode(404)
                                .message("Customer profile not found")
                                .build()
                );
            }

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", customer.getId());
            profileData.put("userId", customer.getAppUser() != null ? customer.getAppUser().getId() : userId);
            profileData.put("username", customer.getAppUser() != null ? customer.getAppUser().getUsername() : "");
            profileData.put("fullName", customer.getFullName() != null ? customer.getFullName() : (customer.getAppUser() != null ? customer.getAppUser().getDisplayName() : ""));
            profileData.put("displayName", customer.getFullName() != null ? customer.getFullName() : (customer.getAppUser() != null ? customer.getAppUser().getDisplayName() : ""));
            profileData.put("phone", customer.getPhone() != null ? customer.getPhone() : "");
            profileData.put("phoneVerified", customer.getAppUser() != null ? Boolean.TRUE.equals(customer.getAppUser().getPhoneVerified()) : false);
            profileData.put("email", customer.getEmail() != null ? customer.getEmail() : (customer.getAppUser() != null ? customer.getAppUser().getUsername() : ""));
            profileData.put("nid", customer.getNid() != null ? customer.getNid() : "");
            profileData.put("gender", customer.getGender() != null ? customer.getGender().name() : "");
            profileData.put("birthDate", customer.getBirthDate());
            if (customer.getAddress() != null) {
                profileData.put("address", customer.getAddress().getAddress());
                profileData.put("addressId", customer.getAddress().getId());
                profileData.put("lat", customer.getAddress().getLat());
                profileData.put("lon", customer.getAddress().getLon());
            }

            return ResponseEntity.ok(baseUtils.generateSuccessResponse(profileData, PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("Error fetching customer profile", e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    /**
     * PUT /api/private/customer/profile
     * Updates customer profile fields (fullName, phone, email, nid, gender, address).
     */
    @PutMapping({"/profile", "/update"})
    @PostMapping({"/profile/update", "/profile"})
    public ResponseEntity<BaseResponse> updateProfile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            if (userId == null) {
                userId = CurrentUserContext.getReferenceId();
            }

            Customer customer = customerRepo.findByAppUserId(userId).orElse(null);
            if (customer == null && userId != null) {
                AppUser user = appUserRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
                customer = new Customer(user);
            }

            if (body.containsKey("fullName") && body.get("fullName") != null) {
                String name = body.get("fullName").toString().trim();
                customer.setFullName(name);
                if (customer.getAppUser() != null) {
                    customer.getAppUser().setDisplayName(name);
                    appUserRepo.save(customer.getAppUser());
                }
            }

            if (body.containsKey("phone") && body.get("phone") != null) {
                customer.setPhone(body.get("phone").toString().trim());
            }

            if (body.containsKey("email") && body.get("email") != null) {
                customer.setEmail(body.get("email").toString().trim());
            }

            if (body.containsKey("nid") && body.get("nid") != null) {
                customer.setNid(body.get("nid").toString().trim());
            }

            if (body.containsKey("gender") && body.get("gender") != null) {
                try {
                    customer.setGender(Gender.valueOf(body.get("gender").toString().trim().toUpperCase()));
                } catch (Exception ignored) {}
            }

            if (body.containsKey("address") && body.get("address") != null) {
                String addrText = body.get("address").toString().trim();
                Address addr = customer.getAddress();
                if (addr == null) {
                    addr = new Address();
                }
                addr.setAddress(addrText);
                if (body.containsKey("lat") && body.get("lat") != null) {
                    addr.setLat(Double.parseDouble(body.get("lat").toString()));
                }
                if (body.containsKey("lon") && body.get("lon") != null) {
                    addr.setLon(Double.parseDouble(body.get("lon").toString()));
                }
                addr = addressRepo.save(addr);
                customer.setAddress(addr);
            }

            Customer saved = customerRepo.save(customer);
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", saved.getId());
            profileData.put("userId", saved.getAppUser() != null ? saved.getAppUser().getId() : userId);
            profileData.put("username", saved.getAppUser() != null ? saved.getAppUser().getUsername() : "");
            profileData.put("fullName", saved.getFullName() != null ? saved.getFullName() : "");
            profileData.put("displayName", saved.getFullName() != null ? saved.getFullName() : "");
            profileData.put("phone", saved.getPhone() != null ? saved.getPhone() : "");
            profileData.put("email", saved.getEmail() != null ? saved.getEmail() : "");
            profileData.put("nid", saved.getNid() != null ? saved.getNid() : "");
            profileData.put("gender", saved.getGender() != null ? saved.getGender().name() : "");
            profileData.put("birthDate", saved.getBirthDate());
            if (saved.getAddress() != null) {
                profileData.put("address", saved.getAddress().getAddress());
                profileData.put("addressId", saved.getAddress().getId());
                profileData.put("lat", saved.getAddress().getLat());
                profileData.put("lon", saved.getAddress().getLon());
            }

            return ResponseEntity.ok(baseUtils.generateSuccessResponse(profileData, "Profile updated successfully", "প্রোফাইল সফলভাবে আপডেট হয়েছে"));
        } catch (Exception e) {
            log.error("Error updating customer profile", e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }
}
