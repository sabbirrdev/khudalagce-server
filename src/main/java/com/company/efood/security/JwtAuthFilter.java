package com.company.efood.security;

import com.company.efood.config.CurrentUserContext;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.model.CurrentUserInfo;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.services.CustomUserDetailsService;
import com.company.efood.sys.utils.AppUserType;
import com.company.efood.sys.utils.AuthTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthTokenUtils authTokenUtils;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private AppUserRepo appUserRepo;

    /**
     * Returns true if the URI is always accessible regardless of phone-verification status.
     * Covers all OTP, auth, public, token-registration and static file endpoints.
     */
    private boolean isOtpOrAuthEndpoint(String uri) {
        return uri.startsWith("/api/auth/")
                || uri.startsWith("/api/public/")
                || uri.startsWith("/api/tokens/")
                || uri.startsWith("/notification/tokens/")
                || uri.startsWith("/api/private/files/view/")
                || uri.startsWith("/api/private/payment/")
                || uri.endsWith("/profile")
                || uri.endsWith("/my-profile")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String requestUri = request.getRequestURI();
        final String method = request.getMethod();

        try {
            String token = authTokenUtils.getTokenFromRequest(request);
            if (token != null) {
                boolean isValid = authTokenUtils.validateJwtToken(token);
                if (isValid) {
                    // 🔐 Extract fields from JWT
                    String username = authTokenUtils.getUserNameFromJwtToken(token);
                    Long userId = authTokenUtils.getUserIdFromJwtToken(token);
                    AppUserType role = authTokenUtils.getRoleFromJwtToken(token);
                    Long referenceId = authTokenUtils.getReferenceIdFromJwtToken(token);
                    Long userTypeId = authTokenUtils.getUserTypeIdFromJwtToken(token);
                    String displayName = authTokenUtils.getDisplayNameFromJwtToken(token);

                    System.out.println("✅ [JWT FILTER SUCCESS] Request: " + method + " " + requestUri + " | User: " + username + " (ID: " + userId + ", Role: " + role + ")");

                    // 🚫 PHONE VERIFICATION GATE: Block unverified users from accessing private APIs
                    // SYSTEM_ADMIN and DEVELOPER are exempt from this check
                    if (role != AppUserType.SYSTEM_ADMIN && role != AppUserType.DEVELOPER && !isOtpOrAuthEndpoint(requestUri)) {
                        AppUser appUser = appUserRepo.findByUsername(username).orElse(null);
                        if (appUser != null && !Boolean.TRUE.equals(appUser.getPhoneVerified())) {
                            System.out.println("🔒 [JWT FILTER] Phone NOT verified for user: " + username + " | Blocking request: " + method + " " + requestUri);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                "{\"status\":false,\"statusCode\":403,\"message\":\"Phone verification required. Please verify your phone number to continue.\","
                                + "\"messageBn\":\"মোবাইল নম্বর যাচাই প্রয়োজন। অনুগ্রহ করে আপনার ফোন নম্বর যাচাই করুন।\","
                                + "\"data\":{\"requiresOtpVerification\":true,\"phone\":\"" + (appUser.getPhone() != null ? appUser.getPhone() : username) + "\"}}"
                            );
                            return; // Stop filter chain — do NOT continue to the controller
                        }
                    }

                    // 👤 Set Current User Context
                    CurrentUserInfo currentUser = new CurrentUserInfo(
                            userId,
                            username,
                            role,
                            referenceId,
                            userTypeId,
                            displayName
                    );
                    CurrentUserContext.set(currentUser);

                    // ✅ Spring Security authentication
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    System.out.println("⚠️ [JWT FILTER INVALID TOKEN] Request: " + method + " " + requestUri + " | Token provided but failed JWT validation!");
                }
            } else {
                System.out.println("ℹ️ [JWT FILTER NO TOKEN] Request: " + method + " " + requestUri + " | No Authorization header present");
            }

        } catch (Exception e) {
            System.out.println("❌ [JWT FILTER EXCEPTION] Request: " + method + " " + requestUri + " | Error: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
        // Clear after request completes (optional, for safety)
        CurrentUserContext.clear();
    }
}
