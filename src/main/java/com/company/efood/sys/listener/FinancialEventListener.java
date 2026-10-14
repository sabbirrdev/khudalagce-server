package com.company.efood.sys.listener;

import com.company.efood.sys.event.OrderStatusChangedEvent;
import com.company.efood.sys.repository.OrderRepo;
import com.company.efood.sys.services.OrderFinancialLedgerService;
import com.company.efood.sys.utils.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialEventListener {

    private final OrderFinancialLedgerService financialLedgerService;
    private final OrderRepo orderRepo;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChange(OrderStatusChangedEvent event) {
        OrderStatus newStatus = event.getNewStatus();
        Long orderId = event.getOrderId();

        // Record financial ledger when order reaches DELIVERED or COMPLETED
        if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.COMPLETED) {
            log.info("[Financial] Order #{} reached status {} — recording financial ledger", orderId, newStatus);
            try {
                orderRepo.findById(orderId).ifPresentOrElse(
                        order -> financialLedgerService.recordOrderCompletion(order),
                        () -> log.warn("[Financial] Order #{} not found, skipping ledger recording", orderId)
                );
            } catch (Exception e) {
                log.error("[Financial] Failed to record financial ledger for order #{}: {}", orderId, e.getMessage(), e);
            }
        }
    }
}
