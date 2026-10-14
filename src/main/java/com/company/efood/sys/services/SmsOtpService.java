package com.company.efood.sys.services;

/**
 * SMS OTP gateway abstraction.
 * <p>
 * In test/dev mode the implementation skips actual SMS delivery so the
 * OTP can be read directly from the database.  In production the
 * implementation calls the real SMS-gateway API configured via
 * {@code sms.gateway.*} properties.
 */
public interface SmsOtpService {

    /**
     * Send a 6-digit OTP to {@code phoneNumber}.
     *
     * @return {@code true} if the SMS was actually dispatched to the gateway,
     *         {@code false} in dev/test mode (OTP is still stored in DB).
     */
    boolean sendOtp(String phoneNumber, String otp);
}
