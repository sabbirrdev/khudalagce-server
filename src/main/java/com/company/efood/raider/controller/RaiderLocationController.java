package com.company.efood.raider.controller;

import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.repository.RaiderLiveLocationRepo;
import com.company.efood.raider.repository.RaiderRepo;
import com.company.efood.sys.entity.Order;
import com.company.efood.sys.entity.RaiderLiveLocation;
import com.company.efood.sys.entity.RideRequest;
import com.company.efood.sys.repository.OrderRepo;
import com.company.efood.sys.services.RideService;
import com.company.efood.sys.utils.AuthTokenUtils;
import com.company.efood.sys.utils.OrderStatus;
import com.company.efood.websocket.service.LiveNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.company.efood.base.BaseConstants.PRIVET_ENDPOINT;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE;
import static com.company.efood.base.BaseConstants.PROCESS_COMPLETE_BN;

@RestController
@RequestMapping(PRIVET_ENDPOINT + "raider")
@AllArgsConstructor
public class RaiderLocationController {

    private final RaiderRepo raiderRepo;
    private final RaiderLiveLocationRepo raiderLiveLocationRepo;
    private final OrderRepo orderRepo;
    private final RideService rideService;
    private final BaseUtils baseUtils;
    private final AuthTokenUtils authTokenUtils;
    private final LiveNotificationService liveNotificationService;
    private final com.company.efood.sys.services.RiderPayoutPolicyService riderPayoutPolicyService;

    @PostMapping("/location")
    public BaseResponse updateLocation(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Rider profile not found"));

            Object latObj = body.get("lat");
            Object lngObj = body.get("lng");
            if (latObj == null || lngObj == null) {
                return BaseResponse.builder()
                        .status(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Latitude and longitude are required")
                        .build();
            }

            Double lat = Double.parseDouble(latObj.toString());
            Double lng = Double.parseDouble(lngObj.toString());

            raider.setCurrentLat(lat);
            raider.setCurrentLng(lng);
            raiderRepo.save(raider);

            RaiderLiveLocation location = raiderLiveLocationRepo.findByRaider(raider).orElseGet(RaiderLiveLocation::new);
            location.setRaider(raider);
            location.setLat(lat);
            location.setLng(lng);
            location.setUpdatedAt(LocalDateTime.now());
            location.setEntryUser(currentUserId);
            location.setEntryDate(LocalDateTime.now());
            raiderLiveLocationRepo.save(location);

            liveNotificationService.sendRiderLocation(raider.getId(), lat, lng);

            // Broadcast to all active delivery orders assigned to this rider
            List<Order> activeOrders = orderRepo.findAll().stream()
                    .filter(o -> o.getRaider() != null && raider.getId().equals(o.getRaider().getId())
                            && (o.getStatus() == OrderStatus.ASSIGNED || o.getStatus() == OrderStatus.PICKED_UP
                                    || o.getStatus() == OrderStatus.ON_THE_WAY || o.getStatus() == OrderStatus.IN_TRANSIT))
                    .collect(Collectors.toList());

            for (Order o : activeOrders) {
                Map<String, Object> orderTracking = new HashMap<>();
                orderTracking.put("orderId", o.getId());
                orderTracking.put("orderStatus", o.getStatus() != null ? o.getStatus().name() : "");
                orderTracking.put("raiderId", raider.getId());
                orderTracking.put("riderLat", lat);
                orderTracking.put("riderLng", lng);
                orderTracking.put("timestamp", System.currentTimeMillis());
                liveNotificationService.sendOrderTrackingUpdate(o.getId(), orderTracking);
            }

            return baseUtils.generateSuccessResponse(Map.of(
                    "raiderId", raider.getId(),
                    "lat", lat,
                    "lng", lng
            ), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @PostMapping("/status")
    public BaseResponse updateStatus(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Rider profile not found"));

            Object isAvailableObj = body.get("isAvailable");
            if (isAvailableObj != null) {
                Boolean isAvailable = Boolean.parseBoolean(isAvailableObj.toString());
                raider.setIsAvailable(isAvailable);
                raiderRepo.save(raider);
            }

            return baseUtils.generateSuccessResponse(Map.of(
                    "raiderId", raider.getId(),
                    "isAvailable", Boolean.TRUE.equals(raider.getIsAvailable()),
                    "vehicleType", raider.getVehicleType() != null ? raider.getVehicleType() : "BIKE"
            ), "Availability updated", "প্রাপ্যতা আপডেট হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    @GetMapping("/my-profile")
    public BaseResponse getMyProfile(HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId).orElse(null);
            if (raider == null) {
                return baseUtils.generateSuccessResponse(Map.of(
                        "isAvailable", false,
                        "vehicleType", "BIKE"
                ), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
            }
            return baseUtils.generateSuccessResponse(Map.of(
                    "id", raider.getId(),
                    "appUserId", currentUserId,
                    "isAvailable", Boolean.TRUE.equals(raider.getIsAvailable()),
                    "vehicleType", raider.getVehicleType() != null ? raider.getVehicleType() : "BIKE",
                    "vehicleNumber", raider.getVehicleNumber() != null ? raider.getVehicleNumber() : "",
                    "currentLat", raider.getCurrentLat() != null ? raider.getCurrentLat() : 0.0,
                    "currentLng", raider.getCurrentLng() != null ? raider.getCurrentLng() : 0.0
            ), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    /**
     * GET /api/private/raider/dashboard
     * Rider earnings and delivery + ride statistics summary.
     */
    @GetMapping("/dashboard")
    public BaseResponse getDashboard(HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId).orElse(null);
            if (raider == null) {
                return baseUtils.generateSuccessResponse(buildEmptyDashboard(), PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
            }

            // Completed delivery orders
            List<Order> allDelivered = orderRepo.findAll().stream()
                    .filter(o -> o.getRaider() != null
                            && raider.getId().equals(o.getRaider().getId())
                            && (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED))
                    .collect(Collectors.toList());

            // Completed ride sharing trips
            List<RideRequest> completedRides = rideService.getRiderRides(raider.getId()).stream()
                    .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());

            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);

            double todayDeliveryEarnings = allDelivered.stream()
                    .filter(o -> o.getEntryDate() != null && o.getEntryDate().toLocalDate().isEqual(today))
                    .mapToDouble(o -> {
                        java.math.BigDecimal fee = o.getDeliveryFee() != null ? o.getDeliveryFee() : java.math.BigDecimal.ZERO;
                        return riderPayoutPolicyService.calculateRiderShare(fee, null).doubleValue();
                    })
                    .sum();

            double todayRideEarnings = completedRides.stream()
                    .filter(r -> r.getEntryDate() != null && r.getEntryDate().toLocalDate().isEqual(today))
                    .mapToDouble(r -> r.getRiderEarnings() != null ? r.getRiderEarnings() : 0.0)
                    .sum();

            double weekDeliveryEarnings = allDelivered.stream()
                    .filter(o -> o.getEntryDate() != null && !o.getEntryDate().toLocalDate().isBefore(weekStart))
                    .mapToDouble(o -> {
                        java.math.BigDecimal fee = o.getDeliveryFee() != null ? o.getDeliveryFee() : java.math.BigDecimal.ZERO;
                        return riderPayoutPolicyService.calculateRiderShare(fee, null).doubleValue();
                    })
                    .sum();

            double weekRideEarnings = completedRides.stream()
                    .filter(r -> r.getEntryDate() != null && !r.getEntryDate().toLocalDate().isBefore(weekStart))
                    .mapToDouble(r -> r.getRiderEarnings() != null ? r.getRiderEarnings() : 0.0)
                    .sum();

            double totalDeliveryEarnings = allDelivered.stream()
                    .mapToDouble(o -> {
                        java.math.BigDecimal fee = o.getDeliveryFee() != null ? o.getDeliveryFee() : java.math.BigDecimal.ZERO;
                        return riderPayoutPolicyService.calculateRiderShare(fee, null).doubleValue();
                    })
                    .sum();

            double totalRideEarnings = completedRides.stream()
                    .mapToDouble(r -> r.getRiderEarnings() != null ? r.getRiderEarnings() : 0.0)
                    .sum();

            long pendingDeliveries = orderRepo.findAll().stream()
                    .filter(o -> o.getRaider() != null
                            && raider.getId().equals(o.getRaider().getId())
                            && o.getStatus() != OrderStatus.DELIVERED
                            && o.getStatus() != OrderStatus.COMPLETED
                            && o.getStatus() != OrderStatus.CANCELLED
                            && o.getStatus() != OrderStatus.REJECTED)
                    .count();

            long activeRides = rideService.getRiderRides(raider.getId()).stream()
                    .filter(r -> "ACCEPTED".equalsIgnoreCase(r.getStatus()) || "ON_THE_WAY".equalsIgnoreCase(r.getStatus()))
                    .count();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("raiderId", raider.getId());
            dashboard.put("isAvailable", Boolean.TRUE.equals(raider.getIsAvailable()));
            dashboard.put("vehicleType", raider.getVehicleType() != null ? raider.getVehicleType() : "BIKE");
            dashboard.put("totalDeliveries", (long) allDelivered.size());
            dashboard.put("pendingDeliveries", pendingDeliveries);
            dashboard.put("totalRides", (long) completedRides.size());
            dashboard.put("activeRides", activeRides);
            dashboard.put("todayEarnings", Math.round((todayDeliveryEarnings + todayRideEarnings) * 100.0) / 100.0);
            dashboard.put("weekEarnings", Math.round((weekDeliveryEarnings + weekRideEarnings) * 100.0) / 100.0);
            dashboard.put("totalEarnings", Math.round((totalDeliveryEarnings + totalRideEarnings) * 100.0) / 100.0);
            dashboard.put("deliveryEarnings", Math.round(totalDeliveryEarnings * 100.0) / 100.0);
            dashboard.put("rideEarnings", Math.round(totalRideEarnings * 100.0) / 100.0);

            return baseUtils.generateSuccessResponse(dashboard, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    // ─── Ride Sharing Endpoints for Rider ─────────────────────────────────────────

    /**
     * GET /api/private/raider/rides/available
     * Get available ride requests matching rider's vehicle type.
     */
    @GetMapping("/rides/available")
    public BaseResponse getAvailableRides(HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId).orElse(null);
            String vType = raider != null && raider.getVehicleType() != null ? raider.getVehicleType() : null;
            List<RideRequest> rides = rideService.getAvailableRides(vType);
            return baseUtils.generateSuccessResponse(rides, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    /**
     * POST /api/private/raider/rides/{id}/accept
     * Accept a ride request.
     */
    @PostMapping("/rides/{id}/accept")
    public BaseResponse acceptRide(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Rider profile not found"));
            RideRequest accepted = rideService.acceptRide(id, raider.getId(), currentUserId);
            return baseUtils.generateSuccessResponse(accepted, "Ride accepted", "রাইড গ্রহণ করা হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    /**
     * POST /api/private/raider/rides/{id}/status
     * Body: { "status": "ON_THE_WAY" | "COMPLETED" | "CANCELLED" }
     */
    @PostMapping("/rides/{id}/status")
    public BaseResponse updateRideStatus(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Rider profile not found"));
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return BaseResponse.builder()
                        .status(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Status is required")
                        .build();
            }
            RideRequest updated = rideService.updateRideStatus(id, status, raider.getId(), currentUserId);
            return baseUtils.generateSuccessResponse(updated, "Ride status updated", "রাইড স্ট্যাটাস আপডেট হয়েছে");
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    /**
     * GET /api/private/raider/rides/my
     * Get all rides (active and past) for the current rider.
     */
    @GetMapping("/rides/my")
    public BaseResponse getMyRides(HttpServletRequest request) {
        try {
            Long currentUserId = authTokenUtils.getUserIdFromRequest(request);
            Raider raider = raiderRepo.findByAppUserId(currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Rider profile not found"));
            List<RideRequest> myRides = rideService.getRiderRides(raider.getId());
            return baseUtils.generateSuccessResponse(myRides, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception ex) {
            return baseUtils.generateErrorResponse(ex);
        }
    }

    private Map<String, Object> buildEmptyDashboard() {
        Map<String, Object> d = new HashMap<>();
        d.put("isAvailable", false);
        d.put("vehicleType", "BIKE");
        d.put("totalDeliveries", 0L);
        d.put("pendingDeliveries", 0L);
        d.put("totalRides", 0L);
        d.put("activeRides", 0L);
        d.put("todayEarnings", 0.0);
        d.put("weekEarnings", 0.0);
        d.put("totalEarnings", 0.0);
        d.put("deliveryEarnings", 0.0);
        d.put("rideEarnings", 0.0);
        return d;
    }
}
