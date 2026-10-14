package com.company.efood.sys.repository;

import com.company.efood.base.BaseDropdownModel;
import com.company.efood.sys.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByPhone(String phone);

    boolean existsByUsername(String username);

    AppUser findByUsernameAndActive(String username, Boolean active);

    List<AppUser> findByActive(boolean active);

    String dropdownQuery = """
            select a.id, a.name as name,\r
            a.menu_type as extra
            from sya_menu_item a \r
            where 1=1\r
            and a.active = true\r
            order by a.id desc""";

    @Query(value = dropdownQuery, nativeQuery = true)
    List<BaseDropdownModel> findDropdownModel();

}
