package com.company.efood.sys.repository;

import com.company.efood.sys.entity.ShopCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopCategoryRepo extends JpaRepository<ShopCategory, Long> {

    Optional<ShopCategory> findByCode(String code);

    boolean existsByCode(String code);

    List<ShopCategory> findByActiveTrue();

    List<ShopCategory> findByServiceIdAndActiveTrue(Long serviceId);

    List<ShopCategory> findByCommissionPolicyIdAndActiveTrue(Long commissionPolicyId);

    Page<ShopCategory> findByServiceId(Long serviceId, Pageable pageable);
}
