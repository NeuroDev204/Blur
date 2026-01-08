package org.identityservice.configuration;

import java.util.HashSet;

import org.identityservice.entity.Role;
import org.identityservice.entity.User;
import org.identityservice.repository.RoleRepository;
import org.identityservice.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppConfiguration {
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Bean
    ApplicationRunner runner(UserRepository userRepository) {
        return args -> {
            Role userRole = roleRepository.save(
                    Role.builder().name("USER").description("User Role").build());
            Role adminRole = roleRepository.save(
                    Role.builder().name("ADMIN").description("Admin role").build());
            var roles = new HashSet<Role>();
            roles.add(adminRole);
            if (userRepository.findByUsername("admin").isEmpty()) {
                User user = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .roles(roles)
                        .email("")
                        .emailVerified(true)
                        .build();
                userRepository.save(user);

                log.info("admin user created with default password admin");
            }
        };
    }
}
