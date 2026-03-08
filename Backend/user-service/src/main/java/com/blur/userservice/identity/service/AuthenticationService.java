package com.blur.userservice.identity.service;

import com.blur.userservice.identity.dto.request.*;
import com.blur.userservice.identity.dto.response.AuthResponse;
import com.blur.userservice.identity.dto.response.IntrospectResponse;
import com.blur.userservice.identity.entity.InvalidatedToken;
import com.blur.userservice.identity.entity.Role;
import com.blur.userservice.identity.entity.User;
import com.blur.userservice.identity.exception.AppException;
import com.blur.userservice.identity.exception.ErrorCode;
import com.blur.userservice.identity.repository.InvalidatedTokenRepository;
import com.blur.userservice.identity.repository.UserRepository;
import com.blur.userservice.identity.repository.httpclient.OutboundIdentityClient;
import com.blur.userservice.identity.repository.httpclient.OutboundUserClient;
import com.blur.userservice.profile.dto.request.ProfileCreationRequest;
import com.blur.userservice.profile.service.UserProfileService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    InvalidatedTokenRepository tokenRepository;
    OutboundIdentityClient outboundIdentityClient;
    OutboundUserClient outboundUserClient;
    UserProfileService userProfileService;
    RedisService redisService;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;
    @NonFinal
    @Value("${outbound.identity.client-id}")
    protected String CLIENT_ID;
    @NonFinal
    @Value("${outbound.identity.client-secret}")
    protected String CLIENT_SECRET;
    @NonFinal
    @Value("${outbound.identity.redirect-url}")
    protected String REDIRECT_URL;
    @NonFinal
    @Value("${outbound.identity.grant-type}")
    protected String GRANT_TYPE;

    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest authRequest) {
        var user = userRepository
                .findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean authenticated = passwordEncoder.matches(authRequest.getPassword(), user.getPassword());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var token = generateToken(user);
        redisService.setOnline(user.getId());
        return AuthResponse.builder().token(token).authenticated(true).build();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .issuer("Blur.vn")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest introspectRequest) throws JOSEException, ParseException {
        var token = introspectRequest.getToken();
        boolean isValid = true;
        SignedJWT signedJWT = null;
        try {
            signedJWT = verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .userId(Objects.nonNull(signedJWT) ? signedJWT.getJWTClaimsSet().getSubject() : null)
                .valid(isValid)
                .build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);
            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();
            tokenRepository.save(invalidatedToken);
            redisService.setOffline(signToken.getJWTClaimsSet().getSubject());
        } catch (AppException e) {
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signJWT = verifyToken(request.getToken(), true);
        var jit = signJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();
        tokenRepository.save(invalidatedToken);

        // subject hiện tại là userId (do generateToken dùng user.getId())
        String userId = signJWT.getJWTClaimsSet().getSubject();

        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String token = generateToken(user);

        return AuthResponse.builder().token(token).authenticated(true).build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expirationDate = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(verifier);
        if (!verified && expirationDate.after(new Date())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (tokenRepository.existsById((signedJWT.getJWTClaimsSet().getJWTID()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return signedJWT;
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
                }
            });
        }
        return stringJoiner.toString();
    }

    // login with google
    @Transactional
    public AuthResponse outboundAuthenticationService(String code) {
        var response = outboundIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(code)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URL)
                .grantType(GRANT_TYPE)
                .build());

        var userInfo = outboundUserClient.exchangeToken("json", response.getAccessToken());

        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().name("USER").build());

        var user = userRepository
                .findByUsername(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(userInfo.getEmail())
                        .firstName(userInfo.getGivenName())
                        .lastName(userInfo.getFamilyName())
                        .roles(roles)
                        .build()));

        var token = generateToken(user);

        // LOCAL CALL thay vi Feign
        userProfileService.createProfile(ProfileCreationRequest.builder()
                .userId(user.getId())
                .firstName(userInfo.getGivenName())
                .lastName(userInfo.getFamilyName())
                .build());

        return AuthResponse.builder().token(token).build();
    }
}
