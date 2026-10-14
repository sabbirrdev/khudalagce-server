package com.company.efood.sys.services.serviceimpl;

import com.company.efood.sys.services.SmsOtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * SMS OTP service implementation.
 *
 * <p>When {@code sms.gateway.enabled} is {@code false} (the default in local/dev
 * mode) the method returns {@code false} without making any HTTP call.
 * The generated OTP is already persisted to the database before this service
 * is called, so testers can look it up in the {@code SYA_APP_USER.OTP} column.
 *
 * <p>In production set:
 * <pre>
 *   sms.gateway.enabled=true
 *   sms.gateway.url=https://your-gateway-api/send
 *   sms.gateway.api-key=YOUR_API_KEY
 *   sms.gateway.sender-id=YourSenderID
 * </pre>
 */
@Slf4j
@Service
public class SmsOtpServiceImpl implements SmsOtpService {

    @Value("${sms.gateway.enabled:false}")
    private boolean smsEnabled;

    @Value("${sms.gateway.url:}")
    private String gatewayUrl;

    @Value("${sms.gateway.api-key:}")
    private String apiKey;

    @Value("${sms.gateway.sender-id:EFood}")
    private String senderId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean sendOtp(String phoneNumber, String otp) {
        if (!smsEnabled) {
            log.info("📵 [SMS OTP] SMS disabled (dev/test mode). OTP for {} is in DB — NOT sent via SMS.", phoneNumber);
            return false;
        }

        try {
            String message = "Your EFood verification code is: " + otp
                    + ". This code expires in 5 minutes. Do not share it with anyone.";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", phoneNumber);
            payload.put("from", senderId);
            payload.put("message", message);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(gatewayUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [SMS OTP] OTP sent to {} via gateway.", phoneNumber);
                return true;
            } else {
                log.warn("⚠️ [SMS OTP] Gateway returned non-2xx status {} for {}.", response.getStatusCode(), phoneNumber);
                return false;
            }
        } catch (Exception ex) {
            log.error("❌ [SMS OTP] Failed to send OTP to {}: {}", phoneNumber, ex.getMessage());
            return false;
        }
    }
}
