package com.blur.communicationservice.websocket.handler;

import java.security.Principal;
import java.util.LinkedHashMap;
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

@Controller
@RequiredArgsConstructor
public class CallSignalingHandler {

    private final CallService callService;
    private final WebSocketNotificationService wsService;
    private final WebSocketSessionManager sessionManager;

    @MessageMapping("/call.initiate")
    public void onCallInitiate(@Payload Map<String, Object> data, Principal principal) {
        String callerId = principal != null ? principal.getName() : null;
        CallSession session = null;

        if (callerId == null || callerId.isBlank()) {
            return;
        }

        try {
            String receiverId = stringValue(data.get("receiverId"));
            String callTypeValue = stringValue(data.get("callType"));

            if (receiverId.isBlank() || callTypeValue.isBlank()) {
                wsService.sendCallEvent(callerId, failedEvent("Invalid call payload"));
                return;
            }

            CallType callType = CallType.valueOf(callTypeValue);

            if (!sessionManager.isUserOnline(receiverId)) {
                wsService.sendCallEvent(callerId, failedEvent("User offline"));
                return;
            }

            session = callService.initiateCall(
                    callerId,
                    stringValue(data.get("callerName")),
                    nullableStringValue(data.get("callerAvatar")),
                    receiverId,
                    stringValue(data.get("receiverName")),
                    nullableStringValue(data.get("receiverAvatar")),
                    callType,
                    null,
                    nullableStringValue(data.get("conversationId")));

            wsService.sendCallEvent(callerId, Map.of("event", "call:initiated", "callId", session.getId()));
            wsService.sendCallEvent(receiverId, incomingCallEvent(session, callerId, callType, data));
        } catch (RuntimeException ex) {
            if (session != null) {
                callService.updateCallStatus(session.getId(), CallStatus.FAILED, null);
            }
            wsService.sendCallEvent(callerId, failedEvent(ex.getMessage()));
        }
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

    private Map<String, Object> incomingCallEvent(
            CallSession session, String callerId, CallType callType, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "call:incoming");
        payload.put("callId", session.getId());
        payload.put("callerId", callerId);
        payload.put("callerName", stringValue(data.get("callerName")));
        payload.put("callerAvatar", stringValue(data.get("callerAvatar")));
        payload.put("callType", callType.name());
        return payload;
    }

    private Map<String, Object> failedEvent(String reason) {
        return Map.of("event", "call:failed", "reason", stringValue(reason));
    }

    private String nullableStringValue(Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }

    private String stringValue(Object value) {
        String stringValue = nullableStringValue(value);
        return stringValue != null ? stringValue : "";
    }
}
