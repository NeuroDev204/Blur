package com.blur.chatservice.dto.request;

import java.util.ArrayList;

import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationRequest {
    String type;

    @Size(min = 1)
    @NonNull
    ArrayList<String> participantIds;
}
