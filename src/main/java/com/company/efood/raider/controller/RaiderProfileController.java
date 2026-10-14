package com.company.efood.raider.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.repository.RaiderRepo;
import com.company.efood.sys.entity.Address;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.repository.AddressRepo;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.utils.AuthTokenUtils;
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
@RequestMapping({PRIVET_ENDPOINT + "raider/profile", "/api/private/raider/profile", "/raider/profile"})
@RequiredArgsConstructor
public class RaiderProfileController {

    private final RaiderRepo raiderRepo;
    private final AppUserRepo appUserRepo;
    private final AddressRepo addressRepo;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    @GetMapping({"", "/my"})
    public ResponseEntity<BaseResponse> getRiderProfile(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            if (userId == null) {
                userId = CurrentUserContext.getReferenceId();
            }

            Raider raider = raiderRepo.findByAppUserId(userId).orElse(null);
            if (raider == null) {
                AppUser user = appUserRepo.findById(userId).orElse(null);
                if (user != null) {
                    raider = new Raider(user);
                    raider.setFullName(user.getDisplayName());
                    raider = raiderRepo.save(raider);
                }
            }

            if (raider == null) {
                return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(new IllegalArgumentException("Rider not found")));
            }

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", raider.getId());
            profileData.put("userId", raider.getAppUser() != null ? raider.getAppUser().getId() : userId);
            profileData.put("username", raider.getAppUser() != null ? raider.getAppUser().getUsername() : "");
            profileData.put("fullName", raider.getFullName() != null ? raider.getFullName() : (raider.getAppUser() != null ? raider.getAppUser().getDisplayName() : ""));
            profileData.put("displayName", raider.getFullName() != null ? raider.getFullName() : (raider.getAppUser() != null ? raider.getAppUser().getDisplayName() : ""));
            profileData.put("phone", raider.getPhone() != null ? raider.getPhone() : "");
            profileData.put("phoneVerified", raider.getAppUser() != null ? Boolean.TRUE.equals(raider.getAppUser().getPhoneVerified()) : false);
            profileData.put("nid", raider.getNid() != null ? raider.getNid() : "");
            profileData.put("vehicleType", raider.getVehicleType() != null ? raider.getVehicleType() : "BIKE");
            profileData.put("vehicleNumber", raider.getVehicleNumber() != null ? raider.getVehicleNumber() : "");
            profileData.put("isAvailable", Boolean.TRUE.equals(raider.getIsAvailable()));
            profileData.put("currentLat", raider.getCurrentLat() != null ? raider.getCurrentLat() : 0.0);
            profileData.put("currentLng", raider.getCurrentLng() != null ? raider.getCurrentLng() : 0.0);
            if (raider.getAddress() != null) {
                profileData.put("address", raider.getAddress().getAddress());
                profileData.put("addressId", raider.getAddress().getId());
            }

            return ResponseEntity.ok(baseUtils.generateSuccessResponse(profileData, PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("Error fetching rider profile", e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    @PutMapping({"", "/update"})
    @PostMapping({"", "/update"})
    public ResponseEntity<BaseResponse> updateRiderProfile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            if (userId == null) {
                userId = CurrentUserContext.getReferenceId();
            }

            Raider raider = raiderRepo.findByAppUserId(userId).orElse(null);
            if (raider == null && userId != null) {
                AppUser user = appUserRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
                raider = new Raider(user);
            }

            if (raider == null) {
                return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(new IllegalArgumentException("Rider not found")));
            }

            if (body.containsKey("fullName") && body.get("fullName") != null) {
                String name = body.get("fullName").toString().trim();
                raider.setFullName(name);
                if (raider.getAppUser() != null) {
                    raider.getAppUser().setDisplayName(name);
                    appUserRepo.save(raider.getAppUser());
                }
            }

            if (body.containsKey("phone") && body.get("phone") != null) {
                raider.setPhone(body.get("phone").toString().trim());
            }

            if (body.containsKey("nid") && body.get("nid") != null) {
                raider.setNid(body.get("nid").toString().trim());
            }

            if (body.containsKey("vehicleType") && body.get("vehicleType") != null) {
                raider.setVehicleType(body.get("vehicleType").toString().trim());
            }

            if (body.containsKey("vehicleNumber") && body.get("vehicleNumber") != null) {
                raider.setVehicleNumber(body.get("vehicleNumber").toString().trim());
            }

            if (body.containsKey("isAvailable") && body.get("isAvailable") != null) {
                raider.setIsAvailable(Boolean.parseBoolean(body.get("isAvailable").toString()));
            }

            if (body.containsKey("address") && body.get("address") != null) {
                String addrText = body.get("address").toString().trim();
                Address addr = raider.getAddress();
                if (addr == null) {
                    addr = new Address();
                }
                addr.setAddress(addrText);
                addr = addressRepo.save(addr);
                raider.setAddress(addr);
            }

            Raider saved = raiderRepo.save(raider);
            return ResponseEntity.ok(baseUtils.generateSuccessResponse(saved, "Rider profile updated successfully", "রাইডার প্রোফাইল সফলভাবে আপডেট হয়েছে"));
        } catch (Exception e) {
            log.error("Error updating rider profile", e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }
}
