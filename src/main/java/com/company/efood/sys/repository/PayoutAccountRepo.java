package com.company.efood.sys.repository;

import com.company.efood.sys.entity.PayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutAccountRepo extends JpaRepository<PayoutAccount, Long> {
    Optional<PayoutAccount> findByUserIdAndUserType(Long userId, String userType);
}
