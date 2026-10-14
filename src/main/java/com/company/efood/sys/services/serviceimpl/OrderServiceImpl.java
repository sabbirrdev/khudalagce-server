package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.config.CurrentUserContext;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.repository.RaiderRepo;
import com.company.efood.seller.repository.BranchRepo;
import com.company.efood.sys.dto.OrderDto;
import com.company.efood.sys.dto.OrderItemDto;
import com.company.efood.sys.dto.PaymentDto;
import com.company.efood.sys.dto.ProductDto;
import com.company.efood.sys.dto.AddressDto;
import com.company.efood.sys.entity.*;
import com.company.efood.sys.event.OrderStatusChangedEvent;
import com.company.efood.sys.repository.*;
import com.company.efood.sys.services.OrderService;
import com.company.efood.sys.utils.AddressType;
import com.company.efood.sys.utils.OrderStatus;
import com.company.efood.user.entity.Customer;
import com.company.efood.user.repository.CartRepo;
import com.company.efood.user.repository.CustomerRepo;
import com.company.efood.user.repository.OrderItemRepo;
import com.company.efood.user.services.PaymentService;
import com.company.efood.sys.utils.PaymentStatus;
import com.company.efood.websocket.service.LiveNotificationService;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.company.efood.sys.utils.AppUserType;


@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepo orderRepository;
    private final CustomerRepo customerRepo;
    private final AppUserRepo appUserRepo;
    private final BaseUtils baseUtils;
    private final OrderRepo orderRepo;
    private final PaymentService paymentService;
    private final ModelMapper modelMapper;
    private final BranchRepo branchRepo;
    private final AddressRepo addressRepo;
    private final RaiderRepo raiderRepo;
    private final ProductRepo productRepo;
    private final PaymentRepo paymentRepo;
    private final OrderItemRepo orderItemRepo;
    private final CartRepo cartRepo;
    private final LiveNotificationService liveNotificationService;
    private final com.company.efood.notification.service.FcmNotificationService fcmNotificationService;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    private final com.company.efood.user.services.LoyaltyService loyaltyService;
    private final com.company.efood.raider.repository.RaiderLiveLocationRepo raiderLiveLocationRepo;
    private final com.company.efood.sys.services.DeliveryFeeCalculationService deliveryFeeCalculationService;
    private final com.company.efood.zone.repository.ZoneRepository zoneRepository;
    private final com.company.efood.sys.services.OrderFinancialLedgerService orderFinancialLedgerService;
    private final com.company.efood.sys.services.RiderPayoutPolicyService riderPayoutPolicyService;


    @Transactional(readOnly = true)
    @Override
    public Page<OrderDto> getCustomerOrders(BasePageableRequest basePageableRequest) {
        Long customerId = CurrentUserContext.getReferenceId();
        return getCustomerOrders(customerId, basePageableRequest);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderDto> getCustomerOrders(Long customerUserId, BasePageableRequest basePageableRequest) {
        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());
        if (customerUserId == null) {
            customerUserId = CurrentUserContext.getReferenceId();
        }
        if (customerUserId == null) {
            return Page.empty(pageRequest);
        }

        Page<Order> orderPage = orderRepository.findByCustomerAppUserId(customerUserId, pageRequest);
        return new PageImpl<>(convertEntityListToDtoList(orderPage.stream()), pageRequest, orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderDto> getBranchOrders(BasePageableRequest basePageableRequest) {
        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());
        Page<Order> orderPage = orderRepository.findByBranchId(basePageableRequest.getIntParam1(), pageRequest);
        return new PageImpl<>(convertEntityListToDtoList(orderPage.stream()), pageRequest, orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderDto> getSellerOrders(Long userId, BasePageableRequest basePageableRequest) {
        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());

        Page<Order> orderPage;
        if (basePageableRequest.getIntParam1() != null) {
            orderPage = orderRepository.findByBranchId(basePageableRequest.getIntParam1(), pageRequest);
        } else {
            orderPage = orderRepository.findBySellerAppUserId(userId, pageRequest);
        }

        return new PageImpl<>(convertEntityListToDtoList(orderPage.stream()), pageRequest, orderPage.getTotalElements());
    }

    @Transactional
    @Override
    public OrderDto updateSellerOrderStatus(Long orderId, String status, Long sellerUserId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }

        OrderStatus prevStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdateUser(sellerUserId);
        Order updatedOrder = orderRepo.save(order);

        applicationEventPublisher.publishEvent(new OrderStatusChangedEvent(
                this, updatedOrder.getId(), prevStatus, newStatus, sellerUserId));

        if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.COMPLETED) {
            try {
                orderFinancialLedgerService.recordOrderCompletion(updatedOrder);
            } catch (Exception e) {
                System.err.println("⚠️ [ORDER SERVICE] Failed to record financial ledger: " + e.getMessage());
            }
        }

        String statusMsg = switch (newStatus) {
            case ACCEPTED -> "Order accepted by restaurant/shop";
            case PREPARING -> "Order is being prepared";
            case ASSIGNED -> "Order is ready for pickup";
            case PICKED_UP -> "Order picked up by rider";
            case DELIVERED, COMPLETED -> "Order delivered successfully";
            case REJECTED, CANCELLED -> "Order was rejected by restaurant/shop";
            default -> "Order status updated";
        };

        liveNotificationService.sendOrderStatusUpdate(updatedOrder.getId(), newStatus.name(), statusMsg);

        try {
            String shopName = (order.getBranch() != null && order.getBranch().getShop() != null) ? order.getBranch().getShop().getShopName() : "Shop";
            String address = (order.getDeliveryAddress() != null) ? order.getDeliveryAddress().getAddress() : "";

            if (newStatus == OrderStatus.ASSIGNED) {
                // Order ready for pickup — broadcast to all available riders
                fcmNotificationService.notifyRidersOrderReadyForPickup(updatedOrder.getId(), shopName, address);
            }

            // Only notify customer on ACCEPTED or CANCELLED
            if ((newStatus == OrderStatus.ACCEPTED || newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REJECTED)
                    && order.getCustomer() != null && order.getCustomer().getAppUser() != null) {
                fcmNotificationService.notifyCustomerOrderStatus(order.getCustomer().getAppUser().getId(), updatedOrder.getId(), newStatus.name(), statusMsg);
            }
        } catch (Exception e) {
            System.err.println("Failed to dispatch push notification: " + e.getMessage());
        }

        return generateDto(updatedOrder);
    }

    @Transactional
    @Override
    public Long placeOrder(OrderDto orderDto, Long userId) {
        System.out.println("==========================================================");
        System.out.println("🛒 [ORDER SERVICE] placeOrder() called by userId=" + userId);
        System.out.println("📦 [ORDER SERVICE] OrderDto deliveryAddress=" + orderDto.getDeliveryAddress());
        System.out.println("📦 [ORDER SERVICE] OrderDto branchId=" + orderDto.getBranchId());
        System.out.println("📦 [ORDER SERVICE] OrderDto items count=" + (orderDto.getItems() != null ? orderDto.getItems().size() : 0));

        Long refCustomerId = CurrentUserContext.getReferenceId();
        Customer customer = null;
        if (refCustomerId != null) {
            customer = customerRepo.findById(refCustomerId).orElse(null);
        }
        if (customer == null && userId != null) {
            customer = customerRepo.findByAppUserId(userId).orElse(null);
        }
        if (customer == null && CurrentUserContext.getUserId() != null) {
            customer = customerRepo.findByAppUserId(CurrentUserContext.getUserId()).orElse(null);
        }
        if (customer == null && (userId != null || CurrentUserContext.getUserId() != null)) {
            Long uid = userId != null ? userId : CurrentUserContext.getUserId();
            AppUser appUser = appUserRepo.findById(uid).orElse(null);
            if (appUser != null) {
                Customer newCust = new Customer(appUser);
                newCust.setFullName(appUser.getDisplayName() != null ? appUser.getDisplayName() : appUser.getUsername());
                customer = customerRepo.save(newCust);
                System.out.println("👤 [ORDER SERVICE] Auto-created Customer profile id=" + customer.getId() + " for appUser=" + uid);
            }
        }
        if (customer == null) {
            System.out.println("❌ [ORDER SERVICE] Customer not found for referenceId=" + refCustomerId + " or userId=" + userId);
            throw new IllegalArgumentException("Customer context not found");
        }
        final Long customerId = customer.getId();
        System.out.println("👤 [ORDER SERVICE] Customer resolved: id=" + customer.getId());

        Address address = null;
        if (orderDto.getDeliveryAddress() != null) {
            System.out.println("📍 [ORDER SERVICE] DeliveryAddress received: id=" + orderDto.getDeliveryAddress().getId()
                    + " | type=" + orderDto.getDeliveryAddress().getAddressType()
                    + " | address=" + orderDto.getDeliveryAddress().getAddress()
                    + " | district=" + orderDto.getDeliveryAddress().getDistrict()
                    + " | lat=" + orderDto.getDeliveryAddress().getLat()
                    + " | lon=" + orderDto.getDeliveryAddress().getLon());

            if (orderDto.getDeliveryAddress().getId() != null) {
                System.out.println("🔍 [ORDER SERVICE] Looking up existing address by id=" + orderDto.getDeliveryAddress().getId());
                address = addressRepo.findById(orderDto.getDeliveryAddress().getId()).orElse(null);
                System.out.println("🔍 [ORDER SERVICE] Existing address lookup result: " + (address != null ? "FOUND id=" + address.getId() : "NOT FOUND"));
            }

            if (address == null) {
                // Enterprise reuse: Check if user already has an active address matching text & type
                final String targetText = orderDto.getDeliveryAddress().getAddress() != null
                        ? orderDto.getDeliveryAddress().getAddress().trim() : "";
                final AddressType targetType = orderDto.getDeliveryAddress().getAddressType() != null
                        ? orderDto.getDeliveryAddress().getAddressType() : AddressType.HOME_ADDRESS;

                if (!targetText.isEmpty()) {
                    List<Address> userAddrs = addressRepo.findByEntryUserAndActiveTrue(userId);
                    address = userAddrs.stream()
                            .filter(a -> a.getAddress() != null && a.getAddress().trim().equalsIgnoreCase(targetText)
                                    && (a.getAddressType() == targetType))
                            .findFirst()
                            .orElse(null);
                    if (address != null) {
                        System.out.println("♻️ [ORDER SERVICE] Reusing existing user address id=" + address.getId() + " instead of creating duplicate");
                    }
                }
            }

            if (address == null) {
                System.out.println("➕ [ORDER SERVICE] Creating new address entity...");
                Address newAddress = new Address();
                newAddress.setAddress(orderDto.getDeliveryAddress().getAddress() != null
                        ? orderDto.getDeliveryAddress().getAddress() : "Default Address");
                newAddress.setDistrict(orderDto.getDeliveryAddress().getDistrict() != null
                        ? orderDto.getDeliveryAddress().getDistrict() : "N/A");
                newAddress.setPoliceStation(orderDto.getDeliveryAddress().getPoliceStation() != null
                        ? orderDto.getDeliveryAddress().getPoliceStation() : "N/A");
                newAddress.setAddressType(orderDto.getDeliveryAddress().getAddressType() != null
                        ? orderDto.getDeliveryAddress().getAddressType() : AddressType.HOME_ADDRESS);
                // These comparisons are now valid because AddressDto.lat/lon are Double (wrapper, not primitive)
                if (orderDto.getDeliveryAddress().getLat() != null) {
                    newAddress.setLat(orderDto.getDeliveryAddress().getLat());
                    System.out.println("📍 [ORDER SERVICE] lat set to: " + orderDto.getDeliveryAddress().getLat());
                } else {
                    System.out.println("⚠️ [ORDER SERVICE] lat is null — defaulting to 0.0");
                }
                if (orderDto.getDeliveryAddress().getLon() != null) {
                    newAddress.setLon(orderDto.getDeliveryAddress().getLon());
                    System.out.println("📍 [ORDER SERVICE] lon set to: " + orderDto.getDeliveryAddress().getLon());
                } else {
                    System.out.println("⚠️ [ORDER SERVICE] lon is null — defaulting to 0.0");
                }
                newAddress.setEntryUser(userId);
                baseUtils.setEntryUserInfo(newAddress);
                address = addressRepo.save(newAddress);
                System.out.println("✅ [ORDER SERVICE] New address saved with id=" + address.getId());
            }
        } else {
            System.out.println("❌ [ORDER SERVICE] deliveryAddress is NULL in OrderDto — delivery address is required!");
            throw new IllegalArgumentException("Delivery address is required");
        }

        if (orderDto.getBranchId() == null) {
            System.out.println("❌ [ORDER SERVICE] branchId is NULL");
            throw new IllegalArgumentException("Branch ID is required");
        }

        if (orderDto.getItems() == null || orderDto.getItems().isEmpty()) {
            System.out.println("❌ [ORDER SERVICE] items list is empty/null");
            throw new IllegalArgumentException("At least one order item is required for checkout");
        }

        // Check if items span multiple branches — use item.branchId first, then product.branchId
        java.util.Set<Long> uniqueBranches = new java.util.HashSet<>();
        if (orderDto.getItems() != null) {
            for (OrderItemDto item : orderDto.getItems()) {
                Long itemBranchId = item.getBranchId();
                if (itemBranchId == null && item.getProduct() != null) {
                    itemBranchId = item.getProduct().getBranchId();
                }
                if (itemBranchId != null) {
                    uniqueBranches.add(itemBranchId);
                }
            }
        }

        // If items span multiple branches, delegate to atomic multi-order placement
        if (uniqueBranches.size() > 1) {
            System.out.println("🔀 [ORDER SERVICE] Multi-branch cart detected (" + uniqueBranches.size() + " branches). Placing multi-vendor order atomically...");
            java.util.List<Long> multiOrderIds = placeMultiOrder(orderDto, userId);
            return multiOrderIds.get(0);
        }

        Branch branch = branchRepo.findById(orderDto.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + orderDto.getBranchId()));
        System.out.println("🏬 [ORDER SERVICE] Branch found: id=" + branch.getId());

        List<OrderItem> orderItems = orderDto.getItems().stream().map(item -> mapOrderItem(item, userId)).collect(Collectors.toList());
        BigDecimal itemTotal = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("🧾 [ORDER SERVICE] Order items mapped: count=" + orderItems.size() + " | itemTotal=" + itemTotal);

        int totalWeightGrams = calculateTotalWeightGrams(orderItems);
        com.company.efood.sys.dto.DeliveryFeeResult feeResult = deliveryFeeCalculationService.calculate(branch.getId(), address.getId(), totalWeightGrams);
        if (!feeResult.isDeliverable()) {
            System.out.println("❌ [ORDER SERVICE] Order not deliverable: " + feeResult.getReason());
            throw new IllegalArgumentException(feeResult.getReason() != null ? feeResult.getReason() : "Order cannot be delivered to this location");
        }
        BigDecimal deliveryFee = feeResult.getFee();
        System.out.println("🚚 [ORDER SERVICE] Dynamic delivery fee applied: " + deliveryFee + " (basis=" + feeResult.getBasis() + ", distance=" + feeResult.getDistanceKm() + "km, weight=" + totalWeightGrams + "g)");
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = orderDto.getTax() != null ? orderDto.getTax() : BigDecimal.ZERO;

        // Mutual exclusivity rule: customer can be discounted by loyalty points OR coupon, only ONE applicable at a time
        if (orderDto.getLoyaltyPointsToRedeem() != null && orderDto.getLoyaltyPointsToRedeem() > 0) {
            // Loyalty points take precedence, ignore coupon
            orderDto.setCouponId(null);
            BigDecimal preLoyaltyTotal = itemTotal.add(deliveryFee).add(tax);
            BigDecimal loyaltyDiscount = loyaltyService.redeemPointsForOrder(
                    customer.getId(),
                    null,
                    orderDto.getLoyaltyPointsToRedeem(),
                    preLoyaltyTotal
            );
            discount = loyaltyDiscount;
            System.out.println("🎁 [ORDER SERVICE] Applied loyalty discount: " + loyaltyDiscount + " using " + orderDto.getLoyaltyPointsToRedeem() + " points");
        } else if (orderDto.getDiscount() != null && orderDto.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discount = orderDto.getDiscount();
        }

        BigDecimal grandTotal = itemTotal.add(deliveryFee).subtract(discount).add(tax);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setDeliveryAddress(address);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(orderDto.getTotalAmount() != null && orderDto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 ? orderDto.getTotalAmount() : grandTotal);
        order.setDiscount(discount);
        order.setTax(tax);
        order.setDeliveryFee(deliveryFee);
        if (feeResult.getZoneId() != null) {
            zoneRepository.findById(feeResult.getZoneId()).ifPresent(order::setZone);
        }
        order.setEntryUser(userId);
        order.setActive(true);
        baseUtils.setEntryUserInfo(order);

        System.out.println("💾 [ORDER SERVICE] Saving order entity...");
        Order savedOrder = orderRepo.save(order);
        System.out.println("✅ [ORDER SERVICE] Order saved with id=" + savedOrder.getId());

        orderItems.forEach(item -> item.setOrder(savedOrder));
        savedOrder.setOrderItemList(orderItems);
        orderItemRepo.saveAll(orderItems);
        System.out.println("✅ [ORDER SERVICE] Order items saved.");

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(savedOrder.getId());
        paymentDto.setPaymentMethod(orderDto.getPaymentMethod());
        System.out.println("💳 [ORDER SERVICE] Initiating payment: method=" + orderDto.getPaymentMethod());
        paymentService.initiatePayment(paymentDto, userId);

        if (customer != null) {
            try {
                cartRepo.deleteByCustomerId(customer.getId());
                System.out.println("🛒 [ORDER SERVICE] Cleared cart items from DB for customerId=" + customer.getId());
            } catch (Exception e) {
                System.out.println("⚠️ [ORDER SERVICE] Failed to clear cart items: " + e.getMessage());
            }
        }

        liveNotificationService.sendOrderStatusUpdate(savedOrder.getId(), OrderStatus.PLACED.name(), "Order placed successfully");

        // 🔔 Dispatch Push + In-App Notification to Seller & Customer
        try {
            Branch b = savedOrder.getBranch();
            if (b != null && b.getShop() != null && b.getShop().getSeller() != null) {
                com.company.efood.seller.entity.Seller seller = b.getShop().getSeller();
                if (seller.getAppUser() != null) {
                    String customerName = (customer != null && customer.getFullName() != null && !customer.getFullName().isBlank())
                            ? customer.getFullName()
                            : "Customer";
                    fcmNotificationService.notifySellerNewOrder(
                            seller.getAppUser().getId(),
                            savedOrder.getId(),
                            customerName,
                            savedOrder.getTotalAmount()
                    );
                    System.out.println("📢 [ORDER SERVICE] Notified seller userId=" + seller.getAppUser().getId() + " for order #" + savedOrder.getId());
                }
            }
            if (customer != null && customer.getAppUser() != null) {
                fcmNotificationService.notifyCustomerOrderStatus(
                        customer.getAppUser().getId(),
                        savedOrder.getId(),
                        "PLACED",
                        "Your order #" + savedOrder.getId() + " has been placed successfully."
                );
            }
        } catch (Exception ex) {
            System.err.println("⚠️ [ORDER SERVICE] Failed to dispatch order placement notification: " + ex.getMessage());
        }

        System.out.println("🎉 [ORDER SERVICE] Order placement COMPLETE. Order ID=" + savedOrder.getId());
        System.out.println("==========================================================");
        return savedOrder.getId();
    }

    @Transactional
    @Override
    public java.util.List<Long> placeMultiOrder(OrderDto orderDto, Long userId) {
        if (orderDto.getItems() == null || orderDto.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required for checkout");
        }

        Long refCustomerId = CurrentUserContext.getReferenceId();
        Customer customer = null;
        if (refCustomerId != null) {
            customer = customerRepo.findById(refCustomerId).orElse(null);
        }
        if (customer == null && userId != null) {
            customer = customerRepo.findByAppUserId(userId).orElse(null);
        }
        if (customer == null && CurrentUserContext.getUserId() != null) {
            customer = customerRepo.findByAppUserId(CurrentUserContext.getUserId()).orElse(null);
        }
        if (customer == null && (userId != null || CurrentUserContext.getUserId() != null)) {
            Long uid = userId != null ? userId : CurrentUserContext.getUserId();
            AppUser appUser = appUserRepo.findById(uid).orElse(null);
            if (appUser != null) {
                Customer newCust = new Customer(appUser);
                newCust.setFullName(appUser.getDisplayName() != null ? appUser.getDisplayName() : appUser.getUsername());
                customer = customerRepo.save(newCust);
            }
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer context not found");
        }

        Address address = null;
        if (orderDto.getDeliveryAddress() != null) {
            if (orderDto.getDeliveryAddress().getId() != null) {
                address = addressRepo.findById(orderDto.getDeliveryAddress().getId()).orElse(null);
            }
            if (address == null) {
                // Enterprise reuse: Check if user already has an active address matching text & type
                final String targetText = orderDto.getDeliveryAddress().getAddress() != null
                        ? orderDto.getDeliveryAddress().getAddress().trim() : "";
                final AddressType targetType = orderDto.getDeliveryAddress().getAddressType() != null
                        ? orderDto.getDeliveryAddress().getAddressType() : AddressType.HOME_ADDRESS;

                if (!targetText.isEmpty()) {
                    List<Address> userAddrs = addressRepo.findByEntryUserAndActiveTrue(userId);
                    address = userAddrs.stream()
                            .filter(a -> a.getAddress() != null && a.getAddress().trim().equalsIgnoreCase(targetText)
                                    && (a.getAddressType() == targetType))
                            .findFirst()
                            .orElse(null);
                    if (address != null) {
                        System.out.println("♻️ [ORDER SERVICE] Reusing existing user address id=" + address.getId() + " in placeMultiOrder");
                    }
                }
            }
            if (address == null) {
                Address newAddress = new Address();
                AddressDto addrDto = orderDto.getDeliveryAddress();
                newAddress.setAddress(addrDto.getAddress() != null ? addrDto.getAddress() : "");
                newAddress.setDistrict(addrDto.getDistrict() != null ? addrDto.getDistrict() : "N/A");
                newAddress.setPoliceStation(addrDto.getPoliceStation() != null ? addrDto.getPoliceStation() : "N/A");
                newAddress.setAddressType(addrDto.getAddressType() != null ? addrDto.getAddressType() : AddressType.HOME_ADDRESS);
                if (addrDto.getLat() != null) newAddress.setLat(addrDto.getLat());
                if (addrDto.getLon() != null) newAddress.setLon(addrDto.getLon());
                newAddress.setEntryUser(userId);
                baseUtils.setEntryUserInfo(newAddress);
                address = addressRepo.save(newAddress);
            }
        }
        if (address == null) {
            throw new IllegalArgumentException("Delivery address is required");
        }

        // Group items by branchId — use OrderItemDto.branchId first, then product.branchId
        java.util.Map<Long, java.util.List<OrderItemDto>> itemsByBranch = new java.util.LinkedHashMap<>();
        for (OrderItemDto item : orderDto.getItems()) {
            Long branchId = item.getBranchId();
            if (branchId == null && item.getProduct() != null) {
                branchId = item.getProduct().getBranchId();
            }
            if (branchId == null) {
                branchId = orderDto.getBranchId();
            }
            if (branchId == null) {
                throw new IllegalArgumentException("Cannot determine branch for item: product=" +
                        (item.getProduct() != null ? item.getProduct().getId() : "null"));
            }
            itemsByBranch.computeIfAbsent(branchId, k -> new java.util.ArrayList<>()).add(item);
        }

        int branchCount = itemsByBranch.size();
        BigDecimal totalLoyaltyDiscount = BigDecimal.ZERO;
        if (orderDto.getLoyaltyPointsToRedeem() != null && orderDto.getLoyaltyPointsToRedeem() > 0) {
            BigDecimal estimatedTotal = orderDto.getTotalAmount() != null ? orderDto.getTotalAmount() : BigDecimal.valueOf(100);
            totalLoyaltyDiscount = loyaltyService.redeemPointsForOrder(
                    customer.getId(),
                    null,
                    orderDto.getLoyaltyPointsToRedeem(),
                    estimatedTotal
            );
        }

        BigDecimal discountPerBranch = branchCount > 0
                ? totalLoyaltyDiscount.divide(BigDecimal.valueOf(branchCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        java.util.List<Long> createdOrderIds = new java.util.ArrayList<>();

        for (java.util.Map.Entry<Long, java.util.List<OrderItemDto>> entry : itemsByBranch.entrySet()) {
            Long branchId = entry.getKey();
            java.util.List<OrderItemDto> branchItems = entry.getValue();

            Branch branch = branchRepo.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

            List<OrderItem> orderItems = branchItems.stream().map(item -> mapOrderItem(item, userId)).collect(Collectors.toList());
            BigDecimal itemTotal = orderItems.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int branchTotalWeightGrams = calculateTotalWeightGrams(orderItems);
            com.company.efood.sys.dto.DeliveryFeeResult branchFeeResult = deliveryFeeCalculationService.calculate(branch.getId(), address.getId(), branchTotalWeightGrams);
            if (!branchFeeResult.isDeliverable()) {
                System.out.println("❌ [ORDER SERVICE] Multi-order branch #" + branch.getId() + " not deliverable: " + branchFeeResult.getReason());
                throw new IllegalArgumentException(branchFeeResult.getReason() != null ? branchFeeResult.getReason() : "Delivery not possible for branch " + branch.getName());
            }
            BigDecimal deliveryFee = branchFeeResult.getFee();
            System.out.println("🚚 [ORDER SERVICE] Dynamic delivery fee for multi-order branch #" + branch.getId() + ": " + deliveryFee + " (weight=" + branchTotalWeightGrams + "g)");
            BigDecimal discount = discountPerBranch;
            BigDecimal tax = BigDecimal.ZERO;
            BigDecimal grandTotal = itemTotal.add(deliveryFee).subtract(discount).add(tax);
            if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;

            Order branchOrder = new Order();
            branchOrder.setCustomer(customer);
            branchOrder.setBranch(branch);
            branchOrder.setDeliveryAddress(address);
            branchOrder.setStatus(OrderStatus.PLACED);
            branchOrder.setTotalAmount(grandTotal);
            branchOrder.setDiscount(discount);
            branchOrder.setTax(tax);
            branchOrder.setDeliveryFee(deliveryFee);
            if (branchFeeResult.getZoneId() != null) {
                zoneRepository.findById(branchFeeResult.getZoneId()).ifPresent(branchOrder::setZone);
            }
            branchOrder.setEntryUser(userId);
            branchOrder.setActive(true);
            baseUtils.setEntryUserInfo(branchOrder);

            Order savedBranchOrder = orderRepo.save(branchOrder);
            orderItems.forEach(item -> item.setOrder(savedBranchOrder));
            savedBranchOrder.setOrderItemList(orderItems);
            orderItemRepo.saveAll(orderItems);

            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setOrderId(savedBranchOrder.getId());
            paymentDto.setPaymentMethod(orderDto.getPaymentMethod());
            paymentService.initiatePayment(paymentDto, userId);

            liveNotificationService.sendOrderStatusUpdate(savedBranchOrder.getId(), OrderStatus.PLACED.name(), "Order placed for branch " + branch.getName());

            // 🔔 Dispatch Push + In-App Notification to Seller & Customer
            try {
                if (branch != null && branch.getShop() != null && branch.getShop().getSeller() != null) {
                    com.company.efood.seller.entity.Seller seller = branch.getShop().getSeller();
                    if (seller.getAppUser() != null) {
                        String customerName = (customer != null && customer.getFullName() != null && !customer.getFullName().isBlank())
                                ? customer.getFullName()
                                : "Customer";
                        fcmNotificationService.notifySellerNewOrder(
                                seller.getAppUser().getId(),
                                savedBranchOrder.getId(),
                                customerName,
                                savedBranchOrder.getTotalAmount()
                        );
                    }
                }
                if (customer != null && customer.getAppUser() != null) {
                    fcmNotificationService.notifyCustomerOrderStatus(
                            customer.getAppUser().getId(),
                            savedBranchOrder.getId(),
                            "PLACED",
                            "Your order #" + savedBranchOrder.getId() + " has been placed successfully."
                    );
                }
            } catch (Exception ex) {
                System.err.println("⚠️ [ORDER SERVICE] Failed to notify on multi-order branch placement: " + ex.getMessage());
            }

            createdOrderIds.add(savedBranchOrder.getId());
        }

        // Clear cart once all branch orders succeed
        try {
            cartRepo.deleteByCustomerId(customer.getId());
        } catch (Exception e) {
            System.out.println("⚠️ [ORDER SERVICE] Failed to clear cart: " + e.getMessage());
        }

        return createdOrderIds;
    }

    @Transactional
    @Override
    public OrderDto assignDeliveryPartner(OrderDto orderDto, Long userId) {
        if (orderDto == null || orderDto.getId() == null) {
            throw new IllegalArgumentException("Order ID is required to assign a delivery partner");
        }

        Order order = orderRepo.findById(orderDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderDto.getId()));

        Raider raider = resolveRaider(orderDto);
        if (raider == null) {
            throw new IllegalArgumentException("No available delivery partner found for the requested vehicle type");
        }

        order.setRaider(raider);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setUpdateUser(userId);
        baseUtils.setUpdateUserInfo(order, orderRepo.findById(order.getId()).orElse(order));
        Order updatedOrder = orderRepo.save(order);
        liveNotificationService.sendRiderAssignment(updatedOrder.getId(), raider.getId(), raider.getVehicleType());
        liveNotificationService.sendOrderStatusUpdate(updatedOrder.getId(), OrderStatus.ASSIGNED.name(), "Rider assigned to the order");
        return generateDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto cancelOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required for cancellation");
        }

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("The order cannot be cancelled in its current state");
        }

        if (order.getOrderItemList() != null) {
            for (OrderItem item : order.getOrderItemList()) {
                Product product = item.getProduct();
                if (product == null) {
                    continue;
                }
                Integer currentQty = product.getQty() != null ? product.getQty() : 0;
                product.setQty(currentQty + item.getQuantity());
                productRepo.save(product);
            }
        }

        Payment payment = paymentRepo.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setEntryUser(userId);
            paymentRepo.save(payment);
        }

        OrderStatus prevCancelStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdateUser(userId);
        baseUtils.setUpdateUserInfo(order, orderRepo.findById(order.getId()).orElse(order));
        Order updatedOrder = orderRepo.save(order);
        applicationEventPublisher.publishEvent(new com.company.efood.sys.event.OrderStatusChangedEvent(
                this, updatedOrder.getId(), prevCancelStatus, OrderStatus.CANCELLED, userId));
        liveNotificationService.sendOrderStatusUpdate(updatedOrder.getId(), OrderStatus.CANCELLED.name(), "Order cancelled successfully");
        return generateDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto updateOrderStatus(Long orderId, String status, Long riderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // Security / Assignment resolution
        Raider currentRider = raiderRepo.findByAppUserId(riderId)
                .orElseGet(() -> raiderRepo.findById(riderId).orElse(null));

        if (order.getRaider() == null) {
            // No rider assigned yet — auto-assign this rider
            if (currentRider != null) {
                order.setRaider(currentRider);
            }
        } else {
            // Check if this rider is the assigned one (compare by Raider entity ID and by AppUser ID)
            Long orderRaiderId = order.getRaider().getId();
            Long orderRaiderAppUserId = (order.getRaider().getAppUser() != null)
                    ? order.getRaider().getAppUser().getId() : null;

            boolean isAssigned = false;
            if (currentRider != null && Objects.equals(orderRaiderId, currentRider.getId())) {
                isAssigned = true;
            } else if (Objects.equals(orderRaiderAppUserId, riderId)) {
                isAssigned = true;
            } else if (Objects.equals(orderRaiderId, riderId)) {
                // fallback: maybe riderId == Raider entity ID (old behavior)
                isAssigned = true;
            }

            if (!isAssigned) {
                throw new IllegalArgumentException("You are not assigned to this order (orderId=" + orderId
                        + ", assignedRaiderId=" + orderRaiderId
                        + ", assignedRaiderUserId=" + orderRaiderAppUserId
                        + ", requestRiderId=" + riderId + ")");
            }
        }

        OrderStatus newStatus;
        if ("PICKED".equalsIgnoreCase(status) || "PICKED_UP".equalsIgnoreCase(status)) {
            newStatus = OrderStatus.ON_THE_WAY;
        } else {
            try {
                newStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status value: " + status);
            }
        }

        // Allow valid sequential rider transitions
        OrderStatus currentStatus = order.getStatus();
        boolean validTransition =
                (currentStatus == OrderStatus.ASSIGNED && (newStatus == OrderStatus.PICKED_UP || newStatus == OrderStatus.ON_THE_WAY)) ||
                (currentStatus == OrderStatus.PICKED_UP && (newStatus == OrderStatus.ON_THE_WAY || newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.COMPLETED)) ||
                (currentStatus == OrderStatus.ON_THE_WAY && (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.COMPLETED)) ||
                (currentStatus == OrderStatus.DELIVERED && newStatus == OrderStatus.COMPLETED);

        if (!validTransition) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + newStatus
                    + ". Valid transitions: ASSIGNED → ON_THE_WAY / PICKED_UP, ON_THE_WAY → DELIVERED / COMPLETED");
        }

        OrderStatus prevStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdateUser(riderId);
        Order updatedOrder = orderRepo.save(order);

        applicationEventPublisher.publishEvent(new com.company.efood.sys.event.OrderStatusChangedEvent(
                this, updatedOrder.getId(), prevStatus, newStatus, riderId));

        if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.COMPLETED) {
            try {
                orderFinancialLedgerService.recordOrderCompletion(updatedOrder);
            } catch (Exception e) {
                System.err.println("⚠️ [ORDER SERVICE] Failed to record financial ledger: " + e.getMessage());
            }
        }

        String statusMsg = switch (newStatus) {
            case PICKED_UP -> "Order picked up by rider";
            case ON_THE_WAY -> "Order is on the way";
            case DELIVERED -> "Order delivered successfully";
            case COMPLETED -> "Order completed";
            default -> "Order status updated";
        };

        liveNotificationService.sendOrderStatusUpdate(updatedOrder.getId(), newStatus.name(), statusMsg);

        // No push notification needed for rider status updates (PICKED_UP, ON_THE_WAY, DELIVERED)
        // Real-time updates are handled via liveNotificationService above

        return generateDto(updatedOrder);
    }


    @Transactional(readOnly = true)
    @Override
    public Page<OrderDto> getRiderOrders(Long riderId, BasePageableRequest basePageableRequest) {
        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());
        // Find the Raider entity by userId first
        Raider raider = raiderRepo.findByAppUserId(riderId).orElse(null);
        if (raider == null) {
            return Page.empty(pageRequest);
        }
        Page<Order> orderPage = orderRepo.findByRaiderId(raider.getId(), pageRequest);
        return new PageImpl<>(convertEntityListToDtoList(orderPage.stream()), pageRequest, orderPage.getTotalElements());
    }



    @Transactional
    @Override
    public OrderDto assignRiderToOrder(Long orderId, Long riderId, Long sellerUserId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        Raider raider = raiderRepo.findById(riderId)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found: " + riderId));
        order.setRaider(raider);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setUpdateUser(sellerUserId);
        Order saved = orderRepo.save(order);
        liveNotificationService.sendRiderAssignment(saved.getId(), raider.getId(), raider.getVehicleType());
        liveNotificationService.sendOrderStatusUpdate(saved.getId(), OrderStatus.ASSIGNED.name(), "Rider assigned — order ready for pickup");

        try {
            if (raider.getAppUser() != null) {
                String shopName = (order.getBranch() != null && order.getBranch().getShop() != null) ? order.getBranch().getShop().getShopName() : "Shop";
                fcmNotificationService.notifyRiderOrderAssigned(raider.getAppUser().getId(), saved.getId(), shopName);
            }
            if (order.getCustomer() != null && order.getCustomer().getAppUser() != null) {
                fcmNotificationService.notifyCustomerOrderStatus(order.getCustomer().getAppUser().getId(), saved.getId(), "ASSIGNED", "Rider assigned to your order");
            }
        } catch (Exception e) {
            System.err.println("Failed to dispatch push notification: " + e.getMessage());
        }

        return generateDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderDto> getAssignedOrders(BasePageableRequest basePageableRequest) {
        PageRequest pageRequest = baseUtils.getPageRequest(basePageableRequest.getPage(), basePageableRequest.getSize());
        // Return orders that are PREPARING or ASSIGNED but NOT YET CLAIMED by any rider
        Page<Order> page = orderRepo.findByStatusInAndRaiderIsNull(
                java.util.List.of(OrderStatus.PREPARING, OrderStatus.ASSIGNED),
                pageRequest
        );
        return new PageImpl<>(convertEntityListToDtoList(page.stream()), pageRequest, page.getTotalElements());
    }

    @Transactional
    @Override
    public OrderDto riderAcceptOrder(Long orderId, Long riderUserId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        Raider raider = raiderRepo.findByAppUserId(riderUserId)
                .orElseGet(() -> raiderRepo.findById(riderUserId).orElse(null));
        if (raider == null) {
            throw new IllegalArgumentException("Rider profile not found for user: " + riderUserId);
        }

        // Admin Approval Gate: locked until verified by admin
        if (!Boolean.TRUE.equals(raider.getIsApproved())) {
            throw new IllegalStateException("Your rider account is locked pending admin document verification and approval.");
        }

        // Concurrency Guard: prevent double-accept race conditions
        if (order.getRaider() != null) {
            throw new IllegalStateException("This order has already been claimed by another rider.");
        }

        OrderStatus prevStatus = order.getStatus();
        order.setRaider(raider);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setUpdateUser(riderUserId);
        Order saved = orderRepo.save(order);

        applicationEventPublisher.publishEvent(new com.company.efood.sys.event.OrderStatusChangedEvent(
                this, saved.getId(), prevStatus, OrderStatus.ASSIGNED, riderUserId));

        liveNotificationService.sendRiderAssignment(saved.getId(), raider.getId(), raider.getVehicleType());
        liveNotificationService.sendOrderStatusUpdate(saved.getId(), OrderStatus.ASSIGNED.name(), "Rider accepted delivery");

        try {
            String riderName = (raider.getFullName() != null && !raider.getFullName().isBlank()) ? raider.getFullName() : "A rider";
            if (order.getCustomer() != null && order.getCustomer().getAppUser() != null) {
                fcmNotificationService.notifyCustomerOrderStatus(
                        order.getCustomer().getAppUser().getId(),
                        saved.getId(),
                        "ASSIGNED",
                        riderName + " has accepted your order and is heading for pickup!"
                );
            }
            if (order.getBranch() != null && order.getBranch().getShop() != null && order.getBranch().getShop().getSeller() != null) {
                com.company.efood.seller.entity.Seller seller = order.getBranch().getShop().getSeller();
                if (seller.getAppUser() != null) {
                    Map<String, String> data = new HashMap<>();
                    data.put("orderId", String.valueOf(saved.getId()));
                    data.put("type", "RIDER_ACCEPTED");
                    fcmNotificationService.sendNotificationToUser(
                            seller.getAppUser().getId(),
                            AppUserType.SELLER,
                            "🛵 Rider Assigned",
                            riderName + " has accepted Order #" + saved.getId() + " for delivery.",
                            data,
                            "RIDER_ACCEPTED",
                            saved.getId()
                    );
                }
            }
        } catch (Exception ex) {
            System.err.println("⚠️ [ORDER SERVICE] Failed to notify on rider acceptance: " + ex.getMessage());
        }

        return generateDto(saved);
    }

    private OrderItem mapOrderItem(OrderItemDto itemDto, Long userId) {
        if (itemDto == null || itemDto.getProduct() == null || itemDto.getProduct().getId() == null) {
            throw new IllegalArgumentException("Each order item must include a valid product reference");
        }

        if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
            throw new IllegalArgumentException("Each order item quantity must be greater than zero");
        }

        Product product = productRepo.findById(itemDto.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProduct().getId()));

        if (product.getQty() == null || product.getQty() < itemDto.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getProductName());
        }

        BigDecimal unitPrice = itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : product.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
        BigDecimal buyingPrice = product.getBuyingPrice() != null ? product.getBuyingPrice() : BigDecimal.ZERO;

        product.setQty(product.getQty() - itemDto.getQuantity());
        productRepo.save(product);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(itemDto.getQuantity());
        orderItem.setUnitPrice(unitPrice);
        orderItem.setTotalPrice(totalPrice);
        orderItem.setBuyingPrice(buyingPrice);
        orderItem.setEntryUser(userId);
        orderItem.setActive(true);
        baseUtils.setEntryUserInfo(orderItem);
        return orderItem;
    }

    private Raider resolveRaider(OrderDto orderDto) {
        if (orderDto.getRaiderId() != null) {
            return raiderRepo.findById(orderDto.getRaiderId())
                    .orElseThrow(() -> new IllegalArgumentException("Raider not found: " + orderDto.getRaiderId()));
        }

        if (orderDto.getVehicleType() != null && !orderDto.getVehicleType().isBlank()) {
            return raiderRepo.findFirstByIsAvailableTrueAndVehicleTypeIgnoreCase(orderDto.getVehicleType().trim())
                    .orElse(null);
        }

        return raiderRepo.findByIsAvailableTrue().stream().findFirst().orElse(null);
    }


    // Helper method to convert CartItem to OrderItem
    private List<OrderDto> convertEntityListToDtoList(Stream<Order> entityList) {
        return entityList.map(this::generateDto).collect(Collectors.toList());
    }


    public OrderDto generateDto(Order entity) {
        if (entity == null) return null;
        OrderDto dto = modelMapper.map(entity, OrderDto.class);
        
        List<OrderItem> orderItems = entity.getOrderItemList();
        if ((orderItems == null || orderItems.isEmpty()) && entity.getId() != null) {
            try {
                orderItems = orderItemRepo.findByOrderId(entity.getId());
            } catch (Exception ignored) {}
        }
        
        if (orderItems != null && !orderItems.isEmpty()) {
            dto.setItems(orderItems.stream().map(item -> {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setOrderId(entity.getId());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setTotalPrice(item.getTotalPrice());
                if (item.getProduct() != null) {
                    ProductDto pDto = new ProductDto();
                    pDto.setId(item.getProduct().getId());
                    pDto.setProductName(item.getProduct().getProductName());
                    pDto.setImgUrl(item.getProduct().getImgUrl());
                    pDto.setPrice(item.getProduct().getPrice());
                    pDto.setDiscountPrice(item.getProduct().getDiscountPrice());
                    if (item.getProduct().getBranch() != null) {
                        pDto.setBranchId(item.getProduct().getBranch().getId());
                    }
                    itemDto.setProduct(pDto);
                }
                return itemDto;
            }).collect(Collectors.toList()));
        }
        if (entity.getDeliveryAddress() != null) {
            AddressDto aDto = modelMapper.map(entity.getDeliveryAddress(), AddressDto.class);
            dto.setDeliveryAddress(aDto);
        }
        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
            if (entity.getCustomer().getFullName() != null && !entity.getCustomer().getFullName().isBlank()) {
                dto.setCustomerName(entity.getCustomer().getFullName());
            } else if (entity.getCustomer().getAppUser() != null) {
                dto.setCustomerName(entity.getCustomer().getAppUser().getDisplayName());
            }
            if (entity.getCustomer().getPhone() != null && !entity.getCustomer().getPhone().isBlank()) {
                dto.setCustomerPhone(entity.getCustomer().getPhone());
            } else if (entity.getCustomer().getAppUser() != null) {
                dto.setCustomerPhone(entity.getCustomer().getAppUser().getUsername());
            }
        }
        if (entity.getBranch() != null) {
            dto.setBranchId(entity.getBranch().getId());
            if (entity.getBranch().getShop() != null) {
                Shop s = entity.getBranch().getShop();
                dto.setShopId(s.getId());
                dto.setShopName(s.getShopName());
                if (s.getShopCategory() != null) {
                    dto.setShopCategoryId(s.getShopCategory().getId());
                    dto.setShopCategoryName(s.getShopCategory().getName());
                    if (s.getShopCategory().getService() != null) {
                        dto.setServiceId(s.getShopCategory().getService().getId());
                        dto.setServiceName(s.getShopCategory().getService().getName());
                        dto.setServiceCode(s.getShopCategory().getService().getCode());
                    }
                }
            }
        }
        if (entity.getDeliveryFee() != null) {
            dto.setDeliveryFee(entity.getDeliveryFee());
            if (riderPayoutPolicyService != null) {
                try {
                    dto.setRiderEarnings(riderPayoutPolicyService.calculateRiderShare(entity.getDeliveryFee(), null));
                } catch (Exception ignored) {}
            }
        }
        if (entity.getZone() != null) {
            dto.setZoneId(entity.getZone().getId());
            dto.setZoneName(entity.getZone().getName());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public java.util.Map<String, Object> getOrderTracking(Long orderId, Long currentUserId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        Customer customer = order.getCustomer();
        boolean isOwner = customer != null && (
                (customer.getId() != null && customer.getId().equals(currentUserId)) ||
                (customer.getAppUser() != null && customer.getAppUser().getId().equals(currentUserId))
        );
        boolean isAssignedRider = order.getRaider() != null && order.getRaider().getAppUser() != null
                && order.getRaider().getAppUser().getId().equals(currentUserId);

        if (!isOwner && !isAssignedRider) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to track this order");
        }

        java.util.Map<String, Object> tracking = new java.util.HashMap<>();
        tracking.put("orderId", order.getId());
        tracking.put("orderStatus", order.getStatus() != null ? order.getStatus().name() : null);

        if (order.getBranch() != null) {
            tracking.put("branchId", order.getBranch().getId());
            tracking.put("branchName", order.getBranch().getName());
            if (order.getBranch().getAddress() != null) {
                tracking.put("pickupAddress", order.getBranch().getAddress().getAddress());
                tracking.put("pickupLat", order.getBranch().getAddress().getLat());
                tracking.put("pickupLng", order.getBranch().getAddress().getLon());
            }
        }

        if (order.getDeliveryAddress() != null) {
            tracking.put("deliveryAddress", order.getDeliveryAddress().getAddress());
            tracking.put("deliveryLat", order.getDeliveryAddress().getLat());
            tracking.put("deliveryLng", order.getDeliveryAddress().getLon());
        }

        Raider raider = order.getRaider();
        boolean isLiveTrackingActive = order.getStatus() == OrderStatus.ASSIGNED
                || order.getStatus() == OrderStatus.PICKED_UP
                || order.getStatus() == OrderStatus.ON_THE_WAY
                || order.getStatus() == OrderStatus.IN_TRANSIT;

        tracking.put("isLiveTrackingActive", isLiveTrackingActive);

        if (raider != null) {
            tracking.put("raiderId", raider.getId());
            tracking.put("raiderName", raider.getAppUser() != null ? raider.getAppUser().getDisplayName() : "Delivery Partner");
            tracking.put("raiderPhone", raider.getAppUser() != null ? raider.getAppUser().getUsername() : null);
            tracking.put("vehicleType", raider.getVehicleType() != null ? raider.getVehicleType() : "BIKE");
            tracking.put("vehicleNumber", raider.getVehicleNumber());

            Double lat = raider.getCurrentLat();
            Double lng = raider.getCurrentLng();
            if (lat == null || lng == null) {
                Optional<RaiderLiveLocation> locOpt = raiderLiveLocationRepo.findByRaider(raider);
                if (locOpt.isPresent()) {
                    lat = locOpt.get().getLat();
                    lng = locOpt.get().getLng();
                }
            }
            tracking.put("riderLat", lat);
            tracking.put("riderLng", lng);
        }

        return tracking;
    }

    private int calculateTotalWeightGrams(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) return 0;
        int total = 0;
        for (OrderItem item : orderItems) {
            if (item.getProduct() != null && item.getProduct().getItemWeight() != null) {
                int itemGrams = parseWeightStringToGrams(item.getProduct().getItemWeight());
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                total += (itemGrams * qty);
            }
        }
        return total;
    }

    private int parseWeightStringToGrams(String weightStr) {
        if (weightStr == null || weightStr.trim().isEmpty()) return 0;
        try {
            String lower = weightStr.trim().toLowerCase();
            if (lower.contains("kg")) {
                String num = lower.replaceAll("[^0-9.]", "").trim();
                return (int) Math.round(Double.parseDouble(num) * 1000.0);
            } else if (lower.contains("gm") || lower.contains("gram") || lower.contains("g")) {
                String num = lower.replaceAll("[^0-9.]", "").trim();
                return (int) Math.round(Double.parseDouble(num));
            } else if (lower.contains("l") || lower.contains("ltr") || lower.contains("liter")) {
                String num = lower.replaceAll("[^0-9.]", "").trim();
                return (int) Math.round(Double.parseDouble(num) * 1000.0);
            } else if (lower.contains("ml")) {
                String num = lower.replaceAll("[^0-9.]", "").trim();
                return (int) Math.round(Double.parseDouble(num));
            } else {
                String num = lower.replaceAll("[^0-9.]", "").trim();
                if (!num.isEmpty()) {
                    return (int) Math.round(Double.parseDouble(num));
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }
}