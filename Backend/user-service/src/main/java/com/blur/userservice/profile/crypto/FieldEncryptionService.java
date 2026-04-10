package com.blur.userservice.profile.crypto;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class FieldEncryptionService {
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final SecretKey encryptionKey;
    private final SecretKey hmacKey;
    private final SecureRandom secureRandom;

    private static FieldEncryptionService instance;

    public static FieldEncryptionService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FieldEncryptionService has not been initialized");
        }
        return instance;
    }

    public FieldEncryptionService(
            @Value("${encryption.field-key}") String base64EncryptionKey,
            @Value("${encryption.hmac-key}") String base64HmacKey
    ) {
        byte[] encKeyBytes = Base64.getDecoder().decode(base64EncryptionKey);
        byte[] hmacKeyBytes = Base64.getDecoder().decode(base64HmacKey);

        //validate key length
        if (encKeyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 32 bytes (256 bits), got:" + encKeyBytes.length);
        }
        if (hmacKeyBytes.length < 32) {
            throw new IllegalArgumentException("HMAC key must be at least 32 bytes, got:" + hmacKeyBytes.length);
        }
        this.encryptionKey = new SecretKeySpec(encKeyBytes, "AES");
        this.hmacKey = new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM);
        this.secureRandom = new SecureRandom();
        instance = this;
    }

    // ma hoa plaintext bang aes256 gcm
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            // generate random IV (12 bytes)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // setup cipher
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);

            // encrypt
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // iv + cipher text
            ByteBuffer output = ByteBuffer.allocate(iv.length + cipherText.length);
            output.put(iv);
            output.put(cipherText);

            // base 64 encode
            return Base64.getEncoder().encodeToString(output.array());
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed encryption failed", e);
        }
    }

    // giai ma cipher text bang AES-256-GCM
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            // bas64 decode
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            // extract iv
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            //  extract cipher text
            byte[] encryptedData = new byte[buffer.remaining()];
            buffer.get(encryptedData);

            // decrypt
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);
            byte[] plaintext = cipher.doFinal(encryptedData);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Field decryption failed", e);
        }
    }

    // tinh blind text bang hmac sha 256
    public String blindIndex(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            byte[] hash = mac.doFinal(plaintext.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));

            // hex encode
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Blind index calculation failed: {}", e.getMessage());
            throw new RuntimeException("Blind index failed ", e);
        }
    }
}
