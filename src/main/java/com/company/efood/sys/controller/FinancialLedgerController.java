package com.company.efood.sys.controller;

import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.dto.OrderFinancialLedgerDto;
import com.company.efood.sys.services.OrderFinancialLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.company.efood.base.BaseConstants.*;

@RestController
@RequestMapping(SYSTEM_ADMIN_END_POINT + "ledger")
@RequiredArgsConstructor
public class FinancialLedgerController {

    private final OrderFinancialLedgerService ledgerService;
    private final BaseUtils baseUtils;

    @PostMapping("/entries")
    public BaseResponse getLedgerEntries(@RequestBody BasePageableRequest request) {
        try {
            Page<OrderFinancialLedgerDto> page = ledgerService.getLedgerEntries(request);
            return baseUtils.generateSuccessResponse(page, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping("/summary")
    public BaseResponse getFinancialSummary() {
        try {
            Map<String, Object> summary = ledgerService.getFinancialSummary();
            return baseUtils.generateSuccessResponse(summary, PROCESS_COMPLETE, PROCESS_COMPLETE_BN);
        } catch (Exception e) {
            return baseUtils.generateErrorResponse(e);
        }
    }
}
