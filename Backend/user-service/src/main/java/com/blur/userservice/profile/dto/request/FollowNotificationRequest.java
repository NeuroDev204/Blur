package com.blur.userservice.profile.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FollowNotificationRequest {
    String senderId;
    String senderName;
    String receiverId;
    String receiverName;
    String receiverEmail;
}
