package com.blur.userservice.profile.crypto;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.lang.Nullable;

public class EncryptedStringConverter implements Neo4jPersistentPropertyConverter<String> {
    private final FieldEncryptionService encryptionService;

    public EncryptedStringConverter() {
        this.encryptionService = FieldEncryptionService.getInstance();
    }

    @Override
    public Value write(@Nullable String source) {
        if (source == null) {
            return Values.NULL;
        }
        return Values.value(encryptionService.encrypt(source));
    }

    @Nullable
    @Override
    public String read(Value source) {
        if (source == null || source.isNull()) {
            return null;
        }
        return encryptionService.decrypt(source.asString());
    }
}
