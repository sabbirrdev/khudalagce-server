package com.company.efood.sys.services;

import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.entity.UserRoleAssignDetails;
import com.company.efood.sys.model.CustomUserDetails;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.repository.UserRoleAssignDetailsRepo;
import com.company.efood.sys.repository.UserRoleAssignMasterRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import com.company.efood.user.repository.CustomerRepo;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.raider.repository.RaiderRepo;
import com.company.efood.sys.utils.AppUserType;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepo appUserRepo;
    private final UserRoleAssignMasterRepo userRoleAssignMasterRepo;
    private final UserRoleAssignDetailsRepo userRoleAssignDetailsRepo;
    private final CustomerRepo customerRepo;
    private final SellerRepo sellerRepo;
    private final RaiderRepo raiderRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final AppUser appUser = appUserRepo.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User does't Exist"));

        if (!appUser.getActive()) {
            System.out.println("User is not active with username: " + username);
            throw new InternalAuthenticationServiceException("User is not active with username: " + username);
        }

        if (appUser.getAccountExpired()) {
            System.out.println("User is expired with username: " + username);
            throw new InternalAuthenticationServiceException("User is expired with username: " + username);
        }

        if (appUser.getAccountLocked()) {
            System.out.println("User is locked with username: " + username);
            throw new InternalAuthenticationServiceException("User is locked with username: " + username);
        }

        if (appUser.getCredentialsExpired()) {
            System.out.println("User credentials expired with username: " + username);
            throw new InternalAuthenticationServiceException("User credentials expired with username: " + username);
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        List<UserRoleAssignDetails> userRoleAssignDetails = userRoleAssignDetailsRepo.findByMasterAppUserId(appUser.getId());
        for (UserRoleAssignDetails userRoleAssignDetail : userRoleAssignDetails) {
            authorities.add(new SimpleGrantedAuthority(userRoleAssignDetail.getUserRole().getName()));
        }

        // Guarantee role-specific entity reference is populated so referenceId in CustomUserDetails is never null
        if (appUser.getAppUserType() != null) {
            switch (appUser.getAppUserType()) {
                case CUSTOMER -> {
                    if (appUser.getCustomer() == null && customerRepo != null) {
                        customerRepo.findByAppUserId(appUser.getId()).ifPresent(appUser::setCustomer);
                    }
                }
                case SELLER -> {
                    if (appUser.getSeller() == null && sellerRepo != null) {
                        sellerRepo.findByAppUserId(appUser.getId()).ifPresent(appUser::setSeller);
                    }
                }
                case RAIDER -> {
                    if (appUser.getRaider() == null && raiderRepo != null) {
                        raiderRepo.findByAppUserId(appUser.getId()).ifPresent(appUser::setRaider);
                    }
                }
            }
        }

        return CustomUserDetails.build(appUser);
    }

}


