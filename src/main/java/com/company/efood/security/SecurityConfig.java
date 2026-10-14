package com.company.efood.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JWTEntryPoint jwtEntryPoint;
    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;
    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    @Autowired
    private FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    /** Allow all origins for every HTTP method — tighten in production. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Preflight (OPTIONS) requests must pass without a token
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Auth & public
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/private/files/view/**", "/api/private/files/upload").permitAll()
                        // FCM token registration (needed early, even before login succeeds on device)
                        .requestMatchers("/api/tokens/**").permitAll()
                        .requestMatchers("/notification/tokens/**").permitAll()
                        .requestMatchers(
                                "/api/private/payment/success",
                                "/api/private/payment/fail",
                                "/api/private/payment/cancel",
                                "/api/private/payment/ipn"
                        ).permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Profile endpoint for any authenticated user
                        .requestMatchers("/api/private/sya/app-user/my-profile").authenticated()
                        // Seller APIs
                        .requestMatchers("/api/private/seller/**").hasAnyRole("SYSTEM_ADMIN", "SELLER")
                        // Admin system management (categories, shops, offers, zones, commission, delivery fee)
                        .requestMatchers("/api/private/sya/**").hasAnyRole("SYSTEM_ADMIN", "DEVELOPER", "SELLER")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                );

        // Firebase Auth & JWT filters
        http.addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
