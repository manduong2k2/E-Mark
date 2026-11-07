package com.Marketplace_Management.Auth.Seeders;

import java.util.List;

import org.springframework.stereotype.Component;

import com.Marketplace_Management.Auth.Contracts.IRoleRepository;
import com.Marketplace_Management.Auth.Entities.Role;

@Component
public class RoleSeeder {
    private final IRoleRepository roleRepository;

    public RoleSeeder(IRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    
    public void seedRoles() {
        if(roleRepository.count() == 0) {
            this.roleRepository.saveAll(List.of(
                new Role(null,"Admin", "ADMIN"),
                new Role(null,"User", "USER"),
                new Role(null,"Vendor", "VENDOR")
            ));
        }
    }
}
