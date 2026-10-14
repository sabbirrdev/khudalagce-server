package com.company.efood.sys.repository;

import com.company.efood.sys.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {

    @Query(value = "SELECT o.* FROM orders o WHERE o.customer_id = :customerId AND o.active = true ORDER BY o.entry_date DESC", nativeQuery = true)
    Page<Order> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query(
            value = "SELECT o FROM Order o " +
                    "JOIN FETCH o.customer c " +
                    "JOIN FETCH o.branch " +
                    "LEFT JOIN FETCH o.raider " +
                    "WHERE c.appUser.id = :userId AND o.active = true " +
                    "ORDER BY o.entryDate DESC",
            countQuery = "SELECT count(o) FROM Order o JOIN o.customer c " +
                    "WHERE c.appUser.id = :userId AND o.active = true"
    )
    Page<Order> findByCustomerAppUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT o.* FROM orders o JOIN branch b ON b.id = o.branch_id WHERE b.id = :branchId AND o.active = true ORDER BY o.entry_date DESC", nativeQuery = true)
    Page<Order> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);

    @Query(value = "SELECT o.* FROM orders o JOIN branch b ON b.id = o.branch_id WHERE b.shop_id = :shopId AND o.active = true ORDER BY o.entry_date DESC", nativeQuery = true)
    Page<Order> findByShopId(@Param("shopId") Long shopId, Pageable pageable);

    @Query(value = "SELECT o.* FROM orders o JOIN branch b ON b.id = o.branch_id JOIN shop s ON s.id = b.shop_id WHERE s.seller_id = :sellerId AND o.active = true ORDER BY o.entry_date DESC", nativeQuery = true)
    Page<Order> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query(
            value = "SELECT o.* FROM orders o " +
                    "JOIN branch b ON b.id = o.branch_id " +
                    "JOIN shop s ON s.id = b.shop_id " +
                    "JOIN seller sel ON sel.id = s.seller_id " +
                    "WHERE sel.user_id = :userId AND o.active = true " +
                    "ORDER BY o.entry_date DESC",
            countQuery = "SELECT COUNT(o.id) FROM orders o " +
                    "JOIN branch b ON b.id = o.branch_id " +
                    "JOIN shop s ON s.id = b.shop_id " +
                    "JOIN seller sel ON sel.id = s.seller_id " +
                    "WHERE sel.user_id = :userId AND o.active = true",
            nativeQuery = true)
    Page<Order> findBySellerAppUserId(@Param("userId") Long userId, Pageable pageable);
    @Query(value = "SELECT o.* FROM orders o JOIN raider r ON r.id = o.raider_id WHERE r.id = :riderId AND o.active = true ORDER BY o.entry_date DESC", nativeQuery = true)
    Page<Order> findByRaiderId(@Param("riderId") Long riderId, Pageable pageable);

    Page<Order> findByStatus(com.company.efood.sys.utils.OrderStatus status, Pageable pageable);

    Page<Order> findByStatusAndRaiderIsNull(com.company.efood.sys.utils.OrderStatus status, Pageable pageable);

    Page<Order> findByStatusInAndRaiderIsNull(java.util.List<com.company.efood.sys.utils.OrderStatus> statuses, Pageable pageable);

    List<Order> findByEntryDateBetween(LocalDateTime start, LocalDateTime end);
}
