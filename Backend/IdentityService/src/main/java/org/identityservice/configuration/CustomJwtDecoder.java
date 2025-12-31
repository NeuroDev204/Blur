package org.identityservice.configuration;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signerKey}")
    public String SECRET;

    @Override
    public Jwt decode(String token) throws JwtException {
        // giai ma token de lay thong tin
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(new MACVerifier(SECRET))) {
                throw new BlurException(ErrorCode.INVALID_SECRET_KEY);
            }
            return new Jwt(
                    token,
                    signedJWT.getJWTClaimsSet().getIssueTime().toInstant(),
                    signedJWT.getJWTClaimsSet().getExpirationTime().toInstant(),
                    signedJWT.getHeader().toJSONObject(),
                    signedJWT.getJWTClaimsSet().getClaims());
        } catch (ParseException e) {
            throw new JwtException("Invalid token");
        } catch (JOSEException e) {
            throw new BlurException(ErrorCode.INVALID_SECRET_KEY);
        }
    }
}
