package com.company.efood.support.repository;

import com.company.efood.support.entity.SupportConversation;
import com.company.efood.sys.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportConversationRepo extends JpaRepository<SupportConversation, Long> {
    List<SupportConversation> findByCustomerOrderByEntryDateDesc(AppUser customer);
    List<SupportConversation> findAllByOrderByEntryDateDesc();
    List<SupportConversation> findByStatusOrderByEntryDateDesc(String status);
}
