package com.company.efood.sys.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.repository.RaiderRepo;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.sys.entity.Product;
import com.company.efood.sys.entity.Shop;
import com.company.efood.sys.repository.ProductRepo;
import com.company.efood.sys.repository.ShopRepo;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;
import static com.company.efood.base.BaseConstants.SYSTEM_ADMIN_END_POINT;

@RestController
@RequestMapping(SYSTEM_ADMIN_END_POINT + "moderation")
@AllArgsConstructor
public class AdminModerationController {

    private final SellerRepo sellerRepo;
    private final RaiderRepo raiderRepo;
    private final ShopRepo shopRepo;
    private final ProductRepo productRepo;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;

    @GetMapping("/pending")
    public BaseResponse getPendingApprovals() {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sellerPendingCount", sellerRepo.countByIsApprovedFalseOrIsApprovedIsNull());
            payload.put("raiderPendingCount", raiderRepo.countByIsApprovedFalseOrIsApprovedIsNull());
            payload.put("shopPendingCount", shopRepo.countByIsApprovedFalseOrIsApprovedIsNull());
            payload.put("productPendingCount", productRepo.countByIsApprovedFalseOrIsApprovedIsNull());
            return baseUtils.generateSuccessResponse(payload, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/pending/sellers")
    public BaseResponse getPendingSellers() {
        try {
            List<Map<String, Object>> sellers = sellerRepo.findByIsApprovedFalseOrIsApprovedIsNull().stream()
                    .map(this::mapSellerToDto)
                    .collect(Collectors.toList());
            return baseUtils.generateSuccessResponse(sellers, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/pending/raiders")
    public BaseResponse getPendingRaiders() {
        try {
            List<Map<String, Object>> raiders = raiderRepo.findByIsApprovedFalseOrIsApprovedIsNull().stream()
                    .map(this::mapRaiderToDto)
                    .collect(Collectors.toList());
            return baseUtils.generateSuccessResponse(raiders, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/pending/shops")
    public BaseResponse getPendingShops() {
        try {
            List<Map<String, Object>> shops = shopRepo.findByIsApprovedFalseOrIsApprovedIsNull().stream()
                    .map(this::mapShopToDto)
                    .collect(Collectors.toList());
            return baseUtils.generateSuccessResponse(shops, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/pending/products")
    public BaseResponse getPendingProducts() {
        try {
            List<Map<String, Object>> products = productRepo.findByIsApprovedFalseOrIsApprovedIsNull().stream()
                    .map(this::mapProductToDto)
                    .collect(Collectors.toList());
            return baseUtils.generateSuccessResponse(products, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/seller/{sellerId}/approve")
    public BaseResponse approveSeller(@PathVariable Long sellerId, HttpServletRequest request) {
        try {
            Seller seller = sellerRepo.findById(sellerId).orElseThrow(() -> new IllegalArgumentException("Seller not found"));
            seller.setIsApproved(true);
            try {
                seller.setApprovedBy(authTokenUtils.getUserIdFromRequest(request));
            } catch (Exception ignored) {}
            seller.setApprovedDate(LocalDateTime.now());
            Seller saved = sellerRepo.save(seller);
            return baseUtils.generateSuccessResponse(mapSellerToDto(saved), "Seller approved", "সেলার অনুমোদিত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/seller/{sellerId}/reject")
    public BaseResponse rejectSeller(@PathVariable Long sellerId) {
        try {
            Seller seller = sellerRepo.findById(sellerId).orElseThrow(() -> new IllegalArgumentException("Seller not found"));
            seller.setActive(false);
            Seller saved = sellerRepo.save(seller);
            return baseUtils.generateSuccessResponse(mapSellerToDto(saved), "Seller rejected", "সেলার প্রত্যাখ্যাত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/raider/{raiderId}/approve")
    public BaseResponse approveRaider(@PathVariable Long raiderId, HttpServletRequest request) {
        try {
            Raider raider = raiderRepo.findById(raiderId).orElseThrow(() -> new IllegalArgumentException("Raider not found"));
            raider.setIsApproved(true);
            try {
                raider.setApprovedBy(authTokenUtils.getUserIdFromRequest(request));
            } catch (Exception ignored) {}
            raider.setApprovedDate(LocalDateTime.now());
            Raider saved = raiderRepo.save(raider);
            return baseUtils.generateSuccessResponse(mapRaiderToDto(saved), "Raider approved", "রাইডার অনুমোদিত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/raider/{raiderId}/reject")
    public BaseResponse rejectRaider(@PathVariable Long raiderId) {
        try {
            Raider raider = raiderRepo.findById(raiderId).orElseThrow(() -> new IllegalArgumentException("Raider not found"));
            raider.setActive(false);
            Raider saved = raiderRepo.save(raider);
            return baseUtils.generateSuccessResponse(mapRaiderToDto(saved), "Raider rejected", "রাইডার প্রত্যাখ্যাত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/shop/{shopId}/approve")
    public BaseResponse approveShop(@PathVariable Long shopId, HttpServletRequest request) {
        try {
            Shop shop = shopRepo.findById(shopId).orElseThrow(() -> new IllegalArgumentException("Shop not found"));
            shop.setIsApproved(true);
            try {
                shop.setApprovedBy(authTokenUtils.getUserIdFromRequest(request));
            } catch (Exception ignored) {}
            shop.setApprovedDate(LocalDateTime.now());
            Shop saved = shopRepo.save(shop);
            return baseUtils.generateSuccessResponse(mapShopToDto(saved), "Shop approved", "শপ অনুমোদিত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/shop/{shopId}/reject")
    public BaseResponse rejectShop(@PathVariable Long shopId) {
        try {
            Shop shop = shopRepo.findById(shopId).orElseThrow(() -> new IllegalArgumentException("Shop not found"));
            shop.setActive(false);
            Shop saved = shopRepo.save(shop);
            return baseUtils.generateSuccessResponse(mapShopToDto(saved), "Shop rejected", "শপ প্রত্যাখ্যাত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/product/{productId}/approve")
    public BaseResponse approveProduct(@PathVariable Long productId, HttpServletRequest request) {
        try {
            Product product = productRepo.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found"));
            product.setIsApproved(true);
            try {
                product.setApprovedBy(authTokenUtils.getUserIdFromRequest(request));
            } catch (Exception ignored) {}
            product.setApprovedDate(LocalDateTime.now());
            Product saved = productRepo.save(product);
            return baseUtils.generateSuccessResponse(mapProductToDto(saved), "Product approved", "প্রোডাক্ট অনুমোদিত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/product/{productId}/reject")
    public BaseResponse rejectProduct(@PathVariable Long productId) {
        try {
            Product product = productRepo.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found"));
            product.setActive(false);
            Product saved = productRepo.save(product);
            return baseUtils.generateSuccessResponse(mapProductToDto(saved), "Product rejected", "প্রোডাক্ট প্রত্যাখ্যাত হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    // ─── Safe DTO Mappers (Eliminate Circular Entity References) ────────────────

    private Map<String, Object> mapSellerToDto(Seller s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("name", s.getFullName() != null && !s.getFullName().isBlank() ? s.getFullName() : (s.getAppUser() != null ? s.getAppUser().getUsername() : "Seller #" + s.getId()));
        map.put("fullName", s.getFullName());
        map.put("phone", s.getPhone());
        map.put("nid", s.getNid());
        map.put("username", s.getAppUser() != null ? s.getAppUser().getUsername() : null);
        map.put("isApproved", Boolean.TRUE.equals(s.getIsApproved()));
        map.put("active", s.getActive());
        map.put("entryDate", s.getEntryDate());
        if (s.getShop() != null) {
            map.put("shopId", s.getShop().getId());
            map.put("shopName", s.getShop().getShopName());
            map.put("shopPhone", s.getShop().getPhoneNumber());
            map.put("shopAddress", s.getShop().getShopAddress());
            map.put("shopType", s.getShop().getShopType());
        }
        return map;
    }

    private Map<String, Object> mapRaiderToDto(Raider r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("name", r.getFullName() != null && !r.getFullName().isBlank() ? r.getFullName() : (r.getAppUser() != null ? r.getAppUser().getUsername() : "Rider #" + r.getId()));
        map.put("fullName", r.getFullName());
        map.put("phone", r.getPhone());
        map.put("nid", r.getNid());
        map.put("vehicleType", r.getVehicleType() != null ? r.getVehicleType() : "BIKE");
        map.put("vehicleNumber", r.getVehicleNumber());
        map.put("username", r.getAppUser() != null ? r.getAppUser().getUsername() : null);
        map.put("isApproved", Boolean.TRUE.equals(r.getIsApproved()));
        map.put("active", r.getActive());
        map.put("entryDate", r.getEntryDate());
        return map;
    }

    private Map<String, Object> mapShopToDto(Shop s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("name", s.getShopName());
        map.put("shopName", s.getShopName());
        map.put("shopNameBn", s.getShopNameBn());
        map.put("phone", s.getPhoneNumber());
        map.put("phoneNumber", s.getPhoneNumber());
        map.put("shopType", s.getShopType() != null ? s.getShopType() : "RESTAURANT");
        map.put("shopAddress", s.getShopAddress());
        map.put("address", s.getShopAddress());
        map.put("about", s.getAbout());
        map.put("logoUrl", s.getLogoUrl());
        map.put("shopLat", s.getShopLat());
        map.put("shopLon", s.getShopLon());
        map.put("isApproved", Boolean.TRUE.equals(s.getIsApproved()));
        map.put("approvedBy", s.getApprovedBy());
        map.put("approvedDate", s.getApprovedDate());
        map.put("active", s.getActive());
        map.put("entryDate", s.getEntryDate());
        // Shop Category + Service (so admin knows which vertical the shop is registering under)
        if (s.getShopCategory() != null) {
            map.put("shopCategoryId", s.getShopCategory().getId());
            map.put("shopCategoryName", s.getShopCategory().getName());
            if (s.getShopCategory().getService() != null) {
                map.put("serviceId", s.getShopCategory().getService().getId());
                map.put("serviceName", s.getShopCategory().getService().getName());
                map.put("serviceCode", s.getShopCategory().getService().getCode());
            }
        }
        if (s.getSeller() != null) {
            map.put("sellerId", s.getSeller().getId());
            map.put("sellerName", s.getSeller().getFullName());
            map.put("sellerPhone", s.getSeller().getPhone());
            if (s.getSeller().getAppUser() != null) {
                map.put("sellerUsername", s.getSeller().getAppUser().getUsername());
                map.put("username", s.getSeller().getAppUser().getUsername());
            }
        }
        if (s.getZone() != null) {
            map.put("zoneId", s.getZone().getId());
            map.put("zoneName", s.getZone().getName());
            if (s.getZone().getUpazila() != null) {
                map.put("upazilaName", s.getZone().getUpazila().getName());
                if (s.getZone().getUpazila().getDistrict() != null) {
                    map.put("districtName", s.getZone().getUpazila().getDistrict().getName());
                }
            }
        }
        return map;
    }

    private Map<String, Object> mapProductToDto(Product p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getProductName());
        map.put("productName", p.getProductName());
        map.put("price", p.getPrice());
        map.put("discountPrice", p.getDiscountPrice());
        map.put("productType", p.getProductType());
        map.put("brand", p.getBrand());
        map.put("imgUrl", p.getImgUrl());
        map.put("isApproved", Boolean.TRUE.equals(p.getIsApproved()));
        map.put("active", p.getActive());
        map.put("entryDate", p.getEntryDate());
        return map;
    }
}
