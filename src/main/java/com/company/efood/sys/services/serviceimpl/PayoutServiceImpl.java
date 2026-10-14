package com.company.efood.sys.services.serviceimpl;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.repository.RaiderRepo;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.sys.dto.PayoutAccountDto;
import com.company.efood.sys.dto.PayoutDisbursementDto;
import com.company.efood.sys.entity.Order;
import com.company.efood.sys.entity.OrderFinancialLedger;
import com.company.efood.sys.entity.PayoutAccount;
import com.company.efood.sys.entity.PayoutDisbursement;
import com.company.efood.sys.repository.OrderFinancialLedgerRepo;
import com.company.efood.sys.repository.OrderRepo;
import com.company.efood.sys.repository.PayoutAccountRepo;
import com.company.efood.sys.repository.PayoutDisbursementRepo;
import com.company.efood.sys.services.PayoutService;
import com.company.efood.sys.services.RiderPayoutPolicyService;
import com.company.efood.sys.utils.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutAccountRepo payoutAccountRepo;
    private final PayoutDisbursementRepo payoutDisbursementRepo;
    private final OrderFinancialLedgerRepo ledgerRepo;
    private final OrderRepo orderRepo;
    private final SellerRepo sellerRepo;
    private final RaiderRepo raiderRepo;
    private final RiderPayoutPolicyService riderPayoutPolicyService;
    private final com.company.efood.sys.services.OrderFinancialLedgerService financialLedgerService;
    private final BaseUtils baseUtils;

    @Override
    @Transactional(readOnly = true)
    public PayoutAccountDto getPayoutAccount(Long userId, String userType) {
        if (userId == null || userType == null) {
            return null;
        }
        return payoutAccountRepo.findByUserIdAndUserType(userId, userType.toUpperCase())
                .map(this::toAccountDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public PayoutAccountDto savePayoutAccount(Long userId, String userType, PayoutAccountDto dto) {
        if (userId == null || userType == null) {
            throw new IllegalArgumentException("User ID and user type are required");
        }
        String normalizedType = userType.trim().toUpperCase();

        PayoutAccount account = payoutAccountRepo.findByUserIdAndUserType(userId, normalizedType)
                .orElse(new PayoutAccount());

        account.setUserId(userId);
        account.setUserType(normalizedType);
        String rawMethod = dto.getPayoutMethod() != null ? dto.getPayoutMethod().trim().toUpperCase() : null;
        if (rawMethod == null && dto.getChannelType() != null) {
            rawMethod = dto.getChannelType().trim().toUpperCase();
        }
        if (rawMethod == null) rawMethod = "MFS";
        boolean isBank = "BANK".equalsIgnoreCase(rawMethod);
        account.setPayoutMethod(isBank ? "BANK" : "MFS");

        String holderName = dto.getAccountHolderName() != null && !dto.getAccountHolderName().isBlank()
                ? dto.getAccountHolderName().trim() : (dto.getAccountName() != null ? dto.getAccountName().trim() : null);

        if (isBank) {
            account.setBankName(dto.getBankName() != null ? dto.getBankName().trim() : null);
            account.setAccountHolderName(holderName);
            account.setAccountNumber(dto.getAccountNumber() != null ? dto.getAccountNumber().trim() : null);
            account.setRoutingNumber(dto.getRoutingNumber() != null ? dto.getRoutingNumber().trim() : null);
            account.setBranchName(dto.getBranchName() != null ? dto.getBranchName().trim() : null);
        } else {
            // MFS (bKash, Nagad, Rocket)
            String provider = dto.getMfsProvider() != null && !dto.getMfsProvider().isBlank()
                    ? dto.getMfsProvider().trim() : (dto.getChannelType() != null ? dto.getChannelType().trim() : "bKash");
            String num = dto.getMfsNumber() != null && !dto.getMfsNumber().isBlank()
                    ? dto.getMfsNumber().trim() : (dto.getAccountNumber() != null ? dto.getAccountNumber().trim() : null);
            account.setMfsProvider(provider);
            account.setMfsNumber(num);
            account.setAccountNumber(num);
            account.setAccountHolderName(holderName);
            account.setMfsAccountType(dto.getMfsAccountType() != null ? dto.getMfsAccountType().trim() : "Personal");
        }

        account.setIsVerified(true);
        account.setActive(true);
        if (account.getId() == null) {
            account.setEntryUser(userId);
            account.setEntryDate(LocalDateTime.now());
        } else {
            account.setUpdateUser(userId);
            account.setUpdateDate(LocalDateTime.now());
        }

        PayoutAccount saved = payoutAccountRepo.save(account);
        return toAccountDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWeeklySettlements(String weekLabel) {
        return getWeeklySettlements(weekLabel, null);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getWeeklySettlements(String weekLabel, Integer weekOffset) {
        // Automatically backfill any completed orders that might be missing a ledger entry
        try {
            financialLedgerService.backfillPendingCompletedOrders();
        } catch (Exception e) {
            log.warn("[Payout] Auto-backfill non-fatal error: {}", e.getMessage());
        }

        LocalDate now = LocalDate.now();
        if (weekOffset != null) {
            now = now.plusWeeks(weekOffset);
        }
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        LocalDate start;
        LocalDate end;

        if (weekLabel != null && !weekLabel.isBlank() && weekLabel.contains("-W")) {
            String[] parts = weekLabel.split("-W");
            int year = Integer.parseInt(parts[0]);
            int week = Integer.parseInt(parts[1]);
            start = LocalDate.ofYearDay(year, 1)
                    .with(weekFields.weekOfYear(), week)
                    .with(DayOfWeek.MONDAY);
            end = start.plusDays(6);
        } else {
            start = now.with(DayOfWeek.MONDAY);
            end = start.plusDays(6);
            int weekNumber = now.get(weekFields.weekOfYear());
            weekLabel = String.format("%d-W%02d", now.getYear(), weekNumber);
        }

        final String effectiveWeekLabel = weekLabel;
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay().minusNanos(1);
        final boolean isCurrentOrInstant = (weekOffset == null || weekOffset == 0);

        List<Map<String, Object>> settlements = new ArrayList<>();

        // 1. Process Sellers
        List<Seller> sellers = sellerRepo.findAll();
        for (Seller seller : sellers) {
            Long sellerUserId = seller.getAppUser() != null ? seller.getAppUser().getId() : seller.getId();
            String sellerName = seller.getFullName() != null && !seller.getFullName().isBlank()
                    ? seller.getFullName()
                    : (seller.getShop() != null ? seller.getShop().getShopName() : "Seller #" + seller.getId());

            List<OrderFinancialLedger> sellerLedgers = ledgerRepo.findAll().stream()
                    .filter(l -> l.getShop() != null && l.getShop().getSeller() != null
                            && Objects.equals(l.getShop().getSeller().getId(), seller.getId())
                            && l.getCompletedAt() != null
                            && (isCurrentOrInstant ? !l.getCompletedAt().isAfter(endDt) : (!l.getCompletedAt().isBefore(startDt) && !l.getCompletedAt().isAfter(endDt))))
                    .collect(Collectors.toList());

            BigDecimal totalSellerGross = sellerLedgers.stream()
                    .map(OrderFinancialLedger::getProductSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalCommissionDeducted = sellerLedgers.stream()
                    .map(OrderFinancialLedger::getSellerCommissionAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal netPayable = sellerLedgers.stream()
                    .map(OrderFinancialLedger::getSellerNetEarnings)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            // Fetch payout account
            PayoutAccount account = payoutAccountRepo.findByUserIdAndUserType(sellerUserId, "SELLER").orElse(null);

            // Fetch disbursement status
            Optional<PayoutDisbursement> existingDisbursement =
                    payoutDisbursementRepo.findByBeneficiaryUserIdAndBeneficiaryTypeAndWeekLabel(sellerUserId, "SELLER", effectiveWeekLabel);

            Map<String, Object> item = new HashMap<>();
            // Frontend properties
            item.put("userId", sellerUserId);
            item.put("displayName", sellerName);
            item.put("totalAmount", netPayable);
            item.put("disbursementStatus", existingDisbursement.map(PayoutDisbursement::getStatus).orElse("PENDING"));
            item.put("disbursementId", existingDisbursement.map(PayoutDisbursement::getId).orElse(null));

            // Backend audit properties
            item.put("beneficiaryUserId", sellerUserId);
            item.put("beneficiaryId", seller.getId());
            item.put("beneficiaryName", sellerName);
            item.put("beneficiaryType", "SELLER");
            item.put("shopName", seller.getShop() != null ? seller.getShop().getShopName() : "N/A");
            item.put("phone", seller.getPhone());
            item.put("weekLabel", effectiveWeekLabel);
            item.put("periodStart", start.toString());
            item.put("periodEnd", end.toString());
            item.put("completedOrdersCount", sellerLedgers.size());
            item.put("grossEarnings", totalSellerGross);
            item.put("commissionDeducted", totalCommissionDeducted);
            item.put("netPayable", netPayable);
            item.put("payoutAccount", account != null ? toAccountDto(account) : null);
            item.put("status", existingDisbursement.map(PayoutDisbursement::getStatus).orElse("PENDING"));
            item.put("transactionRef", existingDisbursement.map(PayoutDisbursement::getTransactionRef).orElse(null));
            item.put("disbursedAt", existingDisbursement.map(PayoutDisbursement::getDisbursedAt).orElse(null));

            settlements.add(item);
        }

        // 2. Process Riders
        List<Raider> raiders = raiderRepo.findAll();
        for (Raider raider : raiders) {
            Long riderUserId = raider.getAppUser() != null ? raider.getAppUser().getId() : raider.getId();
            String riderName = raider.getFullName() != null && !raider.getFullName().isBlank()
                    ? raider.getFullName()
                    : "Rider #" + raider.getId();

            List<OrderFinancialLedger> riderLedgers = ledgerRepo.findAll().stream()
                    .filter(l -> l.getRaider() != null
                            && Objects.equals(l.getRaider().getId(), raider.getId())
                            && l.getCompletedAt() != null
                            && (isCurrentOrInstant ? !l.getCompletedAt().isAfter(endDt) : (!l.getCompletedAt().isBefore(startDt) && !l.getCompletedAt().isAfter(endDt))))
                    .collect(Collectors.toList());

            BigDecimal totalDeliveryFeePool = riderLedgers.stream()
                    .map(OrderFinancialLedger::getDeliveryFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal netPayable = riderLedgers.stream()
                    .map(OrderFinancialLedger::getRiderDisbursementAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            // Fetch payout account (check both RAIDER and RIDER)
            PayoutAccount account = payoutAccountRepo.findByUserIdAndUserType(riderUserId, "RAIDER")
                    .or(() -> payoutAccountRepo.findByUserIdAndUserType(riderUserId, "RIDER"))
                    .orElse(null);

            // Fetch disbursement status (check both RAIDER and RIDER)
            Optional<PayoutDisbursement> existingDisbursement =
                    payoutDisbursementRepo.findByBeneficiaryUserIdAndBeneficiaryTypeAndWeekLabel(riderUserId, "RAIDER", effectiveWeekLabel)
                            .or(() -> payoutDisbursementRepo.findByBeneficiaryUserIdAndBeneficiaryTypeAndWeekLabel(riderUserId, "RIDER", effectiveWeekLabel));

            Map<String, Object> item = new HashMap<>();
            // Frontend properties
            item.put("userId", riderUserId);
            item.put("displayName", riderName);
            item.put("totalAmount", netPayable);
            item.put("disbursementStatus", existingDisbursement.map(PayoutDisbursement::getStatus).orElse("PENDING"));
            item.put("disbursementId", existingDisbursement.map(PayoutDisbursement::getId).orElse(null));

            // Backend audit properties
            item.put("beneficiaryUserId", riderUserId);
            item.put("beneficiaryId", raider.getId());
            item.put("beneficiaryName", riderName);
            item.put("beneficiaryType", "RIDER");
            item.put("vehicleType", raider.getVehicleType() != null ? raider.getVehicleType() : "BIKE");
            item.put("phone", raider.getPhone());
            item.put("weekLabel", effectiveWeekLabel);
            item.put("periodStart", start.toString());
            item.put("periodEnd", end.toString());
            item.put("completedOrdersCount", riderLedgers.size());
            item.put("deliveryFeePool", totalDeliveryFeePool);
            item.put("grossEarnings", totalDeliveryFeePool);
            item.put("netPayable", netPayable);
            item.put("payoutAccount", account != null ? toAccountDto(account) : null);
            item.put("status", existingDisbursement.map(PayoutDisbursement::getStatus).orElse("PENDING"));
            item.put("transactionRef", existingDisbursement.map(PayoutDisbursement::getTransactionRef).orElse(null));
            item.put("disbursedAt", existingDisbursement.map(PayoutDisbursement::getDisbursedAt).orElse(null));

            settlements.add(item);
        }

        return settlements;
    }

    @Override
    @Transactional
    public PayoutDisbursementDto executeDisbursement(Long adminUserId, PayoutDisbursementDto dto) {
        if (dto.getBeneficiaryUserId() == null || dto.getBeneficiaryType() == null) {
            throw new IllegalArgumentException("Beneficiary User ID and Type are required");
        }

        String rawType = dto.getBeneficiaryType().trim().toUpperCase();
        String type = (rawType.equals("RIDER") || rawType.equals("RAIDER")) ? "RIDER" : rawType;
        String weekLabel = dto.getWeekLabel();
        if (weekLabel == null || weekLabel.isBlank()) {
            weekLabel = "INSTANT-" + LocalDate.now();
        }
        final String effectiveWeekLabel = weekLabel;

        PayoutDisbursement record = payoutDisbursementRepo
                .findByBeneficiaryUserIdAndBeneficiaryTypeAndWeekLabel(dto.getBeneficiaryUserId(), type, effectiveWeekLabel)
                .or(() -> payoutDisbursementRepo.findByBeneficiaryUserIdAndBeneficiaryTypeAndWeekLabel(dto.getBeneficiaryUserId(), type.equals("RIDER") ? "RAIDER" : "RIDER", effectiveWeekLabel))
                .orElse(new PayoutDisbursement());

        record.setBeneficiaryUserId(dto.getBeneficiaryUserId());
        record.setBeneficiaryName(dto.getBeneficiaryName());
        record.setBeneficiaryType(type);
        record.setWeekLabel(effectiveWeekLabel);
        record.setPeriodStart(dto.getPeriodStart() != null ? dto.getPeriodStart() : LocalDate.now().minusDays(7));
        record.setPeriodEnd(dto.getPeriodEnd() != null ? dto.getPeriodEnd() : LocalDate.now());
        record.setTotalEarnings(dto.getTotalEarnings() != null ? dto.getTotalEarnings().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        record.setPayoutMethod(dto.getPayoutMethod());
        record.setPayoutDestination(dto.getPayoutDestination());
        record.setStatus("DISBURSED");
        record.setTransactionRef(dto.getTransactionRef() != null && !dto.getTransactionRef().isBlank() ? dto.getTransactionRef().trim() : "TXN-" + System.currentTimeMillis());
        record.setDisbursedAt(LocalDateTime.now());
        record.setDisbursedBy(adminUserId != null ? adminUserId : 0L);
        record.setNotes(dto.getNotes());
        record.setActive(true);

        if (record.getId() == null) {
            record.setEntryUser(adminUserId != null ? adminUserId : 0L);
            record.setEntryDate(LocalDateTime.now());
        } else {
            record.setUpdateUser(adminUserId != null ? adminUserId : 0L);
            record.setUpdateDate(LocalDateTime.now());
        }

        PayoutDisbursement saved = payoutDisbursementRepo.save(record);
        return toDisbursementDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutDisbursementDto> getDisbursementHistory(BasePageableRequest request) {
        PageRequest pageRequest = baseUtils.getPageRequest(request.getPage(), request.getSize());
        Page<PayoutDisbursement> page = payoutDisbursementRepo.findAllByOrderByDisbursedAtDesc(pageRequest);
        List<PayoutDisbursementDto> dtos = page.stream().map(this::toDisbursementDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageRequest, page.getTotalElements());
    }

    private PayoutAccountDto toAccountDto(PayoutAccount entity) {
        PayoutAccountDto dto = new PayoutAccountDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUserType(entity.getUserType());
        dto.setPayoutMethod(entity.getPayoutMethod());
        dto.setBankName(entity.getBankName());
        dto.setAccountHolderName(entity.getAccountHolderName());
        dto.setAccountName(entity.getAccountHolderName());
        dto.setAccountNumber(entity.getAccountNumber() != null ? entity.getAccountNumber() : entity.getMfsNumber());
        dto.setRoutingNumber(entity.getRoutingNumber());
        dto.setBranchName(entity.getBranchName());
        dto.setMfsProvider(entity.getMfsProvider());
        dto.setMfsNumber(entity.getMfsNumber() != null ? entity.getMfsNumber() : entity.getAccountNumber());
        dto.setMfsAccountType(entity.getMfsAccountType());
        dto.setChannelType("BANK".equalsIgnoreCase(entity.getPayoutMethod()) ? "BANK" : (entity.getMfsProvider() != null ? entity.getMfsProvider().toUpperCase() : "BKASH"));
        dto.setIsVerified(entity.getIsVerified());
        dto.setActive(entity.getActive());
        return dto;
    }

    private PayoutDisbursementDto toDisbursementDto(PayoutDisbursement entity) {
        PayoutDisbursementDto dto = new PayoutDisbursementDto();
        dto.setId(entity.getId());
        dto.setBeneficiaryUserId(entity.getBeneficiaryUserId());
        dto.setBeneficiaryName(entity.getBeneficiaryName());
        dto.setBeneficiaryType(entity.getBeneficiaryType());
        dto.setWeekLabel(entity.getWeekLabel());
        dto.setPeriodStart(entity.getPeriodStart());
        dto.setPeriodEnd(entity.getPeriodEnd());
        dto.setTotalEarnings(entity.getTotalEarnings() != null ? entity.getTotalEarnings().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        dto.setPayoutMethod(entity.getPayoutMethod());
        dto.setPayoutDestination(entity.getPayoutDestination());
        dto.setStatus(entity.getStatus());
        dto.setTransactionRef(entity.getTransactionRef());
        dto.setDisbursedAt(entity.getDisbursedAt());
        dto.setDisbursedBy(entity.getDisbursedBy());
        dto.setNotes(entity.getNotes());
        dto.setActive(entity.getActive());
        return dto;
    }
}
