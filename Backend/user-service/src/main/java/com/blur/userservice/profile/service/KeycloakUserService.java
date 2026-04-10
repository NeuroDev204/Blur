package com.blur.userservice.profile.service;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {
    private final Keycloak keycloak;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public String createUser(String username, String email, String password, String firstName, String lastName, String blurUserId, List<String> roles) {
        RealmResource realmResource = getRealmResource();
        UsersResource usersResource = realmResource.users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setAttributes(Map.of("blurUserId", List.of(blurUserId)));

        String keycloakUserId = null;
        try (Response response = usersResource.create(user)) {
            if (response.getStatus() == 201) {
                String locationHeader = response.getHeaderString("Location");
                keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
            } else if (response.getStatus() == 409) {
                log.warn("Keycloak user already exists: username={}", username);
                throw new RuntimeException("User already exists in Keycloak");
            } else {
                log.error("Failed to create Keycloak user: status={}, username={}", response.getStatus(), username);
                throw new RuntimeException("Failed to create Keycloak user: " + response.getStatus());
            }
        }

        try {
            resetPassword(usersResource, keycloakUserId, password);

            log.info("Created Keycloak user: username={}, blurUserId={}, keycloakId={}", username, blurUserId, keycloakUserId);
            return keycloakUserId;
        } catch (Exception e) {
            log.error("Post-create setup failed for Keycloak user {}, rolling back: {}", keycloakUserId, e.getMessage());
            deleteUser(keycloakUserId);
            throw e;
        }
    }

    public String ensureUser(String username, String email, String password, String firstName, String lastName, String blurUserId, List<String> roles) {
        RealmResource realmResource = getRealmResource();
        UsersResource usersResource = realmResource.users();
        Optional<UserRepresentation> existing = findByUsername(usersResource, username);

        if (existing.isEmpty()) {
            return createUser(username, email, password, firstName, lastName, blurUserId, roles);
        }

        UserRepresentation user = existing.get();
        boolean shouldUpdate = false;

        if (!Boolean.TRUE.equals(user.isEnabled())) {
            user.setEnabled(true);
            shouldUpdate = true;
        }
        if (!Boolean.TRUE.equals(user.isEmailVerified())) {
            user.setEmailVerified(true);
            shouldUpdate = true;
        }
        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(email);
            shouldUpdate = true;
        }

        Map<String, List<String>> attributes = user.getAttributes() == null
                ? new HashMap<>()
                : new HashMap<>(user.getAttributes());
        String existingBlurUserId = firstAttributeValue(attributes, "blurUserId").orElse(null);
        if (!blurUserId.equals(existingBlurUserId)) {
            attributes.put("blurUserId", List.of(blurUserId));
            user.setAttributes(attributes);
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            usersResource.get(user.getId()).update(user);
        }

        // Keep system account credentials deterministic for local/dev login.
        resetPassword(usersResource, user.getId(), password);

        log.info("Ensured Keycloak user: username={}, blurUserId={}, keycloakId={}", username, blurUserId, user.getId());
        return user.getId();
    }

    public void deleteUser(String keycloakUserId) {
        try {
            getRealmResource().users().get(keycloakUserId).remove();
            log.info("Deleted Keycloak user: {}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to delete Keycloak user {}: {}", keycloakUserId, e.getMessage());
        }
    }

    private RealmResource getRealmResource() {
        return keycloak.realm(realm);
    }

    private Optional<UserRepresentation> findByUsername(UsersResource usersResource, String username) {
        return usersResource.searchByUsername(username, true).stream()
                .filter(user -> user.getUsername() != null && user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    private Optional<String> firstAttributeValue(Map<String, List<String>> attributes, String attributeName) {
        List<String> values = attributes.get(attributeName);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(0));
    }

    private void resetPassword(UsersResource usersResource, String keycloakUserId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        usersResource.get(keycloakUserId).resetPassword(credential);
    }
}
