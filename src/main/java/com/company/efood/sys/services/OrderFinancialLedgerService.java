package com.company.efood.sys.services;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.sys.dto.OrderFinancialLedgerDto;
import com.company.efood.sys.entity.Order;
import com.company.efood.sys.entity.OrderFinancialLedger;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface OrderFinancialLedgerService {
    OrderFinancialLedger recordOrderCompletion(Order order);
    void backfillPendingCompletedOrders();
    Page<OrderFinancialLedgerDto> getLedgerEntries(BasePageableRequest request);
    Map<String, Object> getFinancialSummary();
}
