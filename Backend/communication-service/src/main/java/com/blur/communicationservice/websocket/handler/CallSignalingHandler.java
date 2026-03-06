package com.blur.communicationservice.websocket.handler;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.blur.communicationservice.chat.service.CallService;
import com.blur.communicationservice.entity.CallSession;
import com.blur.communicationservice.enums.CallStatus;
import com.blur.communicationservice.enums.CallType;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;
import com.blur.communicationservice.websocket.service.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CallSignalingHandler {

    private final CallService callService;
    private final WebSocketNotificationService wsService;
    private final WebSocketSessionManager sessionManager;

    @MessageMapping("/call.initiate")
    public void onCallInitiate(@Payload Map<String, Object> data, Principal principal) {
        String callerId = principal.getName();
        String receiverId = (String) data.get("receiverId");
        CallType callType = CallType.valueOf((String) data.get("callType"));

        if (!sessionManager.isUserOnline(receiverId)) {
            wsService.sendCallEvent(callerId, Map.of("event", "call:failed", "reason", "User offline"));
            return;
        }

        CallSession session = callService.initiateCall(
                callerId,
                (String) data.get("callerName"),
                (String) data.get("callerAvatar"),
                receiverId,
                (String) data.get("receiverName"),
                (String) data.get("receiverAvatar"),
                callType,
                null,
                (String) data.get("conversationId"));

        wsService.sendCallEvent(callerId, Map.of("event", "call:initiated", "callId", session.getId()));
        wsService.sendCallEvent(
                receiverId,
                Map.of(
                        "event", "call:incoming",
                        "callId", session.getId(),
                        "callerId", callerId,
                        "callerName", data.getOrDefault("callerName", ""),
                        "callerAvatar", data.getOrDefault("callerAvatar", ""),
                        "callType", callType.name()));
    }

    @MessageMapping("/call.answer")
    public void onCallAnswer(@Payload Map<String, Object> data, Principal principal) {
        String callId = (String) data.get("callId");
        CallSession session = callService.updateCallStatus(callId, CallStatus.ANSWERED, null);
        if (session != null) {
            wsService.sendCallEvent(session.getCallerId(), Map.of("event", "call:answered", "callId", callId));
        }
    }

    @MessageMapping("/call.reject")
    public void onCallReject(@Payload Map<String, Object> data, Principal principal) {
        String callId = (String) data.get("callId");
        CallSession session = callService.updateCallStatus(callId, CallStatus.REJECTED, null);
        if (session != null) {
            wsService.sendCallEvent(session.getCallerId(), Map.of("event", "call:rejected", "callId", callId));
        }
    }

    @MessageMapping("/call.end")
    public void onCallEnd(@Payload Map<String, Object> data, Principal principal) {
        String callId = (String) data.get("callId");
        CallSession session = callService.endCall(callId, principal.getName());
        if (session != null) {
            Map<String, Object> endData = Map.of(
                    "event",
                    "call:ended",
                    "callId",
                    callId,
                    "duration",
                    session.getDuration() != null ? session.getDuration() : 0);
            wsService.sendCallEvent(session.getCallerId(), endData);
            wsService.sendCallEvent(session.getReceiverId(), endData);
        }
    }

    @MessageMapping("/webrtc.offer")
    public void onWebRTCOffer(@Payload Map<String, Object> data, Principal principal) {
        String to = (String) data.get("to");
        data.put("from", principal.getName());
        wsService.sendWebRTCSignal(to, Map.of("event", "webrtc:offer", "data", data));
    }

    @MessageMapping("/webrtc.answer")
    public void onWebRTCAnswer(@Payload Map<String, Object> data, Principal principal) {
        String to = (String) data.get("to");
        data.put("from", principal.getName());
        wsService.sendWebRTCSignal(to, Map.of("event", "webrtc:answer", "data", data));
    }

    @MessageMapping("/webrtc.ice-candidate")
    public void onICECandidate(@Payload Map<String, Object> data, Principal principal) {
        String to = (String) data.get("to");
        data.put("from", principal.getName());
        wsService.sendWebRTCSignal(to, Map.of("event", "webrtc:ice-candidate", "data", data));
    }
}
