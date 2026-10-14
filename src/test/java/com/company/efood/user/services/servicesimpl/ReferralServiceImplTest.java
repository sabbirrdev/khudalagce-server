package com.company.efood.user.services.servicesimpl;

import com.company.efood.base.BaseUtils;
import com.company.efood.user.dto.ReferralInfoDto;
import com.company.efood.user.entity.Customer;
import com.company.efood.user.entity.LoyaltyTransactionType;
import com.company.efood.user.repository.CustomerRepo;
import com.company.efood.user.repository.LoyaltyAccountRepo;
import com.company.efood.user.repository.LoyaltyTransactionRepo;
import com.company.efood.user.services.LoyaltyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralServiceImplTest {

    @Mock
    private CustomerRepo customerRepo;

    @Mock
    private LoyaltyAccountRepo loyaltyAccountRepo;

    @Mock
    private LoyaltyTransactionRepo loyaltyTransactionRepo;

    @Mock
    private LoyaltyService loyaltyService;

    @Mock
    private BaseUtils baseUtils;

    @InjectMocks
    private ReferralServiceImpl referralService;

    private Customer referrer;
    private Customer referee;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(referralService, "referrerBonus", 50);
        ReflectionTestUtils.setField(referralService, "refereeBonus", 30);
        ReflectionTestUtils.setField(referralService, "shareBaseUrl", "efood://referral?code=");
        ReflectionTestUtils.setField(referralService, "maxReferralsPerDay", 10);

        referrer = new Customer();
        referrer.setId(1L);
        referrer.setReferralCode("REF12345");

        referee = new Customer();
        referee.setId(2L);
    }

    @Test
    void shouldAssignUniqueCodeAndLinkReferrerOnSignup() {
        when(customerRepo.findByReferralCode(anyString())).thenReturn(Optional.empty());
        when(customerRepo.findByReferralCode("REF12345")).thenReturn(Optional.of(referrer));

        referralService.initReferral(referee, "REF12345");

        assertNotNull(referee.getReferralCode());
        assertEquals(referrer, referee.getReferredBy());
        verify(customerRepo).save(referee);
    }

    @Test
    void shouldPreventSelfReferral() {
        when(customerRepo.findByReferralCode(anyString())).thenReturn(Optional.empty());

        // Referee tries to use own code
        referralService.initReferral(referee, null);
        String assignedCode = referee.getReferralCode();

        referralService.initReferral(referee, assignedCode);

        assertNull(referee.getReferredBy());
    }

    @Test
    void shouldCreditReferralBonusesOnFirstCompletedOrder() {
        referee.setReferredBy(referrer);
        referee.setReferralRewardCredited(false);

        when(customerRepo.findById(2L)).thenReturn(Optional.of(referee));
        // Exactly 1 EARNED order in transaction ledger = first completed order
        when(loyaltyTransactionRepo.countByCustomerIdAndType(2L, LoyaltyTransactionType.EARNED)).thenReturn(1L);

        referralService.creditReferralBonusIfEligible(2L, 100L);

        // Referee gets 30 points, Referrer gets 50 points
        verify(loyaltyService).adjustPoints(eq(2L), eq(100L), eq(30), contains("Referral sign-up bonus"));
        verify(loyaltyService).adjustPoints(eq(1L), eq(100L), eq(50), contains("Referral reward"));
        assertTrue(referee.isReferralRewardCredited());
        verify(customerRepo).save(referee);
    }

    @Test
    void shouldNotCreditReferralBonusIfAlreadyCredited() {
        referee.setReferredBy(referrer);
        referee.setReferralRewardCredited(true); // already rewarded

        when(customerRepo.findById(2L)).thenReturn(Optional.of(referee));

        referralService.creditReferralBonusIfEligible(2L, 101L);

        verify(loyaltyService, never()).adjustPoints(any(), any(), anyInt(), any());
        verify(customerRepo, never()).save(any());
    }

    @Test
    void shouldNotCreditReferralBonusIfOrderIsNotFirstCompleted() {
        referee.setReferredBy(referrer);
        referee.setReferralRewardCredited(false);

        when(customerRepo.findById(2L)).thenReturn(Optional.of(referee));
        // 2 completed orders = not the first order
        when(loyaltyTransactionRepo.countByCustomerIdAndType(2L, LoyaltyTransactionType.EARNED)).thenReturn(2L);

        referralService.creditReferralBonusIfEligible(2L, 102L);

        verify(loyaltyService, never()).adjustPoints(any(), any(), anyInt(), any());
    }

    @Test
    void shouldReturnCorrectReferralInfo() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(referrer));
        // countByReferredById: total friends who used this referral code
        when(customerRepo.countByReferredById(1L)).thenReturn(3L);
        // countReferredAndRewarded: friends whose rewards have been credited (for points calculation)
        when(customerRepo.countReferredAndRewarded(1L)).thenReturn(3L);

        ReferralInfoDto info = referralService.getReferralInfo(1L);

        assertEquals("REF12345", info.getReferralCode());
        assertEquals(3L, info.getReferralCount());
        assertEquals(150, info.getPointsFromReferrals()); // 3 * 50 = 150
        assertEquals(30, info.getRefereeBonus());
        assertEquals(50, info.getReferrerBonus());
        assertEquals("efood://referral?code=REF12345", info.getShareUrl());
    }
}
