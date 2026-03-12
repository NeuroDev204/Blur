package com.blur.userservice.profile.configuration;

import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlurUserInitializer implements ApplicationRunner {

    private static final String BLUR_USERNAME = "blur";

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userProfileRepository.findByUsername(BLUR_USERNAME).isPresent()) {
            log.info("Blur system user already exists, skipping initialization");
            return;
        }

        UserProfile blur = new UserProfile();
        blur.setUserId(UUID.randomUUID().toString());
        blur.setUsername(BLUR_USERNAME);
        blur.setEmail("blur.socialnetwork@gmail.com");
        blur.setFirstName("Blur");
        blur.setLastName("");
        blur.setPasswordHash(passwordEncoder.encode("10012004"));
        blur.setRoles(List.of("USER"));
        blur.setEmailVerified(true);
        blur.setVerified(true);
        blur.setCreatedAt(LocalDate.now());

        userProfileRepository.save(blur);
        log.info("Blur system user created successfully");
    }
}
