package com.company.efood.sys.services.serviceimpl;
import com.company.efood.base.BaseDropdownModel;
import com.company.efood.base.BasePageableRequest;
import com.company.efood.base.BaseUtils;
import com.company.efood.user.entity.Customer;
import com.company.efood.user.repository.CustomerRepo;
import com.company.efood.raider.entity.Raider;
import com.company.efood.raider.repository.RaiderRepo;
import com.company.efood.seller.entity.Seller;
import com.company.efood.seller.repository.SellerRepo;
import com.company.efood.sys.dto.AppUserDto;
import com.company.efood.sys.entity.AppUser;
import com.company.efood.sys.entity.PasswordPolicy;
import com.company.efood.sys.repository.AppUserRepo;
import com.company.efood.sys.repository.PasswordPolicyRepo;
import com.company.efood.sys.services.ApplicationUserService;
import com.company.efood.sys.utils.AppUserType;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.naming.NameNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.company.efood.base.BaseConstants.USER_TYPE_ID_CUSTOMER;

@Service
@AllArgsConstructor
public class ApplicationUserServiceImpl implements ApplicationUserService {

    private AppUserRepo appUserRepo;
    private ModelMapper modelMapper;
    private BaseUtils baseUtils;
    private PasswordEncoder encoder;
    private PasswordPolicyRepo passwordPolicyRepo;
    private SellerRepo sellerRepo;
    private RaiderRepo raiderRepo;
    private CustomerRepo customerRepo;



    @Transactional
    @Override
    public AppUserDto save(AppUserDto appUserDto, Long userId) {
        AppUser savedEntity = appUserRepo.save(generateAppUserEntity(appUserDto, userId, true));
        Map<AppUserType, Consumer<AppUser>> userTypeHandlers = Map.of(
                AppUserType.CUSTOMER, user -> customerRepo.save(new Customer(user)),
                AppUserType.SELLER, user -> sellerRepo.save(new Seller(user)),
                AppUserType.RAIDER, user -> raiderRepo.save(new Raider(user))
        );

        userTypeHandlers.getOrDefault(appUserDto.getAppUserType(), user -> {}).accept(savedEntity);

        return generateAppUserDto(savedEntity);
    }

    @Transactional
    @Override
    public AppUserDto update(AppUserDto obj, Long userId) {
        AppUser savedEntity = appUserRepo.save(generateAppUserEntity(obj, userId, false));
        return generateAppUserDto(savedEntity);
    }

    @Transactional
    @Override
    public boolean delete(AppUserDto obj, Long userId) {
        if (!ObjectUtils.isEmpty(obj.getId())) {
            AppUser entity = new AppUser();
            entity.setId(obj.getId());
            appUserRepo.delete(entity);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AppUserDto getById(Long id, Long userId) {
        Optional<AppUser> dataList = appUserRepo.findById(id);
        if (dataList.isEmpty()) {
            return null;
        } else {
            return generateAppUserDto(dataList.get());
        }
    }

    @Override
    public List<BaseDropdownModel> getDropdownList(Long userId) {
        return appUserRepo.findDropdownModel();
    }

    @Override
    public Page<AppUserDto> getPageableAllData(BasePageableRequest pageableBodyRequest, Long userId) {
        PageRequest pageRequest = baseUtils.getPageRequest(pageableBodyRequest.getPage(),pageableBodyRequest.getSize());
        Page<AppUser> appUserPage = appUserRepo.findAll(pageRequest);
        List<AppUserDto> appUserDtoList = convertEntityListToDtoList(appUserPage.stream());
        return new PageImpl<>(appUserDtoList,pageRequest,appUserPage.getTotalElements());
    }

    @Override
    public AppUserDto getUserByUserId(Long userId) {
        Optional<AppUser> dataList = appUserRepo.findById(userId);
        if (dataList.isEmpty()) {
            return null;
        } else {
            return generateAppUserDto(dataList.get());
        }
    }

    //..................... Generate Model....................//

    public AppUser generateAppUserEntity(AppUserDto dto, Long userId, Boolean isSaved) {
        AppUser entity = new AppUser();
        BeanUtils.copyProperties(dto, entity);
        PasswordPolicy pw = new PasswordPolicy();
        try {
            pw = passwordPolicyRepo.findById(dto.getPasswordPolicyId()).orElseThrow(() -> new NameNotFoundException("Not Found"));
        } catch (Exception e) {
            System.out.println("password policy fetching problem");

        }

        String previousPassword = null;

        if (isSaved) {
            entity.setEntryUser(userId);
            entity.setPasswordPolicy(pw);
            entity.setUserTypeId(USER_TYPE_ID_CUSTOMER);
            baseUtils.setEntryUserInfo(entity);

        } else {
            AppUser dbEntity = appUserRepo.findById(dto.getId()).get();
            System.out.println(dbEntity);
            previousPassword = dbEntity.getPassword();
            entity.setUpdateUser(userId);
            baseUtils.setUpdateUserInfo(entity, dbEntity);
            //entity = dbEntity;
        }


        if (!ObjectUtils.isEmpty(dto.getPasswordPolicyId())) {
            PasswordPolicy passwordPolicy = new PasswordPolicy();
            passwordPolicy.setId(dto.getPasswordPolicyId());
            entity.setPasswordPolicy(passwordPolicy);
        }
        if (!ObjectUtils.isEmpty(dto.getPassword())) {
            entity.setPassword(encoder.encode(dto.getPassword()));
        } else {
            entity.setPassword(previousPassword);
        }

//        if (ObjectUtils.isEmpty(dto.getUserTypeId()) || dto.getUserTypeId() != 2) {
//            entity.setUserTypeId(USER_TYPE_SYS_ADMIN);
//        }


        return entity;
    }

    private List<AppUserDto> convertEntityListToDtoList(Stream<AppUser> entityList) {
        return entityList.map(entity -> {
            return generateAppUserDto(entity);
        }).collect(Collectors.toList());
    }


    public AppUserDto generateAppUserDto(AppUser entity) {
        AppUserDto dto = modelMapper.map(entity, AppUserDto.class);
        //dto.setEmpCode(entity.getEmployeeCode());

        if (!ObjectUtils.isEmpty(entity.getPasswordPolicy())) {
            dto.setPasswordPolicyId(entity.getPasswordPolicy().getId());
            dto.setPasswordPolicyName(entity.getPasswordPolicy().getName());
        }

        dto.setPassword(null);
        dto.setPhone(entity.getPhone());
        dto.setDisplayName(entity.getDisplayName());
        dto.setPhoneVerified(entity.getPhoneVerified() != null ? entity.getPhoneVerified() : false);
        return dto;
    }


}
