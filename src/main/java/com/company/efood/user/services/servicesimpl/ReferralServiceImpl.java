package com.company.efood.user.services.servicesimpl;

import com.company.efood.base.BaseUtils;
import com.company.efood.user.dto.ReferralInfoDto;
import com.company.efood.user.entity.Customer;
import com.company.efood.user.entity.LoyaltyTransactionType;
import com.company.efood.user.repository.CustomerRepo;
import com.company.efood.user.repository.LoyaltyAccountRepo;
import com.company.efood.user.repository.LoyaltyTransactionRepo;
import com.company.efood.user.services.LoyaltyService;
import com.company.efood.user.services.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // avoids confusable chars
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepo customerRepo;
    private final LoyaltyAccountRepo loyaltyAccountRepo;
    private final LoyaltyTransactionRepo loyaltyTransactionRepo;
    private final LoyaltyService loyaltyService;
    private final BaseUtils baseUtils;

    @Value("${referral.referrer-bonus-points:50}")
    private int referrerBonus;

    @Value("${referral.referee-bonus-points:30}")
    private int refereeBonus;

    @Value("${referral.share-base-url:efood://referral?code=}")
    private String shareBaseUrl;

    @Value("${referral.max-referrals-per-day:10}")
    private int maxReferralsPerDay;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public void initReferral(Customer customer, String incomingReferralCode) {
        // 1. Generate and assign a unique code for this new customer if they don't have one.
        if (customer.getReferralCode() == null || customer.getReferralCode().isBlank()) {
            String myCode = generateUniqueCode();
            customer.setReferralCode(myCode);
        }

        Customer referrerToReward = null;

        // 2. If a valid referral code was supplied, link the referrer.
        if (incomingReferralCode != null && !incomingReferralCode.isBlank()) {
            String normalised = incomingReferralCode.trim().toUpperCase();

            // Prevent self-referral (edge case where code was typed from a screenshot).
            if (normalised.equalsIgnoreCase(customer.getReferralCode())) {
                log.warn("Customer tried to self-refer using code {}. Ignored.", customer.getReferralCode());
            } else {
                Optional<Customer> referrerOpt = customerRepo.findByReferralCode(normalised);
                if (referrerOpt.isEmpty()) {
                    referrerOpt = customerRepo.findByReferralCodeIgnoreCase(normalised);
                }
                if (referrerOpt.isPresent()) {
                    Customer referrer = referrerOpt.get();
                    // Prevent duplicate referral linking
                    if (customer.getReferredBy() == null) {
                        customer.setReferredBy(referrer);
                        customer.setReferralRewardCredited(true);
                        referrerToReward = referrer;
                        log.info("Customer #{} was referred by customer #{} (code={})",
                                customer.getId(), referrer.getId(), normalised);
                    }
                } else {
                    log.warn("Referral code '{}' was not found in database. Registration proceeding without referral link.", normalised);
                }
            }
        }

        Customer savedCustomer = customerRepo.save(customer);
        if (savedCustomer == null) {
            savedCustomer = customer;
        }

        // Immediately credit referral bonus points to both referee and referrer on signup
        if (referrerToReward != null && savedCustomer.getId() != null) {
            try {
                loyaltyService.adjustPoints(
                        savedCustomer.getId(),
                        null,
                        refereeBonus,
                        "Referral sign-up bonus (" + refereeBonus + " pts) for joining via code " + referrerToReward.getReferralCode()
                );
                loyaltyService.adjustPoints(
                        referrerToReward.getId(),
                        null,
                        referrerBonus,
                        "Referral reward (" + referrerBonus + " pts) — friend joined with your code " + referrerToReward.getReferralCode()
                );
            } catch (Exception ex) {
                log.error("Failed to credit referral bonus points during signup: {}", ex.getMessage(), ex);
            }
        }
    }

    @Override
    @Transactional
    public ReferralInfoDto getReferralInfo(Long customerIdOrUserId) {
        Customer customer = resolveCustomer(customerIdOrUserId);
        Long customerId = customer.getId();

        // Ensure user has a referral code generated if it was missing
        if (customer.getReferralCode() == null || customer.getReferralCode().isBlank()) {
            customer.setReferralCode(generateUniqueCode());
            customer = customerRepo.save(customer);
        }

        // Count all friends who registered using this customer's code
        long referralCount = customerRepo.countByReferredById(customerId);

        // Points earned from referrals = successfully rewarded referrals * referrerBonus
        long rewardedCount = customerRepo.countReferredAndRewarded(customerId);
        int pointsFromReferrals = (int) (rewardedCount * referrerBonus);

        return ReferralInfoDto.builder()
                .referralCode(customer.getReferralCode())
                .referralCount(referralCount)
                .pointsFromReferrals(pointsFromReferrals)
                .refereeBonus(refereeBonus)
                .referrerBonus(referrerBonus)
                .shareUrl(shareBaseUrl + customer.getReferralCode())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> validateCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String normalised = code.trim().toUpperCase();
        Optional<Customer> opt = customerRepo.findByReferralCode(normalised);
        if (opt.isEmpty()) {
            opt = customerRepo.findByReferralCodeIgnoreCase(code.trim());
        }
        return opt.map(Customer::getId);
    }

    @Override
    @Transactional
    public void creditReferralBonusIfEligible(Long refereeCustomerIdOrUserId, Long orderId) {
        Customer referee = null;
        try {
            referee = resolveCustomer(refereeCustomerIdOrUserId);
        } catch (Exception ignored) {
            return;
        }
        if (referee == null) return;
        Long refereeCustomerId = referee.getId();

        // Only credit once, and only if this customer was referred by someone.
        // referralRewardCredited is set true at signup (initReferral), so this is now a no-op
        // for referees who already got their bonus. It still fires for old rows that signed up
        // before the signup-bonus feature existed.
        if (referee.isReferralRewardCredited() || referee.getReferredBy() == null) return;

        // Check this is the referee's first COMPLETED order.
        long completedOrders = loyaltyTransactionRepo.countByCustomerIdAndType(
                refereeCustomerId, LoyaltyTransactionType.EARNED);

        // If they have exactly 1 EARNED transaction it means we just earned from
        // their very first order in LoyaltyEventListener (which fires first).
        // Guard: credit only when completedOrders == 1 to stay idempotent.
        if (completedOrders != 1) return;

        Customer referrer = referee.getReferredBy();

        // --- Credit referee bonus (fallback for pre-signup-bonus accounts) ---
        loyaltyService.adjustPoints(
                refereeCustomerId,
                orderId,
                refereeBonus,
                "Referral sign-up bonus (" + refereeBonus + " pts) for joining via code " + referrer.getReferralCode()
        );

        // --- Credit referrer bonus ---
        loyaltyService.adjustPoints(
                referrer.getId(),
                orderId,
                referrerBonus,
                "Referral reward (" + referrerBonus + " pts) — friend completed first order #" + orderId
        );

        // Mark as credited to prevent double-crediting.
        referee.setReferralRewardCredited(true);
        customerRepo.save(referee);

        log.info("Referral bonuses credited: referee #{} +{} pts, referrer #{} +{} pts for order #{}",
                refereeCustomerId, refereeBonus, referrer.getId(), referrerBonus, orderId);
    }

    private Customer resolveCustomer(Long customerIdOrUserId) {
        if (customerIdOrUserId == null) {
            throw new IllegalArgumentException("Customer or User ID must not be null");
        }
        // First try finding customer directly by customerId (referenceId)
        Optional<Customer> customerOpt = customerRepo.findById(customerIdOrUserId);
        if (customerOpt.isPresent()) {
            return customerOpt.get();
        }
        // Fallback: try finding customer by appUserId
        return customerRepo.findByAppUserId(customerIdOrUserId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for ID: " + customerIdOrUserId));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomCode();
            if (customerRepo.findByReferralCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique referral code after 20 attempts.");
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
