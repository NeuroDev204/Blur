package com.blur.userservice.profile.resilience;

import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientKeycloakService {
  private final Keycloak keycloak;

  @Value("${keycloak.admin.realm:blur}")
  private String realm;

  @Bulkhead(name = "keycloakBH")
  @CircuitBreaker(name = "keycloakCB", fallbackMethod = "createUserFallback")
  @Retry(name = "keycloakRetry")
  public Response createUser(UserRepresentation user){
    return getUsersResouce().create(user);
  }
  @Bulkhead(name = "keycloakBH")
  @CircuitBreaker(name = "keycloakCB", fallbackMethod = "searchUsersFallback")
  @Retry(name = "keycloakRetry")
  public List<UserRepresentation> searchByUsername(String username){
    return getUsersResouce().searchByUsername(username, true);
  }

  @Bulkhead(name = "keycloakBH")
  @CircuitBreaker(name = "keycloakCB")
  @Retry(name = "keycloakRetry")
  public void deleteUser(String keycloakUserId){
    getUsersResouce().delete(keycloakUserId);
  }
  private UsersResource getUsersResouce(){
    return keycloak.realm(realm).users();
  }
  // fall back
  private Response createUserFallback(UserRepresentation user , Throwable t){
    // dang ky phai that bai neu keycloak down
    throw new RuntimeException("He thong dang bao tri, vui long thu lai sau");
  }
  private List<UserRepresentation> searchUsersFallback(String username, Throwable t){
    throw new RuntimeException("He thong dang bao tri, vui long thu lai sau");
  }
}
