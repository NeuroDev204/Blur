package org.identityservice.dto.request;

import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationPasswordRequest {
    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;
}
