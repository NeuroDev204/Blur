package com.blur.userservice.profile.configuration;

import com.blur.userservice.profile.crypto.FieldEncryptionService;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.blur.userservice.profile.service.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlurUserInitializer implements ApplicationRunner {

    private static final String BLUR_USERNAME = "blur";
    private static final String BLUR_EMAIL = "blur.socialnetwork@gmail.com";
    private static final String BLUR_PASSWORD = "10012004";

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final FieldEncryptionService fieldEncryptionService;
    private final KeycloakUserService keycloakUserService;

    @Override
    public void run(ApplicationArguments args) {
        UserProfile blur = userProfileRepository.findByUsername(BLUR_USERNAME)
                .orElseGet(this::createBlurUserProfile);
        blur = normalizeBlurProfile(blur);

        ensureBlurUserInKeycloak(blur);
    }

    private UserProfile createBlurUserProfile() {
        UserProfile blur = new UserProfile();
        blur.setUserId(UUID.randomUUID().toString());
        blur.setUsername(BLUR_USERNAME);
        blur.setEmail(BLUR_EMAIL);
        blur.setEmailIndex(fieldEncryptionService.blindIndex(BLUR_EMAIL));
        blur.setFirstName("Blur");
        blur.setLastName("");
        blur.setPasswordHash(passwordEncoder.encode(BLUR_PASSWORD));
        blur.setRoles(List.of("USER"));
        blur.setEmailVerified(true);
        blur.setVerified(true);
        blur.setCreatedAt(LocalDate.now());

        UserProfile saved = userProfileRepository.save(blur);
        log.info("Blur system user created in user-service database");
        return saved;
    }

    private UserProfile normalizeBlurProfile(UserProfile blur) {
        boolean shouldUpdate = false;

        if (!StringUtils.hasText(blur.getUserId())) {
            blur.setUserId(UUID.randomUUID().toString());
            shouldUpdate = true;
        }
        if (!BLUR_EMAIL.equalsIgnoreCase(blur.getEmail())) {
            blur.setEmail(BLUR_EMAIL);
            shouldUpdate = true;
        }
        String expectedEmailIndex = fieldEncryptionService.blindIndex(BLUR_EMAIL);
        if (!Objects.equals(expectedEmailIndex, blur.getEmailIndex())) {
            blur.setEmailIndex(expectedEmailIndex);
            shouldUpdate = true;
        }
        if (!Boolean.TRUE.equals(blur.getEmailVerified())) {
            blur.setEmailVerified(true);
            shouldUpdate = true;
        }
        if (!Boolean.TRUE.equals(blur.getVerified())) {
            blur.setVerified(true);
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            UserProfile saved = userProfileRepository.save(blur);
            log.info("Normalized Blur system user in user-service database");
            return saved;
        }

        return blur;
    }

    private void ensureBlurUserInKeycloak(UserProfile blur) {
        try {
            keycloakUserService.ensureUser(
                    BLUR_USERNAME,
                    BLUR_EMAIL,
                    BLUR_PASSWORD,
                    "Blur",
                    "",
                    blur.getUserId(),
                    List.of("USER")
            );
        } catch (Exception e) {
            log.error("Failed to ensure Blur system user in Keycloak: {}", e.getMessage(), e);
        }
    }
}
