package com.company.efood.user.services.servicesimpl;

import com.company.efood.config.SSLCommerzConfig;
import com.company.efood.sys.dto.PaymentDto;
import com.company.efood.sys.entity.Order;
import com.company.efood.sys.entity.Payment;
import com.company.efood.sys.repository.OrderRepo;
import com.company.efood.sys.repository.PaymentRepo;
import com.company.efood.sys.utils.OrderStatus;
import com.company.efood.sys.utils.PaymentMethod;
import com.company.efood.sys.utils.PaymentStatus;
import com.company.efood.user.dto.SSLPaymentRequestDto;
import com.company.efood.user.services.PaymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Logger;

@Service
@AllArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final SSLCommerzConfig config;
    private final PaymentRepo paymentRepo;
    private final OrderRepo orderRepo;
    private final com.company.efood.sys.services.OrderFinancialLedgerService financialLedgerService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    @Override
    public String initiatePayment(PaymentDto paymentDto, Long userId) {
        Order order = orderRepo.findById(paymentDto.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));
        PaymentMethod paymentMethod = resolvePaymentMethod(paymentDto.getPaymentMethod());

        Payment payment = paymentRepo.findByOrderId(order.getId()).orElse(new Payment());
        payment.setOrder(order);
        payment.setTransactionId(paymentMethod == PaymentMethod.COD ? "COD-" + order.getId() + "-" + System.currentTimeMillis() : "TXN" + System.currentTimeMillis());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setEntryDate(LocalDateTime.now());
        payment.setEntryUser(userId);
        paymentRepo.save(payment);

        if (paymentMethod == PaymentMethod.COD) {
            order.setStatus(OrderStatus.PLACED);
            orderRepo.save(order);
            return "Cash on delivery selected. Delivery partner will collect the payment on arrival.";
        }

        Logger logger = Logger.getGlobal();
        logger.info("PAYMENT DTO " + paymentDto.getOrderId());
        logger.info("CUSTOMER NAME " + (order.getCustomer() != null ? order.getCustomer().getFullName() : "Customer"));
        SSLPaymentRequestDto request = new SSLPaymentRequestDto();
        request.setTotal_amount(String.valueOf(order.getTotalAmount()));
        request.setTran_id(payment.getTransactionId());
        request.setSuccess_url(resolveCallbackUrl(config.getSuccessUrl(), "/api/private/payment/success"));
        request.setFail_url(resolveCallbackUrl(config.getFailUrl(), "/api/private/payment/fail"));
        request.setCancel_url(resolveCallbackUrl(config.getCancelUrl(), "/api/private/payment/cancel"));
        request.setIpn_url(resolveCallbackUrl(config.getSuccessUrl(), "/api/private/payment/success"));
        request.setCus_name(order.getCustomer() != null && order.getCustomer().getFullName() != null ? order.getCustomer().getFullName() : "Customer");
        request.setCus_phone(order.getCustomer() != null ? order.getCustomer().getPhone() : "");
        request.setCus_email(order.getCustomer() != null ? order.getCustomer().getEmail() : "");
        request.setCus_add1(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getHouseNo() : "");
        request.setCus_city(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getDistrict() : "");
        request.setCus_postcode(order.getDeliveryAddress() != null && order.getDeliveryAddress().getPostCode() != null ? order.getDeliveryAddress().getPostCode().toString() : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.convertValue(request, new TypeReference<>() {});
        body.setAll(map);
        body.add("store_id", config.getStoreId());
        body.add("store_passwd", config.getStorePassword());

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(config.getInitUrl(), httpEntity, Map.class);

        logger.info("SSLCommerz Response: " + response.getBody());

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object gatewayUrl = response.getBody().get("GatewayPageURL");
            return gatewayUrl != null ? gatewayUrl.toString() : "SSLCommerz payment initialized";
        } else {
            throw new RuntimeException("Failed to create SSLCommerz Payment");
        }
    }

    @Transactional
    @Override
    public void handleSuccess(Map<String, String> params) {
        Logger.getGlobal().info("params: " + params);
        String txnId = params.get("tran_id");
        String status = params.get("status");
        String valId = params.get("val_id");

        if (txnId == null || txnId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID is missing");
        }

        // Validate status from gateway callback
        if (status == null || (!status.equalsIgnoreCase("VALID") && !status.equalsIgnoreCase("VALIDATED"))) {
            throw new SecurityException("Payment validation failed: status is " + status);
        }

        Logger.getGlobal().info("Payment Success Verified: " + txnId + " with status: " + status);

        Payment payment = paymentRepo.findByTransactionId(txnId)
                .orElseThrow(() -> new RuntimeException("Payment not found for transaction: " + txnId));

        // Validate amount match if provided
        String callbackAmount = params.get("amount");
        if (callbackAmount != null && !callbackAmount.isBlank() && payment.getAmount() != null) {
            try {
                double paidAmount = Double.parseDouble(callbackAmount.trim());
                if (Math.abs(paidAmount - payment.getAmount().doubleValue()) > 0.01) {
                    throw new SecurityException("Payment amount mismatch. Expected: " + payment.getAmount() + ", received: " + paidAmount);
                }
            } catch (NumberFormatException ignored) {}
        }

        payment.setStatus(PaymentStatus.PAID);
        paymentRepo.save(payment);

        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.PAID);
            orderRepo.save(order);
        }
    }

    @Transactional
    @Override
    public String markCodCollected(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        Payment payment = paymentRepo.findByOrderId(orderId).orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getPaymentMethod() != PaymentMethod.COD) {
            throw new IllegalArgumentException("This order is not using cash on delivery");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setEntryUser(userId);
        paymentRepo.save(payment);

        order.setStatus(OrderStatus.COMPLETED);
        order.setDeliveredAt(LocalDateTime.now());
        Order savedOrder = orderRepo.save(order);

        try {
            financialLedgerService.recordOrderCompletion(savedOrder);
        } catch (Exception e) {
            Logger.getGlobal().warning("Failed to record financial ledger on COD collect: " + e.getMessage());
        }

        return "Cash collected successfully and order completed";
    }

    private PaymentMethod resolvePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return PaymentMethod.COD;
        }

        String normalized = paymentMethod.trim().toUpperCase();
        return switch (normalized) {
            case "COD" -> PaymentMethod.COD;
            case "CARD" -> PaymentMethod.CARD;
            case "BKASH" -> PaymentMethod.BKASH;
            case "NAGAD" -> PaymentMethod.NAGAD;
            case "SSLCOMMERZ", "SSL", "SSL_COMMERZ" -> PaymentMethod.SSLCOMMERZ;
            default -> PaymentMethod.COD;
        };
    }

    private String resolveCallbackUrl(String configuredUrl, String fallbackPath) {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return "http://192.168.0.100:8080" + fallbackPath;
    }
}
