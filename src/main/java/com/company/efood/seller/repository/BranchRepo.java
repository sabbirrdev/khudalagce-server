package com.company.efood.seller.repository;

import com.company.efood.base.BaseDropdownModel;
import com.company.efood.sys.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepo extends JpaRepository<Branch,Long> {

    @Query("SELECT b.id AS id, b.name AS name " +
            "FROM Branch b " +
            "WHERE b.shop.id = :shopId " +
            "AND b.active = true " +
            "ORDER BY b.name ASC")
    List<BaseDropdownModel> findDropdownModelByShopId(@Param("shopId") Long shopId);

    @Query(value = "SELECT b.* FROM branch b " +
            "JOIN address_book a ON b.address_id = a.id " +
            "WHERE b.shop_id = :shopId " +
            "AND b.is_open = true " +
            "AND b.active = true " +
            "ORDER BY (6371.0 * acos(LEAST(1.0, GREATEST(-1.0, " +
            "cos(radians(:customerLat)) * cos(radians(a.lat)) * " +
            "cos(radians(a.lon) - radians(:customerLon)) + " +
            "sin(radians(:customerLat)) * sin(radians(a.lat)))))) ASC " +
            "LIMIT 1", nativeQuery = true)
    java.util.Optional<Branch> findNearestActiveBranchByShopId(
            @Param("shopId") Long shopId,
            @Param("customerLat") double customerLat,
            @Param("customerLon") double customerLon
    );

    java.util.Optional<Branch> findFirstByShopIdAndIsOpenTrueAndActiveTrue(Long shopId);

    /** Efficient existence check — does this shop have at least one active branch? */
    boolean existsByShopIdAndActiveTrue(Long shopId);
}
