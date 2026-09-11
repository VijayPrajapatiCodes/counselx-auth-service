package com.counselx.auth.config;

import com.counselx.auth.entity.roles;
import com.counselx.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RoleConfig {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {

            createRole(roleRepository, "STUDENT");
            createRole(roleRepository, "ADMIN");
            createRole(roleRepository, "OFFICIAL");
            createRole(roleRepository, "COLLEGE");
        };
    }

    private void createRole(RoleRepository roleRepository, String roleName) {

        if (roleRepository.findByName(roleName).isEmpty()) {

            roles role = roles.builder()
                    .name(roleName)
                    .build();

            roleRepository.save(role);
        }
    }
}