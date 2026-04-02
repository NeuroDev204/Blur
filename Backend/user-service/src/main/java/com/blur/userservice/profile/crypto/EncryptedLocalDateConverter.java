package com.blur.userservice.profile.crypto;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EncryptedLocalDateConverter implements Neo4jPersistentPropertyConverter<LocalDate> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private final FieldEncryptionService encryptionService;

    public EncryptedLocalDateConverter() {
        this.encryptionService = FieldEncryptionService.getInstance();
    }

    @Override
    public Value write(@Nullable LocalDate source) {
        if (source == null) {
            return Values.NULL;
        }
        String dateString = source.format(FORMATTER);
        return Values.value(encryptionService.encrypt(dateString));
    }

    @Nullable
    @Override
    public LocalDate read(Value source) {
        if (source == null || source.isNull()) {
            return null;
        }
        String decrypted = encryptionService.decrypt(source.asString());
        return LocalDate.parse(decrypted, FORMATTER);
    }
}
