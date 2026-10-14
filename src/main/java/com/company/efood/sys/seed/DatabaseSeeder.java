package com.company.efood.sys.seed;

import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.entity.PasswordPolicy;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.repository.PasswordPolicyRepo;
import com.company.efood.sys.utils.AppUserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final AppUserRepo appUserRepo;
    private final PasswordPolicyRepo passwordPolicyRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("🚀 [DatabaseSeeder] Initializing default password policy and system super admin...");

        PasswordPolicy defaultPolicy = seedDefaultPasswordPolicy();
        seedSystemSuperAdmin(defaultPolicy);

        log.info("✅ [DatabaseSeeder] Base admin credentials and password policy ready.");
    }

    private PasswordPolicy seedDefaultPasswordPolicy() {
        final String policyName = "Simple Policy";
        return passwordPolicyRepo.findByName(policyName).orElseGet(() -> {
            PasswordPolicy policy = new PasswordPolicy();
            policy.setName(policyName);
            policy.setMinLength(5);
            policy.setPasswordAge(60);
            policy.setSequential(false);
            policy.setSpecialChar(false);
            policy.setAlphanumeric(false);
            policy.setUpperLower(false);
            policy.setMatchUsername(false);
            policy.setActive(true);
            policy.setIsApproved(true);
            policy.setEntryUser(0L);
            policy.setEntryDate(LocalDateTime.now());
            PasswordPolicy saved = passwordPolicyRepo.save(policy);
            log.info("🌱 [DatabaseSeeder] Seeded default password policy: '{}'", policyName);
            return saved;
        });
    }

    private void seedSystemSuperAdmin(PasswordPolicy policy) {
        final String adminUsername = "admin";
        AppUser adminUser = appUserRepo.findByUsername(adminUsername).orElseGet(AppUser::new);
        adminUser.setUsername(adminUsername);
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setDisplayName("System Admin");
        adminUser.setAppUserType(AppUserType.SYSTEM_ADMIN);
        adminUser.setUserTypeId(0);
        adminUser.setPasswordPolicy(policy);
        adminUser.setActive(true);
        adminUser.setIsApproved(true);
        adminUser.setPhoneVerified(true);
        adminUser.setEmailVerified(true);
        adminUser.setAccountExpired(false);
        adminUser.setAccountLocked(false);
        adminUser.setCredentialsExpired(false);
        if (adminUser.getId() == null) {
            adminUser.setEntryUser(0L);
            adminUser.setEntryDate(LocalDateTime.now());
        } else {
            adminUser.setUpdateDate(LocalDateTime.now());
        }

        appUserRepo.save(adminUser);
        log.info("🌱 [DatabaseSeeder] Super Admin [admin / admin123] successfully configured.");
    }
}
