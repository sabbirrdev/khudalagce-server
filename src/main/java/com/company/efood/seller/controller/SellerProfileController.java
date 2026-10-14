package com.company.efood.seller.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.SellerRepo;
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

import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;
import static com.company.efood.base.BaseConstants.SELLER_END_POINT;

@Slf4j
@RestController
@RequestMapping({SELLER_END_POINT + "profile", "/api/private/seller/profile", "/seller/profile"})
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerRepo sellerRepo;
    private final AppUserRepo appUserRepo;
    private final AddressRepo addressRepo;
    private final com.company.efood.sys.repository.ShopCategoryRepo shopCategoryRepo;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    @GetMapping({"", "/my"})
    public ResponseEntity<BaseResponse> getSellerProfile(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            if (userId == null) {
                userId = CurrentUserContext.getReferenceId();
            }

            Seller seller = sellerRepo.findByAppUserId(userId).orElse(null);
            if (seller == null) {
                AppUser user = appUserRepo.findById(userId).orElse(null);
                if (user != null) {
                    seller = new Seller(user);
                    seller.setFullName(user.getDisplayName());
                    seller = sellerRepo.save(seller);
                }
            }

            if (seller == null) {
                return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(new IllegalArgumentException("Seller not found")));
            }

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", seller.getId());
            profileData.put("userId", seller.getAppUser() != null ? seller.getAppUser().getId() : userId);
            profileData.put("username", seller.getAppUser() != null ? seller.getAppUser().getUsername() : "");
            profileData.put("fullName", seller.getFullName() != null ? seller.getFullName() : (seller.getAppUser() != null ? seller.getAppUser().getDisplayName() : ""));
            profileData.put("displayName", seller.getFullName() != null ? seller.getFullName() : (seller.getAppUser() != null ? seller.getAppUser().getDisplayName() : ""));
            profileData.put("phone", seller.getPhone() != null ? seller.getPhone() : "");
            profileData.put("phoneVerified", seller.getAppUser() != null ? Boolean.TRUE.equals(seller.getAppUser().getPhoneVerified()) : false);
            profileData.put("nid", seller.getNid() != null ? seller.getNid() : "");
            profileData.put("birthDate", seller.getBirthDate());
            if (seller.getAddress() != null) {
                profileData.put("address", seller.getAddress().getAddress());
                profileData.put("addressId", seller.getAddress().getId());
            }

            if (seller.getShopCategory() != null) {
                profileData.put("shopCategoryId", seller.getShopCategory().getId());
                profileData.put("shopCategoryName", seller.getShopCategory().getName());
                if (seller.getShopCategory().getService() != null) {
                    profileData.put("serviceId", seller.getShopCategory().getService().getId());
                    profileData.put("serviceName", seller.getShopCategory().getService().getName());
                    profileData.put("serviceCode", seller.getShopCategory().getService().getCode());
                }
            }

            return ResponseEntity.ok(baseUtils.generateSuccessResponse(profileData, PROCESS_COMPLETE, PROCESS_COMPLETE_BN));
        } catch (Exception e) {
            log.error("Error fetching seller profile", e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }

    @PutMapping({"", "/update"})
    @PostMapping({"", "/update"})
    public ResponseEntity<BaseResponse> updateSellerProfile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            if (userId == null) {
                userId = CurrentUserContext.getReferenceId();
            }

            Seller seller = sellerRepo.findByAppUserId(userId).orElse(null);
            if (seller == null && userId != null) {
                AppUser user = appUserRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
                seller = new Seller(user);
            }

            if (seller == null) {
                return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(new IllegalArgumentException("Seller not found")));
            }

            if (body.containsKey("fullName") && body.get("fullName") != null) {
                String name = body.get("fullName").toString().trim();
                seller.setFullName(name);
                if (seller.getAppUser() != null) {
                    seller.getAppUser().setDisplayName(name);
                    appUserRepo.save(seller.getAppUser());
                }
            }

            if (body.containsKey("phone") && body.get("phone") != null) {
                seller.setPhone(body.get("phone").toString().trim());
            }

            if (body.containsKey("nid") && body.get("nid") != null) {
                seller.setNid(body.get("nid").toString().trim());
            }

            if (body.containsKey("shopCategoryId") && body.get("shopCategoryId") != null) {
                try {
                    Long catId = Long.parseLong(body.get("shopCategoryId").toString());
                    com.company.efood.sys.entity.ShopCategory foundCat = shopCategoryRepo.findById(catId).orElse(null);
                    if (foundCat != null) {
                        seller.setShopCategory(foundCat);
                        if (seller.getShop() != null) {
                            seller.getShop().setShopCategory(foundCat);
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (body.containsKey("address") && body.get("address") != null) {
                String addrText = body.get("address").toString().trim();
                Address addr = seller.getAddress();
                if (addr == null) {
                    addr = new Address();
                }
                addr.setAddress(addrText);
                addr = addressRepo.save(addr);
                seller.setAddress(addr);
            }

            Seller saved = sellerRepo.save(seller);
            return ResponseEntity.ok(baseUtils.generateSuccessResponse(saved, "Seller profile updated successfully", "সেলার প্রোফাইল সফলভাবে আপডেট হয়েছে"));
        } catch (Exception e) {
            log.error("Error updating seller profile", e);
            return ResponseEntity.badRequest().body(baseUtils.generateErrorResponse(e));
        }
    }
}
