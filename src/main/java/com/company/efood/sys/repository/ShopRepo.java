package com.company.efood.sys.repository;

import com.company.efood.sys.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopRepo extends JpaRepository<Shop, Long> {

    Optional<Shop> findShopBySellerId(Long sellerId);

    /** Return all shops awaiting admin approval (false OR null for legacy rows). */
    List<Shop> findByIsApprovedFalseOrIsApprovedIsNull();

    /** Count shops awaiting admin approval. */
    long countByIsApprovedFalseOrIsApprovedIsNull();

    // ─── Customer-Facing: Service-Filtered Queries ────────────────────────────
    // All queries use explicit LEFT JOINs to avoid implicit INNER JOIN behavior
    // in complex OR conditions which causes Hibernate to silently exclude shops.

    /** Non-pageable: approved shops under a specific service. */
    @Query("SELECT DISTINCT s FROM Shop s " +
           "LEFT JOIN s.shopCategory sc " +
           "LEFT JOIN sc.service scSvc " +
           "LEFT JOIN s.seller sel " +
           "LEFT JOIN sel.shopCategory selCat " +
           "LEFT JOIN selCat.service selSvc " +
           "WHERE (scSvc.id = :serviceId OR selSvc.id = :serviceId " +
           "   OR (sc IS NULL AND selCat IS NULL AND (" +
           "       (:serviceId = 1 AND UPPER(COALESCE(s.shopType,'')) IN ('RESTAURANT','FOOD','FAST_FOOD','HOME_KITCHEN','CAFE')) OR " +
           "       (:serviceId = 2 AND UPPER(COALESCE(s.shopType,'')) IN ('MEDICINE','PHARMACY','PHARMACY_SHOP','MEDICAL_STORE','HEALTH_CARE')) OR " +
           "       (:serviceId = 3 AND UPPER(COALESCE(s.shopType,'')) IN ('GROCERY','GROCERIES','GROCERY_STORE','SUPERMARKET','VEGETABLES','FRUITS')) OR " +
           "       (:serviceId = 4 AND UPPER(COALESCE(s.shopType,'')) IN ('RIDE','RIDE_SHARING','BIKE_TAXI','CAR_HIRE','CNG')) OR " +
           "       (:serviceId = 5 AND UPPER(COALESCE(s.shopType,'')) IN ('OFFER','EXCLUSIVE_OFFERS','DEALS','VOUCHERS'))" +
           "   )))" +
           " AND (s.active = true OR s.active IS NULL)" +
           " AND s.isApproved = true")
    List<Shop> findByServiceId(@Param("serviceId") Long serviceId);

    /** Pageable (no search): approved shops under a specific service. */
    @Query(value =
           "SELECT DISTINCT s FROM Shop s " +
           "LEFT JOIN s.shopCategory sc " +
           "LEFT JOIN sc.service scSvc " +
           "LEFT JOIN s.seller sel " +
           "LEFT JOIN sel.shopCategory selCat " +
           "LEFT JOIN selCat.service selSvc " +
           "WHERE (scSvc.id = :serviceId OR selSvc.id = :serviceId " +
           "   OR (sc IS NULL AND selCat IS NULL AND (" +
           "       (:serviceId = 1 AND UPPER(COALESCE(s.shopType,'')) IN ('RESTAURANT','FOOD','FAST_FOOD','HOME_KITCHEN','CAFE')) OR " +
           "       (:serviceId = 2 AND UPPER(COALESCE(s.shopType,'')) IN ('MEDICINE','PHARMACY','PHARMACY_SHOP','MEDICAL_STORE','HEALTH_CARE')) OR " +
           "       (:serviceId = 3 AND UPPER(COALESCE(s.shopType,'')) IN ('GROCERY','GROCERIES','GROCERY_STORE','SUPERMARKET','VEGETABLES','FRUITS')) OR " +
           "       (:serviceId = 4 AND UPPER(COALESCE(s.shopType,'')) IN ('RIDE','RIDE_SHARING','BIKE_TAXI','CAR_HIRE','CNG')) OR " +
           "       (:serviceId = 5 AND UPPER(COALESCE(s.shopType,'')) IN ('OFFER','EXCLUSIVE_OFFERS','DEALS','VOUCHERS'))" +
           "   )))" +
           " AND (s.active = true OR s.active IS NULL)" +
           " AND s.isApproved = true",
           countQuery =
           "SELECT COUNT(DISTINCT s) FROM Shop s " +
           "LEFT JOIN s.shopCategory sc " +
           "LEFT JOIN sc.service scSvc " +
           "LEFT JOIN s.seller sel " +
           "LEFT JOIN sel.shopCategory selCat " +
           "LEFT JOIN selCat.service selSvc " +
           "WHERE (scSvc.id = :serviceId OR selSvc.id = :serviceId " +
           "   OR (sc IS NULL AND selCat IS NULL AND (" +
           "       (:serviceId = 1 AND UPPER(COALESCE(s.shopType,'')) IN ('RESTAURANT','FOOD','FAST_FOOD','HOME_KITCHEN','CAFE')) OR " +
           "       (:serviceId = 2 AND UPPER(COALESCE(s.shopType,'')) IN ('MEDICINE','PHARMACY','PHARMACY_SHOP','MEDICAL_STORE','HEALTH_CARE')) OR " +
           "       (:serviceId = 3 AND UPPER(COALESCE(s.shopType,'')) IN ('GROCERY','GROCERIES','GROCERY_STORE','SUPERMARKET','VEGETABLES','FRUITS')) OR " +
           "       (:serviceId = 4 AND UPPER(COALESCE(s.shopType,'')) IN ('RIDE','RIDE_SHARING','BIKE_TAXI','CAR_HIRE','CNG')) OR " +
           "       (:serviceId = 5 AND UPPER(COALESCE(s.shopType,'')) IN ('OFFER','EXCLUSIVE_OFFERS','DEALS','VOUCHERS'))" +
           "   )))" +
           " AND (s.active = true OR s.active IS NULL)" +
           " AND s.isApproved = true")
    Page<Shop> findByServiceIdPageable(@Param("serviceId") Long serviceId, Pageable pageable);

    /** Pageable with optional name/address search: approved shops under a specific service. */
    @Query(value =
           "SELECT DISTINCT s FROM Shop s " +
           "LEFT JOIN s.shopCategory sc " +
           "LEFT JOIN sc.service scSvc " +
           "LEFT JOIN s.seller sel " +
           "LEFT JOIN sel.shopCategory selCat " +
           "LEFT JOIN selCat.service selSvc " +
           "WHERE (scSvc.id = :serviceId OR selSvc.id = :serviceId " +
           "   OR (sc IS NULL AND selCat IS NULL AND (" +
           "       (:serviceId = 1 AND UPPER(COALESCE(s.shopType,'')) IN ('RESTAURANT','FOOD','FAST_FOOD','HOME_KITCHEN','CAFE')) OR " +
           "       (:serviceId = 2 AND UPPER(COALESCE(s.shopType,'')) IN ('MEDICINE','PHARMACY','PHARMACY_SHOP','MEDICAL_STORE','HEALTH_CARE')) OR " +
           "       (:serviceId = 3 AND UPPER(COALESCE(s.shopType,'')) IN ('GROCERY','GROCERIES','GROCERY_STORE','SUPERMARKET','VEGETABLES','FRUITS')) OR " +
           "       (:serviceId = 4 AND UPPER(COALESCE(s.shopType,'')) IN ('RIDE','RIDE_SHARING','BIKE_TAXI','CAR_HIRE','CNG')) OR " +
           "       (:serviceId = 5 AND UPPER(COALESCE(s.shopType,'')) IN ('OFFER','EXCLUSIVE_OFFERS','DEALS','VOUCHERS'))" +
           "   )))" +
           " AND (s.active = true OR s.active IS NULL)" +
           " AND s.isApproved = true" +
           " AND (:search IS NULL OR :search = '' " +
           "     OR LOWER(s.shopName) LIKE LOWER(CONCAT('%',:search,'%'))" +
           "     OR LOWER(COALESCE(s.shopAddress,'')) LIKE LOWER(CONCAT('%',:search,'%')))",
           countQuery =
           "SELECT COUNT(DISTINCT s) FROM Shop s " +
           "LEFT JOIN s.shopCategory sc " +
           "LEFT JOIN sc.service scSvc " +
           "LEFT JOIN s.seller sel " +
           "LEFT JOIN sel.shopCategory selCat " +
           "LEFT JOIN selCat.service selSvc " +
           "WHERE (scSvc.id = :serviceId OR selSvc.id = :serviceId " +
           "   OR (sc IS NULL AND selCat IS NULL AND (" +
           "       (:serviceId = 1 AND UPPER(COALESCE(s.shopType,'')) IN ('RESTAURANT','FOOD','FAST_FOOD','HOME_KITCHEN','CAFE')) OR " +
           "       (:serviceId = 2 AND UPPER(COALESCE(s.shopType,'')) IN ('MEDICINE','PHARMACY','PHARMACY_SHOP','MEDICAL_STORE','HEALTH_CARE')) OR " +
           "       (:serviceId = 3 AND UPPER(COALESCE(s.shopType,'')) IN ('GROCERY','GROCERIES','GROCERY_STORE','SUPERMARKET','VEGETABLES','FRUITS')) OR " +
           "       (:serviceId = 4 AND UPPER(COALESCE(s.shopType,'')) IN ('RIDE','RIDE_SHARING','BIKE_TAXI','CAR_HIRE','CNG')) OR " +
           "       (:serviceId = 5 AND UPPER(COALESCE(s.shopType,'')) IN ('OFFER','EXCLUSIVE_OFFERS','DEALS','VOUCHERS'))" +
           "   )))" +
           " AND (s.active = true OR s.active IS NULL)" +
           " AND s.isApproved = true" +
           " AND (:search IS NULL OR :search = '' " +
           "     OR LOWER(s.shopName) LIKE LOWER(CONCAT('%',:search,'%'))" +
           "     OR LOWER(COALESCE(s.shopAddress,'')) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Shop> findByServiceIdAndSearchPageable(
            @Param("serviceId") Long serviceId,
            @Param("search") String search,
            Pageable pageable);

    // ─── Admin View: All shops by service regardless of approval ─────────────
    @Query(value =
           "SELECT DISTINCT s FROM Shop s " +
           "LEFT JOIN s.shopCategory sc LEFT JOIN sc.service scSvc " +
           "LEFT JOIN s.seller sel LEFT JOIN sel.shopCategory selCat LEFT JOIN selCat.service selSvc " +
           "WHERE scSvc.id = :serviceId OR selSvc.id = :serviceId",
           countQuery =
           "SELECT COUNT(DISTINCT s) FROM Shop s " +
           "LEFT JOIN s.shopCategory sc LEFT JOIN sc.service scSvc " +
           "LEFT JOIN s.seller sel LEFT JOIN sel.shopCategory selCat LEFT JOIN selCat.service selSvc " +
           "WHERE scSvc.id = :serviceId OR selSvc.id = :serviceId")
    Page<Shop> findAllByServiceId(@Param("serviceId") Long serviceId, Pageable pageable);

    /** Find all approved and active shops (admin listing). */
    @Query("SELECT s FROM Shop s WHERE s.active = true AND s.isApproved = true")
    Page<Shop> findByApprovedTrue(Pageable pageable);
}
