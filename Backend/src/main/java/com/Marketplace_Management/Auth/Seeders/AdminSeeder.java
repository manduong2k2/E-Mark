package com.Marketplace_Management.Auth.Seeders;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Constants.UserStatus;
import com.Marketplace_Management.Auth.Contracts.IRoleRepository;
import com.Marketplace_Management.Auth.Contracts.IUserRepository;
import com.Marketplace_Management.Auth.Entities.RoleEntity;
import com.Marketplace_Management.Auth.Entities.UserEntity;

import jakarta.transaction.Transactional;

@Component
public class AdminSeeder {
    private final PasswordEncoder passwordEncoder;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    
    public AdminSeeder(IUserRepository userRepository, PasswordEncoder passwordEncoder, IRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }
    
    @Transactional
    public void seedAdmin() {
        UserEntity admin = UserEntity.builder()
                .email("admin@e-mark.com")
                .password(passwordEncoder.encode("123456"))
                .status(UserStatus.ACTIVE)
                .name("Admin")
                .build();

        if (userRepository.findByEmail(admin.getEmail()).isEmpty()) {
            
            Set<RoleEntity> roles = new HashSet<>();
            roles.add(roleRepository.findByCode("ADMIN").orElseThrow(() -> new RuntimeException(Message.ROLE_NOT_FOUND)));
            admin.setRoles(roles);
            this.userRepository.save(admin);
        }
    }
}
