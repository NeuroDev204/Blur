package com.blur.userservice.profile.service;

import com.blur.userservice.profile.dto.request.AuthRequest;
import com.blur.userservice.profile.dto.request.ExchangeTokenRequest;
import com.blur.userservice.profile.dto.request.IntrospectRequest;
import com.blur.userservice.profile.dto.request.LogoutRequest;
import com.blur.userservice.profile.dto.request.RefreshRequest;
import com.blur.userservice.profile.dto.response.AuthResponse;
import com.blur.userservice.profile.dto.response.IntrospectResponse;
import com.blur.userservice.profile.entity.UserProfile;
import com.blur.userservice.profile.exception.AppException;
import com.blur.userservice.profile.exception.ErrorCode;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.blur.userservice.profile.repository.httpclient.OutboundIdentityClient;
import com.blur.userservice.profile.repository.httpclient.OutboundUserClient;
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
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserProfileRepository userProfileRepository;
    PasswordEncoder passwordEncoder;
    OutboundIdentityClient outboundIdentityClient;
    OutboundUserClient outboundUserClient;
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
        var userProfile = userProfileRepository
                .findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean authenticated = StringUtils.hasText(userProfile.getPasswordHash())
                && passwordEncoder.matches(authRequest.getPassword(), userProfile.getPasswordHash());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var token = generateToken(userProfile);
        redisService.setOnline(userProfile.getUserId());
        return AuthResponse.builder().token(token).authenticated(true).build();
    }

    private String generateToken(UserProfile userProfile) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userProfile.getUserId())
                .issuer("Blur.vn")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(userProfile))
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
            redisService.invalidateToken(jit, expiryTime);
            redisService.setOffline(signToken.getJWTClaimsSet().getSubject());
        } catch (AppException e) {
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signJWT = verifyToken(request.getToken(), true);
        var jit = signJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signJWT.getJWTClaimsSet().getExpirationTime();

        redisService.invalidateToken(jit, expiryTime);

        String userId = signJWT.getJWTClaimsSet().getSubject();
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String token = generateToken(userProfile);

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
        if (redisService.isTokenInvalidated(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return signedJWT;
    }

    private String buildScope(UserProfile userProfile) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        List<String> roles = userProfile.getRoles();
        if (!CollectionUtils.isEmpty(roles)) {
            roles.forEach(role -> stringJoiner.add("ROLE_" + role));
        }
        return stringJoiner.toString();
    }

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

        var userProfile = userProfileRepository
                .findByUsername(userInfo.getEmail())
                .orElseGet(() -> {
                    UserProfile created = new UserProfile();
                    created.setUserId(UUID.randomUUID().toString());
                    created.setUsername(userInfo.getEmail());
                    created.setEmail(userInfo.getEmail());
                    created.setFirstName(userInfo.getGivenName());
                    created.setLastName(userInfo.getFamilyName());
                    created.setImageUrl(userInfo.getPicture());
                    created.setEmailVerified(userInfo.isVerifiedEmail());
                    created.setRoles(List.of("USER"));
                    created.setCreatedAt(LocalDate.now());
                    return userProfileRepository.save(created);
                });

        var token = generateToken(userProfile);

        return AuthResponse.builder().token(token).authenticated(true).build();
    }
}
