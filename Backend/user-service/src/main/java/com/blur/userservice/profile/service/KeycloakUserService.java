package com.blur.userservice.profile.service;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KeycloakUserService {
    @Value("${keycloak.admin.server-url}")
    private String serverUrl;
    @Value("${keycloak.admin.realm}")
    private String realm;
    @Value("${keycloak.admin.client-id}")
    private String clientId;
    @Value("${keycloak.admin.client_secret}")
    private String clientSecret;
    private Keycloak keycloak;

    @PostConstruct
    public void init() {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .build();
    }

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
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            usersResource.get(keycloakUserId).resetPassword(credential);

            log.info("Created Keycloak user: username={}, blurUserId={}, keycloakId={}", username, blurUserId, keycloakUserId);
            return keycloakUserId;
        } catch (Exception e) {
            log.error("Post-create setup failed for Keycloak user {}, rolling back: {}", keycloakUserId, e.getMessage());
            deleteUser(keycloakUserId);
            throw e;
        }
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
}
