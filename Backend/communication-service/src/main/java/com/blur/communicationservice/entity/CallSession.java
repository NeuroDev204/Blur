package com.blur.communicationservice.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.blur.communicationservice.enums.CallStatus;
import com.blur.communicationservice.enums.CallType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "call_sessions")
@NoArgsConstructor
@AllArgsConstructor
public class CallSession {

    @Id
    String id;

    String callerId;
    String callerName;
    String callerAvatar;

    String receiverId;
    String receiverName;
    String receiverAvatar;

    CallType callType;
    CallStatus callStatus;

    LocalDateTime createdAt;
    LocalDateTime startTime;
    LocalDateTime endTime;
    Long duration;

    // socketIds
    String callerSockerId;
    String receiverSockerId;

    String conversationId;
    String endReason;
}
