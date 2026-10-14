package com.company.efood.sys.repository;

import com.company.efood.sys.entity.OrderFinancialLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderFinancialLedgerRepo extends JpaRepository<OrderFinancialLedger, Long> {
    Optional<OrderFinancialLedger> findByOrder_Id(Long orderId);

    Page<OrderFinancialLedger> findAllByOrderByCompletedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.sellerNetEarnings), 0) FROM OrderFinancialLedger l WHERE l.active = true")
    BigDecimal sumTotalSellerNetEarnings();

    @Query("SELECT COALESCE(SUM(l.riderDisbursementAmount), 0) FROM OrderFinancialLedger l WHERE l.active = true")
    BigDecimal sumTotalRiderDisbursement();

    @Query("SELECT COALESCE(SUM(l.netPlatformRevenue), 0) FROM OrderFinancialLedger l WHERE l.active = true")
    BigDecimal sumTotalNetPlatformRevenue();

    @Query("SELECT COALESCE(SUM(l.customerTotal), 0) FROM OrderFinancialLedger l WHERE l.active = true")
    BigDecimal sumTotalCustomerVolume();

    @Query("SELECT COALESCE(SUM(l.sellerNetEarnings), 0) FROM OrderFinancialLedger l WHERE l.shop.id = :shopId AND l.active = true")
    BigDecimal sumSellerNetEarningsByShopId(@Param("shopId") Long shopId);

    @Query("SELECT COALESCE(SUM(l.sellerNetEarnings), 0) FROM OrderFinancialLedger l WHERE l.shop.seller.appUser.id = :sellerUserId AND l.active = true")
    BigDecimal sumSellerNetEarningsBySellerUserId(@Param("sellerUserId") Long sellerUserId);

    @Query("SELECT COALESCE(SUM(l.sellerNetEarnings), 0) FROM OrderFinancialLedger l WHERE l.shop.seller.appUser.id = :sellerUserId AND l.completedAt BETWEEN :start AND :end AND l.active = true")
    BigDecimal sumSellerNetEarningsBySellerUserIdAndPeriod(@Param("sellerUserId") Long sellerUserId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(l.riderDisbursementAmount), 0) FROM OrderFinancialLedger l WHERE l.raider.id = :raiderId AND l.active = true")
    BigDecimal sumRiderDisbursementByRaiderId(@Param("raiderId") Long raiderId);

    @Query("SELECT COALESCE(SUM(l.riderDisbursementAmount), 0) FROM OrderFinancialLedger l WHERE l.raider.appUser.id = :riderUserId AND l.completedAt BETWEEN :start AND :end AND l.active = true")
    BigDecimal sumRiderDisbursementByRiderUserIdAndPeriod(@Param("riderUserId") Long riderUserId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
