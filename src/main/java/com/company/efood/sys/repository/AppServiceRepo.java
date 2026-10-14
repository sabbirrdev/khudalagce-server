package com.company.efood.sys.repository;

import com.company.efood.sys.entity.AppService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppServiceRepo extends JpaRepository<AppService, Long> {

    Optional<AppService> findByCode(String code);

    boolean existsByCode(String code);

    List<AppService> findByActiveTrueOrderByDisplayOrderAsc();

    Page<AppService> findAllByOrderByDisplayOrderAsc(Pageable pageable);
}
