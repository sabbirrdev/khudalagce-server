package com.company.efood.sys.repository;

import com.company.efood.sys.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepo extends JpaRepository<Product,Long> {

    String categoryQuery = "SELECT * FROM product WHERE category_id = :categoryId AND active = true";

    @Query(value = categoryQuery, nativeQuery = true)
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);


    String shopQuery = "SELECT p.* FROM product p JOIN branch b ON b.id = p.branch_id WHERE b.branch_id = :branchId AND p.category_id = :categoryId";

    @Query(value = shopQuery, nativeQuery = true)
    List<Product> findByBranchIdAndCategoryId(@Param("branchId") Long branchId, @Param("categoryId") Long categoryId);


//    String shopQuery2 = "SELECT p.id, p.active, " +
//            "p.entry_app_user_code, p.entry_date, p.entry_user," +
//            " p.update_app_user_code, p.update_date, p.update_user," +
//            " p.brand, p.description, p.discount_price," +
//            " p.image_url, p.price, p.product_name, p.qty," +
//            " c.id AS category_id, b.id AS branch_id" +
//            " FROM product p JOIN category c ON p.category_id = c.id JOIN branch b ON  p.branch_id = b.id " +
//            " WHERE s.branch_id = :branchId";
//    String countQuery = "SELECT count(*) FROM product p JOIN branch b ON b.id = p.branch_id WHERE b.branch_id = :branchId";
//
//    @Query(value = shopQuery2, countQuery = countQuery, nativeQuery = true)
//    Page<Product> findPaginatedProductByBranchId(@Param("branchId") Integer branchId, Pageable pageable);

    @Query(value = "SELECT p.* FROM product p JOIN branch b ON b.id = p.branch_id WHERE b.id = :branchId AND p.active = true", nativeQuery = true)
    Page<Product> findPaginatedProductByBranchId(@Param("branchId") Long branchId, Pageable pageable);

    @Query(value = "SELECT p.* FROM product p JOIN branch b ON b.id = p.branch_id " +
            "WHERE b.id = :branchId AND p.active = true " +
            "AND (:productType IS NULL OR p.product_type = :productType) " +
            "AND (:serviceType IS NULL OR p.service_type = :serviceType)", nativeQuery = true)
    Page<Product> findPaginatedProductByBranchIdAndFilters(
            @Param("branchId") Long branchId,
            @Param("productType") String productType,
            @Param("serviceType") String serviceType,
            Pageable pageable
    );

    @Query(value = "SELECT p.* FROM product p " +
           "WHERE p.branch_id = :branchId AND p.active = true AND p.qty <= :threshold " +
           "ORDER BY p.qty ASC, p.id DESC", nativeQuery = true)
    List<Product> findLowStockProductsByBranchId(@Param("branchId") Long branchId, @Param("threshold") Integer threshold);

    @Query(value = "SELECT p.* FROM product p " +
            "WHERE p.active = true " +
            "AND (:branchId IS NULL OR p.branch_id = :branchId) " +
            "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
            "AND (:productType IS NULL OR UPPER(p.product_type) = UPPER(:productType)) " +
            "AND (:serviceType IS NULL OR UPPER(p.service_type) = UPPER(:serviceType)) " +
            "AND (:search IS NULL OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.id DESC", nativeQuery = true)
    Page<Product> findActiveProductsWithFilters(
            @Param("branchId") Long branchId,
            @Param("categoryId") Long categoryId,
            @Param("productType") String productType,
            @Param("serviceType") String serviceType,
            @Param("search") String search,
            Pageable pageable
    );

    /** Return all products awaiting admin approval. */
    List<Product> findByIsApprovedFalseOrIsApprovedIsNull();

    /** Count products awaiting admin approval. */
    long countByIsApprovedFalseOrIsApprovedIsNull();
}
