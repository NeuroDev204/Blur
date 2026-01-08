package com.blur.chatservice.service;

import java.text.ParseException;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.blur.chatservice.dto.request.IntrospectRequest;
import com.blur.chatservice.dto.response.IntrospecResponse;
import com.blur.chatservice.repository.httpclient.IdentityClient;
import com.nimbusds.jose.JOSEException;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityService {
    IdentityClient identityClient;

    public IntrospecResponse introspect(IntrospectRequest request) {
        try {
            var res = identityClient.introspect(request).getResult();
            if (Objects.isNull(res)) {
                return IntrospecResponse.builder().valid(false).build();
            }
            return res;
        } catch (FeignException e) {
            return IntrospecResponse.builder().valid(false).build();

        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
