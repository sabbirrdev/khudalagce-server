package com.company.efood.user.controller;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.sys.utils.AuthTokenUtils;
import com.company.efood.user.dto.CartItemDto;
import com.company.efood.user.dto.CartItemRequestDTO;
import com.company.efood.user.entity.Customer;
import com.company.efood.user.repository.CartRepo;
import com.company.efood.user.repository.CustomerRepo;
import com.company.efood.user.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.company.efood.base.BaseConstants.PRIVET_ENDPOINT;

@AllArgsConstructor
@RestController
@RequestMapping(PRIVET_ENDPOINT + "cart")
public class CartController {
    private final CartService cartService;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;
    private final CartRepo cartRepo;
    private final CustomerRepo customerRepo;

    @PostMapping("/add")
    public BaseResponse addToCart(@Valid @RequestBody CartItemDto body, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(cartService.addToCart(body), "Item added to cart", "আইটেম কার্টে যোগ হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping("/add-direct")
    public BaseResponse addDirectToCart(
            @Valid @RequestBody CartItemRequestDTO body,
            @RequestHeader(value = "X-Latitude", required = false) Double headerLat,
            @RequestHeader(value = "X-Longitude", required = false) Double headerLon,
            HttpServletRequest request
    ) {
        try {
            return baseUtils.generateSuccessResponse(
                    cartService.addItemToCart(body, headerLat, headerLon),
                    "Item added to cart",
                    "আইটেম কার্টে যোগ হয়েছে"
            );
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping("/remove")
    public BaseResponse removeFromCart(@Valid @RequestBody CartItemDto body, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(cartService.removeItem(body), "Item removed from cart", "আইটেম কার্ট থেকে মুছে ফেলা হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping("/my")
    public BaseResponse getMyCart(@Valid @RequestBody BasePageableRequest body, HttpServletRequest request) {
        try {
            return baseUtils.generateSuccessResponse(cartService.getPageableCartItems(body), "Cart loaded", "কার্ট লোড হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @PostMapping(value = "/merge")
    public BaseResponse mergeCart(@RequestParam(value = "cartKey", required = false) String cartKey,
                                  @RequestBody(required = false) Map<String, String> body,
                                  HttpServletRequest request) {
        try {
            String key = (cartKey != null && !cartKey.isBlank())
                    ? cartKey
                    : (body != null ? body.get("cartKey") : null);
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            if (key != null && !key.isBlank()) {
                cartService.mergeGuestCartToUser(key, userId);
            }
            return baseUtils.generateSuccessResponse(null, "Cart merged successfully", "কার্ট একীভূত হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    /** Clears ALL cart items for the currently logged-in customer. Called after a successful order placement. */
    @PostMapping("/clear")
    public BaseResponse clearMyCart(HttpServletRequest request) {
        try {
            Long userId = authTokenUtils.getUserIdFromRequest(request);
            Long refId = CurrentUserContext.getReferenceId();
            Customer customer = null;
            if (refId != null) {
                customer = customerRepo.findById(refId).orElse(null);
            }
            if (customer == null && userId != null) {
                customer = customerRepo.findByAppUserId(userId).orElse(null);
            }
            if (customer != null) {
                cartRepo.deleteByCustomerId(customer.getId());
                System.out.println("🛒 [CART] Cleared cart for customerId=" + customer.getId());
            }
            return baseUtils.generateSuccessResponse(null, "Cart cleared", "কার্ট পরিষ্কার করা হয়েছে");
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }
}
